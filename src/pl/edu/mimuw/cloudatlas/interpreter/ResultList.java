/**
 * Copyright (c) 2014, University of Warsaw
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package pl.edu.mimuw.cloudatlas.interpreter;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueBoolean;
import pl.edu.mimuw.cloudatlas.model.ValueList;

class ResultList extends Result {
	private final ValueList value;

	public ResultList(ValueList value) {
		this.value = value;
	}
	
	protected ResultList binaryOperationTyped(BinaryOperation operation, ResultList right) {
            throw new UnsupportedOperationException("Not a ResultList.");
            // return new ResultList(operation.perform(value, right.value));
	}

	@Override
	public ResultList unaryOperation(UnaryOperation operation) {
            throw new UnsupportedOperationException("Not a ResultList.");            
            //return new ResultList(operation.perform(value));
	}

	@Override
	protected Result callMe(BinaryOperation operation, Result left) {
            throw new UnsupportedOperationException("Not a ResultList.");
            //return left.binaryOperationTyped(operation, this);
	}

	@Override
	public Value getValue() {
            throw new UnsupportedOperationException("Not a Single Result.");
	}

	@Override
	public ValueList getList() {
            return value;
            // TODO check list column diff
	}

	@Override
	public ValueList getColumn() {
            return value;
	}

	@Override
	public Result filterNulls() {
            if (value.isNull()) {
                return this;
            }
            ValueList filtered_value = new ValueList(new ArrayList<Value>(), 
                    ((TypeCollection)value.getType()).getElementType());
            for (int i = 0; i < value.size(); i++) {
                Value val = value.get(i);
                if (!val.isNull()) {
                    filtered_value.add(val);
                }
            }
            return new ResultList(filtered_value);
	}

	@Override
	public Result first(int size) {
            if (value.isNull() || value.isEmpty()) {
                return new ResultSingle(null);
            }
            return new ResultSingle(value.get(0));
	}

	@Override
	public Result last(int size) {
            if (value.isNull() || value.isEmpty()) {
                return new ResultSingle(null);
            }
            return new ResultSingle(value.get(value.size() - 1));
	}

	@Override
	public Result random(int size) {
            if (value.isNull() || value.isEmpty()) {
                return new ResultSingle(null);
            }
            int index = ThreadLocalRandom.current().nextInt(0, value.size()); // Rand index TODO
            return new ResultSingle(value.get(index));
	}

	@Override
	public ResultList convertTo(Type to) {
            throw new UnsupportedOperationException("Not a ResultList.");
	}

        @Override
	public ResultSingle isNull() {
             return new ResultSingle(new ValueBoolean(value.isNull()));
	}

	@Override
	public Type getType() {
		return value.getType();
	}

    @Override
    protected Result binaryOperationTyped(BinaryOperation operation, ResultSingle right) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
