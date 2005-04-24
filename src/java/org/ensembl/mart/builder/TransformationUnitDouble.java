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
public class TransformationUnitDouble extends TransformationUnit {
	
	
	public TransformationUnitDouble(Table ref_table){
		
		super(ref_table);
		this.ref_table=ref_table;
		
	}
	
	
	
	public String toSQL (){
		
		String sql = null;
		
		if (cardinality.equals("n1") || cardinality.equals("n1r")){
		
			sql = leftJoin(temp_end.getName());
		
		}
		
		/**
		else if (cardinality.equals ("n1r")){
			
			Table new_ref=copyTable(ref_table);
			Table new_start =copyTable(temp_start);
			
			temp_start=new_ref;
			ref_table=new_start;
			
			sql = leftJoin(temp_end.getName());
		}
		*/
		
		else {
			
			sql = simpleJoin(temp_end.getName());	
		}
		
		return sql;
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
	
	
	
	private Column [] addOneColumn(Table temp_end, Table new_ref){
		
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
			columns = addOneColumn(temp_end,new_ref);
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
				
				if (temp_start.getColumns()[j].getName().equals(new_ref.getColumns()[m].getName())
				&& !  new_ref.getColumns()[m].bool ){
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
	
	
	private String simpleJoin(String temp){
		
		StringBuffer temp_start_col = getStartColumns(temp_start);
		StringBuffer ref_table_col = getRefColumns(ref_table);
		
		String sql = getSQL(" , ", " WHERE ", temp, temp_start_col, ref_table_col);
		return sql;				
		
	}
	
	
	private String leftJoin(String temp){
		
		StringBuffer temp_start_col = getStartColumns(temp_start);
		StringBuffer ref_table_col = getRefColumns(ref_table);
		
		
		String sql = null;
		
		if (cardinality.equals("n1")){
		
		sql = getSQL(" LEFT JOIN ", " ON ", temp, temp_start_col, ref_table_col);
		
		} else {
			
			
			Table new_ref=copyTable(ref_table);
			Table new_start =copyTable(temp_start);
			
			temp_start=new_ref;
			ref_table=new_start;
			
			
			
			sql = getSQL(" LEFT JOIN ", " ON ", temp, temp_start_col, ref_table_col);
			
		}
		
		
		
		return sql;
		
	}
	
	
	
	
	private  StringBuffer getStartColumns (Table temp_start){
		
		StringBuffer temp_start_col = new StringBuffer("");
		
		for (int j=0; j<temp_start.getColumns().length;j++){			
			temp_start_col.append(temp_start.getName()+"."+temp_start.getColumns()[j].getName()+",");
		}
		
		return temp_start_col;
			
	}
	
	private StringBuffer getRefColumns (Table ref_table){
		
		StringBuffer ref_table_col = new StringBuffer("");
		
		for (int j=0; j<ref_table.getColumns().length;j++){
			if (ref_table.getColumns()[j].hasAlias()){		
				ref_table_col.append(ref_table.getName()+"."+ref_table.getColumns()[j].getName()+
						" AS "+ ref_table.getColumns()[j].getAlias()+",");
			} else {
				ref_table_col.append(ref_table.getName()+"."+ref_table.getColumns()[j].getName()+",");
			}
		}
		
		ref_table_col.delete(ref_table_col.length()-1,ref_table_col.length());
	
		return ref_table_col;
	
	}
	
	
	private String getSQL (String ONE, String TWO, String temp,StringBuffer temp_start_col, StringBuffer ref_table_col){
		
		String start=temp_start.getName();
		String ref =ref_table.getName();
		
		// temps are always in the target schema
	    if (temp_start.getName().matches(".*TEMP.*") ) start=targetSchema+"."+temp_start.getName();
	    if (ref_table.getName().matches(".*TEMP.*")) ref=targetSchema+"."+ref_table.getName();
	    
		StringBuffer tempsql = new StringBuffer ("CREATE TABLE "+targetSchema+".");
		
		
	
		tempsql.append(temp+ "  AS SELECT "+temp_start_col.toString()+ref_table_col.toString()+" FROM "+ 
				start+ ONE +ref+ TWO +ref+"."+RFKey+" = "+ start+"."+TSKey);
		
		
		if (ref_table.hasExtension()){
			tempsql.append(" AND "+ref+"."+ref_table.getExtension());
		} 
		if (temp_start.hasExtension()){
			tempsql.append(" AND "+start+"."+temp_start.getExtension());
		} 
		tempsql.append(";");
		
		return tempsql.toString();
		
		
	}
	
	
}
