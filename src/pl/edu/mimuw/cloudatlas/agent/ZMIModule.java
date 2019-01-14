/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.agent;

import pl.edu.mimuw.cloudatlas.agent.dissemination.AgentData;
import pl.edu.mimuw.cloudatlas.agent.dissemination.DisseminationMessage;
import java.io.ByteArrayInputStream;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
import pl.edu.mimuw.cloudatlas.model.Query;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.TypeWrapper;
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
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;
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
	private final HashMap<Attribute, Query> queries;  // todo remember signatures; you can also patch webclient to display signatures -> basically you just change map type here, try to recompile, fix mismatching type errors, and try again :P
	private final Signer signVerifier;

	private ValueAndFreshness fallbackContacts;
	private Duration queryExecutionInterval = Duration.ofSeconds(5);
	private Duration removeOutdatedZonesInterval = Duration.ofSeconds(60); // Interval between two events of removing outdated zones
	private Duration zoneLivenessDuration = Duration.ofSeconds(60); // Duration after a zone become outdated
	private int contactsPerNode = 2;
	private ModulesHandler modulesHandler;

	public static ZMIModule createModule(ZMI zmi, JSONObject config) throws SocketException, UnknownHostException {
		PathName name = new PathName(config.getString("name"));
		String pubKeyFilename = config.getString("pubKeyFilename");
		ValueSet localContacts = createLocalContactsSet(name, config);
		ValueSet fallbackContacts = (ValueSet)ZMIJSONSerializer.JSONToValue(config.getJSONObject("fallbackContacts"));
		
		ZMIModule module = new ZMIModule(zmi, name, fallbackContacts, pubKeyFilename);
		findZone(zmi, name).getAttributes().add("contacts", localContacts);
		if (config.has("queryExecutionInterval"))
			module.queryExecutionInterval = ConfigUtils.parseInterval(config.getString("queryExecutionInterval"));
		if (config.has("removeOutdatedZonesInterval"))
			module.removeOutdatedZonesInterval = ConfigUtils.parseInterval(config.getString("removeOutdatedZonesInterval"));
		if (config.has("zoneLivenessDuration"))
			module.zoneLivenessDuration = ConfigUtils.parseInterval(config.getString("zoneLivenessDuration"));
		if (config.has("contactsPerNode"))
			module.contactsPerNode = config.getInt("contactsPerNode");
		if (config.has("queries")) {
			JSONArray queries = config.getJSONArray("queries");
			for (Object queryObject : queries) {
				JSONObject queryJSONObject = (JSONObject)queryObject;
				String queryName = queryJSONObject.getString("name");
				String queryText = queryJSONObject.getString("query");
				String signature = queryJSONObject.getString("signature");
				Query query = new Query(queryText, signature);				
				try {
					module.installQuery(queryName, query);
				} catch (Exception ex) {
					throw new IllegalArgumentException("Error installing query from configuration file. Exception: " + ex.getMessage());
				}
			}
		}
		return module;
	}
	
	private static ValueSet createLocalContactsSet(PathName name, JSONObject config) throws SocketException, UnknownHostException {
		InetAddress ip = null;
		if (config.has("localContactIp")) {
			ip = Inet4Address.getByName(config.getString("localContactIp"));
		} else {
			// Detect prefered outbound IP: https://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
			DatagramSocket socket = new DatagramSocket();
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			ip = socket.getLocalAddress();
		}
		int port = config.getInt("socketPort");
		
		ValueContact contactRaw = new ValueContact(name, ip, port);
		ValueAndFreshness contact = ValueAndFreshness.freshValue(contactRaw);
		List<Value> list = Arrays.asList(new Value[]{contact});
		return new ValueSet(new HashSet<>(list), new TypeWrapper(TypePrimitive.CONTACT));
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

	private ZMIModule(ZMI zmi, PathName name, ValueSet fallbackContacts, String pubKeyFilename) {
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
		try {
			scheduleQueriesExecutionRecurringEvent();
			scheduleRemoveOutdatedZonesRecurringEvent();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to schedule recurring ZMI actions. Occured exception: " + ex);
		}
		while (true) {
			try {
				ZMIMessage message = messages.take();
				if (message.type == ZMIMessage.Type.EXECUTE_QUERIES) {
					updateOurContactTimestamp();
					try {
						executeQueries();
					} catch(Exception ex) {
						Logger.getLogger(ZMIModule.class.getName()).log(Level.FINEST, "Queries exectued.");
					}
					continue;
				}
				if (message.type == ZMIMessage.Type.GET_LOCAL_AGENT_DATA) {
					DisseminationMessage msg = DisseminationMessage.localAgentData(message.pid, zmi.clone(), fallbackContacts, new HashMap(queries));
					modulesHandler.enqueue(msg);
					continue;
				}
				if (message.type == ZMIMessage.Type.UPDATE_WITH_REMOTE_DATA) {
					updateWithRemoteData(message.remoteData);
					continue;
				}
				if (message.type == ZMIMessage.Type.REMOVE_OUTDATED_ZONES) {
					removeOutdatedZones(zmi);
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
							installQuery(((ValueString)message.value1).getValue(), message.query);
							break;
						case UNINSTALL_QUERIES:
							uninstallQuery(((ValueString)message.value1).getValue(), message.query);
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

	private void installQuery(String name, Query query) throws RemoteException {
		String errorMsg = validateQuery(name, query.getText());
		if (errorMsg != null) {
			throw new RmiCallException(errorMsg);
		}

		QueryOperation queryOp = QueryOperation.newQueryInstall(name, query.getText());
		if (!signVerifier.verifyQueryOperationSignature(queryOp, query.getSignature())) {
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
		if (text.isEmpty()) {
			return "Query can't be empty.";
		}
		try {
			tryParse(text);
		} catch (Exception e) {
			return "Error parsing query: " + e.getMessage();
		}
		
		return null;
	}

	private void uninstallQuery(String name, Query query) throws RemoteException {
		Attribute attribute = new Attribute(name);

		QueryOperation queryOp = QueryOperation.newQueryUninstall(name);
		if (!signVerifier.verifyQueryOperationSignature(queryOp, query.getSignature())) {
			throw new RmiCallException("SecurityError: Invalid signatures for queries.");
		}

		if (!queryInstalled(attribute)) {
			throw new RmiCallException("Query not installed.");
		}
		queries.put(attribute, query);
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
		return (ValueSet)fallbackContacts.getValue();
	}

	private void executeQueries() throws Exception {
		executeQueries(zmi);
	}

	private void executeQueries(ZMI zmi) throws Exception {
		zmi.updateFreshness();
		if (!zmi.getSons().isEmpty()) {
			for (ZMI son : zmi.getSons())
				if (name.startsWith(son.getPathName()))
					executeQueries(son);
			Interpreter interpreter = new Interpreter(zmi);
			for (Query query : queries.values()) {
				String queryText = query.getText();
				if (queryText.isEmpty()) {
					continue;
				}
				try {
					Program program = tryParse(queryText);
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
		
	private void scheduleQueriesExecutionRecurringEvent() throws InterruptedException {
		long id = random.nextLong();
		ZMIMessage callbackMessage = ZMIMessage.executeQueries();
		TimerMessage message = TimerMessage.scheduleRecurringCallback(id, queryExecutionInterval, callbackMessage);
		modulesHandler.enqueue(message);
	}

	private boolean queryInstalled(Attribute attribute) {
		Query query = queries.getOrDefault(attribute, null);
		if (query == null) {
			return false;
		}
		String queryText = query.getText();
		return !queryText.isEmpty();
	}

	private void updateWithRemoteData(AgentData remoteData) {
		fallbackContacts = fallbackContacts.getFresher(remoteData.getFallbackContacts());
		for (Entry<Attribute, Query> entry : remoteData.getQueries().entrySet()) {
			String remoteQueryName = entry.getKey().getName();
			Query remoteQuery = entry.getValue();
			if (entry.getValue().isNull())
				continue;
			
			QueryOperation operation = null;
			if (remoteQuery.getText().isEmpty()) {
				operation = QueryOperation.newQueryUninstall(remoteQueryName);
			} else {
				operation = QueryOperation.newQueryInstall(remoteQueryName, remoteQuery.getText());
			}
			try {
				if (!signVerifier.verifyQueryOperationSignature(operation, remoteQuery.getSignature())) {
					continue; // Skip incorrect localQuery.
				}
			} catch (RemoteException ex) {
				Logger.getLogger(ZMIModule.class.getName()).log(Level.FINE, "Exception occured while verifing query. ", ex);
				continue;
			}

			// If local query has been uninstalled we do not let to install it again.
			Query localQuery = queries.getOrDefault(entry.getKey(), null);
			if (localQuery != null && localQuery.getText().isEmpty() && !remoteQuery.getText().isEmpty())
				continue;
			
			queries.merge(entry.getKey(), remoteQuery, (q1, q2) -> q1.getFresher(q2));
		}
		updateZmiWithRemoteData(zmi, remoteData.getZmi());
	}
	
	private void updateZmiWithRemoteData(ZMI zmi, ZMI remoteZmi) {
		ValueSet contacts = null;
		for (ZMI remoteSon : remoteZmi.getSons()) {
			// Add remote son if does not exist locally and is not outdated.
			ZMI localSon = zmi.getSonBySingletonName(remoteSon.getPathName().getSingletonName());
			if (localSon == null) {
				if (!isZmiOutdated(remoteSon.getFreshness())) {
					Logger.getLogger(ZMIModule.class.getName()).log(Level.INFO, "Adding zone from gossiping {0}", new Object[]{remoteSon.getPathName()});
					zmi.addSon(remoteSon);
					remoteSon.setFather(zmi);
				}
			} else {
				if (name.startsWith(remoteSon.getPathName()))
					contacts = (ValueSet)remoteSon.getAttributes().getOrNull("contacts");
				localSon.updateEachOtherContacts(remoteSon, contactsPerNode);
				if (!name.startsWith(remoteSon.getPathName())) // Update only siblings.
					localSon.updateAttributesIfFresher(remoteSon);
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
			updateZmiWithRemoteData(nextZmi, nextRemoteZmi);
		
		addZMINodesFromContacts(contacts);
	}
	
	private void scheduleRemoveOutdatedZonesRecurringEvent() throws InterruptedException {
		ZMIMessage innerMsg = ZMIMessage.removeOutdatedZones();
		TimerMessage msg = TimerMessage.scheduleRecurringCallback(random.nextLong(), removeOutdatedZonesInterval, innerMsg);
		modulesHandler.enqueue(msg);
	}
	
	private void removeOutdatedZones(ZMI zmi) {
		List<ZMI> toRemove = new ArrayList<>();
		ZMI nextZmi = null;
		for (ZMI son : zmi.getSons()) {
			if (name.startsWith(son.getPathName())) {
				nextZmi = son;
			} else if (isZmiOutdated(son.getFreshness())) {
				toRemove.add(son);
			}
		}
		for (ZMI son : toRemove) {
			Logger.getLogger(ZMIModule.class.getName()).log(Level.INFO, "Removing outdated zone {0}", new Object[]{son.getPathName()});
			zmi.removeSon(son);
		}
		if (nextZmi != null) {
			removeOutdatedZones(nextZmi);
		}
	}

	private void addZMINodesFromContacts(ValueSet contacts) {
		if (contacts == null)
			return;
		for (Value contact : contacts) {
			if (contact instanceof ValueAndFreshness) {
				ValueAndFreshness contactWithTs = (ValueAndFreshness)contact;
				// Create ZMI node from contact only if contact is fresh enough.
				if (!isZmiOutdated(contactWithTs.getFreshness()))
					addZMINodeFromContact(zmi, contactWithTs);
			}
		}
	}
	
	private void addZMINodeFromContact(ZMI zmi, ValueAndFreshness contactWithTs) {
		ValueContact contact = (ValueContact)contactWithTs.getValue();
		ValueTime freshness = contactWithTs.getFreshness();
		List<String> components = contact.getName().getComponents();
		for (int i = components.size() - 1; i >= 1; i--) {
			PathName fatherName = new PathName(components.subList(0, i));
			ZMI father = findZone(zmi, fatherName);
			if (father == null)
				continue;
			
			PathName nodeName = new PathName(components.subList(0, i+1));
			ZMI node = findZone(zmi, nodeName);
			if (node == null) {
				node = new ZMI(father, nodeName.getSingletonName(), freshness);
				father.addSon(node);
				Logger.getLogger(ZMIModule.class.getName()).log(Level.INFO, "Adding zone from contacts attribute {0}", new Object[]{node.getPathName()});

				List<Value> list = Arrays.asList(new Value[]{contactWithTs});
				ValueSet contacts = new ValueSet (new HashSet<>(list), new TypeWrapper(TypePrimitive.CONTACT));
				node.getAttributes().add("contacts", contacts);
				break;
			}
		}
	}
	
	private boolean isZmiOutdated(ValueTime zmiFreshness) {
		return zmiFreshness.isLowerThan(getZoneOutdatedThreshold()).getValue();
	}
	
	private ValueTime getZoneOutdatedThreshold() {
		return (ValueTime)ValueTime.now().subtract(new ValueDuration(0, zoneLivenessDuration.toMillis()));
	}
	
	private void updateOurContactTimestamp() {
		ZMI zone = findZone(zmi, name);
		ValueSet contacts = (ValueSet)zone.getAttributes().get("contacts");
		ValueTime freshness = ValueTime.now();
		ValueSet updated = new ValueSet(new TypeWrapper(TypePrimitive.CONTACT));
		for (Value contact : contacts) {
			ValueAndFreshness contactWithTs = (ValueAndFreshness)contact;
			updated.add(new ValueAndFreshness(contactWithTs.getValue(), freshness));
		}
		zone.getAttributes().addOrChange("contacts", updated);
	}
}
