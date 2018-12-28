/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.model;

import java.io.Serializable;
import java.time.Duration;

/**
 *
 * @author pawel
 */
public class ValueAndFreshness implements Serializable {
	private Value val;
	private ValueTime freshness;
	
	public Value getVal() {
		return val;
	}

	public ValueTime getFreshness() {
		return freshness;
	}

	public ValueAndFreshness(Value val, ValueTime freshness) {
		this.val = val;
		this.freshness = freshness;
	}
	
	public static ValueAndFreshness freshValue(Value val) {
		return new ValueAndFreshness(val, ValueTime.now());
	}
	
	public void adjustTime(Duration time) {
		this.freshness = freshness.adjustTime(time);
	}
	
	public void update(ValueAndFreshness valAndTs) {
		if (freshness.isLowerThan(valAndTs.freshness).getValue()) {
			val = valAndTs.val;
			freshness = valAndTs.freshness;
		}
	}
}
