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

/**
 * Contains all of the information required by a UI to display a specific attribute, and create an Attribute object to add to a mart Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributeDescription extends BaseConfigurationObject {

  private String field;
  private String tableConstraint;
  private String source;
  private String homepageURL;
  private String linkoutURL;
  private int maxLength = 0;
  
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
		this(internalName, field, "", 0, "", "", "", "", "");
	}
	/**
	 * Constructor for a AttributeDescription.
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
		int maxLength,
		String tableConstraint,
		String description,
		String source,
		String homePageURL,
		String linkoutURL)
		throws ConfigurationException {
		
    super( internalName, displayName, description );
    
    if ( field == null || field.equals(""))
			throw new ConfigurationException("UIAttributeDescriptions require a field");

		this.field = field;
		this.maxLength = maxLength;
		this.tableConstraint = tableConstraint;
		this.source = source;
		this.homepageURL = homePageURL;
		this.linkoutURL = linkoutURL;
	}

  /**
   * @param string
   */
  public void setHomepageURL(String string) {
    homepageURL = string;
  }

  /**
   * @return
   */
  public String getHomepageURL() {
    return homepageURL;
  }

  /**
   * @param string
   */
  public void setTableConstraint(String string) {
    tableConstraint = string;
  }

  /**
   * Returns the TableConstraint.
   * 
   * @return String tableConstraint.
   */
  public String getTableConstraint() {
    return tableConstraint;
  }

  /**
   * @param string
   */
  public void setField(String string) {
    field = string;
  }
  
	/**
	 * Returns the field.
	 * 
	 * @return String field
	 */
	public String getField() {
		return field;
	}
 
  /**
   * @param i
   */
  public void setMaxLength(int i) {
    maxLength = i;
  }

	/**
	 * Returns the maxLength.
	 * 
	 * @return int MaxLength.
	 */
	public int getMaxLength() {
		return maxLength;
	}

  /**
   * @param string
   */
  public void setSource(String string) {
    source = string;
  }

	/**
	 * Returns the source.
	 * 
	 * @return String source
	 */
	public String getSource() {
		return source;
	}

  /**
   * @param string
   */
  public void setLinkoutURL(String string) {
    linkoutURL = string;
  }
  
	/**
	 * Returns the linkoutURL.
	 * @return String linkoutURL.
	 */
	public String getLinkoutURL() {
		return linkoutURL;
	}

  /**
   * Determine if this AttributeDescription supports a given field and tableConstraint. Useful for mapping Query Attribute Objects
   * back to their corresponding MartConfiguration AttributeDescription.
   * @param field -- String field of the mart datbase table
   * @param tableConstraint -- String constraining the field to a particular table or table type
   * @return boolean, true if given field and given tableConstraint matches underlying values for AttributeDescription 
   */
  public boolean supports(String field, String tableConstraint) {
  	return this.field != null && this.field.equals(field) && this.tableConstraint != null && this.tableConstraint.equals(tableConstraint);
  }
  
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[ AttributeDescription:");
		buf.append( super.toString() );
		buf.append(", field=").append(field);
		buf.append(", maxLength=").append(maxLength);
		buf.append(", tableConstraint=").append(tableConstraint);
		buf.append(", source=").append(source);
		buf.append(", homePageURL=").append(homepageURL);
		buf.append(", linkoutURL=").append(linkoutURL);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons of AttributeDescription objects
	 */
	public boolean equals(Object o) {
		return o instanceof AttributeDescription && hashCode() == ((AttributeDescription) o).hashCode();
	}

	public int hashCode() {
    int hshcode = internalName.hashCode();
    hshcode = (displayName != null) ? (31 * hshcode) + displayName.hashCode() : hshcode;
    hshcode = (field != null) ? (31 * hshcode) + field.hashCode() : hshcode;
    hshcode = (31 * hshcode) + maxLength;
    hshcode = (tableConstraint != null) ? (31 * hshcode) + tableConstraint.hashCode() : hshcode;
    hshcode = (description != null) ? (31 * hshcode) + description.hashCode() : hshcode;
    hshcode = (source != null) ? (31 * hshcode) + source.hashCode() : hshcode;
    hshcode = (homepageURL != null) ? (31 * hshcode) + homepageURL.hashCode() : hshcode;
    hshcode = (linkoutURL != null) ? (31 * hshcode) + linkoutURL.hashCode() : hshcode;
		return hshcode;
	}
}
