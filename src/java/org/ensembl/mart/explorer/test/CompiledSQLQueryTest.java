package org.ensembl.mart.explorer.test;

import junit.framework.*;
import org.ensembl.mart.explorer.*;
//import org.apache.log4j.*;
import java.io.*;
import java.net.*;

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
        suite.addTestSuite( CompiledSQLQueryTest.class );
        return suite;
    }

    /**
     * Convenience method for executing query and printing some results.
     */
	  private void executeQuery( Query query ) throws Exception {
        engine.execute( query, formatspec, stats);
	    System.out.println( query );
        System.out.println( stats );
      
        assertTrue( "No text returned from query", stats.getCharCount()>0 );
        assertTrue( "No lines returned from query", stats.getLineCount()>0 );
    } 

    public void testChrQuery() throws Exception {

	    query.addAttribute( new FieldAttribute("gene_stable_id") );
        query.addFilter( new BasicFilter( "chromosome_id", "=", "3") );
   
	    executeQuery( query );
    }


    public void testStableIDQuery() throws Exception {

      query.addAttribute( new FieldAttribute("gene_stable_id") );
      query.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000005175"}) );
      executeQuery( query );
    }


    /**
     * Test filtering on stable ids from a file.
     */
    public void testStableIDsFromFileQuery() throws Exception {
      
      query.addAttribute( new FieldAttribute("gene_stable_id") );

      query.addFilter( new IDListFilter("gene_stable_id", new File ( org.apache.log4j.helpers.Loader.getResource( STABLE_ID_REL ).getFile() ) ) );
      executeQuery( query );
    }


    public void testStableIDsFromURLQuery() throws Exception {

      // in practice this is effectively the same as testStableIDsFromFile because
      // the implementation converts the file to a url. We include this test incase future
      // implementations work differently.

      query.addAttribute( new FieldAttribute("gene_stable_id") );

      URL stableidurl = org.apache.log4j.helpers.Loader.getResource( STABLE_ID_REL );
      query.addFilter( new IDListFilter("gene_stable_id", stableidurl ) );
      executeQuery( query );
    }


  public void testJoinToPFAM() throws Exception {
    query.addAttribute( new FieldAttribute("gene_stable_id") );
    query.addAttribute( new FieldAttribute("pfam") );
    executeQuery( query );
  }


  public static void main(String[] args) {
    junit.textui.TestRunner.run( suite() );
  }
}
