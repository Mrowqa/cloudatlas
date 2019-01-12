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
	static private class Event {
		Duration durationBetweenEvents;
		boolean recurring;
		ModuleMessage msg;

		public static Event newRecurringEvent(Duration durationBetweenEvents, ModuleMessage msg) {
			Event event = new Event();
			event.durationBetweenEvents = durationBetweenEvents;
			event.recurring = true;
			event.msg = msg;
			return event;
		}
		
		public static Event newOneTimeEvent(ModuleMessage msg) {
			Event event = new Event();
			event.recurring = false;
			event.msg = msg;
			return event;
		}
		
		private Event() {}
	}
	
	private final TreeMultimap<LocalDateTime, Long> timeToId;
	private final HashMap<Long, LocalDateTime> idToTime;
	private final HashMap<Long, Event> idToEvent;

	EventScheduler() {
		this.timeToId = TreeMultimap.create();
		this.idToTime = new HashMap<>();
		this.idToEvent = new HashMap<>();
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
							Event event = idToEvent.get(id);
							messages.add(event.msg);
							idToEvent.remove(id);
							idToTime.remove(id);
							if (event.recurring) {
								LocalDateTime eventTime = LocalDateTime.now().plus(event.durationBetweenEvents);
								scheduleEventImpl(eventTime, id, event);
							}
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

	public synchronized void scheduleOneTimeEvent(LocalDateTime time, long id, ModuleMessage message) {
		Event event = Event.newOneTimeEvent(message);
		scheduleEventImpl(time, id, event);
		notifyAll();
	}
	
	public synchronized void scheduleRecurringEvent(LocalDateTime time, long id, Duration durationBetweenEvents, ModuleMessage message) {
		Event event = Event.newRecurringEvent(durationBetweenEvents, message);
		scheduleEventImpl(time, id, event);
		notifyAll();
	}

	private void scheduleEventImpl(LocalDateTime time, long id, Event event) {
		cancelEventImpl(id);
		timeToId.put(time, id);
		idToTime.put(id, time);
		idToEvent.put(id, event);
	}

	public synchronized void cancelEvent(long id) {
		cancelEventImpl(id);
	}

	private void cancelEventImpl(long id) {
		LocalDateTime time = idToTime.getOrDefault(id, null);
		if (time != null) {
			idToTime.remove(id);
			idToEvent.remove(id);
			timeToId.remove(time, id);
		}
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
				} catch (Exception ex) {
					Logger.getLogger(SleeperThread.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
}

public class TimerModule extends Thread implements Module {

	private final LinkedBlockingQueue<TimerMessage> messages;
	private final EventScheduler events;
	private final SleeperThread sleeperThread;
	private ModulesHandler modulesHandler;

	public TimerModule() {
		this.messages = new LinkedBlockingQueue<>();
		this.events = new EventScheduler();
		this.sleeperThread = new SleeperThread(events);
	}

	@Override
	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
		sleeperThread.setModulesHandler(modulesHandler);
	}

	@Override
	public boolean canHandleMessage(ModuleMessage message) {
		return message instanceof TimerMessage;
	}

	@Override
	public void enqueue(ModuleMessage message) throws InterruptedException {
		messages.put((TimerMessage) message);
	}

	@Override
	public void run() {
		sleeperThread.start();
		while (true) {
			try {
				TimerMessage message = messages.take();
				switch (message.type) {
					case SCHEDULE_ONE_TIME_CALLBACK: {
						LocalDateTime eventTime = LocalDateTime.now().plus(message.duration);
						events.scheduleOneTimeEvent(eventTime, message.id, message.callbackMessage);
						break;
					}
					case SCHEDULE_CYCLIC_CALLBACK: {
						LocalDateTime eventTime = LocalDateTime.now().plus(message.duration);
						events.scheduleRecurringEvent(eventTime, message.id, message.duration, message.callbackMessage);
						break;
					}
					case CANCEL_CALLBACK: {
						events.cancelEvent(message.id);
						break;
					}
					default:
						throw new IllegalArgumentException("Unsupported message type " + message.type);
				}
			} catch (Exception ex) {
				Logger.getLogger(TimerModule.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
