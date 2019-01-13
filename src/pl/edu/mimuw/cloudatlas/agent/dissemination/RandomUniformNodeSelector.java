/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueSet;

/**
 *
 * @author pawel
 */
public class RandomUniformNodeSelector extends NodeSelector {
	public RandomUniformNodeSelector(PathName name, double selectFallbackProbability) {
		super(name, selectFallbackProbability);
	}
	@Override
	/**
	 * Method can be called only if there exist some contact in nodesContacts.
	 */
	protected int selectLevel(LinkedList<LinkedList<ValueSet>> nodesContacts) {
		List<Integer> levels = new LinkedList<>();
		for (int i=0; i < nodesContacts.size(); i++)
			if (!nodesContacts.get(i).isEmpty())
				levels.add(i);
		int ind = r.nextInt(levels.size());
		return levels.get(ind);
	}
}
