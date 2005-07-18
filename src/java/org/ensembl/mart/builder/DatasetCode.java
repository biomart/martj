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


public class DatasetCode {
	
	ArrayList transformations = new ArrayList();
	String name;
	String targetSchemaName;
	String datasetKey;
	DatabaseAdaptor adaptor;
	
	
		
		public DatasetCode(){
		
	}
	
		
		// the below is needed to make sure that 
		// every main table has the same columns
		// needs more thinking for independent mains
				
	/**
	
	public void createTransformationsForMains(){
		
		TransformationCode [] trans = getTransformations();
		ArrayList mains = new ArrayList();
		
		for (int i=0;i<trans.length;i++){
			if (trans[i].finalTableType.equals("MAIN")){
				Table main = trans[i].getFinalUnit().getTemp_end();
				
				System.out.println ("transformation number frm dataset "+trans[i].number);
				
				//transformationKey=trans[i].getFinalUnit().getTemp_end().key;
				//System.out.println(trans[i].getFinalUnit().getTemp_end());
				datasetKey=trans[i].getFinalUnit().getTemp_end().PK;
				
				
				mains.add(main);
			}
		}
		
		
		ArrayList ref_tables = new ArrayList();
		for (int i=1;i<mains.size();i++){
			
            Table main = (Table) mains.get(i);
            Table ref = (Table) mains.get(i);
            ref_tables.add(ref);
            Table [] b = new Table [ref_tables.size()];
            Table [] tables = (Table []) ref_tables.toArray(b);
            
            
            TransformationCode transformation = new TransformationCode();
            transformation.adaptor=adaptor;
            transformation.datasetName=name;
            transformation.targetSchemaName=targetSchemaName;
            transformation.finalTableName =ref.getName();
            
            ref.setName(ref.temp_name);
            transformation.startTable=main;
		    transformation.type="main";
		    transformation.finalTableType = "MAIN";
		    
		    transformation.column_operations="append";
            transformation.createUnits(tables);
            
            transformation.transform();
            addTransformation(transformation);
		
		}		
	}
	
	
	*/
	
	public void createTransformationsForCentralFilters(){
		
		TransformationCode [] dmTransformations = getDMTranformationsForCentral();	
		TransformationCode [] mainTransformations =   getMainTranformationForCentral();
		
		for (int i=0; i<mainTransformations.length;i++){
			
			TransformationCode transformation = new TransformationCode();
			
			transformation.adaptor=adaptor;
			transformation.datasetName=name;
			transformation.targetSchemaName=targetSchemaName;
			transformation.userTableName=mainTransformations[i].userTableName;
			 
			 // get final temp for each main tablev
			
			//System.out.println(" transfromation form central "+mainTransformations[i].finalTableName);
			
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
				
				//System.out.println(" temp end name from central transf "+dmFinalTable.getName());
				
				
			TUnitSingle sunit = 
				new TUnitSingle(dmFinalTable);
			
			sunit.single = true;
			sunit.adaptor = adaptor;
			sunit.targetSchema = targetSchemaName;
			sunit.TSKey=dmTransformations[m].getFinalUnit().TSKey;
			sunit.RFKey=dmTransformations[m].getFinalUnit().RFKey;
			sunit.type="notNull";
			transformation.addUnit(sunit);
			
			

			TUnitDouble dunit = new TUnitDouble(dmFinalTable);
			dunit.cardinality = dmFinalTable.cardinality;
			dunit.column_operations = "addone";
			dunit.final_table_name = "MAIN";
			dunit.adaptor = adaptor;
			dunit.TSKey=dmTransformations[m].getFinalUnit().RFKey;
			dunit.RFKey=dmTransformations[m].getFinalUnit().TSKey;
			
			
			dunit.targetSchema = targetSchemaName;
			transformation.addUnit(dunit);
				
		}

			// resetting the name to temp name
			if (containsCentral) main_table.setName("main_interim");
			
			transformation.transform();
			addTransformation(transformation);
		}
			
	}
	
	
	/**
	public void addTransformationUnit(String final_table_name,String new_table_name,String final_table_key,String final_table_extension,
									  String new_table_key, String new_table_extension, String new_table_cardinality){
		
		TransformationCode trans = getTransformationByFinalName(final_table_name);
		Column [] columns = sourceSchema.getTableColumns(new_table_name);
		
		Table reftable = new Table();
		reftable.setName(new_table_name);
		reftable.setColumns(columns);
		reftable.setName(new_table_name);	
		reftable.setKey(new_table_key);
		reftable.setExtension(new_table_extension);
		reftable.setCardinality(new_table_cardinality);	
		
		trans.addAdditionalUnit(reftable,final_table_key,final_table_extension);
		// redo the transformation
		trans.transform();
		
	}
	
	*/
	
	
	/**
	private void transform(){
		
		TransformationCode [] trans = getTransformations();
		
		for (int i=0;i<trans.length;i++){
			trans[i].transform();
		}
	}
	*/
	
	
	
	public void transform(){
		
		
		TransformationCode[] transformations = getTransformations();
		
		for (int i = 0; i < transformations.length; i++) {
				
		transformations[i].transform();
			
		}
		
		setUserTableNames();
	    createTransformationsForCentralFilters();
		
	}
	
	
		
	public TransformationCode [] getTransformations() {
		
		TransformationCode [] b = new TransformationCode[transformations.size()];
	
		//setFinalNames();
		//return transforms;
		return (TransformationCode []) transformations.toArray(b);	
		
	}
	
	
	public void setUserTableNames(){
		
		TransformationCode [] b = new TransformationCode[transformations.size()];
        TransformationCode [] transforms = (TransformationCode []) transformations.toArray(b);
		
		for (int i = 0; i < transforms.length; i++) { 
			
			
			// temp comment out.
			
			transforms[i].getFinalUnit().getTemp_end().setName(transforms[i].userTableName);
		
			
		//System.out.println(" setting name "+transforms[i].number+ " to "+transforms[i].userTableName);
		
		}
		
	}
	
	
	
	
	public void addTransformation(TransformationCode transformation){
		this.transformations.add(transformation);
			
	}
	
	
	private TransformationCode getTransformationByFinalName(String name){
		
		TransformationCode trans = new TransformationCode();
		
		for (int i=0;i<transformations.size();i++){
			trans = (TransformationCode) transformations.get(i);
			if (trans.finalTableName.equals(name)){
			break;
			}
		}
		
		return trans;
	}
	

	public TransformationCode [] getTransformationsByFinalTableType(String type){
		
		ArrayList trans_list = new ArrayList();
		
		TransformationCode [] trans = getTransformations();
		
		for (int i=0;i<trans.length;i++){
			
			//System.out.println("gettintg tpe "+type);
			
			if (trans[i].finalTableType.equals(type)){
				trans_list.add(trans[i]);
			}
		}
		
		TransformationCode [] b = new TransformationCode[trans_list.size()];
		return (TransformationCode []) trans_list.toArray(b);	
		
	}	
	
	
	private TransformationCode [] getDMTranformationsForCentral (){
		
		ArrayList list = new ArrayList();
		
		TransformationCode [] trans= getTransformationsByFinalTableType("DM");
		for (int i=0; i<trans.length;i++){
			
			if (trans[i].central){
				list.add(trans[i]);			
			}
		}
		TransformationCode [] b = new TransformationCode[list.size()];
		return (TransformationCode []) list.toArray(b);	
		
	}
	
	private TransformationCode [] getMainTranformationForCentral(){
		
		TransformationCode [] mains = getTransformationsByFinalTableType("MAIN");
		
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
		
		TransformationCode [] b = new TransformationCode[list.size()];
		return (TransformationCode []) list.toArray(b);		
	}
	
	
	
}