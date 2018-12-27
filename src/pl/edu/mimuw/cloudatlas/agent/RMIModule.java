/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */

class MessagesCollection {
	private final HashMap<Long, RMIMessage> messages = new HashMap<>();
	public synchronized RMIMessage waitAndPop(long pid) throws InterruptedException {
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

public class RMIModule implements CloudAtlasInterface, Module {
	private final MessagesCollection messages;
	private ModulesHandler modulesHandler;
	volatile AtomicLong nextPid;

	public RMIModule() {
		this.nextPid = new AtomicLong(0L);
		this.messages = new MessagesCollection();
	}
	
	@Override
	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
	}

	@Override
	public boolean canHandleMessage(ModuleMessage message) {
		return message instanceof RMIMessage;
	}

	@Override
	public void enqueue(ModuleMessage message) throws InterruptedException {
		messages.push((RMIMessage) message);
	}

	@Override
	public void start() {
		try {
			CloudAtlasInterface stub = (CloudAtlasInterface) UnicastRemoteObject.exportObject(this, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("CloudAtlas", stub);
		}
		catch (RemoteException ex) { // will be caught in main
			throw new RuntimeException(ex);
		}
	}
	
	public void enqueue(RMIMessage message) throws InterruptedException {
		messages.push(message);
	}
	
	@Override
	public ZMI getWholeZMI() throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.GET_ZMI);
		RMIMessage response = waitForResponseOrError(request);
		return response.zmi;
	}
		
	@Override
	public ValueList getZones() throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.GET_ZONES);
		RMIMessage response = waitForResponseOrError(request);
		return (ValueList)response.value1;
	}

	@Override
	public AttributesMap getZoneAttributes(ValueString zone) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.GET_ZONE_ATTRIBUTES, zone);
		RMIMessage response = waitForResponseOrError(request);
		return response.attributes;
	}
	
	@Override
	public void installQueries(ValueList queryNames, ValueList queries, ValueList signatures) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.INSTALL_QUERIES, queryNames, queries, signatures);
		waitForResponseOrError(request);
	}

	@Override
	public void uninstallQueries(ValueList queryNames, ValueList signatures) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.UNINSTALL_QUERIES, queryNames, signatures);
		waitForResponseOrError(request);
	}

	@Override
	public void setZoneAttributes(ValueString zone, AttributesMap attributes) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.SET_ZONE_ATTRIBUTES, zone, attributes);
		waitForResponseOrError(request);
	}

	@Override
	public void setFallbackContacts(ValueSet contacts) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.SET_FALLBACK_CONTACTS, contacts);
		waitForResponseOrError(request);
	}
	
	@Override
	public ValueSet getFallbackContacts() throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = new ZMIMessage(pid, ZMIMessage.Type.GET_FALLBACK_CONTACTS);
		RMIMessage response = waitForResponseOrError(request);
		return (ValueSet)response.value1;
	}
	
	/**
	 * Sends request, waits for response and returns it. 
	 * If an exception occurs or the response contains error throws RemoteException. 
	 */
	private RMIMessage waitForResponseOrError(ZMIMessage request) throws RemoteException{
		RMIMessage response;
		try {
			modulesHandler.enqueue(request);
			response = messages.waitAndPop(request.pid);
			if (response.type == RMIMessage.Type.ERROR) {
				throw new RemoteException(response.errorMessage);
			}
		} catch (InterruptedException ex) {
			throw new RemoteException("Exception occured while fetching the response " + ex.getMessage());
		}
		return response;
	}
}
