package org.ensembl.mart.lib.test;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.MapFilter;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.Query;

/** JUnit TestSuite. 
 * @testfamily JUnit
 * @testkind testsuite
 * @testsetup Default TestSuite
 * @testpackage org.ensembl.mart.explorer.test*/
public class CompiledSQLQueryTest extends Base {

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
		System.out.println(query);
		System.out.println(stats);

		assertTrue("No text returned from query", stats.getCharCount() > 0);
		assertTrue("No lines returned from query", stats.getLineCount() > 0);
	}

	public void testQueryCopy() throws Exception {
		Query q = new Query(genequery);
		assertTrue("Query Copy Constructor not creating a equal copy\n", genequery.equals(q));
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
		q.addFilter(new IDListFilter("gene_stable_id", new File(org.apache.log4j.helpers.Loader.getResource(STABLE_ID_REL).getFile())));
		executeQuery(q);
	}

	public void testStableIDsFromURLQuery() throws Exception {

		// in practice this is effectively the same as testStableIDsFromFile because
		// the implementation converts the file to a url. We include this test incase future
		// implementations work differently.
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));

		URL stableidurl = org.apache.log4j.helpers.Loader.getResource(STABLE_ID_REL);
		q.addFilter(new IDListFilter("gene_stable_id", stableidurl));
		executeQuery(q);
	}

	public void testJoinToPFAM() throws Exception {
		Query q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addAttribute(new FieldAttribute("pfam"));
		executeQuery(q);
	}

	public void testDSFilterHandlers() throws Exception {
		Filter chrFilter = new BasicFilter("chr_name", "gene_main", "=", "1");

		//Marker
		Query q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(chrFilter);

		MapFilter start = new MapFilter("Marker", "AFMA272XC9:start");
		MapFilter end = new MapFilter("Marker", "RH10794:end");
		q.addDomainSpecificFilter(start);
		q.addDomainSpecificFilter(end);

		executeQuery(q);

		//Band
		q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));
		q.addFilter(chrFilter);
		start = new MapFilter("Band", "p36.33:start");
		end = new MapFilter("Band", "p36.33:end");

		q.addDomainSpecificFilter(start);
		q.addDomainSpecificFilter(end);
		executeQuery(q);

		//Encode
		q = new Query(genequery);

		q.addAttribute(new FieldAttribute("gene_stable_id"));

		MapFilter test = new MapFilter("Encode", "13:29450016:29950015");

		q.addDomainSpecificFilter(test);
		executeQuery(q);

		//Qtl
		q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));

		test = new MapFilter("Qtl", "4:82189556:83189556");

		q.addDomainSpecificFilter(test);
		executeQuery(q);

		//Expression
		q = new Query(genequery);
		q.addAttribute(new FieldAttribute("gene_stable_id"));

		test = new MapFilter("Expression", "est:anatomical_site=ovary,development_stage=adult");

		q.addDomainSpecificFilter(test);
		executeQuery(q);
	}

	public static void main(String[] args) {
		if (args.length > 0)
			junit.textui.TestRunner.run(TestClass(args[0]));
		else
			junit.textui.TestRunner.run(suite());
	}
}
