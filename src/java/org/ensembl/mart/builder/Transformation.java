/*
 * Created on Jun 12, 2004
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

import java.util.*;

public class Transformation {
	
	ArrayList units = new ArrayList();
	ArrayList unwanted = new ArrayList();
	String dataset;
	
	public Transformation (LinkedTables linked){
		this.unwanted = createUnits(linked);	
		//String [] b = new String [unwanted.size()];
		//this.unwanted = (String []) unwanted.toArray(b);
	}
	
	
	private ArrayList createUnits (LinkedTables linked) {
		
		Table temp_end = new Table();
		ArrayList unwanted = new ArrayList();
		
		for (int i=0; i<linked.getReferencedTables().length; i++){
         	
			Table ref_table = linked.getReferencedTables()[i];
		    
			if (ref_table.cardinality.equals("1n") || 
					ref_table.cardinality.equals("0n")){
				unwanted.add(ref_table.getName());
			}
			
			TransformationUnit unit = new TransformationUnit();
			Table temp_start = new Table();
			
			if (i == 0){
				temp_start = linked.getMainTable();	
			} else  {temp_start=temp_end;}
			
			
			String temp_end_name;
			boolean final_table = false;
			temp_end_name ="TEMP"+i;
			
			Table new_ref=copyTable(ref_table);
			
			assignAliases(temp_start, new_ref, temp_end_name);
			assignAliases(temp_start, ref_table, temp_end_name);
			
			temp_end = copyTable(temp_start);
			temp_end.setColumns(appendColumns(temp_end,new_ref));
			
			setNamesToAliases(temp_end);
			
			
			if (i== linked.getReferencedTables().length-1){
				temp_end.setName(linked.final_table_name);
			final_table=true;
			} else {
				temp_end.setName(temp_end_name);
			}
			temp_end.final_table=final_table;
			
			
			
			
			
			unit.setTemp_start(temp_start);
			unit.setTemp_end(temp_end);
			unit.setRef_table(ref_table);
			unit.setJoinType("simple");
			units.add(unit);
		}
	
		return unwanted;
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
	
	
	private void addUnit (TransformationUnit unit){	
		this.units.add(unit);
	}
	
	public TransformationUnit [] getUnits() {
		TransformationUnit [] b = new TransformationUnit[units.size()];
		return (TransformationUnit []) units.toArray(b);	
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
	
	private Column [] appendColumns(Table temp_end, Table new_ref){
		
		Column [] temp_col = new Column [temp_end.getColumns().length+new_ref.getColumns().length];	
		System.arraycopy(temp_end.getColumns(),0,temp_col,0,temp_end.getColumns().length);
		System.arraycopy(new_ref.getColumns(), 0,temp_col,temp_end.getColumns().length, new_ref.getColumns().length);
		
		return temp_col;
	}
	
}
