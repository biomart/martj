package org.ensembl.mart.explorer.test;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.datamodel.Transcript;
import org.ensembl.driver.TranscriptAdaptor;
import org.ensembl.mart.explorer.*;

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
		//test one forward strand gene and one revearse strand gene
		query.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		query.addSequenceDescription(new SequenceDescription("coding"));
		
		executeQuery(query);
	}
	
	public void executeQuery(Query query) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(query, new FormatSpec(FormatSpec.FASTA), out);
		String results = out.toString();
		out.close();
		
		TranscriptAdaptor ta = ensjDriver.getTranscriptAdaptor();
		StringTokenizer sequences = new StringTokenizer(results, ">", false);
		
		while (sequences.hasMoreTokens()) {
			StringTokenizer lines = new StringTokenizer(sequences.nextToken(), "\n", false);
			String transcript_stable_id = new StringTokenizer(new StringTokenizer(lines.nextToken(), "|", false).nextToken(), ".", false).nextToken();
			
			String martseq = lines.nextToken();
			Transcript transcript = ta.fetch(transcript_stable_id);
			
			String ensjseq = transcript.getTranslation().getSequence().getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", martseq,ensjseq);
		}
		    
		    
		    
			
		
	}
}
