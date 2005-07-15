/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package org.ensembl.mart.builder.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.Attribute;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

/**
 * Utility class containing all necessary XML parsing logic for converting
 * between XML and Object.  Uses JDOM as its XML parsing engine.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class TransformationConfigXMLUtils {

  private Logger logger = Logger.getLogger(TransformationConfigXMLUtils.class.getName());

  //this is the only digest algorithm we support
  public static String DEFAULTDIGESTALGORITHM = "MD5";

  // element names
  private final String TRANSFORMATIONCONFIG = "TransformationConfig";
  private final String DATASET = "Dataset";
  private final String TRANSFORMATION = "Transformation";
  private final String TRANSFORMATIONUNIT = "TransformationUnit";

  // attribute names needed by code
  private final String INTERNALNAME = "internalName";
  private final String OPTPARAM = "optional_parameters";
  private final String DEFAULTDATASET = "defaultDataset";
  private final String HIDDEN = "hidden";

  private boolean loadFully = false;
  protected boolean includeHiddenMembers = false;

  public TransformationConfigXMLUtils(boolean includeHiddenMembers) {
    this.includeHiddenMembers = includeHiddenMembers;
  }

  /**
   * Set the load behavior of the getTransformationConfigXXX methods. If set to true, all TransformationConfig objects are
   * fully loaded, if false, this is deferred to the lazyLoad system. This is primarily for the TransformationConfigCache
   * object. 
   * @param loadFully -- boolean, if true instructs all subsequent getTransformationConfigXXX calls to fully load the TransformationConfig
   * object before loading it, if false defers this to the lazyLoad system
   */
  protected void setFullyLoadMode(boolean loadFully) {
    this.loadFully = loadFully;
  }

  public TransformationConfig getTransformationConfigForByteArray(byte[] b) throws ConfigurationException {
    return getTransformationConfigForByteArray(b, null);
  }

  /**
   * Returns a TransformationConfig from an XML stored as a byte[], allowing the system to specify whether to
   * load all Elements, or defer this to the lazyLoad system. Also allows system
   * to supply a md5sum digest byte[] array to store into the resulting TransformationConfig.
   *  
   * @param b - byte[] holding XML
   * @param digest -- byte[] containing the digest
   * @return TransformationConfig for xml in byte[]
   * @throws ConfigurationException
   */
  public TransformationConfig getTransformationConfigForByteArray(byte[] b, byte[] digest) throws ConfigurationException {
    ByteArrayInputStream bin = new ByteArrayInputStream(b);
    return getTransformationConfigForXMLStream(bin, digest);
  }

  public TransformationConfig getTransformationConfigForXMLStream(InputStream xmlinput) throws ConfigurationException {
    return getTransformationConfigForXMLStream(xmlinput, null);
  }

  /**
   * Takes an InputStream containing XML, and creates a TransformationConfig object.
   * If a MessageDigest is supplied, this will be added to the TransformationConfig object
   * before returning it.
   *  
   * @param xmlinput -- InputStream containing TransformationConfig.dtd compliant XML
   * @param digest -- byte[] containing the digest
   * @return TransformationConfig
   * @throws ConfigurationException for all underlying Exceptions
   * @see java.security.MessageDigest
   */
  public TransformationConfig getTransformationConfigForXMLStream(InputStream xmlinput, byte[] digest) throws ConfigurationException {
    return getTransformationConfigForDocument(getDocumentForXMLStream(xmlinput), digest);
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
   * XML document, and returns a TransformationConfig object.
   * @param doc -- Document representing a TransformationConfig.dtd compliant XML document
   * @return TransformationConfig object
   * @throws ConfigurationException for non compliant Objects, and all underlying Exceptions.
   */
  public TransformationConfig getTransformationConfigForDocument(Document doc) throws ConfigurationException {
    return getTransformationConfigForDocument(doc, null);
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

    if (loadFully)
      loadTransformationConfigWithDocument(d, doc);

    if (digest != null)
      d.setMessageDigest(digest);

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
		if (e.getName().equals(DATASET))
			dsv.addDataset(getDataset(e));
				
	}	
  }


  private Dataset getDataset(Element thisElement) throws ConfigurationException {
	Dataset ap = new Dataset();
	loadAttributesFromElement(thisElement, ap);

	List transformationElements = thisElement.getChildren();		
	for (int i = 0; i < transformationElements.size(); i++){
		Element e = (Element) transformationElements.get(i);
		if (e.getName().equals(TRANSFORMATION))
			ap.addTransformation(getTransformation(e));
				
	}	
	return ap;
  }

  private Transformation getTransformation(Element thisElement) throws ConfigurationException {
	Transformation a = new Transformation();
	loadAttributesFromElement(thisElement, a);
	
	List tunitElements = thisElement.getChildren();		
	for (int i = 0; i < tunitElements.size(); i++){
		Element e = (Element) tunitElements.get(i);
		if (e.getName().equals(TRANSFORMATIONUNIT))
			a.addTransformationUnit(getTransformationUnit(e));
				
	}	
	return a;
  }


  private TransformationUnit getTransformationUnit(Element thisElement) throws ConfigurationException {
	TransformationUnit a = new TransformationUnit();
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

    Dataset[] apages = dsconfig.getDatasets();
    for (int i = 0, n = apages.length; i < n; i++)
      root.addContent(getDatasetElement(apages[i]));

    Document thisDoc = new Document(root);
    thisDoc.setDocType(new DocType(TRANSFORMATIONCONFIG));

    return thisDoc;
  }

  private Element getDatasetElement(Dataset apage) {
    Element page = new Element(DATASET);
    loadElementAttributesFromObject(apage, page);

    Transformation[] groups = apage.getTransformations();
    for (int i = 0; i < groups.length;i++) {
      Object group = groups[i];
      if (group instanceof Transformation)
        page.addContent(getTransformationElement((Transformation) group));
      
    }

    return page;
  }

  private Element getTransformationElement(Transformation attribute) {
    Element att = new Element(TRANSFORMATION);
    loadElementAttributesFromObject(attribute, att);
    return att;
  }

  
  private boolean validString(String test) {
    return (test != null && test.length() > 0);
  }

  /**
   * Given a Document object, converts the given document to an TransformationConfigXMLUtils.DEFAULTDIGESTALGORITHM digest using the 
   * JDOM XMLOutputter writing to a java.security.DigestOutputStream.  This is the default method for calculating the MessageDigest 
   * of a TransformationConfig Object used in various places in the MartJ system.
   * @param doc -- Document object representing a TransformationConfig.dtd compliant XML document. 
   * @return byte[] digest algorithm
   * @throws ConfigurationException for NoSuchAlgorithmException, and IOExceptions.
   * @see java.security.DigestOutputStream
   */
  public byte[] getMessageDigestForDocument(Document doc) throws ConfigurationException {
    try {
      MessageDigest mdigest = MessageDigest.getInstance(DEFAULTDIGESTALGORITHM);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DigestOutputStream dout = new DigestOutputStream(bout, mdigest);
      XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

      xout.output(doc, dout);

      byte[] digest = mdigest.digest();

      bout.close();
      dout.close();

      return digest;
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException(
        "Digest Algorithm " + DEFAULTDIGESTALGORITHM + " does not exist, possibly a problem with the Java Installation\n",
        e);
    } catch (IOException e) {
      throw new ConfigurationException("Caught IOException converting Docuement to Digest\n", e);
    }
  }

  /**
   * Returns a MessageDigest digest for a TransformationConfig by first creating a JDOM Document object, and then
   * calculuating its digest using TransformationConfigXMLUtils.DEFAULTDIGESTALGORITHM. 
   * @param dsv -- A TransformationConfig object
   * @return byte[] digest
   * @throws ConfigurationException for all underlying Exceptions
   */
  public byte[] getMessageDigestForTransformationConfig(TransformationConfig dsv) throws ConfigurationException {
    return getMessageDigestForDocument(getDocumentForTransformationConfig(dsv));
  }

  /**
   * This method does not convert the raw bytes of a given InputStream into a Message Digest.  It is intended to calculate a Message Digest
   * that is comparable between multiple XML representations of the same TransformationConfig Object (despite one representation having an Element with
   * an Attribute specified with an empty string, and the other having the same Element with that Attribute specification missing entirely, or each
   * containing the same Element with the same attribute specifications, but occuring in a different order within the XML string defining the Element).  
   * It does this by first converting the InputStream into a TransformationConfig Object (using XMLStreamToTransformationConfig(is)), and then calculating the 
   * digest on the resulting TransformationConfig Object (using TransformationConfigToMessageDigest(dsv, TransformationConfigXMLUtils.DEFAULTDIGESTALGORITHM)).
   * @param xmlinput -- InputStream containing TransformationConfig.dtd compliant XML.
   * @return byte[] digest
   * @throws ConfigurationException for all underlying Exceptions
   */
  public byte[] getMessageDigestForXMLStream(InputStream is) throws ConfigurationException {
    return getMessageDigestForDocument(getDocumentForXMLStream(is));
  }

  /**
   * Returns a byte[] of XML for the given TransformationConfig object.
   * @param dsv - TransformationConfig object to be parsed into a byte[]
   * @return byte[] representing XML for TransformationConfig
   * @throws ConfigurationException for underlying exceptions
   */
  public byte[] getByteArrayForTransformationConfig(TransformationConfig dsv) throws ConfigurationException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    writeTransformationConfigToOutputStream(dsv, bout);
    return bout.toByteArray();
  }

}
