/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Value;

/**
 *
 * @author pawel
 */
public class ZMIMessage extends ModuleMessage{
	public enum Type {
		GET_ZMI, GET_ZONES, GET_ZONE_ATTRIBUTES, SET_ZONE_ATTRIBUTES, 
		INSTALL_QUERIES, UNINSTALL_QUERIES, 
		SET_FALLBACK_CONTACTS, GET_FALLBACK_CONTACTS,
		EXECUTE_QUERIES
	}

	public ZMIMessage(Type type) {
		this.module = Module.ZMI;
		this.type = type;
	}
	
	public ZMIMessage(long pid, Type type) {
		this(type);
		this.module = Module.ZMI;
		this.pid = pid;
	}
	
	public final Type type;
	public long pid;
	public Value value1;
	public Value value2;
	public AttributesMap attributes;
}
