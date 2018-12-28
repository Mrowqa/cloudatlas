/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import java.time.Duration;
import java.util.Map;
import pl.edu.mimuw.cloudatlas.model.Attribute;
import pl.edu.mimuw.cloudatlas.model.ValueAndFreshness;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class GossipingAgentData {
	private ZMI[] zmis;
	private ValueAndFreshness fallbackContacts;
	private Map<Attribute, ValueAndFreshness> queries;

	public void setZmis(ZMI[] zmis) {
		this.zmis = zmis;
	}
	
	public ZMI[] getZmis() {
		return zmis;
	}

		public ValueAndFreshness getFallbackContacts() {
		return fallbackContacts;
	}

	public Map<Attribute, ValueAndFreshness> getQueries() {
		return queries;
	}

	public GossipingAgentData(ZMI zmi, ValueAndFreshness fallbackContacts, Map<Attribute, ValueAndFreshness> queries) {
		this.zmis = new ZMI[]{zmi};
		this.fallbackContacts = fallbackContacts;
		this.queries = queries;
	}
	
	public GossipingAgentData(ZMI[] zmis, ValueAndFreshness fallbackContacts, Map<Attribute, ValueAndFreshness> queries) {
		this.zmis = zmis;
		this.fallbackContacts = fallbackContacts;
		this.queries = queries;
	}
	
	public void adjustTime(Duration diff) {
		for (ZMI zmi : zmis) {
			zmi.adjustTime(diff);
		}
		fallbackContacts.adjustTime(diff);
		for (ValueAndFreshness query : queries.values()) {
			query.adjustTime(diff);
		}
	}
}
