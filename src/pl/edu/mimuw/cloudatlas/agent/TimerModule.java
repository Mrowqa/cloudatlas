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
class SynchronizedMap {

	private final TreeMultimap<LocalDateTime, Long> timeToId;
	private final HashMap<Long, LocalDateTime> idToTime;
	private final HashMap<Long, ModuleMessage> idToCallbackMessage;

	SynchronizedMap() {
		this.timeToId = TreeMultimap.create();
		this.idToTime = new HashMap<>();
		this.idToCallbackMessage = new HashMap<>();
	}

	public synchronized Collection<ModuleMessage> handleNext() {
		while (true) {
			try {
				if (timeToId.isEmpty()) {
					wait();
				} else {
					Entry<LocalDateTime, Collection<Long>> entry = timeToId.asMap().firstEntry();
					LocalDateTime current = LocalDateTime.now();
					if (!entry.getKey().isAfter(current)) {
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

	public synchronized void addMessage(LocalDateTime time, long id, ModuleMessage message) {
		timeToId.put(time, id);
		idToTime.put(id, time);
		idToCallbackMessage.put(id, message);
		notifyAll();
	}

	public synchronized void cancel(long id) {
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
	private final SynchronizedMap events;
	private ModuleHandler moduleHandler;

	SleeperThread(SynchronizedMap events) {
		this.events = events;
	}

	public void setModuleHandler(ModuleHandler moduleHandler) {
		this.moduleHandler = moduleHandler;
	}

	@Override
	public void run() {
		while (true) {
			Collection<ModuleMessage> messages = events.handleNext();
			for (ModuleMessage message : messages) {
				try {
					moduleHandler.enqueue(message);
				} catch (InterruptedException ex) {
					Logger.getLogger(SleeperThread.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
}

public class TimerModule extends Thread {

	private final LinkedBlockingQueue<TimerMessage> messages;
	private final SynchronizedMap events;
	private final SleeperThread sleeperThread;
	private ModuleHandler moduleHandler;

	public TimerModule() {
		this.messages = new LinkedBlockingQueue<>();
		this.events = new SynchronizedMap();
		this.sleeperThread = new SleeperThread(events);
	}

	public SleeperThread getSleeperThread() {
		return sleeperThread;
	}

	public void setModuleHandler(ModuleHandler moduleHandler) {
		this.moduleHandler = moduleHandler;
		sleeperThread.setModuleHandler(moduleHandler);
	}

	public void enqueue(TimerMessage message) throws InterruptedException {
		messages.put(message);
	}

	@Override
	public void run() {
		while (true) {
			try {
				TimerMessage message = messages.take();
				LocalDateTime eventTime = LocalDateTime.now().plus(message.duration);
				if (message.type == TimerMessage.Type.ADD) {
					events.addMessage(eventTime, message.id, message.callbackMessage);
				} else {
					events.cancel(message.id);
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(TimerModule.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
