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
 *  Really just a collection of static enums over the types of MartLocation objects
 *  available.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartLocationBase extends BaseConfigurationObject implements MartLocation {

  public static final String DATABASE = "database";
  public static final String URL = "url";
  public static final String REGISTRY = "registry";
  
  protected final String NAME_KEY = "name";
  protected String type;
  
  public MartLocationBase() {
    super();
  }
  
  public MartLocationBase(String name, String type) {
    super();
    setAttribute(NAME_KEY, name);
    this.type = type;
  }
  
	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.MartLocation#getType()
	 */
	public String getType() {
    return type;
	}

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.MartLocation#getName()
   */
  public String getName() {
    return getAttribute(NAME_KEY);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.MartLocation#setName()
   */  
  public void setName(String name) {
    setAttribute(NAME_KEY, name);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return o instanceof MartLocationBase && o.hashCode() == hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    int hash = super.hashCode();
    hash = (31 * hash) + type.hashCode();
    return hash;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.BaseConfigurationObject#isBroken()
   */
  public boolean isBroken() {
    //never broken
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return super.toString() + ", type=" + type;
  }
}
