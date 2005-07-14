/*
 * Created on Jun 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk </a>
 * 
 *  
 */

import java.io.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
//import java.util.*;

public class MartBuilderOrig {

	private static final String data_dir = "data/builder/";
	private static String config = data_dir+"connection.properties";
	
	private static MetaDataResolver resolver;
	//private static DBAdaptor adaptor;

	//private File file = null;
	
	
	public static void main(String[] args) throws IOException {

		String config_info = "";
		String file = null;
		String ddlFile = null;
		String tSchemaName = null;

		
		// Connections
		DatabaseAdaptor Adaptor = new DatabaseAdaptor(config);

		MetaDataResolver Resolver = null;

		if (Adaptor.rdbms.equals("mysql")) {
			Resolver = new MetaDataResolverFKNotSupported(Adaptor);
		} else if (Adaptor.rdbms.equals("oracle")) {
			Resolver = new MetaDataResolverFKSupported(Adaptor);
		} else if (Adaptor.rdbms.equals("postgresql")) {
			Resolver = new MetaDataResolverFKSupported(Adaptor);
		}

		resolver = Resolver;
		//adaptor = Adaptor;

	

		while (tSchemaName == null || tSchemaName.equals("")) {
			tSchemaName = getUserInput("TARGET SCHEMA: ");
		}
		//targetSchemaName = tSchemaName;

		while (!(config_info.toUpperCase().equals("C") || config_info
				.toUpperCase().equals("R"))) {
			config_info = getUserInput("Configuration Create [C] Read [R]: ");
		}

		if (config_info.toUpperCase().equals("C")) {
			file = getUserInput("OUTPUT CONFIG FILE: ");
			file = data_dir + file;

			File f = new File(file);
			f.delete();
			String user_dataset = getUserInput("NEW DATASET: ");

			while (!(user_dataset == null || user_dataset.equals(""))) {
				
				
                  //	needs new dataset here
				// DatasetCode dataset = new DatasetCode();
				
				createConfiguration(user_dataset, file);
				user_dataset = getUserInput("NEW DATASET: ");
			}
		} else {
			file = getUserInput("INPUT CONFIG FILE: ");
			file = data_dir + file;
		}

		ddlFile = getUserInput("OUTPUT DDL» FILE: ");
		
		File sFile = new File(ddlFile);
		sFile.delete();
		
		System.out.println();
		
		ConfigurationAdaptor configAdaptor = new ConfigurationAdaptor();
		configAdaptor.adaptor=Adaptor;
		configAdaptor.resolver=Resolver;
		configAdaptor.targetSchemaName=tSchemaName;
		
		
		configAdaptor.readConfiguration(file);
		configAdaptor.writeDDL(ddlFile);
		
		System.out.println ("\nWritten DDLs to: "+ddlFile);
	}


		
	
	private static void createConfiguration(String user_dataset,
			String output_file) {

		String prompt = "TYPE MAIN [M] DIMENSION [D] EXIT [E]: ";
		String table_type;
		int transformationCount = 0;

		do
			table_type = getUserInput(prompt);
		while (!(table_type.toUpperCase().equals("M")
				|| table_type.toUpperCase().equals("D") || table_type
				.toUpperCase().equals("E")));

		while (table_type.toUpperCase().equals("M")
				|| table_type.toUpperCase().equals("D")) {
			String table_name = getUserInput("TABLE NAME: ");
			String extension = getUserInput("Extension: ");

			transformationCount++;
			
			// needs new transformation here
			// TransformationCode transformation = new TransformationCode();
			
			writeConfigFile(output_file, user_dataset, table_name, table_type,
					extension, transformationCount);
			table_type = getUserInput(prompt);
		}
	}

	
	
	private static String getUserInput(String prompt) {

		BufferedReader console = new BufferedReader(new InputStreamReader(
				System.in));

		System.out.print(prompt);
		String cardinality = null;

		try {
			cardinality = console.readLine();
		} catch (IOException e) {
			System.err.println("error: " + e);
		}
		;

		return cardinality;

	}
	
	
	
	
	
	
	
	// need to create here transformation units
	
	private static void writeConfigFile(String output_file, String dataset,
			String table_name, String table_type, String extension,
			int transformationCount) {

		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(output_file, true));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String[] columnNames = null;
		columnNames[0] = "%";

		Table[] exp_tables = resolver.getExportedKeyTables(table_name,
				columnNames);
		write(out, exp_tables, table_name, table_type, dataset, extension,
				transformationCount);

		Table[] imp_tables = resolver.getImportedKeyTables(table_name,
				columnNames);
		write(out, imp_tables, table_name, table_type, dataset, extension,
				transformationCount);

		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	

	private static void write(BufferedWriter out, Table[] referenced_tables,
			String table_name, String table_type, String dataset,
			String centralExtension, int transfromationCount) {

		String card_string = " cardinality [11] [n1] [n1r] [0n] [1n] [skip s]: ";
		String extension = null;

		for (int i = 0; i < referenced_tables.length; i++) {

			String cardinality = "";
			Table ref_tab = referenced_tables[i];
			if (centralExtension == null || centralExtension.equals(""))
				centralExtension = "null";

			while (!(cardinality.equals("11") || cardinality.equals("n1")
					|| cardinality.equals("0n") || cardinality.equals("1n")
					|| cardinality.equals("n1r") || cardinality.equals("s")))

			{

				cardinality = getUserInput(table_name + ": " + ref_tab.status
						+ " " + ref_tab.PK + " " + ref_tab.FK + " "
						+ ref_tab.getName().toUpperCase() + card_string);
				extension = "null";
				if (!cardinality.equals("s") & !cardinality.equals("1n"))
					extension = getUserInput("Extension: ");

				if (extension == null || extension.equals(""))
					extension = "null";

			}

			if (cardinality.equals("s") || cardinality.equals("1n"))
				continue;

			
			
			// this needs to create TransformationUnits
			// Transformation unit = new TransforamtionUnit();
			
			try {
				out.write(dataset + "\t" + table_type + "\t" + table_name
						+ "\t" + ref_tab.status + "\t" + ref_tab.PK + "\t"
						+ ref_tab.getName().toUpperCase() + "\t" + cardinality
						+ "\t" + centralExtension + "\t" + extension + "\t"
						+ transfromationCount + "\t" + ref_tab.FK + "\n");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}



//the below need to become a part of an interactive GUI
// switched off for now

/**
 * for (int i = 0; i < transformations.length; i++) {
 * 
 * String newname = getUserInput("CHANGE FINAL TABLE NAME: "
 * +transformations[i].number+" "+ transformations[i].finalTableName + "
 * TO: "); if (newname != null && !newname.equals("\n") &&
 * !newname.equals("")) transformations[i].setFinalName(newname); }
 *  
 */

/**
 * // Add central filters TransformationCode[] tran =
 * dataset.getTransformationsByFinalTableType("DM");
 * 
 * for (int i = 0; i < tran.length; i++) { String input =
 * getUserInput("INCLUDE CENTRAL FILTER FOR: " +
 * tran[i].userTableName + " [Y|N] [Y default] "); if
 * (!(input.equals("N") || input.equals("n"))) { tran[i].central =
 * true;
 *  }
 *  }
 *  
 */




