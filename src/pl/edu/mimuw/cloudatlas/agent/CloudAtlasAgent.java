/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Duration;
import pl.edu.mimuw.cloudatlas.interpreter.Interpreter;
import pl.edu.mimuw.cloudatlas.interpreter.InterpreterException;
import pl.edu.mimuw.cloudatlas.interpreter.QueryResult;
import pl.edu.mimuw.cloudatlas.interpreter.query.Yylex;
import pl.edu.mimuw.cloudatlas.interpreter.query.parser;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.Type.PrimaryType;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
// TODO consider synchronisation - implement reader/writers monitor
public class CloudAtlasAgent implements CloudAtlasInterface {
	private final ZMI zmi;
	private QueryExecutor executor = null;
	private ValueSet fallbackContacts = new ValueSet(new HashSet<>(), TypePrimitive.STRING);

	private static class QueryExecutor extends Thread {
		private final CloudAtlasAgent agent;
		private final Duration duration;
		
		public QueryExecutor(CloudAtlasAgent agent, Duration duration) {
			this.agent = agent;
			this.duration = duration;
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					agent.executeQueries();
					System.out.println("Queries executed.");
					Thread.sleep((long) duration.toMillis());
				} catch (Exception ex) {
					Logger.getLogger(CloudAtlasAgent.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
	
	public CloudAtlasAgent(ZMI zmi) {
		this.zmi = zmi;
	}
	
	public void startQueryExecutor(Duration duration) {
		if (executor != null) {
			System.err.println("Query executor already started.");
			return;
		}
		executor = new QueryExecutor(this, duration);
		executor.start();
		System.out.println("Query executor started");
	}
	
	@Override
	public synchronized ValueList getZones() throws RemoteException {
		return zmi.getZones();
	}

	@Override
	public synchronized AttributesMap getZoneAttributes(ValueString zone) throws RemoteException {
		return findZone(zmi, zone.getValue()).getAttributes();
	}
	
	// TODO refactor {install, uninstall, exectue}Queries to have generic code for ZMI traversing.
	@Override
	public synchronized void installQueries(ValueList queryNames, ValueList queries) throws RemoteException {
		checkElementType((TypeCollection)queryNames.getType(), PrimaryType.STRING);
		checkElementType((TypeCollection)queryNames.getType(), PrimaryType.STRING);
		if (queryNames.size() != queries.size()) {
			throw new RemoteException("QueriesNames and queries should have equal size " + queryNames.size() + " vs " + queries.size());
		}
		for (int i = 0; i < queryNames.size(); i++) {
			Attribute attribute = new Attribute(((ValueString)queryNames.get(i)).getValue());
			ValueString query = (ValueString)queries.get(i);
			if (!Attribute.isQuery(attribute)) {
				throw new RemoteException("Invalid query name " + attribute + " should be proceed with &");
			}
			installQuery(zmi, attribute, query);
		}
	}

	@Override
	public synchronized void uninstallQueries(ValueList queryNames) throws RemoteException {
		checkElementType((TypeCollection)queryNames.getType(), PrimaryType.STRING);
		for (Value queryName : queryNames) {
			Attribute attribute = new Attribute(((ValueString)queryName).getValue());
			if (!Attribute.isQuery(attribute)) {
				throw new RemoteException("Invalid query name " + attribute + " should be proceed with &");
			}
			uninstallQuery(zmi, attribute);
		}
	}

	@Override
	public synchronized void setZoneAttributes(ValueString zone, AttributesMap attributes) throws RemoteException {
		ZMI zoneZmi = findZone(zmi, new PathName(zone.getValue()));
		if (!zoneZmi.getSons().isEmpty()) {
			throw new IllegalArgumentException("setZoneAttributes is only allowed for singleton zone.");
		}
		zoneZmi.getAttributes().addOrChange(attributes);
	}

	@Override
	public synchronized void setFallbackContacts(ValueSet contacts) throws RemoteException {
		if (contacts.isNull()) {
			throw new IllegalArgumentException("Fallback contacts set can't be null");
		}
		checkElementType((TypeCollection)contacts.getType(), PrimaryType.STRING);
		fallbackContacts = contacts;
	}
	
	@Override
	public synchronized ValueSet getFallbackContacts() throws RemoteException {
		return fallbackContacts;
	}

	private synchronized void executeQueries() throws Exception {
		executeQueries(zmi);
	}
	
	private void checkElementType(TypeCollection collectionType, PrimaryType expectedType) {
		PrimaryType actualType = collectionType.getElementType().getPrimaryType();
		if (actualType != expectedType) 
			throw new IllegalArgumentException("Illegal type, got: " + actualType + " expected " + expectedType);
	}
	
	
	private static void executeQueries(ZMI zmi) throws Exception {
		if(!zmi.getSons().isEmpty()) {
			for(ZMI son : zmi.getSons())
				executeQueries(son);
			Interpreter interpreter = new Interpreter(zmi);
			for (Entry<Attribute, Value> entry : zmi.getAttributes()) {
				if (!Attribute.isQuery(entry.getKey())) {
					continue;
				}
				String query = ((ValueString)entry.getValue()).getValue();
				Yylex lex = new Yylex(new ByteArrayInputStream(query.getBytes()));
				try {
					List<QueryResult> result = interpreter.interpretProgram((new parser(lex)).pProgram());
					for (QueryResult r : result) {
						zmi.getAttributes().addOrChange(r.getName(), r.getValue());
					}
				} catch(InterpreterException exception) {
					//System.err.println("Interpreter exception on " + getPathName(zmi) + ": " + exception.getMessage());
				}
			}
		}
	}
	
	private static void installQuery(ZMI zmi, Attribute attribute, ValueString query) {
		if(!zmi.getSons().isEmpty()) {
			for(ZMI son : zmi.getSons())
				installQuery(son, attribute, query);
			zmi.getAttributes().addOrChange(attribute, query);
		}
	}
	
	private static void uninstallQuery(ZMI zmi, Attribute attribute) {
		if(!zmi.getSons().isEmpty()) {
			for(ZMI son : zmi.getSons())
				uninstallQuery(son, attribute);
			zmi.getAttributes().remove(attribute);
		}
	}

	private ZMI findZone(ZMI zmi, String name) throws RemoteException {
		return findZone(zmi, new PathName(name));
	}
	
	private ZMI findZone(ZMI zmi, PathName pathName) throws RemoteException {
		if (pathName.getComponents().isEmpty()) {
			return zmi;
		}
		String currentName = pathName.getComponents().get(0);
		for (ZMI son : zmi.getSons()) {
			PathName sonPathName = son.getPathName();
			if (!sonPathName.getComponents().isEmpty() && sonPathName.getSingletonName().equals(currentName)) {
				return findZone(son, pathName.consumePrefix());
			}
		}
		throw new RemoteException("Zone not found.");
	}
}
