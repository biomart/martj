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


import java.util.logging.Logger;
/**
 * 
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */

public class DynamicDatasetContent extends BaseNamedConfigurationObject {

	private Logger logger =
	  Logger.getLogger(DynamicAttributeContent.class.getName());


  private final String versionKey = "version"; 
  
  private int[] reqFields = {0,1};// rendered red in AttributeTable

  /**
   * Copy constructor. Constructs an exact copy of an existing DynamiacFilterContent.
   * @param a DynamicDatasetContent to copy.
   */
  public DynamicDatasetContent(DynamicDatasetContent a) {
    super(a);
	setRequiredFields(reqFields);
  }

  /**
   * Empty Constructor should only be used by DatasetConfigEditor
   *
   */
  public DynamicDatasetContent() {
    super();
    
    setAttribute(versionKey, null);
	setRequiredFields(reqFields);
  }

  /**
   * Constructor for a DynamicDatasetContent.
   * 
   * @param internalName String name to internally represent the DynamicDatasetContent. Must not be null or empty.
   * @param version .
   * @throws ConfigurationException when required parameters are null or empty
   */
  public DynamicDatasetContent(String internalName,String displayName,String version) throws ConfigurationException {
    super(internalName, displayName,"");

    setAttribute(versionKey, version);
	setRequiredFields(reqFields);
  }

  /**
   * @param version - String 
   */
  public void setVersion(String version) {
    setAttribute(versionKey, version);
  }

  /**
   * Returns the version
   * @return String otherFilters.
   */
  public String getVersion() {
    return getAttribute(versionKey);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[ DynamicDatasetContent:");
    buf.append(super.toString());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons of DynamicDatasetContent objects
   */
  public boolean equals(Object o) {
    return o instanceof DynamicDatasetContent
      && hashCode() == ((DynamicDatasetContent) o).hashCode();
  }

  public boolean isBroken() {
	return false;
  }

}
