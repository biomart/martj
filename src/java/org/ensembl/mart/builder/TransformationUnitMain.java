/*
 * Created on Jun 24, 2004
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
public class TransformationUnitMain extends TransformationUnit {
	
	public TransformationUnitMain (Table ref_table){
		
		super(ref_table);
		
	}

	
	public String toSQL(){
		
		return simpleJoin(temp_start, ref_table, temp_end.getName());
			
	}
	
	
	public void transform (Table temp_start, String temp_end_name){
		
		
		Table new_ref=copyTable(ref_table);
		
	//	assignAliases(temp_start, new_ref, temp_end_name);
	//	assignAliases(temp_start, ref_table, temp_end_name);
		
	
		Table temp_end = copyTable(temp_start);
	//	temp_end.setColumns(appendColumns(temp_end,new_ref));
		
		temp_end.setColumns(addNewColumns(temp_end,new_ref));
		temp_end.setName(temp_end_name);
		
	//	setNamesToAliases(temp_end);
		
		this.setTemp_start(temp_start);
		this.setTemp_end(temp_end);
		this.setRef_table(ref_table);
		
	}
	
	
	private Column [] addNewColumns(Table temp_end, Table new_ref){
		
		Column [] temp_col = new Column [temp_end.getColumns().length+new_ref.getColumns().length];	
		System.arraycopy(temp_end.getColumns(),0,temp_col,0,temp_end.getColumns().length);
		System.arraycopy(new_ref.getColumns(), 0,temp_col,temp_end.getColumns().length, new_ref.getColumns().length);
		
		return temp_col;
	}
	
	
	
	
	
}
