/*
 * Created on Jun 3, 2004
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

import java.sql.*;
import java.util.*;

public class MetaDataResolverFKSupported extends MetaDataResolver {

	
	
	public MetaDataResolverFKSupported(DatabaseAdaptor adaptor){
			
		super(adaptor);
		
	}
	

	
	public Table [] getReferencedTables (String table_name){
		
		Table [] exp_key_tables;
		Table [] imp_key_tables;
		
		String [] columnNames=null;
		columnNames[0]="%";
		
		exp_key_tables = getExportedKeyTables(table_name, columnNames);
		imp_key_tables = getImportedKeyTables(table_name, columnNames);
		
		Table [] join_tables = new Table [exp_key_tables.length+imp_key_tables.length]; 
		System.arraycopy(exp_key_tables,0,join_tables,0,exp_key_tables.length);
		System.arraycopy(imp_key_tables,0,join_tables,exp_key_tables.length,imp_key_tables.length);
		
		return join_tables;
	}
	
	
	
	
	public Table [] getExportedKeyTables (String centralTableName, String [] columnNames){
		
		ArrayList exportedTabs= new ArrayList();
		
		String currentTable = "";
		
		try {
			int i = 0;
			ResultSet keys = dmd.getExportedKeys(getAdaptor().catalog,getAdaptor().schema,centralTableName);
			while (keys.next()){
			
				//to avoid multiple table when the same table is referenced by multiple keys
				// may cause some problems when the key is chosen.
				
				//if (currentTable.equals(keys.getString(7))) continue;
				
				Table table = new Table();
				table.setName(keys.getString(7));
				table.PK=keys.getString(4);
				table.FK=keys.getString(8);
				
				//table.setKey(keys.getString(8));
				table.status="exported";
				table.setColumns(getReferencedColumns(table.getName(), columnNames));
				exportedTabs.add(table);
				currentTable=keys.getString(7);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		// for weired recursive joins
		exportedTabs.add(getCentralTable(centralTableName));
		
		Table [] b = new Table[exportedTabs.size()];
		Table [] array_exp = (Table []) exportedTabs.toArray(b);
		
	return array_exp;
	
	}
	
	
	public Table [] getImportedKeyTables (String centralTableName, String [] columnNames){
		
		ArrayList importedTabs= new ArrayList();
		
		String currentTable = "";
		String currentKey="";
		
		try {
			int i = 0;
			ResultSet keys = dmd.getImportedKeys(getAdaptor().catalog,getAdaptor().schema,centralTableName);
			while (keys.next()){
				
				// to avoid multiple table when the same table is referenced by multiple keys
				// may cause some problems when the key is chosen.
				if (currentTable.equals(keys.getString(3)) & currentKey.equals(keys.getString(4))) continue;
				
				Table table = new Table();
				table.setName(keys.getString(3));
				table.PK=keys.getString(4);
				table.FK=keys.getString(8);
				
				//table.setKey(keys.getString(4));
				table.status="imported";
				table.setColumns(getReferencedColumns(table.getName(), columnNames));
				importedTabs.add(table);
				currentTable=keys.getString(3);
				currentKey=keys.getString(4);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
//		 for weired recursive joins
		importedTabs.add(getCentralTable(centralTableName));
		
		Table [] b = new Table[importedTabs.size()];
		Table [] array_exp = (Table []) importedTabs.toArray(b);
		
	return array_exp;
	}
	

 protected String getPrimaryKeys ( String table){
 	
 	String pk = null;
 	
 	try {
 		DatabaseMetaData dmd = getConnection().getMetaData();
 		ResultSet keys = dmd.getPrimaryKeys(adaptor.catalog,adaptor.schema,table);
 		while (keys.next()){
 	    // This needs to be user specifed as it is not going to work properly with composite keys
 		pk=keys.getString(4);
 		}
 	} catch (SQLException e) {
 		e.printStackTrace();
 	}
 	
 	return pk;
 }



}






