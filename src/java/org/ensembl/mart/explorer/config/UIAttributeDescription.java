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
 * UIAttributeDescriptions must contain a displayName and fieldName.  Disable parameterless construction.
 */
private UIAttributeDescription() {
	this("", "", 0, "", "", "", "", ""); // this will never happen
}

/**
 * Constructor for a UIAttributeDescription.
 * 
 * @param displayName String name of the UIAttribute.  Must not be null.
 * @param fieldName String name of the field in the mart for this attribute.  Must not be null.
 * @param maxLength Int maximum possible length of the field in the mart.
 * @param tableConstraint String base name of a specific table containing this UIAttribute.
 * @param description String description of this UIAttribute.
 * @param source String source for the data for this UIAttribute.
 * @param homePageURL String Web Homepage for the source.
 * @param linkoutURL String Base for a link to a specific entry in a source website.
 */
public UIAttributeDescription(String displayName, String fieldName, int maxLength, String tableConstraint, String description, String source, String homePageURL, String linkoutURL) {
	if(displayName == null || fieldName == null)
	  throw new RuntimeException("UIAttributeDescriptions require a displayName, and fieldName");
	  
	this.displayName = displayName;
	this.fieldName = fieldName;
	this.maxLength = maxLength;
	this.tableConstraint = tableConstraint;
	this.description = description;
	this.source = source;
	this.homePageURL = homePageURL;
	this.linkoutURL = linkoutURL;
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
	return homePageURL;
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
	buf.append(" displayName=").append(displayName);
	buf.append(", fieldName=").append(fieldName);
	buf.append(", maxLength=").append(maxLength);
	buf.append(", tableConstraint=").append(tableConstraint);
	buf.append(", description=").append(description);
	buf.append(", source=").append(source);
	buf.append(", homePageURL=").append(homePageURL);
	buf.append(", linkoutURL=").append(linkoutURL);
	buf.append("]");
	
	return buf.toString();
}

public final String displayName, fieldName, tableConstraint, description, source, homePageURL, linkoutURL;
public final int maxLength; 
}
