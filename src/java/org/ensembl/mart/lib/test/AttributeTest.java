package org.ensembl.mart.lib.test;

import java.io.ByteArrayOutputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.*;

/**
 * Tests that Mart Explorer Sequence retrieval works by comparing it's output to that of ensj.
 * 
 * @author craig
 *
 */
public class AttributeTest extends Base {

	public static void main(String[] args) {
		if (args.length > 0)
			TestRunner.run(TestClass(args[0]));
		else
			TestRunner.run(suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new AttributeTest("testKakaQuery"));
		//suite.addTestSuite( AttributeTest.class );
		return suite;
	}

	public static Test TestClass(String testclass) {
		TestSuite suite = new TestSuite();
		suite.addTest(new AttributeTest(testclass));
		return suite;
	}

	public AttributeTest(String name) {
		super(name);
	}

	public void testKakaQuery()  throws Exception {
		Query q = new Query(genequery);
		
		q.setPrimaryKeys(new String[] { "gene_id", "transcript_id" });
		q.addAttribute(new FieldAttribute("chr_name"));
		q.addAttribute(new FieldAttribute("gene_chrom_start"));
		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(new BasicFilter("chr_name", "=", "22"));
		q.addFilter(new BasicFilter("gene_chrom_start", "<", "15000000"));
    

		StatOutputStream stats = new StatOutputStream();
		engine.execute(q, new FormatSpec(FormatSpec.TABULATED), stats);

		assertTrue("No text returned from query", stats.getCharCount() > 0);
		assertTrue("No lines returned from query", stats.getLineCount() > 0);
		stats.close();
	}

	public void testSimpleQueries() throws Exception {
		Query q = new Query(genequery);
		
		q.setPrimaryKeys(new String[] { "gene_id", "transcript_id" });
		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(new BasicFilter("chr_name", "=", "22"));
		StatOutputStream stats = new StatOutputStream();
		engine.execute(q, new FormatSpec(FormatSpec.TABULATED), stats);

		assertTrue("No text returned from query", stats.getCharCount() > 0);
		assertTrue("No lines returned from query", stats.getLineCount() > 0);
		stats.close();
	}

	public void testSimpleSNPQueries() throws Exception {
		Query q = new Query(snpquery);

		q.addAttribute(new FieldAttribute("external_id"));
		q.addAttribute(new FieldAttribute("allele"));
		q.addFilter(new BasicFilter("chr_name", "=", "21"));
		StatOutputStream stats = new StatOutputStream();
		engine.execute(q, new FormatSpec(FormatSpec.TABULATED), stats);
		assertTrue("No text returned from query", stats.getCharCount() > 0);
		assertTrue("No lines returned from query", stats.getLineCount() > 0);
		stats.close();
	}

	public void testDisambiguationQueries() throws Exception {
		String geneID = "ENSG00000079974";
		String expectedDiseaseID = "RB2B_HUMAN";
		Query q = new Query(genequery);
		
		q.addAttribute(new FieldAttribute("display_id", "xref_SWISSPROT"));
		q.addFilter(new BasicFilter("gene_stable_id", "=", geneID));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		engine.execute(q, new FormatSpec(FormatSpec.TABULATED), out);
		out.close();
		String actualDiseaseID = out.toString().trim();
		assertEquals(
			"Got wrong disease ID for gene " + geneID,
			expectedDiseaseID,
			actualDiseaseID);
	}
}
