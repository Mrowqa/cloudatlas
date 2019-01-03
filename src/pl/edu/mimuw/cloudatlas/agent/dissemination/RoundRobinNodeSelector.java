/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.LinkedList;
import java.util.Queue;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueSet;

/**
 *
 * @author pawel
 */
public abstract class RoundRobinNodeSelector extends NodeSelector {
	protected Queue<Integer> levels;

	public RoundRobinNodeSelector(PathName name) {
		super(name);
	}
	
	@Override
	public int selectLevel(LinkedList<LinkedList<ValueSet>> nodesContacts) {
		while(true) {
			int level = levels.poll();
			levels.add(level);
			if (!nodesContacts.get(level).isEmpty())
				return level;
		}
	}
}
