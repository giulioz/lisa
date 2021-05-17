package it.unive.lisa.analysis.nonrelational.value.impl;


import it.unive.lisa.analysis.combination.ReducedCartesianProduct;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.impl.numeric.Parity;

public class IntervalParityDomain2 extends ReducedCartesianProduct<IntervalParityDomain2, Interval, Parity> {

	public IntervalParityDomain2() {
		this(new Interval(), new Parity());
	}

	/**
	 * Builds the Cartesian product abstract domain.
	 *
	 * @param left  the left-hand side of the Cartesian product
	 * @param right the right-hand side of the Cartesian product
	 */
	protected IntervalParityDomain2(Interval left, Parity right) {
		super(left, right);
	}

	@Override
	protected IntervalParityDomain2 mk(Interval left, Parity right) {
		return new IntervalParityDomain2(left, right);
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
	protected Interval rhoLeft(IntervalParityDomain2 domain) {
		Interval interval = domain.left;
		Parity parity = domain.right;
		
		if (isIntervalParityInvalid(interval, parity)){
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

		// if (!interval.isBottom() && !interval.isTop() && !parity.isBottom() && !parity.isTop()) {
		// 	Parity highParity = Parity.getFromInt(interval.getHigh());
		// 	Parity lowParity = Parity.getFromInt(interval.getLow());

		// 	Integer newHigh = interval.getHigh();
		// 	Integer newLow = interval.getLow();

		// 	if (highParity != parity && newHigh != null) {
		// 		newHigh--;
		// 	}

		// 	if (lowParity != parity && newLow != null) {
		// 		newLow++;
		// 	}

		// 	return new Interval(newLow, newHigh);
		// }
		return interval;
	}

	@Override
	protected Parity rhoRight(IntervalParityDomain2 domain) {
		// [N,N]+TOP where N ODD => [N,N]+ODD
		// [N,N]+TOP where N EVEN => [N,N]+EVEN

		// [N,N]+ODD where N EVEN => [N,N]+TOP
		// [N,N]+EVEN where N ODD => [N,N]+TOP

		// [2,2]+ODD => BOTTOM

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
}
