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
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.Query;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.rmiutils.RmiCallException;

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
	private final String zoneName;

	public RMIModule(String name) {
		this.zoneName = name;
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
			registry.rebind("CloudAtlas" + zoneName, stub);
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
		ZMIMessage request = ZMIMessage.getZmi(pid);
		RMIMessage response = waitForResponseOrError(request);
		return response.zmi;
	}
		
	@Override
	public ValueList getZones() throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = ZMIMessage.getZones(pid);
		RMIMessage response = waitForResponseOrError(request);
		return (ValueList)response.value1;
	}

	@Override
	public AttributesMap getZoneAttributes(ValueString zone) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = ZMIMessage.getZoneAttributes(pid, zone);
		RMIMessage response = waitForResponseOrError(request);
		return response.attributes;
	}
	
	@Override
	public void installQueries(ValueList queryNames, ValueList queries, ValueList signatures) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		checkElementType((TypeCollection) queryNames.getType(), Type.PrimaryType.STRING);
		checkElementType((TypeCollection) queries.getType(), Type.PrimaryType.STRING);
		checkElementType((TypeCollection) signatures.getType(), Type.PrimaryType.STRING);
		if (queryNames.size() != queries.size() || queries.size() != signatures.size()) {
			throw new RmiCallException("QueryNames, queries and signatures must have equal size " + queryNames.size() + " vs " + queries.size() + " vs " + signatures.size());
		}
		if (queryNames.size() != 1) {
			throw new RmiCallException("You can install only one query at once.");
		}
		
		String queryText = ((ValueString)queries.get(0)).getValue();
		String querySignature = ((ValueString)signatures.get(0)).getValue();
		ZMIMessage request = ZMIMessage.installQuery(pid, queryNames.get(0), queryText, querySignature);
		waitForResponseOrError(request);
	}

	@Override
	public void uninstallQueries(ValueList queryNames, ValueList signatures) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		checkElementType((TypeCollection) queryNames.getType(), Type.PrimaryType.STRING);
		checkElementType((TypeCollection) signatures.getType(), Type.PrimaryType.STRING);
		if (queryNames.size() != signatures.size()) {
			throw new RmiCallException("QueryNames and signatures must have equal size " + queryNames.size() + " vs " + signatures.size());
		}
		if (queryNames.size() != 1) {
			throw new RmiCallException("You can uninstall only one query at once.");
		}
		
		String querySignature = ((ValueString)signatures.get(0)).getValue();
		ZMIMessage request = ZMIMessage.uninstallQuery(pid, queryNames.get(0), querySignature);
		waitForResponseOrError(request);
	}
	
	@Override
	public HashMap<Attribute, Query> getAllQueries() throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = ZMIMessage.getAllQueries(pid);
		RMIMessage response = waitForResponseOrError(request);
		return response.queries;
	}

	@Override
	public void setZoneAttributes(ValueString zone, AttributesMap attributes) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = ZMIMessage.setZoneAttributes(pid, zone, attributes);
		waitForResponseOrError(request);
	}

	@Override
	public void setFallbackContacts(ValueSet contacts) throws RemoteException {
		long pid = nextPid.getAndIncrement();
		if (contacts.isNull()) {
			throw new IllegalArgumentException("Fallback contacts set can't be null");
		}
		checkElementType((TypeCollection) contacts.getType(), Type.PrimaryType.CONTACT);
		ZMIMessage request = ZMIMessage.setFallbackContacts(pid, contacts);
		waitForResponseOrError(request);
	}
	
	@Override
	public ValueSet getFallbackContacts() throws RemoteException {
		long pid = nextPid.getAndIncrement();
		ZMIMessage request = ZMIMessage.getFallbackContacts(pid);
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
				throw new RmiCallException(response.errorMessage);
			}
		} catch (InterruptedException ex) {
			throw new RmiCallException("Exception occured while fetching the response " + ex.getMessage());
		}
		return response;
	}
	
	private void checkElementType(TypeCollection collectionType, Type.PrimaryType expectedType) {
		Type.PrimaryType actualType = collectionType.getElementType().getPrimaryType();
		if (actualType != expectedType) {
			throw new IllegalArgumentException("Illegal type, got: " + actualType + " expected " + expectedType);
		}
	}
}
