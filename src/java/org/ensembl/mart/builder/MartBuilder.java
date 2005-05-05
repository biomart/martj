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
import java.util.*;

public class MartBuilder {

	private static String config = "data/builder/connection.properties";
	private static ArrayList mart = new ArrayList();
	private static Dataset dataset = null;
	private static final String data_dir = "data/builder/";
	private static String targetSchemaName;
	private static MetaDataResolver resolver;
	private static DBAdaptor adaptor;

	
	
	
	public static void main(String[] args) throws IOException {

		String config_info = "";
		String file = null;
		String sqlFile = null;

		String tSchemaName = null;

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
		adaptor = Adaptor;

		// user input

		while (tSchemaName == null || tSchemaName.equals("")) {
			tSchemaName = getUserInput("TARGET SCHEMA: ");
		}
		targetSchemaName = tSchemaName;

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
				createConfiguration(user_dataset, file);
				user_dataset = getUserInput("NEW DATASET: ");
			}
		} else {
			file = getUserInput("INPUT CONFIG FILE: ");
			file = data_dir + file;
		}

		System.out.print("Transforming your schema ... please wait ....");
		readConfiguration(file);
        System.out.println ("............................ done");
		
		int ind = 0;

		for (int m = 0; m < mart.size(); m++) {
			dataset = (Dataset) mart.get(m);

			ind++;

			// Reset final table names if you want to

			dataset.setUserTableNames();
			Transformation[] transformations = dataset.getTransformations();


			// the below need to become a part of an interactive GUI
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
			 * // Add central filters Transformation[] tran =
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

			dataset.createTransformationsForCentralFilters();
			Transformation[] final_transformations = dataset
					.getTransformations();

			sqlFile = getUserInput("OUTPUT SQL FILE: ");
			writeDDL(final_transformations, sqlFile, ind);
		}

		System.out.println("WRITTEN TO: "+sqlFile );

	}

	/**
	 * @param final_transformations
	 * @param sqlFile
	 * @param ind
	 */
	private static void writeDDL(Transformation[] final_transformations,
			String sqlFile, int ind) throws IOException {

		File f = new File(sqlFile);
		f.delete();

		BufferedWriter sqlout = null;
		sqlout = new BufferedWriter(new FileWriter(sqlFile, true));

		// Dump to SQL
		for (int i = 0; i < final_transformations.length; i++) {

			ind = 10 + ind;

			TransformationUnit[] units = final_transformations[i].getUnits();

			sqlout.write("\n--\n--       TRANSFORMATION NO "
					+ final_transformations[i].number + "      TARGET TABLE: "
					+ final_transformations[i].userTableName.toUpperCase()
					+ "\n--\n");

			for (int j = 0; j < units.length; j++) {

				// don't want indexes before 'select distinct'
				if (!units[j].single & j > 0)
					sqlout.write(units[j].addIndex(ind + j) + "\n");
				sqlout.write(units[j].toSQL() + "\n");
			}
			for (int j = 0; j < units.length; j++) {
				sqlout.write(units[j].dropTempTable() + "\n");
			}
		}

		// now renaming to _key and final indexes
		for (int i = 0; i < final_transformations.length; i++) {

			ind = 10 + ind;

			TransformationUnit[] units = final_transformations[i].getUnits();

			for (int j = 0; j < units.length; j++) {
				if (!(units[j].tempEnd.getName().matches(".*TEMP.*"))) {

					sqlout.write(units[j].renameKeyColumn(dataset.datasetKey)+ "\n");
					sqlout.write(units[j].addFinalIndex(ind + j,
							dataset.datasetKey + "_key")+ "\n");

				}
			}
		}

		sqlout.close();
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
			writeConfigFile(output_file, user_dataset, table_name, table_type,
					extension, transformationCount);
			table_type = getUserInput(prompt);
		}
	}

	private static void readConfiguration(String input_file) {

		try {
			BufferedReader in = new BufferedReader(new FileReader(input_file));

			//String datasetKey=null;
			String line;
			String lastDatasetName = null;
			String lastTrans = null;
			int lines = 0;
			int dataset_counter = 0;
			Transformation transformation = null;
			String datasetName = null;
			ArrayList linkedList = new ArrayList();

			while ((line = in.readLine()) != null) {

				if (line.startsWith("#"))
					continue;
				String[] fileEntries = line.split("\t");

				// new dataset
				if (!fileEntries[0].equals(lastDatasetName)) {

					if (lines > 0) {
						//dataset.datasetKey=datasetKey;
						mart.add(dataset);
					}
					//datasetKey=null;
					dataset = new Dataset();
					datasetName = fileEntries[0];
					dataset.name = datasetName;
					dataset.adaptor = adaptor;
					dataset.targetSchemaName = targetSchemaName;
					lastDatasetName = datasetName;
					dataset.datasetKey = resolver
							.getPrimaryKeys(fileEntries[2]);
				}

				//if (fileEntries[3].equals("exported") &
				// fileEntries[1].equals("m")) datasetKey=fileEntries[4];

				// new transformation
				if (!fileEntries[9].equals(lastTrans)) {
					if (lines > 0)
						transformation.transform();

					transformation = new Transformation();
					transformation.adaptor = adaptor;
					transformation.datasetName = datasetName;
					transformation.targetSchemaName = targetSchemaName;
					transformation.number = fileEntries[9];
					transformation.finalTableName = fileEntries[12];
					transformation.userTableName = fileEntries[12];
					if (fileEntries[13].equals("Y"))
						transformation.central = true;

					StringBuffer final_table = new StringBuffer(datasetName
							+ "__" + fileEntries[2] + "__");
					if (fileEntries[1].toUpperCase().equals("M")) {

						transformation.finalTableType = "MAIN";
						transformation.finalTableName = final_table.append(
								"main").toString();
					} else {
						transformation.finalTableType = "DM";
						transformation.finalTableName = final_table
								.append("dm").toString();
					}

					transformation.startTable = resolver
							.getCentralTable(fileEntries[2]);
					transformation.type = "linked";
					transformation.column_operations = "addall";

					dataset.addTransformation(transformation);

				}

				String[] columnNames = { "%" };
				if (!fileEntries[11].equals("null"))
					columnNames = fileEntries[11].split(",");

				Table ref_table = resolver.getTable(fileEntries[5]
						.toLowerCase(), columnNames);

				ref_table.status = fileEntries[3];
				//ref_table.PK = fileEntries[4];
				//ref_table.FK = fileEntries[10];
				ref_table.cardinality = fileEntries[6];
				if (!fileEntries[8].equals("null"))
					ref_table.extension = fileEntries[8];
				if (!fileEntries[7].equals("null"))
					ref_table.central_extension = fileEntries[7];

				TransformationUnit dunit = new TransformationUnitDouble(
						ref_table);

				dunit.cardinality = fileEntries[6];
				dunit.column_operations = "addall";
				//dunit.final_table_name = finalTableName;
				dunit.adaptor = adaptor;
				dunit.targetSchema = targetSchemaName;

				if (fileEntries[3].equals("exported"))
					dunit.TSKey = fileEntries[4];
				else
					dunit.TSKey = fileEntries[10];
				if (fileEntries[3].equals("exported"))
					dunit.RFKey = fileEntries[10];
				else
					dunit.RFKey = fileEntries[4];

				transformation.addUnit(dunit);

				lastTrans = fileEntries[9];
				lines++;
			}

			in.close();

			transformation.transform();

			//dataset.datasetKey=
			mart.add(dataset);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

