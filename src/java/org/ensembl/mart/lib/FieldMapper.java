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


package org.ensembl.mart.lib;

import org.apache.log4j.Logger;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Maps column names to the table they come from and also maps them to
 * their fully qualified names (table.column). Also supports partially
 * qualified column names e.g. column="xref_RefSeq.display_id".
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class FieldMapper {
	
	private final static Logger logger = Logger.getLogger( FieldMapper.class.getName()); 

	private String primaryKey = null;
	

  /** column -> table */
  private Properties nameToTable = new Properties();
  
  /** All legal table.columns derived from input Table[]. */
  private String[] tableAndColumns = null;

  /**
   * Provides column mappings for the specified tables. The order of the
   * tables in the array is important; an ambiguous column name is resolved
   * to the first table it occurs in.
   * 
   * @param tables list of tables to create mappings for. 
 	 * @param primaryKey primary key that can be used for joins
   */
  public FieldMapper (Table[] tables, String primaryKey){

		this.primaryKey = primaryKey;
		List tableAndColumnsTmp = new ArrayList();
    
    for(int i=0; i<tables.length; ++i) {

      Table table = tables[i];
      if ( logger.isDebugEnabled() ) {
      	logger.debug("table="+table);
				logger.debug("table.columns.length="+table.columns.length);
      }
      for(int j=0; j<table.columns.length; ++j){
		    
        // this is how we resolve name ambiguity; first encountered mappings
        // are remembered and later ones are ignored.
        String col = table.columns[j];
        if ( !nameToTable.containsKey( col ) ) 
          nameToTable.put(col, table.name);
        
        // add fully qualified name
        col = table.name + "." + col;
        if ( !nameToTable.containsKey( col ) ) 
          nameToTable.put(col, table.name);
          
        tableAndColumnsTmp.add( col );

        // add shortcut name
        // TODO remove all refences to table.shortcut because unused?
        col = table.shortcut + "." + col;
        if ( !nameToTable.containsKey( col ) ) 
          nameToTable.put(col, table.name);

        

      }
    }
    
    tableAndColumns = new String[ tableAndColumnsTmp.size() ];
		tableAndColumnsTmp.toArray( tableAndColumns );	  
  }


	

  /**
   * @return table.column if mapping is available, otherwise null.
   */
  public String qualifiedName(Field field) {

		// TODO possible optimisation: cache field->qName
		String qName = null;
		String name = field.getField();
		String constraint = field.getTableConstraint();
		 
		if ( constraint==null ) {
			String table = nameToTable.getProperty( name );
    	if ( table!=null ) 
    	 	qName = table + "." + strippedColumn( name );
		}
		else {
			Pattern p = Pattern.compile(".*" + constraint + ".*\\." + name);
			for (int i = 0; 
						i < tableAndColumns.length && qName==null; 
						i++) {
				// todo
				String s = tableAndColumns[i];
				if ( p.matcher( s ).matches() ) qName=s;
			}
		}
    
    return qName;
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
  	String table = null;
  	String qName = qualifiedName( field );
  	if ( qName!=null ) 
  		// extract table from "table.column"
  		table = qName.split("\\.",2)[0];	
  	return table;
  }
  

  /**
   * @return true if the column is mapped to a table.
   */
  public boolean canMap(Field field) {
    return qualifiedName( field ) != null;
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
      buf.append(" #map=").append(nameToTable.size());
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
