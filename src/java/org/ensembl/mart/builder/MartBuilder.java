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
	private static TargetSchema target_schema;
	private static String config ="data/builder_connection_oracle.properties";
	
	public static void main(String[] args) {
		
		source_schema = new SourceSchema(config);
		
		String prompt = "MAIN [Y|N]: ";	
		if (getUserInput(prompt).equals("Y")){	
			
			String table_name = getUserInput("TABLE NAME: ");
			processTables(table_name);	
		}			
		
	
		Transformation [] transformations = getTarget_schema().getTransformations();
		for (int i=0;i<transformations.length;i++){
			TransformationUnit [] units = transformations[i].getUnits();
			
			System.out.println("");
			for (int j=0;j<units.length;j++){
				System.out.println(units[j].toSQL());	    	
			}
		}
	}
	
	
	
	private static void processTables(String table_name){
		
		LinkedTables linked_tables=linkTables(table_name);
		
		TargetSchema target_schema = new TargetSchema();
		ArrayList dimensions = new ArrayList();
		for (int j=0;j<source_schema.getLinkedTables().length;j++){
			Transformation final_table = new Transformation(source_schema.getLinkedTables()[j]);	
			dimensions.addAll(final_table.unwanted);
		}
		
		
		String prompt = "DIMENSION [Y|N]: ";
		
		if (getUserInput(prompt).equals("Y")){	
			
			for (int i=0;i<dimensions.size();i++){
				
				String dim = (String) dimensions.get(i).toString();
				linked_tables=linkTables(dim);
		
				for (int j=0;j<source_schema.getLinkedTables().length;j++){
					Transformation final_table = new Transformation(source_schema.getLinkedTables()[j]);	
					target_schema.addTransformation(final_table);
				}		
			}
		}  
	
		setTarget_schema(target_schema);
	}
	
	
	
	private static  LinkedTables linkTables(String table_name){
		
		Table [] key_tables; 
		
		key_tables = source_schema.getKeyTables(table_name);
		
		
		String card_string=" cardinality [11] [n1] [0n] [1n]: ";
		for (int i=0;i<key_tables.length; i++){
			String cardinality = "";
			while (!(cardinality.equals("11") || cardinality.equals("n1")
					|| cardinality.equals("0n") || cardinality.equals("1n"))) 
			{cardinality = getUserInput(table_name+": "+key_tables[i].getName() + card_string);}
			
			key_tables[i].setCardinality(cardinality);
		}
		
		LinkedTables linked_tables= source_schema.createLinkedTables(table_name,key_tables);
		
		/**
		 if(getUserInput("Additional Tables [Y|N]: ").equals("Y")){
		 
		 String name= getUserInput("Table name: ");
		 String key=getUserInput("Table key: ");
		 String extension=getUserInput("SQL Extension: ");
		 String cardinality = getUserInput(table_name+": "+name + card_string);
		 linked_tables=source_schema.addTableToLink(name,key,extension,cardinality,linked_tables);
		 }
		 */
		
		source_schema.addLinkedTables(linked_tables);
		return linked_tables;
	}
	/**	
	 private static void classifyTables(LinkedTables linked_tables){
	 
	 String dim_prompt= " Dimension [D] Next Main [M]: ";
	 for (int i=0; i<linked_tables.getReferencedTables().length;i++){
	 if(linked_tables.getReferencedTables()[i].getCardinality().equals("1n")){
	 
	 String next_main=linked_tables.getReferencedTables()[i].getName();
	 String input = getUserInput (next_main+ dim_prompt);
	 
	 if (input.equals("D")){
	 linked_tables =	linkTables(next_main,"D");
	 source_schema.addLinkedTables(linked_tables);
	 } else if (input.equals("M")){
	 linked_tables =	linkTables(next_main,"M");
	 source_schema.addLinkedTables(linked_tables);
	 }
	 }	
	 }	
	 }
	 */
	
	private static String getUserInput (String prompt ){
		
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print(prompt);
		String cardinality = null;
		
		try {cardinality = console.readLine();}
		catch (IOException e){System.err.println("error: " +e );}; 
		
		return cardinality;
		
	}
	
	/**
	 * @return Returns the target_schema.
	 */
	public static TargetSchema getTarget_schema() {
		return target_schema;
	}
	/**
	 * @param target_schema The target_schema to set.
	 */
	public static void setTarget_schema(TargetSchema target_schema) {
		MartBuilder.target_schema = target_schema;
	}
}




