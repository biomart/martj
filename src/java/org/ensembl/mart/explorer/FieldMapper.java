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

import java.util.*;

/**
 * Maps column names to the table they come from and also maps them to
 * their fully qualified names (table.column). Also supports partially
 * qualified column names e.g. column="xref_RefSeq.display_id".
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class FieldMapper {

	private String primaryKey = null;
	

  /** column -> table */
  private Properties map = new Properties();

  /**
   * Provides column mappings for the specified tables. The order of the
   * tables in the array is important; an ambiguous column name is resolved
   * to the first table in it occurs.
   * 
   *@param tables list of tables to create mappings for. 
   *
   */
  public FieldMapper (Table[] tables, String primaryKey){

		this.primaryKey = primaryKey;

    for(int i=0; i<tables.length; ++i) {

      Table table = tables[i];
      for(int j=0; j<table.columns.length; ++j){
        
        // this is how we resolve name ambiguity; first encountered mappings
        // are remembered and later ones are ignored.
        String col = table.columns[j];
        if ( !map.containsKey( col ) ) 
          map.put(col, table.name);
        
        // add fully qualified name
        col = table.name + "." + col;
        if ( !map.containsKey( col ) ) 
          map.put(col, table.name);

        // add shortcut name
        col = table.shortcut + "." + col;
        if ( !map.containsKey( col ) ) 
          map.put(col, table.name);

        

      }
    }
  }


	

  /**
   * @return table.column if mapping is available, otherwise null.
   */
  public String qualifiedName(Field field) {

    String table = map.getProperty( field.getName() );

    if ( table==null ) return null;
    else return table + "." + strippedColumn( field.getName() );
    
  }


  /**
   * Strips "prefix." from "prefix.column" if it exists. "prefix" is the full
   * or partial table name.
   * @return just the column name without any partial or full table
   */
  public String strippedColumn( String column ) {
    int pos = column.indexOf(".");
    if ( pos==-1 ) return column;
    else return column.substring( pos+1, column.length() );
  }

  /**
   * @return table name if mapping is available, otherwise null.
   */
  public String tableName(Field field) {
    return map.getProperty( field.getName() );
  }
  

  /**
   * @return true if the column is mapped to a table.
   */
  public boolean canMap(Field field) {
    return map.containsKey( field.getName() );
  }
  
  /**
   * @return true if all the columns are mapped to a table.
   */
  public boolean canMap(Field[] fields) {
  	for (int i = 0; i < fields.length; i++) 
			if ( !canMap( fields[i] ) ) return false;
		  	
  	return true;
  }


  public String toString() {
      StringBuffer buf = new StringBuffer();

      buf.append("[");
      buf.append(" #map=").append(map.size());
      buf.append("]");

      return buf.toString();
  }

	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	public String getPrimaryKey() {
		return primaryKey;
	}
}
