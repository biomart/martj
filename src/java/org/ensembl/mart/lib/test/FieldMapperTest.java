package org.ensembl.mart.lib.test;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Field;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.FieldMapper;
import org.ensembl.mart.lib.Table;

import junit.framework.TestCase;

/**
 * @author craig
 *

 */
public class FieldMapperTest extends TestCase {

	/**
	 * Constructor for FieldMapperTest.
	 * @param arg0
	 */
	public FieldMapperTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(FieldMapperTest.class);
	}

	

	public void testQualifiedName() {
		assertEquals("t1.c1", 
							mapper.qualifiedName(new FieldAttribute("c1")));
		assertEquals("t1.c1", 
									mapper.qualifiedName(new BasicFilter("c1",">","1")));			
		assertEquals("t2.c2", 
									mapper.qualifiedName(new FieldAttribute("c2")));
		assertTrue( mapper.canMap(new FieldAttribute("c1")));
		assertTrue( mapper.canMap(new FieldAttribute("t1.c1")));	
		assertTrue( mapper.canMap(new FieldAttribute("c9"))==false );	
		
		assertEquals( "t2", mapper.tableName(new FieldAttribute("c2")) );				

		// check we can disambiguate
		assertEquals( "t2", mapper.tableName(new FieldAttribute("common", "2")));
		assertEquals( "t1", mapper.tableName(new FieldAttribute("common", "1")));
	
	}
	
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		Table[] tables 
			= new Table[]{
				new Table("t1", new String[]{"pk", "c1", "common"}, "shortcut")
				,new Table("t2", new String[]{"pk", "c2", "common"}, "shortcut")
			};
		mapper = new FieldMapper(tables, "pk");
	}


	private FieldMapper mapper = null;
}
