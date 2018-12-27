/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pawel
 */
public class GossipingModule extends Thread implements Module {
	private LinkedBlockingQueue<GossipingMessage> messages = new LinkedBlockingQueue<>();
	private ModulesHandler handler;
	private GossipingNodeSelector selector;
	private int maxRetry;
	private Map<Long, GossipingState> states = new HashMap<>();
	private Random r = new Random();
	
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
							sendInfo(state, msg); break;
						case REMOTE_ZMI_INFO:
							continueWithZMIInfoResponse(state, msg); break;
						case CALLBACK_END_GOSSIPING:
							endGossiping(msg); break;
						default:
							// TODO ignore callbacks or other irrelevant messages
							throw new UnsupportedOperationException();
					}
				} else {
					switch(msg.type) {
						case REMOTE_ZMI_INFO: 
							handleRequestZMIInfo(state, msg); break;
						case LOCAL_ZMI_INFO:
							handleLocalZMIInfoToSendResponse(state, msg); break;
						case CALLBACK_RESEND_ZMI_INFO:
							sendInfo(state, msg); break;
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
		GossipingMessage nextMsg = null; // Get local ZMI info
		state.lastMessageType = msg.type;
		messages.put(nextMsg);
	}

	private void handleLocalZMIInfoToSendRequest(GossipingState state, GossipingMessage msg) {
		// Choose ZMI
		// Select relevant info
		// Construct request
		// Send request
		// Setup callback
	}

	private void sendInfo(GossipingState state, GossipingMessage msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void continueWithZMIInfoResponse(GossipingState state, GossipingMessage msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void endGossiping(GossipingMessage msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void handleRequestZMIInfo(GossipingState state, GossipingMessage msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void handleLocalZMIInfoToSendResponse(GossipingState state, GossipingMessage msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void handleAck(GossipingState state, GossipingMessage msg) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}

class GossipingState {
	public boolean initializedByMe;
	public GossipingMessage.Type lastMessageType;
	public GossipingMessage msgToSend;
	public int tries;
	public CommunicationInfo info;
	
	private GossipingState(boolean initializedByMe) {
		this.initializedByMe = initializedByMe;
	}
	
	public static GossipingState fromFirstMessageOrNull(GossipingMessage msg) {
		if (msg.type == GossipingMessage.Type.CALLBACK_START_GOSSIPING) {
			return new GossipingState(true);
		}
		if (msg.type == GossipingMessage.Type.REMOTE_ZMI_INFO) {
			return new GossipingState(false);
		}
		return null;
	}
}
