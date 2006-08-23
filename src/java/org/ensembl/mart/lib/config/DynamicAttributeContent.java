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

public class DynamicAttributeContent extends BaseNamedConfigurationObject {

	private Logger logger =
	  Logger.getLogger(DynamicAttributeContent.class.getName());


  private final String linkoutURLKey = "linkoutURL"; 
  private final String pointerDatasetKey = "pointerDataset";
  private final String pointerInterfaceKey = "pointerInterface";
  private final String pointerAttributeKey = "pointerAttribute";
  private final String pointerFilterKey = "pointerFilter";
  
  private int[] reqFields = {0,1};// rendered red in AttributeTable

  /**
   * Copy constructor. Constructs an exact copy of an existing DynamiacAttributeContent.
   * @param a DynamiacAttributeContent to copy.
   */
  public DynamicAttributeContent(DynamicAttributeContent a) {
    super(a);
	setRequiredFields(reqFields);
  }

  /**
   * Empty Constructor should only be used by DatasetConfigEditor
   *
   */
  public DynamicAttributeContent() {
    super();
    
    setAttribute(linkoutURLKey, null);
	setAttribute(pointerDatasetKey, null);
	setAttribute(pointerInterfaceKey, null);
	setAttribute(pointerAttributeKey, null);
	setAttribute(pointerFilterKey, null);
	setRequiredFields(reqFields);
  }

  /**
   * Constructor for a DynamiacAttributeContent.
   * 
   * @param internalName String name to internally represent the DynamiacAttributeContent. Must not be null or empty.
   * @param linkoutURL String Base for a link to a specific entry in a source website.
   * @throws ConfigurationException when required parameters are null or empty
   */
  public DynamicAttributeContent(String internalName,String linkoutURL) throws ConfigurationException {
    super(internalName, "","");

    setAttribute(linkoutURLKey, linkoutURL);
	setAttribute(pointerDatasetKey, "");
	setAttribute(pointerInterfaceKey, "");
	setAttribute(pointerAttributeKey, "");
	setAttribute(pointerFilterKey, "");
	setRequiredFields(reqFields);
  }
  
  /**
   * Constructor for a DynamiacAttributeContent.
   * 
   * @param internalName String name to internally represent the DynamiacAttributeContent. Must not be null or empty.
   * @param linkoutURL String Base for a link to a specific entry in a source website.
   * @throws ConfigurationException when required parameters are null or empty
   */
  public DynamicAttributeContent(String internalName,String linkoutURL, String pointerDataset, String pointerInterface, String pointerAttribute, String pointerFilter) throws ConfigurationException {
	super(internalName, "","");

	setAttribute(linkoutURLKey, linkoutURL);
	setAttribute(pointerDatasetKey, pointerDataset);
	setAttribute(pointerInterfaceKey, pointerInterface);
	setAttribute(pointerAttributeKey, pointerAttribute);
	setAttribute(pointerFilterKey, pointerFilter);
	setRequiredFields(reqFields);
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
   * @param pointerDataset - pointer dataset, used for placeholder attributes
   */
  public void setPointerDataset(String pointerDataset) {
	setAttribute(pointerDatasetKey, pointerDataset);
  }

  /**
   * Returns the pointerDataset.
   * 
   * @return String pointerDataset
   */
  public String getPointerDataset() {
	return getAttribute(pointerDatasetKey);
  }

  /**
   * @param pointerInterface - pointer interface, used for placeholder attributes
   */
  public void setPointerInterface(String pointerInterface) {
	setAttribute(pointerInterfaceKey, pointerInterface);
  }

  /**
   * Returns the pointerInterface.
   * 
   * @return String pointerInterface
   */
  public String getPointerInterface() {
	return getAttribute(pointerInterfaceKey);
  }
  
  /**
   * @param pointerAttribute - pointer attribute, used for placeholder attributes
   */
  public void setPointerAttribute(String pointerAttribute) {
	setAttribute(pointerAttributeKey, pointerAttribute);
  }

  /**
   * Returns the pointerDataset.
   * 
   * @return String pointerDataset
   */
  public String getPointerAttribute() {
	return getAttribute(pointerAttributeKey);
  }

  /**
   * @param pointerfilter - pointer filter, used for placeholder filters
   */
  public void setPointerFilter(String pointerFilter) {
	setAttribute(pointerFilterKey, pointerFilter);
  }

  /**
   * Returns the pointerDataset.
   * 
   * @return String pointerFilter
   */
  public String getPointerFilter() {
	return getAttribute(pointerFilterKey);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[ DynamiacAttributeContent:");
    buf.append(super.toString());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons of DynamicAttributeContent objects
   */
  public boolean equals(Object o) {
    return o instanceof DynamicAttributeContent
      && hashCode() == ((DynamicAttributeContent) o).hashCode();
  }

  public boolean isBroken() {
	return false;
  }

}
