package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.PairRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public abstract class ReducedCartesianProduct<C extends ReducedCartesianProduct<C, T1, T2>, T1 extends NonRelationalValueDomain<T1> & Lattice<T1>, T2 extends NonRelationalValueDomain<T2> & Lattice<T2>>
		implements NonRelationalValueDomain<C>, Lattice<C> {

	/**
	 * The left-hand side abstract domain.
	 */
	protected T1 left;

	/**
	 * The right-hand side abstract domain.
	 */
	protected T2 right;

	/**
	 * Builds the Cartesian product abstract domain.
	 *
	 * @param left  the left-hand side of the Cartesian product
	 * @param right the right-hand side of the Cartesian product
	 */
	protected ReducedCartesianProduct(T1 left, T2 right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
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
		ReducedCartesianProduct<?, ?, ?> other = (ReducedCartesianProduct<?, ?, ?>) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		if (left instanceof Environment && right instanceof Environment) {
			Environment<?, ?, ?, ?> leftEnv = (Environment<?, ?, ?, ?>) left;
			Environment<?, ?, ?, ?> rightEnv = (Environment<?, ?, ?, ?>) right;
			String result = "";
			if (!leftEnv.isTop() && !leftEnv.isBottom()) {
				for (Identifier x : leftEnv.getKeys())
					result += x + ": (" + leftEnv.getState(x).representation() + ", " + rightEnv.getState(x).representation()
							+ ")\n";
				return result;
			} else if (!rightEnv.isTop() && !rightEnv.isBottom()) {
				for (Identifier x : rightEnv.getKeys())
					result += x + ": (" + leftEnv.getState(x).representation() + ", " + rightEnv.getState(x).representation()
							+ ")\n";
				return result;
			}
		}

		return "(" + left.representation() + ", " + right.representation() + ")";
	}

	/**
	 * Builds a new instance of cartesian product.
	 *
	 * @param left  the first domain
	 * @param right the second domain
	 * @return the new instance of product
	 */
	protected abstract C mk(T1 left, T2 right);

	protected abstract T1 rhoLeft(C domain);

	protected abstract T2 rhoRight(C domain);

	protected abstract C postEval(C result, ValueExpression expression, ValueEnvironment<C> environment, ProgramPoint pp)
			throws SemanticException;

	private C grangerProduct(C value) {
		T1 reducedLeft = value.left;
		T2 reducedRight = value.right;
		T1 previousLeft;
		T2 previousRight;
		do {
			previousLeft = reducedLeft;
			previousRight = reducedRight;
			reducedLeft = rhoLeft(mk(previousLeft, previousRight));
			reducedRight = rhoRight(mk(previousLeft, previousRight));
		} while (!reducedLeft.equals(previousLeft) || reducedRight != previousRight);
		return mk(reducedLeft, reducedRight);
	}

	private C grangerProductExp(C value, ValueExpression expression, ValueEnvironment<C> environment, ProgramPoint pp)
			throws SemanticException {
		T1 reducedLeft = value.left;
		T2 reducedRight = value.right;
		T1 previousLeft;
		T2 previousRight;
		do {
			previousLeft = reducedLeft;
			previousRight = reducedRight;
			C postEvalProcessed = postEval(mk(reducedLeft, reducedRight), expression, environment, pp);
			reducedLeft = rhoLeft(postEvalProcessed);
			reducedRight = rhoRight(postEvalProcessed);
		} while (!reducedLeft.equals(previousLeft) || reducedRight != previousRight);
		return mk(reducedLeft, reducedRight);
	}

	protected ValueEnvironment<T1> makeLeftEnv(ValueEnvironment<C> environment) {
		ValueEnvironment<T1> newEnv = new ValueEnvironment<T1>(left);
		for (var i : environment) {
			newEnv.getMap().put(i.getKey(), i.getValue().left);
		}
		return newEnv;
	}

	protected ValueEnvironment<T2> makeRightEnv(ValueEnvironment<C> environment) {
		ValueEnvironment<T2> newEnv = new ValueEnvironment<T2>(right);
		for (var i : environment) {
			newEnv.getMap().put(i.getKey(), i.getValue().right);
		}
		return newEnv;
	}

	protected ValueEnvironment<C> mergeEnv(ValueEnvironment<T1> le, ValueEnvironment<T2> re) {
		ValueEnvironment<C> newEnv = new ValueEnvironment<C>(mk(left, right));
		for (var k : le.getKeys()) {
			newEnv.getMap().put(k, mk(le.getMap().get(k), re.getMap().get(k)));
		}
		return newEnv;
	}

	@Override
	public C eval(ValueExpression expression, ValueEnvironment<C> environment, ProgramPoint pp) throws SemanticException {
		C value = mk(left.eval(expression, makeLeftEnv(environment), pp),
				right.eval(expression, makeRightEnv(environment), pp));
		return grangerProductExp(value, expression, environment, pp);
	}

	@Override
	public SemanticDomain.Satisfiability satisfies(ValueExpression expression, ValueEnvironment<C> environment,
			ProgramPoint pp) throws SemanticException {
		SemanticDomain.Satisfiability sLeft = left.satisfies(expression, makeLeftEnv(environment), pp);
		SemanticDomain.Satisfiability sRight = right.satisfies(expression, makeRightEnv(environment), pp);
		return sLeft.and(sRight);
	}

	@Override
	public ValueEnvironment<C> assume(ValueEnvironment<C> environment, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		ValueEnvironment<T1> leftAssume = left.assume(makeLeftEnv(environment), expression, pp);
		ValueEnvironment<T2> rightAssume = right.assume(makeRightEnv(environment), expression, pp);
		return mergeEnv(leftAssume, rightAssume);
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return left.tracksIdentifiers(id) && right.tracksIdentifiers(id);
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return left.canProcess(expression) && right.canProcess(expression);
	}

	@Override
	public final DomainRepresentation representation() {
		return new PairRepresentation(left.representation(), right.representation());
	}

	@Override
	public final C lub(C other) throws SemanticException {
		return grangerProduct(mk(left.lub(other.left), right.lub(other.right)));
	}

	@Override
	public C glb(C other) throws SemanticException {
		return grangerProduct(mk(left.glb(other.left), right.glb(other.right)));
	}

	@Override
	public final C widening(C other) throws SemanticException {
		return grangerProduct(mk(left.widening(other.left), right.widening(other.right)));
	}

	@Override
	public final boolean lessOrEqual(C other) throws SemanticException {
		return left.lessOrEqual(other.left) && right.lessOrEqual(other.right);
	}

	@Override
	public final C top() {
		return mk(left.top(), right.top());
	}

	@Override
	public boolean isTop() {
		return left.isTop() && right.isTop();
	}

	@Override
	public final C bottom() {
		return mk(left.bottom(), right.bottom());
	}

	@Override
	public boolean isBottom() {
		return left.isBottom() && right.isBottom();
	}
}