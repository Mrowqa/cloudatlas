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
import java.util.List;
import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.TypeCollection;
import pl.edu.mimuw.cloudatlas.model.Value;
import pl.edu.mimuw.cloudatlas.model.ValueList;

class ResultList extends ResultColection {
	public ResultList(ValueList values) {
		super(values);
	}

	@Override
	public ResultList unaryOperation(UnaryOperation operation) {
		return new ResultList(unaryOperationHelper(operation));
	}

	@Override
	public ValueList getList() {
		return values;
	}

	@Override
	public ValueList getColumn() {
		throw new UnsupportedOperationException("Not a Column Result.");
	}

	@Override
	public Result filterNulls() {
		return new ResultList(filterNullsList(values));
	}

	@Override
	public Result convertTo(Type to) {
		return new ResultList(convertToHelper(to));
	}

	@Override
	protected Result callMe(BinaryOperation operation, Result left) {
		return left.binaryOperationTyped(operation, this);
	}
	
	@Override
	protected Result binaryOperationTyped(BinaryOperation operation, ResultSingle right) {
		return new ResultList(binaryOperationTypedHelper(operation, right));
	}

	@Override
	protected Result binaryOperationTyped(BinaryOperation operation, ResultColumn right) {
		throw new UnsupportedOperationException("BinaryOperation not supported for ResultList and ResultColumn.");
	}

	@Override
	protected Result binaryOperationTyped(BinaryOperation operation, ResultList right) {
		throw new UnsupportedOperationException("BinaryOperations are not suported for pair of ResultList.");
	}
}
