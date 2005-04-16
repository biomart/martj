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
	String column_operations;
	String targetName;
	DBAdaptor adaptor;
	private LinkedTables linked;
	Table start_table;
	boolean central = false;

	
	
	public void create (Table [] ref_tables) {
		
		//TransformationUnit unit;
		
		Table temp_end = new Table();
		
		for (int i=0; i<ref_tables.length; i++){
			
			if (ref_tables[i].skip) continue;
			if (type.equals("central")){
				TransformationUnitSingle sunit =  new TransformationUnitSingle(ref_tables[i]);
				sunit.single=true;
				sunit.adaptor=adaptor;
				sunit.targetSchema=targetName;
				units.add(sunit);	
			} 
			
			TransformationUnitDouble dunit = new TransformationUnitDouble(ref_tables[i]);
			dunit.cardinality=ref_tables[i].cardinality;
			dunit.column_operations=column_operations; 
			dunit.final_table_name=final_table_name;
			dunit.adaptor=adaptor;
			dunit.targetSchema=targetName;
			units.add(dunit);
		}
	}
	
	
	
	
	
	public void addAdditionalUnit(Table ref_table,String final_table_key,String final_table_extension){
		
		TransformationUnitDouble unit = new TransformationUnitDouble(ref_table);
		unit.extension_key=final_table_key;
		unit.central_extension=final_table_extension;
		unit.is_extension=true;
		unit.has_extension=true;
		unit.adaptor=adaptor;
		unit.targetSchema=targetName;
		addUnit(unit);
	}
	
	
	
	
	
	
	public TransformationUnit getFinalUnit(){
		
		TransformationUnit unit = (TransformationUnit) units.get(units.size()-1);
		return unit;
		
	}
	
		
	public void transform () {
		
		Table temp_end = new Table();
		Table converted_ref = null;
		boolean single = false;
		String temp_end_name="TEMP";
		
		for (int i=0; i<getUnits().length; i++){
			
			TransformationUnit unit = getUnits()[i];
			Table temp_start = new Table();
			
			
			if (i == 0){
				temp_start = start_table;
			} 
			else  {
				Table new_temp_end= unit.copyTable(temp_end);
				temp_start=new_temp_end;
				temp_start.setExtension("");
			}
			
			
			boolean final_table = false;
			
			if (type.equals("central")){
				temp_end_name ="C"+temp_end_name;
			} if (type.equals("linked") && final_table_type.equals("DM")){
				temp_end_name ="D"+temp_end_name;
			}
			temp_end_name =temp_end_name+i;
		
			
			if (!( single ||  unit.single)){
			unit.key=unit.ref_table.key;
			} 
			else {
			unit.key=temp_start.key;
			}
			
			if(single){
				unit.ref_table=converted_ref;
				unit.cardinality="n1";
				single =false;
			}
			
			
			unit.transform(temp_start, temp_end_name);
			
			

			if (unit.single){
				single=true;
				converted_ref=unit.temp_end;
				
			}
			
			
			
			
			if (i== getUnits().length-1){
				unit.temp_end.setName(final_table_name);
				final_table=true;
			} 
			
			
			else {
				unit.temp_end.setName(temp_end_name);
			}
			
			
			
			if (unit.is_extension){
				unit.temp_start.key=unit.extension_key;	
			}
			if (unit.has_extension){
				unit.temp_start.extension=unit.central_extension;    	
			}
			
			unit.temp_end.final_table=final_table;
			unit.temp_end.temp_name=temp_end_name;
			
			
			if(unit.single){
				temp_end=unit.temp_start;
			} else {
			temp_end=unit.temp_end;
			}
		}
	}
			 
			 
	
	
	
	
	
	
	
	
	
	private void addUnit (TransformationUnit unit){	
		this.units.add(unit);
	}
	
	
	public void setFinalName(String name){
		
	getFinalUnit().getTemp_end().setName(name);
		
	}
	
	
	public TransformationUnit [] getUnits() {
		TransformationUnit [] b = new TransformationUnit[units.size()];
		return (TransformationUnit []) units.toArray(b);	
	}
	
	
	
}
