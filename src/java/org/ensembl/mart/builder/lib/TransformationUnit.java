/*
 * Created on Jun 15, 2004
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
public abstract class TransformationUnit extends TransformationUnitBase {
	
	Table tempStart;
	Table tempEnd;
	Table refTable;
	String temp_end_name;
	String column_operations;
	String cardinality;
	String final_table_name;
	String TSKey;
	String RFKey;
	String targetSchema;
	DatabaseAdaptor adaptor;
	boolean single =false;
	public String type;
	
	
	public TransformationUnit(Table ref_table){
		
		this.refTable=ref_table;
		
	}
	

	public abstract String toSQL ();
	public abstract void transform (Table temp_start, String temp_end_name);
	
	public String dropTempTable (){
		
		String sql="";
		if (!tempEnd.isFinalTable == true)
			sql = "DROP TABLE "+ targetSchema+"."+tempEnd.getName()+";";
		return sql;	
	}
		
	
	
	public String addIndex(int i){
		
		String sql = "";
		
		if (adaptor.rdbms.equals("postgresql"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempStart.getName()+" ("+TSKey+");";
		else if 	(adaptor.rdbms.equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+tempStart.getName()+" ADD INDEX ("+TSKey+");";
		else if (adaptor.rdbms.equals("oracle"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempStart.getName()+" ("+TSKey+");";
		
		return sql;
		
	}
	
	public String addFinalIndex(int i, String key){
		
		String sql = "";
		
		if (adaptor.rdbms.equals("postgresql"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempEnd.getName()+" ("+key+");";
		else if 	(adaptor.rdbms.equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" ADD INDEX ("+key+");";
		else if (adaptor.rdbms.equals("oracle"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempEnd.getName()+" ("+key+");";
		
		return sql;
		
	}
	
	
	
	
	public String renameKeyColumn(String key){
		
		String sql = "";
		
		if (adaptor.rdbms.equals("postgresql"))
			sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" RENAME "+TSKey+ " TO "+ key+"_key;";
		else if 	(adaptor.rdbms.equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" CHANGE "+TSKey+ " "+ key+"_key INT;";
		else if (adaptor.rdbms.equals("oracle"))
			sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" RENAME COLUMN "+TSKey+ " TO "+ key+"_key;";
		
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
		return refTable;
	}
	/**
	 * @param ref The ref to set.
	 */
	public void setRef_table(Table ref) {
		this.refTable = ref;
	}
	/**
	 * @return Returns the temp_end.
	 */
	public Table getTemp_end() {
		return tempEnd;
	}
	/**
	 * @param temp_end The temp_end to set.
	 */
	public void setTemp_end(Table temp_end) {
		this.tempEnd = temp_end;
	}
	/**
	 * @return Returns the temp_start.
	 */
	public Table getTemp_start() {
		return tempStart;
	}
	/**
	 * @param temp_start The temp_start to set.
	 */
	public void setTemp_start(Table temp_start) {
		this.tempStart = temp_start;
	}
	
	
	
	
	
	
	
}
