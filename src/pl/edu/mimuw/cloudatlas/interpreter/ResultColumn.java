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

import pl.edu.mimuw.cloudatlas.model.Type;
import pl.edu.mimuw.cloudatlas.model.ValueList;

class ResultColumn extends ResultColection {
	public ResultColumn(ValueList values) {
		super(values);
	}

	@Override
	public ResultColumn unaryOperation(UnaryOperation operation) {
		return new ResultColumn(unaryOperationHelper(operation));
	}

	@Override
	public ValueList getList() {
		throw new UnsupportedOperationException("Not a List Result.");
	}

	@Override
	public ValueList getColumn() {
		return values;
	}

	@Override
	public Result filterNulls() {
		return new ResultColumn(filterNullsList(values));
	}

	@Override
	public Result convertTo(Type to) {
		return new ResultColumn(convertToHelper(to));
	}

	@Override
	protected Result callMe(BinaryOperation operation, Result left) {
		return left.binaryOperationTyped(operation, this);
	}
	
	@Override
	protected Result binaryOperationTyped(BinaryOperation operation, ResultSingle right) {
		return new ResultColumn(binaryOperationTypedHelper(operation, right));
	}

	@Override
	protected Result binaryOperationTyped(BinaryOperation operation, ResultColumn right) {
		return new ResultColumn(binaryOperationTypedHelper(operation, right.getColumn()));
	}

	@Override
	protected Result binaryOperationTyped(BinaryOperation operation, ResultList right) {
		throw new UnsupportedOperationException("BinaryOperation not supported for ResultColumn and ResultList.");
	}
}
