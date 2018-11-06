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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import pl.edu.mimuw.cloudatlas.model.AttributesMap;
import pl.edu.mimuw.cloudatlas.model.PathName;

import pl.edu.mimuw.cloudatlas.model.ValueDuration;
import pl.edu.mimuw.cloudatlas.model.ValueString;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.ValueBoolean;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueDouble;
import pl.edu.mimuw.cloudatlas.model.ValueList;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;
//import pl.edu.mimuw.cloudatlas.model.ZMISerde;

/**
 *
 * @author Mrowqa
 */
public class Sr_labs {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParseException, UnknownHostException, IOException {
        // TODO code application logic here
		
		testSerialize();
		//ValueDuration b = new ValueDuration("-2 23:00:00.005");
		//System.out.println(((ValueString)b.convertTo(TypePrimitive.STRING)).getValue());
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
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		//ZMISerde.SerializeZMI(outputStream, root);
		outputStream.close();
		byte[] serialized = outputStream.toByteArray();
		
		System.out.println("---------------------------");
		System.out.println("len: " + serialized.length + ", content: " + serialized.toString());
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
		//ZMI newRoot = ZMISerde.DeserializeZMI(inputStream);
		
		System.out.println("---------------------------");
		//System.out.println(newRoot.toString());
	}
}
