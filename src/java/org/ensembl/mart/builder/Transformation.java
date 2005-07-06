/*
 * Created on Jun 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk </a>
 * 
 *  
 */

import java.util.*;

public class Transformation {
	
	ArrayList units = new ArrayList();
	
	String finalTableName;
	String finalTableType; // DM MAIN
	String type; // central, linked
	String column_operations;
	String datasetName;
	String targetSchemaName;
	String number;
	Table startTable;
	DBAdaptor adaptor;
	boolean central = false;

	public String userTableName;
	


	public TUnit getFinalUnit() {
		TUnit unit = (TUnit) units.get(units.size() - 1);
		return unit;
	}
	
	
	public void addUnit(TUnit unit) {
		this.units.add(unit);
	}

	public TUnit[] getUnits() {
		TUnit[] b = new TUnit[units.size()];
		return (TUnit[]) units.toArray(b);
	}
	
	

	public void transform() {

		Table temp_end = new Table();
		Table converted_ref = null;
		boolean single = false;
		String temp_end_name =null;
		
		
		String prefix="M";
		if (type.equals("central")) prefix = "C";
		if (type.equals("linked") && finalTableType.equals("DM")) prefix = "D";
		
		
        // transform
		for (int i = 0; i < getUnits().length; i++) {
			
			TUnit unit = getUnits()[i];
			Table temp_start = new Table();

			if (i == 0) {
				temp_start = startTable;
			} else {
				Table new_temp_end = unit.copyTable(temp_end);
				temp_start = new_temp_end;
			}
			
			boolean final_table = false;
			temp_end_name = prefix+"TEMP"+i;

				
			if (single) {
				unit.refTable = converted_ref;
				unit.cardinality = "n1standard"; // needed for left join with central table (boolean filters)
				single = false;
			}
			
			//System.out.println("from tranfromation");
			unit.transform(temp_start, temp_end_name);

			if (unit.single) {
				single = true;
				converted_ref = unit.tempEnd;
			}

			if (i == getUnits().length - 1) {
				unit.tempEnd.setName(finalTableName);
				final_table = true;
			}

			else {
				unit.tempEnd.setName(temp_end_name);
			}

			unit.tempEnd.isFinalTable = final_table;
			unit.tempEnd.temp_name = temp_end_name;

			if (unit.single) {
				temp_end = unit.tempStart;
			} else {
				temp_end = unit.tempEnd;
			}
		}
	
		//getFinalUnit().getTemp_end().setName(myName);
	
	}

	
	
}