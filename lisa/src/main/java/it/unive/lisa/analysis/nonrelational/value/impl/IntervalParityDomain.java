package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.combination.ReducedCartesianProduct;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.impl.numeric.Parity;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;

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
	protected IntervalParityDomain postEval(IntervalParityDomain result, ValueExpression expression,
											ValueEnvironment<IntervalParityDomain> environment, ProgramPoint pp) throws SemanticException {
		if (expression instanceof BinaryExpression) {
			BinaryOperator op = ((BinaryExpression) expression).getOperator();
			if (op == BinaryOperator.NUMERIC_MOD) {
				// ([a,b], p1) % ([c,d], p2)
				// leftExpr rightExpr

				ValueExpression leftExpr = (ValueExpression) ((BinaryExpression) expression).getLeft();
				ValueExpression rightExpr = (ValueExpression) ((BinaryExpression) expression).getRight();
				Parity parityLeft = right.eval(leftExpr, makeRightEnv(environment), pp);
				Parity parityRight = right.eval(rightExpr, makeRightEnv(environment), pp);

				// if (p1 == even && p2 == even) => p1
				// ([2,4], even) % ([8,10], even) => ([?,?], even)
				// ([3,3], odd) % ([2,2], even) => ([0,1], odd)
				if (parityRight.isEven()) {
					return mk(result.left, parityLeft);
				}
			}
		}

		return result;
	}
}
