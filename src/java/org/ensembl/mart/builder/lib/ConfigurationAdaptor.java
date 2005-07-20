/*
 * Created on Jun 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.jdom.Attribute;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;




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
	
	
	
	// Configuration Adaptor to be cleaned up, now an eclectic fusion of old ConfigurationAdaptor and XMLUtils
	
	
	
	  // element names
	  private final String TRANSFORMATIONCONFIG = "TransformationConfig";
	  private final String DATASET = "Dataset";
	  private final String TRANSFORMATION = "Transformation";
	  private final String TRANSFORMATIONUNIT = "TransformationUnit";

	  // attribute names needed by code
	  private final String INTERNALNAME = "internalName";
	 
	  public TransformationConfig getTransformationConfigForXMLStream(InputStream xmlinput) throws ConfigurationException {
	    return getTransformationConfigForDocument(getDocumentForXMLStream(xmlinput),null);
	  }

	  /**
	   * Takes an InputStream containing TransformationConfig.dtd compliant XML, and creates a JDOM Document.
	   * @param xmlinput -- InputStream containin TransformationConfig.dtd compliant XML
	   * @return org.jdom.Document
	   * @throws ConfigurationException for all underlying Exceptions
	   */
	  public Document getDocumentForXMLStream(InputStream xmlinput) throws ConfigurationException {
	    try {
	      SAXBuilder builder = new SAXBuilder();
	      // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the Classpath.
	      //builder.setEntityResolver(new ClasspathDTDEntityResolver());
	      builder.setValidation(false);

	      InputSource is = new InputSource(xmlinput);

	      Document doc = builder.build(is);

	      return doc;
	    } catch (Exception e) {
	      throw new ConfigurationException(e);
	    }
	  }

	  /**
	   * Takes a org.jdom.Document Object representing a TransformationConfig.dtd compliant
	   * XML document, and returns a TransformationConfig object.  If a MD5SUM Message Digest is
	   * supplied, this is added to the TransformationConfig.
	   * @param doc -- Document representing a TransformationConfig.dtd compliant XML document
	   * @param digest -- a digest computed with the given digestAlgorithm
	   * @return TransformationConfig object
	   * @throws ConfigurationException for non compliant Objects, and all underlying Exceptions.
	   */
	  public TransformationConfig getTransformationConfigForDocument(Document doc, byte[] digest) throws ConfigurationException {
	    Element thisElement = doc.getRootElement();

	    TransformationConfig d = new TransformationConfig();
	    loadAttributesFromElement(thisElement, d);
	    return d;
	  }

	  private void loadAttributesFromElement(Element thisElement, BaseConfigurationObject obj) {
	    List attributes = thisElement.getAttributes();

	    for (int i = 0, n = attributes.size(); i < n; i++) {
	      Attribute att = (Attribute) attributes.get(i);
	      String name = att.getName();

	      obj.setAttribute(name, thisElement.getAttributeValue(name));
	    }
	  }

	  /**
	   * Takes a reference to a TransformationConfig, and a JDOM Document, and parses the JDOM document to add all of the information
	   * from the XML for a particular TransformationConfig object into the existing TransformationConfig reference passed into the method.
	   * @param dsv -- TransformationConfig reference to be updated
	   * @param doc -- Document containing TransformationConfig.dtd compliant XML for dsv
	   * @throws ConfigurationException when the internalName returned by the JDOM Document does not match
	   *         that of the dsv reference, and for any other underlying Exception
	   */
	  public void loadTransformationConfigWithDocument(TransformationConfig dsv, Document doc) throws ConfigurationException {
	    Element thisElement = doc.getRootElement();
	    String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		List transformationElements = thisElement.getChildren();		
		for (int i = 0; i < transformationElements.size(); i++){
			Element e = (Element) transformationElements.get(i);
			if (e.getName().equals(DATASET)){
				dsv.addDataset(getDataset(e));
			System.out.println("adding dataset "+e.getName());	
			}
					
		}	
	  }


	  private DatasetBase getDataset(Element thisElement) throws ConfigurationException {
		DatasetBase ap = new DatasetBase();
		loadAttributesFromElement(thisElement, ap);

		List transformationElements = thisElement.getChildren();		
		for (int i = 0; i < transformationElements.size(); i++){
			
			System.out.println("getting childern");
			
			Element e = (Element) transformationElements.get(i);
			if (e.getName().equals(TRANSFORMATION)){
				ap.addTransformation(getTransformation(e));
				
			System.out.println("matching names");	
			}
					
		}	
		return ap;
	  }

	  private TransformationBase getTransformation(Element thisElement) throws ConfigurationException {
		TransformationBase a = new TransformationBase();
		loadAttributesFromElement(thisElement, a);
		
		List tunitElements = thisElement.getChildren();		
		for (int i = 0; i < tunitElements.size(); i++){
			Element e = (Element) tunitElements.get(i);
			if (e.getName().equals(TRANSFORMATIONUNIT))
				a.addTransformationUnit(getTransformationUnit(e));
					
		}	
		return a;
	  }


	  private TransformationUnitBase getTransformationUnit(Element thisElement) throws ConfigurationException {
		TransformationUnitBase a = new TransformationUnitBase();
		loadAttributesFromElement(thisElement, a);
		return a;
	  }



	  /**
	   * Writes a TransformationConfig object as XML to the given File.  Handles opening and closing of the OutputStream.
	   * @param dsv -- TransformationConfig object
	   * @param file -- File to write XML
	   * @throws ConfigurationException for underlying Exceptions
	   */
	  public void writeTransformationConfigToFile(TransformationConfig dsv, File file) throws ConfigurationException {
	    writeDocumentToFile(getDocumentForTransformationConfig(dsv), file);
	  }

	  /**
	   * Writes a TransformationConfig object as XML to the given OutputStream.  Does not close the OutputStream after writing.
	   * If you wish to write a Document to a File, use TransformationConfigToFile instead, as it handles opening and closing the OutputStream.
	   * @param dsv -- TransformationConfig object to write as XML
	   * @param out -- OutputStream to write, not closed after writing
	   * @throws ConfigurationException for underlying Exceptions
	   */
	  public void writeTransformationConfigToOutputStream(TransformationConfig dsv, OutputStream out) throws ConfigurationException {
	    writeDocumentToOutputStream(getDocumentForTransformationConfig(dsv), out);
	  }

	  /**
	   * Writes a JDOM Document as XML to a given File.  Handles opening and closing of the OutputStream.
	   * @param doc -- Document representing a TransformationConfig.dtd compliant XML document
	   * @param file -- File to write.
	   * @throws ConfigurationException for underlying Exceptions.
	   */
	  public void writeDocumentToFile(Document doc, File file) throws ConfigurationException {
	    try {
	      FileOutputStream out = new FileOutputStream(file);
	      writeDocumentToOutputStream(doc, out);
	      out.close();
	    } catch (FileNotFoundException e) {
	      throw new ConfigurationException(
	        "Caught FileNotFoundException writing Document to File provided " + e.getMessage(),
	        e);
	    } catch (ConfigurationException e) {
	      throw e;
	    } catch (IOException e) {
	      throw new ConfigurationException("Caught IOException creating FileOutputStream " + e.getMessage(), e);
	    }
	  }

	  /**
	   * Takes a JDOM Document and writes it as TransformationConfig.dtd compliant XML to a given OutputStream.
	   * Does NOT close the OutputStream after writing.  If you wish to write a Document to a File,
	   * use DocumentToFile instead, as it handles opening and closing the OutputStream. 
	   * @param doc -- Document representing a TransformationConfig.dtd compliant XML document
	   * @param out -- OutputStream to write to, not closed after writing
	   * @throws ConfigurationException for underlying IOException
	   */
	  public void writeDocumentToOutputStream(Document doc, OutputStream out) throws ConfigurationException {
	    XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

	    try {
	      xout.output(doc, out);
	    } catch (IOException e) {
	      throw new ConfigurationException("Caught IOException writing XML to OutputStream " + e.getMessage(), e);
	    }
	  }

	  private void loadElementAttributesFromObject(BaseConfigurationObject obj, Element thisElement) {
	    String[] titles = obj.getXmlAttributeTitles();

	    //sort the attribute titles before writing them out, so that MD5SUM is supported
	    Arrays.sort(titles);

	    for (int i = 0, n = titles.length; i < n; i++) {
	      String key = titles[i];

	      if (validString(obj.getAttribute(key)))
	        thisElement.setAttribute(key, obj.getAttribute(key));
	    }
	  }

	  /**
	   * Takes a TransformationConfig object, and returns a JDOM Document representing the
	   * XML for this Object. Does not store DataSource or Digest information
	   * @param dsconfig -- TransformationConfig object to be converted into a JDOM Document
	   * @return Document object
	   */
	  public Document getDocumentForTransformationConfig(TransformationConfig dsconfig) {
	    Element root = new Element(TRANSFORMATIONCONFIG);
	    loadElementAttributesFromObject(dsconfig, root);

	    DatasetBase[] apages = dsconfig.getDatasets();
	    for (int i = 0, n = apages.length; i < n; i++)
	      root.addContent(getDatasetElement(apages[i]));

	    Document thisDoc = new Document(root);
	    thisDoc.setDocType(new DocType(TRANSFORMATIONCONFIG));

	    return thisDoc;
	  }

	  private Element getDatasetElement(DatasetBase apage) {
	    Element page = new Element(DATASET);
	    loadElementAttributesFromObject(apage, page);

	    TransformationBase[] groups = apage.getTransformations();
	    for (int i = 0; i < groups.length;i++) {
	      Object group = groups[i];
	      if (group instanceof TransformationBase)
	        page.addContent(getTransformationElement((TransformationBase) group));
	      
	    }

	    return page;
	  }

	  private Element getTransformationElement(TransformationBase attribute) {
	    Element att = new Element(TRANSFORMATION);
	    loadElementAttributesFromObject(attribute, att);
	    return att;
	  }
	  
	  private boolean validString(String test) {
	    return (test != null && test.length() > 0);
	  } 
	
	
	
	
	
	
	public void readConfiguration(String input_file) {

		try {
			BufferedReader in = new BufferedReader(new FileReader(input_file));

			//String datasetCodeKey=null;
			String line;
			String lastDatasetName = null;
			String lastTrans = null;
			int lines = 0;
			int datasetCode_counter = 0;
			Transformation transformation = null;
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
					datasetCode = new Dataset();
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
				
					transformation = new Transformation();
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
				
				
                
				TransformationUnit dunit= null;
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
					
					
					dunit = new TransformationUnitDouble(refTable);
				}
                 else {
                 	
                 	if (!fileEntries[7].equals("null"))
						startTable.central_extension = fileEntries[7];
                 	
                 	dunit = new TransformationUnitSingle(startTable);
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
			
			Transformation transformationCode = null;
			String datasetCodeName = null;
			ArrayList linkedList = new ArrayList();
			Table startTable=null;


			DatasetBase[] datasets = tConfig.getDatasets();
			for (int i = 0; i < datasets.length; i++){
				DatasetBase dataset = datasets[i];
	            
				if (i > 0) mart.add(datasetCode);
				
				//	new datasetCode
				datasetCode = new Dataset();
				datasetCodeName = dataset.getInternalName();
				datasetCode.name = datasetCodeName;
				datasetCode.adaptor = adaptor;
				datasetCode.targetSchemaName = targetSchemaName;
				
				// db interaction
				datasetCode.datasetKey = resolver.getPrimaryKeys(dataset.getMainTable());
				
				
				TransformationBase[] transformations = dataset.getTransformations();
				for (int j = 0; j < transformations.length; j++){
					TransformationBase transformation = transformations[j];
						
					transformationCode = new Transformation();
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
						TransformationUnitBase transformationUnit = (TransformationUnitBase) transformationUnits.get(k);
					
						if (!transformationUnit.getCentralColumnNames().equals("")) centralColumnNames = transformationUnit.getCentralColumnNames().split(",");
						if (!transformationUnit.getCentralColumnAliases().equals("")) centralColumnAliases = transformationUnit.getCentralColumnAliases().split(",");
					
						
						//db interaction
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
				
				
                
						TransformationUnit dunit= null;
						Table refTable = null;
				
						if (!transformationUnit.getReferencedTable().equals("")) {
					
							 // switched off fileEntries[5].toLowerCase for oracle
							// "main_interim" name needs to be a centrally settable param
							// config file, ConfigurationAdaptor and DatasetCode.
							
							// db interaction
							
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
					
					
							dunit = new TransformationUnitDouble(refTable);
						}
						 else {
                 	
							if (!transformationUnit.getCentralProjection().equals(""))
								startTable.central_extension = transformationUnit.getCentralProjection();
                 	
							dunit = new TransformationUnitSingle(startTable);
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
