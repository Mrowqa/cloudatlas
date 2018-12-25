/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import java.io.Serializable;
import java.net.SocketAddress;
import java.time.Duration;

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

interface NetworkSendable {
	CommunicationInfo getCommunicationInfo();
	void setCommunicationInfo(CommunicationInfo info);
}

class CommunicationInfo implements Serializable {
	private SocketAddress addr;
	private CommunicationTimestamps ts;
	
	public CommunicationInfo(SocketAddress addr) {
		this(addr, CommunicationTimestamps.newEmpty());
	}
	
	public CommunicationInfo(SocketAddress addr, CommunicationTimestamps ts) {
		this.addr = addr;
		this.ts = ts;
	}
	
	public SocketAddress getAddress() {
		return addr;
	}
	
	public CommunicationTimestamps getTimestamps() {
		return ts;
	}
}

class CommunicationTimestamps implements Serializable {
	private Duration firstGap;
	private Duration secondGap;
	private Duration timeDiff;
	
	public static CommunicationTimestamps newEmpty() {
		return new CommunicationTimestamps();
	}
	
	public CommunicationTimestamps newWithNextGap(Duration secondGap) {
		CommunicationTimestamps ts = new CommunicationTimestamps();
		ts.firstGap = this.secondGap;
		ts.secondGap = secondGap;
		if (ts.firstGap != null) {
			Duration rtd = ts.firstGap.plus(ts.secondGap);
			Duration halvedRtd = Duration.ofNanos(rtd.toNanos() / 2);
			ts.timeDiff = ts.secondGap.negated().plus(halvedRtd);
		}
		
		return ts;
	}
	
	public Duration getTimeDiff() {
		return timeDiff;
	}
}
