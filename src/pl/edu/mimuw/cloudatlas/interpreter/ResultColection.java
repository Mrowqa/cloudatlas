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
import java.util.Collections;
import java.util.List;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.TypePrimitive;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueBoolean;
import pl.edu.mimuw.cloudatlas.model.ValueList;

abstract class ResultColection extends Result {
	protected final ValueList values;

	public ResultColection(ValueList values) {
		this.values = values;
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
		ArrayList<Value> newValues = new ArrayList<>();
		for (Value value : values) {
			newValues.add(operation.perform(value));
		}
		Type new_elem_type = newValues.get(0).getType();
		return new ValueList(newValues, new_elem_type);
	}
		
	@Override
	public Result first(int size) {
		ValueList nlist = filterNullsList(values);
		if(nlist.getValue() == null)
			return new ResultSingle(nlist);
		List<Value> result = new ArrayList<>(size);
		int i = 0;
		for(Value v : nlist) {
			result.add(v);
			if(++i == size)
				break;
		}
		return new ResultSingle(new ValueList(result, ((TypeCollection)nlist.getType()).getElementType()));
	}
	
	@Override
	public Result last(int size) {
		ValueList nlist = filterNullsList(values);
		if(nlist.getValue() == null)
			return new ResultSingle(nlist);
		List<Value> result = new ArrayList<>(size);
		for(int i = Math.max(0, nlist.size() - size); i < nlist.size(); ++i)
			result.add(nlist.get(i));
		Type elementType = ((TypeCollection)nlist.getType()).getElementType();
		return new ResultSingle(new ValueList(result, elementType));
	}
	
	@Override
	public Result random(int size) {
		ValueList nlist = filterNullsList(values);
		if(nlist.getValue() == null || nlist.size() <= size)
			return new ResultSingle(nlist);
		Collections.shuffle(nlist);
		Type elementType = ((TypeCollection)nlist.getType()).getElementType();
		return new ResultSingle(new ValueList(nlist.getValue().subList(0, size), elementType));
	}

	protected ValueList convertToHelper(Type to) {
		ValueList newValues = new ValueList(new ArrayList<>(), to);
		for (Value value : values) {
			newValues.add(value.convertTo(to));
		}
		return newValues;
	}

	@Override
	public ResultSingle isNull() {
		return new ResultSingle(new ValueBoolean(values.isNull()));
	}

	@Override
	public Type getType() {
		return ((TypeCollection) values.getType()).getElementType();
	}

	protected ValueList binaryOperationTypedHelper(BinaryOperation operation, ResultSingle right) {
		if (right.getValue().isNull()) {
			throw new UnsupportedOperationException("Binary operation on Result{List, Column} and ResultSingle being null not supported.");
		}
		if (values.isNull() || values.isEmpty()) {
			return values;
		}
		ArrayList<Value> newValues = new ArrayList<>();
		for (Value value : values) {
			newValues.add(operation.perform(value, right.getValue()));
		}
		Type elementType = newValues.isEmpty() ? TypePrimitive.NULL : newValues.get(0).getType();
		return new ValueList(newValues, elementType);
	}
	
	protected ValueList binaryOperationTypedHelper(BinaryOperation operation, ValueList right) {
		ArrayList<Value> newValues = new ArrayList<>();
		if (values.size() != right.size()) {
			throw new UnsupportedOperationException("BinaryOperation not supported for pair of ResultColumn of different sizes " + values.size() + " "+ right.size());
		}
		for (int i = 0; i < values.size(); i++) {
			newValues.add(operation.perform(values.get(i), right.get(i)));
		}
		Type new_element_type = newValues.isEmpty() ? TypePrimitive.NULL : newValues.get(0).getType();
		return new ValueList(newValues, new_element_type);
	}

	@Override
	public ResultSingle aggregationOperation(AggregationOperation operation) {
		return new ResultSingle(operation.perform(values));
	}
}
