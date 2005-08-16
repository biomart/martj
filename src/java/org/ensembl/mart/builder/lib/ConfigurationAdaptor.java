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
 * @author <a href="mailto:arek@ebi.ac.uk">Arek Kasprzyk</a>
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConfigurationAdaptor {
	
	private MetaDataResolver resolver;
	private String targetSchemaName;
	
	public TransformationConfig getTransformationConfig(
			String file) {

		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = builder.build(file);
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

	private void createTransformations(TransformationConfig tConfig) {
	
			Transformation transformation = null;
			String datasetName = null;
			ArrayList linkedList = new ArrayList();
			Table startTable=null;


			ConfigurationBase[] datasets = tConfig.getChildObjects();
			for (int i = 0; i < datasets.length; i++){
				Dataset dataset = (Dataset) datasets[i];
				//System.out.println("DATASET:"+dataset.getElement().getAttributeValue("internalName"));
				datasetName = dataset.getElement().getAttributeValue("internalName");
				dataset.setRDBMS(resolver.getAdaptor().getRdbms());
				dataset.setTargetSchemaName(targetSchemaName);
				
				dataset.setDatasetKey(resolver.getPrimaryKeys(dataset.getElement().getAttributeValue("mainTable")));
				
				
				ConfigurationBase[] transformations = dataset.getChildObjects();
				for (int j = 0; j < transformations.length; j++){
					transformation = (Transformation) transformations[j];
					
					//System.out.println("TRANSFORMATION:"+transformation.getElement().getAttributeValue("internalName"));	
					
					transformation.setDatasetName(datasetName);
					transformation.setTargetSchemaName(targetSchemaName);
					transformation.setFinalTableName(transformation.getElement().getAttributeValue("userTableName"));
									
					if (transformation.getElement().getAttributeValue("includeCentralFilter").toUpperCase().equals("Y")) transformation.central = true;

					System.out.println ("transforming ... "+transformation.getElement().getAttributeValue("internalName")+" user table "+transformation.getElement().getAttributeValue("userTableName"));
					
					StringBuffer final_table = new StringBuffer(datasetName
							+ "__" + transformation.getElement().getAttributeValue("centralTable") + "__");
					if (transformation.getElement().getAttributeValue("tableType").toUpperCase().equals("M")) {

						transformation.setFinalTableType("MAIN");
						transformation.setFinalTableName(final_table.append("main").toString());
					} else {
						transformation.setFinalTableType("DM");
						transformation.setFinalTableName(final_table.append("dm").toString());
					}

					String [] centralColumnNames = { "%" };
					String [] centralColumnAliases=null;
					
					transformation.setType("linked");
					transformation.setColumnOperations("addall");
					
					ConfigurationBase[] transformationUnits = transformation.getChildObjects();
					for (int k = 0; k < transformationUnits.length; k++){
						TransformationUnit transformationUnit = (TransformationUnit) transformationUnits[k];
						//System.out.println("TRANSFORMATION UNIT:"+transformationUnit.getElement().getAttributeValue("internalName"));	
					
						if (!transformationUnit.getElement().getAttributeValue("centralColumnNames").equals("")) centralColumnNames = transformationUnit.getElement().getAttributeValue("centralColumnNames").split(",");
						if (!transformationUnit.getElement().getAttributeValue("centralColumnAliases").equals("")) centralColumnAliases = transformationUnit.getElement().getAttributeValue("centralColumnAliases").split(",");
											
						//db interaction
						startTable= resolver.getCentralTable(transformation.getElement().getAttributeValue("centralTable"),centralColumnNames,centralColumnAliases);
						transformation.setStartTable(startTable);
					
						
						String [] columnNames = { "%" };
						String [] columnAliases=null;
											
						if (!transformationUnit.getElement().getAttributeValue("referenceColumnNames").equals(""))
							 columnNames = transformationUnit.getElement().getAttributeValue("referenceColumnNames").split(","); 
						if (!transformationUnit.getElement().getAttributeValue("referenceColumnAliases").equals(""))
							columnAliases = transformationUnit.getElement().getAttributeValue("referenceColumnAliases").split(","); 
			
                
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
					
							
							dunit = new TransformationUnitDouble(transformationUnit.getElement(),refTable);
						 }	
						 else {
                 	
							if (!transformationUnit.getElement().getAttributeValue("centralProjection").equals(""))
								startTable.central_extension = transformationUnit.getElement().getAttributeValue("centralProjection");
                 	
							dunit = new TransformationUnitSingle(transformationUnit.getElement(),startTable);
							dunit.setType("partition");
						 }
				
						 dunit.setColumnOperations("addall");
						 dunit.setRDBMS(resolver.getAdaptor().getRdbms());
						 dunit.setTargetSchema(targetSchemaName);

						 if (transformationUnit.getElement().getAttributeValue("referencingType").equals("exported"))
							dunit.setTSKey(transformationUnit.getElement().getAttributeValue("primaryKey"));
						 else
							dunit.setTSKey(transformationUnit.getElement().getAttributeValue("foreignKey"));
						 if (transformationUnit.getElement().getAttributeValue("referencingType").equals("exported"))
							dunit.setRFKey(transformationUnit.getElement().getAttributeValue("foreignKey"));
						 else
							dunit.setRFKey(transformationUnit.getElement().getAttributeValue("primaryKey"));

						 // replace transformationUnit with the newly created dunit
						 transformation.removeChildObject(transformationUnit.getElement().getAttributeValue("internalName"));
						 transformation.insertChildObject(k,dunit);		
					}
				}
				
			}
	}
	
	public void writeDDL(String sqlFile, TransformationConfig tConfig) throws IOException {
		
		createTransformations(tConfig);

		BufferedWriter sqlout = null;
		sqlout = new BufferedWriter(new FileWriter(sqlFile, true));
		int indexNo = 0;
		for (int m = 0; m < tConfig.getChildObjects().length; m++) {
			Dataset dataset = (Dataset) tConfig.getChildObjects()[m];	
			dataset.transform();
		    indexNo++;
			ConfigurationBase[] final_transformations = dataset.getChildObjects();	
		  // Dump to SQL
		  for (int i = 0; i < final_transformations.length; i++) {
			Transformation finalTransformation = (Transformation) final_transformations[i];
			indexNo = 10 + indexNo;

			ConfigurationBase[] units = finalTransformation.getChildObjects();
			
			sqlout.write("\n--\n--       TRANSFORMATION NO "
					+ finalTransformation.getElement().getAttributeValue("internalName") + "      TARGET TABLE: "
					+ finalTransformation.getElement().getAttributeValue("userTableName").toUpperCase()
					+ "\n--\n");

			for (int j = 0; j < units.length; j++) {
					
				// don't want indexes before 'select distinct'
				if (!((TransformationUnit)units[j]).single & j > 0)
					sqlout.write(((TransformationUnit)units[j]).addIndex(indexNo + j) + "\n");
				
				sqlout.write(((TransformationUnit)units[j]).toSQL() + "\n");
			}
			for (int j = 0; j < units.length; j++) {
				sqlout.write(((TransformationUnit)units[j]).dropTempTable() + "\n");
			}
		}

		// now renaming to _key and final indexes
		for (int i = 0; i < final_transformations.length; i++) {
			Transformation finalTransformation = (Transformation) final_transformations[i];
			indexNo = 10 + indexNo;

			ConfigurationBase[] units = finalTransformation.getChildObjects();
			
			for (int j = 0; j < units.length; j++) {
				if (!(((TransformationUnit)units[j]).tempEnd.getName().matches(".*TEMP.*"))) {

					sqlout.write(((TransformationUnit)units[j]).renameKeyColumn(dataset.getDatasetKey())+ "\n");
					sqlout.write(((TransformationUnit)units[j]).addFinalIndex(indexNo + j,dataset.getDatasetKey() + "_key")+ "\n");

				}
			}
		}

	  }
	  sqlout.close();
	}

	

	/**
	 * @return Returns the resolver.
	 */
	public MetaDataResolver getResolver() {
		return resolver;
	}
	/**
	 * @param resolver The resolver to set.
	 */
	public void setResolver(MetaDataResolver resolver) {
		this.resolver = resolver;
	}
	/**
	 * @return Returns the targetSchemaName.
	 */
	public String getTargetSchemaName() {
		return targetSchemaName;
	}
	/**
	 * @param targetSchemaName The targetSchemaName to set.
	 */
	public void setTargetSchemaName(String targetSchemaName) {
		this.targetSchemaName = targetSchemaName;
	}
}
