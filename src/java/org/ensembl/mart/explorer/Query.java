package org.ensembl.mart.explorer;

import java.util.*;

public class Query {

    /* enums over query types
     * clients can set type using the constant
     * and test get results as well
     */
    public final int ATTRIBUTE = 1;
    public final int SEQUENCE = 2;

    // TODO, when implement SEQUENCE, over ride this during the addSequenceDescription method call
    private int querytype = 1;

    public int getType() {  return querytype; }

	public boolean hasAttribute( Attribute attribute ) {
		return attributes.contains( attribute );
    }

	public void addAttribute( Attribute attribute ) {
		if ( !attributes.contains( attribute ) )
			attributes.add( attribute );
  }


  public void removeAttribute(  Attribute attribute ) {
		if ( attributes.contains( attribute ) )
			attributes.remove( attribute );
  }

  public Attribute[] getAttributes() {
    Attribute[] a = new Attribute[ attributes.size() ];
    attributes.toArray( a );
    return a;
  }

  public void setAttributes(List attributes) {
    this.attributes = attributes;
  }
  
  public Filter[] getFilters() {
    Filter[] f = new Filter[ filters.size() ];
    filters.toArray( f );
    return f;
  }

   public void setFilters(Filter[] filters) {
		this.filters = Arrays.asList( filters );
   }

    public void addFilter(Filter filter) {
      // new filters with the same hashCode() override old ones
      if ( filters.contains( filter ) )
      	filters.remove( filter );
      filters.add( filter );
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
      buf.append(" ,species=").append(species);
      buf.append(" ,focus=").append(focus);
      buf.append(" ,attributes=").append(attributes);
      buf.append(" ,filters=").append(filters);
      buf.append("]");

      return buf.toString();
    }

    public String getSpecies(){
            return species;
	}

    public void setSpecies(String species){
            this.species = species;
	}

    public String getFocus(){
            return focus;
        }

    public void setFocus(String focus){
            this.focus = focus;
        }

    private List attributes = new Vector();
    private List filters = new Vector();

    /** @link dependency */

  /*# Attribute lnkAttribute; */

    /** @link dependency */

  /*# Filter lnkFilter; */
  private String species;
  private String focus;
}
