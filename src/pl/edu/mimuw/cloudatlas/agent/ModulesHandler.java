/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;


/**
 *
 * @author pawel
 */
public class ModulesHandler {
	private final ZMIModule zmiModule;
	private final RMIModule rmiModule;
	private final TimerModule timerModule;
	private final CommunicationModule communicationModule;

	public ModulesHandler(ZMIModule zmiModule, RMIModule rmiModule, TimerModule timerModule, CommunicationModule communicationModule) {
		this.zmiModule = zmiModule;
		this.rmiModule = rmiModule;
		this.timerModule = timerModule;
		this.communicationModule = communicationModule;
	}
	
	public void runAll() {
		zmiModule.setModulesHandler(this);
		rmiModule.setModulesHandler(this);
		timerModule.setModulesHandler(this);
		
		timerModule.start();
		communicationModule.start();
		zmiModule.start();
	}
	
	public void enqueue(ModuleMessage message) throws InterruptedException {
		switch(message.module) {
			case ZMI:
				zmiModule.enqueue((ZMIMessage) message);
				break;
			case RMI:
				rmiModule.enqueue((RMIMessage) message);
				break;
			case TIMER:
				timerModule.enqueue((TimerMessage) message);
				break;
			case COMMUNICATION:
				communicationModule.enqueue((CommunicationMessage) message);
				break;
			default:
				throw new IllegalArgumentException("Messages associated with module " + 
						message.module + " not supported");
		}
	}
}
