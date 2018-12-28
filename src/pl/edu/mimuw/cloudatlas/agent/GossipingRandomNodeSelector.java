/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent;

import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueContact;
import pl.edu.mimuw.cloudatlas.model.ValueSet;
import pl.edu.mimuw.cloudatlas.model.ZMI;

/**
 *
 * @author pawel
 */
public class GossipingRandomNodeSelector implements GossipingNodeSelector {
	private PathName name;
	
	@Override
	public ValueContact selectNode(ZMI zmi, ValueSet fallbackContacts) {
		return null;
	}
	
}
