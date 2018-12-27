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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import pl.edu.mimuw.cloudatlas.interpreter.AttributesExtractor;
import pl.edu.mimuw.cloudatlas.interpreter.Interpreter;
import pl.edu.mimuw.cloudatlas.interpreter.InterpreterException;
import pl.edu.mimuw.cloudatlas.interpreter.QueryResult;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.Program;
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
import pl.edu.mimuw.cloudatlas.model.ValueNull;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.signer.QueryOperation;
import pl.edu.mimuw.cloudatlas.signer.Signer;

/**
 *
 * @author pawel
 */
public class ZMIModule extends Thread implements Module {
	private final LinkedBlockingQueue<ZMIMessage> messages;
	private final ZMI zmi;
	private final Duration queryExecutionInterval;
	private final Random random;
	private final Signer signVerifier;
	private ValueSet fallbackContacts;
	private ModulesHandler modulesHandler;

	@Override
	public void setModulesHandler(ModulesHandler modulesHandler) {
		this.modulesHandler = modulesHandler;
	}

	@Override
	public boolean canHandleMessage(ModuleMessage message) {
		return message instanceof ZMIMessage;
	}

	@Override
	public void enqueue(ModuleMessage message) throws InterruptedException {
		messages.put((ZMIMessage) message);
	}

	public ZMIModule(ZMI zmi, Duration queryExecutionInterval, String pubKeyFilename) {
		this.messages = new LinkedBlockingQueue<>();
		this.zmi = zmi;
		this.queryExecutionInterval = queryExecutionInterval;
		this.random = new Random();
		this.fallbackContacts = new ValueSet(new HashSet<>(), TypePrimitive.CONTACT);
		try {
			this.signVerifier = new Signer(Signer.Mode.SIGN_VERIFIER, pubKeyFilename);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void run() {
		scheduleQueriesExecution();
		while (true) {
			try {
				ZMIMessage message = messages.take();
				if (message.type == ZMIMessage.Type.EXECUTE_QUERIES) {
					try {
						executeQueries();
					} catch(Exception ex) {
						Logger.getLogger(ZMIModule.class.getName()).log(Level.FINEST, "Queries exectued.");
					}
					scheduleQueriesExecution();
					continue;
				}
					
				// Received message is from RMI module.
				RMIMessage response = new RMIMessage(message.pid, RMIMessage.Type.SUCCESS);
				try {
					switch (message.type) {
						case GET_ZMI:
							response.zmi = getWholeZMI();
							break;
						case GET_ZONES:
							response.value1 = getZones();
							break;
						case GET_ZONE_ATTRIBUTES:
							response.attributes = getZoneAttributes((ValueString) message.value1);
							break;
						case SET_ZONE_ATTRIBUTES:
							setZoneAttributes((ValueString) message.value1, message.attributes);
							break;
						case INSTALL_QUERIES:
							installQueries((ValueList) message.value1, (ValueList) message.value2, (ValueList) message.value3);
							break;
						case UNINSTALL_QUERIES:
							uninstallQueries((ValueList) message.value1, (ValueList) message.value2);
							break;
						case GET_FALLBACK_CONTACTS:
							response.value1 = getFallbackContacts();
							break;
						case SET_FALLBACK_CONTACTS:
							setFallbackContacts((ValueSet) message.value1);
							break;
						default:
							throw new UnsupportedOperationException("Message not supported: " + message.type);
					}
				} catch (Exception ex) {
					response.type = RMIMessage.Type.ERROR;
					response.errorMessage = ex.getMessage();
				}
				modulesHandler.enqueue(response);
			} catch (InterruptedException ex) {
				Logger.getLogger(ZMIModule.class.getName()).log(Level.FINE, null, ex);
			}
		}
	}

	private ZMI getWholeZMI() {
		return zmi.clone();
	}

	private ValueList getZones() {
		return zmi.getZones();
	}

	private AttributesMap getZoneAttributes(ValueString zone) throws RemoteException {
		return findZone(zmi, zone.getValue()).getAttributes();
	}

	private void installQueries(ValueList queryNames, ValueList queries, ValueList signatures) throws RemoteException {
		checkElementType((TypeCollection) queryNames.getType(), PrimaryType.STRING);
		checkElementType((TypeCollection) queries.getType(), PrimaryType.STRING);
		checkElementType((TypeCollection) signatures.getType(), PrimaryType.STRING);
		if (queryNames.size() != queries.size() || queries.size() != signatures.size()) {
			throw new RemoteException("QueryNames, queries and signatures must have equal size " + queryNames.size() + " vs " + queries.size() + " vs " + signatures.size());
		}
		if (queryNames.size() != 1) {
			throw new RemoteException("You can install only one query at once.");
		}
		String name = ((ValueString) queryNames.get(0)).getValue();
		ValueString query = (ValueString) queries.get(0);
		String errorMsg = validateQuery(name, query.getValue());
		if (errorMsg != null) {
			throw new RemoteException(errorMsg);
		}

		String signature = ((ValueString) signatures.get(0)).getValue();
		QueryOperation queryOp = QueryOperation.newQueryInstall(name, query.getValue());
		if (!signVerifier.verifyQueryOperationSignature(queryOp, signature)) {
			throw new RemoteException("SecurityError: Invalid signatures for queries.");
		}

		Attribute attribute = new Attribute(name);
		installQuery(zmi, attribute, query);
	}
	
	// returns error message, null==ok
	public static String validateQuery(String name, String text) {
		Attribute attribute = new Attribute(name);
		if (!Attribute.isQuery(attribute)) {
			return "Invalid query name " + attribute + " should be proceed with &";
		}
		try {
			tryParse(text);
		} catch (Exception e) {
			return "Error parsing query: " + e.getMessage();
		}
		
		return null;
	}

	private void uninstallQueries(ValueList queryNames, ValueList signatures) throws RemoteException {
		checkElementType((TypeCollection) queryNames.getType(), PrimaryType.STRING);
		checkElementType((TypeCollection) signatures.getType(), PrimaryType.STRING);
		if (queryNames.size() != signatures.size()) {
			throw new RemoteException("QueryNames and signatures must have equal size " + queryNames.size() + " vs " + signatures.size());
		}
		if (queryNames.size() != 1) {
			throw new RemoteException("You can uninstall only one query at once.");
		}
		Value queryName = queryNames.get(0);
		Attribute attribute = new Attribute(((ValueString) queryName).getValue());
		if (!Attribute.isQuery(attribute)) {
			throw new RemoteException("Invalid query name " + attribute + " should be proceed with &");
		}

		String signature = ((ValueString) signatures.get(0)).getValue();
		String queryNameStr = ((ValueString) queryName).getValue();
		QueryOperation queryOp = QueryOperation.newQueryUninstall(queryNameStr);
		if (!signVerifier.verifyQueryOperationSignature(queryOp, signature)) {
			throw new RemoteException("SecurityError: Invalid signatures for queries.");
		}

		if (!uninstallQuery(zmi, attribute)) {
			throw new RemoteException("Query not found.");
		}
	}

	private void setZoneAttributes(ValueString zone, AttributesMap attributes) throws RemoteException {
		ZMI zoneZmi = findZone(zmi, new PathName(zone.getValue()));
		if (!zoneZmi.getSons().isEmpty()) {
			throw new IllegalArgumentException("setZoneAttributes is only allowed for singleton zone.");
		}
		zoneZmi.getAttributes().addOrChange(attributes);
	}

	private void setFallbackContacts(ValueSet contacts) throws RemoteException {
		if (contacts.isNull()) {
			throw new IllegalArgumentException("Fallback contacts set can't be null");
		}
		checkElementType((TypeCollection) contacts.getType(), PrimaryType.CONTACT);
		fallbackContacts = contacts;
	}

	private ValueSet getFallbackContacts() throws RemoteException {
		return fallbackContacts;
	}

	private void executeQueries() throws Exception {
		executeQueries(zmi);
	}

	private void checkElementType(TypeCollection collectionType, PrimaryType expectedType) {
		PrimaryType actualType = collectionType.getElementType().getPrimaryType();
		if (actualType != expectedType) {
			throw new IllegalArgumentException("Illegal type, got: " + actualType + " expected " + expectedType);
		}
	}

	private static void executeQueries(ZMI zmi) throws Exception {
		if (!zmi.getSons().isEmpty()) {
			for (ZMI son : zmi.getSons()) {
				executeQueries(son);
			}
			Interpreter interpreter = new Interpreter(zmi);
			ArrayList<ValueString> queries = new ArrayList<>();
			for (Entry<Attribute, Value> entry : zmi.getAttributes()) {
				if (Attribute.isQuery(entry.getKey())) {
					queries.add((ValueString) entry.getValue());
				}
			}
			for (ValueString query : queries) {
				try {
					Program program = tryParse(query.getValue());
					Set<String> attributes = AttributesExtractor.extractAttributes(program);
					for (String attribute : attributes) {
						zmi.getAttributes().addOrChange(attribute, ValueNull.getInstance());
					}
					List<QueryResult> result = interpreter.interpretProgram(program);
					for (QueryResult r : result) {
						zmi.getAttributes().addOrChange(r.getName(), r.getValue());
					}
				} catch (InterpreterException exception) {
					//System.err.println("Interpreter exception on " + getPathName(zmi) + ": " + exception.getMessage());
				}
			}
		}
	}

	private static Program tryParse(String query) throws Exception {
		Yylex lex = new Yylex(new ByteArrayInputStream(query.getBytes()));
		return (new parser(lex)).pProgram();
	}

	private static void installQuery(ZMI zmi, Attribute attribute, ValueString query) {
		if (!zmi.getSons().isEmpty()) {
			for (ZMI son : zmi.getSons()) {
				installQuery(son, attribute, query);
			}
			zmi.getAttributes().addOrChange(attribute, query);
		}
	}

	private static boolean uninstallQuery(ZMI zmi, Attribute attribute) throws RemoteException {
		boolean uninstalled = false;
		if (!zmi.getSons().isEmpty()) {
			for (ZMI son : zmi.getSons()) {
				uninstalled |= uninstallQuery(son, attribute);
			}
			uninstalled |= zmi.getAttributes().getOrNull(attribute) != null;
			zmi.getAttributes().remove(attribute);
		}
		return uninstalled;
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
	
	private void scheduleQueriesExecution() {
		long id = random.nextLong();
		ZMIMessage callbackMessage = new ZMIMessage(ZMIMessage.Type.EXECUTE_QUERIES);
		TimerMessage message = TimerMessage.scheduleOneTimeCallback(id, queryExecutionInterval, callbackMessage);
		try {
			modulesHandler.enqueue(message);
		} catch (InterruptedException ex) {
			Logger.getLogger(ZMIModule.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
