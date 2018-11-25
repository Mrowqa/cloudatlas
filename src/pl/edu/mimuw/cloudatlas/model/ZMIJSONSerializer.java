/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
// JSON "docs": 
// https://stackoverflow.com/questions/2591098/how-to-parse-json-in-java (https://stackoverflow.com/a/18998203)
// https://www.geeksforgeeks.org/parse-json-java/

// Note: this code is actually awful. No refactoring unless it's necessary!
/**
 *
 * @author mrowqa
 */
public class ZMIJSONSerializer {	
	public static String ZMIToJSON(ZMI zmi) {
		String result = new String();
		result += "[";
		result += ZMIToJSONVisitZMI(zmi);
		result = result.substring(0, result.length() - 1);
		result += "]";
		return result;
	}
	
	private static String ZMIToJSONVisitZMI(ZMI zmi) {
		String result = "{";
		result += "\"zoneName\":\"" + zmi.getPathName().toString() + "\",\"zoneAttrs\":";
		result += attributesMapToJSON(zmi.getAttributes());
		result += "},";
		
		for(ZMI son : zmi.getSons()) {
			result += ZMIToJSONVisitZMI(son);
		}
		return result;
	}
	
	public static String attributesMapToJSON(AttributesMap attributes) {
		String result = "{";
		
		for (Entry<Attribute, Value> entry : attributes) {
			result += "\"" + entry.getKey().toString() + "\":" + valueToJSON(entry.getValue()) + ",";
		}
		result = result.substring(0, result.length() - 1);
		result += "}";
		
		return result;
	}
	
	public static AttributesMap JSONToAttributesMap(String json) {
		AttributesMap attrs = new AttributesMap();
		JSONObject obj = new JSONObject(json);
		for (String key : obj.keySet()) {
			Value val = JSONToValueImpl(obj.getJSONObject(key));
			attrs.add(key, val);
		}
		return attrs;
	}
	
	public static String valueToJSON(Value v) {
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
						result += valueToJSON(vSon) + ",";
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
						result += valueToJSON(vSon) + ",";
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
		return JSONToValueImpl(new JSONObject(json));
	}
	
	private static Value JSONToValueImpl(JSONObject obj) {
		switch (obj.getString("t").split("\\|")[0]) {
			case "b": // boolean
				return new ValueBoolean(obj.has("v") ? obj.getBoolean("v") : null);
			case "c": // contact
				// assumption: contact can't be null
				try {
					String address = obj.getString("a");
					if (address.contains("/")) { // could be in format: hostname/1.2.3.4
						int sepPos = address.lastIndexOf("/");
						if (sepPos == 0) {
							address = address.substring(1);
						}
						else {
							address = address.substring(0, sepPos);
						}
					}
					return new ValueContact(new PathName(obj.getString("n")), InetAddress.getByName(address));
				}
				catch (UnknownHostException ex) {}
				break;
			case "do": // double
				return new ValueDouble(obj.has("v") ? obj.getDouble("v") : null);
			case "du": // duration
				// assert ValueDuration(obj["p"]) == ValueDuration(obj["v"])
				return new ValueDuration(obj.has("v") ? (long) obj.getLong("v") : null);
			case "i": // int
				return new ValueInt(obj.has("v") ? (long) obj.getLong("v") : null);
			case "l": { // list
				List<Value> coll = new ArrayList<>();
				if (obj.has("v")) {
					JSONArray arr = obj.getJSONArray("v");
					for (int i = 0; i < arr.length(); i++) {
						JSONObject elem = arr.getJSONObject(i);
						coll.add(JSONToValueImpl(elem));
					}
				}
				Type elemType = stringToType(obj.getString("t").substring("l|".length()));
				return new ValueList(coll, elemType);
			}
			case "n":
				break;
			case "se": { // set
				Set<Value> coll = new HashSet<>();
				if (obj.has("v")) {
					JSONArray arr = obj.getJSONArray("v");
					for (int i = 0; i < arr.length(); i++) {
						JSONObject elem = arr.getJSONObject(i);
						coll.add(JSONToValueImpl(elem));
					}
				}
				Type elemType = stringToType(obj.getString("t").substring("se|".length()));
				return new ValueSet(coll, elemType);
			}
			case "st": // string
				return new ValueString(obj.has("v") ? obj.getString("v") : null);
			case "t": // time
				// assert ValueTime(obj["p"]) == ValueTime(obj["v"])
				return new ValueTime(obj.has("v") ? (long) obj.getLong("v") : null);
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
