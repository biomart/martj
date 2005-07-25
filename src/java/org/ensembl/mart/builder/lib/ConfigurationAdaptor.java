/*
 * Created on Jun 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;


/**
 * 
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * @author <a href="mailto:arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConfigurationAdaptor {
	
	public DatabaseAdaptor adaptor;
	public MetaDataAdaptor resolver;
	public String targetSchemaName;
	private static ArrayList mart = new ArrayList();
	private static Dataset datasetCode = null;
	
	
	
	

	public TransformationConfig getTransformationConfig(
			String file) {

		SAXBuilder parser = new SAXBuilder();
		Document doc = null;
		try {
			doc = parser.build(file);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		Element root = doc.getRootElement();

		TransformationConfig tc = new TransformationConfig(root);

		List datasetElements = tc.element.getChildren();

		for (int i = 0; i < datasetElements.size(); i++) {
			Dataset ds = new Dataset((Element) datasetElements.get(i));
			tc.addChildObject(ds);

			List transformationElements = ds.element.getChildren();

			for (int m = 0; m < transformationElements.size(); m++) {
				Transformation ts = new Transformation(
						(Element) transformationElements.get(m));
				ds.addChildObject(ts);

				List transformationUnitElements = ts.element.getChildren();

				for (int j = 0; j < transformationUnitElements.size(); j++) {
					TransformationUnit tu = new TransformationUnit(
							(Element) transformationUnitElements.get(j));
					ts.addChildObject(tu);
				}
			}
		}

		return tc;

	}
	
	
	
	

	 public void writeDocument(
			TransformationConfig trans, String xmlFile) {

		Document newDoc = new Document();

		BufferedWriter sqlout = null;
		try {
			sqlout = new BufferedWriter(new FileWriter(xmlFile));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		newDoc.setContent((Element) trans.element.clone());
		
	
		

		XMLOutputter outputter = new XMLOutputter();
		try {
			outputter.output(newDoc, sqlout);
		} catch (IOException e) {
			System.err.println(e);
		}

	}


	

	public void readXMLConfiguration(TransformationConfig tConfig) {

		
			String lastDatasetName = null;
			
			Transformation transformationCode = null;
			String datasetCodeName = null;
			ArrayList linkedList = new ArrayList();
			Table startTable=null;


			ConfigurationBase[] datasets = tConfig.getChildObjects();
			for (int i = 0; i < datasets.length; i++){
				Dataset dataset = (Dataset) datasets[i];
	            
				if (i > 0) mart.add(datasetCode);
				
				//	new datasetCode
				datasetCode = new Dataset();
				datasetCodeName = dataset.getElement().getAttributeValue("internalName");
				datasetCode.name = datasetCodeName;
				datasetCode.adaptor = adaptor;
				datasetCode.targetSchemaName = targetSchemaName;
				
				// db interaction
				datasetCode.datasetKey = resolver.getPrimaryKeys(dataset.getElement().getAttributeValue("mainTable"));
				
				
				ConfigurationBase[] transformations = dataset.getChildObjects();
				for (int j = 0; j < transformations.length; j++){
					Transformation transformation = (Transformation) transformations[j];
						
					transformationCode = new Transformation();
					transformationCode.adaptor = adaptor;
					transformationCode.datasetName = datasetCodeName;
					transformationCode.targetSchemaName = targetSchemaName;
					transformationCode.number = transformation.getElement().getAttributeValue("internalName");
					transformationCode.finalTableName = transformation.getElement().getAttributeValue("userTable");
					transformationCode.userTableName = transformation.getElement().getAttributeValue("userTable");
					if (transformation.getElement().getAttributeValue("includeCentralFilter").toUpperCase().equals("Y")) transformationCode.central = true;

					System.out.println ("transforming ... "+transformationCode.number+" user table "+transformationCode.userTableName);
					
					StringBuffer final_table = new StringBuffer(datasetCodeName
							+ "__" + transformation.getElement().getAttributeValue("centralTable") + "__");
					if (transformation.getElement().getAttributeValue("tableType").toUpperCase().equals("M")) {

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
					
					ConfigurationBase[] transformationUnits = transformation.getChildObjects();
					for (int k = 0; k < transformationUnits.length; k++){
						TransformationUnit transformationUnit = (TransformationUnit) transformationUnits[k];
					
						if (!transformationUnit.getElement().getAttributeValue("centralColumnNames").equals("")) centralColumnNames = transformationUnit.getElement().getAttributeValue("centralColumnNames").split(",");
						if (!transformationUnit.getElement().getAttributeValue("centralColumnNames").equals("")) centralColumnAliases = transformationUnit.getElement().getAttributeValue("centralColumnNames").split(",");
					
						
						//db interaction
						startTable= resolver.getCentralTable(transformation.getElement().getAttributeValue("centralTable"),centralColumnNames,centralColumnAliases);
						transformationCode.startTable = startTable;
					
						
						String [] columnNames = { "%" };
						String [] columnAliases=null;
											
						if (!transformationUnit.getElement().getAttributeValue("referenceColumnNames").equals("")){
							 System.out.println("ADDING A:"+columnNames);
							 columnNames = transformationUnit.getElement().getAttributeValue("referenceColumnNames").split(","); 
						}
						if (!transformationUnit.getElement().getAttributeValue("referenceColumnAliases").equals("")){ 
							System.out.println("ADDING B:"+columnAliases);
							columnAliases = transformationUnit.getElement().getAttributeValue("referenceColumnAliases").split(","); 
						}
				
				
                
						TransformationUnit dunit= null;
						Table refTable = null;
				
						if (!transformationUnit.getElement().getAttributeValue("referencedTable").equals("")) {
					
							 // switched off fileEntries[5].toLowerCase for oracle
							// "main_interim" name needs to be a centrally settable param
							// config file, ConfigurationAdaptor and DatasetCode.
							
							// db interaction
							
							if (!transformationUnit.getElement().getAttributeValue("referencedTable").equals("main_interim")) {
								refTable = resolver.getTableColumns(transformationUnit.getElement().getAttributeValue("referencedTable"), columnNames, columnAliases);
								refTable.type="temp";
							}
							 else {
								refTable = resolver.getTable(transformationUnit.getElement().getAttributeValue("referencedTable"), transformationUnit.getElement().getAttributeValue("primaryKey"));
							 refTable.type="interim";
							 }
					
							refTable.status = transformationUnit.getElement().getAttributeValue("referencingType");
							refTable.cardinality = transformationUnit.getElement().getAttributeValue("cardinality");
							if (!transformationUnit.getElement().getAttributeValue("referencedProjection").equals(""))
								refTable.extension = transformationUnit.getElement().getAttributeValue("referencedProjection");
							if (!transformationUnit.getElement().getAttributeValue("centralProjection").equals(""))
								refTable.central_extension = transformationUnit.getElement().getAttributeValue("centralProjection");
					
					
							dunit = new TransformationUnitDouble(refTable);
						}
						 else {
                 	
							if (!transformationUnit.getElement().getAttributeValue("centralProjection").equals(""))
								startTable.central_extension = transformationUnit.getElement().getAttributeValue("centralProjection");
                 	
							dunit = new TransformationUnitSingle(startTable);
							dunit.type="partition";
						 }
				
						 dunit.cardinality = transformationUnit.getElement().getAttributeValue("cardinality");
						 dunit.column_operations = "addall";
						 dunit.adaptor = adaptor;
						 dunit.targetSchema = targetSchemaName;

						 if (transformationUnit.getElement().getAttributeValue("referencingType").equals("exported"))
							dunit.TSKey = transformationUnit.getElement().getAttributeValue("primaryKey");
						 else
							dunit.TSKey = transformationUnit.getElement().getAttributeValue("foreignKey");
						 if (transformationUnit.getElement().getAttributeValue("referencingType").equals("exported"))
							dunit.RFKey = transformationUnit.getElement().getAttributeValue("foreignKey");
						 else
							dunit.RFKey = transformationUnit.getElement().getAttributeValue("primaryKey");

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
			datasetCode = (Dataset) mart.get(m);	
			
			datasetCode.transform();
			
		    indexNo++;
			Transformation[] final_transformations = datasetCode.getAllTransformations();		
		
		// Dump to SQL
		for (int i = 0; i < final_transformations.length; i++) {

			indexNo = 10 + indexNo;

			TransformationUnit[] units = final_transformations[i].getUnits();

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

			TransformationUnit[] units = final_transformations[i].getUnits();

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
