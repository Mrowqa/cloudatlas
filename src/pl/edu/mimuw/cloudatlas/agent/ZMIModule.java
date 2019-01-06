/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import pl.edu.mimuw.cloudatlas.agent.dissemination.AgentData;
import pl.edu.mimuw.cloudatlas.agent.dissemination.DisseminationMessage;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONArray;
import org.json.JSONObject;
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
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueDuration;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueNull;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.ValueTime;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.rmiutils.RmiCallException;
import pl.edu.mimuw.cloudatlas.signer.QueryOperation;
import pl.edu.mimuw.cloudatlas.signer.Signer;

/**
 *
 * @author pawel
 */
public class ZMIModule extends Thread implements Module {
	private final LinkedBlockingQueue<ZMIMessage> messages;
	private final Random random;
	private final ZMI zmi;
	private final PathName name;
	private final HashMap<Attribute, ValueAndFreshness> queries;  // todo remember signatures; you can also patch webclient to display signatures -> basically you just change map type here, try to recompile, fix mismatching type errors, and try again :P
	private final Signer signVerifier;
	private ValueAndFreshness fallbackContacts;
	private Duration queryExecutionInterval = Duration.ofSeconds(5);
	private Duration removeOutdatedZonesInterval = Duration.ofSeconds(60); // Interval between two events of removing outdated zones
	private Duration zoneOutdatedDuration = Duration.ofSeconds(60); // Duration after a zone become outdated
	private int contactsPerNode = 2;
	private ModulesHandler modulesHandler;

	public static ZMIModule createModule(PathName name, ZMI zmi, ValueSet fallbackContacts, String pubKeyFilename, ValueSet localContacts, JSONObject config) {
		ZMIModule module = new ZMIModule(zmi, name, fallbackContacts, pubKeyFilename);
		findZone(zmi, name).getAttributes().add("contacts", localContacts);
		if (config == null)
			return module;
		if (config.has("queryExecutionInterval"))
			module.queryExecutionInterval = ConfigUtils.parseInterval(config.getString("queryExecutionInterval"));
		if (config.has("removeOutdatedZonesInterval"))
			module.removeOutdatedZonesInterval = ConfigUtils.parseInterval(config.getString("removeOutdatedZonesInterval"));
		if (config.has("zoneOutdatedDuration"))
			module.zoneOutdatedDuration = ConfigUtils.parseInterval(config.getString("zoneOutdatedDuration"));
		if (config.has("contactsPerNode"))
			module.contactsPerNode = config.getInt("contactsPerNode");
		if (config.has("queries")) {
			JSONArray queries = config.getJSONArray("queries");
			for (Object queryObject : queries) {
				JSONObject query = (JSONObject)queryObject;
				String queryName = query.getString("name");
				String queryText = query.getString("query");
				if (validateQuery(queryName, queryText) == null) {
					ValueAndFreshness val = ValueAndFreshness.freshValue(new ValueString(queryText));
					module.queries.put(new Attribute(queryName), val);
				}
			}
		}
		return module;
	}
	
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

	public ZMIModule(ZMI zmi, PathName name, ValueSet fallbackContacts, String pubKeyFilename) {
		this.messages = new LinkedBlockingQueue<>();
		this.random = new Random();
		this.queries = new HashMap<>();
		this.zmi = zmi;
		this.name = name;
		this.fallbackContacts = ValueAndFreshness.freshValue(fallbackContacts);
		try {
			this.signVerifier = new Signer(Signer.Mode.SIGN_VERIFIER, pubKeyFilename, null);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void run() {
		scheduleQueriesExecution();
		scheduleRemoveOutdatedZonesEvent();
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
				if (message.type == ZMIMessage.Type.GET_LOCAL_AGENT_DATA) {
					DisseminationMessage msg = DisseminationMessage.localAgentData(message.pid, zmi.clone(), fallbackContacts, queries);
					modulesHandler.enqueue(msg);
					continue;
				}
				if (message.type == ZMIMessage.Type.UPDATE_WITH_REMOTE_DATA) {
					updateWithRemoteData(message.remoteData);
					continue;
				}
				if (message.type == ZMIMessage.Type.REMOVE_OUTDATED_ZONES) {
					removeOutdatedZones();
					scheduleRemoveOutdatedZonesEvent();
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
							setZoneAttributes((ValueString) message.value1, (ValueTime)message.value2, message.attributes);
							break;
						case INSTALL_QUERIES:
							installQuery(((ValueString)message.value1).getValue(), message.valueAndFreshness, ((ValueString) message.value2).getValue());
							break;
						case UNINSTALL_QUERIES:
							uninstallQuery(((ValueString)message.value1).getValue(), (ValueTime) message.value2, ((ValueString) message.value3).getValue());
							break;
						case GET_ALL_QUERIES:
							response.queries = new HashMap<>(queries); // shallow copy is just fine (we only read once created keys and values)
							break;
						case GET_FALLBACK_CONTACTS:
							response.value1 = getFallbackContacts();
							break;
						case SET_FALLBACK_CONTACTS:
							setFallbackContacts(message.valueAndFreshness);
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
			} catch (Exception ex) {
				Logger.getLogger(ZMIModule.class.getName()).log(Level.SEVERE, null, ex);
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
		return findZoneOrError(zmi, zone.getValue()).getAttributes();
	}

	private void installQuery(String name, ValueAndFreshness query, String signature) throws RemoteException {
		String queryRaw = ((ValueString)query.getVal()).getValue();
		String errorMsg = validateQuery(name, queryRaw);
		if (errorMsg != null) {
			throw new RmiCallException(errorMsg);
		}

		QueryOperation queryOp = QueryOperation.newQueryInstall(name, queryRaw);
		if (!signVerifier.verifyQueryOperationSignature(queryOp, signature)) {
			throw new RmiCallException("SecurityError: Invalid signatures for queries.");
		}
		Attribute attribute = new Attribute(name);
		if (queries.getOrDefault(attribute, null) != null) {
			throw new RmiCallException("Query was already installed");
		}

		queries.put(attribute, query);
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

	private void uninstallQuery(String name, ValueTime freshness, String signature) throws RemoteException {
		Attribute attribute = new Attribute(name);

		QueryOperation queryOp = QueryOperation.newQueryUninstall(name);
		if (!signVerifier.verifyQueryOperationSignature(queryOp, signature)) {
			throw new RmiCallException("SecurityError: Invalid signatures for queries.");
		}

		if (!queryInstalled(attribute)) {
			throw new RmiCallException("Query not installed.");
		}
		removeQuery(attribute, freshness);
	}

	private void setZoneAttributes(ValueString zone, ValueTime freshness, AttributesMap attributes) throws RemoteException {
		ZMI zoneZmi = findZoneOrError(zmi, zone.getValue());
		if (!zoneZmi.getSons().isEmpty()) {
			throw new IllegalArgumentException("setZoneAttributes is only allowed for singleton zone.");
		}
		zoneZmi.getAttributes().addOrChange(attributes);
		zoneZmi.setFreshness(freshness);
	}

	private void setFallbackContacts(ValueAndFreshness contacts) throws RemoteException {
		fallbackContacts = contacts;
	}

	private ValueSet getFallbackContacts() throws RemoteException {
		return (ValueSet)fallbackContacts.getVal();
	}

	private void executeQueries() throws Exception {
		executeQueries(zmi);
	}

	private void executeQueries(ZMI zmi) throws Exception {
		if (!zmi.getSons().isEmpty()) {
			for (ZMI son : zmi.getSons()) {
				executeQueries(son);
			}
			Interpreter interpreter = new Interpreter(zmi);
			for (ValueAndFreshness queryAndTs : queries.values()) {
				String query = ((ValueString)queryAndTs.getVal()).getValue();
				if (query.isEmpty()) {
					continue;
				}
				try {
					Program program = tryParse(query);
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
	
	public static Set<String> extractAttributesCreatedByQuery(String query) throws Exception {
		Program program = tryParse(query);
		return AttributesExtractor.extractAttributes(program);
	}

	private static Program tryParse(String query) throws Exception {
		Yylex lex = new Yylex(new ByteArrayInputStream(query.getBytes()));
		return (new parser(lex)).pProgram();
	}

	private static ZMI findZoneOrError(ZMI zmi, String name) throws RemoteException {
		zmi = findZone(zmi, name);
		if (zmi == null) {
			throw new RmiCallException("Zone not found.");
		}
		return zmi;
	}
	
	public static ZMI findZone(ZMI zmi, String name) {
		return findZone(zmi, new PathName(name));
	}

	public static ZMI findZone(ZMI zmi, PathName pathName) {
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
		return null;
	}
	
	public static void removeInfoUnrelevantForTheOther(ZMI zmi, PathName name, PathName otherName) {
		while (otherName.startsWith(zmi.getPathName())) {
			// Remove attributes if we are on the path from other node to the root.
			if (otherName.startsWith(zmi.getPathName()))
				zmi.removeAllAttributesExceptContacts();
			ZMI nextZmi = null;
			for (ZMI node : zmi.getSons()) {
				// Do not send attributs about the other zone.
				if (otherName.startsWith(node.getPathName()))
					node.removeAllAttributesExceptContacts();
				if (name.startsWith(node.getPathName()))
					nextZmi = node;
			}
			if (nextZmi == null)
				break;
			zmi = nextZmi;
		}
		// We passed LCA of our node and the other node. Remove unrelevant nodes.
		zmi.removeSons();
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

	private boolean queryInstalled(Attribute attribute) {
		ValueAndFreshness query = queries.getOrDefault(attribute, null);
		if (query == null) {
			return false;
		}
		String queryRaw = ((ValueString)query.getVal()).getValue();
		return !queryRaw.isEmpty();
	}

	private void removeQuery(Attribute attribute, ValueTime freshness) {
		ValueString empty = new ValueString("");
		ValueAndFreshness removed = new ValueAndFreshness(empty, freshness);
		queries.put(attribute, removed);
	}

	private void updateWithRemoteData(AgentData remoteData) {
		fallbackContacts.update(remoteData.getFallbackContacts());
		for (Entry<Attribute, ValueAndFreshness> q : remoteData.getQueries().entrySet()) {
			ValueAndFreshness v = queries.getOrDefault(q.getKey(), null);
			if (v == null) {
				queries.put(q.getKey(), q.getValue());
			} else {
				v.update(q.getValue());
			}
		}
		updateWithRemoteData(zmi, remoteData.getZmi());
	}
	
	private void updateWithRemoteData(ZMI zmi, ZMI remoteZmi) {
		ValueSet contacts = null;
		for (ZMI remoteSon : remoteZmi.getSons()) {
			ZMI localSon = zmi.getSonBySingletonName(remoteSon.getPathName().getSingletonName());
			if (localSon == null) {
				zmi.addSon(remoteSon);
			} else {
				if (name.startsWith(remoteSon.getPathName()))
					contacts = (ValueSet)remoteSon.getAttributes().getOrNull("contacts");
				localSon.updateEachOtherContacts(remoteSon, contactsPerNode);
				if (!name.startsWith(remoteSon.getPathName())) // Update only siblings.
					localSon.updateAttributes(remoteSon);
			}
		}

		// Process next level
		ZMI nextZmi = null;
		for (ZMI localSon : zmi.getSons())
			if (name.startsWith(localSon.getPathName()))
				nextZmi = localSon;
		if (nextZmi == null)
			return;
		
		ZMI nextRemoteZmi = remoteZmi.getSonBySingletonName(nextZmi.getPathName().getSingletonName());
		if (nextRemoteZmi != null)
			updateWithRemoteData(nextZmi, nextRemoteZmi);
		
		addZMINodesFromContacts(contacts);
	}

	private void addZMINode(ZMI remoteZmi) {
		PathName fatherPath = remoteZmi.getPathName().levelUp();
		ZMI father = findZone(zmi, fatherPath);
		if (father == null) {
			throw new IllegalArgumentException(
					"You can only add sons of existing nodes");
		}
		father.addSon(remoteZmi);
	}
	
	private void scheduleRemoveOutdatedZonesEvent() {
		ZMIMessage innerMsg = ZMIMessage.removeOutdatedZones();
		TimerMessage msg = TimerMessage.scheduleOneTimeCallback(random.nextLong(), removeOutdatedZonesInterval, innerMsg);
		try {
			modulesHandler.enqueue(msg);
		} catch (InterruptedException ex) {
			Logger.getLogger(ZMIModule.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private void removeOutdatedZones() {
		System.out.println("Remove outdated zones event");
		ValueTime minTime = (ValueTime)ValueTime.now().subtract(new ValueDuration(0, zoneOutdatedDuration.toMillis()));
		removeOutdatedZones(zmi, minTime);
	}

	private void removeOutdatedZones(ZMI zmi, ValueTime minTime) {
		List<ZMI> toRemove = new ArrayList<>();
		ZMI nextZmi = null;
		for (ZMI son : zmi.getSons()) {
			if (name.startsWith(son.getPathName())) {
				nextZmi = son;
			} else if (son.getFreshness().isLowerThan(minTime).getValue()) {
				toRemove.add(son);
			}
		}
		for (ZMI son : toRemove) {
			System.out.println("Removing outadet zone " + son.getPathName());
			zmi.removeSon(son);
		}
		if (nextZmi != null) {
			removeOutdatedZones(nextZmi, minTime);
		}
	}

	private void addZMINodesFromContacts(ValueSet contacts) {
		if (contacts == null)
			return;
		for (Value con : contacts) {
			ValueContact contact = (ValueContact)con;
			if (contact == null)
				continue;
			addZMINodeFromContact(zmi, contact);
		}
	}
	
	private void addZMINodeFromContact(ZMI zmi, ValueContact contact) {
		List<String> components = contact.getName().getComponents();
		for (int i = components.size() - 1; i >= 1; i--) {
			PathName nodeName = new PathName(components.subList(0, i));
			ZMI node = findZone(zmi, nodeName);
			PathName fatherName = new PathName(components.subList(0, i - 1));
			ZMI father = findZone(zmi, fatherName);
			if (node == null && father != null) {
				node = new ZMI(father, components.get(i-1));
				father.addSon(node);
				
				List<Value> list = Arrays.asList(new Value[]{contact});
				ValueSet contacts = new ValueSet (new HashSet<>(list), TypePrimitive.CONTACT);
				node.getAttributes().add("contacts", contacts);
			}
		}
	}
}
