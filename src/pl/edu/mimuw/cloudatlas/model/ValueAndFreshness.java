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
	private Value value;
	private ValueTime freshness;
	
	public Value getValue() {
		return value;
	}

	public ValueTime getFreshness() {
		return freshness;
	}

	public ValueAndFreshness(Value val, ValueTime freshness) {
		this.value = val;
		this.freshness = freshness;
	}
	
	public static ValueAndFreshness freshValue(Value val) {
		return new ValueAndFreshness(val, ValueTime.now());
	}
	
	public ValueAndFreshness adjustTime(Duration time) {
		return new ValueAndFreshness(value, freshness.adjustTime(time));
	}
	
	public ValueAndFreshness getFresher(ValueAndFreshness valAndTs) {
		if (freshness.isLowerThan(valAndTs.freshness).getValue()) {
			return valAndTs;
		}
		return this;
	}
}
