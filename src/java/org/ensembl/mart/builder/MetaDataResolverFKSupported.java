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

	
	
	public MetaDataResolverFKSupported(DBAdaptor adaptor){
			
		super(adaptor);
		
	}
	

	
	public Table [] getReferencedTables (String table_name){
		
		Table [] exp_key_tables;
		Table [] imp_key_tables;
		exp_key_tables = getExportedKeyTables(table_name);
		imp_key_tables = getImportedKeyTables(table_name);
		
		Table [] join_tables = new Table [exp_key_tables.length+imp_key_tables.length]; 
		System.arraycopy(exp_key_tables,0,join_tables,0,exp_key_tables.length);
		System.arraycopy(imp_key_tables,0,join_tables,exp_key_tables.length,imp_key_tables.length);
		
		return join_tables;
	}
	
	
	
	
	public Table [] getExportedKeyTables (String maintable){
		
		ArrayList exported_tabs= new ArrayList();
		
		String currentTable = "";
		
		try {
			int i = 0;
			ResultSet keys = dmd.getExportedKeys(getAdaptor().catalog,getAdaptor().schema,maintable);
			while (keys.next()){
			
				//to avoid multiple table when the same table is referenced by multiple keys
				// may cause some problems when the key is chosen.
				
				if (currentTable.equals(keys.getString(7))) continue;
				
				Table table = new Table();
				table.setName(keys.getString(7));
				table.setKey(keys.getString(8));
				table.status="exported";
				table.setColumns(getReferencedColumns(table.getName()));
				exported_tabs.add(table);
				currentTable=keys.getString(7);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		Table [] b = new Table[exported_tabs.size()];
		Table [] array_exp = (Table []) exported_tabs.toArray(b);
		
	return array_exp;
	
	}
	
	
	public Table [] getImportedKeyTables (String maintable){
		
		ArrayList exported_tabs= new ArrayList();
		
		String currentTable = "";
		
		try {
			int i = 0;
			ResultSet keys = dmd.getImportedKeys(getAdaptor().catalog,getAdaptor().schema,maintable);
			while (keys.next()){
				
				// to avoid multiple table when the same table is referenced by multiple keys
				// may cause some problems when the key is chosen.
				if (currentTable.equals(keys.getString(3))) continue;
				
				Table table = new Table();
				table.setName(keys.getString(3));
				table.setKey(keys.getString(4));
				table.status="imported";
				table.setColumns(getReferencedColumns(table.getName()));
				exported_tabs.add(table);
				currentTable=keys.getString(3);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		Table [] b = new Table[exported_tabs.size()];
		Table [] array_exp = (Table []) exported_tabs.toArray(b);
		
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






