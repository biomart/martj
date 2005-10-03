/*
 * Created on Jun 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;



/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */

import java.sql.*;
import java.util.*;


public class MetaDataResolverDMDPlatform extends MetaDataResolver {

	
	
	public MetaDataResolverDMDPlatform(DatabaseAdaptor adaptor){
			
		super(adaptor);
		
	}
	
	
	/*public String [] getAllKeys (String tableName){
		
		// gets all PK and FKs for a table
		HashMap allKeys= new HashMap();
		
		String currentTable = "";
		String currentPK = "";
		String currentFK = "";
		
		try {
			ResultSet keys = dmd.getExportedKeys(getAdaptor().getCatalog(),getAdaptor().getSchema(),tableName);
			while (keys.next()){
			
				// avoid duplications when table referenced by multiple keys	
				if (currentTable.equals(keys.getString(7))
					&& currentPK.equals(keys.getString(4))
					&& currentFK.equals(keys.getString(8))) continue;
				
				allKeys.put(keys.getString(4),"1");
				currentTable=keys.getString(7);
				currentPK=keys.getString(4);
				currentFK=keys.getString(8);		
			}
			currentTable = "";
			currentPK = "";
			currentFK = "";
			
			keys = dmd.getImportedKeys(getAdaptor().getCatalog(),getAdaptor().getSchema(),tableName);
			while (keys.next()){
			
							// avoid duplications when table referenced by multiple keys	
							if (currentTable.equals(keys.getString(7))
								&& currentPK.equals(keys.getString(4))
								&& currentFK.equals(keys.getString(8))) continue;
				
				allKeys.put(keys.getString(8),"");
							currentTable=keys.getString(7);
							currentPK=keys.getString(4);
							currentFK=keys.getString(8);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		String[] b = new String[allKeys.size()];
		int i = 0;
		for (Iterator iter = allKeys.keySet().iterator(); iter.hasNext();) {
		   b[i] = (String) iter.next();
		   i++;
		}
		return b;
	
	}*/
	
	
	public Table [] getExportedKeyTables (String centralTableName, String [] columnNames){
		
		ArrayList exportedTabs= new ArrayList();
		
		String currentTable = "";
		String currentPK = "";
		String currentFK = "";
		
		try {
			int i = 0;
			ResultSet keys = dmd.getExportedKeys(getAdaptor().getCatalog(),getAdaptor().getSchema(),centralTableName);
			while (keys.next()){
			
				// avoid duplications when table referenced by multiple keys	
				if (currentTable.equals(keys.getString(7))
					&& currentPK.equals(keys.getString(4))
					&& currentFK.equals(keys.getString(8))) continue;
				
				//Table table = new Table();
				Table table;
				if (tableStore.get(keys.getString(7)) != null){
					table = (Table) tableStore.get(keys.getString(7));
					table.PK=keys.getString(4);// update incase changed
					table.FK=keys.getString(8);
					table.status="exported";
					table.setColumns(getReferencedColumns(table.getName(), columnNames));			
				}
				else{
					table = new Table();
					table.setName(keys.getString(7));
					table.PK=keys.getString(4);
					table.FK=keys.getString(8);
				
				
					//table.setKey(keys.getString(8));
					table.status="exported";
					table.setColumns(getReferencedColumns(table.getName(), columnNames));
					tableStore.put(keys.getString(7),table);
				}
				exportedTabs.add(table);
				currentTable=keys.getString(7);
				currentPK=keys.getString(4);
				currentFK=keys.getString(8);			
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
		String currentPK="";
		String currentFK="";
		
		try {
			int i = 0;
			ResultSet keys = dmd.getImportedKeys(getAdaptor().getCatalog(),getAdaptor().getSchema(),centralTableName);
			while (keys.next()){
				
				// to avoid multiple table when the same table is referenced by multiple keys
				// may cause some problems when the key is chosen.
				//if (currentTable.equals(keys.getString(3)) & currentKey.equals(keys.getString(4))) continue;
				
//				avoid duplications when table referenced by multiple keys	
							 if (currentTable.equals(keys.getString(7))
								 && currentPK.equals(keys.getString(4))
								 && currentFK.equals(keys.getString(8))) continue;
				
//				Table table = new Table();
				Table table;
				if (tableStore.get(keys.getString(3)) != null){
					table = (Table) tableStore.get(keys.getString(3));
					table.PK=keys.getString(4);//update incase changed
					table.FK=keys.getString(8);
					table.status="imported";
					table.setColumns(getReferencedColumns(table.getName(), columnNames));			
				}
				else{
					table = new Table();
					table.setName(keys.getString(3));
					table.PK=keys.getString(4);
					table.FK=keys.getString(8);
				
					//table.setKey(keys.getString(4));
					table.status="imported";
					table.setColumns(getReferencedColumns(table.getName(), columnNames));
					tableStore.put(keys.getString(3),table);
				}
				importedTabs.add(table);
				currentTable=keys.getString(3);
				currentPK=keys.getString(4);
				currentFK=keys.getString(8);
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
 		ResultSet keys = dmd.getPrimaryKeys(adaptor.getCatalog(),adaptor.getSchema(),table);
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






