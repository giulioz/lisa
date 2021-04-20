package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Interval {

	public static final Interval TOP = new Interval(null, null, true, false);
	public static final Interval BOTTOM = new Interval(null, null, false, true);

	public final boolean isTop, isBottom;

	public final Integer low;
	public final Integer high;

	public Interval(Integer low, Integer high, boolean isTop, boolean isBottom) {
		this.low = low;
		this.high = high;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	public Interval(Integer low, Integer high) {
		this(low, high, false, false);
	}

	/**
	 * Builds the top interval.
	 */
	public Interval() {
		this(null, null, true, false);
	}

	@Override
	public String toString() {
		if (isTop)
			return Lattice.TOP_STRING;
		else if (isBottom)
			return Lattice.BOTTOM_STRING;

		return "[" + (lowIsMinusInfinity() ? "-Inf" : low) + ", " + (highIsPlusInfinity() ? "+Inf" : high) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((high == null) ? 0 : high.hashCode());
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + (isTop ? 1231 : 1237);
		result = prime * result + ((low == null) ? 0 : low.hashCode());
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
		Interval other = (Interval) obj;
		if (high == null) {
			if (other.high != null)
				return false;
		} else if (!high.equals(other.high))
			return false;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		if (low == null) {
			if (other.low != null)
				return false;
		} else if (!low.equals(other.low))
			return false;
		return true;
	}

	public static Interval evalNullConstant(ProgramPoint pp) {
		return TOP;
	}

	public static Interval evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return new Interval(i, i);
		}

		return TOP;
	}

	public static Interval evalUnaryExpression(UnaryOperator operator, Interval arg, ProgramPoint pp) {
		switch (operator) {
			case NUMERIC_NEG:
				if (arg.isTop)
					return TOP;
				return arg.mul(new Interval(-1, -1));
			case STRING_LENGTH:
				return new Interval(0, null);
			default:
				return TOP;
		}
	}

	public boolean is(int n) {
		if (low == null || high == null)
			return false;

		return low == n && high == n;
	}

	public static Interval evalBinaryExpression(BinaryOperator operator, Interval left, Interval right, ProgramPoint pp) {
		switch (operator) {
			case NUMERIC_ADD:
				if (left.isTop || right.isTop)
					return TOP;
				return left.add(right);
			case NUMERIC_SUB:
				if (left.isTop || right.isTop)
					return TOP;
				return left.sub(right);
			case NUMERIC_MUL:
				if (left.is(0) || right.is(0))
					return new Interval(0, 0);

				if (left.isTop || right.isTop)
					return TOP;

				return left.mul(right);
			case NUMERIC_DIV:
				if (right.is(0))
					return BOTTOM;

				if (left.is(0))
					return new Interval(0, 0);

				if (left.isTop || right.isTop)
					return TOP;

				return left.div(right);
			case NUMERIC_MOD:
				return TOP;
			default:
				return TOP;
		}
	}
	
	public Interval evalTernaryExpression(TernaryOperator operator, Interval left, Interval middle, Interval right,
																				ProgramPoint pp) {
		return TOP;
	}

	public Interval lubAux(Interval other) throws SemanticException {
		Integer newLow = lowIsMinusInfinity() || other.lowIsMinusInfinity() ? null : Math.min(low, other.low);
		Integer newHigh = highIsPlusInfinity() || other.highIsPlusInfinity() ? null : Math.max(high, other.high);
		return new Interval(newLow, newHigh);
	}

	public Interval glbAux(Interval other) {
		Integer newLow = lowIsMinusInfinity() ? other.low : other.lowIsMinusInfinity() ? low : Math.max(low, other.low);
		Integer newHigh = highIsPlusInfinity() ? other.high
						: other.highIsPlusInfinity() ? high : Math.min(high, other.high);
		return new Interval(newLow, newHigh);
	}

	public Interval wideningAux(Interval other) throws SemanticException {
		Integer newLow, newHigh;
		if (other.highIsPlusInfinity() || (!highIsPlusInfinity() && other.high > high))
			newHigh = null;
		else
			newHigh = other.high;

		if (other.lowIsMinusInfinity() || (!lowIsMinusInfinity() && other.low < low))
			newLow = null;
		else
			newLow = other.low;

		return new Interval(newLow, newHigh);
	}

	public boolean lessOrEqualAux(Interval other) throws SemanticException {
		return geqLow(low, other.low) && leqHigh(high, other.high);
	}

	public boolean lowIsMinusInfinity() {
		return low == null;
	}

	public boolean highIsPlusInfinity() {
		return high == null;
	}

	public Interval add(Interval other) {
		Integer newLow, newHigh;

		if (lowIsMinusInfinity() || other.lowIsMinusInfinity())
			newLow = null;
		else
			newLow = low + other.low;

		if (highIsPlusInfinity() || other.highIsPlusInfinity())
			newHigh = null;
		else
			newHigh = high + other.high;

		return new Interval(newLow, newHigh);
	}

	public Interval sub(Interval other) {
		Integer newLow, newHigh;

		if (other.highIsPlusInfinity() || lowIsMinusInfinity())
			newLow = null;
		else
			newLow = low - other.high;

		if (other.lowIsMinusInfinity() || highIsPlusInfinity())
			newHigh = null;
		else
			newHigh = high - other.low;

		return new Interval(newLow, newHigh);
	}

	public Interval mul(Interval other) {
		// this = [l1, h1]
		// other = [l2, h2]

		SortedSet<Integer> boundSet = new TreeSet<>();
		Integer l1 = low;
		Integer h1 = high;
		Integer l2 = other.low;
		Integer h2 = other.high;

		AtomicBoolean lowInf = new AtomicBoolean(false), highInf = new AtomicBoolean(false);

		// l1 * l2
		multiplyBounds(boundSet, l1, l2, lowInf, highInf);

		// x1 * y2
		multiplyBounds(boundSet, l1, h2, lowInf, highInf);

		// x2 * y1
		multiplyBounds(boundSet, h2, l2, lowInf, highInf);

		// x2 * y2
		multiplyBounds(boundSet, h1, h2, lowInf, highInf);

		return new Interval(lowInf.get() ? null : boundSet.first(), highInf.get() ? null : boundSet.last());
	}

	public Interval div(Interval other) {
		// this = [l1, h1]
		// other = [l2, h2]

		SortedSet<Integer> boundSet = new TreeSet<>();
		Integer l1 = low;
		Integer h1 = high;
		Integer l2 = other.low;
		Integer h2 = other.high;

		AtomicBoolean lowInf = new AtomicBoolean(false), highInf = new AtomicBoolean(false);

		// l1 / l2
		divideBounds(boundSet, l1, l2, lowInf, highInf);

		// x1 / y2
		divideBounds(boundSet, l1, h2, lowInf, highInf);

		// x2 / y1
		divideBounds(boundSet, h2, l2, lowInf, highInf);

		// x2 / y2
		divideBounds(boundSet, h1, h2, lowInf, highInf);

		return new Interval(lowInf.get() ? null : boundSet.first(), highInf.get() ? null : boundSet.last());
	}

	public Interval mod(Interval other) {
		return TOP;
	}

	public void multiplyBounds(SortedSet<Integer> boundSet, Integer i, Integer j, AtomicBoolean lowInf,
														 AtomicBoolean highInf) {
		if (i == null) {
			if (j == null)
				// -inf * -inf = +inf
				highInf.set(true);
			else {
				if (j > 0)
					// -inf * positive
					lowInf.set(true);
				else if (j < 0)
					// -inf * negative
					highInf.set(true);
				else
					boundSet.add(0);
			}
		} else if (j == null) {
			if (i > 0)
				// -inf * positive
				lowInf.set(true);
			else if (i < 0)
				// -inf * negative
				highInf.set(true);
			else
				boundSet.add(0);
		} else
			boundSet.add(i * j);
	}

	public void divideBounds(SortedSet<Integer> boundSet, Integer i, Integer j, AtomicBoolean lowInf,
													 AtomicBoolean highInf) {
		if (i == null) {
			if (j == null)
				// -inf * -inf = +inf
				highInf.set(true);
			else {
				if (j > 0)
					// -inf * positive
					lowInf.set(true);
				else if (j < 0)
					// -inf * negative
					highInf.set(true);

				// division by zero!
			}
		} else if (j == null) {
			if (i > 0)
				// -inf * positive
				lowInf.set(true);
			else if (i < 0)
				// -inf * negative
				highInf.set(true);
			else
				boundSet.add(0);
		} else if (j != 0) {
			boundSet.add((int) Math.ceil(i / (double) j));
			boundSet.add((int) Math.floor(i / (double) j));
		}
		// division by zero!
	}

	/**
	 * Given two interval lower bounds, yields {@code true} iff l1 >= l2, taking
	 * into account -Inf values (i.e., when l1 or l2 is {@code null}.) This
	 * method is used for the implementation of {@link Interval#lessOrEqualAux}.
	 *
	 * @param l1 the lower bound of the first interval.
	 * @param l2 the lower bounds of the second interval.
	 * @return {@code true} iff iff l1 >= l2, taking into account -Inf values;
	 */
	public boolean geqLow(Integer l1, Integer l2) {
		if (l1 == null) {
			if (l2 == null)
				return true;
			else
				return false;
		} else {
			if (l2 == null)
				return true;
			else
				return l1 >= l2;
		}
	}

	/**
	 * Given two interval upper bounds, yields {@code true} iff h1 <= h2, taking
	 * into account +Inf values (i.e., when h1 or h2 is {@code null}.) This
	 * method is used for the implementation of {@link Interval#lessOrEqualAux}.
	 *
	 * @param h1 the upper bound of the first interval.
	 * @param h2 the upper bounds of the second interval.
	 * @return {@code true} iff iff h1 <= h2, taking into account +Inf values;
	 */
	public boolean leqHigh(Integer h1, Integer h2) {
		if (h1 == null) {
			if (h2 == null)
				return true;
			else
				return false;
		} else {
			if (h2 == null)
				return false;
			else
				return h1 <= h2;
		}
	}

	public <T extends NonRelationalValueDomain<T>> ValueEnvironment<T> assumeBinaryExpression(
					ValueEnvironment<T> environment, BinaryOperator operator, ValueExpression left,
					ValueExpression right, ProgramPoint pp) throws SemanticException {
						return environment;
		// switch (operator) {
		// 	case COMPARISON_EQ:
		// 		if (left instanceof Identifier)
		// 			environment = environment.assign((Identifier) left, right, pp);
		// 		else if (right instanceof Identifier)
		// 			environment = environment.assign((Identifier) right, left, pp);
		// 		return environment;
		// 	case COMPARISON_GE:
		// 		if (left instanceof Identifier) {
		// 			Interval rightEval = eval(right, environment, pp);
		// 			if (rightEval.lowIsMinusInfinity())
		// 				return environment;

		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			Interval bound = new Interval(rightEval.low, null);
		// 			map.put((Identifier) left, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else if (right instanceof Identifier) {
		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			Interval leftEval = eval(left, environment, pp);
		// 			Interval bound = leftEval.lowIsMinusInfinity() ? leftEval : new Interval(null, leftEval.low);
		// 			map.put((Identifier) right, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else
		// 			return environment;
		// 	case COMPARISON_GT:
		// 		if (left instanceof Identifier) {
		// 			Interval rightEval = eval(right, environment, pp);
		// 			if (rightEval.lowIsMinusInfinity())
		// 				return environment;

		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			Interval bound = new Interval(rightEval.low + 1, null);
		// 			map.put((Identifier) left, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else if (right instanceof Identifier) {
		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			Interval leftEval = eval(left, environment, pp);
		// 			Interval bound = leftEval.lowIsMinusInfinity() ? leftEval : new Interval(null, leftEval.low - 1);
		// 			map.put((Identifier) right, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else
		// 			return environment;
		// 	case COMPARISON_LE:
		// 		if (left instanceof Identifier) {
		// 			Interval rightEval = eval(right, environment, pp);
		// 			Interval bound = rightEval.lowIsMinusInfinity() ? rightEval : new Interval(null, rightEval.low);
		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			map.put((Identifier) left, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else if (right instanceof Identifier) {
		// 			Interval leftEval = eval(left, environment, pp);
		// 			if (leftEval.lowIsMinusInfinity())
		// 				return environment;

		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			Interval bound = new Interval(leftEval.low, null);
		// 			map.put((Identifier) right, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else
		// 			return environment;
		// 	case COMPARISON_LT:
		// 		if (left instanceof Identifier) {
		// 			Interval rightEval = eval(right, environment, pp);
		// 			Interval bound = rightEval.lowIsMinusInfinity() ? rightEval : new Interval(null, rightEval.low - 1);
		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			map.put((Identifier) left, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else if (right instanceof Identifier) {
		// 			Interval leftEval = eval(left, environment, pp);
		// 			if (leftEval.lowIsMinusInfinity())
		// 				return environment;

		// 			Map<Identifier, Interval> map = new HashMap<>(environment.getMap());
		// 			Interval bound = new Interval(leftEval.low + 1, null);
		// 			map.put((Identifier) right, bound);
		// 			return new ValueEnvironment<T>(BOTTOM, map);
		// 		} else
		// 			return environment;
		// 	default:
		// 		return environment;
		// }
	}
}
