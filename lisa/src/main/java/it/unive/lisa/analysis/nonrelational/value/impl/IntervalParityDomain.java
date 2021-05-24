package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.impl.numeric.Parity;
import it.unive.lisa.analysis.nonrelational.combination.ReducedCartesianProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

public class IntervalParityDomain extends ReducedCartesianProduct<IntervalParityDomain, Interval, Parity> {

	public IntervalParityDomain() {
		this(new Interval(), new Parity());
	}

	/**
	 * Builds the Cartesian product abstract domain.
	 *
	 * @param left  the left-hand side of the Cartesian product
	 * @param right the right-hand side of the Cartesian product
	 */
	protected IntervalParityDomain(Interval left, Parity right) {
		super(left, right);
	}

	@Override
	protected IntervalParityDomain mk(Interval left, Parity right) {
		return new IntervalParityDomain(left, right);
	}

	private boolean isIntervalParityInvalid(Interval interval, Parity parity) {
		if (interval.isSingleton() && !parity.isTop() && !parity.isBottom()) {
			Integer value = interval.getLow();
			Parity valueParity = Parity.getFromInt(value);
			return !valueParity.equals(parity);
		}
		return false;
	}

	@Override
	protected Interval rhoLeft(IntervalParityDomain domain) {
		Interval interval = domain.left;
		Parity parity = domain.right;

		if (isIntervalParityInvalid(interval, parity)) {
			return interval.bottom();
		}

		if (!interval.isTop() && !parity.isTop() && !interval.isBottom() && !parity.isBottom()) {
			if (interval.getLow() != null && Parity.getFromInt(interval.getLow() + 1).equals(parity)) {
				return new Interval(interval.getLow() + 1, interval.getHigh());
			}

			if (interval.getHigh() != null && Parity.getFromInt(interval.getHigh() - 1).equals(parity)) {
				return new Interval(interval.getLow(), interval.getHigh() - 1);
			}
		}
		return interval;
	}

	@Override
	protected Parity rhoRight(IntervalParityDomain domain) {
		Interval interval = domain.left;
		Parity parity = domain.right;

		if (isIntervalParityInvalid(interval, parity)) {
			return parity.bottom();
		}

		if (interval.isSingleton()) {
			Integer value = interval.getLow();
			return Parity.getFromInt(value);
		}

		return parity;
	}

	@Override
	public SemanticDomain.Satisfiability satisfies(ValueExpression expression,
			ValueEnvironment<IntervalParityDomain> environment, ProgramPoint pp) throws SemanticException {
		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == UnaryOperator.LOGICAL_NOT)
				return satisfies((ValueExpression) unary.getExpression(), environment, pp).negate();
		}

		// SPECIAL CASE: modulo 2 check equals can be satisfied checking only parity!
		if (expression instanceof BinaryExpression && ((BinaryExpression) expression).getLeft() instanceof BinaryExpression
				&& ((BinaryExpression) ((BinaryExpression) expression).getLeft()).getOperator() == BinaryOperator.NUMERIC_MOD) {
			BinaryExpression comparison = (BinaryExpression) expression;
			BinaryExpression modulo = (BinaryExpression) comparison.getLeft();

			IntervalParityDomain equalsValue = eval((ValueExpression) comparison.getRight(), environment, pp);
			IntervalParityDomain modValue = eval((ValueExpression) modulo.getRight(), environment, pp);
			IntervalParityDomain value = eval((ValueExpression) modulo.getLeft(), environment, pp);

			Integer equalsValueInt = equalsValue.left.isSingleton() ? equalsValue.left.getHigh() : -1;
			Integer modValueInt = modValue.left.isSingleton() ? modValue.left.getHigh() : -1;
			Parity valueParity = value.right;

			switch (comparison.getOperator()) {
				// value % modValue == equalsValue
				case COMPARISON_EQ:
					if (modValueInt.equals(2) && valueParity.equals(Parity.getFromInt(equalsValueInt))) {
						return SemanticDomain.Satisfiability.SATISFIED;
					} else {
						return SemanticDomain.Satisfiability.NOT_SATISFIED;
					}

				// value % modValue != equalsValue
				case COMPARISON_NE:
					if (modValueInt.equals(2) && valueParity.equals(Parity.getFromInt(equalsValueInt))) {
						return SemanticDomain.Satisfiability.NOT_SATISFIED;
					} else {
						return SemanticDomain.Satisfiability.SATISFIED;
					}
				default:
			}
		}

		return super.satisfies(expression, environment, pp);
	}
}
