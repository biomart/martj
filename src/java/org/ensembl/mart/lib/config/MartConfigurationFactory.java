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

import java.net.URL;
import java.sql.Connection;
import java.util.Iterator;

import org.apache.log4j.Logger;
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

	private Logger logger =
		Logger.getLogger(MartConfigurationFactory.class.getName());
	private String martConfSystemID = "MartConfiguration.xml";
	// default, but can be over-ridden

	// element names
	private final String DATASET = "Dataset";
	private final String STARBASE = "StarBase";
	private final String PRIMARYKEY = "PrimaryKey";
	private final String FILTERSET = "FilterSet";
	private final String FILTERSETDESCRIPTION = "FilterSetDescription";
	private final String FILTERPAGE = "FilterPage";
	private final String FILTERGROUP = "FilterGroup";
	private final String DSFILTERGROUP = "DSFilterGroup";
	private final String FILTERCOLLECTION = "FilterCollection";
	private final String FILTERDESCRIPTION = "FilterDescription";
	private final String DSFILTERDESCRIPTION = "MapFilterDescription";
	private final String ATTRIBUTEPAGE = "AttributePage";
	private final String ATTRIBUTEGROUP = "AttributeGroup";
	private final String ATTRIBUTECOLLECTION = "AttributeCollection";
	private final String ATTRIBUTEDESCRIPTION = "AttributeDescription";
	private final String DSATTRIBUTEGROUP = "DSAttributeGroup";
	private final String OPTION = "Option";
  private final String OPTIONPUSH = "OptionPush";
	private final String DEFAULTFILTER = "DefaultFilter";

	// attribute names
	private final String INTERNALNAME = "internalName";
	private final String DISPLAYNAME = "displayName";
	private final String DESCRIPTION = "description";
	private final String TYPE = "type";
	private final String FIELDNAME = "fieldName";
	private final String QUALIFIER = "qualifier";
	private final String TABLECONSTRAINT = "tableConstraint";
	private final String MAXSELECT = "maxSelect";
	private final String MAXLENGTH = "maxLength";
	private final String SOURCE = "source";
	private final String HOMEPAGEURL = "homepageURL";
	private final String LINKOUTURL = "linkoutURL";
	private final String TABLECONSTRAINTMODIFIER = "tableConstraintModifier";
	private final String FIELDNAMEMODIFIER = "fieldNameModifier";
	private final String FILTERSETNAME = "filterSetName";
	private final String FILTERSETREQ = "filterSetReq";
	private final String HANDLER = "handler";
	private final String ISSELECTABLE = "isSelectable";
  private final String VALUE = "value";
  private final String REF = "ref";

	public MartConfiguration getInstance(Connection conn)
		throws ConfigurationException {

		return getInstance(conn, martConfSystemID);
	}

	/**
	 * Default getInstance method.  Fetches the MartConfiguration.xml document from the mart database named by martName.
	 * 
	 * @param conn - A java.sql.Connection object
	 * @param sytemID - id of config file to be retrieved from database
	 * 
	 * @return MartConfiguration object for the requested mart database
	 * @throws ConfigurationException.  Chains all Exceptions resulting from SQL, JDOM parsing, etc. into a ConfigurationException.
	 */
	public MartConfiguration getInstance(Connection conn, String systemID)
		throws ConfigurationException {

		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(true); // validate against the DTD
      // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.
      builder.setEntityResolver(new MartDTDEntityResolver(conn));
			
			Document doc =
				builder.build(MartXMLutils.getInputSourceFor(conn, systemID));
        
			return createMartConfiguration(doc);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}



  /**
   * Overloaded getInstance method allowing user to use an alternate xml configuration file stored on the file system, by
   * supplying a URL.  Note, this requires that the DTD for the supplied document be available on the file system as well.
   * Users should make sure that the DOCTYPE declaration correctly locates the DTD for their document.
   *  
   * @param martConfFile - URL for the MartConfiguration xml document.
   * 
   * @return MartConfiguration object for the mart defined by this document
   * @throws ConfigurationException.  Chains all Exceptions from URL, IO, etc. into ConfigurationExceptions
   */
  public MartConfiguration getInstance(URL martConfFile)
    throws ConfigurationException {

    try {
      SAXBuilder builder = new SAXBuilder();
      builder.setValidation(true); // validate against the DTD

      Document doc =
        builder.build(MartXMLutils.getInputSourceFor(martConfFile));

      return createMartConfiguration(doc);
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }



	/**
   * Converts the document into a MartConfiguration object.
	 * @param doc
	 * @return a MartConfiguration object derived from the document.
	 */
	private MartConfiguration createMartConfiguration(Document doc)
		throws ConfigurationException {
		Element martconfElement = doc.getRootElement();
		String martName = martconfElement.getAttributeValue(INTERNALNAME, "");

		String dispname = martconfElement.getAttributeValue(DISPLAYNAME, "");
		String desc = martconfElement.getAttributeValue(DESCRIPTION, "");

		MartConfiguration martconf =
			new MartConfiguration(martName, dispname, desc);

		for (Iterator iter =
			martconfElement.getDescendants(new MartElementFilter(DATASET));
			iter.hasNext();
			) {
			Element datasetElement = (Element) iter.next();
			martconf.addDataset(getDataset(datasetElement));
		}

    // Get the layout. THis is the first element of the mart configuration. THere is only one of these.
    Iterator iter = martconfElement.getDescendants( new MartElementFilter( FILTERDESCRIPTION ) );
    if ( !iter.hasNext() ) throw new ConfigurationException("Layout is missing from configuration file.");
    Element layoutElement = (Element) iter.next();
    martconf.setLayout( getUIFilterDescription( layoutElement ) );

		return martconf;
	}



	private Dataset getDataset(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		Dataset d = new Dataset(intName, dispname, desc);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator();
			iter.hasNext();
			) {
			Element option = (Element) iter.next();
			d.addOption(getOption(option));
		}

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(DEFAULTFILTER));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			d.addDefaultFilter(getDefaultFilter(element));
		}

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(STARBASE));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			d.addStarBase(element.getTextNormalize());
		}

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(PRIMARYKEY));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			d.addPrimaryKey(element.getTextNormalize());
		}

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(FILTERPAGE));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			d.addFilterPage(getFilterPage(element));
		}

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(ATTRIBUTEPAGE));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			d.addAttributePage(getAttributePage(element));
		}
		return d;
	}

	private DefaultFilter getDefaultFilter(Element thisElement)
		throws ConfigurationException {
		FilterDescription desc =
			getUIFilterDescription(thisElement.getChildElement(FILTERDESCRIPTION));
		String value = thisElement.getAttributeValue(VALUE);
		return new DefaultFilter(desc, value);
	}

	private FilterPage getFilterPage(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		FilterPage fp = new FilterPage(intName, dispname, desc);
		for (Iterator iter =
			thisElement.getDescendants(new MartFilterGroupFilter());
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			if (element.getName().equals(FILTERGROUP))
				fp.addFilterGroup(getFilterGroup(element));
			else
				fp.addDSFilterGroup(getDSFilterGroup(element));
		}

		return fp;
	}

	private FilterGroup getFilterGroup(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		FilterGroup fg = new FilterGroup(intName, dispname, desc);

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(FILTERSET));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			fg.addFilterSet(getFilterSet(element));
		}

		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(FILTERCOLLECTION));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			fg.addFilterCollection(getFilterCollection(element));
		}

		return fg;
	}

	private DSFilterGroup getDSFilterGroup(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String objCode = thisElement.getAttributeValue(HANDLER, "");

		DSFilterGroup fg = new DSFilterGroup(intName, dispname, desc, objCode);

		return fg;
	}

	private FilterSet getFilterSet(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String typeval = thisElement.getAttributeValue(TYPE, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		FilterSet fs = new FilterSet(intName, typeval, dispname, desc);
		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(FILTERSETDESCRIPTION));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			fs.addFilterSetDescription(getFilterSetDescription(element));
		}
		return fs;
	}

	private FilterSetDescription getFilterSetDescription(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String typeval = thisElement.getAttributeValue(TYPE, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String tableConstraintModifier =
			thisElement.getAttributeValue(TABLECONSTRAINTMODIFIER, "");
		String fieldNameModifier =
			thisElement.getAttributeValue(FIELDNAMEMODIFIER, "");

		FilterSetDescription fsd =
			new FilterSetDescription(
				intName,
				tableConstraintModifier,
				fieldNameModifier,
				dispname,
				desc);

		return fsd;
	}

	private FilterCollection getFilterCollection(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String filterSetName = thisElement.getAttributeValue(FILTERSETNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String typeval = thisElement.getAttributeValue(TYPE, "");

		FilterCollection fc =
			new FilterCollection(intName, typeval, dispname, filterSetName, desc);
		for (Iterator iter =
			thisElement.getDescendants(new MartFilterDescriptionFilter());
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			if (element.getName().equals(FILTERDESCRIPTION))
				fc.addUIFilter(getUIFilterDescription(element));
			else if (element.getName().equals(DSFILTERDESCRIPTION))
				fc.addUIDSFilterDescription(getUIDSFilterDescription(element));
		}

		return fc;
	}

	private Option getOption(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");

		String isSelectableString = thisElement.getAttributeValue(ISSELECTABLE, "");

		boolean isSelectable = Boolean.valueOf(isSelectableString).booleanValue();
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
    String field = thisElement.getAttributeValue(FIELDNAME, "");
    String tableConstraint = thisElement.getAttributeValue(TABLECONSTRAINT, "");
    String value = thisElement.getAttributeValue(VALUE, "");
    String ref = thisElement.getAttributeValue(REF, "");

		Option o = new Option(intName, isSelectable, dispname, desc, field, tableConstraint, value, ref );

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator();
			iter.hasNext();
			) {
			Element suboption = (Element) iter.next();
			o.addOption(getOption(suboption));
		}

    for (Iterator iter = thisElement.getChildElements(OPTIONPUSH).iterator();
          iter.hasNext();
          ) {
          o.addOptionPush( getOptionPush( (Element) iter.next() ) );
        }

		return o;
	}

	/**
   * @param suboption
   */
  private OptionPush getOptionPush(Element thisElement)  throws ConfigurationException {
    // TODO Auto-generated method stub
    String intName = thisElement.getAttributeValue(INTERNALNAME, "");
    String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
    String desc = thisElement.getAttributeValue(DESCRIPTION, "");
    String ref = thisElement.getAttributeValue(REF, "");
        
    OptionPush op = new OptionPush( intName, dispname, desc, ref);
    
    for (Iterator iter = thisElement.getChildElements(OPTION).iterator();
      iter.hasNext();
      ) {
      op.addOption( getOption( (Element) iter.next() ) );
    }
    
    return op;
  }

  private MapFilterDescription getUIDSFilterDescription(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String typeval = thisElement.getAttributeValue(TYPE, "");
		String objCode = thisElement.getAttributeValue(HANDLER, "");
		String filterSetReq = thisElement.getAttributeValue(FILTERSETREQ, "");

		MapFilterDescription f =
			new MapFilterDescription(
				intName,
				typeval,
				objCode,
				filterSetReq,
				dispname,
				desc);

		return f;
	}

	private FilterDescription getUIFilterDescription(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String typeval = thisElement.getAttributeValue(TYPE, "");
		String fieldnm = thisElement.getAttributeValue(FIELDNAME, "");
		String tableconst = thisElement.getAttributeValue(TABLECONSTRAINT, "");

		String filterSetReq = thisElement.getAttributeValue(FILTERSETREQ, "");

		String qual = thisElement.getAttributeValue(QUALIFIER, "");

		FilterDescription f =
			new FilterDescription(
				intName,
				fieldnm,
				typeval,
				qual,
				dispname,
				tableconst,
				filterSetReq,
				desc);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator();
			iter.hasNext();
			) {
			Element option = (Element) iter.next();
			f.addOption(getOption(option));
		}

		return f;
	}

	private AttributePage getAttributePage(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		AttributePage ap = new AttributePage(intName, dispname, desc);

		for (Iterator iter =
			thisElement.getDescendants(new MartAttributeGroupFilter());
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			if (element.getName().equals(ATTRIBUTEGROUP))
				ap.addAttributeGroup(getAttributeGroup(element));
			else
				ap.addDSAttributeGroup(getDSAttributeGroup(element));
		}

		return ap;
	}

	private AttributeGroup getAttributeGroup(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		AttributeGroup ag = new AttributeGroup(intName, dispname, desc);
		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(ATTRIBUTECOLLECTION));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			ag.addAttributeCollection(getAttributeCollection(element));
		}

		return ag;
	}

	private DSAttributeGroup getDSAttributeGroup(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String objCode = thisElement.getAttributeValue(HANDLER, "");

		DSAttributeGroup ag =
			new DSAttributeGroup(intName, dispname, desc, objCode);

		return ag;
	}

	private AttributeCollection getAttributeCollection(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		int maxs = 0;
		if (!thisElement.getAttributeValue(MAXSELECT, "").equals(""))
			maxs = Integer.parseInt(thisElement.getAttributeValue(MAXSELECT));

		AttributeCollection ac =
			new AttributeCollection(intName, maxs, dispname, desc);
		for (Iterator iter =
			thisElement.getDescendants(new MartElementFilter(ATTRIBUTEDESCRIPTION));
			iter.hasNext();
			) {
			Element element = (Element) iter.next();
			ac.addUIAttribute(getUIAttributeDescription(element));
		}

		return ac;
	}

	private AttributeDescription getUIAttributeDescription(Element thisElement)
		throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		int maxl = 0;
		if (!thisElement.getAttributeValue(MAXLENGTH, "").equals(""))
			maxl = Integer.parseInt(thisElement.getAttributeValue(MAXLENGTH));

		String fieldnm = thisElement.getAttributeValue(FIELDNAME, "");
		String tableconst = thisElement.getAttributeValue(TABLECONSTRAINT, "");
		String src = thisElement.getAttributeValue(SOURCE, "");
		String hpage = thisElement.getAttributeValue(HOMEPAGEURL, "");
		String link = thisElement.getAttributeValue(LINKOUTURL, "");

		AttributeDescription a =
			new AttributeDescription(
				intName,
				fieldnm,
				dispname,
				maxl,
				tableconst,
				desc,
				src,
				hpage,
				link);
		return a;
	}
}
