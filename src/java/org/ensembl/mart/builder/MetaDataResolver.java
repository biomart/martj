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

public abstract class MetaDataResolver {
	
	protected Table [] tabs;
	protected Connection connection;
	protected Column [] columns;
	protected DBAdaptor adaptor;
	protected DatabaseMetaData dmd;
	
	public MetaDataResolver(DBAdaptor adaptor){
		
		try {		
			setAdaptor(adaptor);
			setConnection(adaptor.getCon());
			dmd = adaptor.getCon().getMetaData();
			
		}
		catch(SQLException ex) {
			System.err.print("SQLException: ");
			System.err.println(ex.getMessage());
		}		
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
	
	
	protected abstract Table [] getExportedKeyTables (String table_name);
	protected abstract Table [] getImportedKeyTables (String table_name);
	protected abstract String getPrimaryKeys(String table_name);
	
	
	public Column [] getReferencedColumns (String name){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		try {
			ResultSet columns=dmd.getColumns(getAdaptor().catalog,getAdaptor().username,name,"%");
			int z=0;
			while (columns.next()){	
				
				Column column = new Column();
				column.setName(columns.getString(4));
				column.original_name=columns.getString(4);
				column.original_table=name;
				cols.add(column);
				z++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Column [] b = new Column[cols.size()];
		return (Column []) cols.toArray(b);
	}
	
	
	public String [] getColumnNames (String name){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		try {
			ResultSet columns=dmd.getColumns(getAdaptor().catalog,getAdaptor().username,name,"%");
			int z=0;
			while (columns.next()){	
				cols.add(columns.getString(4));
				z++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String [] b = new String[cols.size()];
		return (String []) cols.toArray(b);
	}
	
	
	
	
	
	
	public Table getMainTable (String main_name){
		
		Table table = new Table();
		table.setName(main_name);
		table.setColumns(getReferencedColumns(table.getName()));
		
		table.setKey(getPrimaryKeys(main_name));
	
	/**	
		try {
			DatabaseMetaData dmd = getConnection().getMetaData();
			ResultSet keys = dmd.getPrimaryKeys(adaptor.catalog,adaptor.username,main_name);
			while (keys.next()){
			table.setKey(keys.getString(4));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		*/
		
		return table;
	}
	
	
	/**
	 * @return Returns the tabs.
	 */
	protected Table[] getTabs() {
		return tabs;
	}
	/**
	 * @param tabs The tabs to set.
	 */
	protected void setTabs(Table[] tabs) {
		this.tabs = tabs;
	}
	/**
	 * @return Returns the connection.
	 */
	public Connection getConnection() {
		return connection;
	}
	/**
	 * @param connection The connection to set.
	 */
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	/**
	 * @return Returns the adaptor.
	 */
	public DBAdaptor getAdaptor() {
		return adaptor;
	}
	/**
	 * @param adaptor The adaptor to set.
	 */
	public void setAdaptor(DBAdaptor adaptor) {
		this.adaptor = adaptor;
	}
}






