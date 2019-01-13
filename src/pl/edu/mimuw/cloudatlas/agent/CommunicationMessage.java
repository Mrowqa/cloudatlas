/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

/**
 *
 * @author mrowqa
 */
public class CommunicationMessage extends ModuleMessage {
	public enum Type {
		SEND_MESSAGE, MESSAGE_RECEIVE_TIMED_OUT,
	};
	public Type type;
	public ModuleMessage msg;  // for send
	Long msgId;  // for timed out
	
	public static CommunicationMessage sendMessage(ModuleMessage msg) {
		CommunicationMessage ret = new CommunicationMessage();
		ret.type = Type.SEND_MESSAGE;
		ret.msg = msg;
		
		if (((NetworkSendable) msg) == null) {
			throw new IllegalArgumentException(
					"Network messages have to implement NetworkSendable interface");
		}
		
		return ret;
	}
	
	static CommunicationMessage messageReceiveTimedOut(long msgId) {
		CommunicationMessage ret = new CommunicationMessage();
		ret.type = Type.MESSAGE_RECEIVE_TIMED_OUT;
		ret.msgId = msgId;
		
		return ret;
	}
}
