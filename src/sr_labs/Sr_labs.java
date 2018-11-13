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
import java.util.Date;
import java.util.Map.Entry;
import java.util.Scanner;
import javafx.util.Pair;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasAgent;
import pl.edu.mimuw.cloudatlas.agent.CloudAtlasInterface;
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
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;
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
		testCloudAtlasAgent();
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
	
	private static void testCloudAtlasAgent() {
		try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            CloudAtlasInterface stub = (CloudAtlasInterface) registry.lookup("CloudAtlas");
			Scanner scanner = new Scanner(System.in);
			scanner.useDelimiter("\\n");
			System.out.println("Option 0 - install query, 1 - uninstall query, 2 - get zones, 3 - get zone attributes");
			while(scanner.hasNext()) {
				int op = scanner.nextInt();
				String arg;
				try {
				switch (op) {
					case 0: 
						System.out.println("Enter query:");
						arg = scanner.next();
						stub.installQuery(new ValueString(arg));
						break;
					case 1:
						System.out.println("Enter query:");
						arg = scanner.next();
						stub.uninstallQuery(new ValueString(arg));
						break;
					case 2: 
						ValueList zones = stub.getZones();
						for (Value zone : zones) {
							System.out.println(((ValueString)zone).getValue());
						}
						break;
					case 3: 
						System.out.println("Enter zone:");
						arg = scanner.next();
						AttributesMap map = stub.getAttributes(new ValueString(arg));
						for (Entry<Attribute, Value> entry : map) {
							System.out.println(entry.getKey() + ": " + entry.getValue());
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
}
