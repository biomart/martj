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
	

	
	public abstract Table [] getReferencedTables (String table_name);
	
	
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
	
	public Table getMainTable (String main_name){
		
		Table table = new Table();
		table.setName(main_name);
		table.setColumns(getReferencedColumns(table.getName()));
		
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






