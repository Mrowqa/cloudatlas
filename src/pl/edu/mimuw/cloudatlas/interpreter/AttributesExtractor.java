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

public class AttributesExtractor {
	public static Set<String> extractAttributes(Program program) {
		return program.accept(new ProgramExtractor(), false /*unused*/);
	}

	public static class ProgramExtractor implements Program.Visitor<Set<String>, Boolean> {
		public Set<String> visit(ProgramC program, Boolean unused) {
			Set<String> results = new HashSet<>();
			for(Statement s : program.liststatement_) {
				try {
					Set<String> result = s.accept(new StatementExtractor(), unused);
					results.addAll(result);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(s), exception);
				}
			}
			return results;
		}
	}

	public static class StatementExtractor implements Statement.Visitor<Set<String>, Boolean> {
		public Set<String> visit(StatementC statement, Boolean unused) {
			Set<String> ret = new HashSet<>();

			for(SelItem selItem : statement.listselitem_) {
				try {
					Set<String> s = selItem.accept(new SelItemExtractor(), unused);
					ret.addAll(s);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(selItem), exception);
				}
			}
			return ret;
		}
	}

	public static class SelItemExtractor implements SelItem.Visitor<Set<String>, Boolean> {
		public Set<String> visit(SelItemC selItem, Boolean unused) {
			return new HashSet<>();
		}

		public Set<String> visit(AliasedSelItemC selItem, Boolean unused) {
			Set<String> result = new HashSet<>();
			result.add(selItem.qident_);
			return result;
		}
	}
}