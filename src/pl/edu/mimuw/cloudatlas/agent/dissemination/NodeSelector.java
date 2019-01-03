/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.LinkedList;
import java.util.Random;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public abstract class NodeSelector {
	public static final String DEFAULT_SELECTOR_NAME = "random";
	protected final PathName name;
	protected final Random r = new Random();

	public NodeSelector(PathName name) {
		this.name = name;
	}
	
	public static NodeSelector createByName(String className, PathName name) {
		switch(className) {
			case "random": return new RandomUniformNodeSelector(name);
			case "randomExponential": return new RandomExponentialNodeSelector(name);
			case "roundRobin": return new RoundRobinUniformNodeSelector(name);
			case "roundRobinExponential": return new RoundRobinExponentialNodeSelector(name);
			default: return null; // Unknon subclass;
		}
	}
	
	public ValueContact selectNode(ZMI zmi, ValueSet fallbackContacts) {
		LinkedList<LinkedList<ValueSet>> nodesContacts = extractContacts(zmi);
		fallbackContacts = filterMe(fallbackContacts);
		if (!hasAny(nodesContacts)) {
			return selectContact(fallbackContacts);
		}
		int level = selectLevel(nodesContacts);
		return selectContact(nodesContacts.get(level));
	}
	
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
			ValueContact contact1 = (ValueContact)contact;
			if (!contact1.getName().equals(name)) {
				filtered.add(contact);
			}
		}
		return filtered;
	}
	
	private ValueSet getNotEmptyContacts(ZMI zmi) {
		ValueSet contacts = (ValueSet)zmi.getAttributes().getOrNull("contacts");
		ValueSet result = null;
		if (contacts == null || contacts.isNull() || contacts.isEmpty()) {
			return result;
		}
		result = new ValueSet(TypePrimitive.CONTACT);
		for (Value contact : contacts.getValue()) {
			ValueContact contact1 = (ValueContact)contact;
			ValueContact outContact = new ValueContact(zmi.getPathName(), contact1.getAddress(), contact1.getPort());
			result.add(outContact);
		}
		return result;
		//  && ((TypeCollection)contacts.getType()).getElementType().getPrimaryType() != TypePrimitive.CONTACT
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
		return (ValueContact)contacts.getValue().toArray()[ind];
	}

	private ValueContact selectContact(LinkedList<ValueSet> nodesContacts) {
		int ind = r.nextInt(nodesContacts.size());
		return selectContact(nodesContacts.get(ind));
	}
}
