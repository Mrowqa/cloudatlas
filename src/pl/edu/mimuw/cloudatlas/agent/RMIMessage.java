/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import org.omg.PortableInterceptor.SUCCESSFUL;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class RMIMessage {
	public enum MessageType {
		SUCCESS, ERROR
	}

	public RMIMessage(Long pid, MessageType type) {
		this.pid = pid;
		this.type = type;
	}
	
	public Long pid;
	public MessageType type;
	public String errorMessage;
	
	public ZMI zmi;
	public AttributesMap attributes;
	public Value value1;
}
