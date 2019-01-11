/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sr_labs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.agent.ZMIModule;
import pl.edu.mimuw.cloudatlas.agent.dissemination.NodeSelector;
import pl.edu.mimuw.cloudatlas.fetcher.Fetcher;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.PathName;

import pl.edu.mimuw.cloudatlas.model.ValueDuration;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueBoolean;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueDouble;
import pl.edu.mimuw.cloudatlas.model.ValueInt;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.model.ZMIHierarchyBuilder;
import static pl.edu.mimuw.cloudatlas.model.ZMIHierarchyBuilder.createContact;
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;
import pl.edu.mimuw.cloudatlas.model.ZMISerializer;
import pl.edu.mimuw.cloudatlas.signer.QueryOperation;
import pl.edu.mimuw.cloudatlas.signer.SignerInterface;

/**
 *
 * @author Mrowqa
 */
public class Sr_labs {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws ParseException, UnknownHostException, IOException, ClassNotFoundException {		
		//testZMIModuleUtilFunctions();
		testNodeSelection();
		//testSerialize();
		//testSerializeJSON();
		//testCloudAtlasAgent();
		//testFetcherDataCollection();
	}

	private static void testSerialize() throws ParseException, UnknownHostException, IOException {
		ZMI root = new ZMI();
		ZMI warsaw = new ZMI(root, "warsaw");
		root.addSon(warsaw);
		ZMI roomA = new ZMI(warsaw, "roomA");
		warsaw.addSon(roomA);
		ZMI roomB = new ZMI(warsaw, "roomB");
		warsaw.addSon(roomB);

		AttributesMap attrs = roomB.getAttributes();
		attrs.add("vBool", new ValueBoolean(true));
		attrs.add("vDouble", new ValueDouble(4.5));
		attrs.add("vDuration", new ValueDuration("+0 01:02:03.004"));
		ValueContact contact = new ValueContact(new PathName("/warsaw/roomB"), InetAddress.getByName("localhost"));
		ValueSet set = new ValueSet(TypePrimitive.CONTACT);
		set.add(contact); // ------------------------------------------------------------------------------------------------- WHY IT DOESN'T WORK ?
		//System.out.println(set.toString());
		//System.out.println(set.getValue());
		attrs.addOrChange("contacts", set);
		ValueList list = new ValueList(TypePrimitive.BOOLEAN);
		list.add(new ValueBoolean(false));
		list.add(new ValueBoolean(null));
		attrs.add("vList", list);

		System.out.println(root.toString());
		System.out.println(roomB.toString());

		byte[] serialized = ZMISerializer.SerializeZMI(root);

		System.out.println("---------------------------");
		System.out.println("len: " + serialized.length + ", content: " + serialized.toString());

		ZMI newRoot = ZMISerializer.DeserializeZMI(serialized);

		System.out.println("---------------------------");
		System.out.println(newRoot.toString());
	}

	private static void testSerializeJSON() throws ParseException, UnknownHostException {
		ZMI zmi = ZMIHierarchyBuilder.createDefaultHierarchy();
		List<AttributesMap> attrsMapList = new ArrayList<>();
		collectAttributesMaps(zmi, attrsMapList);
		for (AttributesMap attrs : attrsMapList) {
			String serialized = ZMIJSONSerializer.attributesMapToJSONString(attrs);
			String doubleSerialized = ZMIJSONSerializer.attributesMapToJSONString(ZMIJSONSerializer.JSONStringToAttributesMap(serialized));
			assert (serialized.equals(doubleSerialized));
		}
		System.out.println("OK");
	}

	private static void testZMIModuleUtilFunctions() throws ParseException, UnknownHostException {
		ZMI zmi = ZMIHierarchyBuilder.createTestHierarchy();
		PathName name = new PathName("/uw/violet07");
		PathName otherName = new PathName("/uw/khaki13");
		ZMIModule.removeInfoUnrelevantForTheOther(zmi, name, otherName);
	}
	
	private static void collectAttributesMaps(ZMI zmi, List<AttributesMap> attrs) {
		attrs.add(zmi.getAttributes());
		for (ZMI son : zmi.getSons()) {
			collectAttributesMaps(son, attrs);
		}
	}

	private static void testCloudAtlasAgent() {
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			CloudAtlasInterface stubZmi = (CloudAtlasInterface) registry.lookup("CloudAtlas");
			SignerInterface stubSigner = (SignerInterface) registry.lookup("CloudAtlasSigner");
			Scanner scanner = new Scanner(System.in);
			scanner.useDelimiter("\\n");
			printTestCloudAtlasMenu();
			while (scanner.hasNext()) {
				int op = scanner.nextInt();
				String arg, query;
				try {
					switch (op) {
						case 1:
							ValueList zones = stubZmi.getZones();
							for (Value zone : zones) {
								System.out.println(((ValueString) zone).getValue());
							}
							break;
						case 2:
							System.out.println("Enter zone:");
							arg = scanner.next();
							AttributesMap map = stubZmi.getZoneAttributes(new ValueString(arg));
							for (Entry<Attribute, Value> entry : map) {
								System.out.println(entry.getKey() + ": " + entry.getValue());
							}
							break;
						case 3:
							System.out.println("Enter zone:");
							arg = scanner.next();

							System.out.println("Enter attribute:");
							ValueString attribute = new ValueString(scanner.next());

							System.out.println("Enter value (only ValueInt)");
							ValueInt value = new ValueInt(scanner.nextLong());
							AttributesMap attrs = new AttributesMap();
							attrs.add(attribute.getValue(), value);
							stubZmi.setZoneAttributes(new ValueString(arg), attrs);
							System.out.println("Value set.");
							break;
						case 4:
							System.out.println("Enter query name (preceded with &):");
							arg = scanner.next();
							ValueList queryNames = new ValueList(new ArrayList<>(), TypePrimitive.STRING);
							queryNames.add(new ValueString(arg));

							System.out.println("Enter query:");
							query = scanner.next();
							ValueList queries = new ValueList(new ArrayList<>(), TypePrimitive.STRING);
							queries.add(new ValueString(query));

							String signature = stubSigner.signQueryOperation(QueryOperation.newQueryInstall(arg, query));
							ValueList signatures = new ValueList(new ArrayList<>(), TypePrimitive.STRING);
							signatures.add(new ValueString(signature));

							stubZmi.installQueries(queryNames, queries, signatures);
							System.out.println("Query installed.");
							break;
						case 5:
							System.out.println("Enter query name (preceded with &):");
							arg = scanner.next();
							ValueList queryNames2 = new ValueList(new ArrayList<>(), TypePrimitive.STRING);
							queryNames2.add(new ValueString(arg));

							String signature2 = stubSigner.signQueryOperation(QueryOperation.newQueryUninstall(arg));
							ValueList signatures2 = new ValueList(new ArrayList<>(), TypePrimitive.STRING);
							signatures2.add(new ValueString(signature2));

							stubZmi.uninstallQueries(queryNames2, signatures2);
							System.out.println("Query uninstalled.");
							break;
						case 6:
							System.out.println("Enter number of contacts:");
							int n = scanner.nextInt();
							ValueSet contacts = new ValueSet(new HashSet<>(), TypePrimitive.CONTACT);
							for (int i = 0; i < n; i++) {
								System.out.println("Enter path name of the contact:");
								arg = scanner.next();
								System.out.println("Enter address of the contact:");
								InetAddress address = InetAddress.getByName(scanner.next());
								contacts.add(new ValueContact(new PathName(arg), address));
							}
							stubZmi.setFallbackContacts(contacts);
							System.out.println("Fallback contacts set.");
							break;
						case 7:
							ValueSet contacts1 = stubZmi.getFallbackContacts();
							System.out.println("Fallback contacts:");
							for (Value contact : contacts1) {
								System.out.println(contact);
							}
							break;
						default:
							System.out.println("Unknown operation. Clossing.");
					}
				} catch (RemoteException e) {
					System.out.println("CloudAtlas exception: " + e.getMessage());
				}
			}
			scanner.close();
		} catch (Exception e) {
			System.err.println("CloudAtlas exception:");
			e.printStackTrace();
		}
	}

	private static void printTestCloudAtlasMenu() {
		System.out.println("1: Get zones.");
		System.out.println("2: Get attributes.");
		System.out.println("3: Set value for an attribute.");
		System.out.println("4: Install query.");
		System.out.println("5: Uninstall query.");
		System.out.println("6: Set fallback contacts.");
		System.out.println("7: Get fallback contacts.");
		System.out.println("Default: exit.");
	}

	private static void testFetcherDataCollection() {
		AttributesMap attrs = Fetcher.collectData();
		System.out.println(attrs);
	}
	
	private static void testNodeSelection() throws ParseException, UnknownHostException {
		int numRepeats = 100;
		PathName name = new PathName("/uw/violet07");
		ValueContact violet07Contact = createContact("/uw/violet07", (byte)1, (byte)1, (byte)1, (byte)10);
		ValueContact whatever01Contact = ZMIHierarchyBuilder.createContact("/pjwstk/whatever01", (byte)4, (byte)1, (byte)1, (byte)1);
		ValueContact whatever02Contact = ZMIHierarchyBuilder.createContact("/pjwstk/whatever02", (byte)5, (byte)1, (byte)1, (byte)1);
		List<Value> list = Arrays.asList(new Value[] {whatever01Contact, whatever02Contact, violet07Contact});
		
		ValueSet fallback = new ValueSet(new HashSet<>(list), TypePrimitive.CONTACT);
		ZMI zmi = ZMIHierarchyBuilder.createHierarchyForNodeSelectionTest(); // ZMIHierarchyBuilder.createLeafNodeHierarchy(name); 
		NodeSelector selector = NodeSelector.createByName("roundRobinExponential", name, NodeSelector.DEFAULT_SELECT_FALLBACK_PROBABILITY);
		
		HashMap<String, Integer> nodeCount = new HashMap<>();
		for (int i = 0; i < numRepeats; i++) {
			ValueContact contact = selector.selectNode(zmi, fallback);
			String nodeName = contact.getName().getName();
			nodeCount.compute(nodeName, (k, v) -> (v == null) ? 1 : v + 1);
			System.out.println(i + ": "+ contact);	
		}
		for (Entry<String, Integer> entry : nodeCount.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
}
