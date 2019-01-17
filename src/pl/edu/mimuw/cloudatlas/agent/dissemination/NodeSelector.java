/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.LinkedList;
import java.util.Random;
import org.json.JSONObject;
import pl.edu.mimuw.cloudatlas.agent.ConfigUtils;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.TypeWrapper;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public abstract class NodeSelector {
	public static final String DEFAULT_SELECTOR_NAME = "random";
	public static final double DEFAULT_SELECT_FALLBACK_PROBABILITY = 0.1;
	protected final PathName name;
	protected final Random r = new Random();
	protected final double selectFallbackProbability;
	
	public NodeSelector(PathName name, double selectFallbackProbability) {
		this.name = name;
		this.selectFallbackProbability = selectFallbackProbability;
	}
	
	public static NodeSelector createByName(String className, PathName name, double selectFallbackProbability) {
		switch(className) {
			case "random": return new RandomUniformNodeSelector(name, selectFallbackProbability);
			case "randomExponential": return new RandomExponentialNodeSelector(name, selectFallbackProbability);
			case "roundRobin": return new RoundRobinUniformNodeSelector(name, selectFallbackProbability);
			case "roundRobinExponential": return new RoundRobinExponentialNodeSelector(name, selectFallbackProbability);
			default: return null; // Unknon subclass;
		}
	}
	
	
	public static NodeSelector fromConfig(JSONObject config) {
		PathName nodeName = new PathName(config.getString("name"));
		double selectFallbackPr = ConfigUtils.getDoubleOrDefault("selectFallbackOrDefault", DEFAULT_SELECT_FALLBACK_PROBABILITY, config);
		String className = ConfigUtils.getStringOrDefault("nodeSelector", DEFAULT_SELECTOR_NAME, config);
		return createByName(className, nodeName, selectFallbackPr);
	}
	
	public ValueContact selectNode(ZMI zmi, ValueSet fallbackContacts) {
		LinkedList<LinkedList<ValueSet>> nodesContacts = extractContacts(zmi);
		fallbackContacts = filterMe(fallbackContacts);
		// We sometimes select fallback at random to get information from whole ZMI hierarchy.
		if (!hasAny(nodesContacts) || shouldRandomlyContactFallback()) {
			return selectContact(fallbackContacts);
		}
		int level = selectLevel(nodesContacts);
		return selectContact(nodesContacts.get(level));
	}
	
	/**
	 * Method assumes that there is some contact in nodesContacts.
	 */
	protected abstract int selectLevel(LinkedList<LinkedList<ValueSet>> nodesContacts);


	private LinkedList<LinkedList<ValueSet>> extractContacts(ZMI zmi) {
		LinkedList<LinkedList<ValueSet>> results = new LinkedList<>();
		PathName curPath = name;

		while (!zmi.getSons().isEmpty() && !curPath.getComponents().isEmpty()) {
			LinkedList<ValueSet> result = new LinkedList<>();
			String curName = curPath.getComponents().get(0);
			for (ZMI node : zmi.getSons()) {
				if (!node.getPathName().getSingletonName().equals(curName)) {
					ValueSet contacts = getNotEmptyContacts(node);
					if (contacts != null)
						result.push(contacts);
				}
			}
			results.push(result);
			zmi = zmi.getSonBySingletonName(curName);
			if (zmi == null) {
				break;
			}
			curPath = curPath.consumePrefix();	
		}
		return results;
	}
	
	private ValueSet filterMe(ValueSet contacts) {
		ValueSet filtered = new ValueSet(TypePrimitive.CONTACT);
		for (Value contact : contacts.getValue()) {
			ValueContact contact1 = ZMI.unwrapContact(contact);
			if (!contact1.getName().equals(name)) {
				filtered.add(contact);
			}
		}
		return filtered;
	}
	
	private ValueSet getNotEmptyContacts(ZMI zmi) {
		Value contactsRaw = zmi.getAttributes().getOrNull("contacts");
		if (contactsRaw == null || contactsRaw.isNull())
			return null;
		if (!(contactsRaw instanceof ValueSet))
			return null;
		ValueSet contacts = (ValueSet)contactsRaw;
		if (contacts.isEmpty())
			return null;
		Type elementType = ((TypeCollection)contacts.getType()).getElementType();
		if (elementType.getPrimaryType() != Type.PrimaryType.WRAPPER)
			return null;
		if (((TypeWrapper)elementType).getNestedType().getPrimaryType() != Type.PrimaryType.CONTACT)
			return null;
		ValueSet result = new ValueSet(new TypeWrapper(TypePrimitive.CONTACT));
		for (Value contact : contacts.getValue()) {
			result.add(contact);
		}
		return result;
	}

	private boolean hasAny(LinkedList<LinkedList<ValueSet>> nodesContacts) {
		for (LinkedList<ValueSet> nodes : nodesContacts) {
			for (ValueSet node : nodes) {
				if (!node.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private ValueContact selectContact(ValueSet contacts) {
		if (contacts.isEmpty())
			return null;
		int ind = r.nextInt(contacts.size());
		return ZMI.unwrapContact((Value)contacts.getValue().toArray()[ind]);
	}

	private ValueContact selectContact(LinkedList<ValueSet> nodesContacts) {
		int ind = r.nextInt(nodesContacts.size());
		return selectContact(nodesContacts.get(ind));
	}
	
	private boolean shouldRandomlyContactFallback() {
		return r.nextDouble() <= selectFallbackProbability;
	}
}
