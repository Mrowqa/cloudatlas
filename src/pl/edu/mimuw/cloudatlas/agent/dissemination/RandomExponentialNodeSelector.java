/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.Collections;
import java.util.LinkedList;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueSet;

/**
 *
 * @author pawel
 */
public class RandomExponentialNodeSelector extends NodeSelector {
	public static int DEFAULT_MULTIPLIER = 2;
	private final int multiplier;	
	
	public RandomExponentialNodeSelector(PathName name, double selectFallbackProbability) {
		this(name, selectFallbackProbability, DEFAULT_MULTIPLIER);
	}
	
	public RandomExponentialNodeSelector(PathName name, double selectFallbackProbability, int multiplier) {
		super(name, selectFallbackProbability);
		this.multiplier = multiplier;
	}
	
	@Override
	protected int selectLevel(LinkedList<LinkedList<ValueSet>> nodesContacts) {
		LinkedList<Integer> counts = new LinkedList<>();
		int count = 1;
		for (int i = 0; i < nodesContacts.size(); i++) {
			counts.add(count);
			count = count * multiplier;
		}
		Collections.reverse(counts);
		
		for (int i = 0; i < counts.size(); i++)
			if (nodesContacts.get(i).isEmpty())
				counts.set(i, 0);
		
		for (int i = 1; i < counts.size(); i++)
			counts.set(i, counts.get(i-1) + counts.get(i));
		
		count = r.nextInt(counts.getLast());
		for (int i = 0; i < counts.size(); i++) {
			if (count >= counts.get(i) && !nodesContacts.isEmpty()) {
				return Math.max(i-1, 0);
			}
		}
		return counts.size() - 1;
	}
}
