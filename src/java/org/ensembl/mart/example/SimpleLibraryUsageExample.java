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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.example;
import java.sql.SQLException;

import org.apache.log4j.*;
import org.ensembl.mart.lib.*;

public class SimpleLibraryUsageExample {

  public static void main(String[] args) throws SequenceException, FormatException, InvalidQueryException, SQLException  {
    
    // Configure the logging system.
    BasicConfigurator.configure();
    Logger.getRoot().setLevel(Level.INFO);
    
    // Initialise an engine encapsualting a specific Mart database.
    Engine engine = new Engine( "jdbc:mysql://kaka.sanger.ac.uk:3306/ensembl_mart_15_1", "anonymous", "" );
    
    // Create a Query object.
    Query query = new Query();
		
    // prefixes for databases we want to use
    query.setStarBases(new String[] { "hsapiens_ensemblgene", "hsapiens_ensembltranscript" });
    
    // primary keys available for sql table joins 
    query.setPrimaryKeys(new String[] { "gene_id", "transcript_id" });
    
    // Attribute to return
    query.addAttribute(new FieldAttribute("gene_stable_id"));
		
    // Filter, only consider chromosome 3
    query.addFilter( new BasicFilter("chr_name", "=", "3") );
    
    //Execute the Query and print the results to stdout.
    engine.execute( query, new FormatSpec( FormatSpec.TABULATED, "\t"),  System.out);    
  }
}
