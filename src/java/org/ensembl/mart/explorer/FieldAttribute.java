/* Generated by Together */

package org.ensembl.mart.explorer;

/**
 * Holds the name of a single field attribute for inclusion in a query.
 * Implements hashCode() and equals() for simple retrieval from Collections.
 */
public class FieldAttribute implements Attribute {
    public FieldAttribute(String field) {
      this.field = field;
    }

    public String getField() { return null; }

    public void setField(String field) { }

    public int hashCode() {
        return (field == null) ? 0 : field.hashCode();
    }

    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode() && obj instanceof FieldAttribute;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
      buf.append(" field=").append(field);
      buf.append(" ]");

      return buf.toString();
    }

    private String field;
}
