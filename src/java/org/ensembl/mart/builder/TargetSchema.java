/*
 * Created on Jun 14, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

import java.util.*;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */


public class TargetSchema {
	
	ArrayList transformations = new ArrayList();
	SourceSchema source_schema;
	
	public TargetSchema (SourceSchema source_schema){
		
		this.source_schema=source_schema;
	
		createTransformationsForLinked();
		createTransformationsForMains();
		createTransformationsForCentralFilters();
		
	}
	
	
	
	private void createTransformationsForLinked(){
		
		for (int j=0;j<source_schema.getLinkedTables().length;j++){
			
			LinkedTables linked = source_schema.getLinkedTables()[j];
		    Table [] referenced_tables = linked.getReferencedTables();
			
		    Transformation transformation = new Transformation();
			transformation.final_table_type=linked.final_table_type;
			transformation.final_table_name=linked.final_table_name;
			transformation.start_table=linked.getMainTable();
			transformation.type="linked";
			transformation.create(referenced_tables);
			
			transformation.transform();
			addTransformation(transformation);
		    
			}
	}
	
	private void createTransformationsForMains(){
		
		Transformation [] trans = getTransformations();
		ArrayList mains = new ArrayList();
		
		for (int i=0;i<trans.length;i++){
			if (trans[i].final_table_type.equals("MAIN")){
				Table main = trans[i].getFinalUnit().getTemp_end();
				mains.add(main);
			}
		}
		
		
		ArrayList ref_tables = new ArrayList();
		for (int i=1;i<mains.size();i++){
			
            Table main = (Table) mains.get(0);
            Table ref = (Table) mains.get(i);
            ref_tables.add(ref);
            Table [] b = new Table [ref_tables.size()];
            Table [] tables = (Table []) ref_tables.toArray(b);
            
            
            Transformation transformation = new Transformation();
            transformation.final_table_name =ref.getName();
            
            ref.setName(ref.temp_name);
            transformation.start_table=main;
		    transformation.type="main";
            transformation.create(tables);
		    
            transformation.transform();
            addTransformation(transformation);
		
		}		
	}
	
	
	
	
	private void createTransformationsForCentralFilters(){
		
	}
	
	private void transform(){
		
		Transformation [] trans = getTransformations();
		
		for (int i=0;i<trans.length;i++){
			trans[i].transform();
		}
	}
	
	
	public void addTransformationUnit(String final_table_name,String new_table_name,String final_table_key,String final_table_extension,
			String new_table_key, String new_table_extension, String new_table_cardinality){
		
		Transformation trans = getTransformationByFinalName(final_table_name);
		Column [] columns = source_schema.getTableColumns(new_table_name);
	
		Table reftable = new Table();
		reftable.setName(new_table_name);
		reftable.setColumns(columns);
		reftable.setName(new_table_name);	
		reftable.setKey(new_table_key);
		reftable.setExtension(new_table_extension);
		reftable.setCardinality(new_table_cardinality);	
	    
		trans.addAdditionalUnit(reftable,final_table_key,final_table_extension);
		trans.transform();
	
}
	
	public Transformation [] getTransformations() {
		
		Transformation [] b = new Transformation[transformations.size()];
		return (Transformation []) transformations.toArray(b);	
		
	}
	
	public void addTransformation(Transformation transformation){
		this.transformations.add(transformation);
			
	}
	
	
	private Transformation getTransformationByFinalName(String name){
		
		Transformation trans = new Transformation();
		
		for (int i=0;i<transformations.size();i++){
			trans = (Transformation) transformations.get(i);
			if (trans.final_table_name.equals(name)){
			break;
			}
		}
		
		return trans;
	}
	
	
	
}