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

import javax.sql.DataSource;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.LoggingUtils;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceException;
import org.ensembl.mart.lib.config.ConfigurationException;


/**
 * Demonstrates how to construct a Query and execute it against a database.
 */
public class SimpleLibraryUsageExample {

  /**
   * Builds a query and executes it against a database.
   * @param args ignored
   * @throws SequenceException
   * @throws FormatException
   * @throws InvalidQueryException
   * @throws SQLException
   * @throws ConfigurationException
   */
	public static void main(String[] args)
		throws SequenceException, FormatException, InvalidQueryException, SQLException, ConfigurationException {

		// Configure the logging system, don't show verbose messages
    LoggingUtils.setVerbose(false);
		

		// Initialise an engine encapsualting a specific Mart database.
		Engine engine = new Engine();

		// Create a Query object.
		Query query = new Query();
		query.setDatasetName("hsapiens");
		DataSource ds =
			DatabaseUtil.createDataSource(
				"jdbc:mysql://kaka.sanger.ac.uk:3306/ensembl_mart_17_1",
				"anonymous",
				null,
				10,
				"com.mysql.jdbc.Driver");
		query.setDataSource(ds);
    
    // dataset query applies to
    query.setDatasetName("hsapiens_ensemblgene");

		// prefixes for databases we want to use
		query.setStarBases(
			new String[] { "hsapiens_ensemblgene", "hsapiens_ensembltranscript" });

		// primary keys available for sql table joins 
		query.setPrimaryKeys(new String[] { "gene_id", "transcript_id" });

		// Attribute to return
		query.addAttribute(new FieldAttribute("gene_stable_id"));

		// Filter, only consider chromosome 3
		query.addFilter(new BasicFilter("chr_name", "=", "3"));

		//Execute the Query and print the results to stdout.
		engine.execute(
			query,
			new FormatSpec(FormatSpec.TABULATED, "\t"),
			System.out);
	}
}
