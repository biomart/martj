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
	private static String config ="data/builder_connection_oracle.properties";
	
	public static void main(String[] args) {
		
		source_schema = new SourceSchema(config);
	    String dataset = getUserInput("DATASET: ");
	    source_schema.dataset=dataset;
	    
		String prompt = "TYPE MAIN [M] DIMENSION [D] EXIT [E]: ";
		String output;
		
		do
				output=getUserInput(prompt);
		while (!(output.equals("M") || output.equals("D") || output.equals("E")));
	
			while (output.equals("M") || output.equals("D")){
				LinkedTables linked_tables=setCardinality(getUserInput("TABLE NAME: "));
				if (output.equals("M")){
					linked_tables.type = "MAIN";
				} else { linked_tables.type = "DM";}
				linked_tables.dataset=dataset;
				source_schema.addLinkedTables(linked_tables);
				output=getUserInput(prompt);
			}
		
		
		TargetSchema target_schema = new TargetSchema(source_schema);
		
		Transformation [] transformations = target_schema.getTransformations();
		
		for (int i=0;i<transformations.length;i++){
			TransformationUnit [] units = transformations[i].getUnits();
			
			System.out.println("");
			for (int j=0;j<units.length;j++){
				System.out.println(units[j].toSQL());	    	
			}
			for (int j=0;j<units.length;j++){
				System.out.println(units[j].dropTempTable());	    	
			}
		}
	}
	
	
	
	private static  LinkedTables setCardinality(String table_name){
		
		Table [] key_tables; 
		
		key_tables = source_schema.getKeyTables(table_name);
		
		ArrayList list = new ArrayList();
		
		String card_string=" cardinality [11] [n1] [0n] [1n] [SKIP S]: ";
		for (int i=0;i<key_tables.length; i++){
			String cardinality = "";
			Table key_tab=key_tables[i];
			
			while (!(cardinality.equals("11") || cardinality.equals("n1")
					|| cardinality.equals("0n") || cardinality.equals("1n")
					|| cardinality.equals("S")))
				
			{cardinality = getUserInput(table_name+": "+key_tab.getName() + card_string);}
			
			if (!cardinality.equals("S")){
				key_tab.setCardinality(cardinality);
				list.add(key_tab);
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
	
	
}




