package org.ensembl.mart.explorer.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.explorer.*;

/**
 * Tests that Mart Explorer Sequence retrieval works by comparing it's output to that of ensj.
 * 
 * @author craig
 *
 */
public class AttributeTest extends Base {
	
	 public static void main(String[] args){
		if (args.length > 0)
		    TestRunner.run( TestClass(args[0]) );
		else
		    TestRunner.run( suite() );
	}

	public static Test suite() {
		return new TestSuite( AttributeTest.class );
	}

    public static Test TestClass(String testclass) {
    	TestSuite suite = new TestSuite();
		suite.addTest(new AttributeTest(testclass));
		return suite;
    }
    
	public AttributeTest(String name) {
		super(name);
	}


	public void testSimpleQueries() throws Exception {
		Query q = new Query();
		q.setStarBases( new String[] { "hsapiens_ensemblgene", "hsapiens_ensembltranscript"} );
		q.setPrimaryKeys( new String[] {"gene_id", "transcript_id"});
		q.addAttribute( new FieldAttribute("gene_stable_id"));
		q.addFilter( new BasicFilter("chr_name", "=", "22") );
		engine.execute( q, new FormatSpec( FormatSpec.TABULATED ), System.out);	
	}
	
	
	public void testSimpleSNPQueries() throws Exception {
			Query q = new Query();
			q.setStarBases( new String[] { "hsapiens_snp"} );
			q.setPrimaryKeys( new String[] {"snp_id"});
			q.addAttribute( new FieldAttribute("external_id"));
			q.addAttribute( new FieldAttribute("allele"));
			q.addFilter( new BasicFilter("chr_name", "=", "21") );
			engine.execute( q, new FormatSpec( FormatSpec.TABULATED ), System.out);	
		}
}
