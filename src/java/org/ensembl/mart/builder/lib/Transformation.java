/*
 * Created on Jun 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk </a>
 * @author <a href="mailto: damian@ebi.ac.uk">Damian Smedley </a>
 * 
 *  
 */

import org.jdom.Element;


public class Transformation extends ConfigurationBase {
	
	private String finalTableName;
	private String finalTableType; // DM MAIN
	private String type; // central, linked
	private String columnOperations;
	private String datasetName;
	private String targetSchemaName;
	private Table startTable;
	private DatabaseAdaptor adaptor;
	boolean central = false;
	
	
	public Transformation (Element element){
		super(element);
		int[] requiredFields = {0,1,2,3,4};
		setRequiredFields(requiredFields);
	}
	
	public Transformation (){
		super();
	}

	void setStartTable(Table startTable) {
	  this.startTable = startTable;
	}

	Table getStartTable() {
	  return startTable;
	}
	
	void setDatasetName(String datasetName) {
	  this.datasetName = datasetName;
	}

	String getDatasetName() {
	  return datasetName;
	}
		
	void setTargetSchemaName(String targetSchemaName) {
	  this.targetSchemaName = targetSchemaName;
	}

	String getTargetSchemaName() {
	  return targetSchemaName;
	}
	
	void setFinalTableName(String finalTableName) {
	  this.finalTableName = finalTableName;
	}

	String getFinalTableName() {
	  return finalTableName;
	}
	
	void setFinalTableType(String finalTableType) {
	  this.finalTableType = finalTableType;
	}

	String getFinalTableType() {
	  return finalTableType;
	}
	
	void setType(String type) {
	  this.type = type;
	}

	String getType() {
	  return type;
	}
	
	void setColumnOperations(String columnOperations) {
	  this.columnOperations = columnOperations;
	}

	String getColumnOperations() {
	  return columnOperations;
	}
	void setAdaptor(DatabaseAdaptor adaptor) {
	  this.adaptor = adaptor;
	}

	DatabaseAdaptor getAdaptor() {
	  return adaptor;
	}	
		
		
	public TransformationUnit getFinalUnit() {
		TransformationUnit unit = (TransformationUnit) getChildObjects()[getChildObjects().length - 1];
		return unit;
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
		for (int i = 0; i < getChildObjects().length; i++) {
			
			TransformationUnit unit = (TransformationUnit) getChildObjects()[i];
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
				unit.getElement().setAttribute("cardinality","n1standard"); // needed for left join with central table (boolean filters)
				single = false;
			}
			
			unit.transform(temp_start, temp_end_name);

			if (unit.single) {
				single = true;
				converted_ref = unit.tempEnd;
			}

			if (i == getChildObjects().length - 1) {
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
	
	}

	
}