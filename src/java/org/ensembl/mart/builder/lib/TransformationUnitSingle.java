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
public class TransformationUnitSingle extends TransformationUnit {
	
	
	public TransformationUnitSingle(Table ref_table){
		
		super(ref_table);
		this.refTable=ref_table;
	}
	
	
	
	public String toSQL (){
		
		String sql = null;
		 
		if (type.equals("partition")) sql = partitionSQL(); 
		else if (type.equals("notNull")) sql = notNullSQL();
		else System.out.println ("not supported TUnit type");
	
		return sql;
		
	}
	
	
	private String partitionSQL(){
		
		String sql = sql = "CREATE TABLE "+ targetSchema+"."+tempEnd.getName()+" AS SELECT * FROM "+refTable.getName();
		
		if (refTable.hasCentralExtension()){
			sql= sql+" WHERE "+refTable.getName()+"."+refTable.getCentralExtension()+";";
		} else sql = sql +";";
		
		return sql;
	}
	
	
	
	
	private String notNullSQL(){
		
		String sql = sql = "CREATE TABLE "+ targetSchema+"."+tempEnd.getName()+" AS SELECT DISTINCT "+ TSKey+
		" FROM "+ targetSchema+"."+refTable.getName()+" WHERE "+ RFKey+ " IS NOT NULL;";
		
		return sql;
	}
	
	
	
	
	
	public void transform (Table temp_start, String temp_end_name){
		
		Table temp_end = null;
		
		// for central convert ref table
		if (this.type.equals("notNull")){
		Table new_ref=convertTable(refTable, temp_start);
		temp_end = copyTable(new_ref);
		temp_end.isFinalTable=false;
		} else temp_end = copyTable(refTable);
		
		this.setTemp_end(temp_end);
		this.setTemp_start(temp_start);
		
	}

	private Table convertTable(Table ref_table, Table temp_start){
		
		Table new_ref = new Table();
		new_ref = copyTable(ref_table);
		Column [] columns = ref_table.getColumns();
		Column [] newcol = new Column[1];
		boolean foundKey=false;
		//System.out.println("temp start name "+tempStart.getName());
		//System.out.println(" temp end name "+ tempEnd.getName());
		//System.out.println(" ref name "+refTable.getName()+" start "+temp_start.getName());
		
		String [] tableNameParts=refTable.getName().split("__");
			
		//System.out.println("name parts 1 "+tableNameParts[1]);
		
		for (int i=0;i<columns.length;i++){
			
			// below depends if you start your transformation directly from 
			// link table or one level down (as it is at the moment)
			// it requires therefore a central transformation key comparison rather
			// than tempStart.PK or refTable.PK as this will vary
			
			// the below assumes that you can't have different cases for column names in one schema
			if (columns[i].getName().toUpperCase().equals(TSKey.toUpperCase())){
				
				//System.out.println ("PK "+temp_start.PK+" TSKey "+TSKey+" RFKey "+RFKey);
				
				newcol[0]=columns[i];
				newcol[0].setAlias(tableNameParts[1]+"_bool");
				newcol[0].bool=true;
				foundKey=true;
				break;
			}	
		}
		assert foundKey: "CAN'T FIND MATCHING TRANSROMATION KEY:    "+TSKey;		
		new_ref.setColumns(newcol);
		return new_ref;
	}
	
}
