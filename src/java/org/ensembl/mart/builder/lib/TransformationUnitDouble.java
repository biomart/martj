/*
 * Created on Jun 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import org.jdom.Element;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */
public class TransformationUnitDouble extends TransformationUnit {
	
	
	public TransformationUnitDouble(Table ref_table){
		
		super(ref_table);
		this.refTable=ref_table;
		
	}
	
	public TransformationUnitDouble(Element element, Table ref_table){
		
		super(ref_table);
		this.element = element;
		this.refTable=ref_table;
	}
	
	public String toSQL (){
		
		String sql = null;
		
		// needed for left join with central filters (boolean)
		if (getElement().getAttributeValue("cardinality").equals("n1standard") || 
			getElement().getAttributeValue("cardinality").equals("n1r")){
			
			//if (cardinality.equals("n1r")){
		
			sql = leftJoin(tempEnd.getName());
		
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
			
			sql = simpleJoin(tempEnd.getName());	
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
		
		Table new_ref=copyTable(refTable);
		//assignAliasesForTempStart(temp_start);
		assignAliases(temp_start, new_ref, temp_end_name);
		assignAliases(temp_start, refTable, temp_end_name);
		
		Table temp_end = copyTable(temp_start);
		
		Column [] columns = null;
		
		if (columnOperations.equals("addall")){
			columns = addAllColumns(temp_end,new_ref);
		} else if (columnOperations.equals("append")){
			columns = appendColumns(temp_end,new_ref);
		} else if (columnOperations.equals("addone")){
			columns = addOneColumn(temp_end,new_ref);
		}
		
		temp_end.setColumns(columns);
		
		setNamesToAliases(temp_end);
		
		this.setTempStart(temp_start);
		this.setTempEnd(temp_end);
		this.setRefTable(refTable);
		
	}
	


	private static void assignAliases(Table temp_start, Table new_ref, String temp){
		
		for (int j=0; j<temp_start.getColumns().length;j++){
			for (int m=0; m<new_ref.getColumns().length;m++){
				
				if (temp_start.getColumns()[j].getName().equals(new_ref.getColumns()[m].getName())
				&& !  new_ref.getColumns()[m].bool  // not sure what that is for 
				&& ! new_ref.getColumns()[m].userAlias // user overrides
				){
					if (new_ref.type.equals("interim")){
						temp_start.getColumns()[j].setAlias(temp_start.getColumns()[j].getName()+"_"+temp);
						//System.out.println("adding alias for interim:"+temp_start.getColumns()[m].getName()+"_"+temp);
					}
					else{
						new_ref.getColumns()[m].setAlias(new_ref.getColumns()[m].getName()+"_"+temp);
						//System.out.println("adding alias:"+temp_start.getColumns()[m].getName()+"_"+temp);	
					}
					
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
		
		StringBuffer temp_start_col = getStartColumns(tempStart);
		StringBuffer ref_table_col = getRefColumns(refTable);
		
		String sql = getSQL(" , ", " WHERE ", temp, temp_start_col, ref_table_col);
		return sql;				
		
	}
	
	
	private String leftJoin(String temp){
		
		StringBuffer temp_start_col = getStartColumns(tempStart);
		StringBuffer ref_table_col = getRefColumns(refTable);
		
		
		String sql = null;
		
		// needed for left join with central fiters (boolean)
		if (getElement().getAttributeValue("cardinality").equals("n1standard")
		    || (getElement().getAttributeValue("cardinality").equals("n1r") && !refTable.type.equals("interim"))){
		
			sql = getSQL(" LEFT JOIN ", " ON ", temp, temp_start_col, ref_table_col);
		
		} 
		else {
			
			
			Table new_ref=copyTable(refTable);
			Table new_start =copyTable(tempStart);
			
			tempStart=new_ref;
			refTable=new_start;
					
			sql = getSQL(" LEFT JOIN ", " ON ", temp, temp_start_col, ref_table_col);
			
		}
		
		
		
		return sql;
		
	}
	
	
	
	
	private  StringBuffer getStartColumns (Table temp_start){
		
		StringBuffer temp_start_col = new StringBuffer("");
		
		for (int j=0; j<temp_start.getColumns().length;j++){	
			
			// user defined alias for the central table
			if (temp_start.getColumns()[j].hasAlias()){		
				temp_start_col.append(temp_start.getName()+"."+temp_start.getColumns()[j].getName()+
						" AS "+ temp_start.getColumns()[j].getAlias()+",");
			} else {
			
			temp_start_col.append(temp_start.getName()+"."+temp_start.getColumns()[j].getName()+",");
		}
		}
		return temp_start_col;
			
	}
	
	private StringBuffer getRefColumns (Table ref_table){
		
		StringBuffer ref_table_col = new StringBuffer("");
		
		for (int j=0; j<ref_table.getColumns().length;j++){
			//System.out.println("REF TABLE " + ref_table.getName() + " HAS COL " + ref_table.getColumns()[j].getName());
			if (ref_table.getColumns()[j].hasAlias()){		
				//System.out.println("HAS ALIAS");
				ref_table_col.append(ref_table.getName()+"."+ref_table.getColumns()[j].getName()+
						" AS "+ ref_table.getColumns()[j].getAlias()+",");
			} else {
				ref_table_col.append(ref_table.getName()+"."+ref_table.getColumns()[j].getName()+",");
			}
		}
		if (ref_table_col.length() > 0){
			ref_table_col.delete(ref_table_col.length()-1,ref_table_col.length());
		}
		return ref_table_col;
	
	}
	
	
	private String getSQL (String ONE, String TWO, String temp,StringBuffer temp_start_col, StringBuffer ref_table_col){
		
		String start=tempStart.getName();
		String ref =refTable.getName();
		
		// temps and interims are always in the target schema
	    if (tempStart.getName().matches(".*TEMP.*") ) start=targetSchema+"."+start;
	    else if (tempStart.type.equals("interim")) start=targetSchema+"."+start;
	    
	    if (refTable.getName().matches(".*TEMP.*")) ref=targetSchema+"."+ref;
	    else if (refTable.type.equals("interim")) ref=targetSchema+"."+ref;
	    
	    
	    //	System.out.println("type: "+tempStart.type+" name "+tempStart.getName());
	    
		StringBuffer tempsql = new StringBuffer ("CREATE TABLE "+targetSchema+".");
		
		if (ref_table_col.length() <= 0 && temp_start_col.length() > 0){// if no ref cols then delete the extra comma
			temp_start_col.delete(temp_start_col.length()-1,temp_start_col.length());
		}
		
		tempsql.append(temp+ "  AS SELECT "+temp_start_col.toString()+ref_table_col.toString()+" FROM "+ 
							start+ ONE +ref+ TWO +ref+"."+RFKey+" = "+ start+"."+TSKey);	
		
		
		
		if (refTable.hasExtension()){
			tempsql.append(" AND "+ref+"."+refTable.getExtension());
		} 
		if (refTable.hasCentralExtension()){
			tempsql.append(" AND "+start+"."+refTable.getCentralExtension());
		} 
		
		if (this.getElement().getAttributeValue("distinct") != null && 
				this.getElement().getAttributeValue("distinct").equals("1")){
			tempsql.append(" GROUP BY "+temp_start_col.toString()+ref_table_col.toString());
			//System.out.println(tempsql);
		}
		
		tempsql.append(";");
		
		return tempsql.toString();
		
		
	}
	
	
}
