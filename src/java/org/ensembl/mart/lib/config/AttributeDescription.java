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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains all of the information required by a UI to display a specific attribute, and create an Attribute object to add to a mart Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributeDescription extends BaseNamedConfigurationObject {

  private Logger logger = Logger.getLogger(AttributeDescription.class.getName());
  
  /**
   * The default maxLength is 10
   */
  public final int DEFAULTMAXLENGTH = 10;
  
	private final String fieldKey = "field";
	private final String tableConstraintKey = "tableConstraint";
	private final String sourceKey = "source";
	private final String homepageURLKey = "homepageURL";
	private final String linkoutURLKey = "linkoutURL";
	private final String maxLengthKey = "maxLength";
	// helper field so that only setter/constructors will throw ConfigurationExceptions when string values are converted to integers

	private boolean hasBrokenField = false;
	private boolean hasBrokenTableConstraint = false;

	/**
	 * Copy constructor. Constructs an exact copy of an existing AttributeDescription.
	 * @param a AttributeDescription to copy.
	 */
	public AttributeDescription(AttributeDescription a) {
		super(a);
	}

	/**
	 * Empty Constructor should only be used by DatasetViewEditor
	 *
	 */
	public AttributeDescription() {
		super();
	}

	/**
	 * Constructs a AttributeDescription with just the internalName and field.
	 * 
	 * @param internalName String name to internally represent the AttributeDescription. Must not be null or empty
	 * @param field String name of the field in the mart for this Attribute. Must not be null or empty.
	 * @throws ConfigurationException when values are null or empty.
	 */
	public AttributeDescription(String internalName, String field) throws ConfigurationException {
		this(internalName, field, "", "0", "", "", "", "", "");
	}
	/**
	 * Constructor for an AttributeDescription.
	 * 
	 * @param internalName String name to internally represent the AttributeDescription. Must not be null or empty.
	 * @param field String name of the field in the mart for this attribute.  Must not be null or empty.
	 * @param displayName String name of the AttributeDescription.
	 * @param maxLength Int maximum possible length of the field in the mart.
	 * @param tableConstraint String base name of a specific table containing this UIAttribute.
	 * @param description String description of this UIAttribute.
	 * @param source String source for the data for this UIAttribute.
	 * @param homePageURL String Web Homepage for the source.
	 * @param linkoutURL String Base for a link to a specific entry in a source website.
	 * @throws ConfigurationException when required parameters are null or empty
	 */
	public AttributeDescription(
		String internalName,
		String field,
		String displayName,
		String maxLength,
		String tableConstraint,
		String description,
		String source,
		String homePageURL,
		String linkoutURL)
		throws ConfigurationException {

		super(internalName, displayName, description);

		if (field == null || field.equals(""))
			throw new ConfigurationException("UIAttributeDescriptions require a field");

		setAttribute(fieldKey, field);

		setMaxLength(maxLength);
		setAttribute(tableConstraintKey, tableConstraint);
		setAttribute(sourceKey, source);
		setAttribute(homepageURLKey, homePageURL);
		setAttribute(linkoutURLKey, linkoutURL);
	}

	/**
	 * @param homePageURL - url to homepage for the data source
	 */
	public void setHomepageURL(String homePageURL) {
		setAttribute(homepageURLKey, homePageURL);
	}

	/**
	 * @return homepageURL
	 */
	public String getHomepageURL() {
		return getAttribute(homepageURLKey);
	}

	/**
	 * @param tableConstraint - tableConstraint for the field
	 */
	public void setTableConstraint(String tableConstraint) {
		setAttribute(tableConstraintKey, tableConstraint);
	}

	/**
	 * Returns the TableConstraint.
	 * 
	 * @return String tableConstraint.
	 */
	public String getTableConstraint() {
		return getAttribute(tableConstraintKey);
	}

	/**
	 * @param field - field in mart table
	 */
	public void setField(String field) {
		setAttribute(fieldKey, field);
	}

	/**
	 * Returns the field.
	 * 
	 * @return String field
	 */
	public String getField() {
		return getAttribute(fieldKey);
	}

	/**
	 * @param maxLength - String maximum length of the table field
	 * @throws ConfigurationException for underlying numberFormatException when maxLength is parsed to an integer
	 */
	public void setMaxLength(String maxLength) throws ConfigurationException {
		setAttribute(maxLengthKey, maxLength);
	}

	/**
	 * Returns the maxLength. If the value for maxLength
	 * is not a valid integer (eg, a NumberFormatException is
	 * thrown by Integer.parseInt( maxLength )) this method will
	 * return DEFAULTMAXLENGTH
	 * 
	 * @return int MaxLength.
	 */
	public int getMaxLength()  {
		try {
			return Integer.parseInt(getAttribute(maxLengthKey));
		} catch (NumberFormatException e) {
			if (logger.isLoggable(Level.WARNING))
			  logger.warning("Could not parse maxLength value to integer: " + e.getMessage());
			return DEFAULTMAXLENGTH;
		}
	}

	/**
	 * @param source - String name of data source
	 */
	public void setSource(String source) {
		setAttribute(sourceKey, source);
	}

	/**
	 * Returns the source.
	 * 
	 * @return String source
	 */
	public String getSource() {
		return getAttribute(sourceKey);
	}

	/**
	 * @param LinkoutURL - String base for HTML link references
	 */
	public void setLinkoutURL(String linkoutURL) {
		setAttribute(linkoutURLKey, linkoutURL);
	}

	/**
	 * Returns the linkoutURL.
	 * @return String linkoutURL.
	 */
	public String getLinkoutURL() {
		return getAttribute(linkoutURLKey);
	}

	/**
	 * Determine if this AttributeDescription supports a given field and tableConstraint. Useful for mapping Query Attribute Objects
	 * back to their corresponding MartConfiguration AttributeDescription.
	 * @param field -- String field of the mart datbase table
	 * @param tableConstraint -- String constraining the field to a particular table or table type
	 * @return boolean, true if given field and given tableConstraint matches underlying values for AttributeDescription 
	 */
	public boolean supports(String field, String tableConstraint) {
		return getAttribute(fieldKey) != null
			&& getAttribute(fieldKey).equals(field)
			&& getAttribute(tableConstraintKey) != null
			&& getAttribute(tableConstraintKey).equals(tableConstraint);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[ AttributeDescription:");
		buf.append(super.toString());
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons of AttributeDescription objects
	 */
	public boolean equals(Object o) {
		return o instanceof AttributeDescription && hashCode() == ((AttributeDescription) o).hashCode();
	}

	/**
	 * set the hasBrokenField flag to true, eg. the field
	 * does not refer to an existing field in a particular Mart Dataset instance.
	 *
	 */
	public void setFieldBroken() {
		hasBrokenField = true;
	}

	/**
	 * Determine if this AttributeDescription has a broken field reference.
	 * @return boolean, true if field is broken, false otherwise
	 */
	public boolean hasBrokenField() {
		return hasBrokenField;
	}

	/**
	 * set the hasBrokenTableConstraint flag to true, eg. the tableConstraint
	 * does not refer to an existing table in a particular Mart Dataset instance.
	 *
	 */
	public void setTableConstraintBroken() {
		hasBrokenTableConstraint = true;
	}

	/**
	 * Determine if this AttributeDescription has a broken tableConstraint reference.
	 * @return boolean, true if tableConstraint is broken, false otherwise
	 */
	public boolean hasBrokenTableConstraint() {
		return hasBrokenTableConstraint;
	}

	/**
	 * True if one of hasBrokenField or hasBrokenTableConstraint is true.
	 * @return boolean
	 */
	public boolean isBroken() {
		return hasBrokenField || hasBrokenTableConstraint;
	}
}
