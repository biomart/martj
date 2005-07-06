/*
 * Created on Jun 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author arek
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConfigurationAdaptor {
	
	public DatabaseAdaptor adaptor;
	public MetaDataResolver resolver;
	public String targetSchemaName;
	private static ArrayList mart = new ArrayList();
	private static Dataset dataset = null;
	
	
	public void readConfiguration(String input_file) {

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
			Table startTable=null;

			while ((line = in.readLine()) != null) {

				if (line.startsWith("#"))
					continue;
				String[] fileEntries = line.split("\t");

				// new dataset
				if (!fileEntries[0].equals(lastDatasetName)) {

					
					// finish the old dataset
					if (lines > 0) {
						transformation.transform();
						dataset.setUserTableNames();
						dataset.createTransformationsForCentralFilters();	
						mart.add(dataset);
					
						
						/**
						Transformation[] final_transformations = dataset.getTransformations();
						for (int i=0;i<final_transformations.length;i++){
						Table dmFinalTable=final_transformations[i].getFinalUnit().getTemp_end();
							
						System.out.println(" ADDED dataset "+dmFinalTable.getName());			
							}
					*/
					}
		
					// new dataset
					dataset = new Dataset();
					datasetName = fileEntries[0];
					dataset.name = datasetName;
					dataset.adaptor = adaptor;
					dataset.targetSchemaName = targetSchemaName;
					dataset.datasetKey = resolver.getPrimaryKeys(fileEntries[2]);
					
					//System.out.println("dataset "+dataset.name+" dateaset key "+dataset.datasetKey);
				}
				
				// new transformation
				if (!fileEntries[9].equals(lastTrans)) {
					
					
					if (lines > 0 && fileEntries[0].equals(lastDatasetName)) transformation.transform();
				
					transformation = new Transformation();
					transformation.adaptor = adaptor;
					transformation.datasetName = datasetName;
					transformation.targetSchemaName = targetSchemaName;
					transformation.number = fileEntries[9];
					transformation.finalTableName = fileEntries[15];
					transformation.userTableName = fileEntries[15];
					if (fileEntries[16].toUpperCase().equals("Y")) transformation.central = true;

					System.out.println ("transforming ... "+transformation.number+" user table "+transformation.userTableName);
					
					StringBuffer final_table = new StringBuffer(datasetName
							+ "__" + fileEntries[2] + "__");
					if (fileEntries[1].toUpperCase().equals("M")) {

						transformation.finalTableType = "MAIN";
						transformation.finalTableName = final_table.append("main").toString();
					} else {
						transformation.finalTableType = "DM";
						transformation.finalTableName = final_table.append("dm").toString();
					}

					String [] centralColumnNames = { "%" };
					String [] centralColumnAliases=null;
					
					if (!fileEntries[13].equals("null")) centralColumnNames = fileEntries[13].split(",");
					if (!fileEntries[14].equals("null")) centralColumnAliases = fileEntries[14].split(",");
					
					startTable= resolver.getCentralTable(fileEntries[2],centralColumnNames,centralColumnAliases);
					transformation.startTable = startTable;
					transformation.type = "linked";
					transformation.column_operations = "addall";

					dataset.addTransformation(transformation);

				}

				String [] columnNames = { "%" };
				String [] columnAliases=null;
				
				if (!fileEntries[11].equals("null")) columnNames = fileEntries[11].split(",");
				if (!fileEntries[12].equals("null")) columnAliases = fileEntries[12].split(",");
				
				
                
				TUnit dunit= null;
				
				if (!fileEntries[5].equals("null")) {
					
//					 switched off fileEntries[5].toLowerCase for oracle
					Table refTable = resolver.getTable(fileEntries[5], columnNames, columnAliases);

					refTable.status = fileEntries[3];
					refTable.cardinality = fileEntries[6];
					if (!fileEntries[8].equals("null"))
						refTable.extension = fileEntries[8];
					if (!fileEntries[7].equals("null"))
						refTable.central_extension = fileEntries[7];
					
					
					dunit = new TUnitDouble(refTable);
				}
                 else {
                 	
                 	if (!fileEntries[7].equals("null"))
						startTable.central_extension = fileEntries[7];
                 	
                 	dunit = new TUnitSingle(startTable);
                 	dunit.type="partition";
                 }
				
				dunit.cardinality = fileEntries[6];
				dunit.column_operations = "addall";
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
				lastDatasetName = datasetName;
				lines++;
			}

			in.close();
			transformation.transform();
			
			dataset.setUserTableNames();
			dataset.createTransformationsForCentralFilters();	
			
			mart.add(dataset);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}

	
	public void writeDDL(
			String sqlFile) throws IOException {

		BufferedWriter sqlout = null;
		sqlout = new BufferedWriter(new FileWriter(sqlFile, true));
		
		//f.delete();

		int indexNo = 0;
		for (int m = 0; m < mart.size(); m++) {
			dataset = (Dataset) mart.get(m);	
		    indexNo++;
			Transformation[] final_transformations = dataset.getTransformations();		
		
		// Dump to SQL
		for (int i = 0; i < final_transformations.length; i++) {

			indexNo = 10 + indexNo;

			TUnit[] units = final_transformations[i].getUnits();

			sqlout.write("\n--\n--       TRANSFORMATION NO "
					+ final_transformations[i].number + "      TARGET TABLE: "
					+ final_transformations[i].userTableName.toUpperCase()
					+ "\n--\n");

			for (int j = 0; j < units.length; j++) {

				// don't want indexes before 'select distinct'
				if (!units[j].single & j > 0)
					sqlout.write(units[j].addIndex(indexNo + j) + "\n");
				sqlout.write(units[j].toSQL() + "\n");
			}
			for (int j = 0; j < units.length; j++) {
				sqlout.write(units[j].dropTempTable() + "\n");
			}
		}

		// now renaming to _key and final indexes
		for (int i = 0; i < final_transformations.length; i++) {

			indexNo = 10 + indexNo;

			TUnit[] units = final_transformations[i].getUnits();

			for (int j = 0; j < units.length; j++) {
				if (!(units[j].tempEnd.getName().matches(".*TEMP.*"))) {

					sqlout.write(units[j].renameKeyColumn(dataset.datasetKey)+ "\n");
					sqlout.write(units[j].addFinalIndex(indexNo + j,dataset.datasetKey + "_key")+ "\n");

				}
			}
		}

	}
		sqlout.close();
	}

	

}
