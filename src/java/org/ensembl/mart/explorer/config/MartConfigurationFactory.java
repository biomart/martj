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

import java.sql.Connection;
import java.util.Iterator;

import org.jdom.*;
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

	public MartConfiguration getInstance(Connection conn, String martName) throws ConfigurationException {

		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(true); // validate against the DTD
			builder.setEntityResolver(new MartDTDEntityResolver(conn)); // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.

			Document doc = builder.build(MartXMLutils.getInputSourceFor(conn, martConfSystemID));

			Element martconfElement = doc.getRootElement();
			String mname = martconfElement.getAttributeValue(internalName);
			if (!mname.equals(martName))
				throw new ConfigurationException(
					"Warning, xml from " + martName + " contains different internalName " + mname + " may need to load a different xml into the mart database");

			String dispname = martconfElement.getAttributeValue(displayName);
			String desc = martconfElement.getAttributeValue(description);

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
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);

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
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);

		FilterPage fp = new FilterPage(intName, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filtergroup)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fp.addFilterGroup(getFilterGroup(element));
		}

		return fp;
	}

	private FilterGroup getFilterGroup(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);

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
		String intName = thisElement.getAttributeValue(internalName);
		String typeval = thisElement.getAttributeValue(type);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);

        FilterSet fs = new FilterSet(intName, typeval, dispname, desc);
        for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filterSetDescription)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fs.addFilterSetDescription(getFilterSetDescription(element));
		}
        return fs;
    }
    
    private FilterSetDescription getFilterSetDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String typeval = thisElement.getAttributeValue(type);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);
        String tableConstraintModifier = thisElement.getAttributeValue(tableConstraintModifierA);
        String fieldNameModifier = thisElement.getAttributeValue(fieldNameModifierA);
        
		FilterSetDescription fsd = new FilterSetDescription(intName, tableConstraintModifier, fieldNameModifier, dispname, desc);
    	
    	return fsd;
    }
    
	private FilterCollection getFilterCollection(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String filterSetName = thisElement.getAttributeValue(filterSetNameA);		
		String desc = thisElement.getAttributeValue(description);
		String typeval = thisElement.getAttributeValue(type);

		FilterCollection fc = new FilterCollection(intName, typeval, dispname, filterSetName, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(filterdescription)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fc.addUIFilter(getUIFilterDescription(element));
		}

		return fc;
	}

	private UIFilterDescription getUIFilterDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);
		String typeval = thisElement.getAttributeValue(type);
		String fieldnm = thisElement.getAttributeValue(fieldName);
		String tableconst = thisElement.getAttributeValue(tableConstraint);
		
		int filterSetReq = 0;
		if ( thisElement.getAttributeValue(filterSetReqA) != null )
		  filterSetReq = Integer.parseInt( thisElement.getAttributeValue(filterSetReqA) );
		
		String qual = thisElement.getAttributeValue(qualifier);

		UIFilterDescription f = new UIFilterDescription(intName, fieldnm, typeval, qual, dispname, tableconst, filterSetReq, desc);

		return f;
	}

	private AttributePage getAttributePage(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);

		AttributePage ap = new AttributePage(intName, dispname, desc);
		
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributegroup)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ap.addAttributeGroup(getAttributeGroup(element));
		}

		return ap;
	}

	private AttributeGroup getAttributeGroup(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);

		AttributeGroup ag = new AttributeGroup(intName, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributecollection)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ag.addAttributeCollection(getAttributeCollection(element));
		}

		return ag;
	}

	private AttributeCollection getAttributeCollection(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);
		int maxs = 0;
		if (thisElement.getAttributeValue(maxSelect) != null)
			maxs = Integer.parseInt(thisElement.getAttributeValue(maxSelect));

		AttributeCollection ac = new AttributeCollection(intName, maxs, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(attributedescription)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ac.addUIAttribute(getUIAttributeDescription(element));
		}

		return ac;
	}

	private UIAttributeDescription getUIAttributeDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(internalName);
		String dispname = thisElement.getAttributeValue(displayName);
		String desc = thisElement.getAttributeValue(description);
		int maxl = 0;
		if (thisElement.getAttributeValue(maxLength) != null)
			maxl = Integer.parseInt(thisElement.getAttributeValue(maxLength));

		String fieldnm = thisElement.getAttributeValue(fieldName);
		String tableconst = thisElement.getAttributeValue(tableConstraint);
		String src = thisElement.getAttributeValue(source);
		String hpage = thisElement.getAttributeValue(homepageURL);
		String link = thisElement.getAttributeValue(linkoutURL);

		UIAttributeDescription a = new UIAttributeDescription(intName, fieldnm, dispname, maxl, tableconst, desc, src, hpage, link);
		return a;
	}
}
