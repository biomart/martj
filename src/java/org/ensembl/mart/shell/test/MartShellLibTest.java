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

import org.ensembl.mart.lib.test.Base;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.test.StatOutputStream;
import org.ensembl.mart.lib.config.MartConfiguration;

import org.ensembl.mart.shell.MartShellLib;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartShellLibTest extends Base {

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

  public void testMartShellLib() throws Exception {
  	MartConfiguration martconf = engine.getMartConfiguration();
  	
  	MartShellLib msl = new MartShellLib(engine, martconf);
  	
  	String martSQL = "select ensembl_gene_id from homo_sapiens_ensembl_genes limit 100";
  	
		StatOutputStream stats = new StatOutputStream();

    msl.setOutputStream(stats);
    
    msl.parseQuery(martSQL);
    
		assertTrue("No text returned from query", stats.getCharCount() > 0);
		assertTrue("No lines returned from query", stats.getLineCount() > 0);
		stats.close();		
  }  
}
