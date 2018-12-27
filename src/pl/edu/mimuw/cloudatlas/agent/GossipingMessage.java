/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.time.Instant;
import java.util.Map;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class GossipingMessage extends ModuleMessage implements NetworkSendable {
	public enum Type {
		CALLBACK_START_GOSSIPING, LOCAL_ZMI_INFO, 
		REMOTE_ZMI_INFO, CALLBACK_RESEND_ZMI_INFO,
		ACK, CALLBACK_END_GOSSIPING
	}
	private CommunicationInfo info;
	public Type type;
	public long pid;
	
	public ZMI[] zmi;
	public ValueAndFreshness fallbackContacts;
	public Map<Attribute, ValueAndFreshness> queries;
	public PathName senderPathName;
	
	private GossipingMessage(Type type, long msgId) {
		this.type = type;
		this.pid = msgId;
	}
	
	public GossipingMessage callbackStartGossiping(long msgId) {
		GossipingMessage msg = new GossipingMessage(Type.CALLBACK_START_GOSSIPING, msgId);
		return msg;
	}
	
	public GossipingMessage localZMIInfo(long msgId, ZMI zmi, 
			ValueAndFreshness fallbackContacts, 
			Map<Attribute, ValueAndFreshness> queries) {
		GossipingMessage msg = new GossipingMessage(Type.LOCAL_ZMI_INFO, msgId);
		this.zmi = new ZMI[1];
		this.zmi[0] = zmi;
		this.fallbackContacts = fallbackContacts;
		this.queries = queries;
		return msg;
	}
	
	public GossipingMessage sendRemoteZMIInfo(long msgId, ZMI[] zmi, 
			CommunicationInfo info,
			ValueAndFreshness fallbackContacts, 
			Map<Attribute, ValueAndFreshness> queries,
			PathName senderPathName) {
		GossipingMessage msg = new GossipingMessage(Type.REMOTE_ZMI_INFO, msgId);
		this.info = info;
		this.zmi = zmi;
		this.fallbackContacts = fallbackContacts;
		this.queries = queries;
		this.senderPathName = senderPathName;
		return msg;
	}
	
	public GossipingMessage callbackResendZMIInfo(long msgId) {
		return new GossipingMessage(Type.CALLBACK_RESEND_ZMI_INFO, msgId);
	}
	
	public GossipingMessage ack(long msgId, CommunicationInfo info) {
		GossipingMessage msg = new GossipingMessage(Type.ACK, msgId);
		this.info = info;
		return msg;
	}
	
	public GossipingMessage callbackEndGossiping(long msgId) {
		return new GossipingMessage(Type.CALLBACK_END_GOSSIPING, msgId);
	}
	
	@Override
	public CommunicationInfo getCommunicationInfo() {
		return info;
	}

	@Override
	public void setCommunicationInfo(CommunicationInfo info) {
		this.info = info;
	}
	
}
