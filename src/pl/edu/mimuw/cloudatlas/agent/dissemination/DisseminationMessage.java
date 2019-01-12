/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.io.Serializable;
import java.util.HashMap;
import pl.edu.mimuw.cloudatlas.agent.CommunicationInfo;
import pl.edu.mimuw.cloudatlas.agent.ModuleMessage;
import pl.edu.mimuw.cloudatlas.agent.NetworkSendable;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.Query;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class DisseminationMessage extends ModuleMessage implements NetworkSendable, Serializable {
	public enum Type {
		CALLBACK_START_NEW_EXCHANGE, LOCAL_AGENT_DATA, 
		REMOTE_AGENT_DATA, CALLBACK_RESEND_DATA,
		ACK_RECEIVED_REMOTE_DATA, CALLBACK_SHUTDOWN_EXCHANGE
	}
	private CommunicationInfo info;
	public Type type;
	public long pid;
	
	public PathName pathName;
	public AgentData data;
	
	private DisseminationMessage(Type type, long msgId) {
		this.type = type;
		this.pid = msgId;
	}
	
	public static DisseminationMessage callbackStartNewExchange(long pid) {
		return new DisseminationMessage(Type.CALLBACK_START_NEW_EXCHANGE, pid);
	}
	
	public static DisseminationMessage localAgentData(long msgId, ZMI zmi, 
			ValueAndFreshness fallbackContacts, 
			HashMap<Attribute, Query> queries) {
		DisseminationMessage msg = new DisseminationMessage(Type.LOCAL_AGENT_DATA, msgId);
		msg.data = new AgentData(zmi, fallbackContacts, queries);
		return msg;
	}
	
	public static DisseminationMessage remoteAgentData(long msgId, 
			CommunicationInfo info,
			AgentData data,
			PathName senderPathName) {
		DisseminationMessage msg = new DisseminationMessage(Type.REMOTE_AGENT_DATA, msgId);
		msg.info = info;
		msg.data = data;
		msg.pathName = senderPathName;
		return msg;
	}
	
	public static DisseminationMessage callbackResendData(long msgId) {
		return new DisseminationMessage(Type.CALLBACK_RESEND_DATA, msgId);
	}
	
	public static DisseminationMessage ack(long msgId, CommunicationInfo info) {
		DisseminationMessage msg = new DisseminationMessage(Type.ACK_RECEIVED_REMOTE_DATA, msgId);
		msg.info = info;
		return msg;
	}
	
	public static DisseminationMessage callbackShutdownExchange(long msgId) {
		return new DisseminationMessage(Type.CALLBACK_SHUTDOWN_EXCHANGE, msgId);
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
