/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.time.Duration;

/**
 *
 * @author pawel
 */
public class TimerMessage extends ModuleMessage {
	public enum Type {
		SCHEDULE_ONE_TIME_CALLBACK, CANCEL_CALLBACK
	}

	public Type type;
	public long id;
	public Duration duration;
	public ModuleMessage callbackMessage;

	public static TimerMessage scheduleOneTimeCallback(long id, Duration callbackDuration, ModuleMessage callbackMessage) {
		return new TimerMessage(id, callbackDuration, callbackMessage);
	}
	
	public static TimerMessage cancelCallback(long id) {
		return new TimerMessage(id);
	}
	
	private TimerMessage(long id) {
		this.module = ModuleMessage.Module.TIMER;
		this.type = Type.CANCEL_CALLBACK;
		this.id = id;
	}
	
	private TimerMessage(long id, Duration duration, ModuleMessage callbackMessage) {
		this.module = ModuleMessage.Module.TIMER;
		this.type = Type.SCHEDULE_ONE_TIME_CALLBACK;
		this.id = id;
		this.duration = duration;
		this.callbackMessage = callbackMessage;
	}
}
