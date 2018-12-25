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
	private final Module[] modules;

	public ModulesHandler(Module[] modules) {
		this.modules = modules;
	}
	
	public void runAll() {
		for (Module m : modules) {
			m.setModulesHandler(this);
			m.start();
		}
	}
	
	public void enqueue(ModuleMessage message) throws InterruptedException {
		for (Module m : modules) {
			if (m.canHandleMessage(message)) {
				m.enqueue(message);
				return;
			}
		}

		throw new IllegalArgumentException("Messages not supported: " + message.toString());
	}
}
