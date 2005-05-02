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

public class MetaDataResolverFKNotSupported extends MetaDataResolver {
	
	
	
	public MetaDataResolverFKNotSupported(DBAdaptor adaptor){
		
		super(adaptor);
		
	}
	
	
	
	public Table [] getExportedKeyTables (String maintable, String [] columnNames){
		
		ArrayList exported_tabs= new ArrayList();
		
		String pkey = getPrimaryKeys(maintable);
		String [] tables = getAllTables();
		
		for (int i =0; i<tables.length;i++){
			
			String [] columns = getColumnNames(tables[i]);
			
			for (int j=0;j<columns.length;j++)
			{
				if (columns[j].equals(pkey)){
					
					Table table = new Table();
					exported_tabs.add(table);
					table.setName(tables[i]);
					
					// needs FK and PK instead
					//table.setKey(pkey);
					
					table.status="exported";
					table.setColumns(getReferencedColumns(tables[i],columnNames));
					break;
					
				}	
			}
		}
		
		
		Table [] b = new Table[exported_tabs.size()];
		Table [] array_exp = (Table []) exported_tabs.toArray(b);
		
		return array_exp;
		
	}
	
	
	public Table [] getImportedKeyTables (String maintable, String [] columnNames){
		
		ArrayList imported_tabs= new ArrayList();
		
		String [] fkeys = getForeignKeys(maintable);
		String [] tables = getAllTables();
		
		
		for (int k=0;k<fkeys.length;k++){
			for (int i =0; i<tables.length;i++){
				
				if (getPrimaryKeys(tables[i]) == null){ continue;}
				
				String pk = getPrimaryKeys(tables[i]);
				String [] columns = getColumnNames(tables[i]);
				
				for (int j=0;j<columns.length;j++)
				{
					if (columns[j].equals(fkeys[k]) && pk.equals(fkeys[k])){
						
						Table table = new Table();
						imported_tabs.add(table);
						table.setName(tables[i]);
						//table.setKey(fkeys[k]);
						// needs PK and FK here
						
						table.status="imported";
						table.setColumns(getReferencedColumns(tables[i], columnNames));
						
						break;
					}	
				}
				
			}
		}
		
		
		Table [] b = new Table[imported_tabs.size()];
		Table [] array_imp = (Table []) imported_tabs.toArray(b);
		
		return array_imp;
	}
	
	
	
	
	protected String getPrimaryKeys (String maintable){
		
		String pk = null;
		
		try {
			
			// DMD does not understand mysql composite key, need to get them 'hard way'.
			//ResultSet keys = dmd.getPrimaryKeys(getAdaptor().catalog,getAdaptor().username,maintable);

			String sql = "describe " + maintable;
			PreparedStatement ds = connection.prepareStatement(sql);
            ResultSet rs= ds.executeQuery();
			
            // this is not going to work properly with composite keys, needs user input
			while (rs.next()){
				if (rs.getString(4).equals("PRI"))
					pk = rs.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return pk;
	}
	
	
	
	private String [] getForeignKeys (String maintable){
		
		ArrayList fkeys= new ArrayList();
		
		String [] columns = getColumnNames(maintable);
		String pk = getPrimaryKeys(maintable);
		
		for (int i=0;i<columns.length;i++){
			
			if (columns[i].equals(pk)) continue;
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
			ResultSet keys = dmd.getTables(getAdaptor().catalog,getAdaptor().schema,"%",null);
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






