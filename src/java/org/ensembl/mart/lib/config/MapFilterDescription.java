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
 * Description of a Domain Specific Filter made available by an instance of a Mart Database.
 * Domain Specific Filters are handled in the Engine by a DomainSpecificFilterHandler implimenting object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see org.ensembl.mart.lib.DSFilterHandler
 */
public class MapFilterDescription extends FilterDescription {
	
  /**
   * @throws ConfigurationException
   */
  public MapFilterDescription() throws ConfigurationException {
    this("", "", "", "", "", "", "", "", "");
  }

  /**
   * Constructor for a minimal MapFilterDescription.
   * @param internalName
   * @param fieldName
   * @param type
   * @param qualifier
   * @throws ConfigurationException
   */
  public MapFilterDescription(
    String internalName,
    String fieldName,
    String type,
    String qualifier,
    String handler)
    throws ConfigurationException {

    this(internalName, fieldName, type, qualifier, "", "", "", "", handler);
  }

  /**
   * Constructor for a fully defined MapFilterDescription.
   * @param internalName
   * @param fieldName
   * @param type
   * @param qualifier
   * @param displayName
   * @param tableConstraint
   * @param filterSetReq
   * @param description
   * @param handler - String type of DomainSpecificFilterHandler to use to resolve this FilterDescription  
   * @throws ConfigurationException
   */
  public MapFilterDescription(
    String internalName,
    String fieldName,
    String type,
    String qualifier,
    String displayName,
    String tableConstraint,
    String filterSetReq,
    String description,
    String handler
    )
    throws ConfigurationException {
    super(
      internalName,
      fieldName,
      type,
      qualifier,
      displayName,
      tableConstraint,
      filterSetReq,
      description);
    
    this.handler = handler;
    hashcode = super.hashCode();
    hashcode = (31 * hashcode) + this.handler.hashCode();
  
  }

 
  
	/**
	 * Returns the handler of this MapFilterDescription
	 * 
	 * @return String handler
	 */
	public String getHandler() {
		return handler;
	}

  
  /**
   * Used for debugging output.
   */
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[ MapFilterDescription:");
		buf.append( super.toString() );
		buf.append(", handler=").append(handler);
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows equality comparisons of MapFilterDescription objects
	 */
	public boolean equals(Object o) {
    return o instanceof MapFilterDescription && hashCode() == ( (MapFilterDescription) o).hashCode(); 
	}

	/**
	 * Allows Collections manipulation of UIDSFIlterDescription objects
	 */
	public int hashCode() {
		 return hashcode;
	}
	
  private String handler;
  private int hashcode = 0;
}
