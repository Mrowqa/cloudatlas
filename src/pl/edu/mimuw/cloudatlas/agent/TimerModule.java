/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.TreeMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author pawel
 */
class EventScheduler {

	private final TreeMultimap<LocalDateTime, Long> timeToId;
	private final HashMap<Long, LocalDateTime> idToTime;
	private final HashMap<Long, ModuleMessage> idToCallbackMessage;

	EventScheduler() {
		this.timeToId = TreeMultimap.create();
		this.idToTime = new HashMap<>();
		this.idToCallbackMessage = new HashMap<>();
	}

	public synchronized Collection<ModuleMessage> waitForNextBatch() {
		while (true) {
			try {
				if (timeToId.isEmpty()) {
					wait();
				} else {
					Entry<LocalDateTime, Collection<Long>> entry = timeToId.asMap().firstEntry();
					LocalDateTime current = LocalDateTime.now();
					LocalDateTime eventsTime = entry.getKey();
					if (eventsTime.isEqual(current) || eventsTime.isBefore(current)) {
						LinkedList<ModuleMessage> messages = new LinkedList<>();
						for (Long id : entry.getValue()) {
							messages.add(idToCallbackMessage.get(id));
							idToCallbackMessage.remove(id);
							idToTime.remove(id);
						}
						timeToId.removeAll(entry.getKey());
						return messages;
					}
					Duration sleepInterval = Duration.between(current, entry.getKey());
					wait(sleepInterval.toMillis());
				}
			} catch (InterruptedException ex) {
			}
		}
	}

	public synchronized void scheduleEvent(LocalDateTime time, long id, ModuleMessage message) {
		timeToId.put(time, id);
		idToTime.put(id, time);
		idToCallbackMessage.put(id, message);
		notifyAll();
	}

	public synchronized void cancelEvent(long id) {
		LocalDateTime time = idToTime.getOrDefault(id, null);
		if (time != null) {
			idToTime.remove(id);
			idToCallbackMessage.remove(id);
			timeToId.remove(time, id);
		}
		notifyAll();
	}
}

class SleeperThread extends Thread {
	private final EventScheduler events;
	private ModulesHandler modulesHandler;

	SleeperThread(EventScheduler events) {
		this.events = events;
	}

	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
	}

	@Override
	public void run() {
		while (true) {
			Collection<ModuleMessage> messages = events.waitForNextBatch();
			for (ModuleMessage message : messages) {
				try {
					modulesHandler.enqueue(message);
				} catch (InterruptedException ex) {
					Logger.getLogger(SleeperThread.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
}

public class TimerModule extends Thread {

	private final LinkedBlockingQueue<TimerMessage> messages;
	private final EventScheduler events;
	private final SleeperThread sleeperThread;
	private ModulesHandler modulesHandler;

	public TimerModule() {
		this.messages = new LinkedBlockingQueue<>();
		this.events = new EventScheduler();
		this.sleeperThread = new SleeperThread(events);
	}

	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
		sleeperThread.setModulesHandler(modulesHandler);
	}

	public void enqueue(TimerMessage message) throws InterruptedException {
		messages.put(message);
	}

	@Override
	public void run() {
		sleeperThread.start();
		while (true) {
			try {
				TimerMessage message = messages.take();
				LocalDateTime eventTime = LocalDateTime.now().plus(message.duration);
				if (message.type == TimerMessage.Type.ADD_ONE_TIME_CALLBACK) {
					events.scheduleEvent(eventTime, message.id, message.callbackMessage);
				} else {
					events.cancelEvent(message.id);
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(TimerModule.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
