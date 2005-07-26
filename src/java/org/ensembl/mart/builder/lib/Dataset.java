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
	
	String targetSchemaName;
	String datasetKey;
	DatabaseAdaptor adaptor;
	
	public Dataset (Element element){
		super(element);
	}
		
	public Dataset (){
		super();
	}
	
	
	public void createTransformationsForCentralFilters(){
		
		Transformation [] dmTransformations = getDMTranformationsForCentral();	
		Transformation [] mainTransformations =   getMainTranformationForCentral();
		
		for (int i=0; i<mainTransformations.length;i++){
			
			Transformation transformation = new Transformation();
			
			transformation.adaptor=adaptor;
			transformation.datasetName=getElement().getAttributeValue("internalName");
			transformation.targetSchemaName=targetSchemaName;
			transformation.getElement().setAttribute("userTableName",
				mainTransformations[i].getElement().getAttributeValue("userTableName"));
		
			Table main_table=mainTransformations[i].getFinalUnit().getTemp_end();
			// set this again
			main_table.type="interim";
			
			transformation.finalTableName=main_table.getName();
			transformation.finalTableType="MAIN";
			
			
			transformation.startTable=main_table;
			transformation.type="central";
			
			boolean containsCentral=false;
			for (int m = 0; m < dmTransformations.length; m++) {
				
			    if(dmTransformations[m].central) containsCentral=true; 
				
				Table dmFinalTable=dmTransformations[m].getFinalUnit().getTemp_end();
				
				TransformationUnitSingle sunit = 
					new TransformationUnitSingle(dmFinalTable);
			
				sunit.single = true;
				sunit.adaptor = adaptor;
				sunit.targetSchema = targetSchemaName;
				sunit.TSKey=dmTransformations[m].getFinalUnit().TSKey;
				sunit.RFKey=dmTransformations[m].getFinalUnit().RFKey;
				sunit.type="notNull";
				transformation.addChildObject(sunit);
			
			

				TransformationUnitDouble dunit = new TransformationUnitDouble(dmFinalTable);
				dunit.getElement().setAttribute("cardinality",dmFinalTable.getCardinality());
				dunit.column_operations = "addone";
				dunit.final_table_name = "MAIN";
				dunit.adaptor = adaptor;
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
			
			if (((Transformation)trans[i]).finalTableType.equals(type)){
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
			
			if(name.equals(mains[i].finalTableName)){
				list.set(i-1,mains[i]);		
			} else {
				name = mains[i].finalTableName;
				list.add(mains[i]);
			}
		}
		
		Transformation [] b = new Transformation[list.size()];
		return (Transformation []) list.toArray(b);		
	}
	
	
	
}