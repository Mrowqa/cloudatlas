/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.model;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

// Note: this code is actually awful. No refactoring unless it's necessary!
/**
 *
 * @author mrowqa
 */
public class ZMIJSONSerializer {	
	public static String ZMIToJSON(ZMI zmi) {
		String result = new String();
		result += "[";
		result = toJSON_visitZMI(zmi, result);
		result = result.substring(0, result.length() - 1);
		result += "]";
		return result;
	}
	
	private static String toJSON_visitZMI(ZMI zmi, String result) {
		result += "{";
		result += "\"zoneName\":\"" + zmi.getPathName().toString() + "\",\"zoneInfo\":{";
		
		for (Entry<Attribute, Value> entry : zmi.getAttributes()) {
			result = toJSON_visitMapEntry(entry, result);
		}
		result = result.substring(0, result.length() - 1);
		result += "}},";
		
		for(ZMI son : zmi.getSons()) {
			result = toJSON_visitZMI(son, result);
		}
		return result;
	}
	
	private static String toJSON_visitMapEntry(Entry<Attribute, Value> entry, String result) {
		result += "\"" + entry.getKey().toString() + "\":";
		result += ValueToJSON(entry.getValue());
		return result;
	}
	
	public static String ValueToJSON(Value v) {
		String result = "{";
		result += "\"t\":\"" + typeToString(v.getType()) + "\"";
		switch(v.getType().toString().split(" ")[0]) {
			case "BOOLEAN":
				try {
					result += ",\"v\":" + (((ValueBoolean)v).getValue() ? "true" : "false");
				}
				catch (NullPointerException ex) {}
				break;
			case "CONTACT":
				try {
					result += ",\"n\":\"" + ((ValueContact)v).getName().toString() + "\",\"a\":\"" + ((ValueContact)v).getAddress().toString() + "\"";
				}
				catch (NullPointerException ex) {}
				break;
			case "DOUBLE":
				try {
					result += ",\"v\":" + ((ValueDouble)v).getValue().toString();
				}
				catch (NullPointerException ex) {}
				break;
			case "DURATION":
				try {
					result += ",\"v\":" + ((ValueDuration)v).getValue().toString() + ",\"p\":\"" + ((ValueDuration)v).toString() + "\"";
				}
				catch (NullPointerException ex) {}
				break;
			case "INT":
				try {
					result += ",\"v\":" + ((ValueInt)v).getValue().toString();
				}
				catch (NullPointerException ex) {}
				break;
			case "LIST":
				try {
					result += ",\"v\":[";
					for (Value vSon : ((ValueList)v).getValue()) {
						result += ValueToJSON(vSon) + ",";
					}
					if (result.charAt(result.length() - 1) == ',') {
						result = result.substring(0, result.length() - 1);
					}
					result += "]";
				}
				catch (NullPointerException ex) {}
				break;
			case "NULL":
				break;
			case "SET":
				try {
					result += ",\"v\":[";
					for (Value vSon : ((ValueSet)v).getValue()) {
						result += ValueToJSON(vSon) + ",";
					}
					if (result.charAt(result.length() - 1) == ',') {
						result = result.substring(0, result.length() - 1);
					}
					result += "]";
				}
				catch (NullPointerException ex) {}
				break;
			case "STRING":
				String vv = ((ValueString)v).getValue();
				if (vv != null) {
					// replaceAll takes regex, hence "\\\\" is actually "match a single \"
					result += ",\"v\":\"" + vv.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"") + "\"";
				}
				break;
			case "TIME":
				try {
					result += ",\"v\":" + ((ValueTime)v).getValue().toString() + ",\"p\":\"" + ((ValueTime)v).toString() + "\"";
				}
				catch (NullPointerException ex) {}
				break;
			default:
				throw new IllegalArgumentException("Invalid Value type!");
		}
		result += "}";
		return result;
	}
	
	public static Value JSONToValue(String json) {
		return JSONToValue_impl(new JSONObject(json));
	}
	
	private static Value JSONToValue_impl(JSONObject obj) { // todo implement the rest!
		switch (obj.getString("t").split("\\|")[0]) {
			case "b":
				break;
			case "c": // contact
				try {
					String address = obj.getString("a");
					if (address.contains("/")) { // could be in format: hostname/1.2.3.4
						address = address.substring(0, address.lastIndexOf("/"));
					}
					return new ValueContact(new PathName(obj.getString("n")), InetAddress.getByName(address));
				}
				catch (UnknownHostException ex) {}
				break;
			case "do":
				break;
			case "du":
				break;
			case "i":
				break;
			case "l":
				break;
			case "n":
				break;
			case "se": // set
				Set<Value> coll = new HashSet<>();
				JSONArray arr = obj.getJSONArray("v");
				for (int i = 0; i < arr.length(); i++) {
					JSONObject elem = arr.getJSONObject(i);
					coll.add(JSONToValue_impl(elem));
				}
				Type elemType = stringToType(obj.getString("t").substring("se|".length()));
				return new ValueSet(coll, elemType);
			case "st":
				break;
			case "t":
				break;
			default:
				throw new RuntimeException("Invalid Value type!");
		}
		
		return ValueNull.getInstance();
	}
	
	private static Type stringToType(String str) {
		switch (str.split("\\|")[0]) {
			case "b": return TypePrimitive.BOOLEAN;
			case "c": return TypePrimitive.CONTACT;
			case "do": return TypePrimitive.DOUBLE;
			case "du": return TypePrimitive.DURATION;
			case "i": return TypePrimitive.INTEGER;
			case "l": return new TypeCollection(Type.PrimaryType.LIST, stringToType(str.substring("l|".length())));
			case "n": return TypePrimitive.NULL;
			case "se": return new TypeCollection(Type.PrimaryType.SET, stringToType(str.substring("se|".length())));
			case "st": return TypePrimitive.STRING;
			case "t": return TypePrimitive.TIME;
			default:
				throw new IllegalArgumentException("Unknown type!");
		}
	}
	
	private static String typeToString(Type t) {
		switch (t.toString().split(" ")[0]) {
			case "BOOLEAN": return "b";
			case "CONTACT": return "c";
			case "DOUBLE": return "do";
			case "DURATION": return "du";
			case "INT": return "i";
			case "LIST": return "l|" + typeToString(((TypeCollection)t).getElementType());
			case "NULL": return "n";
			case "SET": return "se|" + typeToString(((TypeCollection)t).getElementType());
			case "STRING": return "st";
			case "TIME": return "t";
			default:
				throw new IllegalArgumentException("Unknown type!");
		}
	}
}
