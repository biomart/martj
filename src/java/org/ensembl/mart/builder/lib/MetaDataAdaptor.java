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


public abstract class MetaDataAdaptor {
	
	protected Table [] tabs;
	protected Connection connection;
	protected Column [] columns;
	//protected ArrayList columns;
	protected DatabaseAdaptor adaptor;
	protected DatabaseMetaData dmd;
	
	public MetaDataAdaptor(DatabaseAdaptor adaptor){
		
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
		
		String [] columnNames={"%"};
		//columnNames[0]="%";
		
		exp_key_tables = getExportedKeyTables(table_name, columnNames);
		imp_key_tables = getImportedKeyTables(table_name, columnNames);
		
		Table [] join_tables = new Table [exp_key_tables.length+imp_key_tables.length]; 
		System.arraycopy(exp_key_tables,0,join_tables,0,exp_key_tables.length);
		System.arraycopy(imp_key_tables,0,join_tables,exp_key_tables.length,imp_key_tables.length);
		
		return join_tables;
	}
	
	
	
	
	public abstract Table [] getExportedKeyTables (String table_name, String [] columnNames);
	public abstract Table [] getImportedKeyTables (String table_name, String [] columnNames);
	protected abstract String getPrimaryKeys(String table_name);
	
	
	
	public String [] getAllTableNames () throws SQLException{
		
		String [] types = {"TABLE"};
		ArrayList nameList = new ArrayList();
		
		int i = 0;
		// types filter doesn't work
		ResultSet resultNames = dmd.getTables(getAdaptor().getCon().getCatalog(),getAdaptor().schema,"%",types);
		while (resultNames.next()){
			nameList.add(resultNames.getString(3));
			i++;
		}
		String[] names = new String[nameList.size()];
		nameList.toArray(names);
		
		return names;
		
	}
	
	
	
	public Column [] getReferencedColumns (String name, String [] columnNames){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		for (int i=0;i<columnNames.length;i++){
		try {
			ResultSet columns=dmd.getColumns(getAdaptor().catalog,getAdaptor().schema,name,columnNames[i]);
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
		}
		Column [] b = new Column[cols.size()];
		
						
		if (cols.size() == 0){
			System.out.println("no columns for your table, please check the table/column name " +
			" for "+name.toUpperCase()+" in your config file");		
		}
				
		return (Column []) cols.toArray(b);
	}
	
	
	public Column [] getReferencedColumns (String name, String [] columnNames, String [] columnAliases){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		for (int i=0;i<columnNames.length;i++){
			
			try {
			ResultSet columns=dmd.getColumns(getAdaptor().catalog,getAdaptor().schema,name,columnNames[i]);
			
			
			//System.out.println("cat "+getAdaptor().catalog+" schema "+getAdaptor().schema+" name "+name+" column "+columnNames[i]);
			
		     // had to switch this off, 'beforeFirst()' does not work with oracle
			//columns.beforeFirst();
			
			while (columns.next()){	
				
				//System.out.println("got column: "+columns.getString(4));
				
				Column column = new Column();
				column.setName(columns.getString(4));
				column.original_name=columns.getString(4);
				column.original_table=name;
				if (columnAliases != null){
				if (!columnAliases[i].equals("null")) {
					column.setAlias(columnAliases[i]);
				    column.userAlias=true;
				
				//System.out.println("setting alias "+column.original_table+" colmn name "+column.name+" alias "+column.alias);
				}
			}
			
				cols.add(column);
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		}
		Column [] b = new Column[cols.size()];
				
		if (cols.size() == 0){
			System.out.println("no columns for your table, please check name/capitalisation for your schema: "+getAdaptor().schema+ 
				" table: "+name+" and user defined columns in your transformation config file and db connection window");		
		}
		
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
	
	
	
	
	
	
	public Table getCentralTable (String main_name){
		
		Table table = new Table();
		table.setName(main_name);
		String [] columnNames = {"%"};
		
		table.setColumns(getReferencedColumns(table.getName(),columnNames));
		table.PK =getPrimaryKeys(main_name);
		
		// this table needs to behave like a ref table for recursive joins
		table.PK=getPrimaryKeys(main_name);
		table.FK=getPrimaryKeys(main_name);
		// for weired recursive joins
		table.status="exported";
		
		return table;
	}
	
	
	public Table getCentralTable (String centralTableName,String [] columnNames, String [] columnAliases){
		
		Table table = new Table();
		table.setName(centralTableName);
		//String [] columnNames = {"%"};
		
		table.setColumns(getReferencedColumns(table.getName(),columnNames,columnAliases));
		table.PK =getPrimaryKeys(centralTableName);
		
		// this table needs to behave like a ref table for recursive joins
		table.PK=getPrimaryKeys(centralTableName);
		table.FK=getPrimaryKeys(centralTableName);
		// for weired recursive joins
		table.status="exported";
		
		return table;
	}
	
	
	
	
	
	public Table getTableColumns (String tableName, String [] columnNames, String [] columnAliases) {
		
		Table table = new Table();
		table.setName(tableName);
		table.setColumns(getReferencedColumns(tableName, columnNames, columnAliases));
		
		return table;
	}
	
	
	
	public Table getTable (String tableName, String columnName) {
		
		Table table = new Table();
		table.setName(tableName);
		
		Column column = new Column();
		column.setName(columnName);
		columns  = new Column[1];
		columns[0]=column;
		table.setColumns(columns);
		//table.setColumns(getReferencedColumns(tableName, columnNames, columnAliases));
		
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
	public DatabaseAdaptor getAdaptor() {
		return adaptor;
	}
	/**
	 * @param adaptor The adaptor to set.
	 */
	public void setAdaptor(DatabaseAdaptor adaptor) {
		this.adaptor = adaptor;
	}
}






