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

package org.ensembl.mart.builder.lib;
import org.ensembl.mart.lib.config.ConfigurationException;
/**
 * Contains all of the information for the UI to display a TransformationUnit
 * 
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class TransformationUnitBase extends BaseNamedConfigurationObject {

  private final String referencingTypeKey        = "referencingType";
  private final String primaryKeyKey             = "primaryKey";
  private final String referencedTableKey        = "referencedTable";
  private final String cardinalityKey            = "cardinality";
  private final String centralProjectionKey      = "centralProjection";
  private final String referencedProjectionKey   = "referencedProjection";
  private final String foreignKeyKey             = "foreignKey";
  private final String referenceColumnNamesKey   = "referenceColumnNames";
  private final String referenceColumnAliasesKey = "referenceColumnAliases";
  private final String centralColumnNamesKey     = "centralColumnNames";
  private final String centralColumnAliasesKey   = "centralColumnAliases";  
   
  private int[] reqFields = {0,1,2,3,4,5,7};// rendered red in AttributeTable
  
  /**
   * Copy constructor. Constructs an exact copy of an existing TransformationUnit.
   * @param a TransformationUnit to copy.
   */
  public TransformationUnitBase(TransformationUnitBase a) {
    super(a);
  }

  /**
   * Empty Constructor should only be used by MartBuilder
   *
   */
  public TransformationUnitBase() {
    super();
	setAttribute(referencingTypeKey, null);
    setAttribute(primaryKeyKey, null);
    setAttribute(referencedTableKey, null);
    setAttribute(cardinalityKey, null);
	setAttribute(centralProjectionKey, null);
	setAttribute(referencedProjectionKey, null);	
	setAttribute(foreignKeyKey, null);
	setAttribute(referenceColumnNamesKey, null);
	setAttribute(referenceColumnAliasesKey, null);
	setAttribute(centralColumnNamesKey, null);
	setAttribute(centralColumnAliasesKey, null);
	setRequiredFields(reqFields);
  }

  /**
   * Constructs a TransformationUnit with just the internalName.
   * @param internalName String name to internally represent the TransformationUnit. Must not be null or empty
   * @throws ConfigurationException when values are null or empty.
   */
  public TransformationUnitBase(String internalName)
    throws ConfigurationException {
    this(internalName, "", "", "", "", "", "", "","","","","");
  }
  /**
   * Constructor for an TransformationUnit.
   * 
   * @param internalName String name to internally represent the TransformationUnit. Must not be null or empty.
   * @param referencingType
   * @param primaryKey
   * @param referencedTable
   * @param cardinality 
   * @param centralProjection
   * @param referencedProjection
   * @param foreign key
   * @param reference column names
   * @param reference column aliases
   * @param central column names
   * @param central column aliases
   * @throws ConfigurationException when required parameters are null or empty
   */
  public TransformationUnitBase(
    String internalName,
	String referencingType,
	String primaryKey,
	String referencedTable,
	String cardinality,
	String centralProjection,
	String referencedProjection,
	String foreignKey,
	String refColNames,
	String refColAliases,
	String centralColNames,
	String centralColAliases)
    throws ConfigurationException {

    super(internalName);
	setAttribute(referencingTypeKey, referencingType);
    setAttribute(primaryKeyKey, primaryKey);
    setAttribute(referencedTableKey, referencedTable);
    setAttribute(cardinalityKey, cardinality);
	setAttribute(centralProjectionKey, centralProjection);
	setAttribute(referencedProjectionKey, referencedProjection);
	setAttribute(foreignKeyKey, foreignKey);
	setAttribute(referenceColumnNamesKey, refColNames);
	setAttribute(referenceColumnAliasesKey, refColAliases);
	setAttribute(centralColumnNamesKey, centralColNames);
	setAttribute(centralColumnAliasesKey, centralColAliases);
	
	setRequiredFields(reqFields);
  }

  public void setReferencedTable(String homePageURL) {
    setAttribute(referencedTableKey, homePageURL);
  }

  public String getReferencedTable() {
    return getAttribute(referencedTableKey);
  }

  public void setReferencingType(String key) {
	  setAttribute(referencingTypeKey, key);
  }

  public String getReferencedProjection() {
	 return getAttribute(referencedProjectionKey);
  }

  public void setReferencedProjection(String referencedProjectionString) {
	 setAttribute(referencingTypeKey, referencedProjectionString);
  }

  public String getReferencingType() {
	return getAttribute(referencingTypeKey);
  }

  public void setCentralProjection(String centralProjection){
    setAttribute(centralProjectionKey, centralProjection);
  }

  public String getCentralProjection() {
      return getAttribute(centralProjectionKey);
  }

  public void setCentralColumnNames(String centralColNames){
	setAttribute(centralColumnNamesKey, centralColNames);
  }

  public String getCentralColumnNames() {
	  return getAttribute(centralColumnNamesKey);
  }
  
  public void setCentralColumnAliases(String centralColAliases){
	setAttribute(centralColumnAliasesKey, centralColAliases);
  }

  public String getCentralColumnAliases() {
	  return getAttribute(centralColumnAliasesKey);
  }
  
  public void setReferenceColumnNames(String referenceColNames){
	setAttribute(referenceColumnNamesKey, referenceColNames);
  }

  public String getReferenceColumnNames() {
	  return getAttribute(referenceColumnNamesKey);
  }
  
  public void setReferenceColumnAliases(String referenceColAliases){
	setAttribute(referenceColumnAliasesKey, referenceColAliases);
  }

  public String getReferenceColumnAliases() {
	  return getAttribute(referenceColumnAliasesKey);
  }
  
  public void setPrimaryKey(String primaryKey) {
    setAttribute(primaryKeyKey, primaryKey);
  }

  public String getPrimaryKey() {
    return getAttribute(primaryKeyKey);
  }

  public void setForeignKey(String foreignKey) {
	setAttribute(foreignKeyKey, foreignKey);
  }

  public String getForeignKey() {
	return getAttribute(foreignKeyKey);
  }
  
  public void setCardinality(String cardinality) {
    setAttribute(cardinalityKey, cardinality);
  }

  public String getCardinality() {
    return getAttribute(cardinalityKey);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[ TransformationUnit:");
    buf.append(super.toString());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons of TransformationUnit objects
   */
  public boolean equals(Object o) {
    return o instanceof TransformationUnitBase
      && hashCode() == ((TransformationUnitBase) o).hashCode();
  }

}
