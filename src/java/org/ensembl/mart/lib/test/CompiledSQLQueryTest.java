package org.ensembl.mart.lib.test;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.util.PropertiesUtil;

/** JUnit TestSuite. 
 * @testfamily JUnit
 * @testkind testsuite
 * @testsetup Default TestSuite
 * @testpackage org.ensembl.mart.explorer.test*/
public class CompiledSQLQueryTest extends Base {

  private Logger logger =
		Logger.getLogger(CompiledSQLQueryTest.class.getName());

	public final String STABLE_ID_REL = "data/gene_stable_id.test";
	private StatOutputStream stats = new StatOutputStream();
	private FormatSpec formatspec = new FormatSpec(FormatSpec.TABULATED, "\t");

	public CompiledSQLQueryTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(CompiledSQLQueryTest.class);
		return suite;
	}

	public static Test TestClass(String testclass) {
		TestSuite suite = new TestSuite();
		suite.addTest(new CompiledSQLQueryTest(testclass));
		return suite;
	}

	/**
	 * Convenience method for executing query and printing some results.
	 */
	private void executeQuery(Query query) throws Exception {
		engine.execute(query, formatspec, stats);
//		System.out.println(query);
//		System.out.println(stats);

		assertTrue("No text returned from query", stats.getCharCount() > 0);
		assertTrue("No lines returned from query", stats.getLineCount() > 0);
	}

	public void testQueryCopy() throws Exception {
		Query q = new Query(genequery);
		assertTrue("Query Copy Constructor creating a equal copy\n", genequery.equals(q));
	}

	public void testChrQuery() throws Exception {
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(new BasicFilter("chromosome_id", "=", "3"));

		executeQuery(q);
	}

	public void testStableIDQuery() throws Exception {
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(new IDListFilter("gene_stable_id", new String[] { "ENSG00000005175" }));
		executeQuery(q);
	}

	/**
	 * Test filtering on stable ids from a file.
	 */
	public void testStableIDsFromFileQuery() throws Exception {
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(new IDListFilter("gene_stable_id", new File(PropertiesUtil.class.getClassLoader().getResource(STABLE_ID_REL).getFile())));
		executeQuery(q);
	}

	public void testStableIDsFromURLQuery() throws Exception {

		// in practice this is effectively the same as testStableIDsFromFile because
		// the implementation converts the file to a url. We include this test incase future
		// implementations work differently.
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));

		URL stableidurl = CompiledSQLQueryTest.class.getClassLoader().getResource(STABLE_ID_REL);
		q.addFilter(new IDListFilter("gene_stable_id", stableidurl));
		executeQuery(q);
	}

	public void testJoinToPFAM() throws Exception {
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addAttribute(new FieldAttribute("pfam"));
		executeQuery(q);
	}

	public void testUnprocessedFilterHandlers() throws Exception {
		Filter chrFilter = new BasicFilter("chr_name", "gene_main",  "gene_id_key", "=", "1");

		//Marker
		Query q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(chrFilter);

		Filter start = new BasicFilter("marker_start", null, "=", "AFMA272XC9", "org.ensembl.mart.lib.MarkerFilterHandler");
	  Filter end = new BasicFilter("marker_end", null, "=", "RH10794", "org.ensembl.mart.lib.MarkerFilterHandler");
		
		q.addFilter(start);
		q.addFilter(end);

		executeQuery(q);

		//Band
		q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(chrFilter);
		
		start = new BasicFilter("band_start", null, "=", "p36.33", "org.ensembl.mart.lib.BandFilterHandler");
		end = new BasicFilter("band_end", null, "=", "p36.33", "org.ensembl.mart.lib.BandFilterHandler");

		q.addFilter(start);
		q.addFilter(end);
		executeQuery(q);

		//Encode
		q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));

		Filter test = new BasicFilter("encode", null, "=", "13:29450016:29950015", "org.ensembl.mart.lib.EncodeQtlFilterHandler");

		q.addFilter(test);
		executeQuery(q);

		//Qtl
		q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));

		test = new BasicFilter("qtl", null, "=", "4:82189556:83189556", "org.ensembl.mart.lib.EncodeQtlFilterHandler");

		q.addFilter(test);
		executeQuery(q);

		//Expression
		q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));

		Filter anatomical_filter = new BasicFilter("est.anatomical_site", null, "=", "ovary", "org.ensembl.mart.lib.ExpressionFilterHandler");
		Filter development_filter = new BasicFilter("est.development_stage", null, "=", "adult", "org.ensembl.mart.lib.ExpressionFilterHandler");
		
		q.addFilter(anatomical_filter);
		q.addFilter(development_filter);
		executeQuery(q);
		
		//TODO:GO
		/*
    mol Function          GO:0003673
    biol proc             GO:0008150
    cell comp             GO:0005575
    go_evidence_code:IEA  excluded
  
    5345 entries
     
    ======  
    mol Function          chaperone activity
    biol proc             development
    cell comp             cell
    go_evidence_code:IEA  excluded

  3 entries
		 */
	}

	public static void main(String[] args) {
		if (args.length > 0)
			junit.textui.TestRunner.run(TestClass(args[0]));
		else
			junit.textui.TestRunner.run(suite());
	}
}
