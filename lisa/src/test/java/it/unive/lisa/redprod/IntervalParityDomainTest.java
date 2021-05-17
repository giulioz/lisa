package it.unive.lisa.redprod;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.LiSAFactory;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.IntervalParityDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.IntervalParityDomain2;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import org.junit.Test;


public class IntervalParityDomainTest {
	@Test
	public void testSign() throws ParsingException, AnalysisException {
		LiSAConfiguration configuration = new LiSAConfiguration();
		configuration.setDumpAnalysis(true);
		configuration.setWorkdir("test-outputs/red-prod");
//		configuration.setAbstractState(LiSAFactory.getDefaultFor(AbstractState.class,
//						LiSAFactory.getDefaultFor(HeapDomain.class), new IntervalParityDomain()));
		configuration.setAbstractState(LiSAFactory.getDefaultFor(AbstractState.class,
						LiSAFactory.getDefaultFor(HeapDomain.class), new IntervalParityDomain2()));
		LiSA lisa = new LiSA(configuration);
		lisa.run(IMPFrontend.processFile("redprod.imp"));
	}
}
