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
	String dataset;
	SourceSchema source_schema;
	
	public TargetSchema (SourceSchema source_schema){
		
		this.source_schema=source_schema;
		dataset= source_schema.dataset;
		
		for (int j=0;j<source_schema.getLinkedTables().length;j++){
	    Transformation final_table = new Transformation(source_schema.getLinkedTables()[j]);	
		final_table.dataset = dataset;
	    addTransformation(final_table);
		}
	}
	
	
	public void addTransformationUnit(String final_table_name,String new_table_name,String final_table_key,String final_table_extension,
			String new_table_key, String new_table_extension, String new_table_cardinality){
		
		Transformation trans = getTransformationByFinalName(final_table_name);
		Table table = source_schema.getTableColumns(new_table_name);
		System.out.println("length "+table.getColumns().length);
		table.setName(new_table_name);	
		table.setKey(new_table_key);
		table.setExtension(new_table_extension);
		table.setCardinality(new_table_cardinality);	
	
		trans.addAdditionalUnit(table,final_table_key,final_table_extension);
		
		


}
	
	public Transformation [] getTransformations() {
		
		Transformation [] b = new Transformation[1];
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