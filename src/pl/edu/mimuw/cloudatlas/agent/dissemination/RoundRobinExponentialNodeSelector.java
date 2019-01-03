/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import pl.edu.mimuw.cloudatlas.model.PathName;
import pl.edu.mimuw.cloudatlas.model.ValueSet;

/**
 *
 * @author pawel
 */
public class RoundRobinExponentialNodeSelector extends RoundRobinNodeSelector {
	public static int DEFAULT_MULTIPLIER = 2;
	private final int multiplier;
	
	public RoundRobinExponentialNodeSelector(PathName name) {
		this(name, DEFAULT_MULTIPLIER);
	}
	
	public RoundRobinExponentialNodeSelector(PathName name, int multiplier) {
		super(name);
		this.multiplier = multiplier;
		this.levels = new ArrayDeque<>();
		
		ArrayList<Integer> elements = new ArrayList<>();
		int count = 1;
		int maxLevel = name.getComponents().size() - 1;
		for (int i = maxLevel; i >= 0; i--) {
			for (int j = 0; j < count; j++) {
				elements.add(i);
			}
			count = count * multiplier;
		}
		
		Collections.reverse(elements);
		for (int element : elements) {
			System.out.println("Element " + element);
			levels.add(element);
		}
	}
}
