/* Generated by Together */

package org.ensembl.mart.explorer;

public interface Filter {
    String getType();

    /**
     * Value, null if not needed. This can be added to the prepared statment.
     */
    String getValue();

    /**
     * String to be included in where clause.
     * TODO: remove this?
     */
    String getWhereClause();


  /**
   * String representing the "right hand side of the condition". This is any
   * operator and values
   */
  String getRightHandClause();
}
