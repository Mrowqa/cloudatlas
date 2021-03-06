/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import pl.edu.mimuw.cloudatlas.model.PathName;

/**
 *
 * @author pawel
 */
public class RoundRobinExponentialNodeSelector extends RoundRobinNodeSelector {
	public static int DEFAULT_MULTIPLIER = 2;
	private final int multiplier;
	
	public RoundRobinExponentialNodeSelector(PathName name, double selectFallbackProbability) {
		this(name, selectFallbackProbability, DEFAULT_MULTIPLIER);
	}
	
	public RoundRobinExponentialNodeSelector(PathName name, double selectFallbackProbability, int multiplier) {
		super(name, selectFallbackProbability);
		this.multiplier = multiplier;
		this.levels = new ArrayDeque<>();
		
		ArrayList<Level> elements = new ArrayList<>();
		int count = 1;
		int maxLevel = name.getComponents().size() - 1;
		for (int i = maxLevel; i >= 0; i--) {
			elements.add(new Level(i, count));
			count = count * multiplier;
		}
		
		Collections.reverse(elements);
		for (Level element : elements)
			levels.add(element);
	}
}
