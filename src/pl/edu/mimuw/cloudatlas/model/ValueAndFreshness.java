/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.edu.mimuw.cloudatlas.model;

import java.io.Serializable;
import java.time.Duration;

/**
 * Wrapper on single Value. Alongside with value we store its freshness.
 * @author pawel
 */
public class ValueAndFreshness extends Value implements Serializable {
	private TypeWrapper type;
	private Value value;
	private ValueTime freshness;
	
	public Value getValue() {
		return value;
	}

	public ValueTime getFreshness() {
		return freshness;
	}

	public ValueAndFreshness(Value value, ValueTime freshness) {
		this.type = new TypeWrapper(value.getType());
		this.value = value;
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

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public boolean isNull() {
		return value.isNull();
	}

	@Override
	public Value convertTo(Type to) {
		switch(to.getPrimaryType()) {
			case WRAPPER:
				if (((TypeWrapper)to).getNestedType() == type.getNestedType())
					return this;
				throw new UnsupportedConversionException(getType(), to);
			case STRING:
				if(isNull())
					return ValueString.NULL_STRING;
				else
					return new ValueString("(" + value.toString() + " at " + freshness.toString() + ")");
			default:
				throw new UnsupportedConversionException(getType(), to);
		}
	}

	@Override
	public Value getDefaultValue() {
		return new ValueAndFreshness(ValueNull.getInstance(), ValueTime.now());
	}
}
