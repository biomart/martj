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

package org.ensembl.mart.explorer.test;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.explorer.config.MartDTDEntityResolver;
import org.ensembl.mart.explorer.config.MartXMLutils;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Tests for the Mart Configuration System.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class ConfigurationTest extends Base {

	public static void main(String[] args) {
		if (args.length > 0)
			TestRunner.run(TestClass(args[0]));
		else
			TestRunner.run(suite());
	}

	public static Test suite() {
		return new TestSuite(ConfigurationTest.class);
	}

	public static Test TestClass(String testclass) {
		TestSuite suite = new TestSuite();
		suite.addTest(new ConfigurationTest(testclass));
		return suite;
	}

	public ConfigurationTest(String name) {
		super(name);
	}
	
	/**
	 * Tests the ability to retrieve an XML configuration file from the Mart Database,
	 * parse it with JDOM using validation (implicitly tests getting the DTD from the Mart Database as well),
	 * store it back into the Database, retrieve it again, and compare to the original.
	 * 
	 * @throws Exception
	 */
	public void testXMLRoundTrip() throws Exception {
		String xmlTestID = "http://www.ensembl.org/EnsMart/test.xml";
		
		Connection conn = getDBConnection();
		
		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(true); // validate against the DTD
		builder.setEntityResolver(new MartDTDEntityResolver(conn)); // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.
		
		Document doc = builder.build(MartXMLutils.getInputSourceFor(conn, xmlTestID));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLOutputter xout = new XMLOutputter();
		xout.output(doc, out);
			
		String initxml = out.toString();
		out.close();
		
		MartXMLutils.storeConfiguration(conn, xmlTestID, doc);
		
		SAXBuilder newbuilder = new SAXBuilder();
		newbuilder.setValidation(true);
		newbuilder.setEntityResolver(new MartDTDEntityResolver(conn)); // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.
		
		Document newdoc = newbuilder.build(MartXMLutils.getInputSourceFor(conn, xmlTestID));
		
		out = new ByteArrayOutputStream();
		xout.output(newdoc, out);
			
		String newxml = out.toString();
		out.close();
		
		assertEquals("Warning, initial xml does not match final xml after a roundtrip.", initxml, newxml);
	}
}
