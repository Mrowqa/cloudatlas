/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.model;

import java.io.Serializable;
import java.time.Instant;

/**
 *
 * @author pawel
 */
public class ValueAndFreshness implements Serializable {
	private final Value val;
	private ValueTime freshness;
	
	public Value getVal() {
		return val;
	}

	public ValueTime getFreshness() {
		return freshness;
	}

	private ValueAndFreshness(Value val, ValueTime freshness) {
		this.val = val;
		this.freshness = freshness;
	}
	
	public static ValueAndFreshness freshValue(Value val) {
		return new ValueAndFreshness(val, ValueTime.now());
	}
}
