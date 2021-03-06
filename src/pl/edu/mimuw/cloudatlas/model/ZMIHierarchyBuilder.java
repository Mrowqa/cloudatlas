/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author pawel
 */
public class ZMIHierarchyBuilder {
	public static ValueContact createContact(String path, byte ip1, byte ip2, byte ip3, byte ip4)
			throws UnknownHostException {
		return new ValueContact(new PathName(path), InetAddress.getByAddress(new byte[] {
			ip1, ip2, ip3, ip4
		}));
	}
	
	public static ValueAndFreshness createContactWithTimestamp(String path, byte ip1, byte ip2, byte ip3, byte ip4)
			throws UnknownHostException {
		ValueContact value = new ValueContact(new PathName(path), InetAddress.getByAddress(new byte[] {
			ip1, ip2, ip3, ip4
		}));
		return ValueAndFreshness.freshValue(value);
	}
	
	public static ZMI createTestHierarchy() throws ParseException, UnknownHostException {
		ValueContact violet07Contact = createContact("/uw/violet07", (byte)10, (byte)1, (byte)1, (byte)10);
		ValueContact khaki13Contact = createContact("/uw/khaki13", (byte)10, (byte)1, (byte)1, (byte)38);
		ValueContact khaki31Contact = createContact("/uw/khaki31", (byte)10, (byte)1, (byte)1, (byte)39);
		ValueContact whatever01Contact = createContact("/uw/whatever01", (byte)82, (byte)111, (byte)52, (byte)56);
		ValueContact whatever02Contact = createContact("/uw/whatever02", (byte)82, (byte)111, (byte)52, (byte)57);
		
		List<Value> list;
		
		ZMI root = new ZMI();
		root.getAttributes().add("level", new ValueInt(0l));
		root.getAttributes().add("owner", new ValueString("/uw/violet07"));
		root.getAttributes().add("timestamp", new ValueTime("2012/11/09 20:10:17.342"));
		root.getAttributes().add("contacts", new ValueSet(TypePrimitive.CONTACT));
		root.getAttributes().add("cardinality", new ValueInt(0l));
		
		ZMI uw = new ZMI(root, "uw");
		root.addSon(uw);
		uw.getAttributes().add("level", new ValueInt(1l));
		uw.getAttributes().add("owner", new ValueString("/uw/violet07"));
		uw.getAttributes().add("timestamp", new ValueTime("2012/11/09 20:8:13.123"));
		uw.getAttributes().add("contacts", new ValueSet(TypePrimitive.CONTACT));
		uw.getAttributes().add("cardinality", new ValueInt(0l));
		
		ZMI pjwstk = new ZMI(root, "pjwstk");
		root.addSon(pjwstk);
		pjwstk.getAttributes().add("level", new ValueInt(1l));
		pjwstk.getAttributes().add("owner", new ValueString("/pjwstk/whatever01"));
		pjwstk.getAttributes().add("timestamp", new ValueTime("2012/11/09 20:8:13.123"));
		pjwstk.getAttributes().add("contacts", new ValueSet(TypePrimitive.CONTACT));
		pjwstk.getAttributes().add("cardinality", new ValueInt(0l));
		
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
		khaki13.getAttributes().add("has_ups", new ValueBoolean(true));
		list = Arrays.asList(new Value[] {});
		khaki13.getAttributes().add("some_names", new ValueList(list, TypePrimitive.STRING));
		khaki13.getAttributes().add("expiry", new ValueDuration((Long)null));
		
		ZMI whatever01 = new ZMI(pjwstk, "whatever01");
		pjwstk.addSon(whatever01);
		whatever01.getAttributes().add("level", new ValueInt(2l));
		whatever01.getAttributes().add("owner", new ValueString("/uw/whatever01"));
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
		list = Arrays.asList(new Value[] {
			new ValueString("odbc")
		});
		whatever02.getAttributes().add("php_modules", new ValueList(list, TypePrimitive.STRING));
		
		return root;
	}

	public static ZMI createHierarchyForNodeSelectionTest() throws ParseException, UnknownHostException {
		ValueContact violet07Contact = createContact("/uw/violet07", (byte)1, (byte)1, (byte)1, (byte)10);
		ValueContact khaki13Contact = createContact("/uw/khaki13", (byte)2, (byte)1, (byte)1, (byte)38);
		ValueContact khaki31Contact = createContact("/uw/khaki31", (byte)3, (byte)1, (byte)1, (byte)39);
		ValueContact whatever01Contact = createContact("/pjwstk/whatever01", (byte)4, (byte)1, (byte)1, (byte)1);
		ValueContact whatever02Contact = createContact("/pjwstk/whatever02", (byte)5, (byte)1, (byte)1, (byte)1);		
			
		List<Value> list;
		
		ZMI root = new ZMI();
		root.getAttributes().add("level", new ValueInt(0l));
		root.getAttributes().add("contacts", new ValueSet(TypePrimitive.CONTACT));
		
		ZMI uw = new ZMI(root, "uw");
		root.addSon(uw);
		uw.getAttributes().add("level", new ValueInt(1l));
		uw.getAttributes().add("owner", new ValueString("/uw/violet07"));
		list = Arrays.asList(new Value[]{violet07Contact});
		uw.getAttributes().add("contacts", new ValueSet(new HashSet<>(list), TypePrimitive.CONTACT));
	
		ZMI pjwstk = new ZMI(root, "pjwstk");
		root.addSon(pjwstk);
		list = Arrays.asList(new Value[]{whatever01Contact, whatever02Contact});
		pjwstk.getAttributes().add("level", new ValueInt(1l));
		pjwstk.getAttributes().add("contacts", new ValueSet(new HashSet<>(list),TypePrimitive.CONTACT));
		
		ZMI violet07 = new ZMI(uw, "violet07");
		uw.addSon(violet07);
		violet07.getAttributes().add("level", new ValueInt(2l));
		list = Arrays.asList(new Value[] {
			violet07Contact
		});
		violet07.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		
		ZMI khaki31 = new ZMI(uw, "khaki31");
		uw.addSon(khaki31);
		khaki31.getAttributes().add("level", new ValueInt(2l));
		khaki31.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), TypePrimitive.CONTACT));
		list = Arrays.asList(new Value[] {
			khaki31Contact
		});
		
		ZMI khaki13 = new ZMI(uw, "khaki13");
		uw.addSon(khaki13);
		khaki13.getAttributes().add("level", new ValueInt(2l));
		list = Arrays.asList(new Value[] {
			khaki13Contact,
		});
		khaki13.getAttributes().add("contacts", new ValueSet(new HashSet<>(list), TypePrimitive.CONTACT));


		return root;
	}
	
	public static ZMI createDefaultHierarchy() throws ParseException, UnknownHostException {
		ZMI root;
		Value uw1 = createContactWithTimestamp("/uw1", (byte)10, (byte)1, (byte)1, (byte)10);
		Value uw1a = createContactWithTimestamp("/uw1a", (byte)10, (byte)1, (byte)1, (byte)10);
		Value uw1b = createContactWithTimestamp("/uw1b", (byte)10, (byte)1, (byte)1, (byte)10);
		Value uw1c = createContactWithTimestamp("/uw1c", (byte)10, (byte)1, (byte)1, (byte)10);
				
		Value uw2a = createContactWithTimestamp("/uw2a", (byte)10, (byte)1, (byte)1, (byte)10);
		Value uw3a = createContactWithTimestamp("/uw3a", (byte)10, (byte)1, (byte)1, (byte)10);
		Value uw3b = createContactWithTimestamp("/uw3b", (byte)10, (byte)1, (byte)1, (byte)10);
		
		Value pj1 = createContactWithTimestamp("/pj1", (byte)10, (byte)1, (byte)1, (byte)10);
		Value pj2 = createContactWithTimestamp("/pj2", (byte)10, (byte)1, (byte)1, (byte)10);
		Type contactType = new TypeWrapper(TypePrimitive.CONTACT);
		
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
			uw1a, uw1b, uw1c
		});
		violet07.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), contactType));
		violet07.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			uw1,
		});
		violet07.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), contactType));
		violet07.getAttributes().add("creation", new ValueTime("2011/11/09 20:8:13.123"));
		violet07.getAttributes().add("cpu_usage", new ValueDouble(0.9));
		violet07.getAttributes().add("num_cores", new ValueInt(3l));
		violet07.getAttributes().add("num_processes", new ValueInt(131l));
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
			uw2a
		});
		khaki31.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), contactType));
		khaki31.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			uw2a
		});
		khaki31.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), contactType));
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
		list = Arrays.asList(new Value[] {
			uw3a, uw3b
		});
		khaki13.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), contactType));
		khaki13.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			uw3b
		});
		khaki13.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), contactType));
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
			pj1
		});
		whatever01.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), contactType));
		whatever01.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			pj1
		});
		whatever01.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), contactType));
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
			pj2
		});
		whatever02.getAttributes().add("contacts", new ValueSet(new HashSet<Value>(list), contactType));
		whatever02.getAttributes().add("cardinality", new ValueInt(1l));
		list = Arrays.asList(new Value[] {
			pj2
		});
		whatever02.getAttributes().add("members", new ValueSet(new HashSet<Value>(list), contactType));
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

	public static ZMI createLeafNodeHierarchy(PathName path) throws ParseException, UnknownHostException {
		ZMI root = new ZMI();
		ZMI father = root;
		while (!path.getComponents().isEmpty()) {
			String name = path.getComponents().get(0);
			father = new ZMI(father, name);
			father.getFather().addSon(father);
			path = path.consumePrefix();
		}
		return root;
	}
}
