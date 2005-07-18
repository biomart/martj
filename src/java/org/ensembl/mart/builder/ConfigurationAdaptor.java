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
import java.util.List;



import org.ensembl.mart.builder.config.*;

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
	private static DatasetCode datasetCode = null;
	
	
	public void readConfiguration(String input_file) {

		try {
			BufferedReader in = new BufferedReader(new FileReader(input_file));

			//String datasetCodeKey=null;
			String line;
			String lastDatasetName = null;
			String lastTrans = null;
			int lines = 0;
			int datasetCode_counter = 0;
			TransformationCode transformation = null;
			String datasetCodeName = null;
			ArrayList linkedList = new ArrayList();
			Table startTable=null;

			while ((line = in.readLine()) != null) {

				if (line.startsWith("#"))
					continue;
				String[] fileEntries = line.split("\t");

				// new datasetCode
				if (!fileEntries[0].equals(lastDatasetName)) {

					System.out.println("CALLED A");					
					// finish the old datasetCode
					if (lines > 0) {
						System.out.println("CALLED B");
						transformation.transform();
						datasetCode.setUserTableNames();
						datasetCode.createTransformationsForCentralFilters();	
						mart.add(datasetCode);
					
						
						/**
						TransformationCode[] final_transformations = datasetCode.getTransformations();
						for (int i=0;i<final_transformations.length;i++){
						Table dmFinalTable=final_transformations[i].getFinalUnit().getTemp_end();
							
						System.out.println(" ADDED datasetCode "+dmFinalTable.getName());			
							}
					*/
					}
		
					// new datasetCode
					datasetCode = new DatasetCode();
					datasetCodeName = fileEntries[0];
					datasetCode.name = datasetCodeName;
					datasetCode.adaptor = adaptor;
					datasetCode.targetSchemaName = targetSchemaName;
					datasetCode.datasetKey = resolver.getPrimaryKeys(fileEntries[2]);
					
					//System.out.println("datasetCode "+datasetCode.name+" dateaset key "+datasetCode.datasetCodeKey);
				}
				
				// new transformation
				if (!fileEntries[9].equals(lastTrans)) {
					
					
					if (lines > 0 && fileEntries[0].equals(lastDatasetName)) transformation.transform();
				
					transformation = new TransformationCode();
					transformation.adaptor = adaptor;
					transformation.datasetName = datasetCodeName;
					transformation.targetSchemaName = targetSchemaName;
					transformation.number = fileEntries[9];
					transformation.finalTableName = fileEntries[15];
					transformation.userTableName = fileEntries[15];
					if (fileEntries[16].toUpperCase().equals("Y")) transformation.central = true;

					System.out.println ("transforming ... "+transformation.number+" user table "+transformation.userTableName);
					
					StringBuffer final_table = new StringBuffer(datasetCodeName
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

					datasetCode.addTransformation(transformation);

				}

				String [] columnNames = { "%" };
				String [] columnAliases=null;
				
				if (!fileEntries[11].equals("null")) columnNames = fileEntries[11].split(",");
				if (!fileEntries[12].equals("null")) columnAliases = fileEntries[12].split(",");
				
				
                
				TUnit dunit= null;
				Table refTable = null;
				
				if (!fileEntries[5].equals("null")) {
					
                     // switched off fileEntries[5].toLowerCase for oracle
					// "main_interim" name needs to be a centrally settable param
					// config file, ConfigurationAdaptor and DatasetCode.
					if (!fileEntries[5].equals("main_interim")) {
						refTable = resolver.getTableColumns(fileEntries[5], columnNames, columnAliases);
					    refTable.type="temp";
					}
                     else {
                     	refTable = resolver.getTable(fileEntries[5], fileEntries[4]);
                     refTable.type="interim";
                     }
					
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
				lastDatasetName = datasetCodeName;
				lines++;
			}

			in.close();
			transformation.transform();
			
			datasetCode.setUserTableNames();
			datasetCode.createTransformationsForCentralFilters();	
			
			mart.add(datasetCode);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}



	public void readXMLConfiguration(TransformationConfig tConfig) {

		
			String lastDatasetName = null;
			
			TransformationCode transformationCode = null;
			String datasetCodeName = null;
			ArrayList linkedList = new ArrayList();
			Table startTable=null;


			Dataset[] datasets = tConfig.getDatasets();
			for (int i = 0; i < datasets.length; i++){
				Dataset dataset = datasets[i];
	            
				if (i > 0) mart.add(datasetCode);
				
				//	new datasetCode
				datasetCode = new DatasetCode();
				datasetCodeName = dataset.getInternalName();
				datasetCode.name = datasetCodeName;
				datasetCode.adaptor = adaptor;
				datasetCode.targetSchemaName = targetSchemaName;
				datasetCode.datasetKey = resolver.getPrimaryKeys(dataset.getMainTable());
				
				
				Transformation[] transformations = dataset.getTransformations();
				for (int j = 0; j < transformations.length; j++){
					Transformation transformation = transformations[j];
						
					transformationCode = new TransformationCode();
					transformationCode.adaptor = adaptor;
					transformationCode.datasetName = datasetCodeName;
					transformationCode.targetSchemaName = targetSchemaName;
					transformationCode.number = transformation.getInternalName();
					transformationCode.finalTableName = transformation.getUserTableName();
					transformationCode.userTableName = transformation.getUserTableName();
					if (transformation.getIncludeCentralFilter().toUpperCase().equals("Y")) transformationCode.central = true;

					System.out.println ("transforming ... "+transformationCode.number+" user table "+transformationCode.userTableName);
					
					StringBuffer final_table = new StringBuffer(datasetCodeName
							+ "__" + transformation.getCentralTable() + "__");
					if (transformation.getTableType().toUpperCase().equals("M")) {

						transformationCode.finalTableType = "MAIN";
						transformationCode.finalTableName = final_table.append("main").toString();
					} else {
						transformationCode.finalTableType = "DM";
						transformationCode.finalTableName = final_table.append("dm").toString();
					}

					String [] centralColumnNames = { "%" };
					String [] centralColumnAliases=null;
					
					transformationCode.type = "linked";
					transformationCode.column_operations = "addall";

					datasetCode.addTransformation(transformationCode);
					
					List transformationUnits = transformation.getTransformationUnits();
					for (int k = 0; k < transformationUnits.size(); k++){
						TransformationUnit transformationUnit = (TransformationUnit) transformationUnits.get(k);
					
						if (!transformationUnit.getCentralColumnNames().equals("")) centralColumnNames = transformationUnit.getCentralColumnNames().split(",");
						if (!transformationUnit.getCentralColumnAliases().equals("")) centralColumnAliases = transformationUnit.getCentralColumnAliases().split(",");
					
						startTable= resolver.getCentralTable(transformation.getCentralTable(),centralColumnNames,centralColumnAliases);
						transformationCode.startTable = startTable;
					
						
						String [] columnNames = { "%" };
						String [] columnAliases=null;
											
						if (!transformationUnit.getReferenceColumnNames().equals("")){
							 System.out.println("ADDING A:"+columnNames);
							 columnNames = transformationUnit.getReferenceColumnNames().split(","); 
						}
						if (!transformationUnit.getReferenceColumnAliases().equals("")){ 
							System.out.println("ADDING B:"+columnAliases);
							columnAliases = transformationUnit.getReferenceColumnAliases().split(","); 
						}
				
				
                
						TUnit dunit= null;
						Table refTable = null;
				
						if (!transformationUnit.getReferencedTable().equals("")) {
					
							 // switched off fileEntries[5].toLowerCase for oracle
							// "main_interim" name needs to be a centrally settable param
							// config file, ConfigurationAdaptor and DatasetCode.
							if (!transformationUnit.getReferencedTable().equals("main_interim")) {
								refTable = resolver.getTableColumns(transformationUnit.getReferencedTable(), columnNames, columnAliases);
								refTable.type="temp";
							}
							 else {
								refTable = resolver.getTable(transformationUnit.getReferencedTable(), transformationUnit.getPrimaryKey());
							 refTable.type="interim";
							 }
					
							refTable.status = transformationUnit.getReferencingType();
							refTable.cardinality = transformationUnit.getCardinality();
							if (!transformationUnit.getReferencedProjection().equals(""))
								refTable.extension = transformationUnit.getReferencedProjection();
							if (!transformationUnit.getCentralProjection().equals(""))
								refTable.central_extension = transformationUnit.getCentralProjection();
					
					
							dunit = new TUnitDouble(refTable);
						}
						 else {
                 	
							if (!transformationUnit.getCentralProjection().equals(""))
								startTable.central_extension = transformationUnit.getCentralProjection();
                 	
							dunit = new TUnitSingle(startTable);
							dunit.type="partition";
						 }
				
						 dunit.cardinality = transformationUnit.getCardinality();
						 dunit.column_operations = "addall";
						 dunit.adaptor = adaptor;
						 dunit.targetSchema = targetSchemaName;

						 if (transformationUnit.getReferencingType().equals("exported"))
							dunit.TSKey = transformationUnit.getPrimaryKey();
						 else
							dunit.TSKey = transformationUnit.getForeignKey();
						 if (transformationUnit.getReferencingType().equals("exported"))
							dunit.RFKey = transformationUnit.getForeignKey();
						 else
							dunit.RFKey = transformationUnit.getPrimaryKey();

						 transformationCode.addUnit(dunit);

						 lastDatasetName = datasetCodeName;
								
					}
				}
				
			}
			
			mart.add(datasetCode);
	}























	
	public void writeDDL(
			String sqlFile) throws IOException {

		BufferedWriter sqlout = null;
		sqlout = new BufferedWriter(new FileWriter(sqlFile, true));
		
		//f.delete();

		int indexNo = 0;
		for (int m = 0; m < mart.size(); m++) {
			datasetCode = (DatasetCode) mart.get(m);	
			
			datasetCode.transform();
			
		    indexNo++;
			TransformationCode[] final_transformations = datasetCode.getTransformations();		
		
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
				
				System.out.println("UNIT:"+j);
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

					sqlout.write(units[j].renameKeyColumn(datasetCode.datasetKey)+ "\n");
					sqlout.write(units[j].addFinalIndex(indexNo + j,datasetCode.datasetKey + "_key")+ "\n");

				}
			}
		}

	}
		sqlout.close();
	}

	

}
