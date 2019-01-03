/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.LinkedList;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueSet;

/**
 *
 * @author pawel
 */
public class RandomUniformNodeSelector extends NodeSelector {
	public RandomUniformNodeSelector(PathName name) {
		super(name);
	}
	
	@Override
	protected int selectLevel(LinkedList<LinkedList<ValueSet>> nodesContacts) {
		int level;
		do {
			level = r.nextInt(nodesContacts.size());
		} while (nodesContacts.get(level).isEmpty());
		return level;
	}
}
