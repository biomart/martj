/*
    Copyright (C) 2003 EBI, GRL

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
 
package org.ensembl.mart.shell.test;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.lib.test.Base;
import org.ensembl.mart.lib.test.StatOutputStream;
import org.ensembl.mart.shell.MartShellLib;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartShellLibTest extends Base {

  private final String HSAPDSETFILE = "data/XML/homo_sapiens__ensembl_genes.xml";
  
	public static void main(String[] args) {
		if (args.length > 0)
			TestRunner.run(TestClass(args[0]));
		else
			TestRunner.run(suite());
	}

	public static Test suite() {
		return new TestSuite(MartShellLibTest.class);
	}

	public static Test TestClass(String testclass) {
		TestSuite suite = new TestSuite();
		suite.addTest(new MartShellLibTest(testclass));
		return suite;
	}

	public MartShellLibTest(String name) {
		super(name);
	}

  public void testMQLtoQuery() throws Exception {
    URL hsapdsetXMLURL = MartShellLibTest.class.getClassLoader().getResource(HSAPDSETFILE);
    assertNotNull("Missing dataset file: " + HSAPDSETFILE + "\n", hsapdsetXMLURL);
    
    URLDSViewAdaptor adaptor = new URLDSViewAdaptor(hsapdsetXMLURL);
  	
  	MartShellLib msl = new MartShellLib(adaptor);
  	
  	//String martSQL = "using ensembl_genes_homo_sapiens get ensembl_gene_id limit 100";
    String martSQL = "using ensembl_genes_homo_sapiens get sequence peptide where chromosome_name=1";
		StatOutputStream stats = new StatOutputStream();
    Query query = msl.MQLtoQuery(martSQL);
    query.setDataSource(martJDataSource);
    
    engine.execute(query, FormatSpec.TABSEPARATEDFORMAT, stats);
    
    int charCount = stats.getCharCount();
    int lineCount = stats.getLineCount();
		assertTrue("No text returned from query\n", charCount > 0);
		assertTrue("No lines returned from query\n", lineCount > 0);
		assertEquals("Wrong number of genes returned from Query\n", 100, lineCount);
		
		stats.close();		
  }  
}
