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

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;

/**
 * Class for interaction between UI and Mart Database.  Manages mySQL database
 * connections, and executes Querys.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */

//TODO: implement broad(transcript based) versus narrow(gene based) filtering of resultsets

public class Engine {
	private static Logger logger = Logger.getLogger(Engine.class.getName());

	/**
	 * Attempts to load the database drivers normally shipped with 
	 * martlib distribution.
	 */
	private static void loadFallbackDatabaseDrivers() {
		String[] driverNames = new String[] { "org.gjt.mm.mysql.Driver" };
		for (int i = 0; i < driverNames.length; i++) {
			try {
				Class.forName(driverNames[i]).newInstance();
			} catch (Exception e) {
				logger.warn("Failed to load driver" + driverNames[i], e);
				throw new RuntimeException(e.getMessage());
			}
		}
	}

	/** 
	 * Load drivers normally distributed with mart lib. These will be
	 * available if no other drivers are previously loaded.
	 */
	{
		loadFallbackDatabaseDrivers();
	}

	private String baseConnectionString;
	private Connection connection = null;
	private String connectionString;

	public Engine(Connection connection) {
		this.connection = connection;

	}

	public Engine(String databaseURL, String user, String password) throws SQLException {
		connection = DatabaseUtil.getConnection(databaseURL, user, password);
	}

	/**
	 * Creates an Engine connected to a specific mart
	 * database.
	 * 
	 * @param databaseType - type of database to be includeded in 
	 * jdbc connection string, e.g. "mysql"
	 * @param host - the host computer name or ip number
	 * @param port - the database program's port, <code>null</code> if to use default
	 * @param databaseName - the name of the mart database
	 * @param user - user name
	 * @param password - password (can be null)
	 * @throws SQLException if problem connecting to database.
	 */
	public Engine(String databaseType, String host, String port, String databaseName, String user, String password) throws SQLException {

		connection = DatabaseUtil.getConnection(databaseType, host, port, databaseName, user, password);

	}

	public int countFocus(Query query) {
		throw new RuntimeException();
	}

	public int countRows(Query query) {
		throw new RuntimeException();
	}

	/**
	 * Checks for DomainSpecificFilters in the Query, and uses the DSFilterHandler
	 * system to modify the Query accordingly, if present.
	 * Constructs a QueryRunner object for the given Query, and format using 
	 * a QueryRunnerFactory.  Uses the QueryRunner to execute the Query
	 * with the mySQL connection of this Engine, and write the results to 
	 * a specified OutputStream.
	 * 
	 * @param query - A Query Object
	 * @param formatspec - A FormatSpec Object
	 * @param os - An OutputStream
	 * @throws FormatException - unsupported Format supplied to the QueryRunnerFactory
	 * @throws SequenceException - general Exception thrown for a variety of reasons that the SeqQueryRunners cannot write out sequence data
	 * @throws InvalidQueryException - general Exception thrown when invalid query parameters have been presented, and the resulting SQL will not work.
	 * @see Query
	 * @see FormatSpec
	 * @see QueryRunnerFactory
	 * @see QueryRunner
	 * @see DSFilterHandler
	 * @see DSFilterHandlerFactory
	 */
	public void execute(Query query, FormatSpec formatspec, OutputStream os) throws SequenceException, FormatException, InvalidQueryException, SQLException {

		if (query.hasLimit())
			execute(query, formatspec, os, query.getLimit());
		else
			execute(query, formatspec, os, 0);

	}

	/**
	 * Checks for DomainSpecificFilters in the Query, and uses the DSFilterHandler
	 * system to modify the Query accordingly, if present.
	 * Constructs a QueryRunner object for the given Query, and format using 
	 * a QueryRunnerFactory.  Applies a limit clause to the SQL.
	 * Uses the QueryRunner to execute the Query with the mySQL connection of 
	 * this Engine, and write the results to a specified OutputStream.
	 * 
	 * @param query A Query Object
	 * @param formatspec A FormatSpec Object
	 * @param os An OutputStream
	 * @param limit limits the number of records returned by the query
	 * @throws SequenceException
	 * @throws FormatException
	 * @throws InvalidQueryException
	 * @see Query
	 * @see FormatSpec
	 * @see QueryRunnerFactory
	 * @see QueryRunner
	 * @see DSFilterHandler
	 * @see DSFilterHandlerFactory
	 */
	public void execute(Query query, FormatSpec formatspec, OutputStream os, int limit)
		throws SequenceException, FormatException, InvalidQueryException, SQLException {

		Connection conn = getConnection();
		if (query.hasDomainSpecificFilters()) {
			DomainSpecificFilter[] dsfilters = query.getDomainSpecificFilters();
			for (int i = 0, n = dsfilters.length; i < n; i++) {
				DomainSpecificFilter dsf = dsfilters[i];
				DSFilterHandler dsfh = DSFilterHandlerFactory.getInstance(dsf.getHandler());
				query = dsfh.ModifyQuery(conn, dsf.getCludgyParameter(), query);
			}
		}

		if (query.hasUnprocessedListFilters()) {
			IDListFilter[] unprocessedFilters = query.getUnprocessedListFilters();
			for (int i = 0, n = unprocessedFilters.length; i < n; i++) {
				IDListFilter filter = unprocessedFilters[i];
				IDListFilterHandler idhandler = IDListFilterHandlerFactory.getInstance(filter.getType());
				query = idhandler.ModifyQuery(this, filter, query);
			}  
		}
      
		logger.info(query);
		QueryRunner qr = QueryRunnerFactory.getInstance(query, formatspec, conn, os);
		qr.execute(limit);
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	/**
	 * Returns a MartConfiguration object with all of the information needed to interact with
	 * the mart defined by the connection parameters provided to this Engine.
	 * 
	 * @return MartConfiguration object
	 */
	public MartConfiguration getMartConfiguration() throws ConfigurationException, SQLException {

		return new MartConfigurationFactory().getInstance(connection);
	}

	/**
	 * @param string
	 */
	public void setBaseConnectionString(String string) {
		baseConnectionString = string;
	}

	public String sql(Query query) {
		throw new RuntimeException();
	}

}
