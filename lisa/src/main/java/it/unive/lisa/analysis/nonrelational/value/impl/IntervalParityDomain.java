package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;

import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class IntervalParityDomain extends BaseNonRelationalValueDomain<IntervalParityDomain> {
	private final Interval interval;
	private final Parity parity;

	public IntervalParityDomain() {
		this(Interval.TOP, Parity.TOP);
	}

	private IntervalParityDomain(Interval interval, Parity parity) {
		Interval reducedInterval = new Interval(interval);
		Parity reducedParity = parity;
		Interval previousInterval;
		Parity previousParity;
		// Granger's product impl
		do {
			previousInterval = new Interval(reducedInterval);
			previousParity = reducedParity;
			reducedInterval = reduceInterval(previousInterval, previousParity);
			reducedParity = reduceParity(previousParity, previousInterval);
		} while (!reducedInterval.equals(previousInterval) || reducedParity != previousParity);
		this.interval = reducedInterval;
		this.parity = reducedParity;
	}

	private Interval reduceInterval(Interval interval, Parity parity) {
		// [0,6] + Odd => [1,5]
		if (!interval.isBottom && !interval.isTop && !parity.isBottom() && !parity.isTop()) {
			Parity highParity = Parity.getFromInt(interval.high);
			Parity lowParity = Parity.getFromInt(interval.low);

			Integer newHigh = interval.high;
			Integer newLow = interval.low;

			if (highParity != parity && newHigh != null) {
				newHigh--;
			}

			if (lowParity != parity && newLow != null) {
				newLow++;
			}

			return new Interval(newLow, newHigh);
		}
		return interval;
	}

	private Parity reduceParity(Parity parity, Interval interval) {
		// [N,N]+TOP where N ODD => [N,N]+ODD
		// [N,N]+TOP where N EVEN => [N,N]+EVEN

		// [N,N]+ODD where N EVEN => [N,N]+TOP
		// [N,N]+EVEN where N ODD => [N,N]+TOP

		// [2,2]+ODD =>

		// else return parity
		if (interval.isSingleton()) {
			return Parity.getFromInt(interval.low);
		}
		return !parity.equals(Parity.getFromInt(interval.low)) ? Parity.TOP : parity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((interval == null) ? 0 : interval.hashCode());
		result = prime * result + ((parity == null) ? 0 : parity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntervalParityDomain other = (IntervalParityDomain) obj;
		if (interval != other.interval)
			return false;
		if (parity != other.parity)
			return false;
		return true;
	}

	@Override
	public IntervalParityDomain top() {
		return new IntervalParityDomain();
	}

	@Override
	public boolean isTop() {
		return interval.isTop && parity == Parity.TOP;
	}

	@Override
	public IntervalParityDomain bottom() {
		return new IntervalParityDomain(Interval.BOTTOM, Parity.BOTTOM);
	}

	@Override
	public boolean isBottom() {
		return interval.isBottom && parity == Parity.BOTTOM;
	}

	@Override
	public StringRepresentation representation() {
		return new StringRepresentation("[" + interval.toString() + " ; " + parity.toString() + "]");
	}

	@Override
	protected IntervalParityDomain lubAux(IntervalParityDomain other) throws SemanticException {
		return top();
	}

	@Override
	protected IntervalParityDomain wideningAux(IntervalParityDomain other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(IntervalParityDomain other) throws SemanticException {
		return false;
	}

	@Override
	protected IntervalParityDomain evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	protected IntervalParityDomain evalNonNullConstant(Constant constant, ProgramPoint pp) {
		Interval newInterval = Interval.evalNonNullConstant(constant, pp);
		Parity newParity = Parity.evalNonNullConstant(constant, pp);
		return new IntervalParityDomain(newInterval, newParity);
	}

	@Override
	protected IntervalParityDomain evalUnaryExpression(UnaryOperator operator, IntervalParityDomain arg,
			ProgramPoint pp) {
		Interval newInterval = Interval.evalUnaryExpression(operator, arg.interval, pp);
		Parity newParity = Parity.evalUnaryExpression(operator, arg.parity, pp);
		return new IntervalParityDomain(newInterval, newParity);
	}

	@Override
	protected IntervalParityDomain evalBinaryExpression(BinaryOperator operator, IntervalParityDomain left,
			IntervalParityDomain right, ProgramPoint pp) {

		Interval newInterval = Interval.evalBinaryExpression(operator, left.interval, right.interval, pp);
		Parity newParity = Parity.evalBinaryExpression(operator, left.parity, right.parity, pp);
		return new IntervalParityDomain(newInterval, newParity);
	}

	@Override
	protected IntervalParityDomain evalTernaryExpression(TernaryOperator operator, IntervalParityDomain left,
			IntervalParityDomain middle, IntervalParityDomain right, ProgramPoint pp) {
		return top();
	}
}
