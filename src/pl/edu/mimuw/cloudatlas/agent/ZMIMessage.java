/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.util.HashMap;
import pl.edu.mimuw.cloudatlas.agent.dissemination.AgentData;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Query;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ValueTime;

/**
 *
 * @author pawel
 */
public class ZMIMessage extends ModuleMessage {
	public enum Type {
		GET_ZMI, GET_ZONES, GET_ZONE_ATTRIBUTES, SET_ZONE_ATTRIBUTES, 
		INSTALL_QUERIES, UNINSTALL_QUERIES, GET_ALL_QUERIES,
		SET_FALLBACK_CONTACTS, GET_FALLBACK_CONTACTS,
		EXECUTE_QUERIES, REMOVE_OUTDATED_ZONES,
		GET_LOCAL_AGENT_DATA, UPDATE_WITH_REMOTE_DATA,
	}
	
	static ZMIMessage getZmi(long pid) {
		return new ZMIMessage(pid, Type.GET_ZMI);
	}
	
	static ZMIMessage getZones(long pid) {
		return new ZMIMessage(pid, Type.GET_ZONES);
	}
	
	static ZMIMessage getZoneAttributes(long pid, ValueString zone) {
		ZMIMessage ret = new ZMIMessage(pid, Type.GET_ZONE_ATTRIBUTES);
		ret.value1 = zone;
		return ret;
	}
	
	static ZMIMessage getFallbackContacts(long pid) {
		return new ZMIMessage(pid, Type.GET_FALLBACK_CONTACTS);
	}

	static ZMIMessage executeQueries() {
		return new ZMIMessage(Type.EXECUTE_QUERIES);
	}

	public static ZMIMessage getLocalAgentData(long pid) {
		return new ZMIMessage(pid, Type.GET_LOCAL_AGENT_DATA);
	}
	
	public static ZMIMessage updateWithRemoteData(long pid, AgentData data) {
		ZMIMessage ret = new ZMIMessage(pid, Type.UPDATE_WITH_REMOTE_DATA);
		ret.remoteData = data;
		return ret;
	}
	
	public static ZMIMessage installQuery(long pid, Value name, String query, String signature) {
		ZMIMessage ret = new ZMIMessage(pid, Type.INSTALL_QUERIES);
		ret.value1 = name;
		ret.query = new Query(query, signature);
		return ret;
	}
	
	public static ZMIMessage uninstallQuery(long pid, Value name, String signature) {
		ZMIMessage ret = new ZMIMessage(pid, Type.UNINSTALL_QUERIES);
		ret.value1 = name;
		ret.query = new Query("", signature);
		return ret;
	}
	
	public static ZMIMessage getAllQueries(long pid) {
		ZMIMessage ret = new ZMIMessage(pid, Type.GET_ALL_QUERIES);
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
	
	public static ZMIMessage removeOutdatedZones() {
		return new ZMIMessage(Type.REMOVE_OUTDATED_ZONES);
	}
	
	private ZMIMessage(Type type) {
		this.type = type;
	}
	
	private ZMIMessage(long pid, Type type) {
		this(type);
		this.pid = pid;
	}
	
	public final Type type;
	public long pid;
	public Value value1;
	public Value value2;
	public Query query;
	public ValueAndFreshness valueAndFreshness;
	public AttributesMap attributes;
	public AgentData remoteData;
	public HashMap<Attribute, Query> queries;
}
