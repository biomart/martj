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

public class MetaDataResolverMySQL extends MetaDataResolver {
	
	
	
	public MetaDataResolverMySQL(DBAdaptor adaptor){
		
		super(adaptor);
		
	}
	
	
	
	protected Table [] getExportedKeyTables (String maintable){
		
		ArrayList exported_tabs= new ArrayList();
		
		String [] pkeys = getPrimaryKeys(maintable);
		String [] tables = getAllTables();
		
		for (int i =0; i<tables.length;i++){
			
			String [] columns = getColumnNames(tables[i]);
			
			for (int j=0;j<columns.length;j++)
			{
				if (columns[j].equals(pkeys[0])){
					
					Table table = new Table();
					exported_tabs.add(table);
					table.setName(tables[i]);
					table.setKey(pkeys[0]);
					table.status="exported";
					table.setColumns(getReferencedColumns(tables[i]));
					break;
					
				}	
			}
		}
		
		
		Table [] b = new Table[exported_tabs.size()];
		Table [] array_exp = (Table []) exported_tabs.toArray(b);
		
		return array_exp;
		
	}
	
	
	protected Table [] getImportedKeyTables (String maintable){
		
		ArrayList imported_tabs= new ArrayList();
		
		String [] fkeys = getForeignKeys(maintable);
		String [] tables = getAllTables();
		
		
		for (int k=0;k<fkeys.length;k++){
			for (int i =0; i<tables.length;i++){
				
				if (getPrimaryKeys(tables[i]).length == 0){ continue;}
				
				String pk= getPrimaryKeys(tables[i])[0];
				String [] columns = getColumnNames(tables[i]);
				
				for (int j=0;j<columns.length;j++)
				{
					if (columns[j].equals(fkeys[k]) && pk.equals(fkeys[k])){
						
						Table table = new Table();
						imported_tabs.add(table);
						table.setName(tables[i]);
						table.setKey(fkeys[k]);
						table.status="imported";
						table.setColumns(getReferencedColumns(tables[i]));
						
						break;
					}	
				}
				
			}
		}
		
		
		Table [] b = new Table[imported_tabs.size()];
		Table [] array_imp = (Table []) imported_tabs.toArray(b);
		
		return array_imp;
	}
	
	
	
	
	private String [] getPrimaryKeys (String maintable){
		
		ArrayList pkeys= new ArrayList();
		
		try {
			int i = 0;
			ResultSet keys = dmd.getPrimaryKeys(getAdaptor().catalog,getAdaptor().username,maintable);
			while (keys.next()){
				pkeys.add(keys.getString(4));
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		String [] b = new String[pkeys.size()];
		String [] array_keys = (String []) pkeys.toArray(b);
		
		return array_keys;
	}
	
	
	
	private String [] getForeignKeys (String maintable){
		
		ArrayList fkeys= new ArrayList();
		
		String [] columns = getColumnNames(maintable);
		String [] pk = getPrimaryKeys(maintable);
		
		for (int i=0;i<columns.length;i++){
			
			if (columns[i].equals(pk[0])) continue;
			else { 
				fkeys.add(columns[i]);
			}
		}	
		
		String [] b = new String[fkeys.size()];
		String [] array_keys = (String []) fkeys.toArray(b);
		
		return array_keys;
	}
		
	
	private String [] getAllTables (){
		
		ArrayList tabs= new ArrayList();
		
		try {
			int i = 0;
			ResultSet keys = dmd.getTables(getAdaptor().catalog,getAdaptor().username,"%",null);
			while (keys.next()){
				
				String table = keys.getString(3);
				tabs.add(table);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		String [] b = new String [tabs.size()];
		String [] array_tab = (String []) tabs.toArray(b);
		
		return array_tab;
		
	}
	
	
	
	
	
}






