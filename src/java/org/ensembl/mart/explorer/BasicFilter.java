/* Generated by Together */

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
package org.ensembl.mart.explorer;

/**
 * Basic Implimentation of a Filter object.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class BasicFilter implements Filter {

    /**
     * constructs a BasicFilter object, which can be added to a Query
     * 
     * @param field -- String type.  The type of filter being applied
     * @param condition -- String condition of the clause, eg. =<>
     * @param value -- parameter of the condition, applicable to the type.
     */
    public BasicFilter(String field, String condition, String value) {
      this.type = field;
      this.condition = condition;
      this.value = value;
    }

  /**
   * sets the condition for the filter.  Conditions can be =,<, or >.
   * 
   * @param condition -- String =<>
   */
    public void setCondition(String condition){ this.condition = condition; }

    private String condition;

  /**
   * returns the condition for the filter
   * @return String condition =<>
   */
    public String getCondition() { return condition; }

  /**
   * sets the value of the condition, which is the actual filter string to
   * apply to the query.
   * 
   * @param value -- String value that is applicable to the specified type.
   */
    public void setValue(String value){ this.value = value; }

    private String value;

  /**
   * returns the value set for the query filter
   * 
   * @return String value 
   */
    public String getValue() { return value; }

   /**
    * returns the type set for the query filter
    * 
    * @return String type
    */
    public String getType(){
            return type;
        }

    /**
     * prints information about the filter, for logging purposes
     *
     * @return String filter information (field=type\ncondition=condition\nvalue=value)
     */
    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
      buf.append(" field=").append(type);
      buf.append(" ,condition=").append(condition);
      buf.append(" ,value=").append(value);
      buf.append("]");

      return buf.toString();
    }
  /**
   * returns a where clause with the type, condition, and a bind parameter 
   * for the value, suitable for inclusion in a SQL PreparedStatement
   * 
   * @return String where clause
   */
  public String getWhereClause(){
    return type +condition+"?";
  }
  
  /**
   * returns the right hand of the where clause, with the condition, and
   * a bind value suitable for inclusion into a SQL PreparedStatement.
   */
  public String getRightHandClause() {
    return condition+"?";
  }

  /**
   * returns the value of the filter
   * 
   * @return String value
   */
  public String sqlValue() {
    return value;
  }

    private String type;
}
