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
 * Contains all of the information necessary for the UI to display the information for a specific filter,
 * and add this filter as a Filter to a Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class UIFilterDescription {

	/*
	 * UIFilterDescriptions require a displayName, fieldName, type, and qualifier.  disable parameterless constructor
	 */
	private UIFilterDescription() {
		this("", "", "", "", "", ""); // this will never happen
	}

	public UIFilterDescription(String displayName, String fieldName, String type,	String qualifier,	String tableConstraint,	String description) {
		if (displayName == null
			|| fieldName == null
			|| type == null
			|| qualifier == null)
			throw new RuntimeException("UIFilterDescription requires a displayName, fieldName, type, and qualifier");

		this.displayName = displayName;
		this.fieldName = fieldName;
		this.type = type;
		this.qualifier = qualifier;
		this.tableConstraint = tableConstraint;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getType() {
		return type;
	}

	public String getQualifier() {
		return qualifier;
	}

	public String getTableConstraint() {
		return tableConstraint;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" displayName=").append(displayName);
		buf.append(", fieldName=").append(fieldName);
		buf.append(", type=").append(type);
		buf.append(", qualifier=").append(qualifier);
		buf.append(", tableConstraint=").append(tableConstraint);
		buf.append(", description=").append(description);
		buf.append("]");

		return buf.toString();
	}

	private final String displayName,	fieldName,	type,	qualifier,	tableConstraint,	description;
}
