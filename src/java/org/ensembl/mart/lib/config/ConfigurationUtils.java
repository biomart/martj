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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

/**
 * Utility class containing all necessary XML parsing logic for converting
 * between XML and Object.  Uses JDOM as its XML parsing engine.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class ConfigurationUtils {

	private static Logger logger = Logger.getLogger(ConfigurationUtils.class.getName());

	// element names
	private static final String DATASET = "DatasetView";
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
	private static final String PUSHOPTIONS = "PushOptions";
	private static final String DEFAULTFILTER = "DefaultFilter";

	// attribute names
	private static final String INTERNALNAME = "internalName";
	private static final String DISPLAYNAME = "displayName";
	private static final String DESCRIPTION = "description";
	private static final String TYPE = "type";
	private static final String FIELD = "field";
	private static final String QUALIFIER = "qualifier";
	private static final String LEGALQUALIFIERS = "legal_qualifiers";
	private static final String TABLECONSTRAINT = "tableConstraint";
	private static final String MAXSELECT = "maxSelect";
	private static final String MAXLENGTH = "maxLength";
	private static final String SOURCE = "source";
	private static final String HOMEPAGEURL = "homepageURL";
	private static final String LINKOUTURL = "linkoutURL";
	private static final String HANDLER = "handler";
	private static final String ISSELECTABLE = "isSelectable";
	private static final String VALUE = "value";
	private static final String REF = "ref";
	private static final String VALUECONDITION = "valueCondition";

	public static DatasetView getDatasetView(InputStream xmlinput, boolean validate) throws ConfigurationException {
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(true); // validate against the DTD
			// set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.
			builder.setEntityResolver(new MartDTDEntityResolver());
      builder.setValidation(validate);
      
			InputSource is = new InputSource(xmlinput);

			Document doc = builder.build(is);

			return createDatasetView(doc);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	private static DatasetView createDatasetView(Document doc) throws ConfigurationException {
    
    Element thisElement = doc.getRootElement();
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		DatasetView d = new DatasetView(intName, dispname, desc);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			Element option = (Element) iter.next();
			d.addOption(getOption(option));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(DEFAULTFILTER)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addDefaultFilter(getDefaultFilter(element));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(STARBASE)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addStarBase(element.getTextNormalize());
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(PRIMARYKEY)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addPrimaryKey(element.getTextNormalize());
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(FILTERPAGE)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addFilterPage(getFilterPage(element));
		}

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(ATTRIBUTEPAGE)); iter.hasNext();) {
			Element element = (Element) iter.next();
			d.addAttributePage(getAttributePage(element));
		}

		// we need to manually set the "parent" references on these options
		// so they are availbe for future use.
		List fds = d.getAllFilterDescriptions();
		for (Iterator iter = fds.iterator(); iter.hasNext();) {
			FilterDescription fd = (FilterDescription) iter.next();
			fd.setParentsForAllPushOptionOptions(d);
		}

		return d;
	}

	private static DefaultFilter getDefaultFilter(Element thisElement) throws ConfigurationException {
		FilterDescription desc = getFilterDescription(thisElement.getChildElement(FILTERDESCRIPTION));
		String value = thisElement.getAttributeValue(VALUE);
		return new DefaultFilter(desc, value);
	}

	private static FilterPage getFilterPage(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		FilterPage fp = new FilterPage(intName, dispname, desc);
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
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		FilterGroup fg = new FilterGroup(intName, dispname, desc);

		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(FILTERCOLLECTION)); iter.hasNext();) {
			Element element = (Element) iter.next();
			fg.addFilterCollection(getFilterCollection(element));
		}

		return fg;
	}

	private static DSFilterGroup getDSFilterGroup(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String handler = thisElement.getAttributeValue(HANDLER, "");

		DSFilterGroup fg = new DSFilterGroup(intName, dispname, desc, handler);

		return fg;
	}

	private static FilterCollection getFilterCollection(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		FilterCollection fc = new FilterCollection(intName, dispname, desc);

		for (Iterator iter = thisElement.getDescendants(new MartFilterDescriptionFilter()); iter.hasNext();) {
			Element element = (Element) iter.next();
			fc.addFilterDescription(getFilterDescription(element));
		}

		return fc;
	}

	private static Option getOption(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");

		String isSelectableString = thisElement.getAttributeValue(ISSELECTABLE, "");

		boolean isSelectable = Boolean.valueOf(isSelectableString).booleanValue();
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String field = thisElement.getAttributeValue(FIELD, "");
		String tableConstraint = thisElement.getAttributeValue(TABLECONSTRAINT, "");
		String type = thisElement.getAttributeValue(TYPE, "");
		String qualifier = thisElement.getAttributeValue(QUALIFIER, "");
		String lquals = thisElement.getAttributeValue(LEGALQUALIFIERS, "");
		String value = thisElement.getAttributeValue(VALUE, "");
		String ref = thisElement.getAttributeValue(REF, "");
		String handler = thisElement.getAttributeValue(HANDLER);

		Option o = new Option(intName, isSelectable, dispname, desc, field, tableConstraint, value, ref, type, qualifier, lquals, handler);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			Element suboption = (Element) iter.next();
			Option o2 = getOption(suboption);
			o2.setParent(o);
			o.addOption(o2);
		}

		for (Iterator iter = thisElement.getChildElements(PUSHOPTIONS).iterator(); iter.hasNext();) {
			o.addPushAction(getPushOptions((Element) iter.next()));
		}

		return o;
	}

	private static PushAction getPushOptions(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String ref = thisElement.getAttributeValue(REF, "");

		PushAction op = new PushAction(intName, dispname, desc, ref);

		for (Iterator iter = thisElement.getChildElements(OPTION).iterator(); iter.hasNext();) {
			op.addOption(getOption((Element) iter.next()));
		}

		return op;
	}

	private static FilterDescription getFilterDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String typeval = thisElement.getAttributeValue(TYPE, "");
		String fieldnm = thisElement.getAttributeValue(FIELD, "");
		String tableconst = thisElement.getAttributeValue(TABLECONSTRAINT, "");
		String handler = thisElement.getAttributeValue(HANDLER);
		String qualifier = thisElement.getAttributeValue(QUALIFIER);
		String lquals = thisElement.getAttributeValue(LEGALQUALIFIERS, "");

		FilterDescription f = new FilterDescription(intName, fieldnm, typeval, qualifier, lquals, dispname, tableconst, handler, desc);

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
		String ref = thisElement.getAttributeValue(REF, "");
		String valueCondition = thisElement.getAttributeValue(VALUECONDITION, "");
		return new Enable(ref, valueCondition);
	}

	private static Disable getDisable(Element thisElement) throws ConfigurationException {
		String ref = thisElement.getAttributeValue(REF, "");
		String valueCondition = thisElement.getAttributeValue(VALUECONDITION, "");
		return new Disable(ref, valueCondition);
	}

	private static AttributePage getAttributePage(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		AttributePage ap = new AttributePage(intName, dispname, desc);

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
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");

		AttributeGroup ag = new AttributeGroup(intName, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(ATTRIBUTECOLLECTION)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ag.addAttributeCollection(getAttributeCollection(element));
		}

		return ag;
	}

	private static DSAttributeGroup getDSAttributeGroup(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		String objCode = thisElement.getAttributeValue(HANDLER, "");

		DSAttributeGroup ag = new DSAttributeGroup(intName, dispname, desc, objCode);

		return ag;
	}

	private static AttributeCollection getAttributeCollection(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		int maxs = 0;
		if (!thisElement.getAttributeValue(MAXSELECT, "").equals(""))
			maxs = Integer.parseInt(thisElement.getAttributeValue(MAXSELECT));

		AttributeCollection ac = new AttributeCollection(intName, maxs, dispname, desc);
		for (Iterator iter = thisElement.getDescendants(new MartElementFilter(ATTRIBUTEDESCRIPTION)); iter.hasNext();) {
			Element element = (Element) iter.next();
			ac.addAttributeDescription(getUIAttributeDescription(element));
		}

		return ac;
	}

	private static AttributeDescription getUIAttributeDescription(Element thisElement) throws ConfigurationException {
		String intName = thisElement.getAttributeValue(INTERNALNAME, "");
		String dispname = thisElement.getAttributeValue(DISPLAYNAME, "");
		String desc = thisElement.getAttributeValue(DESCRIPTION, "");
		int maxl = 0;
		if (!thisElement.getAttributeValue(MAXLENGTH, "").equals(""))
			maxl = Integer.parseInt(thisElement.getAttributeValue(MAXLENGTH));

		String fieldnm = thisElement.getAttributeValue(FIELD, "");
		String tableconst = thisElement.getAttributeValue(TABLECONSTRAINT, "");
		String src = thisElement.getAttributeValue(SOURCE, "");
		String hpage = thisElement.getAttributeValue(HOMEPAGEURL, "");
		String link = thisElement.getAttributeValue(LINKOUTURL, "");

		AttributeDescription a = new AttributeDescription(intName, fieldnm, dispname, maxl, tableconst, desc, src, hpage, link);
		return a;
	}
}
