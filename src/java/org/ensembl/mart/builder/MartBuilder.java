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
	
	
	private static String config ="data/builder/connection.properties";
	private static ArrayList mart = new ArrayList();
	private static Dataset dataset = null;
	private static final String data_dir="data/builder/";
	private  static String targetSchemaName;
	private static MetaDataResolver resolver;
	private static DBAdaptor adaptor;
	
	
	
	public static void main(String[] args) {
	
		String config_info= "";
		String file=null;
		
		
		String tSchemaName=null; 
		
		// Connections
		DBAdaptor Adaptor = new DBAdaptor(config);

		MetaDataResolver Resolver = null;

		if (Adaptor.rdbms.equals("mysql")) {
			Resolver = new MetaDataResolverFKNotSupported(Adaptor);
		} else if (Adaptor.rdbms.equals("oracle")) {
			Resolver = new MetaDataResolverFKSupported(Adaptor);
		} else if (Adaptor.rdbms.equals("postgresql")) {
			Resolver = new MetaDataResolverFKSupported(Adaptor);
		} 
		
		resolver = Resolver;
		adaptor= Adaptor;
		
		
		// user input
		
		while (tSchemaName == null || tSchemaName.equals("")){
			tSchemaName = getUserInput("TARGET SCHEMA: ");	
		} 
		targetSchemaName=tSchemaName;
		
		
		while (! (config_info.toUpperCase().equals("C") || config_info.toUpperCase().equals("R"))){
			config_info=getUserInput("Configuration Create [C] Read [R]: ");
		}
		
		if (config_info.toUpperCase().equals("C")){
			file=getUserInput("OUTPUT CONFIG FILE: ");
			file = data_dir+file;
			
			File f = new File(file);
			f.delete();
			String user_dataset = getUserInput("NEW DATASET: ");
			
			while (!(user_dataset == null || user_dataset.equals(""))){
				createConfiguration(user_dataset,file);
				user_dataset = getUserInput("NEW DATASET: ");	
			} 
		} else {
			file=getUserInput("INPUT CONFIG FILE: ");
			file = data_dir+file;
		}
		
		
		
		
		
       System.out.println("Transforming your schema ... please wait ....");	
	   readConfiguration(file);
		
	   
	   int ind=0;
	   
		for (int m=0;m<mart.size();m++){	
			dataset = (Dataset) mart.get(m);
			
			ind++;
		
			/**
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
			
			Transformation [] transformations = dataset.getTransformations();
		
			
			System.out.println("\n\n");
			
			for (int i=0;i<transformations.length;i++){
				
				String newname = getUserInput("CHANGE FINAL TABLE NAME: "+transformations[i].final_table_name+" TO: " );
				if (newname != null && ! newname.equals("\n") && !newname.equals(""))
					transformations[i].setFinalName(newname);
			}
			
			
			
			// Add central filters
			Transformation [] tran = dataset.getTransformationsByFinalTableType("DM");
			
			for (int i=0;i<tran.length;i++){
				String input = getUserInput("INCLUDE CENTRAL FILTER FOR: "+tran[i].final_table_name+" [Y|N] [Y default] ");
				if (!(input.equals("N") || input.equals("n"))){
					tran[i].central=true;		
					
					//String extension = getUserInput(tran[i].final_table_name+" EXTENSION: ");
					//tran[i].getFinalUnit().getTemp_end().central_extension=extension;
				}
				
			}
			
			//System.out.println("ds name from MBuilder 2 "+dataset.name);
			
			dataset.createTransformationsForCentralFilters();
			
			
			Transformation [] final_transformations = dataset.getTransformations();
			
			//int ind=0;
			// Dump to SQL
			for (int i=0;i<final_transformations.length;i++){
				
				ind =10+ind;
				
				TransformationUnit [] units = final_transformations[i].getUnits();
				
				System.out.println("");
				for (int j=0;j<units.length;j++){
					System.out.println(units[j].toSQL());
					System.out.println(units[j].addIndex(ind+j));
				}
				for (int j=0;j<units.length;j++){
					System.out.println(units[j].dropTempTable());	 
				}
				
			
			}
			
			
			
			// now renaming to _key and final indexes
			for (int i=0;i<final_transformations.length;i++){
				
				ind =10+ind;
				
				TransformationUnit [] units = final_transformations[i].getUnits();
				
				for (int j=0;j<units.length;j++){
						if (!(units[j].temp_end.getName().matches(".*TEMP.*") )){
							System.out.println(units[j].renameKeyColumn(dataset.transformationKey));
						System.out.println(units[j].addFinalIndex(ind+j,dataset.transformationKey+"_key"));
						}
					}				
			}
			
			
			
		}
		
	}		
	
	
	
	
	
	
	
	private static void createConfiguration(String user_dataset,String output_file){
		
		
		String prompt = "TYPE MAIN [M] DIMENSION [D] EXIT [E]: ";
		String table_type;
		
		do
			table_type=getUserInput(prompt);
		while (!(table_type.toUpperCase().equals("M") || table_type.toUpperCase().equals("D") || table_type.toUpperCase().equals("E")));
		
		while (table_type.toUpperCase().equals("M") || table_type.toUpperCase().equals("D")){
			String table_name = getUserInput("TABLE NAME: ");
			String extension = getUserInput("Extension: ");
			//if (extension == null)
			writeConfigFile(output_file,user_dataset,table_name,table_type, extension);
			table_type=getUserInput(prompt);
		}	
	}
	
	
	private static void readConfiguration(String input_file){
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(input_file));
			
			String line;
			String last_table = null;
			String last_type = null;
			String lastDatasetName = null;
			Table [] referenced_tables = null;
			ArrayList referencedList = new ArrayList();
			int lines =0;
			int dataset_counter =0;
			String centralExtension = null;
			LinkedTables [] linkedTables=null;
			
			//String extension =null;
			
			//SourceSchema source_schema = new SourceSchema(config);
			
			
			
			String datasetName=null;
			
			ArrayList linkedList = new ArrayList();
			
			while((line = in.readLine()) != null){
				
				if(line.startsWith("#")) continue;
				String [] fileEntries = line.split("\t");
				
				
				// if new central table or new dataset 
				if (!fileEntries[2].equals(last_table) || !fileEntries[0].equals(lastDatasetName)){
					
					datasetName=lastDatasetName;
					
					// get referenced tables for a central table	
				    referenced_tables = resolver.getReferencedTables(fileEntries[2]);
				    		    
					// create new linked tables (central plus referenced) 
					if (lines !=0){
						LinkedTables lt = createLinkedTables(datasetName,referencedList,last_type,last_table,centralExtension);
						linkedList.add(lt);
						referencedList.clear();
					}
				}
				
				centralExtension=null;
				
				// if new dataset
				if (!fileEntries[0].equals(lastDatasetName)){
					
					lines=0;
					
					
					last_table = null;
					last_type = null;
					
				if(dataset_counter !=0){
					
					
						dataset = new Dataset(linkedList,lastDatasetName,targetSchemaName,adaptor);
				         //dataset.name=lastDatasetName;
						mart.add(dataset);	
					
						//System.out.println("adding dataset1 "+dataset.name);
						
						//source_schema = new SourceSchema(config);
						//source_schema.datasetName=fileEntries[0];
						
					}
					linkedList.clear();
					dataset_counter++;
				}
				
				// check with a line if it is a referenced for transformation
				for (int i=0; i<referenced_tables.length;i++){
					
					Table ref_table = referenced_tables[i];
					
					// this should match key and a table
					
					
					if(ref_table.getName().toUpperCase().equals(fileEntries[5]) & ref_table.key.equals(fileEntries[4])){
						if (!fileEntries[6].toUpperCase().equals("S")){
							ref_table.setCardinality(fileEntries[6]);
							if (!fileEntries[8].equals("null")) ref_table.extension = fileEntries[8];
							if (!fileEntries[7].equals("null")) ref_table.central_extension = fileEntries[7];
							if (fileEntries[6].equals("1n")){
								ref_table.skip= true;
							}
							referencedList.add(ref_table);
						}
					}
				}	
				
				last_table=fileEntries[2];
				last_type=fileEntries[1];
				lastDatasetName=fileEntries[0];
				//if (!fileEntries[7].equals("null")) centralExtension = fileEntries[7];
				lines++;
			}	
			
			
			in.close();
			// get last linked tables
			
			
			LinkedTables lt= createLinkedTables(lastDatasetName,referencedList,last_type,last_table,centralExtension);
			linkedList.add(lt);
			
			dataset = new Dataset(linkedList,lastDatasetName,targetSchemaName, adaptor);
			//dataset.name=lastDatasetName;
			//System.out.println("adding dataset at the bottom "+dataset.name);
			
			mart.add(dataset);
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	
	private static LinkedTables createLinkedTables(String datasetName,ArrayList referencedList,String last_type, String last_table,String extension){
		
		Table [] b = new Table [referencedList.size()];	
		
		//ArrayList linkedList = new ArrayList();
		
		//LinkedTables linked_tables= createLinkedTables(last_table,(Table []) list.toArray(b));
		
		
		
		Table central = resolver.getCentralTable(last_table);
		
		LinkedTables linked_tables = new LinkedTables();
		linked_tables.setCentralTable(central);
		linked_tables.setReferencedTables((Table []) referencedList.toArray(b));
		
		
		
		
		
		StringBuffer final_table = new StringBuffer(datasetName + "__"+last_table+"__");
		if (last_type.toUpperCase().equals("M")){
			
			linked_tables.final_table_type ="MAIN";
			linked_tables.final_table_name= final_table.append("main").toString();
		} else { 
			linked_tables.final_table_type = "DM";
			linked_tables.final_table_name= final_table.append("dm").toString();
		}
		linked_tables.datasetName=datasetName;
		linked_tables.centralTable.extension=extension;
		
		//linkedList.add(linked_tables);
		//LinkedTables[] c = new LinkedTables[linkedList.size()];
		//return (LinkedTables[]) linkedList.toArray(c);
		
		return linked_tables;
		
		//addLinkedTables(linked_tables);
	}
	
	
	
	
	
	private static void writeConfigFile (String output_file,String dataset,String table_name, String table_type, String extension){
		
		
		
		BufferedWriter out =null;
		try {
			out = new BufferedWriter(new FileWriter(output_file, true));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//SourceSchema source_schema = new SourceSchema(config);
		
		Table [] exp_tables = resolver.getExportedKeyTables(table_name);
		write (out,exp_tables,table_name, table_type,dataset,extension,"exported");
		
		Table [] imp_tables = resolver.getImportedKeyTables(table_name);
		write (out,imp_tables,table_name, table_type,dataset,extension,"imported");
		
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
	
	
	private static void write (BufferedWriter out,Table [] referenced_tables, String table_name, String table_type,String dataset, String centralExtension,String type){
		
		String card_string=" cardinality [11] [n1] [n1r] [0n] [1n] [skip s]: ";
		String extension = null;
		
		for (int i=0;i<referenced_tables.length; i++){
			
			String cardinality = "";
			Table ref_tab=referenced_tables[i];
			if (centralExtension == null || centralExtension.equals("")) centralExtension="null";
			
			
			
			while (!(cardinality.equals("11") || cardinality.equals("n1")
					|| cardinality.equals("0n") || cardinality.equals("1n")
					|| cardinality.equals("n1r") || cardinality.equals("s")))
				
			{
				
				cardinality = getUserInput(table_name+": "+type+" "+ref_tab.key+" "+ref_tab.getName().toUpperCase() + card_string);
				extension="null";
				if (!cardinality.equals("s") & !cardinality.equals("1n")) extension = getUserInput("Extension: ");
				//if (!cardinality.equals("s") & !cardinality.equals("1n")) centralExtension = getUserInput("Central Extension: ");
				if (extension == null || extension.equals("")) extension="null";
				
			
			}
			
			if (cardinality.equals("s") || cardinality.equals("1n")) continue;
			
			try {
				out.write(dataset+"\t"+ table_type+"\t"+table_name+"\t"+type+"\t"+ref_tab.key+"\t"+ref_tab.getName().toUpperCase() +"\t"+ cardinality+"\t"+centralExtension+"\t"+extension+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
			
	}
	
	
}




