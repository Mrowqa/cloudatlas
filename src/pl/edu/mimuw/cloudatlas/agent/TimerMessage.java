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
	public enum MessageType {
		ADD, CANCEL
	}

	public MessageType type;
	public long id;
	public Duration duration;
	public ModuleMessage callbackMessage;

	public TimerMessage(long id) {
		this.module = ModuleMessage.Module.TIMER;
		this.type = MessageType.CANCEL;
		this.id = id;
	}
	
	public TimerMessage(long id, Duration duration, ModuleMessage callbackMessage) {
		this.module = ModuleMessage.Module.TIMER;
		this.type = MessageType.ADD;
		this.id = id;
		this.duration = duration;
		this.callbackMessage = callbackMessage;
	}
}
