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

package org.ensembl.mart.lib.config;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Attribute;
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
public class DatasetViewXMLUtils {

	private static Logger logger = Logger.getLogger(DatasetViewXMLUtils.class.getName());

	public static String DEFAULTDIGESTALGORITHM = "MD5";

	// element names
	private static final String DATASETVIEW = "DatasetView";
	private static final String STARBASE = "StarBase";
	private static final String PRIMARYKEY = "PrimaryKey";
	private static final String ENABLE = "Enable";
	private static final String DISABLE = "Disable";
	private static final String FILTERPAGE = "FilterPage";
	private static final String FILTERGROUP = "FilterGroup";
	private static final String DSFILTERGROUP = "DSFilterGroup";
	private static final String FILTERCOLLECTION = "FilterCollection";
	private static final String FILTERDESCRIPTION = "FilterDescription";
	private static final String ATTRIBUTEPAGE = "AttributePage";
	private static final String ATTRIBUTEGROUP = "AttributeGroup";
	private static final String ATTRIBUTECOLLECTION = "AttributeCollection";
	private static final String ATTRIBUTEDESCRIPTION = "AttributeDescription";
	private static final String DSATTRIBUTEGROUP = "DSAttributeGroup";
	private static final String OPTION = "Option";
	private static final String PUSHACTION = "PushAction";
	private static final String DEFAULTFILTER = "DefaultFilter";

	// attribute names needed by code
	private static final String INTERNALNAME = "internalName";

	/**
	 * Takes an InputStream containing DatasetView.dtd compliant XML, and creates a DatasetView object.
	 * 
	 * @param xmlinput -- InputStream containing DatasetView.dtd compliant XML.
	 * @return DatasetView
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static DatasetView XMLStreamToDatasetView(InputStream xmlinput) throws ConfigurationException {
		return XMLStreamToDatasetView(xmlinput, null, false);
	}

	/**
	 * Takes an InputStream containing DatasetView.dtd compliant XML, and creates a DatasetView object,
	 * with optional validation of the XML against the DatasetView.dtd contained in the Java CLASSPATH.
	 * @param xmlinput -- InputStream containing DatasetView.dtd compliant XML
	 * @param validate -- if true, XML is validated against the DatasetView.dtd contained in the Java CLASSPATH.
	 * @return DatasetView
	 * @throws ConfigurationException for all underlying Exceptions.
	 */
	public static DatasetView XMLStreamToDatasetView(InputStream xmlinput, boolean validate)
		throws ConfigurationException {
		return XMLStreamToDatasetView(xmlinput, null, validate);
	}

	/**
	 * Takes an InputStream containing DatasetView.dtd compliant XML, and creates a DatasetView object, with
	 * a precomputed Message Digest using a given Algorithm.
	 * @param xmlinput -- InputStream containing DatasetView.dtd compliant XML
	 * @param digest -- byte[] containing the digest
	 * @return DatasetView
	 * @throws ConfigurationException for all underlying Exceptions
	 * @see java.security.MessageDigest
	 */
	public static DatasetView XMLStreamToDatasetView(InputStream xmlinput, byte[] digest) throws ConfigurationException {
		return XMLStreamToDatasetView(xmlinput, digest, false);
	}

	/**
	 * Takes an InputStream containing XML, and creates a DatasetView object.
	 * Optional parameters exist for creating a DatasetView with a message digest
	 * created by a sun.security.MessageDigest object, and for validating the xml against
	 * the DatasetView.dtd stored in the java CLASSPATH. 
	 * @param xmlinput -- InputStream containing DatasetView.dtd compliant XML
	 * @param digest -- byte[] containing the digest
	 * @param validate -- if true, XML is validated against the DatasetView.dtd contained in the Java CLASSPATH.
	 * @return
	 * @throws ConfigurationException for all underlying Exceptions
	 * @see java.security.MessageDigest
	 */
	public static DatasetView XMLStreamToDatasetView(InputStream xmlinput, byte[] digest, boolean validate)
		throws ConfigurationException {
		return DocumentToDatasetView(XMLStreamToDocument(xmlinput, validate), digest);
	}

	/**
	 * Takes an InputStream containing DatasetView.dtd compliant XML, and creates a JDOM Document.
	 * @param xmlinput -- InputStream containin DatasetView.dtd compliant XML
	 * @param validate -- if true, JDOM validates the XML against the DatasetView.dtd in the CLASSPATH
	 * @return org.jdom.Document
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static Document XMLStreamToDocument(InputStream xmlinput, boolean validate) throws ConfigurationException {
		try {
			SAXBuilder builder = new SAXBuilder();
			// set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the Classpath.
			builder.setEntityResolver(new ClasspathDTDEntityResolver());
			builder.setValidation(validate);

			InputSource is = new InputSource(xmlinput);

			Document doc = builder.build(is);

			return doc;
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	/**
	 * Takes a org.jdom.Document Object representing a DatasetView.dtd compliant
	 * XML document, and returns a DatasetView object.
	 * @param doc -- Document representing a DatasetView.dtd compliant XML document
	 * @return DatasetView object
	 * @throws ConfigurationException for non compliant Objects, and all underlying Exceptions.
	 */
	public static DatasetView DocumentToDatasetView(Document doc) throws ConfigurationException {
		return DocumentToDatasetView(doc, null);
	}

	/**
	 * Takes a org.jdom.Document Object representing a DatasetView.dtd compliant
	 * XML document, and returns a DatasetView object.  If a digestAlgorithm and
	 * Message Digest are supplied, these are added to the DatasetView.
	 * @param doc -- Document representing a DatasetView.dtd compliant XML document
	 * @param digest -- a digest computed with the given digestAlgorithm
	 * @return DatasetView object
	 * @throws ConfigurationException for non compliant Objects, and all underlying Exceptions.
	 */
	public static DatasetView DocumentToDatasetView(Document doc, byte[] digest) throws ConfigurationException {
		Element thisElement = doc.getRootElement();

		DatasetView d = new DatasetView();
    loadAttributesFromElement(thisElement, d);

		LoadDatasetViewWithDocument(d, doc);

		if (digest != null)
			d.setMessageDigest(digest);

		return d;
	}

  private static void loadAttributesFromElement(Element thisElement, BaseConfigurationObject obj) {
		List attributes = thisElement.getAttributes();
		
		for (int i = 0, n = attributes.size(); i < n; i++) {
			Attribute att = (Attribute) attributes.get(i);
			String name = att.getName();
			
			obj.setAttribute(name, thisElement.getAttributeValue(name));
		}
  }
  
	/**
	 * Takes a reference to a DatasetView, and a JDOM Document, and parses the JDOM document to add all of the information
	 * from the XML for a particular DatasetView object into the existing DatasetView reference passed into the method.
	 * @param dsv -- DatasetView reference to be updated
	 * @param doc -- Document containing DatasetView.dtd compliant XML for dsv
	 * @throws ConfigurationException when the internalName returned by the JDOM Document does not match
	 *         that of the dsv reference, and for any other underlying Exception
	 */
	public static void LoadDatasetViewWithDocument(DatasetView dsv, Document doc) throws ConfigurationException {
		Element thisElement = doc.getRootElement();
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");

		// a DatasetView object must have been constructed with an internalName
		// test that the internalNames match , throw an exception if they are not
		if (!intName.equals(dsv.getInternalName()))
			throw new ConfigurationException("Document internalName does not match input dsv reference internalName, they may not represent the same data\n");

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			Element option = (Element) iter.next();
			dsv.addOption(getOption(option));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(DEFAULTFILTER)); iter.hasNext();) {
			Element element = (Element) iter.next();
			dsv.addDefaultFilter(getDefaultFilter(element));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(STARBASE)); iter.hasNext();) {
			Element element = (Element) iter.next();
			dsv.addStarBase(element.getTextNormalize());
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(PRIMARYKEY)); iter.hasNext();) {
			Element element = (Element) iter.next();
			dsv.addPrimaryKey(element.getTextNormalize());
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(FILTERPAGE)); iter.hasNext();) {
			Element element = (Element) iter.next();
			dsv.addFilterPage(getFilterPage(element));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(ATTRIBUTEPAGE)); iter.hasNext();) {
			Element element = (Element) iter.next();
			dsv.addAttributePage(getAttributePage(element));
		}

		// we need to manually set the "parent" references on these options
		// so they are availbe for future use.
		List fds = dsv.getAllFilterDescriptions();
		for (Iterator iter = fds.iterator(); iter.hasNext();) {
			FilterDescription fd = (FilterDescription) iter.next();
			fd.setParentsForAllPushOptionOptions(dsv);
		}
	}

	private static DefaultFilter getDefaultFilter(Element thisElement) throws ConfigurationException {
		FilterDescription desc = getFilterDescription(thisElement.getChildElement(FILTERDESCRIPTION));

		DefaultFilter df = new DefaultFilter();
		loadAttributesFromElement(thisElement, df);
    df.setFilterDescription( desc );
    
		return df;
	}

	private static FilterPage getFilterPage(Element thisElement) throws ConfigurationException {
		FilterPage fp = new FilterPage();
		loadAttributesFromElement(thisElement, fp);
		
		for (Iterator iter = thisElement.getDescendants(new MartFilterGroupFilter()); iter.hasNext();) {
			Element element = (Element) iter.next();
			if (element.getName().equals(FILTERGROUP))
				fp.addFilterGroup(getFilterGroup(element));
			else
				fp.addDSFilterGroup(getDSFilterGroup(element));
		}
		
		return fp;
	}

	private static FilterGroup getFilterGroup(Element thisElement) throws ConfigurationException {
		FilterGroup fg = new FilterGroup();
		loadAttributesFromElement(thisElement, fg);

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(FILTERCOLLECTION)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fg.addFilterCollection(getFilterCollection(element));
		}

		return fg;
	}

	private static DSFilterGroup getDSFilterGroup(Element thisElement) throws ConfigurationException {
		DSFilterGroup fg = new DSFilterGroup();
		loadAttributesFromElement(thisElement, fg);

		return fg;
	}

	private static FilterCollection getFilterCollection(Element thisElement) throws ConfigurationException {
		FilterCollection fc = new FilterCollection();
		loadAttributesFromElement(thisElement, fc);

		for (Iterator iter = thisElement.getDescendants(new MartFilterDescriptionFilter()); iter.hasNext();) {
			Element element = (Element) iter.next();
			fc.addFilterDescription(getFilterDescription(element));
		}

		return fc;
	}

	private static Option getOption(Element thisElement) throws ConfigurationException {
		Option o =	new Option();
		loadAttributesFromElement(thisElement, o);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			Element suboption = (Element) iter.next();
			Option o2 = getOption(suboption);
			o2.setParent(o);
			o.addOption(o2);
		}

		for (Iterator iter = thisElement.getChildElements(PUSHACTION).iterator(); iter.hasNext();) {
			o.addPushAction(getPushOptions((Element) iter.next()));
		}

		return o;
	}

	private static PushAction getPushOptions(Element thisElement) throws ConfigurationException {
		PushAction pa = new PushAction();
		loadAttributesFromElement(thisElement, pa);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			pa.addOption(getOption((Element) iter.next()));
		}

		return pa;
	}

	private static FilterDescription getFilterDescription(Element thisElement) throws ConfigurationException {
		FilterDescription f = new FilterDescription();
		loadAttributesFromElement(thisElement, f);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			Element option = (Element) iter.next();
			Option o = getOption(option);
			o.setParent(f);
			f.addOption(o);
		}

		for (Iterator iter = thisElement.getChildElements(ENABLE).iterator(); iter.hasNext();) {
			f.addEnable(getEnable((Element) iter.next()));
		}

		for (Iterator iter = thisElement.getChildElements(DISABLE).iterator(); iter.hasNext();) {
			f.addDisable(getDisable((Element) iter.next()));
		}

		return f;
	}

	private static Enable getEnable(Element thisElement) throws ConfigurationException {
		Enable e = new Enable();
		loadAttributesFromElement(thisElement, e);
		return e;
	}

	private static Disable getDisable(Element thisElement) throws ConfigurationException {
		Disable d = new Disable();
		loadAttributesFromElement(thisElement, d);
		return d;
	}

	private static AttributePage getAttributePage(Element thisElement) throws ConfigurationException {
		AttributePage ap = new AttributePage();
		loadAttributesFromElement(thisElement, ap);

		for (Iterator iter = thisElement.getDescendants(new MartAttributeGroupFilter()); iter.hasNext();) {
			Element element = (Element) iter.next();
			if (element.getName().equals(ATTRIBUTEGROUP))
				ap.addAttributeGroup(getAttributeGroup(element));
			else
				ap.addDSAttributeGroup(getDSAttributeGroup(element));
		}

		return ap;
	}

	private static AttributeGroup getAttributeGroup(Element thisElement) throws ConfigurationException {
		AttributeGroup ag = new AttributeGroup();
		loadAttributesFromElement(thisElement, ag);
		
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(ATTRIBUTECOLLECTION)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ag.addAttributeCollection(getAttributeCollection(element));
		}

		return ag;
	}

	private static DSAttributeGroup getDSAttributeGroup(Element thisElement) throws ConfigurationException {
		DSAttributeGroup ag = new DSAttributeGroup();
		loadAttributesFromElement(thisElement, ag);
		return ag;
	}

	private static AttributeCollection getAttributeCollection(Element thisElement) throws ConfigurationException {
		AttributeCollection ac = new AttributeCollection();
		loadAttributesFromElement(thisElement, ac);
		
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(ATTRIBUTEDESCRIPTION)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ac.addAttributeDescription(getAttributeDescription(element));
		}

		return ac;
	}

	private static AttributeDescription getAttributeDescription(Element thisElement) throws ConfigurationException {
		AttributeDescription a = new AttributeDescription();
		loadAttributesFromElement(thisElement, a);
		return a;
	}

	/**
	 * Writes a DatasetView object as XML to the given File.  Handles opening and closing of the OutputStream.
	 * @param dsv -- DatasetView object
	 * @param file -- File to write XML
	 * @throws ConfigurationException for underlying Exceptions
	 */
	public static void DatasetViewToFile(DatasetView dsv, File file) throws ConfigurationException {
		DocumentToFile(DatasetViewToDocument(dsv), file);
	}

	/**
	 * Writes a DatasetView object as XML to the given OutputStream.  Does not close the OutputStream after writing.
	 * If you wish to write a Document to a File, use DatasetViewToFile instead, as it handles opening and closing the OutputStream.
	 * @param dsv -- DatasetView object to write as XML
	 * @param out -- OutputStream to write, not closed after writing
	 * @throws ConfigurationException for underlying Exceptions
	 */
	public static void DatasetViewToOutputStream(DatasetView dsv, OutputStream out) throws ConfigurationException {
		DocumentToOutputStream(DatasetViewToDocument(dsv), out);
	}

	/**
	 * Writes a JDOM Document as XML to a given File.  Handles opening and closing of the OutputStream.
	 * @param doc -- Document representing a DatasetView.dtd compliant XML document
	 * @param file -- File to write.
	 * @throws ConfigurationException for underlying Exceptions.
	 */
	public static void DocumentToFile(Document doc, File file) throws ConfigurationException {
		try {
			FileOutputStream out = new FileOutputStream(file);
			DocumentToOutputStream(doc, out);
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
	 * Takes a JDOM Document and writes it as DatasetView.dtd compliant XML to a given OutputStream.
	 * Does NOT close the OutputStream after writing.  If you wish to write a Document to a File,
	 * use DocumentToFile instead, as it handles opening and closing the OutputStream. 
	 * @param doc -- Document representing a DatasetView.dtd compliant XML document
	 * @param out -- OutputStream to write to, not closed after writing
	 * @throws ConfigurationException for underlying IOException
	 */
	public static void DocumentToOutputStream(Document doc, OutputStream out) throws ConfigurationException {
		XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

		try {
			xout.output(doc, out);
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing XML to OutputStream " + e.getMessage(), e);
		}
	}

  private static void loadElementAttributesFromObject(BaseConfigurationObject obj, Element thisElement) {
  	String[] titles = obj.getXmlAttributeTitles();
  	
  	//sort the attribute titles before writing them out, so that MD5SUM is supported
  	Arrays.sort(titles);
  	
  	for (int i = 0, n = titles.length; i < n; i++) {
			String key = titles[i];
			
			if (validString( obj.getAttribute(key) ))
			  thisElement.setAttribute(key, obj.getAttribute(key));
		}
  }
  
	/**
	 * Takes a DatasetView object, and returns a JDOM Document representing the
	 * XML for this Object. Does not store DataSource or Digest information
	 * @param dsview -- DatasetView object to be converted into a JDOM Document
	 * @return Document object
	 */
	public static Document DatasetViewToDocument(DatasetView dsview) {
		Element root = new Element(DATASETVIEW);
		loadElementAttributesFromObject(dsview, root);
		
		Option[] os = dsview.getOptions();
		for (int i = 0, n = os.length; i < n; i++)
			root.addContent(getOptionElement(os[i]));

		DefaultFilter[] dfilts = dsview.getDefaultFilters();
		for (int i = 0, n = dfilts.length; i < n; i++)
			root.addContent(getDefaultFilterElement(dfilts[i]));

		String[] starbases = dsview.getStarBases();
		for (int i = 0, n = starbases.length; i < n; i++)
			root.addContent(getStarBaseElement(starbases[i]));

		String[] pkeys = dsview.getPrimaryKeys();
		for (int i = 0, n = pkeys.length; i < n; i++)
			root.addContent(getPrimaryKeyElement(pkeys[i]));

		FilterPage[] fpages = dsview.getFilterPages();
		for (int i = 0, n = fpages.length; i < n; i++)
			root.addContent(getFilterPageElement(fpages[i]));

		AttributePage[] apages = dsview.getAttributePages();
		for (int i = 0, n = apages.length; i < n; i++)
			root.addContent(getAttributePageElement(apages[i]));

		return new Document(root);
	}

	private static Element getAttributePageElement(AttributePage apage) {
		Element page = new Element(ATTRIBUTEPAGE);
		loadElementAttributesFromObject(apage, page);
		
		List groups = apage.getAttributeGroups();
		for (Iterator iter = groups.iterator(); iter.hasNext();) {
			Object group = iter.next();
			if (group instanceof AttributeGroup)
				page.addContent(getAttributeGroupElement((AttributeGroup) group));
			else
				page.addContent(getDSAttributeGroupElement((DSAttributeGroup) group));
		}

		return page;
	}

	private static Element getDSAttributeGroupElement(DSAttributeGroup group) {
		Element dsag = new Element(DSATTRIBUTEGROUP);
		loadElementAttributesFromObject(group, dsag);
		return dsag;
	}

	private static Element getAttributeGroupElement(AttributeGroup group) {
		Element ag = new Element(ATTRIBUTEGROUP);
		loadElementAttributesFromObject(group, ag);
		
		AttributeCollection[] acs = group.getAttributeCollections();
		for (int i = 0, n = acs.length; i < n; i++)
			ag.addContent(getAttributeCollectionElement(acs[i]));

		return ag;
	}

	private static Element getAttributeCollectionElement(AttributeCollection collection) {
		Element ac = new Element(ATTRIBUTECOLLECTION);
    loadElementAttributesFromObject(collection, ac);

		List ads = collection.getAttributeDescriptions();
		//currently there are only AttributeDescription objects, may be DSAttributeDescription in the future
		for (Iterator iter = ads.iterator(); iter.hasNext();)
			ac.addContent(getAttributeDescriptionElement((AttributeDescription) iter.next()));

		return ac;
	}

	private static Element getAttributeDescriptionElement(AttributeDescription attribute) {
		Element att = new Element(ATTRIBUTEDESCRIPTION);
		loadElementAttributesFromObject(attribute, att);
		return att;
	}

	private static Element getFilterPageElement(FilterPage fpage) {
		Element page = new Element(FILTERPAGE);
		loadElementAttributesFromObject(fpage, page);
		
		List groups = fpage.getFilterGroups();
		for (Iterator iter = groups.iterator(); iter.hasNext();) {
			Object group = iter.next();
			if (group instanceof FilterGroup)
				page.addContent(getFilterGroupElement((FilterGroup) group));
			else
				page.addContent(getDSFilterGroupElement((DSFilterGroup) group));
		}

		return page;
	}

	private static Element getDSFilterGroupElement(DSFilterGroup group) {
		Element dsfg = new Element(DSFILTERGROUP);
		loadElementAttributesFromObject(group, dsfg);
		return dsfg;
	}

	/**
	 * @param group
	 * @return
	 */
	private static Element getFilterGroupElement(FilterGroup group) {
		Element fg = new Element(FILTERGROUP);
		loadElementAttributesFromObject(group, fg);

		FilterCollection[] acs = group.getFilterCollections();
		for (int i = 0, n = acs.length; i < n; i++)
			fg.addContent(getFilterCollectionElement(acs[i]));

		return fg;
	}

	private static Element getFilterCollectionElement(FilterCollection collection) {
		Element fc = new Element(FILTERCOLLECTION);
		loadElementAttributesFromObject(collection, fc);
		
		List ads = collection.getFilterDescriptions();
		//currently there are only FilterDescription objects, may be DSFilterDescription in the future
		for (Iterator iter = ads.iterator(); iter.hasNext();)
			fc.addContent(getFilterDescriptionElement((FilterDescription) iter.next()));

		return fc;
	}

	private static Element getPrimaryKeyElement(String primaryKeyString) {
		Element pkey = new Element(PRIMARYKEY);
		pkey.setText(primaryKeyString);
		return pkey;
	}

	private static Element getStarBaseElement(String starbaseString) {
		Element sbase = new Element(STARBASE);
		sbase.setText(starbaseString);
		return sbase;
	}

	private static Element getDefaultFilterElement(DefaultFilter filter) {
		Element def = new Element(DEFAULTFILTER);
		loadElementAttributesFromObject(filter, def);
		def.addContent(getFilterDescriptionElement(filter.getFilterDescription()));

		return def;
	}

	private static Element getOptionElement(Option o) {
		Element option = new Element(OPTION);
		loadElementAttributesFromObject(o, option);
		
		Option[] subops = o.getOptions();
		for (int i = 0, n = subops.length; i < n; i++)
			option.addContent(getOptionElement(subops[i]));

		PushAction[] pushops = o.getPushActions();
		for (int i = 0, n = pushops.length; i < n; i++)
			option.addContent(getPushActionElement(pushops[i]));

		return option;
	}

	private static Element getPushActionElement(PushAction pa) {
		Element pushAction = new Element(PUSHACTION);
		loadElementAttributesFromObject(pa, pushAction);
		
		Option[] os = pa.getOptions();
		for (int i = 0, n = os.length; i < n; i++)
			pushAction.addContent(getOptionElement(os[i]));

		return pushAction;
	}

	private static Element getFilterDescriptionElement(FilterDescription filter) {
		Element fdesc = new Element(FILTERDESCRIPTION);
		loadElementAttributesFromObject(filter, fdesc);
		
		Enable[] enables = filter.getEnables();
		for (int i = 0, n = enables.length; i < n; i++)
			fdesc.addContent(getEnableElement(enables[i]));

		Disable[] disables = filter.getDisables();
		for (int i = 0, n = disables.length; i < n; i++)
			fdesc.addContent(getDisableElement(disables[i]));

		Option[] subops = filter.getOptions();
		for (int i = 0, n = subops.length; i < n; i++)
			fdesc.addContent(getOptionElement(subops[i]));

		return fdesc;
	}

	private static Element getDisableElement(Disable disable) {
		Element dsbl = new Element(DISABLE);
		loadElementAttributesFromObject(disable, dsbl);
		return dsbl;
	}

	private static Element getEnableElement(Enable enable) {
		Element enbl = new Element(ENABLE);
		loadElementAttributesFromObject(enable, enbl);
		return enbl;
	}

	private static boolean validString(String test) {
		return (test != null && test.length() > 0);
	}

	/**
	 * Given a Document object, converts the given document to an DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM digest using the 
	 * JDOM XMLOutputter writing to a java.security.DigestOutputStream.  This is the default method for calculating the MessageDigest 
	 * of a DatasetView Object used in various places in the MartJ system.
	 * @param doc -- Document object representing a DatasetView.dtd compliant XML document. 
	 * @return byte[] digest algorithm
	 * @throws ConfigurationException for NoSuchAlgorithmException, and IOExceptions.
	 * @see java.security.DigestOutputStream
	 */
	public static byte[] DocumentToMessageDigest(Document doc) throws ConfigurationException {
		return DocumentToMessageDigest(doc, DEFAULTDIGESTALGORITHM);
	}

	/**
	 * Given a Document object and a digestAlgorithm, converts the given document to
	 * a digest using the JDOM XMLOutputter writing to a DigestOutputStream.  This is the default
	 * method for calculating the MessageDigest of a DatasetView object used in various places in the MartJ system.
	 * If the digestAlgorithm is null, defaults to DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM.
	 * @param doc -- Document object representing a DatasetView.dtd compliant XML document.
	 * @param digestAlgorithm -- Algorithm to use to compute the MessageDigest. If null, DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM is used.
	 * @return byte[] digest algorithm
	 * @throws ConfigurationException for NoSuchAlgorithmException, and IOExceptions.
	 * @see java.security.DigestOutputStream
	 */
	public static byte[] DocumentToMessageDigest(Document doc, String digestAlgorithm) throws ConfigurationException {
		String dalg = (digestAlgorithm != null) ? digestAlgorithm : DEFAULTDIGESTALGORITHM;

		try {
			MessageDigest mdigest = MessageDigest.getInstance(dalg);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			DigestOutputStream dout = new DigestOutputStream(bout, mdigest);
			XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

			xout.output(doc, dout);

			byte[] digest = mdigest.digest();

			bout.close();
			dout.close();

			return digest;
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException("Digest Algorithm " + dalg + " does not exist\n", e);
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException converting Docuement to Digest\n", e);
		}
	}

	/**
	 * Returns a MessageDigest digest for a DatasetView by first creating a JDOM Document object, and then
	 * calculuating its digest using DatasetViewXMLUtils.DocumentToMEssageDigest(dsv, DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM). 
	 * @param dsv -- A DatasetView object
	 * @return byte[] digest
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static byte[] DatasetViewToMessageDigest(DatasetView dsv) throws ConfigurationException {
		return DatasetViewToMessageDigest(dsv, DEFAULTDIGESTALGORITHM);
	}

	/**
	 * Returns a MessageDigest digest for a DatasetView by first creating a JDOM Document object, and then
	 * calculuating its digest using DatasetViewXMLUtils.DocumentToMEssageDigest(dsv, digestAlgorithm). 
	 * @param dsv -- A DatasetView object
	 * @param digestAlgorithm -- String digest algorithm to use. If null, DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM is used.
	 * @return byte[] digest
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static byte[] DatasetViewToMessageDigest(DatasetView dsv, String digestAlgorithm)
		throws ConfigurationException {
		return DocumentToMessageDigest(DatasetViewToDocument(dsv), digestAlgorithm);
	}

	/**
	 * This method does not convert the raw bytes of a given InputStream into a Message Digest.  It is intended to calculate a Message Digest
	 * that is comparable between multiple XML representations of the same DatasetView Object (despite one representation having an Element with
	 * an Attribute specified with an empty string, and the other having the same Element with that Attribute specification missing entirely, or each
	 * containing the same Element with the same attribute specifications, but occuring in a different order within the XML string defining the Element).  
	 * It does this by first converting the InputStream into a DatasetView Object (using XMLStreamToDatasetView(is)), and then calculating the 
	 * digest on the resulting DatasetView Object (using DatasetViewToMessageDigest(dsv, DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM)).
	 * @param xmlinput -- InputStream containing DatasetView.dtd compliant XML.
	 * @return byte[] digest
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static byte[] XMLStreamToMessageDigest(InputStream is) throws ConfigurationException {
		return XMLStreamToMessageDigest(is, DEFAULTDIGESTALGORITHM);
	}

	/**
	 * This method does not convert the raw bytes of a given InputStream into a Message Digest.  It is intended to calculate a Message Digest
	 * that is comparable between multiple XML representations of the same DatasetView Object (despite one representation having an Element with
	 * an Attribute specified with an empty string, and the other having the same Element with that Attribute specification missing entirely, or each
	 * containing the same Element with the same attribute specifications, but occuring in a different order within the XML string defining the Element).  
	 * It does this by first converting the InputStream into a DatasetView Object (using XMLStreamToDatasetView(is)), and then calculating the 
	 * digest on the resulting DatasetView Object (using DatasetViewToMessageDigest(dsv, digestAlgorithm)).
	 * @param xmlinput -- InputStream containing DatasetView.dtd compliant XML.
	 * @param digestAlgorithm -- MessageDigest Algorithm to compute the digest. If null, DatasetViewXMLUtils.DEFAULTDIGESTALGORITHM is used.
	 * @return byte[] digest
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static byte[] XMLStreamToMessageDigest(InputStream is, String digestAlgorithm) throws ConfigurationException {
		return DatasetViewToMessageDigest(XMLStreamToDatasetView(is), digestAlgorithm);
	}

}
