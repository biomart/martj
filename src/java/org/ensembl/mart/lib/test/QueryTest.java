package org.ensembl.mart.lib.test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.*;

/**
 * Tests that Mart Explorer Sequence retrieval works by comparing it's output to that of ensj.
 * 
 * @author craig
 *
 */
public class QueryTest extends TestCase implements PropertyChangeListener{

	public static void main(String[] args) {
			TestRunner.run(suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		//suite.addTest(new QueryTest("testKakaQuery"));
		suite.addTestSuite( QueryTest.class );
		return suite;
	}

	public QueryTest(String name) {
		super(name);
	}

	public void testQuery()  throws Exception {
		Query q = new Query();
    q.addPropertyChangeListener( this );
		
    int expected = 0;
    String[] pKeys = new String[] { "gene_id", "transcript_id" };
		q.setPrimaryKeys( pKeys );
    expected++;
    assertEquals(expected, nChanges);
    q.setPrimaryKeys( null );
    expected++;
    assertEquals(expected, nChanges);
    
    FieldAttribute fa = new FieldAttribute("chr_name"); 
		q.addAttribute( fa );
    expected++;
    assertEquals(expected, nChanges);
    q.removeAttribute( fa );
    expected++;
    assertEquals(expected, nChanges);
    
    Filter f = new BasicFilter("chr_name", "=", "22");    
    q.addFilter( f );
    expected++;
    assertEquals(expected, nChanges);
    q.removeFilter( f );
    expected++;
    assertEquals(expected, nChanges);
    
    
    q.setLimit(100);
		expected++;
    assertEquals(expected, nChanges);
    
   }

  /* Called when a bound attribute on query changes. It updates the number
   * of events counted so far.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    // TODO Auto-generated method stub
    System.out.println( evt.getPropertyName() + "\t"+ evt.getNewValue()  + "\t" + evt.getOldValue());
    nChanges++;
  }
  
  private int nChanges = 0;
}
