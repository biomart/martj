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
	String final_table_name;
	String final_table_type;
	String type;
	private LinkedTables linked;
	Table main_table;
	
	public Transformation (){		
	}
	
	public Transformation (Table [] referenced_tables){
		
		createUnits(referenced_tables);
		
	}
	
	
	
	public void addAdditionalUnit(Table ref_table,String final_table_key,String final_table_extension){
		
		TransformationUnit unit = new TransformationUnit(ref_table);
		unit.setJoinType("simple");
		unit.extension_key=final_table_key;
		unit.extension=final_table_extension;
		unit.is_extension=true;
		unit.has_extension=true;
		addUnit(unit);
	}
	
	public TransformationUnit getFinalUnit(){
		
		TransformationUnit unit = (TransformationUnit) units.get(units.size()-1);
		return unit;
		
	}
	
	private void createUnits (Table [] tables) {
		
		Table temp_end = new Table();
		for (int i=0; i<tables.length; i++){
			
			TransformationUnit unit = new TransformationUnit(tables[i]);
			units.add(unit);
		}
	}
	
	
	
	public void transform () {
		
		Table temp_end = new Table();
		ArrayList unwanted = new ArrayList();
		
		for (int i=0; i<getUnits().length; i++){
			
			TransformationUnit unit = getUnits()[i];
			
			Table temp_start = new Table();
			
			if (i == 0){
				temp_start = main_table;	
			} else  {temp_start=temp_end;}
			
			
			String temp_end_name;
			boolean final_table = false;
			temp_end_name ="TEMP"+i;
			
			
			unit.transform(temp_start, temp_end_name);
			
			
			if (i== getUnits().length-1){
				unit.temp_end.setName(final_table_name);
				final_table=true;
			} else {
				unit.temp_end.setName(temp_end_name);
			}
			if (unit.is_extension){
				unit.temp_start.key=unit.extension_key;	
			}
			if (unit.has_extension){
				unit.temp_start.extension=unit.extension;    	
			}
			
			unit.temp_end.final_table=final_table;
			unit.temp_end.temp_name=temp_end_name;
			temp_end=unit.temp_end;
			
			unit.setJoinType("simple");
			units.set(i,unit);
			
		}
	}
	
	
	
	private void addUnit (TransformationUnit unit){	
		this.units.add(unit);
	}
	
	
	
	
	
	public TransformationUnit [] getUnits() {
		TransformationUnit [] b = new TransformationUnit[units.size()];
		return (TransformationUnit []) units.toArray(b);	
	}
	
	
	
}
