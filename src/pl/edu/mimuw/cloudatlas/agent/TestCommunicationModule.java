/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrowqa
 */
public class TestCommunicationModule extends Thread implements Module {
	private final LinkedBlockingQueue<TestCommunicationMessage> messages = new LinkedBlockingQueue<>();
	private final Random random = new Random();
	private ModulesHandler modulesHandler;
	private byte[] nextExpectedMessage;
	
	@Override
	public boolean canHandleMessage(ModuleMessage message) {
		return message instanceof TestCommunicationMessage;
	}

	@Override
	public void enqueue(ModuleMessage message) throws InterruptedException {
		messages.put((TestCommunicationMessage) message);
	}

	@Override
	public void setModulesHandler(ModulesHandler handler) {
		modulesHandler = handler;
	}
	
	@Override
	public void run() {
		sendFirstMessage();
		while (true) {
			try {
				TestCommunicationMessage msg = messages.take();
				answerToMessage(msg);
			} catch (Exception ex) {
				Logger.getLogger(TestCommunicationModule.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
	private void sendFirstMessage() {
		try {
			int size = 100;
			nextExpectedMessage = new byte[size];
			random.nextBytes(nextExpectedMessage);
			TestCommunicationMessage msg = new TestCommunicationMessage(nextExpectedMessage);
			CommunicationInfo info = new CommunicationInfo(new InetSocketAddress(
					InetAddress.getLocalHost(), CommunicationModule.SOCKET_PORT));
			msg.setCommunicationInfo(info);
			CommunicationMessage msg2 = CommunicationMessage.sendMessage(msg);
			modulesHandler.enqueue(msg2);
			
			System.out.println("Enqueued first message, size: " + size);
		} catch (Exception ex) {
			Logger.getLogger(TestCommunicationModule.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private void answerToMessage(TestCommunicationMessage message) throws InterruptedException {
		CommunicationInfo commInfo = message.getCommunicationInfo();
		int size = message.getTestData().length;
		Duration dT = commInfo.getTimestamps().getTimeDiff();  // null for first message
		System.out.println("\nReceived message:");
		System.out.println("\t- size: " + size);
		System.out.println("\t- data is ok: " + Arrays.equals(message.getTestData(), nextExpectedMessage));
		System.out.println("\t- dT: " + dT);

		int newSize = size * 2;
		Duration latency = Duration.ofSeconds(5);

		nextExpectedMessage = new byte[newSize];
		random.nextBytes(nextExpectedMessage);
		TestCommunicationMessage msg = new TestCommunicationMessage(nextExpectedMessage);
		msg.setCommunicationInfo(commInfo);

		CommunicationMessage msg2 = CommunicationMessage.sendMessage(msg);
		TimerMessage msg3 = TimerMessage.scheduleOneTimeCallback(random.nextInt(), latency, msg2);
		modulesHandler.enqueue(msg3);
		System.out.println("Scheduled reply with " + newSize + " bytes in " + latency);
	}
}
