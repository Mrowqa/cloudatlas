/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Random;
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
			@Override
			public void run() {
				ThisModule.messageHandlerThread();
			}
		}.start();
		new Thread() {
			@Override
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

				if (msg.type == CommunicationMessage.Type.SEND_MESSAGE) {
					sendMessage(msg.msg);
				}
				else { // timed out
					messageFragmentationHandler.removeTimedOutMessage(msg.msgId);
				}
			}
			catch (Exception ex) {
				Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
			}
		}
	}

	private void sendMessage(ModuleMessage msg) {
		try {
			SocketAddress addr = ((NetworkSendable) msg).getCommunicationInfo().getAddress();
			ModuleMessageFragment[] fragments = ModuleMessageFragmentationHandler.fragmentMessage(msg);  // can throw IllegalArgumentException
			for (ModuleMessageFragment frag : fragments) {
				frag.updateSentTimeToNow();
				ByteBuffer data = frag.getEncodedFragment();
				data.position(0);
				int bytesTotal = data.remaining();
				int bytesSent = networkChannel.send(data, addr);
				if (data.remaining() != 0) {
					throw new IOException("Sent only " + bytesSent + " of " + bytesTotal + " total bytes for fragment.");
				}
			}
		}
		catch (Exception ex) {
			Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
		}
	}

	private void networkReceiverThread() {
		ByteBuffer buffer = ByteBuffer.allocate(ModuleMessageFragment.MY_HEADER_SIZE + ModuleMessageFragment.USER_DATA_SIZE_LIMIT);
		while (true) {
			try {
				buffer.position(0);
				SocketAddress addr = networkChannel.receive(buffer);
				Instant recvTime = Instant.now();
				byte[] binaryPacket = new byte[buffer.position()];
				buffer.position(0);
				buffer.get(binaryPacket);
				ModuleMessageFragment msgFrag = new ModuleMessageFragment(binaryPacket);
				messageFragmentationHandler.addMessageFragment(addr, msgFrag, recvTime);
			}
			catch (Exception ex) {
				Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
			}
		}
	}
}

class ModuleMessageFragmentationHandler {
	public static final Duration MESSAGE_FRAGMENTS_TIMEOUT = Duration.ofSeconds(30);
	private static final Random random = new Random();
	private final HashMap<Long, PartiallyConstructedModuleMessage> fragments = new HashMap<>();
	private ModulesHandler modulesHandler;
	
	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
	}
	
	public void addMessageFragment(SocketAddress sender, ModuleMessageFragment msgFrag, Instant recvTime) {
		try {
			long id = msgFrag.getId();
			PartiallyConstructedModuleMessage partMsg = fragments.get(id);
			if (partMsg == null) {
				partMsg = new PartiallyConstructedModuleMessage(sender, msgFrag, recvTime);
				fragments.put(id, partMsg);
			}
			else {
				partMsg.addNewFragment(sender, msgFrag, recvTime);  // can throw IllegalArgumentException
			}

			if (partMsg.isComplete()) {
				modulesHandler.enqueue(partMsg.reassembleModuleMessage()); // can also throw NullPointerException in case of invalid packet
				fragments.remove(id);

				ModuleMessage msg = TimerMessage.cancelCallback(id);
				modulesHandler.enqueue(msg);
			}
			else {
				ModuleMessage callbackMessage = CommunicationMessage.messageReceiveTimedOut(id);
				ModuleMessage msg = TimerMessage.scheduleOneTimeCallback(id, MESSAGE_FRAGMENTS_TIMEOUT, callbackMessage);

				modulesHandler.enqueue(msg);
			}
		}
		catch (Exception ex) {
			Logger.getLogger(CommunicationModule.class.getName()).log(Level.SEVERE, ex.getMessage());
		}
	}
	
	public void removeTimedOutMessage(long id) {
		fragments.remove(id);
	}
	
	public static ModuleMessageFragment[] fragmentMessage(ModuleMessage msg) throws IOException {
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		ObjectOutputStream out2 = new ObjectOutputStream(out1);
		out2.writeObject(msg);
		out2.close();
		out1.close();
		ByteBuffer buffer = ByteBuffer.wrap(out1.toByteArray());

		int fullFrags = buffer.remaining() / ModuleMessageFragment.USER_DATA_SIZE_LIMIT;
		int lastFragLen = buffer.remaining() % ModuleMessageFragment.USER_DATA_SIZE_LIMIT;
		long id = random.nextLong();
		byte[] binaryBuffer = new byte[ModuleMessageFragment.USER_DATA_SIZE_LIMIT];
		int fragsCnt = fullFrags + (lastFragLen > 0 ? 1 : 0);
		ModuleMessageFragment[] frags = new ModuleMessageFragment[fragsCnt];

		for (int i = 0; i < fullFrags; i++) {
			buffer.get(binaryBuffer);
			frags[i] = new ModuleMessageFragment(id, i, fragsCnt, binaryBuffer);
		}

		if (lastFragLen > 0) {
			byte[] binaryBufferLast = new byte[lastFragLen];
			buffer.get(binaryBufferLast);
			frags[fullFrags] = new ModuleMessageFragment(id, fullFrags, fragsCnt, binaryBufferLast);
		}

		return frags;
	}
}


class PartiallyConstructedModuleMessage {
	private final SocketAddress sender;
	private final ModuleMessageFragment[] fragments;
	private final Instant[] recvTimes;
	private final long id;
	private int remainingFragments;
	
	public PartiallyConstructedModuleMessage(SocketAddress sender, ModuleMessageFragment frag, Instant recvTime) {
		int fragsCnt = frag.getFragmentsCount();
		int fragNum = frag.getFragmentNumber();
		this.sender = sender;
		fragments = new ModuleMessageFragment[fragsCnt];
		fragments[fragNum] = frag;
		recvTimes = new Instant[fragsCnt];
		recvTimes[fragNum] = recvTime;
		id = frag.getId();
		remainingFragments = fragsCnt - 1;
	}
	
	public void addNewFragment(SocketAddress sender, ModuleMessageFragment frag, Instant recvTime) {
		if (frag.getId() != id || frag.getFragmentsCount() != fragments.length) {
			throw new IllegalArgumentException("Received malformed message fragment");
		}
		if (!sender.equals(this.sender)) {  // ultra low chances
			throw new IllegalArgumentException("Got message with same ID, but different sender");
		}
		
		int fragNum = frag.getFragmentNumber();
		if (fragments[fragNum] == null) {
			fragments[fragNum] = frag;
			recvTimes[fragNum] = recvTime;
			remainingFragments--;
		}
	}
	
	public boolean isComplete() {
		return remainingFragments == 0;
	}

	public ModuleMessage reassembleModuleMessage() throws IOException, ClassNotFoundException {
		assert isComplete();

		int userDataSize = (fragments.length - 1) * ModuleMessageFragment.USER_DATA_SIZE_LIMIT
				+ fragments[fragments.length - 1].getUserDataLength();
		ByteBuffer buffer = ByteBuffer.allocate(userDataSize);
		for (ModuleMessageFragment frag : fragments) {
			buffer.put(frag.getUserData());
		}

		ByteArrayInputStream in1 = new ByteArrayInputStream(buffer.array());
		ObjectInputStream in2 = new ObjectInputStream(in1);
		Object obj = in2.readObject();
		in2.close();
		in1.close();

		// below NullPointerException can be thrown
		ModuleMessage msg = (ModuleMessage) obj;
		NetworkSendable msgSendable = (NetworkSendable) msg;
		CommunicationInfo info = msgSendable.getCommunicationInfo();
		msgSendable.setCommunicationInfo(new CommunicationInfo(
				info.getAddress(),
				info.getTimestamps().newWithNextGap(calculateTimeGap())));

		return msg;
	}

	private Duration calculateTimeGap() {
		double FILTERING_FACTOR = 2;
		long[] times = new long[fragments.length];
		long timesSum = 0;
		long timesIncluded = 0;
		for (int i = 0; i < times.length; i++) {
			times[i] = Duration.between(fragments[i].getSentTime(), recvTimes[i]).toNanos();
			timesSum += times[i];
			timesIncluded++;
		}
		long timesAvgFirst = timesSum / timesIncluded;

		for (long d : times) {
			if (d >= timesAvgFirst * FILTERING_FACTOR) {
				timesSum -= d;
				timesIncluded--;
			}
		}

		return Duration.ofNanos(timesSum / timesIncluded);
	}
}


final class ModuleMessageFragment {
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
		if (fragNumber < fragsCnt - 1 && userData.length != USER_DATA_SIZE_LIMIT) {
			throw new IllegalArgumentException("Only last fragment can have user data smaller than USER_DATA_SIZE_LIMIT bytes");
		}
		
		data = ByteBuffer.allocate(MY_HEADER_SIZE + userData.length);
		data.putLong(id);
		data.putLong(0);  // sent time
		data.putInt(fragNumber);
		data.putInt(fragsCnt);
		data.put(userData);
	}
	
	public void updateSentTimeToNow() {
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
	
	public int getUserDataLength() {
		data.position(USER_DATA_OFFSET);
		return data.remaining();
	}

	public ByteBuffer getEncodedFragment() {
		return data;
	}
}
