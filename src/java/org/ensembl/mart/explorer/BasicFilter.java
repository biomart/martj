/* Generated by Together */

package org.ensembl.mart.explorer;

public class BasicFilter implements Filter {

    public BasicFilter(String field, String condition, String value) {
      this.field = field;
      this.condition = condition;
      this.value = value;
    }

    public void setCondition(String condition){ this.condition = condition; }

    private String condition;

    public String getCondition() { return condition; }

    public void setValue(String value){ this.value = value; }

    private String value;

    public String getValue() { return value; }

    public String getField(){
            return field;
        }

    public void setField(String field){
            this.field = field;
        }

    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
      buf.append(" field=").append(field);
      buf.append(" ,condition=").append(condition);
      buf.append(" ,value=").append(value);
      buf.append("]");

      return buf.toString();
    }

    private String field;



}
