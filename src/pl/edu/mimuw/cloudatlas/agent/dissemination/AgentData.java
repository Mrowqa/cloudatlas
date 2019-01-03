/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class AgentData implements Serializable {
	private ZMI zmi;
	private ValueAndFreshness fallbackContacts;
	private HashMap<Attribute, ValueAndFreshness> queries;

	public void setZmi(ZMI zmi) {
		this.zmi = zmi;
	}
	
	public ZMI getZmi() {
		return zmi;
	}

		public ValueAndFreshness getFallbackContacts() {
		return fallbackContacts;
	}

	public Map<Attribute, ValueAndFreshness> getQueries() {
		return queries;
	}

	public AgentData(ZMI zmi, ValueAndFreshness fallbackContacts, HashMap<Attribute, ValueAndFreshness> queries) {
		this.zmi = zmi;
		this.fallbackContacts = fallbackContacts;
		this.queries = queries;
	}
	
	
	public void adjustTime(Duration diff) {
		zmi.adjustTime(diff);
		fallbackContacts.adjustTime(diff);
		for (ValueAndFreshness query : queries.values())
			query.adjustTime(diff);
	}
}
