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

public class MetaDataResolverOracle {
	
	private Table [] tabs;
	private Connection connection;
	private Column [] columns;
	private DBAdaptor adaptor;
	private DatabaseMetaData dmd;
	
	public MetaDataResolverOracle(DBAdaptor adaptor){
		
		try {		
			Connection con = adaptor.initialiseConnections();
			setAdaptor(adaptor);
			setConnection(con);
			dmd = con.getMetaData();
			
		}
		catch(SQLException ex) {
			System.err.print("SQLException: ");
			System.err.println(ex.getMessage());
		}		
	}
	
	
	
	public Table [] getExportedKeyTables (String maintable){
		
		ArrayList exported_tabs= new ArrayList();
		
		try {
			int i = 0;
			ResultSet keys = dmd.getExportedKeys(getAdaptor().catalog,getAdaptor().username,maintable);
			while (keys.next()){
				
				Table table = new Table();
				table.setName(keys.getString(7));
				table.setKey(keys.getString(8));
				table.status="exported";
				table.setColumns(getReferencedColumns(table));
				exported_tabs.add(table);
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
		
		try {
			int i = 0;
			ResultSet keys = dmd.getImportedKeys(getAdaptor().catalog,getAdaptor().username,maintable);
			while (keys.next()){
				
				Table table = new Table();
				table.setName(keys.getString(3));
				table.setKey(keys.getString(4));
				table.status="imported";
				table.setColumns(getReferencedColumns(table));
				exported_tabs.add(table);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		Table [] b = new Table[exported_tabs.size()];
		Table [] array_exp = (Table []) exported_tabs.toArray(b);
		
	return array_exp;
	}
	
	
	
	public Column [] getReferencedColumns (Table table){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		try {
			ResultSet columns=dmd.getColumns(getAdaptor().catalog,getAdaptor().username,table.getName(),"%");
			int z=0;
			while (columns.next()){	
				
				Column column = new Column();
				column.setName(columns.getString(4));
				column.original_table=table.getName();
				cols.add(column);
				z++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Column [] b = new Column[cols.size()];
		return (Column []) cols.toArray(b);
	}
	
	public Table getMainTable (String main_name){
		
		Table table = new Table();
		table.setName(main_name);
		table.setColumns(getReferencedColumns(table));
		
		try {
			DatabaseMetaData dmd = getConnection().getMetaData();
			ResultSet keys = dmd.getPrimaryKeys(adaptor.catalog,adaptor.username,main_name);
			while (keys.next()){
			table.setKey(keys.getString(4));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return table;
	}
	
	
	/**
	 * @return Returns the tabs.
	 */
	private Table[] getTabs() {
		return tabs;
	}
	/**
	 * @param tabs The tabs to set.
	 */
	private void setTabs(Table[] tabs) {
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






