/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.Module;
import pl.edu.mimuw.cloudatlas.agent.ModuleMessage;
import pl.edu.mimuw.cloudatlas.agent.ModulesHandler;
import pl.edu.mimuw.cloudatlas.agent.TimerMessage;
import pl.edu.mimuw.cloudatlas.agent.ZMIModule;
import pl.edu.mimuw.cloudatlas.model.PathName;

/**
 *
 * @author pawel
 */
// TODO cancel all callbacks once we are shutting down
// TODO deep copy when passing data through messages
public class DisseminationModule extends Thread implements Module {
	private final ExchangeProcesConfig config;
	private ModulesHandler handler;
	
	private final LinkedBlockingQueue<DisseminationMessage> messages = new LinkedBlockingQueue<>();
	private final Map<Long, ExchangeProcess> processes = new HashMap<>();
	private final Random r = new Random();

	private DisseminationModule(ExchangeProcesConfig config) {
		this.config = config;
	}
	
	public static DisseminationModule createModule(PathName node, JSONObject config) {
		NodeSelector selector;
		if (config != null && config.has("nodeSelector"))
			selector = NodeSelector.createByName(config.getString("nodeSelector"), node);
		else 
			selector = NodeSelector.createByName(NodeSelector.DEFAULT_SELECTOR_NAME, node);
		ExchangeProcesConfig config2 = ExchangeProcesConfig.createConfig(node, selector, config);
		return new DisseminationModule(config2);
	}
	
	@Override
	public boolean canHandleMessage(ModuleMessage message) {
		return message instanceof DisseminationMessage;
	}

	@Override
	public void enqueue(ModuleMessage message) throws InterruptedException {
		messages.put((DisseminationMessage) message);
	}

	@Override
	public void setModulesHandler(ModulesHandler handler) {
		this.handler = handler;
		config.setHandler(handler);
	}

	@Override
	public void run() {
		try {
			if (config.initializeDissemination)
				scheduleNextDissemination();
		} catch (InterruptedException ex) {
			Logger.getLogger(DisseminationModule.class.getName()).log(Level.SEVERE, null, ex);
		}
		while(true) {
			try {
				DisseminationMessage msg = messages.take();
				ExchangeProcess proc = processes.getOrDefault(msg.pid, null);
				System.out.println("Pid "+ msg.pid +" msg " + msg + " " + msg.type);
				if (proc == null) {
					proc = ExchangeProcess.fromFirstMessage(msg, config);
					if (proc == null) {
						// Incorrect type of first message. Skipping.
						Logger.getLogger(ZMIModule.class.getName()).log(Level.FINE, 
								"Got message of type {0} with unknown process id {1}", 
								new Object[]{msg.type, msg.pid});
						continue;
					}
					processes.put(msg.pid, proc);
				}
				try {
					proc.handleMsg(msg);
				} catch (Exception ex) {
					Logger.getLogger(DisseminationModule.class.getName()).log(Level.SEVERE, null, ex);
					assert proc.isFinished();
				}
				if (proc.isFinished()) {
					System.out.println("Proc is finished ");
					if (proc.isInitializedByMe()) {
						System.out.println("Scheduling new dissemination");
						scheduleNextDissemination();
					}
					processes.remove(msg.pid);
				}
			}
			catch (InterruptedException ex) {
				Logger.getLogger(DisseminationModule.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private void scheduleNextDissemination() throws InterruptedException {
		long pid = r.nextLong();
		DisseminationMessage innerMsg = DisseminationMessage.callbackStartNewExchange(pid);
		TimerMessage msg = TimerMessage.scheduleOneTimeCallback(pid, config.disseminationInterval, innerMsg);
		config.handler.enqueue(msg);
	}
}
