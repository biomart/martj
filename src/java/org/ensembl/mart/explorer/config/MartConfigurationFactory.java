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

package org.ensembl.mart.explorer.config;

import java.net.URL;
import java.sql.Connection;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * Factory object to create a MartConfiguration object.
 * The xml configuration file for a specific mart is contained within
 *  the _meta_configuration table in the xml blob field.  In addition
 *  an XMLSchema for this xml file is contained within the data
 *  directory of the mart-explorer distribution.  The factory pulls
 *  the xml configuration data as a Stream from the database,
 *  and parses it to create the MartConfiguration object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartConfigurationFactory {

	private String martConfSystemID = "MartConfiguration.xml"; // default, but can be over-ridden

	// element names
	private final String dataset = "Dataset";
	private final String starbase = "StarBase";
	private final String primarykey = "PrimaryKey";
	private final String filterset = "FilterSet";
	private final String filterSetDescription = "FilterSetDescription";
	private final String filterpage = "FilterPage";
	private final String filtergroup = "FilterGroup";
	private final String filtercollection = "FilterCollection";
	private final String filterdescription = "UIFilterDescription";
	private final String dsfilterdescription = "UIDSFilterDescription";
	private final String attributepage = "AttributePage";
	private final String attributegroup = "AttributeGroup";
	private final String attributecollection = "AttributeCollection";
	private final String attributedescription = "UIAttributeDescription";

	// attribute names
	private final String internalName = "internalName";
	private final String displayName = "displayName";
	private final String description = "description";
	private final String type = "type";
	private final String fieldName = "fieldName";
	private final String qualifier = "qualifier";
	private final String tableConstraint = "tableConstraint";
	private final String maxSelect = "maxSelect";
	private final String maxLength = "maxLength";
	private final String source = "source";
	private final String homepageURL = "homepageURL";
	private final String linkoutURL = "linkoutURL";
	private final String tableConstraintModifierA = "tableConstraintModifier";
	private final String fieldNameModifierA = "fieldNameModifier"; 
	private final String filterSetNameA = "filterSetName";
	private final String filterSetReqA = "filterSetReq";
	private final String objectCode = "objectCode";

	private MartConfiguration martconf = null;

	/**
	 * Overloaded getInstance method allowing user to supply an alternate xml configuration to use.  This configuration
	 * must exist in the database, and must conform to the MartConfiguration.dtd.  Intended mostly for use by the Unit Test
	 * ConfigurationTest.testMartConfiguration
	 * 
	 * @param conn
	 * @param martName
	 * @param system_id
	 * @return
	 * @throws ConfigurationException
	 */
	public MartConfiguration getInstance(Connection conn, String martName, String system_id) throws ConfigurationException {
		martConfSystemID = system_id;
		return getInstance(conn, martName);
	}

    /**
     * Default getInstance method.  Fetches the MartConfiguration.xml document from the mart database named by martName.
     * 
     * @param conn - A java.sql.Connection object
     * @param martName - name of the mart database for which the configuration is requested
     * 
     * @return MartConfiguration object for the requested mart database
     * @throws ConfigurationException.  Chains all Exceptions resulting from SQL, JDOM parsing, etc. into a ConfigurationException.
     */
	public MartConfiguration getInstance(Connection conn, String martName) throws ConfigurationException {

		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(true); // validate against the DTD
			builder.setEntityResolver(new MartDTDEntityResolver(conn)); // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.

			Document doc = builder.build(MartXMLutils.getInputSourceFor(conn, martConfSystemID));

			Element martconfElement = doc.getRootElement();
			String mname = martconfElement.getAttributeValue(internalName, "");
			if (!mname.equals(martName))
				throw new ConfigurationException(
					"Warning, xml from " + martName + " contains different internalName " + mname + " may need to load a different xml into the mart database");

			String dispname = martconfElement.getAttributeValue(displayName, "");
			String desc = martconfElement.getAttributeValue(description, "");

			martconf = new MartConfiguration(martName, dispname, desc);

			for (Iterator iter = martconfElement.getDescendants(new MartElementFilter(dataset)); iter.hasNext();) {
				Element datasetElement = (Element) iter.next();
				martconf.addDataset(getDataset(datasetElement));
			}

			return martconf;
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

    /**
     * Overloaded getInstance method allowing user to use an alternate xml configuration file stored on the file system, by
     * supplying a URL.  Note, this requires that the DTD for the supplied document be available on the file system as well.
     * Users should make sure that the DOCTYPE declaration correctly locates the DTD for their document.
     *  
     * @param conn - A java.sql.Connection object, used to fetch the dtd from the database, if not supplied as a URL.
     * @param martConfFile - URL for the MartConfiguration xml document.
     * 
     * @return MartConfiguration object for the mart defined by this document
     * @throws ConfigurationException.  Chains all Exceptions from URL, IO, etc. into ConfigurationExceptions
     */
	public MartConfiguration getInstance(Connection conn, URL martConfFile) throws ConfigurationException {

		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(true); // validate against the DTD

			Document doc = builder.build(MartXMLutils.getInputSourceFor(martConfFile));

			Element martconfElement = doc.getRootElement();
			String martName = martconfElement.getAttributeValue(internalName, "");

			String dispname = martconfElement.getAttributeValue(displayName, "");
			String desc = martconfElement.getAttributeValue(description, "");

			martconf = new MartConfiguration(martName, dispname, desc);

			for (Iterator iter = martconfElement.getDescendants(new MartElementFilter(dataset)); iter.hasNext();) {
				Element datasetElement = (Element) iter.next();
				martconf.addDataset(getDataset(datasetElement));
			}

			return martconf;
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}
	
	private Dataset getDataset(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");

		Dataset d = new Dataset(intName, dispname, desc);

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(starbase)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addStarBase(element.getTextNormalize());
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(primarykey)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addPrimaryKey(element.getTextNormalize());
		}
        
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filterpage)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addFilterPage(getFilterPage(element));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributepage)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addAttributePage(getAttributePage(element));
		}
		return d;
	}

	private FilterPage getFilterPage(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");

		FilterPage fp = new FilterPage(intName, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filtergroup)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fp.addFilterGroup(getFilterGroup(element));
		}

		return fp;
	}

	private FilterGroup getFilterGroup(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");

		FilterGroup fg = new FilterGroup(intName, dispname, desc);
		
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filterset)); iter.hasNext();) {
			  Element element = (Element) iter.next();
			  fg.addFilterSet(getFilterSet(element));
		}
		  
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filtercollection)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fg.addFilterCollection(getFilterCollection(element));
		}

		return fg;
	}

    private FilterSet getFilterSet(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String typeval = thisElement.getAttributeValue(type, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");

        FilterSet fs = new FilterSet(intName, typeval, dispname, desc);
        for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filterSetDescription)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fs.addFilterSetDescription(getFilterSetDescription(element));
		}
        return fs;
    }
    
    private FilterSetDescription getFilterSetDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String typeval = thisElement.getAttributeValue(type, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");
        String tableConstraintModifier = thisElement.getAttributeValue(tableConstraintModifierA, "");
        String fieldNameModifier = thisElement.getAttributeValue(fieldNameModifierA, "");
        
		FilterSetDescription fsd = new FilterSetDescription(intName, tableConstraintModifier, fieldNameModifier, dispname, desc);
    	
    	return fsd;
    }
    
	private FilterCollection getFilterCollection(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String filterSetName = thisElement.getAttributeValue(filterSetNameA, "");		
		String desc = thisElement.getAttributeValue(description, "");
		String typeval = thisElement.getAttributeValue(type, "");

		FilterCollection fc = new FilterCollection(intName, typeval, dispname, filterSetName, desc);
		for (Iterator iter = thisElement.getDescendants(new MartFilterDescriptionFilter()); iter.hasNext();) {
			Element element = (Element) iter.next();
			if (element.getName().equals(filterdescription))
			  fc.addUIFilter(getUIFilterDescription(element));
			else if (element.getName().equals(dsfilterdescription))
			  fc.addUIDSFilterDescription(getUIDSFilterDescription(element));
		}

		return fc;
	}

	private UIDSFilterDescription getUIDSFilterDescription(Element element) throws ConfigurationException {
		String intName = element.getAttributeValue(internalName, "");
		String dispname = element.getAttributeValue(displayName, "");
		String desc = element.getAttributeValue(description, "");
		String typeval = element.getAttributeValue(type, "");
    int objCode = 0;
    if (! element.getAttributeValue(objectCode, "").equals(""))
      objCode = Integer.parseInt( element.getAttributeValue(objectCode) );

		UIDSFilterDescription f = new UIDSFilterDescription(intName, typeval, objCode, dispname, desc);

		return f;
	}

	private UIFilterDescription getUIFilterDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");
		String typeval = thisElement.getAttributeValue(type, "");
		String fieldnm = thisElement.getAttributeValue(fieldName, "");
		String tableconst = thisElement.getAttributeValue(tableConstraint, "");
		
		int filterSetReq = 0;
		if ( ! thisElement.getAttributeValue(filterSetReqA, "").equals("") )
		  filterSetReq = Integer.parseInt( thisElement.getAttributeValue(filterSetReqA) );
		
		String qual = thisElement.getAttributeValue(qualifier, "");

		UIFilterDescription f = new UIFilterDescription(intName, fieldnm, typeval, qual, dispname, tableconst, filterSetReq, desc);

		return f;
	}

	private AttributePage getAttributePage(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");

		AttributePage ap = new AttributePage(intName, dispname, desc);
		
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributegroup)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ap.addAttributeGroup(getAttributeGroup(element));
		}

		return ap;
	}

	private AttributeGroup getAttributeGroup(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");

		AttributeGroup ag = new AttributeGroup(intName, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributecollection)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ag.addAttributeCollection(getAttributeCollection(element));
		}

		return ag;
	}

	private AttributeCollection getAttributeCollection(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");
		int maxs = 0;
		if (! thisElement.getAttributeValue(maxSelect, "").equals(""))
			maxs = Integer.parseInt(thisElement.getAttributeValue(maxSelect));

		AttributeCollection ac = new AttributeCollection(intName, maxs, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributedescription)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ac.addUIAttribute(getUIAttributeDescription(element));
		}

		return ac;
	}

	private UIAttributeDescription getUIAttributeDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName, "");
		String dispname = thisElement.getAttributeValue(displayName, "");
		String desc = thisElement.getAttributeValue(description, "");
		int maxl = 0;
		if (! thisElement.getAttributeValue(maxLength, "").equals(""))
			maxl = Integer.parseInt(thisElement.getAttributeValue(maxLength));

		String fieldnm = thisElement.getAttributeValue(fieldName, "");
		String tableconst = thisElement.getAttributeValue(tableConstraint, "");
		String src = thisElement.getAttributeValue(source, "");
		String hpage = thisElement.getAttributeValue(homepageURL, "");
		String link = thisElement.getAttributeValue(linkoutURL, "");

		UIAttributeDescription a = new UIAttributeDescription(intName, fieldnm, dispname, maxl, tableconst, desc, src, hpage, link);
		return a;
	}
}
