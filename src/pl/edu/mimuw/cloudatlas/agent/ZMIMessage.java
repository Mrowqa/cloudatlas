/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.time.Instant;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ValueTime;

/**
 *
 * @author pawel
 */
public class ZMIMessage extends ModuleMessage {
	public enum Type {
		GET_ZMI, GET_ZONES, GET_ZONE_ATTRIBUTES, SET_ZONE_ATTRIBUTES, 
		INSTALL_QUERIES, UNINSTALL_QUERIES, 
		SET_FALLBACK_CONTACTS, GET_FALLBACK_CONTACTS,
		EXECUTE_QUERIES, GET_ZMI_CONTACTS_QUERIES
	}

	public static ZMIMessage getLocalZMIInfo(long pid) {
		return new ZMIMessage(pid, Type.GET_ZMI_CONTACTS_QUERIES);
	}
	
	public static ZMIMessage installQuery(long pid, Value name, Value query, Value signature) {
		ZMIMessage ret = new ZMIMessage(pid, Type.INSTALL_QUERIES);
		ret.value1 = name;
		ret.value2 = signature;
		ret.valueAndFreshness = ValueAndFreshness.freshValue(query);
		return ret;
	}
	
	public static ZMIMessage uninstallQuery(long pid, Value name, Value signature) {
		ZMIMessage ret = new ZMIMessage(pid, Type.UNINSTALL_QUERIES);
		ret.value1 = name;
		ret.value2 = ValueTime.now();
		ret.value3 = signature;
		return ret;
	}
	
	public static ZMIMessage setZoneAttributes(long pid, Value zone, AttributesMap map) {
		ZMIMessage ret = new ZMIMessage(pid, Type.SET_ZONE_ATTRIBUTES);
		ret.value1 = zone;
		ret.value2 = ValueTime.now();
		ret.attributes = map;
		return ret;
	}
	
	public static ZMIMessage setFallbackContacts(long pid, Value contacts) {
		ZMIMessage ret = new ZMIMessage(pid, Type.SET_FALLBACK_CONTACTS);
		ret.valueAndFreshness = ValueAndFreshness.freshValue(contacts);
		return ret;
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
	
	public ZMIMessage(long pid, Type type, Value value1, Value value2, Value value3) {
		this(pid, type, value1);
		this.value2 = value2;
		this.value3 = value3;
	}

	public ZMIMessage(long pid, Type type, Value value, AttributesMap attributes) {
		this(pid, type, value);
		this.attributes = attributes;
	}
	
	public final Type type;
	public long pid;
	public Value value1;
	public Value value2;
	public Value value3;
	public ValueAndFreshness valueAndFreshness;
	public AttributesMap attributes;
}
