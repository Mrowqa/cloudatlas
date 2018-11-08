/**
 * Copyright (c) 2014, University of Warsaw All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pl.edu.mimuw.cloudatlas.interpreter;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueBoolean;
import pl.edu.mimuw.cloudatlas.model.ValueList;

abstract class ResultColection extends Result {
	protected final ValueList values;

	public ResultColection(ValueList values) {
		this.values = values;
	}

	@Override
	protected Result callMe(BinaryOperation operation, Result left) {
		throw new UnsupportedOperationException("Not a Single Result.");
	}
	
	@Override
	public Result transformOperation(TransformOperation operation) {
		return new ResultList(operation.perform(values));
	}
	
	@Override
	public Value getValue() {
		throw new UnsupportedOperationException("Not a Single Result.");
	}
	
	protected ValueList unaryOperationHelper(UnaryOperation operation) {
		if (values.isNull() || values.isEmpty()) {
			return values;
		}
		ValueList new_values = new ValueList(new ArrayList<Value>(),
				((TypeCollection) values.getType()).getElementType());
		for (Value value : values) {
			new_values.add(operation.perform(value));
		}
		return new_values;
	}

	protected ValueList filterNullsHelper() {
		if (values.isNull() || values.isEmpty()) {
			return values;
		}
		ValueList new_values = new ValueList(new ArrayList<Value>(),
				((TypeCollection) values.getType()).getElementType());
		for (Value value : values) {
			if (!value.isNull()) {
				new_values.add(value);
			}
		}
		return new_values;
	}

	// TODO check value.isNull and value.isEmpty diff in first and last functions
	protected ValueList firstHelper(int size) {
		if (values.isNull() || values.isEmpty() || size <= 0) {
			return new ValueList(null);
		}
		if (size >= values.size()) {
			return values;
		}
		ArrayList<Value> new_list = new ArrayList<>(values.subList(0, size));
		return new ValueList(new_list, ((TypeCollection)values.getType()).getElementType());
	}

	protected ValueList lastHelper(int size) {
		if (values.isNull() || values.isEmpty() || size <= 0) {
			return new ValueList(null);
		}
		if (size >= values.size()) {
			return values;
		}
		ArrayList<Value> new_list = new ArrayList<>(values.subList(values.size() - size, values.size()));
		return new ValueList(new_list, ((TypeCollection)values.getType()).getElementType());
		// TODO check null policy
	}

	protected ValueList randomHelper(int size) {
		// TODO change to random selection
		if (values.isNull() || values.isEmpty() || size <= 0) {
			return new ValueList(null);
		}
		if (size >= values.size()) {
			return values;
		}
		ArrayList<Value> new_list = new ArrayList<>(values.subList(values.size() - size, values.size()));
		return new ValueList(new_list, ((TypeCollection)values.getType()).getElementType());
	}

	protected ValueList convertToHelper(Type to) {
		// TODO check if type is convertible to
		ValueList new_values = new ValueList(new ArrayList<Value>(), to);
		for (Value value : values) {
			new_values.add(value.convertTo(to));
		}
		return new_values;
	}

	@Override
	public ResultSingle isNull() {
		return new ResultSingle(new ValueBoolean(values.isNull()));
	}

	@Override
	public Type getType() {
		return ((TypeCollection) values.getType()).getElementType(); // TODO check if not collection type.
	}

	protected ValueList binaryOperationTypedHelper(BinaryOperation operation, ResultSingle right) {
		// TODO what if right null?? Can operation be null
		// TODO Is operation typed? what with nulls in the list
		if (values.isNull() || values.isEmpty()) {
			return values;
		}
		ValueList new_values = new ValueList(new ArrayList<Value>(), ((TypeCollection) values.getType()).getElementType());
		for (Value value : values) {
			new_values.add(operation.perform(value, right.getValue()));
		}
		return new_values;
	}

	@Override
	public ResultSingle aggregationOperation(AggregationOperation operation) {
		return new ResultSingle(operation.perform(values));
	}
}
