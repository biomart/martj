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

public class MartBuilder {
	
	private static SourceSchema source_schema;
	private static String config ="data/builder_connection_oracle.properties";
	
	public static void main(String[] args) {
		
		source_schema = new SourceSchema(config);
		String main_name = getUserInput("MAIN TABLE: ");
		LinkedTables linked_tables=linkTables(main_name);
		classifyTables(linked_tables);
		
		
		TargetSchema target_schema = new TargetSchema();
		for (int j=0;j<source_schema.getLinkedTables().length;j++){
			Transformation transformation = new Transformation(source_schema.getLinkedTables()[j]);	
			target_schema.addTransformation(transformation);
		}
		
		Transformation [] transformations = target_schema.getTransformations();
		for (int i=0;i<transformations.length;i++){
			TransformationUnit [] units = transformations[i].getUnits();
			
			System.out.println("");
			for (int j=0;j<units.length;j++){
				System.out.println(units[j].toSQL());	    	
			}
		}
	}
	
	
	private static LinkedTables linkTables(String main_name){
		
		Table [] exp_key_tables = source_schema.getExportedKeyTables(main_name);
		
		String card_string=" cardinality [11] [n1] [0n] [1n] [mn] : ";
		for (int i=0;i<exp_key_tables.length; i++){
			String cardinality = getUserInput(main_name+": "+exp_key_tables[i].getName() + card_string);
			
			exp_key_tables[i].setCardinality(cardinality);
		}
		
		LinkedTables linked_tables= source_schema.createLinkedTables(main_name,exp_key_tables);
		
		if(getUserInput("Additional dimensions [Y|N]: ").equals("Y")){
			
			Table table = new Table();
			table.setName(getUserInput("Table name: "));
			table.setKey(getUserInput("Table key: "));
			table.setExtension(getUserInput("SQL Extension: "));
			String cardinality = getUserInput(main_name+": "+table.getName() + card_string);
			table.setCardinality(cardinality);
			linked_tables=source_schema.addDimension(table,linked_tables);
			
		}
		
		source_schema.addLinkedTables(linked_tables);
		return linked_tables;
	}
	
	private static void classifyTables(LinkedTables linked_tables){
		
		String dim_prompt= " Dimension [D] Next Main [M]: ";
		for (int i=0; i<linked_tables.getReferencedTables().length;i++){
			if(linked_tables.getReferencedTables()[i].getCardinality().equals("1n")){
				
				String next_main=linked_tables.getReferencedTables()[i].getName();
				String input = getUserInput (next_main+ dim_prompt);
				
				if (input.equals("D")){
					linked_tables =	linkTables(next_main);
					source_schema.addLinkedTables(linked_tables);
				} else if (input.equals("M")){
					linked_tables =	linkTables(next_main);
					source_schema.addLinkedTables(linked_tables);
				}
			}	
		}	
	}
	
	
	private static String getUserInput (String prompt ){
		
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print(prompt);
		String cardinality = null;
		
		try {cardinality = console.readLine();}
		catch (IOException e){System.err.println("error: " +e );}; 
		
		return cardinality;
		
	}
	
}




