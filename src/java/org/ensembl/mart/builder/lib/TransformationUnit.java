/*
 * Created on Jun 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import org.jdom.Element;


/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 * @author <a href="mailto: arek@ebi.ac.uk">Damian Smedley</a>
 * 
 */
public  class TransformationUnit extends ConfigurationBase {
	
	protected String tempEndName;
	protected Table tempStart;
	protected Table tempEnd;
	protected Table refTable;
	protected String TSKey;
	protected String RFKey;
	protected String targetSchema;
	protected String type;
	protected String RDBMS;
	protected String columnOperations;

	private int[] requiredFields = {0,1,2,3,4,7};
	boolean single =false;
	
	public TransformationUnit (Element element){
		super(element);
		setRequiredFields(requiredFields);
	}
		
	public TransformationUnit (){
		super();
		setRequiredFields(requiredFields);
	}
	
	public TransformationUnit(Table ref_table){	
		this.refTable=ref_table;
	}

	public TransformationUnit(String internalName,String referencingType,String primaryKey,String referencedTable,
		String cardinality,String centralProjection,String referencedProjection,String foreignKey,
		String referenceColumnNames, String referenceColumnAliases,String centralColumnNames, String centralColumnAliases,
		String externalSchema, String distinct){
		super();
		element.setAttribute("internalName",internalName);
		element.setAttribute("referencingType",referencingType);
		element.setAttribute("primaryKey",primaryKey);
		element.setAttribute("referencedTable",referencedTable);
		element.setAttribute("cardinality",cardinality);
		element.setAttribute("centralProjection",centralProjection);
		element.setAttribute("referencedProjection",referencedProjection);
		element.setAttribute("foreignKey",foreignKey);
		element.setAttribute("referenceColumnNames",referenceColumnNames);
		element.setAttribute("referenceColumnAliases",referenceColumnAliases);
		element.setAttribute("centralColumnNames",centralColumnNames);
		element.setAttribute("centralColumnAliases",centralColumnAliases);
		element.setAttribute("externalSchema",externalSchema);
		element.setAttribute("distinct",distinct);
			
		setRequiredFields(requiredFields);
	}

	void setTempStart(Table tempStart) {
	  this.tempStart = tempStart;
	}

	Table getTempStart() {
	  return tempStart;
	}
	
	void setTempEnd(Table tempEnd) {
	  this.tempEnd = tempEnd;
	}

	Table getTempEnd() {
	  return tempEnd;
	}

	void setRefTable(Table refTable) {
	  this.refTable = refTable;
	}

	Table getRefTable() {
	  return refTable;
	}

	void setTargetSchema(String targetSchema) {
	  this.targetSchema = targetSchema;
	}

	String getTargetSchema() {
	  return targetSchema;
	}
	
	void setType(String type) {
	  this.type = type;
	}

	String getType() {
	  return type;
	}
	
	void setTempEndName(String tempEndName) {
	  this.tempEndName = tempEndName;
	}

	String getTempEndName() {
	  return tempEndName;
	}
	
	void setColumnOperations(String columnOperations) {
	  this.columnOperations = columnOperations;
	}

	String getColumnOperations() {
	  return columnOperations;
	}
		
	void setTSKey(String TSKey) {
	  this.TSKey = TSKey;
	}

	String getTSKey() {
	  return TSKey;
	}
	
	void setRFKey(String RFKey) {
	  this.RFKey = RFKey;
	}

	String getRFKey() {
	  return RFKey;
	}

	String getRDBMS() {
		return RDBMS;
	}

	void setRDBMS(String rdbms) {
		RDBMS = rdbms;
	}
	
	public  String toSQL (){
		String sql="override this method";
		return sql;
		
	}
	
	public  void transform (Table temp_start, String temp_end_name){
	}
	
	public String dropTempTable (){		
		String sql="";
		if (!tempEnd.isFinalTable == true)
			sql = "DROP TABLE "+ targetSchema+"."+tempEnd.getName()+";";
		return sql;	
	}
		
	public String addIndex(int i){
		
		String sql = "";
		
		if (getRDBMS().equals("postgresql"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempStart.getName()+" ("+TSKey+");";
		else if 	(getRDBMS().equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+tempStart.getName()+" ADD INDEX ("+TSKey+");";
		else if (getRDBMS().equals("oracle"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempStart.getName()+" ("+TSKey+");";
		
		return sql;
		
	}
	
	public String addFinalIndex(int i, String key){
		
		String sql = "";
		
		if (getRDBMS().equals("postgresql"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempEnd.getName()+" ("+key+");";
		else if 	(getRDBMS().equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" ADD INDEX ("+key+");";
		else if (getRDBMS().equals("oracle"))
			sql = "CREATE INDEX index"+i+" ON "+targetSchema+"."+tempEnd.getName()+" ("+key+");";
		
		return sql;
		
	}
	
	
	public String renameKeyColumn(String key){
		
		String sql = "";
		
		if (getRDBMS().equals("postgresql"))
			sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" RENAME "+TSKey+ " TO "+ key+"_key;";
		else if 	(getRDBMS().equals("mysql"))
		sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" CHANGE "+TSKey+ " "+ key+"_key INT;";
		else if (getRDBMS().equals("oracle"))
			sql = "ALTER TABLE "+targetSchema+"."+tempEnd.getName()+" RENAME COLUMN "+TSKey+ " TO "+ key+"_key;";
		
		return sql;
		
	}
	
	
	
	protected Table copyTable(Table old_table){
		
		Table new_table= new Table();
		
		try {
			new_table = (Table) old_table.clone();
		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}	
		
		return new_table;
	}
	
}
