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
	protected class Level {
		private final int level;
		private final int timesInitial;
		private int timesLeft;
		
		public Level(int level, int timesInitial) {
			this.level = level;
			this.timesInitial = timesInitial;
			this.timesLeft = timesInitial;
		}
		
		public int getLevel() {
			return level;
		}
		
		public int getTimesLeft() {
			return timesLeft;
		}
		
		public void decreaseTimesLeft() {
			timesLeft--;
		}
		
		public void reset() {
			timesLeft = timesInitial;
		}
	}
	protected Queue<Level> levels;
	
	public RoundRobinNodeSelector(PathName name, double selectFallbackProbability) {
		super(name, selectFallbackProbability);
	}
	
	@Override
	public int selectLevel(LinkedList<LinkedList<ValueSet>> nodesContacts) {
		while(true) {
			Level level = levels.peek();
			level.decreaseTimesLeft();
			boolean isLevelEmpty = nodesContacts.get(level.getLevel()).isEmpty();
			
			if (level.getTimesLeft() <= 0 || isLevelEmpty) {
				levels.poll();
				level.reset();
				levels.add(level);
			}
			
			if (!isLevelEmpty)
				return level.getLevel();
		}
	}
}
