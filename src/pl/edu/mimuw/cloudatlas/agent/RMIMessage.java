/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class RMIMessage extends ModuleMessage {
	public enum Type {
		SUCCESS, ERROR
	}

	/**
	 *
	 * @param pid
	 * @param type
	 */
	public RMIMessage(long pid, Type type) {
		this.pid = pid;
		this.type = type;
	}
	
	public long pid;
	public Type type;
	public String errorMessage;
	
	public ZMI zmi;
	public AttributesMap attributes;
	public Value value1;
}
