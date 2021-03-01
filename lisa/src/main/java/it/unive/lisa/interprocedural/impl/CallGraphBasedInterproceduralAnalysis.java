package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.*;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueIdentifier;

abstract public class CallGraphBasedInterproceduralAnalysis<A extends AbstractState<A, H, V>,
        H extends HeapDomain<H>,
        V extends ValueDomain<V>> implements InterproceduralAnalysis<A, H, V> {

    /**
     * The call graph used to resolve method calls
     */
    private CallGraph callgraph;

    protected Program program;


    @Override
    public final void build(Program program, CallGraph callgraph) throws InterproceduralAnalysisException {
        this.callgraph = callgraph;
        this.program = program;
    }

    @Override
    public final Call resolve(UnresolvedCall unresolvedCall) throws CallResolutionException {
        return callgraph.resolve(unresolvedCall);
    }

    protected final AnalysisState<A, H, V> prepareEntryStateOfEntryPoint(AnalysisState<A, H, V> entryState, CFG cfg)
            throws SemanticException {
        AnalysisState<A, H, V> prepared = entryState;
        for (Parameter arg : cfg.getDescriptor().getArgs())
            if (arg.getStaticType().isPointerType()) {
                prepared = prepared.smallStepSemantics(
                        new HeapReference(Caches.types().mkSingletonSet(arg.getStaticType()), arg.getName()),
                        cfg.getGenericProgramPoint());
                for (SymbolicExpression expr : prepared.getComputedExpressions())
                    prepared = prepared.assign((HeapIdentifier) expr,
                            new PushAny(Caches.types().mkSingletonSet(arg.getStaticType())),
                            cfg.getGenericProgramPoint());
            } else {
                ValueIdentifier id = new ValueIdentifier(Caches.types().mkSingletonSet(arg.getStaticType()),
                        arg.getName());
                prepared = prepared.assign(id, new PushAny(Caches.types().mkSingletonSet(arg.getStaticType())),
                        cfg.getGenericProgramPoint());
            }
        return prepared;
    }
}