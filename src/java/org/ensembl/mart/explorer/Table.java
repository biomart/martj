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
 * Represents a table in the database; name of the table and it's columns.
 * 
 * 
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */

public class Table implements Comparable {
  
  /** full table name. */
  public String name;

  /** shortcut for table name that can be used instead of the full name. */
  public String shortcut;

  /** columns in table */
  public String[] columns;
  
  public Table( String name, String[] columns, String baseName) {
    this.name = name;
    this.columns = columns;
    this.shortcut = "";

    // omit "base" + "_" to create table name shortcut
    if (name.length()> baseName.length()+1) 
      this.shortcut = name.substring(baseName.length()+1, name.length());
  }
  

  public int compareTo( Object other ) {
    if ( ! (other instanceof Table) ) return 1;
    else return name.compareTo( ((Table)other).name );
  }


  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[");

    buf.append("name=").append(name);
    buf.append(", shortcut=").append(shortcut);
    buf.append(", columns=").append(columns);

    buf.append("]");
    return buf.toString();
  }

}
