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
 * Contains all of the information required by a UI to display a specific attribute, and create an Attribute object to add to a mart Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class UIAttributeDescription {

/*
 * UIAttributeDescriptions must contain an internalName and fieldName.  Disable parameterless construction.
 */
private UIAttributeDescription() throws ConfigurationException {
	this("", "", "", 0, "", "", "", "", ""); // this will never happen
}

/**
 * Constructs a UIAttributeDescription with just the internalName and fieldName.
 * 
 * @param internalName String name to internally represent the UIAttributeDescription. Must not be null or empty
 * @param fieldName String name of the field in the mart for this Attribute. Must not be null or empty.
 * @throws ConfigurationException when values are null or empty.
 */
public UIAttributeDescription(String internalName, String fieldName) throws ConfigurationException {
	this(internalName, fieldName,"", 0, "", "", "", "", "");
}
/**
 * Constructor for a UIAttributeDescription.
 * 
 * @param internalName String name to internally represent the UIAttributeDescription. Must not be null or empty.
 * @param fieldName String name of the field in the mart for this attribute.  Must not be null or empty.
 * @param displayName String name of the UIAttributeDescription.
 * @param maxLength Int maximum possible length of the field in the mart.
 * @param tableConstraint String base name of a specific table containing this UIAttribute.
 * @param description String description of this UIAttribute.
 * @param source String source for the data for this UIAttribute.
 * @param homePageURL String Web Homepage for the source.
 * @param linkoutURL String Base for a link to a specific entry in a source website.
 * @throws ConfigurationException when required parameters are null or empty
 */
public UIAttributeDescription(String internalName, String fieldName, String displayName, int maxLength, String tableConstraint, String description, String source, String homePageURL, String linkoutURL) throws ConfigurationException {
	if(internalName == null || internalName.equals("") || fieldName == null || fieldName.equals(""))
	  throw new ConfigurationException("UIAttributeDescriptions require a displayName, and fieldName");
	  
	this.internalName = internalName;
	this.displayName = displayName;
	this.fieldName = fieldName;
	this.maxLength = maxLength;
	this.tableConstraint = tableConstraint;
	this.description = description;
	this.source = source;
	this.homepageURL = homePageURL;
	this.linkoutURL = linkoutURL;
	
	hshcode = internalName.hashCode();
	hshcode = (31 * hshcode) + displayName.hashCode();
	hshcode = (31 * hshcode) + fieldName.hashCode();
	hshcode = (31 * hshcode) + maxLength;
	hshcode = (31 * hshcode) + tableConstraint.hashCode();
	hshcode = (31 * hshcode) + description.hashCode();
	hshcode = (31 * hshcode) + source.hashCode();
	hshcode = (31 * hshcode) + homepageURL.hashCode();
	hshcode = (31 * hshcode) + linkoutURL.hashCode();
}

/**
 * Returns the internalName.
 * 
 * @return String internalName
 */
public String getInternalName() {
	return internalName;
}

/**
 * Returns the displayName.
 * 
 * @return String displayName
 */
public String getDisplayName() {
	return displayName;
}

/**
 * Returns the fieldName.
 * 
 * @return String fieldName
 */
public String getFieldName() {
	return fieldName;
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
 * Returns the TableConstraint.
 * 
 * @return String tableConstraint.
 */
public String getTableConstraint() {
	return tableConstraint;
}

/**
 * Returns the Description.
 * 
 * @return String description.
 */
public String getDescription() {
	return description;
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
 * Returns the homePageURL.
 * 
 * @return String homePageURL.
 */
public String getHomePageURL() {
	return homepageURL;
}

/**
 * Returns the linkoutURL.
 * @return String linkoutURL.
 */
public String getLinkoutURL() {
	return linkoutURL;
}

public String toString() {
	StringBuffer buf = new StringBuffer();
	
	buf.append("[");
	buf.append(" internalName=").append(internalName);
	buf.append(", displayName=").append(displayName);
	buf.append(", fieldName=").append(fieldName);
	buf.append(", maxLength=").append(maxLength);
	buf.append(", tableConstraint=").append(tableConstraint);
	buf.append(", description=").append(description);
	buf.append(", source=").append(source);
	buf.append(", homePageURL=").append(homepageURL);
	buf.append(", linkoutURL=").append(linkoutURL);
	buf.append("]");
	
	return buf.toString();
}

public boolean equals(Object o) {
	if (!(o instanceof UIAttributeDescription))
		return false;

	UIAttributeDescription otype = (UIAttributeDescription) o;
	
	if (! (internalName.equals(otype.getInternalName()) ) )
		return false;
	  
	if (! (displayName.equals(otype.getDisplayName()) ) )
		return false;

	if (! ( fieldName.equals(otype.getFieldName() ) ) )
				return false;
				
  if (! (maxLength == otype.getMaxLength() ) )
    return false;
    
	if (! ( tableConstraint.equals(otype.getTableConstraint() ) ) )
				return false;
											  
	if (! (description.equals(otype.getDescription()) ) )
		return false;

	if (! (source.equals(otype.getSource()) ) )
			return false;
	
	if (! (homepageURL.equals(otype.getHomePageURL()) ) )
			return false;
	
	if (! (linkoutURL.equals(otype.getLinkoutURL()) ) )
			return false;
					
	return true;
}

public int hashCode() {
	return hshcode;
}

private final String internalName, displayName, fieldName, tableConstraint, description, source, homepageURL, linkoutURL;
private final int maxLength;
private int hshcode = 0;
 
}
