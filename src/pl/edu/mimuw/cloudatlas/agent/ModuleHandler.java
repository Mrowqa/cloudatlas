/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.time.Duration;

/**
 *
 * @author pawel
 */
public class ModuleHandler {
	private final ZMIModule zmiModule;
	private final RMIModule rmiModule;
	private final TimerModule timerModule;

	public ModuleHandler(ZMIModule zmiModule, RMIModule rmiModule, TimerModule timerModule) {
		this.zmiModule = zmiModule;
		this.rmiModule = rmiModule;
		this.timerModule = timerModule;
	}
	
	public void runAll() {
		zmiModule.setModuleHandler(this);
		rmiModule.setModuleHandler(this);
		// timerModule.setModuleHandler(this);
		zmiModule.run();
	}
	
	public void enqueue(ZMIMessage message) throws InterruptedException {
		zmiModule.enqueue(message);
	}
	
	public void enqueue(RMIMessage message) throws InterruptedException {
		rmiModule.enqueue(message);
	}
	
	public void enqueue(TimerMessage message) throws InterruptedException {
		//timerModule.enqueue(message);
	}
}
