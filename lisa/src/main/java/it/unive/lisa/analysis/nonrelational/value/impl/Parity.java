package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;

public class Parity {

	public static final Parity EVEN = new Parity();
	public static final Parity ODD = new Parity();
	public static final Parity TOP = new Parity();
	public static final Parity BOTTOM = new Parity();

	public static Parity getFromInt(Integer value) {
		if (value == null)
			return TOP;
		return value % 2 == 0 ? EVEN : ODD;
	}

	private Parity() {
	}

	public Parity top() {
		return TOP;
	}

	public boolean isTop() {
		return this == TOP;
	}

	public Parity bottom() {
		return BOTTOM;
	}

	public boolean isBottom() {
		return this == BOTTOM;
	}

	public boolean isEven() {
		return this == EVEN;
	}

	public boolean isOdd() {
		return this == ODD;
	}

	public String representation() {
		if (equals(BOTTOM))
			return Lattice.BOTTOM_STRING;
		else if (equals(EVEN))
			return "Even";
		else if (equals(ODD))
			return "Odd";
		else
			return Lattice.TOP_STRING;
	}

	@Override
	public String toString() {
		if (equals(BOTTOM))
			return Lattice.BOTTOM_STRING;
		else if (equals(EVEN))
			return "Even";
		else if (equals(ODD))
			return "Odd";
		else
			return Lattice.TOP_STRING;
	}

	public static Parity evalNullConstant(ProgramPoint pp) {
		return TOP;
	}

	public static Parity evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i % 2 == 0 ? EVEN : ODD;
		}

		return TOP;
	}

	public static Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp) {
		switch (operator) {
			case NUMERIC_NEG:
				return arg;
			default:
				return TOP;
		}
	}

	public static Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp) {
		if (left.isTop() || right.isTop())
			return TOP;

		switch (operator) {
			case NUMERIC_ADD:
			case NUMERIC_SUB:
				if (right.equals(left))
					return EVEN;
				else
					return ODD;
			case NUMERIC_MUL:
				if (left.isEven() || right.isEven())
					return EVEN;
				else
					return ODD;
			case NUMERIC_DIV:
				if (left.isOdd())
					return right.isOdd() ? ODD : EVEN;
				else
					return right.isOdd() ? EVEN : TOP;
			case NUMERIC_MOD:
				return TOP;
			default:
				return TOP;
		}
	}

	public Parity lubAux(Parity other) throws SemanticException {
		return TOP;
	}

	public Parity wideningAux(Parity other) throws SemanticException {
		return lubAux(other);
	}

	public boolean lessOrEqualAux(Parity other) throws SemanticException {
		return false;
	}

	public int hashCode() {
		if (this == BOTTOM)
			return 1;
		else if (this == EVEN)
			return 2;
		else if (this == ODD)
			return 3;
		else
			return 4;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return this == obj;
	}

	public <T extends NonRelationalValueDomain<T>> ValueEnvironment<T> assumeBinaryExpression(
			ValueEnvironment<T> environment, BinaryOperator operator, ValueExpression left, ValueExpression right,
			ProgramPoint pp) throws SemanticException {
		switch (operator) {
			case COMPARISON_EQ:
				if (left instanceof Identifier)
					environment = environment.assign((Identifier) left, right, pp);
				else if (right instanceof Identifier)
					environment = environment.assign((Identifier) right, left, pp);
				return environment;
			default:
				return environment;
		}
	}
}