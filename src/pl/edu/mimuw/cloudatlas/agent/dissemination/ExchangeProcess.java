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
// TODO check data flow, whether we are seting communication info correct
// TODO examine in which moment you switch states
// TODO callback resend fix
// TODO czy my swapujemy communication info dla sendera i reveivera?
public class ExchangeProcess {
	enum State {
		INITIALIZED,
		WAITING_FOR_LOCAL_DATA,
		WAITING_FOR_REMOTE_DATA,
		WAITING_FOR_SHUTDOWN,
		FINISHED
	}
	private final ExchangeProcesConfig config;
	private Random r = new Random();
	private boolean initializedByMe;
	private State state;
	private long pid;

	public CommunicationMessage msgToSend;
	public int retryCnt;

	public AgentData remoteData;
	public CommunicationInfo info;
	public PathName remoteName;

	private ExchangeProcess(boolean initializedByMe, long pid, ExchangeProcesConfig config) {
		this.initializedByMe = initializedByMe;
		this.pid = pid;
		this.config = config;
		this.state = State.INITIALIZED;
	}

	private ExchangeProcess(boolean initializedByMe, long pid, ExchangeProcesConfig config, DisseminationMessage msg) {
		this(initializedByMe, pid, config);
		this.info = msg.getCommunicationInfo();
		System.out.println("Creating process, communication info: " + info.getAddress());
		this.remoteName = msg.pathName;
		this.remoteData = msg.data;
	}

	public static ExchangeProcess fromFirstMessage(DisseminationMessage msg, ExchangeProcesConfig config) {
		switch (msg.type) {
			case CALLBACK_START_NEW_EXCHANGE:
				return new ExchangeProcess(true, msg.pid, config);
			case REMOTE_AGENT_DATA:
				return new ExchangeProcess(false, msg.pid, config, msg);
			default:
				return null;
		}
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
								handleLocalDataToSendRequest(msg);
								break;
						}
						break;
					case WAITING_FOR_REMOTE_DATA:
						switch (msg.type) {
							case REMOTE_AGENT_DATA:
								handleRemoteDataResponse(msg);
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
								handleLocalDataToSendResponse(msg);
								break;
						}
						break;
					case WAITING_FOR_REMOTE_DATA:
						switch (msg.type) {
							case REMOTE_AGENT_DATA:
							case CALLBACK_RESEND_DATA:
								sendDataToRemote();
								break;
							case ACK:
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
		config.handler.enqueue(ZMIMessage.getLocalZMIInfo(pid));
	}

	private void handleLocalDataToSendRequest(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		state = State.WAITING_FOR_REMOTE_DATA;
		selectNodeForExchange(msg.data);
		System.out.println("Selected node " + remoteName);
		prepareRemoteDataMsg(msg.data);
		sendDataToRemote();
	}

	private void sendDataToRemote() throws InterruptedException, TooManyTriesException {
		if (retryCnt > config.maxRetry) {
			throw new TooManyTriesException("We resend message to the remote host maximum number of times: " + retryCnt);
		}
		retryCnt++;
		setupTimeoutCallback();
		config.handler.enqueue(msgToSend);
	}

	private void handleRemoteDataResponse(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		state = State.WAITING_FOR_SHUTDOWN;
		remoteData = msg.data;
		assert remoteData != null;
		info = msg.getCommunicationInfo();
		remoteData.adjustTime(msg.getCommunicationInfo().getTimeDiff());
		config.handler.enqueue(ZMIMessage.updateWithRemoteData(pid, remoteData));

		prepareAckMsg();
		sendDataToRemote();
	}

	private void handleRemoteDataRequest(DisseminationMessage msg) throws InterruptedException {
		state = State.WAITING_FOR_LOCAL_DATA;
		config.handler.enqueue(ZMIMessage.getLocalZMIInfo(pid));
	}

	private void handleLocalDataToSendResponse(DisseminationMessage msg) throws InterruptedException, TooManyTriesException {
		state = State.WAITING_FOR_REMOTE_DATA;
		prepareRemoteDataMsg(msg.data);
		sendDataToRemote();
	}

	private void prepareRemoteDataMsg(AgentData localData) {
		ZMI localZmi = localData.getZmis()[0];
		localData.setZmis(ZMIModule.getSiblings(localZmi, remoteName));
		DisseminationMessage payload = DisseminationMessage.remoteAgentData(pid, info, localData, config.name);
		msgToSend = CommunicationMessage.sendMessage(payload);
		retryCnt = 0;
	}

	private void prepareAckMsg() {
		DisseminationMessage payload = DisseminationMessage.ack(pid, info);
		msgToSend = CommunicationMessage.sendMessage(payload);
		retryCnt = 0;
	}

	private void selectNodeForExchange(AgentData localData) {
		ZMI localZmi = localData.getZmis()[0];
		ValueContact contact = config.selector.selectNode(localZmi, (ValueSet) localData.getFallbackContacts().getVal());
		if (contact == null) {
			throw new IllegalArgumentException("There is no node to exchange information with.");
		}
		remoteName = contact.getName();
		info = new CommunicationInfo(contact.getAddress(), contact.getPort());
	}

	private void handleAck(DisseminationMessage msg) throws InterruptedException {
		state = State.FINISHED;
		Duration timeDiff = msg.getCommunicationInfo().getTimeDiff();
		remoteData.adjustTime(timeDiff);

		ZMIMessage updateMsg = ZMIMessage.updateWithRemoteData(pid, remoteData);
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
