package it.unive.lisa.analysis.impl.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.HashMap;
import java.util.Map;

/**
 * The Parity abstract domain, tracking if a numeric value is even or odd,
 * implemented as a {@link BaseNonRelationalValueDomain}, handling top and
 * bottom values for the expression evaluation and bottom values for the
 * expression satisfiability. Top and bottom cases for least upper bound,
 * widening and less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual} methods, respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Parity extends BaseNonRelationalValueDomain<Parity> {

	private static final Parity EVEN = new Parity(false, false);
	private static final Parity ODD = new Parity(false, false);
	private static final Parity TOP = new Parity();
	private static final Parity BOTTOM = new Parity(false, true);

	private final boolean isTop, isBottom;

	/**
	 * Builds the parity abstract domain, representing the top of the parity
	 * abstract domain.
	 */
	public Parity() {
		this(true, false);
	}

	private Parity(boolean isTop, boolean isBottom) {
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	@Override
	public Parity top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public Parity bottom() {
		return BOTTOM;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.BOTTOM_REPR;
		if (isTop())
			return Lattice.TOP_REPR;

		String repr;
		if (this == EVEN)
			repr = "Even";
		else
			repr = "Odd";

		return new StringRepresentation(repr);
	}

	public static Parity getFromInt(Integer x){
		if (x != null) {
			return x % 2 == 0 ? EVEN : ODD;
		}
		return TOP;
	}

	@Override
	protected Parity evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	protected Parity evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i % 2 == 0 ? EVEN : ODD;
		}

		return top();
	}

	public boolean isEven() {
		return this == EVEN;
	}

	public boolean isOdd() {
		return this == ODD;
	}

	@Override
	protected Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_NEG:
			return arg;
		default:
			return top();
		}
	}

	@Override
	protected Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_ADD:
		case NUMERIC_SUB:
			if (left.isTop() || right.isTop()){
				return top();
			}
			if (right.equals(left))
				return EVEN;
			else
				return ODD;
		case NUMERIC_MUL:
			if (left.isEven() || right.isEven())
				return EVEN;
			if (left.isTop() || right.isTop()){
				return top();
			}
			else
				return ODD;
		case NUMERIC_DIV:
			if (left.isTop() || right.isTop()){
				return top();
			}
			if (left.isOdd())
				return right.isOdd() ? ODD : EVEN;
			else
				return right.isOdd() ? EVEN : TOP;
		case NUMERIC_MOD:
			// if (p1 == even && p2 == even) => p1
			// ([2,4], even) % ([8,10], even) => ([?,?], even)
			// ([3,3], odd) % ([2,2], even) => ([0,1], odd)
			if (right.isEven()) {
				return left;
			}
			return TOP;
		default:
			return TOP;
		}
	}

	@Override
	protected Parity lubAux(Parity other) throws SemanticException {
		return TOP;
	}

	@Override
	protected Parity wideningAux(Parity other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(Parity other) throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		if (isBottom())
			return 1;
		else if (this == EVEN)
			return 2;
		else if (this == ODD)
			return 3;
		else
			return 4;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parity other = (Parity) obj;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		return isTop && other.isTop;
	}

	private boolean isModTwo(ValueExpression exp) {
		return exp instanceof BinaryExpression && ((BinaryExpression) exp).getOperator() == BinaryOperator.NUMERIC_MOD
				&& ((BinaryExpression) exp).getRight() instanceof Constant
				&& ((Constant) ((BinaryExpression) exp).getRight()).getValue().equals(2);
	}

	@Override
	protected ValueEnvironment<Parity> assumeBinaryExpression(ValueEnvironment<Parity> environment,
			BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint pp) throws SemanticException {
		Map<Identifier, Parity> map = null;

		if (environment.getMap() == null)
			map = new HashMap<>();
		else
			map = new HashMap<>(environment.getMap());

		switch (operator) {
			case COMPARISON_EQ:
				if (left instanceof Identifier)
					environment = environment.assign((Identifier) left, right, pp);
				else if (right instanceof Identifier)
					environment = environment.assign((Identifier) right, left, pp);

				// If we check c % 2 == 0 we can satisfy it with parity only!
				if (isModTwo(left) && right instanceof Constant) {
					Identifier ident = (Identifier) ((BinaryExpression) left).getLeft();
					if (((Constant) right).getValue().equals(0))
						map.put(ident, EVEN);
					else if (((Constant) right).getValue().equals(1))
						map.put(ident, ODD);
					return new ValueEnvironment<>(bottom(), map);
				} else if (isModTwo(right)) {
					Identifier ident = (Identifier) ((BinaryExpression) right).getLeft();
					if (((Constant) right).getValue().equals(0))
						map.put(ident, EVEN);
					else if (((Constant) right).getValue().equals(1))
						map.put(ident, ODD);
					return new ValueEnvironment<>(bottom(), map);
				}
				return environment;

			case COMPARISON_NE:
				// same for not equal
				if (isModTwo(left)) {
					Identifier ident = (Identifier) ((BinaryExpression) left).getLeft();
					if (((Constant) right).getValue().equals(0))
						map.put(ident, ODD);
					else if (((Constant) right).getValue().equals(1))
						map.put(ident, EVEN);
					return new ValueEnvironment<>(bottom(), map);
				} else if (isModTwo(right)) {
					Identifier ident = (Identifier) ((BinaryExpression) right).getLeft();
					if (((Constant) right).getValue().equals(0))
						map.put(ident, ODD);
					else if (((Constant) right).getValue().equals(1))
						map.put(ident, EVEN);
					return new ValueEnvironment<>(bottom(), map);
				}
				return environment;
			default:
				return environment;
		}
	}
}