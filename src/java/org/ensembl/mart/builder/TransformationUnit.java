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
	String extension;
	String extension_key;
	boolean is_extension=false;
	boolean has_extension=false;
	boolean useFK=false;
	
	
	public TransformationUnit(Table ref_table){
		
		this.ref_table=ref_table;
		
	}
	
	

	public abstract String toSQL ();
	
	public String dropTempTable (){
		
		String sql="";
		if (!temp_end.final_table == true)
			sql = "drop table "+ temp_end.getName()+";";
		return sql;
		
	}
	
	protected static String simpleJoin(Table temp_start, Table ref_table, String temp){
		
		StringBuffer temp_start_col = new StringBuffer("");
		StringBuffer ref_table_col = new StringBuffer("");
		
		for (int j=0; j<temp_start.getColumns().length;j++){			
			temp_start_col.append(temp_start.getName()+"."+temp_start.getColumns()[j].getName()+",");
		}
		for (int j=0; j<ref_table.getColumns().length;j++){
			if (ref_table.getColumns()[j].hasAlias()){		
				ref_table_col.append(ref_table.getName()+"."+ref_table.getColumns()[j].getName()+
						" AS "+ ref_table.getColumns()[j].getAlias()+",");
			} else {
				ref_table_col.append(ref_table.getName()+"."+ref_table.getColumns()[j].getName()+",");
			}
		}
		
		ref_table_col.delete(ref_table_col.length()-1,ref_table_col.length());
		
		
		StringBuffer tempsql = new StringBuffer ("CREATE TABLE ");
		
		tempsql.append(temp+ "  SELECT "+temp_start_col.toString()+ref_table_col.toString()+" FROM "+ 
				temp_start.getName()+	", "+ref_table.getName()+ " WHERE " +ref_table.getName()+"."+
				ref_table.getKey()+" = "+ temp_start.getName()+"."+ref_table.getKey());
		if (ref_table.hasExtension()){
			tempsql.append(" AND "+ref_table.getName()+"."+ref_table.getExtension());	
		} 
		if (temp_start.hasExtension()){
			tempsql.append(" AND "+temp_start.getName()+"."+temp_start.getExtension());	
		} 
		tempsql.append(";");
		
		return tempsql.toString();				
		
	}
	
	public abstract void transform (Table temp_start, String temp_end_name);
	
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
