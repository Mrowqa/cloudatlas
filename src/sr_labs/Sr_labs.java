/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sr_labs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import javafx.util.Pair;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
import pl.edu.mimuw.cloudatlas.fetcher.Fetcher;
import static pl.edu.mimuw.cloudatlas.interpreter.Main.executeQueries;
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
import pl.edu.mimuw.cloudatlas.model.ValueTime;
import pl.edu.mimuw.cloudatlas.model.ZMI;
import pl.edu.mimuw.cloudatlas.model.ZMIJSONSerializer;
import pl.edu.mimuw.cloudatlas.model.ZMISerializer;

/**
 *
 * @author Mrowqa
 */
public class Sr_labs {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParseException, UnknownHostException, IOException {
		//testSerialize();
		testSerializeJSON();
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
		
		root.updateAttributes();
		
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
		System.out.println(ZMIJSONSerializer.toJSON(createTestHierarchy()));
	}
	
	private static void testCloudAtlasAgent() {
		try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            CloudAtlasInterface stub = (CloudAtlasInterface) registry.lookup("CloudAtlas");
			Scanner scanner = new Scanner(System.in);
			scanner.useDelimiter("\\n");
			printTestCloudAtlasMenu();
			while(scanner.hasNext()) {
				int op = scanner.nextInt();
				String arg, query;
				try {
				switch (op) {
					case 1: 
						ValueList zones = stub.getZones();
						for (Value zone : zones) {
							System.out.println(((ValueString)zone).getValue());
						}
						break;
					case 2: 
						System.out.println("Enter zone:");
						arg = scanner.next();
						AttributesMap map = stub.getZoneAttributes(new ValueString(arg));
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
						stub.setZoneAttributes(new ValueString(arg), attrs);
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
						
						stub.installQueries(queryNames, queries);
						System.out.println("Query installed.");
						break;
					case 5:
						System.out.println("Enter query name (preceded with &):");
						arg = scanner.next();
						ValueList queryNames2 = new ValueList(new ArrayList<>(), TypePrimitive.STRING);
						queryNames2.add(new ValueString(arg));
						
						stub.uninstallQueries(queryNames2);
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
						stub.setFallbackContacts(contacts);
						System.out.println("Fallback contacts set.");
						break;
					case 7:
						ValueSet contacts1 = stub.getFallbackContacts();
						System.out.println("Fallback contacts:");
						for (Value contact : contacts1) {
							System.out.println(contact);
						}
						break;
					default:
						System.out.println("Unknown operation. Clossing.");
				}
				} catch(RemoteException e) {
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
	
	
	// TODO: extract this test hierarchy and avoid copy-paste!
	private static ValueContact createContact(String path, byte ip1, byte ip2, byte ip3, byte ip4)
			throws UnknownHostException {
		return new ValueContact(new PathName(path), InetAddress.getByAddress(new byte[] {
			ip1, ip2, ip3, ip4
		}));
	}
	
	private static ZMI createTestHierarchy() throws ParseException, UnknownHostException {
		ZMI root;
		ValueContact violet07Contact = createContact("/uw/violet07", (byte)10, (byte)1, (byte)1, (byte)10);
		ValueContact khaki13Contact = createContact("/uw/khaki13", (byte)10, (byte)1, (byte)1, (byte)38);
		ValueContact khaki31Contact = createContact("/uw/khaki31", (byte)10, (byte)1, (byte)1, (byte)39);
		ValueContact whatever01Contact = createContact("/uw/whatever01", (byte)82, (byte)111, (byte)52, (byte)56);
		ValueContact whatever02Contact = createContact("/uw/whatever02", (byte)82, (byte)111, (byte)52, (byte)57);
		
		List<Value> list;
				
		root = new ZMI();
		root.getAttributes().add("level", new ValueInt(0l));
		
		ZMI uw = new ZMI(root, "uw");
		root.addSon(uw);
		uw.getAttributes().add("level", new ValueInt(1l));
		
		ZMI pjwstk = new ZMI(root, "pjwstk");
		root.addSon(pjwstk);
		pjwstk.getAttributes().add("level", new ValueInt(1l));
		
		ZMI violet07 = new ZMI(uw, "violet07");
		uw.addSon(violet07);
		violet07.getAttributes().add("level", new ValueInt(2l));
		violet07.getAttributes().add("owner", new ValueString("/uw/violet07"));
		violet07.getAttributes().add("timestamp", new ValueTime("2012/11/09 18:00:00.000"));
		list = Arrays.asList(new Value[] {
			khaki31Contact, whatever01Contact
		});
		violet07.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		violet07.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			violet07Contact,
		});
		violet07.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		violet07.getAttributes().add("creation", new ValueTime("2011/11/09 20:8:13.123"));
		violet07.getAttributes().add("cpu_usage", new ValueDouble(0.9));
		violet07.getAttributes().add("num_cores", new ValueInt(3l));
		violet07.getAttributes().add("num_processes", new ValueInt(13l));
		violet07.getAttributes().add("has_ups", new ValueBoolean(null));
		list = Arrays.asList(new Value[] {
			new ValueString("tola"), new ValueString("tosia"),
		});
		violet07.getAttributes().add("some_names", new ValueList(list, TypePrimitive.STRING));
		violet07.getAttributes().add("expiry", new ValueDuration(13l, 12l, 0l, 0l, 0l));
		
		ZMI khaki31 = new ZMI(uw, "khaki31");
		uw.addSon(khaki31);
		khaki31.getAttributes().add("level", new ValueInt(2l));
		khaki31.getAttributes().add("owner", new ValueString("/uw/khaki31"));
		khaki31.getAttributes().add("timestamp", new ValueTime("2012/11/09 20:03:00.000"));
		list = Arrays.asList(new Value[] {
			violet07Contact, whatever02Contact,
		});
		khaki31.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		khaki31.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			khaki31Contact
		});
		khaki31.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		khaki31.getAttributes().add("creation", new ValueTime("2011/11/09 20:12:13.123"));
		khaki31.getAttributes().add("cpu_usage", new ValueDouble(null));
		khaki31.getAttributes().add("num_cores", new ValueInt(3l));
		khaki31.getAttributes().add("num_processes", new ValueInt(124L));
		khaki31.getAttributes().add("has_ups", new ValueBoolean(false));
		list = Arrays.asList(new Value[] {
			new ValueString("agatka"), new ValueString("beatka"), new ValueString("celina"),
		});
		khaki31.getAttributes().add("some_names", new ValueList(list, TypePrimitive.STRING));
		khaki31.getAttributes().add("expiry", new ValueDuration(-13l, -11l, 0l, 0l, 0l));
		
		ZMI khaki13 = new ZMI(uw, "khaki13");
		uw.addSon(khaki13);
		khaki13.getAttributes().add("level", new ValueInt(2l));
		khaki13.getAttributes().add("owner", new ValueString("/uw/khaki13"));
		khaki13.getAttributes().add("timestamp", new ValueTime("2012/11/09 21:03:00.000"));
		list = Arrays.asList(new Value[] {});
		khaki13.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		khaki13.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			khaki13Contact,
		});
		khaki13.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		khaki13.getAttributes().add("creation", new ValueTime((Long)null));
		khaki13.getAttributes().add("cpu_usage", new ValueDouble(0.1));
		khaki13.getAttributes().add("num_cores", new ValueInt(null));
		khaki13.getAttributes().add("num_processes", new ValueInt(107L));
		khaki13.getAttributes().add("has_ups", new ValueBoolean(true));
		list = Arrays.asList(new Value[] {});
		khaki13.getAttributes().add("some_names", new ValueList(list, TypePrimitive.STRING));
		khaki13.getAttributes().add("expiry", new ValueDuration((Long)null));
				
		ZMI whatever01 = new ZMI(pjwstk, "whatever01");
		pjwstk.addSon(whatever01);
		whatever01.getAttributes().add("level", new ValueInt(2l));
		whatever01.getAttributes().add("owner", new ValueString("/pjwstk/whatever01"));
		whatever01.getAttributes().add("timestamp", new ValueTime("2012/11/09 21:12:00.000"));
		list = Arrays.asList(new Value[] {
			violet07Contact, whatever02Contact,
		});
		whatever01.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		whatever01.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			whatever01Contact,
		});
		whatever01.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		whatever01.getAttributes().add("creation", new ValueTime("2012/10/18 07:03:00.000"));
		whatever01.getAttributes().add("cpu_usage", new ValueDouble(0.1));
		whatever01.getAttributes().add("num_cores", new ValueInt(7l));
		whatever01.getAttributes().add("num_processes", new ValueInt(215L));
		list = Arrays.asList(new Value[] {
			new ValueString("rewrite")
		});
		whatever01.getAttributes().add("php_modules", new ValueList(list, TypePrimitive.STRING));
				
		ZMI whatever02 = new ZMI(pjwstk, "whatever02");
		pjwstk.addSon(whatever02);
		whatever02.getAttributes().add("level", new ValueInt(2l));
		whatever02.getAttributes().add("owner", new ValueString("/uw/whatever02"));
		whatever02.getAttributes().add("timestamp", new ValueTime("2012/11/09 21:13:00.000"));
		list = Arrays.asList(new Value[] {
			khaki31Contact, whatever01Contact,
		});
		whatever02.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		whatever02.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			whatever02Contact,
		});
		whatever02.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		whatever02.getAttributes().add("creation", new ValueTime("2012/10/18 07:04:00.000"));
		whatever02.getAttributes().add("cpu_usage", new ValueDouble(0.4));
		whatever02.getAttributes().add("num_cores", new ValueInt(13l));
		whatever02.getAttributes().add("num_processes", new ValueInt(222L));
		list = Arrays.asList(new Value[] {
			new ValueString("odbc")
		});
		whatever02.getAttributes().add("php_modules", new ValueList(list, TypePrimitive.STRING));
		
		return root;
	}
}
