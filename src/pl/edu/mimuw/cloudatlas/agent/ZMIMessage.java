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
public class ZMIMessage extends ModuleMessage {
	public enum Type {
		GET_ZMI, GET_ZONES, GET_ZONE_ATTRIBUTES, SET_ZONE_ATTRIBUTES, 
		INSTALL_QUERIES, UNINSTALL_QUERIES, 
		SET_FALLBACK_CONTACTS, GET_FALLBACK_CONTACTS,
		EXECUTE_QUERIES
	}

	public ZMIMessage(Type type) {
		this.type = type;
	}
	
	public ZMIMessage(long pid, Type type) {
		this(type);
		this.pid = pid;
	}
	
	public ZMIMessage(long pid, Type type, Value value) {
		this(pid, type);
		this.value1 = value;
	}
	
	public ZMIMessage(long pid, Type type, Value value1, Value value2) {
		this(pid, type, value1);
		this.value2 = value2;
	}
	
	public ZMIMessage(long pid, Type type, Value value, AttributesMap attributes) {
		this(pid, type, value);
		this.attributes = attributes;
	}
	
	public final Type type;
	public long pid;
	public Value value1;
	public Value value2;
	public AttributesMap attributes;
}
