/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.agent.dissemination;

import java.util.ArrayDeque;
import pl.edu.mimuw.cloudatlas.model.PathName;

/**
 *
 * @author pawel
 */
public class RoundRobinUniformNodeSelector extends RoundRobinNodeSelector {
	public RoundRobinUniformNodeSelector(PathName name, double selectFallbackProbability) {
		super(name, selectFallbackProbability);
		this.levels = new ArrayDeque<>();
		for (int i = 0; i < name.getComponents().size(); i++)
			levels.add(new Level(i, 1));
	}
}
