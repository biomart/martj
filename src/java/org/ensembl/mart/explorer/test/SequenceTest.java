package org.ensembl.mart.explorer.test;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.datamodel.AssemblyLocation;
import org.ensembl.datamodel.Gene;
import org.ensembl.datamodel.Transcript;
import org.ensembl.driver.GeneAdaptor;

/**
 * Tests that Mart Explorer Sequence retrieval works by comparing it's output to that of ensj.
 * 
 * @author craig
 *
 */
public class SequenceTest extends Base {
	
	public static void main(String[] args){
		TestRunner.run( suite() );
	}

	public static Test suite() {
		return new TestSuite( SequenceTest.class );
	}

	public SequenceTest(String name) {
		super(name);
	}


	public void testCodingSequence() throws Exception {
		GeneAdaptor ga = ensjDriver.getGeneAdaptor();
		System.out.println("Fetching genes... ");
		List genes = ga.fetch( AssemblyLocation.valueOf("12:14m-15m") );
		System.out.println("Found " +genes.size()+ " genes.");
		for (int g=0; g<genes.size(); ++g) {
			Gene gene = (Gene)genes.get(g);
			List transcripts = gene.getTranscripts();
			for (int t=0; t<transcripts.size(); ++t) {
				Transcript transcript = (Transcript)transcripts.get(t);
				String codingSeq = transcript.getTranslation().getSequence().getString();
				System.out.println( "Transcript: "  + transcript.getAccessionID() 
									+ " " + codingSeq);
			}
		}

	}
	
}
