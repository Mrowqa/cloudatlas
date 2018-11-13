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

import java.util.HashSet;
import java.util.Set;

import pl.edu.mimuw.cloudatlas.interpreter.query.PrettyPrinter;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.AliasedSelItemC;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.Program;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.ProgramC;

import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.SelItem;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.SelItemC;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.Statement;
import pl.edu.mimuw.cloudatlas.interpreter.query.Absyn.StatementC;
import pl.edu.mimuw.cloudatlas.model.ZMI;

public class AttributesExtractor {
	private final ZMI zmi;

	public AttributesExtractor(ZMI zmi) {
		this.zmi = zmi;
	}

	public Set<String> extractAttributes(Program program) {
		return program.accept(new ProgramExtractor(), zmi);
	}

	public Set<String> interpretStatement(Statement statement) {
		return statement.accept(new StatementExtractor(), zmi);
	}

	public class ProgramExtractor implements Program.Visitor<Set<String>, ZMI> {
		public Set<String> visit(ProgramC program, ZMI zmi) {
			Set<String> results = new HashSet<>();
			for(Statement s : program.liststatement_) {
				try {
					Set<String> result = s.accept(new StatementExtractor(), zmi);
					results.addAll(result);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(s), exception);
				}
			}
			return results;
		}
	}

	public class StatementExtractor implements Statement.Visitor<Set<String>, ZMI> {
		public Set<String> visit(StatementC statement, ZMI zmi) {
			Set<String> ret = new HashSet<>();

			for(SelItem selItem : statement.listselitem_) {
				try {
					Set<String> s = selItem.accept(new SelItemExtractor(), zmi);
					ret.addAll(s);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(selItem), exception);
				}
			}
			return ret;
		}
	}

	public class SelItemExtractor implements SelItem.Visitor<Set<String>, ZMI> {
		public Set<String> visit(SelItemC selItem, ZMI zmi) {
			return new HashSet<>();
		}

		public Set<String> visit(AliasedSelItemC selItem, ZMI zmi) {
			Set<String> result = new HashSet<>();
			result.add(selItem.qident_);
			return result;
		}
	}
}
