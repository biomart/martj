 /*
 * Created on Jun 14, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import java.util.*;

import org.jdom.Element;


/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 * @author <a href="mailto: damian@ebi.ac.uk">Damian Smedley</a>
 * 
 */


public class Dataset extends ConfigurationBase {
	
	private String targetSchemaName;
	private String datasetKey;
	private String RDBMS;
	
	/**
	 * @return Returns the rDBMS.
	 */
	public String getRDBMS() {
		return RDBMS;
	}
	/**
	 * @param rdbms The rDBMS to set.
	 */
	public void setRDBMS(String rdbms) {
		RDBMS = rdbms;
	}
	public Dataset (Element element){
		super(element);
		int[] requiredFields = {0,1};
		setRequiredFields(requiredFields);
	}
		
	public Dataset(){
		super("Dataset");
		int[] requiredFields = {0,1};
		setRequiredFields(requiredFields);
	}
	
	void setTargetSchemaName(String targetSchemaName) {
	  this.targetSchemaName = targetSchemaName;
	}

	String getTargetSchemaName() {
	  return targetSchemaName;
	}
	
	void setDatasetKey(String datasetKey) {
	  this.datasetKey = datasetKey;
	}

	String getDatasetKey() {
	  return datasetKey;
	}
	
	
	
	public void createTransformationsForCentralFilters(){
		
		Transformation [] dmTransformations = getDMTranformationsForCentral();	
		Transformation [] mainTransformations =   getMainTranformationForCentral();
		
		for (int i=0; i<mainTransformations.length;i++){
			
			Transformation transformation = new Transformation();
			transformation.setDatasetName(getElement().getAttributeValue("internalName"));
			transformation.setTargetSchemaName(targetSchemaName);
			transformation.getElement().setAttribute("userTableName",
				mainTransformations[i].getElement().getAttributeValue("userTableName"));
		
			Table main_table=mainTransformations[i].getFinalUnit().getTemp_end();
			// set this again
			main_table.type="interim";
			
			transformation.setFinalTableName(main_table.getName());
			transformation.setFinalTableType("MAIN");
			
			
			transformation.setStartTable(main_table);
			transformation.setType("central");
			
			boolean containsCentral=false;
			for (int m = 0; m < dmTransformations.length; m++) {
				
			    if(dmTransformations[m].central) containsCentral=true; 
				
				Table dmFinalTable=dmTransformations[m].getFinalUnit().getTemp_end();
				
				TransformationUnitSingle sunit = 
					new TransformationUnitSingle(dmFinalTable);
			
				sunit.single = true;
				sunit.setRDBMS(getRDBMS());
				sunit.targetSchema = targetSchemaName;
				sunit.TSKey=dmTransformations[m].getFinalUnit().TSKey;
				sunit.RFKey=dmTransformations[m].getFinalUnit().RFKey;
				sunit.type="notNull";
				transformation.addChildObject(sunit);
			
			

				TransformationUnitDouble dunit = new TransformationUnitDouble(dmFinalTable);
				dunit.getElement().setAttribute("cardinality",dmFinalTable.getCardinality());
				dunit.setColumnOperations("addone");
				dunit.setfinalTableName("MAIN");
				dunit.setRDBMS(getRDBMS());
				dunit.TSKey=dmTransformations[m].getFinalUnit().RFKey;
				dunit.RFKey=dmTransformations[m].getFinalUnit().TSKey;
			
			
				dunit.targetSchema = targetSchemaName;
				transformation.addChildObject(dunit);
				
			}

			// resetting the name to temp name
			if (containsCentral) main_table.setName("main_interim");
			
			transformation.transform();
			addChildObject(transformation);
		}
			
	}
	
	
	public void transform(){
		
		
		ConfigurationBase[] transformations = getChildObjects();
		
		for (int i = 0; i < transformations.length; i++) {
				
			((Transformation) transformations[i]).transform();
			
		}
		
		setUserTableNames();
	    createTransformationsForCentralFilters();
		
	}
	
	
	public void setUserTableNames(){
		
		ConfigurationBase[] transforms = getChildObjects();
		
		for (int i = 0; i < transforms.length; i++) { 
			
			Transformation trans = (Transformation) transforms[i];
			trans.getFinalUnit().getTemp_end().setName(trans.getElement().getAttributeValue("userTableName"));
		}
		
	}
	

	public Transformation [] getTransformationsByFinalTableType(String type){
		
		ArrayList trans_list = new ArrayList();
		
		ConfigurationBase [] trans = getChildObjects();
		
		for (int i=0;i<trans.length;i++){
			
			if (((Transformation)trans[i]).getFinalTableType().equals(type)){
				trans_list.add(trans[i]);
			}
		}
		
		Transformation [] b = new Transformation[trans_list.size()];
		return (Transformation []) trans_list.toArray(b);	
		
	}	
	
	
	private Transformation [] getDMTranformationsForCentral (){
		
		ArrayList list = new ArrayList();
		
		Transformation [] trans= getTransformationsByFinalTableType("DM");
		for (int i=0; i<trans.length;i++){
			
			if (trans[i].central){
				list.add(trans[i]);			
			}
		}
		Transformation [] b = new Transformation[list.size()];
		return (Transformation []) list.toArray(b);	
		
	}
	
	private Transformation [] getMainTranformationForCentral(){
		
		Transformation [] mains = getTransformationsByFinalTableType("MAIN");
		
		ArrayList list = new ArrayList();
		String name = "";
		
		for (int i=0;i<mains.length;i++){
			
			if(name.equals(mains[i].getFinalTableName())){
				list.set(i-1,mains[i]);		
			} else {
				name = mains[i].getFinalTableName();
				list.add(mains[i]);
			}
		}
		
		Transformation [] b = new Transformation[list.size()];
		return (Transformation []) list.toArray(b);		
	}
	
	
	
	
}