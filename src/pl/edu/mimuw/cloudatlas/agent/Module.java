/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

/**
 *
 * @author mrowqa
 */
public interface Module {
	public boolean canHandleMessage(ModuleMessage message);
	public void enqueue(ModuleMessage message) throws InterruptedException;
	public void setModulesHandler(ModulesHandler handler);
	public void start();
}
