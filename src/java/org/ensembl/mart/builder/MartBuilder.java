/*
 * Created on Jun 3, 2004
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

import java.io.*;
import java.util.*;


public class MartBuilder {
	
	private static SourceSchema source_schema;
	private static String config ="data/builder_connection_mysql.properties";
	
	public static void main(String[] args) {
		
		source_schema = new SourceSchema(config);
		String dataset = getUserInput("DATASET: ");
		source_schema.dataset=dataset;
		
		String prompt = "TYPE MAIN [M] DIMENSION [D] EXIT [E]: ";
		String output;
		
		
		// Get source schema info
		do
			output=getUserInput(prompt);
		while (!(output.equals("M") || output.equals("D") || output.equals("E")));
		
		while (output.equals("M") || output.equals("D")){
			String table_name = getUserInput("TABLE NAME: ");
			LinkedTables linked_tables=getLinked(table_name);
			StringBuffer final_table = new StringBuffer(dataset + "__"+table_name+"__");
			if (output.equals("M")){
				linked_tables.final_table_type ="MAIN";
				linked_tables.final_table_name= final_table.append("main").toString();
			} else { 
				linked_tables.final_table_type = "DM";
				linked_tables.final_table_name= final_table.append("dm").toString();
			}
			linked_tables.dataset=dataset;
			
			linked_tables=setColumnsToFinal(linked_tables);
			
			source_schema.addLinkedTables(linked_tables);
			output=getUserInput(prompt);
		}
		
		
		
		// Create Target schema and transformations	
		TargetSchema target_schema = new TargetSchema(source_schema);
		Transformation [] transformations = target_schema.getTransformations();
		
/**		
		// final key
		String key = getUserInput("Final KEY: ");
		
		for (int i=0;i<transformations.length; i++){
			
			Column [] columns = transformations[i].getFinalUnit().getTemp_end().getColumns();
			
			for (int j=0;j<columns.length;j++){
				
				if(columns[j].name.equals(key)){
					columns[j].setAlias(columns[j].name +"_key");
			
				System.out.println("resetting "+ columns[j].name);
				
				}	
			}
		}
		
	*/	
		
		// Include extensions
		LinkedTables [] extlinked = source_schema.getLinkedTables();
		
		for (int i=0;i<extlinked.length;i++){
			String name = extlinked[i].final_table_name;
			String input = getUserInput("ADD EXTENSION: "+name+" [Y|N]: " );
			if (input.equals("Y")){
				
				String card_string=" cardinality [11] [n1] [0n] [1n] [SKIP S]: ";
				
				String final_table_key = getUserInput(name+ " KEY: ");
				String final_table_extension = getUserInput(name+ " EXTENSION: ");
				String new_table_name = getUserInput("EXTENSION TABLE NAME: ");
				String new_table_key = getUserInput("EXTENSION TABLE KEY: ");
				String new_table_extension = getUserInput("EXTENSION TABLE EXTENSION: ");
				String new_table_cardinality = getUserInput(name+": "+new_table_name + card_string);
				
				target_schema.addTransformationUnit(name, new_table_name,final_table_key,final_table_extension,
						new_table_key, new_table_extension, new_table_cardinality);
				
			}
		}
		
		
		// Reset final table names if you want to
		for (int i=0;i<transformations.length;i++){
			
			String newname = getUserInput("CHANGE FINAL TABLE NAME: "+transformations[i].final_table_name+" TO: " );
			if (newname != null && ! newname.equals("\n") && !newname.equals(""))
				transformations[i].setFinalName(newname);
		}
		
		
		
		// Add central filters
		Transformation [] tran = target_schema.getTransformationsByFinalTableType("DM");
		
		for (int i=0;i<tran.length;i++){
			String input = getUserInput("INCLUDE CENTRAL FILTER FOR: "+tran[i].final_table_name+" [Y|N] ");
			if (input.equals("Y")){
				tran[i].central=true;		
			
			//String extension = getUserInput(tran[i].final_table_name+" EXTENSION: ");
			//tran[i].getFinalUnit().getTemp_end().central_extension=extension;
			}
			
		}
		
		target_schema.createTransformationsForCentralFilters();
		
		
		Transformation [] final_transformations = target_schema.getTransformations();
		
		
		// Dump to SQL
		for (int i=0;i<final_transformations.length;i++){
			
			TransformationUnit [] units = final_transformations[i].getUnits();
			
			System.out.println("");
			for (int j=0;j<units.length;j++){
				System.out.println(units[j].toSQL());
				System.out.println(units[j].addIndex());
			}
			for (int j=0;j<units.length;j++){
				System.out.println(units[j].dropTempTable());	    	
			}
		}
	}
	
	
	
	private static  LinkedTables getLinked(String table_name){
		
		Table [] referenced_tables = source_schema.getReferencedTables(table_name);
		ArrayList list = new ArrayList();
		
		String card_string=" cardinality [11] [n1] [0n] [1n] [SKIP S]: ";
		for (int i=0;i<referenced_tables.length; i++){
			String cardinality = "";
			Table ref_tab=referenced_tables[i];
			
			while (!(cardinality.equals("11") || cardinality.equals("n1")
					|| cardinality.equals("0n") || cardinality.equals("1n")
					|| cardinality.equals("S")))
				
			{cardinality = getUserInput(table_name+": "+ref_tab.getName() + card_string);}
			
			if (!cardinality.equals("S")){
				
				ref_tab.setCardinality(cardinality);
				
				if (cardinality.equals("1n")){
					ref_tab.skip= true;
				}
					
				list.add(ref_tab);
			}
		}
		
		Table [] b = new Table [list.size()];	
		LinkedTables linked_tables= source_schema.createLinkedTables(table_name,(Table []) list.toArray(b));
		
		return linked_tables;
	}
	
	
	private static String getUserInput (String prompt ){
		
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print(prompt);
		String cardinality = null;
		
		try {cardinality = console.readLine();}
		catch (IOException e){System.err.println("error: " +e );}; 
		
		return cardinality;
		
	}
	
	
	private static LinkedTables setColumnsToFinal (LinkedTables linked){
		
		Table [] reftables = linked.getReferencedTables();
		for (int i=0;i<reftables.length;i++){
			Column [] columns = reftables[i].getColumns();
			for (int j=0;j<columns.length;j++){

			}
		}
		return linked;
	}
}




