/*
 * Created on Jun 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */


import java.util.*;

public class LinkedTables {
	
	Table centralTable;
	ArrayList referenced_tables = new ArrayList();
	String next_main;
	String final_table_type;
	String datasetName;
	String final_table_name;
	
	
	public void addTable (Table table){	
		this.referenced_tables.add(table);
	}
	
	/**
	 * @return Returns the main_table.
	 */
	public Table getCentralTable() {
		return centralTable;
	}
	/**
	 * @param centralTable The main_table to set.
	 */
	public void setCentralTable(Table centralTable) {
		this.centralTable = centralTable;
	}
	/**
	 * @return Returns the referenced_tables.
	 */
	public Table [] getReferencedTables() {
		Table [] b = new Table[referenced_tables.size()];
		return (Table []) referenced_tables.toArray(b);	
	}
	/**
	 * @param referenced_tables The referenced_tables to set.
	 */
	public void setReferencedTables(Table [] tables) {
		this.referenced_tables.addAll(Arrays.asList(tables));
		
	}
}
