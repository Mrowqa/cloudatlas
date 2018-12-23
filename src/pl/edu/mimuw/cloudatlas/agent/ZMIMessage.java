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
public class ZMIMessage {
	public enum MessageType {
		GET_ZMI, GET_ZONES, GET_ZONE_ATTRIBUTES, SET_ZONE_ATTRIBUTES, 
		INSTALL_QUERIES, UNINSTALL_QUERIES, 
		SET_FALLBACK_CONTACT, GET_FALLBACK_CONTACT
	}

	public ZMIMessage(long pid, MessageType type) {
		this.pid = pid;
		this.type = type;
	}
	
	public final long pid;
	public final MessageType type;
	public Value value1;
	public Value value2;
	public AttributesMap attributes;
}
