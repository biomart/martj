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
public class TransformationUnitSimple extends TransformationUnit {
	
	
	
	
	public TransformationUnitSimple(Table ref_table){
	    
		super(ref_table);
		this.ref_table=ref_table;
		
	}
	/*
	/**
	*/
	public String toSQL (){
		
		return simpleJoin(temp_start, ref_table, temp_end.getName());
	  
	}
	
	
	
	private Column [] appendColumns(Table temp_end, Table new_ref){
		
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
		temp_end.setColumns(appendColumns(temp_end,new_ref));
		
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
	
	
	
	
}
	
