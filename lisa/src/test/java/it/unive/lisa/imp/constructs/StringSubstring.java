package it.unive.lisa.imp.constructs;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.imp.types.BoolType;
import it.unive.lisa.imp.types.IntType;
import it.unive.lisa.imp.types.StringType;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.TernaryOperator;

/**
 * The native construct representing the substring operation. This construct can
 * be invoked on a string variable {@code x} with
 * {@code x.substring(start, end)}, where {@code start} is an integer
 * representing the index where the substring starts (inclusive) and {@code end}
 * is an integer representing the index where the substring ends (exclusive).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringSubstring extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringSubstring(CodeLocation location, CompilationUnit stringUnit) {
		super(new CFGDescriptor(location, stringUnit, true, "substring", BoolType.INSTANCE,
				new Parameter(location, "this", StringType.INSTANCE),
				new Parameter(location, "start", IntType.INSTANCE),
				new Parameter(location, "end", IntType.INSTANCE)),
				IMPStringSubstring.class);
	}

	/**
	 * An expression modeling the string substring operation. The type of the
	 * first operand must be {@link StringType}, while the other two operands'
	 * types must be {@link IntType}. The type of this expression is the
	 * {@link StringType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringSubstring extends TernaryNativeCall implements PluggableStatement {

		private Statement original;

		@Override
		public void setOriginatingStatement(Statement st) {
			original = st;
		}

		/**
		 * Builds the substring.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param left       the left-hand side of this operation
		 * @param middle     the middle operand of this operation
		 * @param right      the right-hand side of this operation
		 */
		public IMPStringSubstring(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression middle, Expression right) {
			super(cfg, new SourceCodeLocation(sourceFile, line, col), "substring", StringType.INSTANCE, left, middle,
					right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> ternarySemantics(AnalysisState<A, H, V> entryState,
						InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> leftState,
						SymbolicExpression leftExp,
						AnalysisState<A, H, V> middleState, SymbolicExpression middleExp,
						AnalysisState<A, H, V> rightState, SymbolicExpression rightExp) throws SemanticException {
			// we allow untyped for the type inference phase
			if (!leftExp.getDynamicType().isStringType() && !leftExp.getDynamicType().isUntyped())
				return entryState.bottom();
			if (!middleExp.getDynamicType().isNumericType() && !middleExp.getDynamicType().isUntyped())
				return entryState.bottom();
			if (!rightExp.getDynamicType().isNumericType() && !rightExp.getDynamicType().isUntyped())
				return entryState.bottom();

			return rightState.smallStepSemantics(new TernaryExpression(getRuntimeTypes(), leftExp, middleExp, rightExp,
					TernaryOperator.STRING_SUBSTRING), original);
		}
	}
}
