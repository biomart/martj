package org.ensembl.mart.explorer.test;

import junit.framework.*;

/** JUnit TestSuite. 
 * @testfamily JUnit
 * @testkind testsuite
 * @testsetup Default TestSuite
 * @testpackage org.ensembl.mart.explorer.test*/
public class CompiledSQLQueryTest extends TestCase {
    public CompiledSQLQueryTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run( suite() );
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite( CompiledSQLQueryTest.class );
        return suite;
    }

    public void test_db_chr_query() {

    }
}
