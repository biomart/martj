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
public class TransformationUnit {
	
	Table temp_start;
	Table temp_end;
	Table ref_table;
	String temp_end_name;
	String extension;
	String extension_key;
	String column_operations;
	boolean is_extension=false;
	boolean has_extension=false;
	boolean useFK=false;
	
	
	public TransformationUnit(Table ref_table){
		
		this.ref_table=ref_table;
		
	}
	
		
	
	public String toSQL (){
		
		return simpleJoin(temp_start, ref_table, temp_end.getName());
		
	}
	
	
	
	private Column [] addAllColumns(Table temp_end, Table new_ref){
		
		Column [] temp_col = new Column [temp_end.getColumns().length+new_ref.getColumns().length];	
		System.arraycopy(temp_end.getColumns(),0,temp_col,0,temp_end.getColumns().length);
		System.arraycopy(new_ref.getColumns(), 0,temp_col,temp_end.getColumns().length, new_ref.getColumns().length);
		
		return temp_col;
	}
	
	private Column [] appendColumns(Table temp_end, Table new_ref){
		
		Column [] temp_col = new Column [temp_end.getColumns().length+new_ref.getColumns().length];	
		System.arraycopy(temp_end.getColumns(),0,temp_col,0,temp_end.getColumns().length);
		System.arraycopy(new_ref.getColumns(), 0,temp_col,temp_end.getColumns().length, new_ref.getColumns().length);
		
		return temp_col;
	}
	
	
	
	private Column [] addOneColumns(Table temp_end, Table new_ref){
		
		Column [] temp_col = new Column [temp_end.getColumns().length+new_ref.getColumns().length];	
		System.arraycopy(temp_end.getColumns(),0,temp_col,0,temp_end.getColumns().length);
		System.arraycopy(new_ref.getColumns(), 0,temp_col,temp_end.getColumns().length, new_ref.getColumns().length);
		
		return temp_col;
	}
	
	
	
	
	
	public void transform (Table temp_start, String temp_end_name){
		
		Table new_ref=copyTable(ref_table);
		
		assignAliases(temp_start, new_ref, temp_end_name);
		assignAliases(temp_start, ref_table, temp_end_name);
		
		Table temp_end = copyTable(temp_start);
		
		Column [] columns = null;
		
		if (column_operations.equals("addall")){
			columns = addAllColumns(temp_end,new_ref);
		} else if (column_operations.equals("append")){
			columns = appendColumns(temp_end,new_ref);
		} else if (column_operations.equals("addone")){
			columns = addOneColumns(temp_end,new_ref);
		}
		
		temp_end.setColumns(columns);
		
		setNamesToAliases(temp_end);
		
		this.setTemp_start(temp_start);
		this.setTemp_end(temp_end);
		this.setRef_table(ref_table);
		
	}
	
	private static void assignAliases(Table temp_start, Table new_ref, String temp){
		
		for (int j=0; j<temp_start.getColumns().length;j++){
			for (int m=0; m<new_ref.getColumns().length;m++){
				
				if (temp_start.getColumns()[j].getName().equals(new_ref.getColumns()[m].getName())){
					new_ref.getColumns()[m].setAlias(new_ref.getColumns()[m].getName()+"_"+temp);
				}
			}
		}		
	}
	
	
	private static void setNamesToAliases(Table temp_end){
		
		
		for (int m=0; m<temp_end.getColumns().length;m++){
			
			if (temp_end.getColumns()[m].hasAlias()){
				temp_end.getColumns()[m].setName(temp_end.getColumns()[m].getAlias());
				temp_end.getColumns()[m].setAlias("");
				
			}
		}			
	}
	
	
	
	
	
	
	
	
	
	public String dropTempTable (){
		
		String sql="";
		if (!temp_end.final_table == true)
			sql = "drop table "+ temp_end.getName()+";";
		return sql;
		
	}
	
	private static String simpleJoin(Table temp_start, Table ref_table, String temp){
		
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
				ref_table.getKey()+" = "+ temp_start.getName()+"."+temp_start.getKey());
		if (ref_table.hasExtension()){
			tempsql.append(" AND "+ref_table.getName()+"."+ref_table.getExtension());	
		} 
		if (temp_start.hasExtension()){
			tempsql.append(" AND "+temp_start.getName()+"."+temp_start.getExtension());	
		} 
		tempsql.append(";");
		
		return tempsql.toString();				
		
	}
	
	
	
	private Table copyTable(Table old_table){
		
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
