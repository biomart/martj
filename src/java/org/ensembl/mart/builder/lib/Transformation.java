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

import java.util.HashMap;

import org.jdom.Element;


public class Transformation extends ConfigurationBase {
	
	private String finalTableName;
	private String finalTableType; // DM MAIN
	private String type; // central, linked
	private String columnOperations;
	private String datasetName;
	private String targetSchemaName;
	private Table startTable;
	boolean central = false;
	private int[] requiredFields = {0,1,2,3,4};
	
	public Transformation (Element element){
		super(element);
		setRequiredFields(requiredFields);
	}
	
	public Transformation (){
		super();
		setRequiredFields(requiredFields);
	}

	public Transformation(String internalName,String tableType,String centralTable,String userTableName,
		String includeCentralFilter){
		super();
		element.setAttribute("internalName",internalName);
		element.setAttribute("tableType",tableType);
		element.setAttribute("centralTable",centralTable);
		element.setAttribute("userTableName",userTableName);
		element.setAttribute("includeCentralFilter",includeCentralFilter);
		setRequiredFields(requiredFields);
	}
	
	
	
	/**
	public Transformation(Transformation transformation){		 
			this(transformation.getElement().getAttributeValue("internalName"),//+"_copy",
					transformation.getElement().getAttributeValue("tableType"),
					transformation.getElement().getAttributeValue("centralTable"),
					transformation.getElement().getAttributeValue("userTableName"),
					transformation.getElement().getAttributeValue("includeCentralFilter"));
			ConfigurationBase[] transformationUnits = transformation.getChildObjects();
			for (int j = 0; j < transformationUnits.length; j++){
				//TransformationUnit tUnit = new TransformationUnit((Element) transformationUnits[j].element.clone());
				TransformationUnit tUnit = new TransformationUnit(
					transformationUnits[j].getElement().getAttributeValue("internalName"),
					transformationUnits[j].getElement().getAttributeValue("referencingType"),
					transformationUnits[j].getElement().getAttributeValue("primaryKey"),
					transformationUnits[j].getElement().getAttributeValue("referencedTable"),
					transformationUnits[j].getElement().getAttributeValue("cardinality"),
					transformationUnits[j].getElement().getAttributeValue("centralProjection"),
					transformationUnits[j].getElement().getAttributeValue("referencedProjection"),
					transformationUnits[j].getElement().getAttributeValue("foreignKey"),
					transformationUnits[j].getElement().getAttributeValue("referenceColumnNames"),
					transformationUnits[j].getElement().getAttributeValue("referenceColumnAliases"),
					transformationUnits[j].getElement().getAttributeValue("centralColumnNames"),
					transformationUnits[j].getElement().getAttributeValue("centralColumnAliases"),
					transformationUnits[j].getElement().getAttributeValue("externalSchema"));
				insertChildObject(j,tUnit);
			}
	}
	
	*/
	
	
	
	
	

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
	
	
		
	public TransformationUnit getFinalUnit() {
		TransformationUnit unit = (TransformationUnit) getChildObjects()[getChildObjects().length - 1];
		return unit;
	}

	public String createNewMain (String extraMain){
		String[] tableParts = finalTableName.split("__");
		String newTableName = tableParts[0]+"__"+extraMain+"__main";
		
		ConfigurationBase[] trUnits = getChildObjects();
		boolean seenUnit = false;
		HashMap selectNames = new HashMap();
		String targetSchema = "";
		for (int i = 0; i < trUnits.length; i++){
			TransformationUnit trUnit = (TransformationUnit) trUnits[i];
			targetSchema = trUnit.getTargetSchema();
			if (trUnit.getRefTable().getName().toLowerCase().equals(extraMain.toLowerCase())){
				seenUnit = true;
				selectNames.put(trUnit.RFKey,"1");
			}	
			
			if (!seenUnit) continue;
			
			Column[] cols = trUnit.getRefTable().getColumns();
			for (int j = 0; j < cols.length; j++){
				if (cols[j].hasAlias()){
					selectNames.put(cols[j].getAlias(),"1");
				}
				else{
					selectNames.put(cols[j].getName(),"1");
				}
			}
		}
		
		StringBuffer tempsql = new StringBuffer ("CREATE TABLE "+targetSchema+"."+newTableName);		
		
		String[] selectCols = new String[selectNames.size()];
		selectNames.keySet().toArray(selectCols);
		
		StringBuffer selectClauseBuffer = new StringBuffer("");
		for (int i = 0; i < selectCols.length; i++){
			selectClauseBuffer = selectClauseBuffer.append(selectCols[i] + ",");
		}
		if (selectClauseBuffer.length() > 0)	
			selectClauseBuffer.delete(selectClauseBuffer.length()-1,selectClauseBuffer.length());
		//TransformationUnitDouble finalUnit = ((TransformationUnitDouble) getChildObjects()[getChildObjects().length - 1]);
		tempsql.append("  AS SELECT "+selectClauseBuffer+" FROM "+targetSchema+"."+finalTableName+" GROUP BY "+selectClauseBuffer+";\n");
		
		return tempsql.toString();
		
	}
	
	public String createNewMainBools (String extraMain,String PK, int index){
		
		String[] tableParts = finalTableName.split("__");
		String newTableName = tableParts[0]+"__"+extraMain+"__main";//gene__gene__main
		String targetSchema = "";
		StringBuffer tempsql = new StringBuffer("");	
		ConfigurationBase[] trUnits = getChildObjects();
		for (int i = 0; i < trUnits.length; i++){
			TransformationUnit trUnit = (TransformationUnit) trUnits[i];
			if (!trUnit.getRefTable().getName().matches(".*__dm")){
				continue;
			}	
			targetSchema = trUnit.getTargetSchema();
			String[] dmTableParts = trUnit.getRefTable().getName().split("__");
			String boolColumn = dmTableParts[1]+"_bool";
			//System.out.println("BOOL COL "+boolColumn);
			tempsql.append("CREATE INDEX index"+index+" ON "+targetSchema+"."+newTableName+"("+PK+");\n");		
			index++;
			tempsql.append("CREATE TABLE "+targetSchema+".BOOL_TEMP AS SELECT DISTINCT "+PK+",1 AS "+
				boolColumn+" FROM "+targetSchema+"."+finalTableName+" WHERE "+boolColumn+" IS NOT NULL;\n");
			tempsql.append("CREATE INDEX index"+index+" ON "+targetSchema+".BOOL_TEMP ("+PK+");\n");
			tempsql.append("CREATE TABLE "+targetSchema+".MAIN_TEMP AS SELECT m.*, t."+boolColumn+" FROM "+targetSchema+"."+newTableName+
				" m LEFT JOIN "+targetSchema+".BOOL_TEMP t ON m."+PK+"=t."+PK+";\n");
			index++;
			tempsql.append("DROP TABLE "+targetSchema+".BOOL_TEMP;\n");
			tempsql.append("DROP TABLE "+targetSchema+"."+newTableName+";\n");
			//tempsql.append("ALTER TABLE "+targetSchema+".MAIN_TEMP RENAME AS "+targetSchema+"."+newTableName+";\n");
			tempsql.append("RENAME "+targetSchema+".MAIN_TEMP TO "+targetSchema+"."+newTableName+";\n");
			//tempsql.append("CREATE INDEX"+index+" ON "+targetSchema+"."+newTableName+"("+PK+");\n");	
		}
		tempsql.append("ALTER TABLE "+targetSchema+"."+newTableName+" RENAME COLUMN "+PK+" TO "+PK+"_key;\n");
		tempsql.append("CREATE INDEX index"+index+" ON "+targetSchema+"."+newTableName+"("+PK+"_key);\n");
		index++;
		tempsql.append("ALTER TABLE "+targetSchema+"."+finalTableName+" RENAME COLUMN "+PK+" TO "+PK+"_key;\n");
		tempsql.append("CREATE INDEX index"+index+" ON "+targetSchema+"."+finalTableName+"("+PK+"_key);\n");
		
		return tempsql.toString();
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