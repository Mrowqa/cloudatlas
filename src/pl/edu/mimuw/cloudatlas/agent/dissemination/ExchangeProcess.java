/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.time.Duration;
import java.util.Random;
import pl.edu.mimuw.cloudatlas.agent.CommunicationInfo;
import pl.edu.mimuw.cloudatlas.agent.CommunicationMessage;
import pl.edu.mimuw.cloudatlas.agent.TimerMessage;
import pl.edu.mimuw.cloudatlas.agent.ZMIMessage;
import pl.edu.mimuw.cloudatlas.agent.ZMIModule;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class ExchangeProcess {
	enum State {
		INITIALIZED,
		WAITING_FOR_LOCAL_DATA,
		WAITING_FOR_REMOTE_DATA,
		WAITING_FOR_SHUTDOWN,
		FINISHED
	}
	private static final Random r = new Random();
	private final ExchangeProcesConfig config;
	private boolean initializedByMe;
	private State state;
	private long pid;

	public CommunicationMessage msgToSend;
	public int retryCnt;

	public AgentData remoteData;
	public CommunicationInfo communicationInfo;
	public PathName remoteName;

	private ExchangeProcess(boolean initializedByMe, long pid, ExchangeProcesConfig config) {
		this.initializedByMe = initializedByMe;
		this.pid = pid;
		this.config = config;
		this.state = State.INITIALIZED;
	}

	private ExchangeProcess(boolean initializedByMe, long pid, ExchangeProcesConfig config, DisseminationMessage msg) {
		this(initializedByMe, pid, config);
		this.communicationInfo = msg.getCommunicationInfo();
		this.remoteName = msg.pathName;
		this.remoteData = msg.data;
	}

	public static ExchangeProcess fromFirstMessage(DisseminationMessage msg, ExchangeProcesConfig config) {
		switch (msg.type) {
			case CALLBACK_START_NEW_EXCHANGE:
				// Since this is cyclic callback msg we need to generate new pid.
				return new ExchangeProcess(true, r.nextLong(), config);
			case REMOTE_AGENT_DATA:
				return new ExchangeProcess(false, msg.pid, config, msg);
			default:
				return null;
		}
	}

	public long getPid() {
		return pid;
	}
	
	public void handleMsg(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		try {
			if (initializedByMe) {
				switch (state) {
					case INITIALIZED:
						switch (msg.type) {
							case CALLBACK_START_NEW_EXCHANGE:
								requestLocalData();
								break;
						}
						break;
					case WAITING_FOR_LOCAL_DATA:
						switch (msg.type) {
							case LOCAL_AGENT_DATA:
								handleLocalDataAndSendRequest(msg);
								break;
						}
						break;
					case WAITING_FOR_REMOTE_DATA:
						switch (msg.type) {
							case REMOTE_AGENT_DATA:
								handleRemoteDataResponseAndSendAck(msg);
								break;
							case CALLBACK_RESEND_DATA:
								sendDataToRemote();
								break;
						}
						break;
					case WAITING_FOR_SHUTDOWN:
						switch (msg.type) {
							case REMOTE_AGENT_DATA:
								sendDataToRemote();
								break;
							case CALLBACK_SHUTDOWN_EXCHANGE:
								state = State.FINISHED;
								break;
						}
						break;
				}
			} else {
				switch (state) {
					case INITIALIZED:
						switch (msg.type) {
							case REMOTE_AGENT_DATA:
								requestLocalData();
								break;
						}
						break;
					case WAITING_FOR_LOCAL_DATA:
						switch (msg.type) {
							case LOCAL_AGENT_DATA:
								handleLocalDataAndSendResponse(msg);
								break;
						}
						break;
					case WAITING_FOR_REMOTE_DATA:
						switch (msg.type) {
							case REMOTE_AGENT_DATA:
							case CALLBACK_RESEND_DATA:
								sendDataToRemote();
								break;
							case ACK_RECEIVED_REMOTE_DATA:
								handleAck(msg);
								break;
						}
						break;
					default:
				}
			}
		} catch (Exception ex) {
			state = State.FINISHED;
			throw ex;
		}
	}

	private void requestLocalData() throws InterruptedException {
		state = State.WAITING_FOR_LOCAL_DATA;
		config.handler.enqueue(ZMIMessage.getLocalAgentData(pid));
	}

	private void handleLocalDataAndSendRequest(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		state = State.WAITING_FOR_REMOTE_DATA;
		selectNodeForExchange(msg.data);
		prepareRemoteDataMsg(msg.data);
		sendDataToRemote();
	}

	private void sendDataToRemote() throws InterruptedException, TooManyTriesException {
		if (retryCnt >= config.maxRetry) {
			throw new TooManyTriesException("Resent message to the remote host " + remoteName + " maximum number of times: " + retryCnt + " and did not receive response.");
		}
		retryCnt++;
		setupTimeoutCallback();
		config.handler.enqueue(msgToSend);
	}

	private void handleRemoteDataResponseAndSendAck(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		state = State.WAITING_FOR_SHUTDOWN;
		remoteData = msg.data;
		assert remoteData != null;
		communicationInfo = msg.getCommunicationInfo();
		remoteData.adjustTime(msg.getCommunicationInfo().getTimeDiff());
		
		ZMIMessage updateMsg = ZMIMessage.updateWithRemoteData(pid, remoteData);
		remoteData = null; // To have shared nothing architecture we do not use remoteData after sending to other module.
		config.handler.enqueue(updateMsg);
		
		prepareAckMsg();
		sendDataToRemote();
	}

	private void handleRemoteDataRequest(DisseminationMessage msg) throws InterruptedException {
		state = State.WAITING_FOR_LOCAL_DATA;
		config.handler.enqueue(ZMIMessage.getLocalAgentData(pid));
	}

	private void handleLocalDataAndSendResponse(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		state = State.WAITING_FOR_REMOTE_DATA;
		prepareRemoteDataMsg(msg.data);
		sendDataToRemote();
	}

	private void prepareRemoteDataMsg(AgentData localData) {
		// Once we prepared data we won't modify it. Sending to other module is correct in terms of shared-nothing architecture.
		ZMIModule.removeInfoUnrelevantForTheOther(localData.getZmi(), config.name, remoteName);
		DisseminationMessage payload = DisseminationMessage.remoteAgentData(pid, communicationInfo, localData, config.name);
		msgToSend = CommunicationMessage.sendMessage(payload);
		retryCnt = 0;
	}

	private void prepareAckMsg() {
		DisseminationMessage payload = DisseminationMessage.ack(pid, communicationInfo);
		msgToSend = CommunicationMessage.sendMessage(payload);
		retryCnt = 0;
	}

	private void selectNodeForExchange(AgentData localData) {
		ZMI localZmi = localData.getZmi();
		ValueContact contact = config.selector.selectNode(localZmi, (ValueSet) localData.getFallbackContacts().getValue());
		if (contact == null) {
			throw new IllegalArgumentException("There is no node to exchange information with.");
		}
		remoteName = contact.getName();
		communicationInfo = new CommunicationInfo(contact.getAddress(), contact.getPort());
	}

	private void handleAck(DisseminationMessage msg) throws InterruptedException {
		state = State.FINISHED;
		Duration timeDiff = msg.getCommunicationInfo().getTimeDiff();
		remoteData.adjustTime(timeDiff);

		ZMIMessage updateMsg = ZMIMessage.updateWithRemoteData(pid, remoteData);
		remoteData = null; // To have shared nothing architecture we do not use remoteData after sending to other module.
		config.handler.enqueue(updateMsg);
	}

	private void setupTimeoutCallback() throws InterruptedException {
		DisseminationMessage innerMsg = null;
		switch (state) {
			case WAITING_FOR_REMOTE_DATA:
				innerMsg = DisseminationMessage.callbackResendData(pid);
				break;
			case WAITING_FOR_SHUTDOWN:
				innerMsg = DisseminationMessage.callbackShutdownExchange(pid);
				break;
			default:
				throw new IllegalArgumentException("Callback setup is not supported in " + state + " state");
		}
		TimerMessage msg = TimerMessage.scheduleOneTimeCallback(pid, config.resendInterval, innerMsg);
		config.handler.enqueue(msg);
	}

	public boolean isFinished() {
		return state == State.FINISHED;
	}

	boolean isInitializedByMe() {
		return initializedByMe;
	}
}
