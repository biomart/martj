/*
 * Created on Jun 15, 2004
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
public abstract class TransformationUnit {
	
	Table temp_start;
	Table temp_end;
	Table ref_table;
	String temp_end_name;
	String central_extension;
	String extension_key;
	String column_operations;
	String cardinality;
	String final_table_name;
	String key;
	String targetSchema;
	DBAdaptor adaptor;
	boolean is_extension=false;
	boolean has_extension=false;
	boolean useFK=false;
	boolean single =false;
	boolean isFirst;
	
	
	public TransformationUnit(Table ref_table){
		
		this.ref_table=ref_table;
		
	}
	

	public abstract String toSQL ();
	public abstract void transform (Table temp_start, String temp_end_name);
	
	public String dropTempTable (){
		
		String sql="";
		if (!temp_end.final_table == true)
			sql = "drop table "+ targetSchema+"."+temp_end.getName()+";";
		return sql;	
	}
		
	
	
	public String addIndex(int i){
		
		String sql = "";
		
		if (adaptor.rdbms.equals("postgresql"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+temp_end.getName()+" ("+temp_start.key+");";
		else if 	(adaptor.rdbms.equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+temp_end.getName()+" ADD INDEX ("+temp_start.key+");";
		else if (adaptor.rdbms.equals("oracle"))
			sql = "CREATE INDEX index "+i+targetSchema+"."+temp_end.getName()+" ADD INDEX ("+temp_start.key+");";	
		
		return sql;
		
	}
	
	public String addFinalIndex(int i, String key){
		
		String sql = "";
		
		if (adaptor.rdbms.equals("postgresql"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+temp_end.getName()+" ("+key+");";
		else if 	(adaptor.rdbms.equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+temp_end.getName()+" ADD INDEX ("+temp_start.key+");";
		else if (adaptor.rdbms.equals("oracle"))
			sql = "CREATE INDEX index "+i+targetSchema+"."+temp_end.getName()+" ADD INDEX ("+temp_start.key+");";	
		
		return sql;
		
	}
	
	
	
	
	public String renameKeyColumn(String key){
		
		String sql = "";
		
		if (adaptor.rdbms.equals("postgresql"))
			sql = "ALTER TABLE "+targetSchema+"."+temp_end.getName()+" RENAME "+key+ " TO "+ key+"_key;";
		else if 	(adaptor.rdbms.equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+temp_end.getName()+" ADD INDEX ("+temp_start.key+");";
		else if (adaptor.rdbms.equals("oracle"))
			sql = "CREATE INDEX index "+key+targetSchema+"."+temp_end.getName()+" ADD INDEX ("+temp_start.key+");";	
		
		return sql;
		
	}
	
	
	
	protected Table copyTable(Table old_table){
		
		Table new_table= new Table();
		
		try {
			new_table = (Table) old_table.clone();
		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}	
		
		return new_table;
	}
	
	
	
	/**
	 * @return Returns the ref.
	 */
	public Table getRef_table() {
		return ref_table;
	}
	/**
	 * @param ref The ref to set.
	 */
	public void setRef_table(Table ref) {
		this.ref_table = ref;
	}
	/**
	 * @return Returns the temp_end.
	 */
	public Table getTemp_end() {
		return temp_end;
	}
	/**
	 * @param temp_end The temp_end to set.
	 */
	public void setTemp_end(Table temp_end) {
		this.temp_end = temp_end;
	}
	/**
	 * @return Returns the temp_start.
	 */
	public Table getTemp_start() {
		return temp_start;
	}
	/**
	 * @param temp_start The temp_start to set.
	 */
	public void setTemp_start(Table temp_start) {
		this.temp_start = temp_start;
	}
	
	
	
	
	
	
	
}
