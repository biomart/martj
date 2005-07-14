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

package org.ensembl.mart.builder.config;

/**
 * Contains all of the information 
 * 
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class TransformationUnit extends BaseNamedConfigurationObject {

//  private Logger logger =
//    Logger.getLogger(TransformationUnit.class.getName());

  private final String referencingTypeKey = "referencingType";
  private final String primaryKeyKey = "primaryKey";
  private final String referencedTableKey = "referencedTable";
  private final String cardinalityKey = "cardinality";
  private final String centralProjectionKey = "centralProjection";
  private final String referencedProjectionKey = "referencedProjection";
  private final String foreignKeyKey = "foreignKey";
  private final String referenceColumnNamesKey = "referenceColumnNames";
  private final String referenceColumnAliasesKey = "referenceColumnAliases";
  private final String centralColumnNamesKey = "centralColumnNames";
  private final String centralColumnAliasesKey = "centralColumnAliases";  
  //private final String includeCentralFilterKey = "includeCentralFilter";
  
  private int[] reqFields = {0,2,3,4,5,7};// rendered red in AttributeTable
  
  /**
   * Copy constructor. Constructs an exact copy of an existing TransformationUnit.
   * @param a TransformationUnit to copy.
   */
  public TransformationUnit(TransformationUnit a) {
    super(a);
  }

  /**
   * Empty Constructor should only be used by DatasetConfigEditor
   *
   */
  public TransformationUnit() {
    super();
    setAttribute(centralProjectionKey, null);
	setAttribute(referencingTypeKey, null);
    setAttribute(primaryKeyKey, null);
    setAttribute(referencedTableKey, null);
    setAttribute(cardinalityKey, null);
	setAttribute(referencedProjectionKey, null);
	
	setAttribute(foreignKeyKey, null);
	setAttribute(referenceColumnNamesKey, null);
	setAttribute(referenceColumnAliasesKey, null);
	setAttribute(centralColumnNamesKey, null);
	setAttribute(centralColumnAliasesKey, null);
	//setAttribute(includeCentralFilterKey, null);
	setRequiredFields(reqFields);
  }

  /**
   * Constructs a TransformationUnit with just the internalName and tableType.
   * not used anywhere yet and should probably add centralTable and Key
   * @param internalName String name to internally represent the TransformationUnit. Must not be null or empty
   * @param tableType String name of the tableType in the mart for this Attribute. Must not be null or empty.
   * @throws ConfigurationException when values are null or empty.
   */
  public TransformationUnit(String internalName)
    throws ConfigurationException {
    this(internalName, "", "", "", "", "", "", "", "", "", "", "","","","","","","");
  }
  /**
   * Constructor for an TransformationUnit.
   * 
   * @param internalName String name to internally represent the TransformationUnit. Must not be null or empty.
   * @param tableType String name of the tableType in the mart for this attribute.  Must not be null or empty.
   * @param displayName String name of the TransformationUnit.
   * @param centralProjection Int maximum possible length of the tableType in the mart.
   * @param centralTable String base name of a specific table containing this UIAttribute.
   * @param key String name of the key to use with this attribute
   * @param description String description of this UIAttribute.
   * @param primaryKey String primaryKey for the data for this UIAttribute.
   * @param homePageURL String Web Homepage for the primaryKey.
   * @param cardinality String Base for a link to a specific entry in a primaryKey website.
   * @param referencedProjection attribute for a dataset if set to true.
   * @throws ConfigurationException when required parameters are null or empty
   */
  public TransformationUnit(
    String internalName,
    String tableType,
    String displayName,
    String centralProjection,
    String centralTable,
    String key,
    String description,
    String primaryKey,
    String homePageURL,
    String cardinality,
    String dataset,
    String referencedProjectionString,
    String foreignKey,
    String refColNames,
    String refColAliases,
    String centralColNames,
    String centralColAliases,
    String userTableName)
    throws ConfigurationException {

    super(internalName);
    setAttribute(centralProjectionKey, centralProjection);
	setAttribute(referencingTypeKey, key);
    setAttribute(primaryKeyKey, primaryKey);
    setAttribute(referencedTableKey, homePageURL);
    setAttribute(cardinalityKey, cardinality);
	setAttribute(referencedProjectionKey, referencedProjectionString);
	
	setAttribute(foreignKeyKey, foreignKey);
	setAttribute(referenceColumnNamesKey, refColNames);
	setAttribute(referenceColumnAliasesKey, refColAliases);
	setAttribute(centralColumnNamesKey, centralColNames);
	setAttribute(centralColumnAliasesKey, centralColAliases);
	//setAttribute(includeCentralFilterKey, includeCentralFilter);
	setRequiredFields(reqFields);
  }

  /**
   * @param homePageURL - url to homepage for the data primaryKey
   */
  public void setReferencedTable(String homePageURL) {
    setAttribute(referencedTableKey, homePageURL);
  }

  /**
   * @return referencedTable
   */
  public String getReferencedTable() {
    return getAttribute(referencedTableKey);
  }



  /**
   * @param key - join tableType key for the tableType
   */
	public void setReferencingType(String key) {
	  setAttribute(referencingTypeKey, key);
	}

	/**
	 * Returns the join tableType key.
	 * 
	 * @return key.
	 */
	public String getReferencedProjection() {
	  return getAttribute(referencedProjectionKey);
	}
	
	/**
	 * @param key - join tableType key for the tableType
	 */
	public void setReferencedProjection(String referencedProjectionString) {
	   setAttribute(referencingTypeKey, referencedProjectionString);
	}

	  /**
	   * Returns the join tableType key.
	   * 
	   * @return key.
	   */
	  public String getReferencingType() {
		return getAttribute(referencingTypeKey);
	  }



  /**
   * @param centralProjection - String maximum length of the table tableType
   */
  public void setCentralProjection(String centralProjection){
    setAttribute(centralProjectionKey, centralProjection);
  }

  /**
   * Returns the centralProjection. If the value for centralProjection
   * is not a valid integer (eg, a NumberFormatException is
   * thrown by Integer.parseInt( centralProjection )) this method will
   * return DEFAULTMAXLENGTH
   * 
   * @return int CentralProjection.
   */
  public String getCentralProjection() {
      return getAttribute(centralProjectionKey);
  }
  
  
  /**
   * @param centralProjection - String maximum length of the table tableType
   */
  public void setCentralColumnNames(String centralColNames){
	setAttribute(centralColumnNamesKey, centralColNames);
  }

  /**
   * Returns the centralProjection. If the value for centralProjection
   * is not a valid integer (eg, a NumberFormatException is
   * thrown by Integer.parseInt( centralProjection )) this method will
   * return DEFAULTMAXLENGTH
   * 
   * @return int CentralProjection.
   */
  public String getCentralColumnNames() {
	  return getAttribute(centralColumnNamesKey);
  }
  
  /**
   * @param centralProjection - String maximum length of the table tableType
   */
  public void setCentralColumnAliases(String centralColAliases){
	setAttribute(centralColumnAliasesKey, centralColAliases);
  }

  /**
   * Returns the centralProjection. If the value for centralProjection
   * is not a valid integer (eg, a NumberFormatException is
   * thrown by Integer.parseInt( centralProjection )) this method will
   * return DEFAULTMAXLENGTH
   * 
   * @return int CentralProjection.
   */
  public String getCentralColumnAliases() {
	  return getAttribute(centralColumnAliasesKey);
  }
  

  /**
   * @param centralProjection - String maximum length of the table tableType
   */
  public void setReferenceColumnNames(String referenceColNames){
	setAttribute(referenceColumnNamesKey, referenceColNames);
  }

  /**
   * Returns the ReferenceProjection. If the value for ReferenceProjection
   * is not a valid integer (eg, a NumberFormatException is
   * thrown by Integer.parseInt( ReferenceProjection )) this method will
   * return DEFAULTMAXLENGTH
   * 
   * @return int ReferenceProjection.
   */
  public String getReferenceColumnNames() {
	  return getAttribute(referenceColumnNamesKey);
  }
  
  /**
   * @param ReferenceProjection - String maximum length of the table tableType
   */
  public void setReferenceColumnAliases(String referenceColAliases){
	setAttribute(referenceColumnAliasesKey, referenceColAliases);
  }

  /**
   * Returns the ReferenceProjection. If the value for ReferenceProjection
   * is not a valid integer (eg, a NumberFormatException is
   * thrown by Integer.parseInt( ReferenceProjection )) this method will
   * return DEFAULTMAXLENGTH
   * 
   * @return int ReferenceProjection.
   */
  public String getReferenceColumnAliases() {
	  return getAttribute(referenceColumnAliasesKey);
  }
  
 
  /**
   * @param primaryKey - String name of data primaryKey
   */
  public void setPrimaryKey(String primaryKey) {
    setAttribute(primaryKeyKey, primaryKey);
  }

  /**
   * Returns the primaryKey.
   * 
   * @return String primaryKey
   */
  public String getPrimaryKey() {
    return getAttribute(primaryKeyKey);
  }

  /**
   * @param primaryKey - String name of data primaryKey
   */
  public void setForeignKey(String foreignKey) {
	setAttribute(foreignKeyKey, foreignKey);
  }

  /**
   * Returns the primaryKey.
   * 
   * @return String primaryKey
   */
  public String getForeignKey() {
	return getAttribute(foreignKeyKey);
  }
  
  /**
   * @param Cardinality - String base for HTML link references
   */
  public void setCardinality(String cardinality) {
    setAttribute(cardinalityKey, cardinality);
  }

  /**
   * Returns the cardinality.
   * @return String cardinality.
   */
  public String getCardinality() {
    return getAttribute(cardinalityKey);
  }



  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[ TransformationLine:");
    buf.append(super.toString());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons of TransformationUnit objects
   */
  public boolean equals(Object o) {
    return o instanceof TransformationUnit
      && hashCode() == ((TransformationUnit) o).hashCode();
  }

}
