/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.biomart.builder.controller.dialects;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.model.Column;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This class provides methods which generate atomic DDL or SQL statements. It
 * could be an interface, except for the static initializers which the
 * implementing classes use to register themselves. Once registered, the
 * {@link DatabaseDialect#getDialect(DataLink)} method will be able to identify
 * which dialect to use for a given {@link DataLink}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public abstract class DatabaseDialect {

	private static final Set dialects = new HashSet();

	/**
	 * Registers all known dialects for use with this system. Each implementing
	 * class should be added to this list.
	 */
	static {
		DatabaseDialect.dialects.add(new MySQLDialect());
		DatabaseDialect.dialects.add(new OracleDialect());
		DatabaseDialect.dialects.add(new PostgreSQLDialect());
	}

	/**
	 * Common constructor for subclasses does nothing except log that the
	 * subclass has been created and registered.
	 */
	protected DatabaseDialect() {
		Log
				.info(Resources.get("logRegisterDialect", this.getClass()
						.getName()));
	}

	/**
	 * Work out what kind of dialect to use for the given data link. It does
	 * this by checking each registered dialect to see if the
	 * {@link DatabaseDialect#understandsDataLink(DataLink)} method returns
	 * <tt>true</tt> for that data link. It returns the first one that returns
	 * <tt>true</tt>, ignoring any subsequent ones.
	 * 
	 * @param dataLink
	 *            the data link to work out the dialect for.
	 * @return the appropriate DatabaseDialect, or <tt>null</tt> if none
	 *         found.
	 */
	public static DatabaseDialect getDialect(final DataLink dataLink) {
		Log.info(Resources.get("logGetDialect", "" + dataLink));
		for (final Iterator i = DatabaseDialect.dialects.iterator(); i
				.hasNext();) {
			final DatabaseDialect d = (DatabaseDialect) i.next();
			if (d.understandsDataLink(dataLink)) {
				Log
						.info(Resources.get("logGotDialect", d.getClass()
								.getName()));
				return d;
			}
		}
		Log.info(Resources.get("logGotNoDialect"));
		return null;
	}

	/**
	 * Gets all distinct values in the given column. This method should perform
	 * the select and return the results as a list. If the column is empty, the
	 * list will be empty, but it will never be <tt>null</tt>.
	 * <p>
	 * The method will use the {@link Column#getTable()} method to discover
	 * which table to use, then call {@link Table#getSchema()} to find out what
	 * schema that table is in. It will use the {@link Column#getName()},
	 * {@link Table#getName()} and {@link Schema#getName()} methods to work out
	 * the names to use in the query. It is expected that the method will be
	 * able to work out from the type of {@link Schema} object it finds exactly
	 * how to connect to the database in question and use this information. For
	 * instance, if it finds a {@link JDBCSchema}, it knows that this will
	 * implement {@link JDBCDataLink} and therefore will be able to discover and
	 * use {@link JDBCDataLink#getConnection()} to connect to the database and
	 * execute the query.
	 * 
	 * @param col
	 *            the column to get the distinct values from.
	 * @return a list of the distinct values in the column.
	 * @throws SQLException
	 *             in case of problems.
	 */
	public abstract Collection executeSelectDistinct(Column col)
			throws SQLException;

	/**
	 * Gets rows from the given table. This method should perform the select and
	 * return the results. Each row is returned in a {@link List}, where each
	 * entry in the list is another {@link List} which contains the values of
	 * the columns, in order. If the column is empty, the list will be empty,
	 * but it will never be <tt>null</tt>. It is not possible for any member
	 * {@link List} inside the list to be empty or <tt>null</tt>.
	 * <p>
	 * An offset and count are provided to allow the function to be used to
	 * return a slice of the table instead of the whole table. The offset
	 * determines the number of the first row to return, and the count
	 * determines how many rows to return from that point. The offset is
	 * 0-indexed, such that the first row in a table is row 0.
	 * <p>
	 * The method will use {@link Table#getSchema()} to find out what schema
	 * that table is in. It will use the {@link Table#getName()} and
	 * {@link Schema#getName()} methods to work out the names to use in the
	 * query. It is expected that the method will be able to work out from the
	 * type of {@link Schema} object it finds exactly how to connect to the
	 * database in question and use this information. For instance, if it finds
	 * a {@link JDBCSchema}, it knows that this will implement
	 * {@link JDBCDataLink} and therefore will be able to discover and use
	 * {@link JDBCDataLink#getConnection()} to connect to the database and
	 * execute the query.
	 * <p>
	 * Note that if the table belongs to a grouped schema, this function is
	 * undefined.
	 * 
	 * @param table
	 *            the table to get the rows from.
	 * @param offset
	 *            how far into the table to start. This value is 0-indexed, so
	 *            the first row is row 0.
	 * @param count
	 *            how many rows to get.
	 * @return the rows as a nested list.
	 * @throws SQLException
	 *             in case of problems.
	 */
	public abstract Collection executeSelectRows(Table table, int offset,
			int count) throws SQLException;

	/**
	 * Given a particular action, return a SQL or DDL statement that will
	 * perform it. If multiple statements are required to perform the action,
	 * they are returned as an array. Each line of the array is considered to be
	 * a complete single statement which could, for example, be executed
	 * directly with a JDBC database function such as
	 * {@link PreparedStatement#execute()}.
	 * <p>
	 * Note that the statements returned should not be parameterised. They
	 * should contain all values hard-coded into them, as the user may choose to
	 * save them to file for later use, preventing parameterisation from
	 * working.
	 * 
	 * @param action
	 *            the action to translate into SQL or DDL.
	 * @param includeComments
	 *            <tt>true</tt> if comments exlaining the process should be
	 *            embedded into the generated statements, <tt>false</tt> if
	 *            not.
	 * @return the statement(s) that represent the action.
	 * @throws ConstructorException
	 *             if the action was not able to be converted into one or more
	 *             SQL or DDL statements.
	 */
	public abstract String[] getStatementsForAction(
			MartConstructorAction action, boolean includeComments)
			throws ConstructorException;

	/**
	 * Call this method before using the dialect for anything. This is necessary
	 * in order to clear out any state it may be keeping track of.
	 */
	public abstract void reset();

	/**
	 * Test to see whether this particular dialect implementation can understand
	 * the data link given, ie. it knows how to interact with it and speak the
	 * appropriate version of SQL or DDL.
	 * 
	 * @param dataLink
	 *            the data link to test compatibility with.
	 * @return <tt>true</tt> if it understands it, <tt>false</tt> if not.
	 */
	public abstract boolean understandsDataLink(DataLink dataLink);
}
