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

/**
 * Description of a Domain Specific Attribute made available by a specific instance
 * of a mart database.  Domain Specific Attributes are attributes, or entities that
 * do not map to the normal main/dimension table fields.  They should be handled with
 * a special Query object, and QueryRunner designed to output this data.
 *  
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class UIDSAttributeDescription {

  /**
   * this will throw an exception.
   * @throws ConfigurationException
   */
  public UIDSAttributeDescription() throws ConfigurationException {
  	this("", "", "", "", "", "", "");
  }
  
  /**
   * Constructor for a minimally defined UIDSFilterDescription, with internalName and objectCode.
   * 
   * @param internalName - String name to internally represent the UIDSFilterDescription
   * @param objectCode - String name used by the UI when resolving an object to add to a Query for this Attribute.
   * @throws ConfigurationException when either are null or empty.
   */
  public UIDSAttributeDescription(String internalName, String objectCode) throws ConfigurationException {
  	this(internalName, objectCode, "", "", "", "", "");
  }
  
  /**
   * Constructor for a fully qualified UIDSFilterDescription
   * 
   * @param internalName - String name to internally represent the UIDSFilterDescription
   * @param objectCode - String name used by the UI when resolving an object to add to a Query for this Attribute.
   * @param displayName - String name to display in a UI
   * @param description - String information on the Attribute
   * @param source - String source for the Attribute data.
   * @param homePageURL - String home page Web Address for the source of the data.
   * @param linkoutURL - String URL to link to for specific data from the external source.
   * @throws ConfigurationException when internalName or objectCode are null or empty.
   */
  public UIDSAttributeDescription(String internalName, String objectCode, String displayName, String description, String source, String homePageURL, String linkoutURL) throws ConfigurationException {
  	if (internalName == null || internalName.equals("")
  	|| objectCode == null || objectCode.equals(""))
  	  throw new ConfigurationException("UIDSAttributeDescription objects must contain an internalName and objectCode");
  	  
  	this.internalName = internalName;
  	hashcode = internalName.hashCode();
  	
  	this.objectCode = objectCode;
  	hashcode = (31 * hashcode) + objectCode.hashCode();
  	
  	this.displayName = displayName;
		hashcode = (31 * hashcode) + displayName.hashCode();
		
		this.description = description;
		hashcode = (31 * hashcode) + description.hashCode();
		
		this.source = source;
		hashcode = (31 * hashcode) + source.hashCode();
		
		this.homePageURL = homePageURL;
		hashcode = (31 * hashcode) + homePageURL.hashCode();
		
		this.linkoutURL = linkoutURL;
		hashcode = (31 * hashcode) + linkoutURL.hashCode();
  }

	/**
	 * Returns the description for this UIDSAttributeDescription
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the displayName for this UIDSAttributeDescription
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the homepage URL for this UIDSAttributeDescription
	 * @return String homePageURL
	 */
	public String getHomePageURL() {
		return homePageURL;
	}

	/**
	 * Returns the internalName for this UIDSAttributeDescription
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the linkout URL for this UIDSAttributeDescription
	 * @return String linkoutURL
	 */
	public String getLinkoutURL() {
		return linkoutURL;
	}

	/**
	 * Returns the objectCode for this UIDSAttributeDescription
	 * @return String objectCode
	 */
	public String getObjectCode() {
		return objectCode;
	}

	/**
	 * Returns the source for the data for this UIDSAttributeDescription
	 * @return String source
	 */
	public String getSource() {
		return source;
	}
  
  /**
   * Useful for debugging purposes
   */
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[ UIDSAttributeDescription:");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", objectCode=").append(objectCode);
		buf.append(", description=").append(description);
		buf.append(", source=").append(source);
		buf.append(", homePageURL=").append(homePageURL);
		buf.append(", linkoutURL=").append(linkoutURL);				
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of UIDSAttributeDescription objects
	 */
	public boolean equals(Object o) {
		return o instanceof UIDSAttributeDescription && hashCode() == ((UIDSAttributeDescription) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return hashcode;
	}

  private final String internalName, displayName, description, objectCode, source, homePageURL, linkoutURL;
  private int hashcode = 0;
}
