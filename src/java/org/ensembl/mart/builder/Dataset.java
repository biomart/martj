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


public class Dataset {
	
	ArrayList transformations = new ArrayList();
	String name;
	String targetSchemaName;
	String transformationKey;
	private LinkedTables [] linkedTables;
	private DBAdaptor adaptor;
	
	public Dataset (ArrayList linkedList, String name,String targetSchemaName, DBAdaptor adaptor){
		
		LinkedTables[] c = new LinkedTables[linkedList.size()];
		LinkedTables [] linkies= (LinkedTables[]) linkedList.toArray(c);
		
		this.linkedTables=linkies;
		this.name=name;
		this.targetSchemaName=targetSchemaName;
		this.adaptor=adaptor;
		createTransformationsForLinked();
		createTransformationsForMains();
		//createTransformationsForCentralFilters();
		
	}
	
	
	
	private void createTransformationsForLinked(){
		
		for (int j=0;j<linkedTables.length;j++){
			
			LinkedTables linked = linkedTables[j];
		    Table [] referenced_tables = linked.getReferencedTables();
			
		    Transformation transformation = new Transformation();
		    transformation.adaptor=adaptor;
		    transformation.datasetName=name;
		    transformation.targetSchemaName=targetSchemaName;
		    
		    //System.out.println("adding name for linked "+name);
		    
			transformation.finalTableType=linked.final_table_type;
			transformation.finalTableName=linked.final_table_name;
			transformation.startTable=linked.getCentralTable();
			transformation.type="linked";
			transformation.finalTableType=linked.final_table_type;
			
			transformation.column_operations="addall";
			transformation.create(referenced_tables);
			
			transformation.transform();
			addTransformation(transformation);
		    
			}
	}
	
	private void createTransformationsForMains(){
		
		Transformation [] trans = getTransformations();
		ArrayList mains = new ArrayList();
		
		for (int i=0;i<trans.length;i++){
			if (trans[i].finalTableType.equals("MAIN")){
				Table main = trans[i].getFinalUnit().getTemp_end();
				
				//transformationKey=trans[i].getFinalUnit().getTemp_end().key;
				transformationKey=trans[i].getFinalUnit().getTemp_end().PK;
				
				
				mains.add(main);
			}
		}
		
		
		ArrayList ref_tables = new ArrayList();
		for (int i=1;i<mains.size();i++){
			
            Table main = (Table) mains.get(0);
            Table ref = (Table) mains.get(i);
            ref_tables.add(ref);
            Table [] b = new Table [ref_tables.size()];
            Table [] tables = (Table []) ref_tables.toArray(b);
            
            
            Transformation transformation = new Transformation();
            transformation.adaptor=adaptor;
            transformation.datasetName=name;
            transformation.targetSchemaName=targetSchemaName;
            transformation.finalTableName =ref.getName();
            
            ref.setName(ref.temp_name);
            transformation.startTable=main;
		    transformation.type="main";
		    transformation.finalTableType = "MAIN";
		    
		    transformation.column_operations="append";
            transformation.create(tables);
            
            transformation.transform();
            addTransformation(transformation);
		
		}		
	}
	
	
	
	
	public void createTransformationsForCentralFilters(){
		
		Transformation [] central = getDMTranformationsForCentral();	
		Table [] central_tables = new Table [central.length];
		
		for (int j=0;j<central.length;j++){
			central_tables[j]=central[j].getFinalUnit().getTemp_end();
			
		}
		
		Transformation [] mains =   getMainTranformationForCentral();
		
		for (int i=0; i<mains.length;i++){
			Transformation transformation = new Transformation();
			
			transformation.adaptor=adaptor;
			 transformation.datasetName=name;
			 transformation.targetSchemaName=targetSchemaName;
			 //System.out.println("adding name for central "+name);
			 
			Table main_table=mains[i].getFinalUnit().getTemp_end();
			
			transformation.finalTableName=main_table.getName(); 
			main_table.setName(main_table.temp_name);
			
			transformation.startTable=main_table;
			transformation.type="central";
			
			transformation.column_operations="addone";
			transformation.create(central_tables);
			transformation.transform();
			addTransformation(transformation);
		}
			
	}
	
	
	/**
	public void addTransformationUnit(String final_table_name,String new_table_name,String final_table_key,String final_table_extension,
									  String new_table_key, String new_table_extension, String new_table_cardinality){
		
		Transformation trans = getTransformationByFinalName(final_table_name);
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
		
		Transformation [] trans = getTransformations();
		
		for (int i=0;i<trans.length;i++){
			trans[i].transform();
		}
	}
	*/
	
		
	public Transformation [] getTransformations() {
		
		Transformation [] b = new Transformation[transformations.size()];
		return (Transformation []) transformations.toArray(b);	
		
	}
	
	public void addTransformation(Transformation transformation){
		this.transformations.add(transformation);
			
	}
	
	
	private Transformation getTransformationByFinalName(String name){
		
		Transformation trans = new Transformation();
		
		for (int i=0;i<transformations.size();i++){
			trans = (Transformation) transformations.get(i);
			if (trans.finalTableName.equals(name)){
			break;
			}
		}
		
		return trans;
	}
	

	public Transformation [] getTransformationsByFinalTableType(String type){
		
		ArrayList trans_list = new ArrayList();
		
		Transformation [] trans = getTransformations();
		
		for (int i=0;i<trans.length;i++){
			if (trans[i].finalTableType.equals(type)){
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