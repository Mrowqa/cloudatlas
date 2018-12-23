/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.rmi.RemoteException;
import java.util.HashMap;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */

class MessageHandler {
	private final HashMap<Long, RMIMessage> messages = new HashMap<>();
	public synchronized RMIMessage pop(long pid) throws InterruptedException {
		while (true) {
			RMIMessage message = messages.getOrDefault(pid, null);
			if (message != null) {
				messages.remove(pid);
				return message;
			}
			wait();
		}
	}
	
	public synchronized void push(RMIMessage message) {
		messages.put(message.pid, message);
		notifyAll();
	}
}

public class RMIModule implements CloudAtlasInterface {
	private MessageHandler messages = new MessageHandler();
	private ModuleHandler moduleHandler;
	volatile long nextPid = 0;
	
	public void setModuleHandler(ModuleHandler moduleHandler) {
		this.moduleHandler = moduleHandler;
	}
	
	public void enqueue(RMIMessage message) throws InterruptedException {
		messages.push(message);
	}
	
	@Override
	public ZMI getWholeZMI() throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.GET_ZMI);
		RMIMessage response = getResponseOrError(request);
		return response.zmi;
	}
		
	@Override
	public ValueList getZones() throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.GET_ZONES);
		RMIMessage response = getResponseOrError(request);
		return (ValueList)response.value1;
	}

	@Override
	public AttributesMap getZoneAttributes(ValueString zone) throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.GET_ZONE_ATTRIBUTES);
		request.value1 = zone;
		RMIMessage response = getResponseOrError(request);
		return response.attributes;
	}
	
	@Override
	public void installQueries(ValueList queryNames, ValueList queries) throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.INSTALL_QUERIES);
		request.value1 = queryNames;
		request.value2 = queries;
		getResponseOrError(request);
	}

	@Override
	public void uninstallQueries(ValueList queryNames) throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.UNINSTALL_QUERIES);
		request.value1 = queryNames;
		getResponseOrError(request);
	}

	@Override
	public void setZoneAttributes(ValueString zone, AttributesMap attributes) throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.SET_ZONE_ATTRIBUTES);
		System.out.println("Set zone attributes");
		request.value1 = zone;
		request.attributes = attributes;
		getResponseOrError(request);
	}

	@Override
	public void setFallbackContacts(ValueSet contacts) throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.SET_FALLBACK_CONTACT);
		request.value1 = contacts;
		getResponseOrError(request);
	}
	
	@Override
	public ValueSet getFallbackContacts() throws RemoteException {
		long pid = nextPid++;
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.MessageType.GET_FALLBACK_CONTACT);
		RMIMessage response = getResponseOrError(request);
		return (ValueSet)response.value1;
	}
	
	public RMIMessage getResponseOrError(ZMIMessage request) throws RemoteException{
		RMIMessage response;
		try {
			moduleHandler.enqueue(request);
			response = messages.pop(request.pid);
			if (response.type == RMIMessage.MessageType.ERROR) {
				throw new RemoteException(response.errorMessage);
			}
		} catch (InterruptedException ex) {
			throw new RemoteException("Exception occured during fetching the message " + ex.getMessage());
		}
		return response;
	}
}
