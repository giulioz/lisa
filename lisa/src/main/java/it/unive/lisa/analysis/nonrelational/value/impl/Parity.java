package it.unive.lisa.analysis.nonrelational.value.impl;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;

enum Parity {
	ODD {
		@Override
		Parity minus() {
			return ODD;
		}

		@Override
		Parity add(Parity other) {
			// odd + even = odd
			// odd + odd = even
			// odd + top = top
			// odd + bottom = bottom
			if (other == EVEN) return ODD;
			if (other == ODD) return EVEN;
			return other;
		}

		@Override
		Parity div(Parity other) {
			// Can we calculate parity with div?
			// Apparently not...
			if (other == BOTTOM) return BOTTOM;
			return TOP;
		}

		@Override
		Parity mul(Parity other) {
			// odd × even = even
			// odd × odd = odd
			// odd × top = top
			// odd × bottom = bottom
			if (other == EVEN) return EVEN;
			if (other == ODD) return ODD;
			return other;
		}

		@Override
		Parity mod(Parity other) {
			// odd % even = odd   proof by induction?
			// odd % odd = top		3 % 3 = even && 1 % 3 = odd 
			// odd % top = top  
			// odd % bottom = bottom
			if (other == EVEN) return ODD;
			if (other == ODD) return TOP;
			return other;
		}

		@Override
		public String toString() {
			return "ODD";
		}
	},

	EVEN {
		@Override
		Parity minus() {
			return EVEN;
		}

		@Override
		Parity add(Parity other) {
			// even + even = even
			// even + odd = odd
			// even + top = top
			// even + bottom = bottom
			if (other == ODD) return ODD;
			if (other == EVEN) return EVEN;
			return other;
		}

		@Override
		Parity div(Parity other) {
			if (other == BOTTOM) return BOTTOM;
			return TOP;
		}

		@Override
		Parity mul(Parity other) {
			// even × even = even
			// even × odd = even
			// even × top = top
			// even × bottom = bottom
			if (other == EVEN) return EVEN;
			if (other == ODD) return ODD;
			return other;
		}

		@Override
		Parity mod(Parity other) {
			// even % even = even  proof by induction?
			// even % odd = top		2 % 3 = odd && 0 % 3 = even 
			// even % top = top  
			// even % bottom = bottom
			if (other == EVEN) return EVEN;
			if (other == ODD) return TOP;
			return other;
		}

		@Override
		public String toString() {
			return "EVEN";
		}
	},

	TOP {
		@Override
		Parity minus() {
			return TOP;
		}

		@Override
		Parity add(Parity other) {
			if (other == BOTTOM) return BOTTOM;
			return TOP;
		}

		@Override
		Parity div(Parity other) {
			if (other == BOTTOM) return BOTTOM;
			return TOP;
		}

		@Override
		Parity mul(Parity other) {
			if (other == BOTTOM) return BOTTOM;
			return TOP;
		}

		@Override
		Parity mod(Parity other) {
			if (other == BOTTOM) return BOTTOM;
			return TOP;
		}

		@Override
		public String toString() {
			return Lattice.TOP_STRING;
		}
	},

	BOTTOM {
		@Override
		Parity minus() {
			return BOTTOM;
		}

		@Override
		Parity add(Parity other) {
			return BOTTOM;
		}

		@Override
		Parity div(Parity other) {
			return BOTTOM;
		}

		@Override
		Parity mul(Parity other) {
			return BOTTOM;
		}

		@Override
		Parity mod(Parity other) {
			return BOTTOM;
		}

		@Override
		public String toString() {
			return Lattice.BOTTOM_STRING;
		}
	};

	abstract Parity minus();

	abstract Parity add(Parity other);

	final Parity sub(Parity other) {
		return add(other.minus());
	}

	abstract Parity div(Parity other);

	abstract Parity mul(Parity other);

	abstract Parity mod(Parity other);

	@Override
	public abstract String toString();

	static Parity evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			int c = (int) constant.getValue();
			return c % 2 == 0 ? EVEN : ODD;
		}
		return TOP;
	}
}
