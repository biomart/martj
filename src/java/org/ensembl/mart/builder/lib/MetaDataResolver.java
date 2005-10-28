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


public abstract class MetaDataResolver {
	
	protected Table [] tabs;
	protected Connection connection;
	protected Column [] columns;
	//protected ArrayList columns;
	protected DatabaseAdaptor adaptor;
	protected DatabaseMetaData dmd;
	protected HashMap tableStore = new HashMap();
	
	public MetaDataResolver(DatabaseAdaptor adaptor){
		
		try {		
			setAdaptor(adaptor);
			setConnection(adaptor.getCon());
			if (adaptor.getCon() != null) dmd = adaptor.getCon().getMetaData();
			
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
		
		exp_key_tables = getExportedKeyTables(table_name, columnNames);
		imp_key_tables = getImportedKeyTables(table_name, columnNames);
		
		Table [] join_tables = new Table [exp_key_tables.length+imp_key_tables.length+1]; 
		System.arraycopy(exp_key_tables,0,join_tables,0,exp_key_tables.length);
		System.arraycopy(imp_key_tables,0,join_tables,exp_key_tables.length,imp_key_tables.length);
		
		// adds central table for recursive transformations
		join_tables[join_tables.length-1]=getCentralTable(table_name);
		
		return join_tables;
	}
	
	
	
	
	public abstract Table [] getExportedKeyTables (String table_name, String [] columnNames);
	public abstract Table [] getImportedKeyTables (String table_name, String [] columnNames);
	protected abstract String getPrimaryKeys(String table_name);
	//protected abstract String[] getAllKeys(String table_name);
	
	public String [] getAllTableNames () {
		
		String [] types = {"TABLE"};
		ArrayList nameList = new ArrayList();
		
		int i = 0;
		// types filter doesn't work
		ResultSet resultNames;
		try {
			resultNames = dmd.getTables(getAdaptor().getCon().getCatalog(),getAdaptor().getSchema(),"%",types);
		
		while (resultNames.next()){
			nameList.add(resultNames.getString(3));
			i++;
		}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		String[] names = new String[nameList.size()];
		nameList.toArray(names);
		
		return names;
		
	}
	
	public String [] getAllTableNamesBySchema (String schema) {
		
		String [] types = {"TABLE"};
		ArrayList nameList = new ArrayList();
		
		int i = 0;
		// types filter doesn't work
		ResultSet resultNames;
		try {
			resultNames = dmd.getTables(getAdaptor().getCon().getCatalog(),schema,"%",types);
		
		while (resultNames.next()){
			nameList.add(resultNames.getString(3));
			i++;
		}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		String[] names = new String[nameList.size()];
		nameList.toArray(names);
		
		return names;
		
	}
	
	public String [] getAllSchemas () {
		
		ArrayList nameList = new ArrayList();
		
		ResultSet resultNames;
		try {
			resultNames = dmd.getSchemas();
		
		while (resultNames.next()){
			nameList.add(resultNames.getString(1));
		}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String[] names = new String[nameList.size()];
		nameList.toArray(names);
		
		return names;
		
	}
	
	public Column [] getReferencedColumns (String name, String [] columnNames){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		// for reading oracle created config in mysql
		if (adaptor.getRdbms().equals("mysql")) name =name.toLowerCase();
		
		
		
		for (int i=0;i<columnNames.length;i++){
		try {
			ResultSet columns=dmd.getColumns(getAdaptor().getCatalog(),getAdaptor().getSchema(),name,columnNames[i]);
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
			System.out.println("Message from DMDREsolver: No columns for your table, please check name/capitalisation for your schema: "+getAdaptor().getSchema()+ 
					" table: "+name+" and user defined columns in your transformation config file and db connection window");		
		}
				
		return (Column []) cols.toArray(b);
	}
	
	
	public Column [] getReferencedColumns (String name, String [] columnNames, String [] columnAliases){//, Column[] centralCols){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		

		// for reading oracle created config in mysql
		if (adaptor.getRdbms().equals("mysql")) name =name.toLowerCase();
		
		
				
		for (int i=0;i<columnNames.length;i++){
			
			try {
			ResultSet columns=dmd.getColumns(getAdaptor().getCatalog(),getAdaptor().getSchema(),name,columnNames[i]);
			
			
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
						if (!columnAliases[i].equals(columns.getString(4))){
							column.setAlias(columnAliases[i]);
				    		column.userAlias=true;
						}
					}
				}
			
				cols.add(column);
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		}
		
		/*
		// get a list of essential keys to add incase aliased or removed
		String[] essentialKeys = getAllKeys(name);
		OUTER:for (int i = 0; i < essentialKeys.length; i++){
			for (int j = 0; j < cols.size(); j++){
				if (((Column) cols.get(j)).getName().toLowerCase().equals(essentialKeys[i].toLowerCase())
						&& (!((Column) cols.get(j)).hasAlias()) || (((Column) cols.get(j)).hasAlias() && ((Column) cols.get(j)).getAlias().toLowerCase().equals(essentialKeys[i].toLowerCase()))){
					continue OUTER;
				}
			}
			
			//	checking this column doesn't already exist in the current central/temp table
			if (centralCols != null){
				for(int k = 0; k < centralCols.length; k++){
					if (centralCols[k].getName().toLowerCase().equals(essentialKeys[i].toLowerCase()))
						continue OUTER;		
				}
			}
			
			//System.out.println("ADDING "+essentialKeys[i]+" TO "+name);
			Column column = new Column();
			column.setName(essentialKeys[i]);
			column.original_name=essentialKeys[i];
			column.original_table=name;
			
			cols.add(column);
		}*/
		
		Column [] b = new Column[cols.size()];
				
		if (cols.size() == 0){
			System.out.println("Message from DMDREsolver: No columns for your table, please check name/capitalisation for your schema: "+getAdaptor().getSchema()+ 
				" table: "+name+" and user defined columns in your transformation config file and db connection window");		
		}
		
		return (Column []) cols.toArray(b);
	}
	
	
	
	
	public String [] getColumnNames (String name){
		
		Column [] col;
		ArrayList cols = new ArrayList();
		
		try {
			ResultSet columns=dmd.getColumns(getAdaptor().getCatalog(),getAdaptor().getUsername(),name,"%");
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
		//Table table = new Table();
		Table table;
		if (tableStore.get(main_name) != null){
			table = (Table) tableStore.get(main_name);
			// should recache cols but breaks things
			table.PK=getPrimaryKeys(main_name);
			table.FK=getPrimaryKeys(main_name);
			// for weired recursive joins
			table.status="exported";
		}
		else{
			table = new Table();
			table.setName(main_name);
			String [] columnNames = {"%"};
			table.setColumns(getReferencedColumns(table.getName(),columnNames));
			// this table needs to behave like a ref table for recursive joins
			table.PK=getPrimaryKeys(main_name);
			table.FK=getPrimaryKeys(main_name);
			// for weired recursive joins
			table.status="exported";
			tableStore.put(main_name,table);
		}
		return table;
	}
	
	
	public Table getCentralTable (String centralTableName,String [] columnNames, String [] columnAliases){

		Table table;
		if (tableStore.get(centralTableName) != null){
			table = (Table) tableStore.get(centralTableName);
			// should cache cols again
			
			//System.out.println("USING CACHED COPY FOR "+centralTableName);
			//System.out.println("SHOULD SET TO FOLLOWING ONLY:");
			for (int i = 0; i < columnNames.length; i++){
				//System.out.println(columnNames[i]);
			}
			//System.out.println("CACHED TABLE HAS:");
			Column[] cols = table.getColumns();
			for (int i = 0; i < cols.length; i++){
				//System.out.println(cols[i].getName());
			}
			
			table.setColumns(getReferencedColumns(table.getName(),columnNames,columnAliases));
			//System.out.println("CACHED TABLE NOW HAS:");
			cols = table.getColumns();
			for (int i = 0; i < cols.length; i++){
				//System.out.println(cols[i].getName());
			}
			
			
			// this table needs to behave like a ref table for recursive joins
			table.PK=getPrimaryKeys(centralTableName);
			table.FK=getPrimaryKeys(centralTableName);
			// for weired recursive joins
			table.status="exported";			
		}
		else{
			table = new Table();		
			table.setName(centralTableName);
			//String [] columnNames = {"%"};
		
			table.setColumns(getReferencedColumns(table.getName(),columnNames,columnAliases));
			// this table needs to behave like a ref table for recursive joins
			table.PK=getPrimaryKeys(centralTableName);
			table.FK=getPrimaryKeys(centralTableName);
			// for weired recursive joins
			table.status="exported";
			tableStore.put(centralTableName,table);
		}
		return table;
	}
	
	
	public ArrayList getDistinctValuesForPartitioning (String chosenColumn, String chosenTable){
		
		
		
		
		ArrayList allValList = new ArrayList();
		 
		   String sql = "SELECT DISTINCT "+chosenColumn+" FROM "+getAdaptor().getSchema()+"."+chosenTable
			  +" WHERE "+chosenColumn+" IS NOT NULL";
		   PreparedStatement ps;
		try {
			ps = getConnection().prepareStatement(sql);
		
		ResultSet rs = ps.executeQuery();
		   
		   
		   while (rs.next()){
		   		allValList.add(rs.getString(1));
		   }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return allValList;
	}
	
	
	
	public Table getTableColumns (String tableName, String [] columnNames, String [] columnAliases){//, Column[] centralCols) {
		
		//Table table = new Table();
		Table table;
		if (tableStore.get(tableName) != null){
			table = (Table) tableStore.get(tableName);
			table.setColumns(getReferencedColumns(tableName, columnNames, columnAliases));//incase changed				
		}
		else{
			table = new Table();
			table.setName(tableName);
			table.setColumns(getReferencedColumns(tableName, columnNames, columnAliases));
			tableStore.put(tableName,table);
		}
		return table;
	}
	
	
	
	public Table getTable (String tableName, String columnName) {
		
		//Table table = new Table();
		Table table;
		if (tableStore.get(tableName) != null){
			table = (Table) tableStore.get(tableName);
			Column column = new Column();// incase changed
			column.setName(columnName);
			columns  = new Column[1];
			columns[0]=column;
			table.setColumns(columns);
		}
		else{
			table = new Table();
			table.setName(tableName);
		
			Column column = new Column();
			column.setName(columnName);
			columns  = new Column[1];
			columns[0]=column;
			table.setColumns(columns);
			//table.setColumns(getReferencedColumns(tableName, columnNames, columnAliases));
			tableStore.put(tableName,table);
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






