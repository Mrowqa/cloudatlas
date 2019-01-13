/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;

/**
 *
 * @author pawel
 */
public class CommunicationInfo implements Serializable {
	private SocketAddress addr;
	private CommunicationTimestamps ts;
	
	public CommunicationInfo(SocketAddress addr) {
		this(addr, CommunicationTimestamps.newEmpty());
	}
	
	public CommunicationInfo(InetAddress addr, int port) {
		this(new InetSocketAddress(addr, port));
	}
	
	public CommunicationInfo(SocketAddress addr, CommunicationTimestamps ts) {
		this.addr = addr;
		this.ts = ts;
	}
	
	public SocketAddress getAddress() {
		return addr;
	}
	
	public Duration getTimeDiff() {
		return ts.getTimeDiff();
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

