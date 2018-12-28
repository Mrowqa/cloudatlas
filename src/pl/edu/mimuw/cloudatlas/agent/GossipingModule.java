/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.util.HashMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class GossipingModule extends Thread implements Module {
	private static final ZMI[] NO_ZMI = {};
	
	private final PathName name;
	private Duration resendInterval = Duration.ofSeconds(5);
	private LinkedBlockingQueue<GossipingMessage> messages = new LinkedBlockingQueue<>();
	private ModulesHandler handler;
	private GossipingNodeSelector selector;
	private int maxRetry;
	private Map<Long, GossipingState> states = new HashMap<>();
	private Random r = new Random();

	public GossipingModule(PathName name) {
		this.name = name;
	}
	
	@Override
	public boolean canHandleMessage(ModuleMessage message) {
		return message instanceof GossipingMessage;
	}

	@Override
	public void enqueue(ModuleMessage message) throws InterruptedException {
		messages.put((GossipingMessage) message);
	}

	@Override
	public void setModulesHandler(ModulesHandler handler) {
		this.handler = handler;
	}

	@Override
	public void run() {
		while(true) {
			try {
				GossipingMessage msg = messages.take();
				GossipingState state = states.getOrDefault(msg.pid, null);
				if (state == null) {
					state = GossipingState.fromFirstMessageOrNull(msg);
					if (state == null) {
						// Incorrect type of first message. Skipping.
						Logger.getLogger(ZMIModule.class.getName()).log(Level.FINE, 
								"Got message of type {0} with unknown process id {1}", 
								new Object[]{msg.type, msg.pid});
						continue;
					}
					states.put(msg.pid, state);
				}
				if (state.initializedByMe) {
					switch (msg.type) {
						case CALLBACK_START_GOSSIPING:
							startGossiping(state, msg); break;
						case LOCAL_ZMI_INFO:
							handleLocalZMIInfoToSendRequest(state, msg); break;
						case CALLBACK_RESEND_ZMI_INFO:
							sendInfo(state); break;
						case REMOTE_ZMI_INFO:
							continueWithZMIInfoResponse(state, msg); break;
						case CALLBACK_END_GOSSIPING:
							states.remove(msg.pid); break;
						default:
							Logger.getLogger(GossipingModule.class.getName()).log(Level.FINE, "Unexpected msg type");
							// TODO ignore callbacks or other irrelevant messages
					}
				} else {
					switch(msg.type) {
						case REMOTE_ZMI_INFO: 
							handleRequestZMIInfo(state, msg); break;
						case LOCAL_ZMI_INFO:
							handleLocalZMIInfoToSendResponse(state, msg); break;
						case CALLBACK_RESEND_ZMI_INFO:
							sendInfo(state); break;
						case ACK:
							handleAck(state, msg); break;
						default:
							throw new UnsupportedOperationException();
					}
				}
			}
			catch (InterruptedException ex) {
				Logger.getLogger(GossipingModule.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private void startGossiping(GossipingState state, GossipingMessage msg) throws InterruptedException {
		ZMIMessage nextMsg = ZMIMessage.getLocalZMIInfo(state.pid);
		state.lastMessageType = msg.type;
		handler.enqueue(nextMsg);
	}

	private void handleLocalZMIInfoToSendRequest(GossipingState state, GossipingMessage msg) throws InterruptedException {
		try {
			selectNodeForGossiping(state, msg.data);
		} catch (IllegalArgumentException ex) {
			Logger.getLogger(GossipingModule.class.getName()).log(Level.SEVERE, null, ex);
			states.remove(state.pid);
			scheduleNextGossiping();
		}
		prepareRemoteAgentDataMsg(state, msg.data);
		sendInfo(state);
	}

	private void sendInfo(GossipingState state) throws InterruptedException {
		if (state.retryCnt > maxRetry) {
			return;
		}
		state.retryCnt++;
		setupTimeoutCallback(state);
		handler.enqueue(state.msgToSend);
	}

	private void continueWithZMIInfoResponse(GossipingState state, GossipingMessage msg) throws InterruptedException {
		if (state.lastMessageType == GossipingMessage.Type.REMOTE_ZMI_INFO) {
			// We already received remote info but we got data once again.
			// Resend ACK.
			state.lastMessageType = GossipingMessage.Type.REMOTE_ZMI_INFO;
			sendInfo(state);
			return;
		}
		if (state.lastMessageType != GossipingMessage.Type.LOCAL_ZMI_INFO) {
			return;
		}
		state.lastMessageType = GossipingMessage.Type.REMOTE_ZMI_INFO;
		
		GossipingAgentData data = msg.data;
		Duration diff = msg.getCommunicationInfo().getTimestamps().getTimeDiff();
		data.adjustTime(diff);
		ZMIMessage updateMsg = ZMIMessage.updateWithRemoteData(state.pid, state.remoteData);
		handler.enqueue(updateMsg);
		
		prepareAckMsg(state);
		sendInfo(state);
	}

	private void handleRequestZMIInfo(GossipingState state, GossipingMessage msg) throws InterruptedException {
		state.lastMessageType = GossipingMessage.Type.LOCAL_ZMI_INFO;
		ZMIMessage zmiMsg = ZMIMessage.getLocalZMIInfo(state.pid);
		handler.enqueue(zmiMsg);
	}

	private void handleLocalZMIInfoToSendResponse(GossipingState state, GossipingMessage msg) throws InterruptedException {
		state.lastMessageType = GossipingMessage.Type.REMOTE_ZMI_INFO;
		prepareRemoteAgentDataMsg(state, msg.data);
		sendInfo(state);
	}
	
	private void prepareRemoteAgentDataMsg(GossipingState state, GossipingAgentData localData) {
		ZMI localZmi = localData.getZmis()[0];
		localData.setZmis(getSiblings(localZmi, state.remoteName));
		GossipingMessage payload = GossipingMessage.sendRemoteZMIInfo(state.pid, state.info, localData, name);
		CommunicationMessage msgToSend = CommunicationMessage.sendMessage(payload);
		
		state.msgToSend = msgToSend;
		state.retryCnt = 0;
	}
	
	private void prepareAckMsg(GossipingState state) {
		GossipingMessage payload = GossipingMessage.ack(state.pid, state.info);
		CommunicationMessage msgToSend = CommunicationMessage.sendMessage(payload);
		state.retryCnt = 0;
		state.msgToSend = msgToSend;
	}
	
	private void selectNodeForGossiping(GossipingState state, GossipingAgentData localData) {
		ZMI localZmi = localData.getZmis()[0];
		ValueContact contact = selector.selectNode(localZmi, (ValueSet) localData.getFallbackContacts().getVal());
		if (contact == null) {
			throw new IllegalArgumentException("");
		}
		state.remoteName = contact.getName();
		state.info = new CommunicationInfo(contact.getAddress());;
	}

	private void handleAck(GossipingState state, GossipingMessage msg) throws InterruptedException {
		Duration timeDiff = msg.getCommunicationInfo().getTimestamps().getTimeDiff();
		state.remoteData.adjustTime(timeDiff);
		
		ZMIMessage updateMsg = ZMIMessage.updateWithRemoteData(state.pid, state.remoteData);
		handler.enqueue(updateMsg);
		states.remove(state.pid);
	}

	private void scheduleNextGossiping() throws InterruptedException {
		long pid = r.nextLong();
		GossipingMessage innerMsg = GossipingMessage.callbackStartGossiping(pid);
		TimerMessage msg = TimerMessage.scheduleOneTimeCallback(pid, resendInterval, innerMsg);
		handler.enqueue(msg);
	}

	private ZMI[] getSiblings(ZMI zmi, PathName name) {
		if (name.getComponents().isEmpty()) {
			return NO_ZMI;
		}
		String singletonName = name.getSingletonName();
		name = name.levelUp();

		ZMI parrentZmi = ZMIModule.findZone(zmi, name);
		if (parrentZmi == null) {
			return NO_ZMI;
		}
		
		ArrayList result = new ArrayList();
		for (ZMI child : parrentZmi.getSons()) {
			if (!child.getPathName().getName().equals(singletonName)) {
				result.add(child.clone());
			}
		}
		return (ZMI[]) result.toArray();
	}
	
	private void setupTimeoutCallback(GossipingState state) throws InterruptedException {
		GossipingMessage innerMsg = null;
		if (state.lastMessageType == GossipingMessage.Type.LOCAL_ZMI_INFO) {
			innerMsg = GossipingMessage.callbackResendZMIInfo(state.pid);
		} else { // GossipingMessage.Type.REMOTE_ZMI_INFO
			innerMsg = GossipingMessage.callbackEndGossiping(state.pid);
		}
		TimerMessage msg = TimerMessage.scheduleOneTimeCallback(state.pid, resendInterval, innerMsg);
		handler.enqueue(msg);
	}
}

class GossipingState {
	public boolean initializedByMe;
	public long pid;
	public GossipingMessage.Type lastMessageType;
	public CommunicationMessage msgToSend;
	public int retryCnt;

	public GossipingAgentData remoteData;
	public CommunicationInfo info;
	public PathName remoteName;
	
	private GossipingState(boolean initializedByMe, long pid) {
		this.initializedByMe = initializedByMe;
	}
	
	public static GossipingState fromFirstMessageOrNull(GossipingMessage msg) {
		if (msg.type == GossipingMessage.Type.CALLBACK_START_GOSSIPING) {
			return new GossipingState(true, msg.pid);
		}
		if (msg.type == GossipingMessage.Type.REMOTE_ZMI_INFO) {
			GossipingState state = new GossipingState(false, msg.pid);
			state.info = msg.getCommunicationInfo();
			state.remoteName = msg.pathName;
			state.remoteData = msg.data;
			return state;
		}
		return null;
	}
}
