package org.ensembl.mart.explorer.test;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.datamodel.Exon;
import org.ensembl.datamodel.Transcript;
import org.ensembl.driver.TranscriptAdaptor;
import org.ensembl.driver.ExonAdaptor;
import org.ensembl.mart.explorer.*;

/**
 * Tests that Mart Explorer Sequence retrieval works by comparing it's output to that of ensj.
 * 
 * @author craig
 *
 */
public class SequenceTest extends Base {
	
	public static void main(String[] args){
		if (args.length > 0)
		    TestRunner.run( TestClass(args[0]) );
		else
		    TestRunner.run( suite() );
	}

	public static Test suite() {
		return new TestSuite( SequenceTest.class );
	}

    public static Test TestClass(String testclass) {
    	TestSuite suite = new TestSuite();
		suite.addTest(new SequenceTest(testclass));
		return suite;
    }
    
	public SequenceTest(String name) {
		super(name);
	}


	public void testCodingSequence() throws Exception {
		//test one forward strand gene and one revearse strand gene
		query.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		query.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTCODING));
		
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
	
	public void testPeptideSequence() throws Exception {
		//test one forward strand gene and one revearse strand gene
		query.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		query.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTPEPTIDE));
		
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
			
			String ensjseq = transcript.getTranslation().getPeptide();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}		
	}
	
	public void testCdnaSequence() throws Exception {
		//test one forward strand gene and one revearse strand gene
		query.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		query.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTCDNA));
		
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
			
			String ensjseq = transcript.getSequence().getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}
	}
	
	public void testTranscriptExonSequence() throws Exception {
		//test one forward strand gene and one revearse strand gene
		query.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		query.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTEXONS));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(query, new FormatSpec(FormatSpec.FASTA), out);
		String results = out.toString();
		out.close();
		
		ExonAdaptor ea = ensjDriver.getExonAdaptor();
		StringTokenizer sequences = new StringTokenizer(results, ">", false);
		
		while (sequences.hasMoreTokens()) {
			StringTokenizer lines = new StringTokenizer(sequences.nextToken(), "\n", false);
			String exon_stable_id = new StringTokenizer(new StringTokenizer(lines.nextToken(), "|", false).nextToken(), ".", false).nextToken();
			
			String martseq = lines.nextToken();
			Exon exon = ea.fetch(exon_stable_id);
			
			String ensjseq = exon.getSequence().getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}
	}
}
