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
 * Place Holder object to signal to the UI where to render complex
 * Domain Specific Attribute leafWidgets that do not fit the generic
 * MartConfiguration scheme.  Does not contain any lower level objects.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DSAttributeGroup extends BaseNamedConfigurationObject {
  
  private final String handlerKey = "handler";
  
  /**
   * Copy constructor.  Constructs an exact copy of an existing DSAttributeGroup.
   * @param dsag DSAttributeGroup to copy.
   */
  public DSAttributeGroup(DSAttributeGroup dsag) {
  	super(dsag);
  }
  
	/**
	 * Empty Constructor should really only be used by the DatasetViewEditory
	 */
	public DSAttributeGroup() throws ConfigurationException {
		super();
	}

	/**
	 * Constructor for a minimal DSAttributeGroup, with internalName set.
	 * @param internalName - String name to internally represent the object.
	 * @throws ConfigurationException when internalName is null or empty
	 */
	public DSAttributeGroup(String internalName) throws ConfigurationException {
		this(internalName, "", "", "");
	}

	/**
	 * Constructor for a fully qualified DSAttributeGroup
	 * @param internalName - String name to internally represent the object.
	 * @param displayName - String name to display in a UI
	 * @param description - String description of the DSAttributeGroup
	 * @param handler - String name of handler Class object to use to process this group
	 * @throws ConfigurationException when internalName is null or empty
	 */
	public DSAttributeGroup(String internalName, String displayName, String description, String handler) throws ConfigurationException {
		super(internalName, displayName, description);
		setAttribute(handlerKey, handler);
	}

  /**
   * Set the Handler for this DSAttributeGroup
   * @param handler - String name of Object handler code.
   */
	public void setHandler(String handler) {
		setAttribute(handlerKey, handler);
	}

	/**
	 * Returns the handler
	 * @return Sring handler
	 */
	public String getHandler() {
		return getAttribute(handlerKey);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of DSAttributeGroup objects
	 */
	public boolean equals(Object o) {
		return o instanceof DSAttributeGroup && hashCode() == ((DSAttributeGroup) o).hashCode();
	}

	/**
	 * always false. Cannot be broken
	 */
	public boolean isBroken() {
		return false;
	}
}
