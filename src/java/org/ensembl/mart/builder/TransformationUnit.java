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
	Table ref;
	String join;
	
	
	public String toSQL (){
		
		String sql="";
		if (join.equals("simple"))
			sql=simpleJoin(temp_start, ref, temp_end.getName());
		
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
		
		tempsql.append(temp+ " as SELECT "+temp_start_col.toString()+ref_table_col.toString()+" FROM "+ 
				temp_start.getName()+	", "+ref_table.getName()+ " WHERE " +ref_table.getName()+"."+
				ref_table.getKey()+" = "+ temp_start.getName()+"."+temp_start.getKey());
		if (ref_table.hasExtension()){
			tempsql.append(" AND "+ref_table.getName()+"."+ref_table.getExtension()+";");	
		} else {tempsql.append(";");}
		
		return tempsql.toString();				
		
	}
	
	
	/**
	 * @return Returns the ref.
	 */
	public Table getRef() {
		return ref;
	}
	/**
	 * @param ref The ref to set.
	 */
	public void setRef(Table ref) {
		this.ref = ref;
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
	/**
	 * @return Returns the join.
	 */
	public String getJoinType() {
		return join;
	}
	/**
	 * @param join The join to set.
	 */
	public void setJoinType(String join) {
		this.join = join;
	}
}
