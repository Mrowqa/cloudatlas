/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.mimuw.cloudatlas.model;

import java.util.Map.Entry;

/**
 *
 * @author mrowqa
 */
public class ZMIJSONSerializer {
	public static String toJSON(ZMI zmi) {
		String result = new String();
		result += "[";
		result = toJSON_visitZMI(zmi, result);
		result = result.substring(0, result.length() - 1);
		result += "]";
		return result;
	}
	
	private static String toJSON_visitZMI(ZMI zmi, String result) {
		result += "{";
		result += "\"!zone\":\"" + zmi.getPathName().toString() + "\",";
		
		for (Entry<Attribute, Value> entry : zmi.getAttributes()) {
			result = toJSON_visitMapEntry(entry, result);
		}
		result = result.substring(0, result.length() - 1);
		result += "},";
		
		for(ZMI son : zmi.getSons()) {
			result = toJSON_visitZMI(son, result);
		}
		return result;
	}
	
	private static String toJSON_visitMapEntry(Entry<Attribute, Value> entry, String result) {
		result += "\"" + entry.getKey().toString() + "\":";
		return toJSON_visitValue(entry.getValue(), result);
	}
	
	private static String toJSON_visitValue(Value v, String result) {
		result += "{";
		switch(v.getType().toString().split(" ")[0]) {
			case "BOOLEAN":
				result += "\"t\":\"b\"";
				try {
					result += ",\"v\":" + (((ValueBoolean)v).getValue() ? "true" : "false");
				}
				catch (NullPointerException ex) {}
				break;
			case "CONTACT":
				result += "\"t\":\"c\"";
				try {
					result += ",\"n\":\"" + ((ValueContact)v).getName().toString() + "\",\"a\":\"" + ((ValueContact)v).getAddress().toString() + "\"";
				}
				catch (NullPointerException ex) {}
				break;
			case "DOUBLE":
				result += "\"t\":\"do\"";
				try {
					result += ",\"v\":" + ((ValueDouble)v).getValue().toString();
				}
				catch (NullPointerException ex) {}
				break;
			case "DURATION":
				result += "\"t\":\"du\"";
				try {
					result += ",\"v\":" + ((ValueDuration)v).getValue().toString() + ",\"p\":\"" + ((ValueDuration)v).toString() + "\"";
				}
				catch (NullPointerException ex) {}
				break;
			case "INT":
				result += "\"t\":\"i\"";
				try {
					result += ",\"v\":" + ((ValueInt)v).getValue().toString();
				}
				catch (NullPointerException ex) {}
				break;
			case "LIST":
				result += "\"t\":\"l\"";
				try {
					result += ",\"v\":[";
					for (Value vSon : ((ValueList)v).getValue()) {
						toJSON_visitValue(vSon, result);
					}
					if (result.charAt(result.length() - 1) == ',') {
						result = result.substring(0, result.length() - 1);
					}
					result += "]";
				}
				catch (NullPointerException ex) {}
				break;
			case "NULL":
				result += "\"t\":\"n\"";
				break;
			case "SET":
				result += "\"t\":\"se\"";
				try {
					result += ",\"v\":[";
					for (Value vSon : ((ValueSet)v).getValue()) {
						toJSON_visitValue(vSon, result);
					}
					if (result.charAt(result.length() - 1) == ',') {
						result = result.substring(0, result.length() - 1);
					}
					result += "]";
				}
				catch (NullPointerException ex) {}
				break;
			case "STRING":
				result += "\"t\":\"st\"";
				String vv = ((ValueString)v).getValue();
				if (vv != null) {
					// replaceAll takes regex, hence "\\\\" is actually "match a single \"
					result += ",\"v\":\"" + vv.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"") + "\"";
				}
				break;
			case "TIME":
				result += "\"t\":\"t\"";
				try {
					result += ",\"v\":" + ((ValueTime)v).getValue().toString() + ",\"p\":\"" + ((ValueTime)v).toString() + "\"";
				}
				catch (NullPointerException ex) {}
				break;
			default:
				throw new RuntimeException("Invalid Value type!");
		}
		result += "},";
		return result;
	}
}
