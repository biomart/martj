package org.ensembl.mart.explorer.test;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.datamodel.AssemblyLocation;
import org.ensembl.datamodel.Exon;
import org.ensembl.datamodel.Gene;
import org.ensembl.datamodel.Transcript;
import org.ensembl.driver.ExonAdaptor;
import org.ensembl.driver.GeneAdaptor;
import org.ensembl.driver.SequenceAdaptor;
import org.ensembl.driver.TranscriptAdaptor;
import org.ensembl.mart.explorer.FormatSpec;
import org.ensembl.mart.explorer.IDListFilter;
import org.ensembl.mart.explorer.Query;
import org.ensembl.mart.explorer.SequenceDescription;

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
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTCODING));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
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
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTPEPTIDE));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
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
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTCDNA));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
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
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTEXONS));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
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
	
	public void testTranscriptExonIntronSequence() throws Exception {
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTEXONINTRON));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
		String results = out.toString();
		out.close();
		
		TranscriptAdaptor ta = ensjDriver.getTranscriptAdaptor();
		SequenceAdaptor sa = ensjDriver.getSequenceAdaptor();
		
		StringTokenizer sequences = new StringTokenizer(results, ">", false);
		
		while (sequences.hasMoreTokens()) {
			StringTokenizer lines = new StringTokenizer(sequences.nextToken(), "\n", false);
			String transcript_stable_id = new StringTokenizer(new StringTokenizer(lines.nextToken(), "|", false).nextToken(), ".", false).nextToken();
			
			String martseq = lines.nextToken();
			Transcript transcript = ta.fetch(transcript_stable_id);
			
			AssemblyLocation first_exon_loc = (AssemblyLocation) ((Exon) transcript.getExons().get(0)).getLocation();
			AssemblyLocation last_exon_loc = (AssemblyLocation) ((Exon) transcript.getExons().get(transcript.getExons().size() - 1)).getLocation();
			
			AssemblyLocation newloc = new AssemblyLocation(first_exon_loc.getChromosome(), Math.min(first_exon_loc.getStart(), last_exon_loc.getStart()), Math.max(first_exon_loc.getEnd(), last_exon_loc.getEnd()), first_exon_loc.getStrand() );
			
			String ensjseq = sa.fetch(newloc).getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}		
	}

	public void testTranscriptFlankSequence() throws Exception {
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		int rightflank = 1000;
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.TRANSCRIPTFLANKS, 0, rightflank));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
		String results = out.toString();
		out.close();
		
		TranscriptAdaptor ta = ensjDriver.getTranscriptAdaptor();
		SequenceAdaptor sa = ensjDriver.getSequenceAdaptor();
		
		StringTokenizer sequences = new StringTokenizer(results, ">", false);
		
		while (sequences.hasMoreTokens()) {
			StringTokenizer lines = new StringTokenizer(sequences.nextToken(), "\n", false);
			String transcript_stable_id = new StringTokenizer(new StringTokenizer(lines.nextToken(), "|", false).nextToken(), ".", false).nextToken();
			
			String martseq = lines.nextToken();
			Transcript transcript = ta.fetch(transcript_stable_id);
			
			AssemblyLocation last_exon_loc = (AssemblyLocation) ((Exon) transcript.getExons().get(transcript.getExons().size() - 1)).getLocation();
			
			AssemblyLocation newloc;
			
			if (last_exon_loc.getStrand() < 0)
			  newloc = new AssemblyLocation(last_exon_loc.getChromosome(), last_exon_loc.getStart() - rightflank, last_exon_loc.getStart() - 1, last_exon_loc.getStrand());
		  else
		    newloc = new AssemblyLocation(last_exon_loc.getChromosome(), last_exon_loc.getEnd() + 1, last_exon_loc.getEnd() + rightflank, last_exon_loc.getStrand());
			
			String ensjseq = sa.fetch(newloc).getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}		
	}
	
	public void testGeneExonIntronSequence() throws Exception {
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.GENEEXONINTRON));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
		String results = out.toString();
		out.close();
		
		GeneAdaptor ga = ensjDriver.getGeneAdaptor();
		SequenceAdaptor sa = ensjDriver.getSequenceAdaptor();
		
		StringTokenizer sequences = new StringTokenizer(results, ">", false);
		
		while (sequences.hasMoreTokens()) {
			StringTokenizer lines = new StringTokenizer(sequences.nextToken(), "\n", false);
			String gene_stable_id = new StringTokenizer(new StringTokenizer(lines.nextToken(), " ", false).nextToken(), ".", false).nextToken();
			
			String martseq = lines.nextToken();
			Gene gene = ga.fetch(gene_stable_id);
			
			AssemblyLocation first_exon_loc = (AssemblyLocation) ((Exon) gene.getExons().get(0)).getLocation();
			AssemblyLocation last_exon_loc = (AssemblyLocation) ((Exon) gene.getExons().get(gene.getExons().size() - 1)).getLocation();
			
			AssemblyLocation newloc = new AssemblyLocation(first_exon_loc.getChromosome(), Math.min(first_exon_loc.getStart(), last_exon_loc.getStart()), Math.max(first_exon_loc.getEnd(), last_exon_loc.getEnd()), first_exon_loc.getStrand() );
			
			String ensjseq = sa.fetch(newloc).getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}				
	}
	
	public void testGeneFlankSequence() throws Exception {
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		int leftflank = 1000;
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.GENEFLANKS, leftflank, 0));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
		String results = out.toString();
		out.close();
		
		GeneAdaptor ga = ensjDriver.getGeneAdaptor();
		SequenceAdaptor sa = ensjDriver.getSequenceAdaptor();
		
		StringTokenizer sequences = new StringTokenizer(results, ">", false);
		
		while (sequences.hasMoreTokens()) {
			StringTokenizer lines = new StringTokenizer(sequences.nextToken(), "\n", false);
			String gene_stable_id = new StringTokenizer(new StringTokenizer(lines.nextToken(), " ", false).nextToken(), ".", false).nextToken();
			
			String martseq = lines.nextToken();
			Gene gene = ga.fetch(gene_stable_id);
			
			AssemblyLocation first_exon_loc = (AssemblyLocation) ((Exon) gene.getExons().get(0)).getLocation();
			
			AssemblyLocation newloc;
			
			if (first_exon_loc.getStrand() < 0)
			  newloc = new AssemblyLocation(first_exon_loc.getChromosome(), first_exon_loc.getEnd() + 1, first_exon_loc.getEnd() + leftflank, first_exon_loc.getStrand() );
			else
			  newloc = new AssemblyLocation(first_exon_loc.getChromosome(), first_exon_loc.getStart() - leftflank, first_exon_loc.getStart() - 1, first_exon_loc.getStrand() );
			
			String ensjseq = sa.fetch(newloc).getString();
			
			assertEquals("WARNING: Mart Sequence Doesnt match ENSJ Sequence\n", ensjseq, martseq);
		}				
	}
	
	public void testGeneExonSequence() throws Exception {
		Query q = new Query(genequery);
		
		//test one forward strand gene and one revearse strand gene
		q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000161929", "ENSG00000111960"}) );
		q.setSequenceDescription(new SequenceDescription(SequenceDescription.GENEEXONS));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		engine.execute(q, new FormatSpec(FormatSpec.FASTA), out);
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
