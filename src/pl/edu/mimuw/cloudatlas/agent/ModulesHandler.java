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

	public ModulesHandler(ZMIModule zmiModule, RMIModule rmiModule, TimerModule timerModule) {
		this.zmiModule = zmiModule;
		this.rmiModule = rmiModule;
		this.timerModule = timerModule;
	}
	
	public void runAll() {
		zmiModule.setModulesHandler(this);
		rmiModule.setModulesHandler(this);
		timerModule.setModulesHandler(this);
		
		timerModule.start();
		zmiModule.run();
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
			default:
				throw new IllegalArgumentException("Messages associated with module " + 
						message.module + " not supported");
		}
	}
}
