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
	
	
	private static String config ="data/builder_connection_mysql.properties";
	private static ArrayList schemas = new ArrayList();
	private static TargetSchema target_schema = null;
	
	
	
	
	
	public static void main(String[] args) {
		
		
		
		
		String config_info= "";
		
		while (! (config_info.equals("C") || config_info.equals("R"))){
			config_info=getUserInput("Configuration Create [C] Read [R]: ");
		}
		
		if (config_info.equals("C")){
			String output_file=getUserInput("OUTPUT CONFIG FILE: ");
			output_file = "data/"+output_file;
			
			File f = new File(output_file);
			f.delete();
			
			String user_dataset = getUserInput("NEW DATASET: ");
			
			while (!(user_dataset == null || user_dataset.equals(""))){
				
				createConfiguration(user_dataset,output_file);
				user_dataset = getUserInput("NEW DATASET: ");
				
			} 
			
			readConfiguration(output_file);
			
		} else {
			String input_file=getUserInput("INPUT CONFIG FILE: ");
			input_file = "data/"+input_file;
			readConfiguration(input_file);
		}
		
		
		
		for (int m=0;m<schemas.size();m++){
			
			target_schema = (TargetSchema) schemas.get(m);
			
			
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
			
			// Include extensions
			LinkedTables [] extlinked = s_schema.getLinkedTables();
			
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
			*/
			
			
			// Reset final table names if you want to
			
			Transformation [] transformations = target_schema.getTransformations();
			
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
		
	}		
	
	
	
	
	
	
	
	private static void createConfiguration(String user_dataset,String output_file){
		
		
		String prompt = "TYPE MAIN [M] DIMENSION [D] EXIT [E]: ";
		String table_type;
		
		do
			table_type=getUserInput(prompt);
		while (!(table_type.equals("M") || table_type.equals("D") || table_type.equals("E")));
		
		while (table_type.equals("M") || table_type.equals("D")){
			String table_name = getUserInput("TABLE NAME: ");
			
			writeConfigFile(output_file,user_dataset,table_name,table_type);
			table_type=getUserInput(prompt);
		}	
	}
	
	
	private static void readConfiguration(String input_file){
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(input_file));
			
			String line;
			String last_table = null;
			String last_type = null;
			String last_dataset = null;
			Table [] referenced_tables = null;
			ArrayList list = new ArrayList();
			int lines =0;
			int dataset_counter =0;
			
			SourceSchema source_schema = new SourceSchema(config);
			
			while((line = in.readLine()) != null){
				
				String [] items = line.split("\t");
				
				if (lines ==0) source_schema.dataset=items[0];
				
				
				if (!items[2].equals(last_table) || !items[0].equals(last_dataset)){
					
					referenced_tables = source_schema.getReferencedTables(items[2]);
					
					if (lines !=0){
						createLinkedTables(source_schema,list,last_type,last_table);
						list.clear();
					}
				}
				
				
				
				if (!items[0].equals(last_dataset)){
					
					lines=0;
					
					
					last_table = null;
					last_type = null;
					
					if(dataset_counter !=0){
						
						target_schema = new TargetSchema(source_schema);
						schemas.add(target_schema);	
						source_schema = new SourceSchema(config);
						
						source_schema.dataset=items[0];
						
					}
					
					dataset_counter++;
				}
				
				
				for (int i=0; i<referenced_tables.length;i++){
					
					Table ref_table = referenced_tables[i];
					
					if(ref_table.getName().equals(items[3])){
						if (!items[4].equals("S")){
							ref_table.setCardinality(items[4]);
							if (items[4].equals("1n")){
								ref_table.skip= true;
							}
							list.add(ref_table);
						}
					}
				}	
				
				last_table=items[2];
				last_type=items[1];
				last_dataset=items[0];
				lines++;
			}	
			
			
			in.close();
			createLinkedTables(source_schema,list,last_type,last_table);
			target_schema = new TargetSchema(source_schema);
			schemas.add(target_schema);
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	
	private static void createLinkedTables(SourceSchema source_schema,ArrayList list,String last_type, String last_table){
		
		Table [] b = new Table [list.size()];	
		LinkedTables linked_tables= source_schema.createLinkedTables(last_table,(Table []) list.toArray(b));
		
		StringBuffer final_table = new StringBuffer(source_schema.dataset + "__"+last_table+"__");
		if (last_type.equals("M")){
			linked_tables.final_table_type ="MAIN";
			linked_tables.final_table_name= final_table.append("main").toString();
		} else { 
			linked_tables.final_table_type = "DM";
			linked_tables.final_table_name= final_table.append("dm").toString();
		}
		linked_tables.dataset=source_schema.dataset;	
		source_schema.addLinkedTables(linked_tables);
	}
	
	
	
	
	
	private static void writeConfigFile (String output_file,String dataset,String table_name, String table_type){
		
		SourceSchema source_schema = new SourceSchema(config);
		Table [] referenced_tables = source_schema.getReferencedTables(table_name);
		
		BufferedWriter out =null;
		try {
			out = new BufferedWriter(new FileWriter(output_file, true));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		String card_string=" cardinality [11] [n1] [n1r] [0n] [1n] [SKIP S]: ";
		
		for (int i=0;i<referenced_tables.length; i++){
			
			String cardinality = "";
			Table ref_tab=referenced_tables[i];
			
			while (!(cardinality.equals("11") || cardinality.equals("n1")
					|| cardinality.equals("0n") || cardinality.equals("1n")
					|| cardinality.equals("n1r") || cardinality.equals("S")))
				
			{cardinality = getUserInput(table_name+": "+ref_tab.getName() + card_string);}
			
			try {
				out.write(dataset+"\t"+ table_type+"\t"+table_name+"\t"+ref_tab.getName() +"\t"+ cardinality+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
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




