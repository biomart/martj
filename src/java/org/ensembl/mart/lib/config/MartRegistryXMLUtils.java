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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Collection of static methods for translating MartRegistry.dtd compliant documents
 * to and from MartRegistry objects.  Contains all of the necessary XML parsing logic
 * to accomplish these tasks.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartRegistryXMLUtils {

	private Logger logger = Logger.getLogger(MartRegistryXMLUtils.class.getName());

	//element names
	private static final String MARTREGISTRY = "MartRegistry";
	private static final String URLLOCATION = "URLLocation";
	private static final String DATABASELOCATION = "DatabaseLocation";
	private static final String REGISTRYLOCATION = "RegistryLocation";
  private static final String REGISTRYDOCTYPEURL = "classpath:data/XML/MartRegistry.dtd";
  
	//attribute names
//  private static final String NAME = "name";
//	private static final String HOST = "host";
//	private static final String PORT = "port";
//	private static final String DATABASETYPE = "databaseType";
//	private static final String INSTANCENAME = "instanceName";
//	private static final String USER = "user";
//	private static final String PASSWORD = "password";
//	private static final String URL = "url";
//  private static final String JDBCDRIVER = "jdbcDriverClassName";

	public static MartRegistry XMLStreamToMartRegistry(InputStream in) throws ConfigurationException {
		return XMLStreamToMartRegistry(in, false);
	}

	public static MartRegistry XMLStreamToMartRegistry(InputStream in, boolean validate) throws ConfigurationException {
		return DocumentToMartRegistry(XMLStreamToDocument(in, validate));
	}

	public static Document XMLStreamToDocument(InputStream in, boolean validate) throws ConfigurationException {
		try {
			SAXBuilder builder = new SAXBuilder();
			// set the EntityResolver to a allow it to get the DTD from the Classpath.
			 builder.setEntityResolver(new ClasspathDTDEntityResolver());
			builder.setValidation(validate);

			InputSource is = new InputSource(in);

			Document doc = builder.build(is);

			return doc;
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	public static MartRegistry DocumentToMartRegistry(Document doc) throws ConfigurationException {
		Element thisElement = doc.getRootElement();

		MartRegistry martreg = new MartRegistry();

		for (Iterator iter = thisElement.getChildElements(URLLOCATION).iterator(); iter.hasNext();) {
			Element urlloc = (Element) iter.next();
			martreg.addMartLocation(getURLLocation(urlloc));
		}

		for (Iterator iter = thisElement.getChildElements(DATABASELOCATION).iterator(); iter.hasNext();) {
			Element dbloc = (Element) iter.next();
			martreg.addMartLocation(getDBLocation(dbloc));
		}

		for (Iterator iter = thisElement.getChildElements(REGISTRYLOCATION).iterator(); iter.hasNext();) {
			Element regloc = (Element) iter.next();
			martreg.addMartLocation(getRegLocation(regloc));
		}

		return martreg;
	}
  
  public static MartRegistry ByteArrayToMartRegistry(byte[] b) throws ConfigurationException {
    ByteArrayInputStream bin = new ByteArrayInputStream(b);
    return XMLStreamToMartRegistry(bin);
  }
  
	// private static ElementToObject methods 
	private static MartLocation getURLLocation(Element urlloc) throws ConfigurationException {
    URLLocation loc = new URLLocation();
    loadAttributesFromElement(urlloc, loc);
    
    //fail now if the url string is not valid
		loc.getUrl();
    
    return loc;
	}

	private static MartLocation getDBLocation(Element dbloc) throws ConfigurationException {
    DatabaseLocation loc = new DatabaseLocation();
    loadAttributesFromElement(dbloc, loc);
    return loc;
	}

	private static MartLocation getRegLocation(Element regloc) throws ConfigurationException {
    RegistryLocation loc = new RegistryLocation();
    loadAttributesFromElement(regloc, loc);
    
    //fail now if the url string is not valid
    loc.getUrl();
    
    return loc;
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
   * Writes a MartRegistry object as XML to the given File.  Handles opening and closing of the OutputStream.
   * @param dsv -- MartRegistry object
   * @param file -- File to write XML
   * @throws ConfigurationException for underlying Exceptions
   */
  public static void MartRegistryToFile(MartRegistry mr, File file) throws ConfigurationException {
    DocumentToFile(MartRegistryToDocument(mr), file);
  }
  
  /**
   * Writes a MartRegistry object as XML to the given OutputStream.  Does not close the OutputStream after writing.
   * If you wish to write a Document to a File, use MartRegistryToFile instead, as it handles opening and closing the OutputStream.
   * @param dsv -- MartRegistry object to write as XML
   * @param out -- OutputStream to write, not closed after writing
   * @throws ConfigurationException for underlying Exceptions
   */
  public static void MartRegistryToOutputStream(MartRegistry dsv, OutputStream out) throws ConfigurationException {
    DocumentToOutputStream(MartRegistryToDocument(dsv), out);
  }
  
  /**
   * Writes a JDOM Document as XML to a given File.  Handles opening and closing of the OutputStream.
   * @param doc -- Document representing a MartRegistry.dtd compliant XML document
   * @param file -- File to write.
   * @throws ConfigurationException for underlying Exceptions.
   */
  public static void DocumentToFile(Document doc, File file) throws ConfigurationException {
    try {
      FileOutputStream out = new FileOutputStream(file);
      DocumentToOutputStream(doc, out);
      out.close();
    } catch (FileNotFoundException e) {
      throw new ConfigurationException("Caught FileNotFoundException writing Document to File provided " + e.getMessage(), e);
    } catch (ConfigurationException e) {
      throw e;
    } catch (IOException e) {
      throw new ConfigurationException("Caught IOException creating FileOutputStream " + e.getMessage(), e);
    }
  }
  
  /**
   * Takes a JDOM Document and writes it as MartRegistry.dtd compliant XML to a given OutputStream.
   * Does NOT close the OutputStream after writing.  If you wish to write a Document to a File,
   * use DocumentToFile instead, as it handles opening and closing the OutputStream. 
   * @param doc -- Document representing a MartRegistry.dtd compliant XML document
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
  
  public static byte[] DocumentToByteArray(Document doc) throws ConfigurationException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DocumentToOutputStream(doc, bout);
    return bout.toByteArray(); 
  }
  
  public static byte[] MartRegistryToByteArray(MartRegistry martreg) throws ConfigurationException {
    return DocumentToByteArray(MartRegistryToDocument(martreg));
  }
  
	public static Document MartRegistryToDocument(MartRegistry martreg) throws ConfigurationException {
		Element root = new Element(MARTREGISTRY);

		MartLocation[] martlocs = martreg.getMartLocations();
		for (int i = 0, n = martlocs.length; i < n; i++) {
			MartLocation location = martlocs[i];

			if (location.getType().equals(MartLocationBase.URL))
				root.addContent(getURLLocationElement((URLLocation) location));
			else if (location.getType().equals(MartLocationBase.DATABASE))
				root.addContent(getDatabaseLocationElement((DatabaseLocation) location));
			else if (location.getType().equals(MartLocationBase.REGISTRY))
				root.addContent(getRegistryLocationElement((RegistryLocation) location));
			//else not needed, but may need to add other else ifs in future
		}

    Document thisDoc = new Document(root);
    thisDoc.setDocType( new DocType(MARTREGISTRY, REGISTRYDOCTYPEURL));
		return thisDoc;
	}

	//private static ObjectToElement methods
	private static Element getURLLocationElement(URLLocation loc) throws ConfigurationException {
		Element location = new Element(URLLOCATION);
    loadElementAttributesFromObject(loc, location);
		return location;
	}

	private static Element getDatabaseLocationElement(DatabaseLocation loc) throws ConfigurationException {
		Element location = new Element(DATABASELOCATION);
    loadElementAttributesFromObject(loc, location);
		return location;
	}

	private static Element getRegistryLocationElement(RegistryLocation loc) throws ConfigurationException {
		Element location = new Element(URLLOCATION);
    loadElementAttributesFromObject(loc, location);
		return location;
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
  
  private static boolean validString(String test) {
    return (test != null && test.length() > 0);
  }
}
