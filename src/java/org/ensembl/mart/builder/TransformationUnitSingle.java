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
public class TransformationUnitSingle extends TransformationUnit {
	
	
	
	public TransformationUnitSingle(Table ref_table){
		
		super(ref_table);
		this.ref_table=ref_table;
	}
	
	
	
	public String toSQL (){
		
		String sql = "CREATE TABLE "+ temp_end_name+" SELECT DISTINCT("+ ref_table.key+") " +
		"FROM "+ ref_table.getName()+" WHERE "+ ref_table.central_extension+";";
		return sql;
		
	}
	
	
	
	public void transform (Table temp_start, String temp_end_name){
		
		Table new_ref=convertTable(ref_table);
		Table temp_end = copyTable(new_ref);
		temp_end.final_table=false;
		this.setTemp_end(temp_end);
		
	}

	private Table convertTable(Table ref_table){
		
		Table new_ref = new Table();
		new_ref = copyTable(ref_table);
		Column [] columns = ref_table.getColumns();
		Column [] newcol = new Column [1];
		
		for (int i=0;i<columns.length;i++){
			if (columns[i].getName().equals(ref_table.key)){
				
				newcol[0]=columns[i];
				newcol[0].setAlias(newcol[0].original_table+"__bool");
				newcol[0].bool=true;
				break;
			}	
		}
		
		new_ref.setColumns(newcol);
		return new_ref;
	}
	
}
