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
package org.ensembl.mart.util.test;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.test.Base;
import org.ensembl.mart.util.BigPreferences;

/** 
* @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
* @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
*/
public class BigPreferencesTest extends Base {

  private Logger logger = Logger.getLogger(BigPreferencesTest.class.getName());
  private final String TEST_NODE = "test";
  private Preferences userPref = BigPreferences.userNodeForPackage(BigPreferences.class);
  private Preferences sysPref = BigPreferences.systemNodeForPackage(BigPreferences.class);
  private Random rand = new Random();
  
  private final int MAX_BYTE_LENGTH = (3 * BigPreferences.MAX_VALUE_LENGTH ) / 4;
  private final String TEST_KEY = "testBytes";
  
  /**
   * @param name
   */
  public BigPreferencesTest(String name) {
    super(name);
  }

  public static void main(String[] args) {
    if (args.length > 0)
      TestRunner.run(TestClass(args[0]));
    else
      TestRunner.run(suite());
  }

  public static Test suite() {
    return new TestSuite(BigPreferencesTest.class);
  }

  public static Test TestClass(String testclass) {
    TestSuite suite = new TestSuite();
    suite.addTest(new BigPreferencesTest(testclass));
    return suite;
  }

  public void testUserBigPreferences() throws Exception {
    smallByteTest(userPref);
    evenDivisionBigByteTest(userPref);
    oddDivisionBigByteTest(userPref);  
  }

  public void testSystemPreferences() throws Exception {
    smallByteTest(sysPref);
    evenDivisionBigByteTest(sysPref);
    oddDivisionBigByteTest(sysPref);
  }
  
  private void smallByteTest(Preferences pref) throws Exception {
    byte[] refBytes = new byte[MAX_BYTE_LENGTH - 37];
    rand.nextBytes(refBytes);
    byteTest(pref, refBytes);
  }
  
  private void evenDivisionBigByteTest(Preferences pref) throws Exception {
    byte[] refBytes = new byte[MAX_BYTE_LENGTH * 5]; //5 equal divisions
    rand.nextBytes(refBytes);
    byteTest(pref, refBytes);
  }
  
  private void oddDivisionBigByteTest(Preferences pref) throws Exception {
    byte[] refBytes = new byte[( MAX_BYTE_LENGTH * 5 ) - 37]; //4 equal divisions, and one with 37 fewer entries
    rand.nextBytes(refBytes);
    byteTest(pref, refBytes);
  }

  private void byteTest(Preferences pref, byte[] refBytes) throws Exception {
    pref.putByteArray(TEST_KEY, refBytes);
    
    byte[] nBytes = pref.getByteArray(TEST_KEY, null);
    
    assertEquals("Byte Lengths differ between refBytes and nBytes\n", refBytes.length, nBytes.length );
    
    for (int i = 0, n = refBytes.length; i < n; i++) {
      assertEquals("Byte " + i + " differs between refBytes and nBytes\n", refBytes[i], nBytes[i]);
    }
    
    pref.remove(TEST_KEY);
    pref.flush();
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    clearPrefs();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    clearPrefs();
  }

  private void clearPrefs() {
    //empty test nodes
    try {
      userPref.node(TEST_NODE).removeNode();
      userPref.flush();

      sysPref.node(TEST_NODE).removeNode();
      sysPref.flush();
      userPref.node(TEST_NODE).removeNode();
      userPref.flush();
      sysPref.node(TEST_NODE).removeNode();
      sysPref.flush();

    } catch (BackingStoreException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("BackingStoreException prevented clearing the preferences." + e.getMessage() + "\n");
      e.printStackTrace();
    }
  }
}
