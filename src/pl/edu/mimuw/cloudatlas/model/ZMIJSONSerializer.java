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
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
// JSON "docs": 
// https://stleary.github.io/JSON-java/
// https://stackoverflow.com/questions/2591098/how-to-parse-json-in-java (https://stackoverflow.com/a/18998203)
// https://www.geeksforgeeks.org/parse-json-java/

/**
 *
 * @author mrowqa
 */
public class ZMIJSONSerializer {
	public static String ZMIToJSONString(ZMI zmi) {
		return ZMIToJSON(zmi).toString();
	}
	
	public static JSONArray ZMIToJSON(ZMI zmi) {
		JSONArray arr = new JSONArray();
		ZMIToJSONVisitZMI(zmi, arr);
		return arr;
	}
	
	private static void ZMIToJSONVisitZMI(ZMI zmi, JSONArray arr) {
		JSONObject obj = new JSONObject();
		obj.put("zoneName", zmi.getPathName().toString());
		obj.put("zoneAttrs", attributesMapToJSON(zmi.getAttributes()));
		arr.put(obj);
		
		for(ZMI son : zmi.getSons()) {
			ZMIToJSONVisitZMI(son, arr);
		}
	}
	
	public static String attributesMapToJSONString(AttributesMap attributes) {		
		return attributesMapToJSON(attributes).toString();
	}
	
	public static JSONObject attributesMapToJSON(AttributesMap attributes) {
		JSONObject obj = new JSONObject();
		for (Entry<Attribute, Value> entry : attributes) {
			obj.put(entry.getKey().toString(), valueToJSON(entry.getValue()));
		}
		return obj;
	}
	
	public static AttributesMap JSONStringToAttributesMap(String json) {
		AttributesMap attrs = new AttributesMap();
		JSONObject obj = new JSONObject(json);
		for (String key : obj.keySet()) {
			Value val = JSONToValue(obj.getJSONObject(key));
			attrs.add(key, val);
		}
		return attrs;
	}
	
	public static String valueToJSONString(Value v) {
		return valueToJSON(v).toString();
	}
	
	public static JSONObject valueToJSON(Value v) {
		JSONObject obj = new JSONObject();
		obj.put("t", typeToString(v.getType()));
		switch(v.getType().toString().split(" ")[0]) {
			case "BOOLEAN":
				try {
					obj.put("v", ((ValueBoolean)v).getValue());
				}
				catch (NullPointerException ex) {}
				break;
			case "CONTACT":
				try {
					// two steps, because we want to add both or neither
					String name = ((ValueContact)v).getName().toString();
					String address = ((ValueContact)v).getAddress().toString();
					int port = ((ValueContact)v).getPort();
					obj.put("n", name);
					obj.put("a", address);
					obj.put("p", port);
				}
				catch (NullPointerException ex) {}
				break;
			case "DOUBLE":
				try {
					obj.put("v", ((ValueDouble)v).getValue());
				}
				catch (NullPointerException ex) {}
				break;
			case "DURATION":
				try {
					// two steps, because we want to add both or neither
					long value = ((ValueDuration)v).getValue();
					String pretty = ((ValueDuration)v).toString();
					obj.put("v", value);
					obj.put("p", pretty);
				}
				catch (NullPointerException ex) {}
				break;
			case "INT":
				try {
					obj.put("v", ((ValueInt)v).getValue());
				}
				catch (NullPointerException ex) {}
				break;
			case "LIST":
				try {
					JSONArray arr = new JSONArray();
					for (Value vSon : ((ValueList)v).getValue()) {
						arr.put(valueToJSON(vSon));
					}
					obj.put("v", arr);
				}
				catch (NullPointerException ex) {}
				break;
			case "NULL":
				break;
			case "SET":
				try {
					JSONArray arr = new JSONArray();
					for (Value vSon : ((ValueSet)v).getValue()) {
						arr.put(valueToJSON(vSon));
					}
					obj.put("v", arr);
				}
				catch (NullPointerException ex) {}
				break;
			case "STRING":
				try {
					obj.put("v", ((ValueString)v).getValue());
				}
				catch (NullPointerException ex) {}
				break;
			case "TIME":
				try {
					// two steps, because we want to add both or neither
					long value = ((ValueTime)v).getValue();
					String pretty = ((ValueTime)v).toString();
					obj.put("v", value);
					obj.put("p", pretty);
				}
				catch (NullPointerException ex) {}
				break;
			case "WRAPPER":
				try {
					// two steps, because we want to add both or neither
					Value value = ((ValueAndFreshness)v).getValue();
					Value freshness = ((ValueAndFreshness)v).getFreshness();
					obj.put("v", valueToJSON(value));
					obj.put("t", valueToJSON(freshness));
				}
				catch (NullPointerException ex) {}
				break;
			default:
				throw new IllegalArgumentException("Invalid Value type!");
		}
		return obj;
	}
	
	public static Value JSONStringToValue(String json) {
		return JSONToValue(new JSONObject(json));
	}
	
	public static Value JSONToValue(JSONObject obj) {
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
					return new ValueContact(new PathName(obj.getString("n")), InetAddress.getByName(address), obj.getInt("p"));
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
						coll.add(JSONToValue(elem));
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
						coll.add(JSONToValue(elem));
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
			case "w": { // wrapper (ValueAndFreshness)
				Value value = JSONToValue(obj.getJSONObject("v"));
				ValueTime freshness = (ValueTime)JSONToValue(obj.getJSONObject("t"));
				return new ValueAndFreshness(value, freshness);
			}
			
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
			case "w": return new TypeWrapper(stringToType(str.substring("se|".length())));
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
			case "WRAPPER": return "w|" + typeToString(((TypeWrapper)t).getNestedType());
			default:
				throw new IllegalArgumentException("Unknown type!");
		}
	}
	
	public static String allQueriesToJSONString(HashMap<Attribute, Query> queries) {
		JSONArray arr = new JSONArray();
		
		for (Entry<Attribute, Query> q : queries.entrySet()) {
			JSONObject obj = new JSONObject();
			obj.put("name", q.getKey().getName());
			obj.put("text", q.getValue().getText());
			obj.put("signature", q.getValue().getSignature());
			obj.put("freshness", q.getValue().getFreshness().getValue());
			arr.put(obj);
		}
		
		return arr.toString();
	}
}
