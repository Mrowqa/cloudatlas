/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrowqa
 */
public class CommunicationModule extends Thread {
	public final static int SOCKET_PORT = 42777;
	public final static Duration SOCKET_RECOVERY_INTERVAL = Duration.ofMinutes(5);
	
	private final LinkedBlockingQueue<CommunicationMessage> messages;
	private ModulesHandler modulesHandler;
	private DatagramChannel networkChannel;
	private ModuleMessageFragmentationHandler messageFragmentationHandler;
	
	public CommunicationModule() {
		this.messages = new LinkedBlockingQueue<>();
		this.messageFragmentationHandler = new ModuleMessageFragmentationHandler();
	}

	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
		messageFragmentationHandler.setModulesHandler(modulesHandler);
	}
	
	public void enqueue(CommunicationMessage message) throws InterruptedException {
		messages.put(message);
	}
	
	@Override
	public void run() {
		CommunicationModule ThisModule = this;
		new Thread() {
			public void run() {
				ThisModule.messageHandlerThread();
			}
		}.start();
		new Thread() {
			public void run() {
				ThisModule.networkReceiverThread();
			}
		}.start();
		
		while (true) {
			if (checkSocketFailure()) {
				recoverFromSocketFailure();
			}
			
			try {
				Thread.sleep(SOCKET_RECOVERY_INTERVAL.toMillis());
			}
			catch (InterruptedException ex) {}
		}
	}
	
	private boolean checkSocketFailure() {
		DatagramSocket socket = networkChannel.socket();
		return socket == null || !socket.isBound() || socket.isClosed();
	}
	
	private void recoverFromSocketFailure() {
		try {
			networkChannel.close();
		} catch (IOException ex) {}
		
		try {
			networkChannel = DatagramChannel.open();
			networkChannel.configureBlocking(true);
			networkChannel.bind(new InetSocketAddress(SOCKET_PORT));
		}
		catch (IOException ex) {
			Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
		}
	}
	
	private void messageHandlerThread() {
		while (true) {
			try {
				CommunicationMessage msg = messages.take();
				// make fragments
				// update timestamps
				// send them
				// TODO
			}
			catch (InterruptedException ex) {
				Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
			}
		}
	}
	
	private void networkReceiverThread() {
		// TODO
	}
}

class ModuleMessageFragmentationHandler {
	public static final Duration MESSAGE_FRAGMENTS_TIMEOUT = Duration.ofSeconds(30);
	private final HashMap<Long, PartiallyConstructedModuleMessage> fragments = new HashMap<>();
	private ModulesHandler modulesHandler;
	
	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
	}
	
	public void addMessageFragment(ModuleMessageFragment msgFrag) { // todo received time
		long id = msgFrag.getId();
		PartiallyConstructedModuleMessage partMsg = fragments.get(id);
		if (partMsg == null) {
			partMsg = new PartiallyConstructedModuleMessage(msgFrag);
			fragments.put(id, partMsg);
		}
		
		partMsg.addNewFragment(msgFrag);
		if (partMsg.isComplete()) {
			try {
				modulesHandler.enqueue(partMsg.reassembleModuleMessage());
				fragments.remove(id);
				
				ModuleMessage msg = TimerMessage.cancelCallback(id);
				modulesHandler.enqueue(msg);
			}
			catch (InterruptedException ex) {
				Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
			}
		}
		else {
			ModuleMessage callbackMessage = CommunicationMessage.messageReceiveTimedOut(id);
			ModuleMessage msg = TimerMessage.scheduleOneTimeCallback(id, MESSAGE_FRAGMENTS_TIMEOUT, callbackMessage);
			
			try {
				modulesHandler.enqueue(msg);
			}
			catch (InterruptedException ex) {
				Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
			}
		}
	}
	
	public void removeTimedOutMessage(long id) {
		fragments.remove(id);
	}
	
	public static ModuleMessageFragment[] fragmentMessage(ModuleMessage msg) {
		// todo
		return null;
	}
}


class PartiallyConstructedModuleMessage {
	// todo received time
	private ModuleMessageFragment[] fragments;
	private long id;
	private int remainingFragments;
	
	public PartiallyConstructedModuleMessage(ModuleMessageFragment frag) {
		int fragsCnt = frag.getFragmentsCount();
		fragments = new ModuleMessageFragment[fragsCnt];
		fragments[frag.getFragmentNumber()] = frag;
		remainingFragments = fragsCnt - 1;
	}
	
	public void addNewFragment(ModuleMessageFragment frag) {
		if (frag.getId() != id || frag.getFragmentsCount() != fragments.length) {
			throw new IllegalArgumentException("Received malformed message fragment"); // todo handle it
		}
		
		int fragNum = frag.getFragmentNumber();
		if (fragments[fragNum] == null) {
			fragments[fragNum] = frag;
			remainingFragments--;
		}
	}
	
	public boolean isComplete() {
		return remainingFragments == 0;
	}
	
	public ModuleMessage reassembleModuleMessage() {
		return null; // todo
		// todo remember about CommunicationTimestamps
	}
}


class ModuleMessageFragment {
	public static final int MY_HEADER_SIZE = 24;
	public static final int USER_DATA_SIZE_LIMIT =
			576   // minimum guaranteed MTU in the internet
			- 40  // max(IPv4 header, IPv6 header)
			- 8   // UDP header
			- MY_HEADER_SIZE;
	
	private static final int ID_OFFSET = 0;
	private static final int SENT_TIME_OFFSET = 8;
	private static final int FRAG_NUMBER_OFFSET = 16;
	private static final int FRAGS_CNT_OFFSET = 20;
	private static final int USER_DATA_OFFSET = 24;
	
	private ByteBuffer data;
	
	public ModuleMessageFragment(byte[] binaryPacket) {
		if (binaryPacket.length <= MY_HEADER_SIZE || binaryPacket.length > MY_HEADER_SIZE + USER_DATA_SIZE_LIMIT) {
			throw new IllegalArgumentException("Received malformed packet"); // todo catch it!
		}
		data = ByteBuffer.allocate(binaryPacket.length);
		data.put(binaryPacket);
		
		int fragNumber = getFragmentNumber();
		int fragsCnt = getFragmentsCount();
		if (fragsCnt < 0 || fragNumber >= fragsCnt) {
			throw new IllegalArgumentException("Received malformed packet");
		}
	}

	public ModuleMessageFragment(long id, int fragNumber, int fragsCnt, byte [] userData) {
		if (userData == null || userData.length == 0 || userData.length > USER_DATA_SIZE_LIMIT) {
			throw new IllegalArgumentException("User data is null or empty or its size exceeded the limit");
		}
		if (fragsCnt < 0 || fragNumber >= fragsCnt) {
			throw new IllegalArgumentException("FragNumber must be smaller than FragsCnt, and both can't be negative");
		}
		
		data = ByteBuffer.allocate(MY_HEADER_SIZE + userData.length);
		data.putLong(id);
		data.putLong(0);  // sent time
		data.putInt(fragNumber);
		data.putInt(fragsCnt);
		data.put(userData);
	}
	
	public void setSentTimeToNow() {
		data.putLong(SENT_TIME_OFFSET, Instant.now().toEpochMilli());
	}
	
	public long getId() {
		return data.getLong(ID_OFFSET);
	}
	
	public Instant getSentTime() {
		return Instant.ofEpochMilli(data.getLong(SENT_TIME_OFFSET));
	}
	
	public int getFragmentNumber() {
		return data.getInt(FRAG_NUMBER_OFFSET);
	}
	
	public int getFragmentsCount() {
		return data.getInt(FRAGS_CNT_OFFSET);
	}
	
	public byte [] getUserData() {
		data.position(USER_DATA_OFFSET);
		byte[] userData = new byte[data.remaining()];
		data.get(userData);
		return userData;
	}
	
	public ByteBuffer getEncodedUserData() {
		return data;
	}
}
