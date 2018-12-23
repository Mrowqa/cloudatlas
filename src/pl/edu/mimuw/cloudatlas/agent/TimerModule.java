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
public class TimerModule extends Thread {
	private ModuleHandler moduleHandler;
	
	public void setModuleHandler(ModuleHandler moduleHandler) {
		this.moduleHandler = moduleHandler;
	}
	
	@Override
	public void run() {
		throw new UnsupportedOperationException("Not yet supported");
	}
}
