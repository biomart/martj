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

package org.biomart.common.controller;

import java.sql.SQLException;
import java.util.List;

import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.GenericForeignKey;
import org.biomart.common.model.Key.GenericPrimaryKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.model.Schema.JDBCSchema;

/**
 * Tools for working with BioMart apps from a GUI or CLI. These wrapper methods
 * exist to prevent the GUI or CLI having to know about the exact details of
 * manipulating the various objects in the data model.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class CommonUtils {
	/**
	 * Attempts to create a foreign key on a table given a set of columns. The
	 * new key will have a status of {@link ComponentStatus#HANDMADE}.
	 * 
	 * @param table
	 *            the table to create the key on.
	 * @param columns
	 *            the colums, in order, to create the key over.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void createForeignKey(final Table table, final List columns)
			throws AssociationException {
		final ForeignKey fk = new GenericForeignKey(columns);
		fk.setStatus(ComponentStatus.HANDMADE);
		table.addForeignKey(fk);
	}

	/**
	 * Creates a JDBC schema and returns it. No further action is taken - the
	 * schema will not synchronise itself.
	 * 
	 * @param driverClassName
	 *            the name of the JDBC driver class, eg.
	 *            <tt>com.mysql.jdbc.Driver</tt>
	 * @param url
	 *            the JDBC url to connect to.
	 * @param schemaName
	 *            the target schema to connect to.
	 * @param username
	 *            the username to connect with.
	 * @param password
	 *            the password to connect with. If the empty string is passed
	 *            in, no password is given to the connection at all.
	 * @param name
	 *            the name to give the created schema.
	 * @param keyGuessing
	 *            a flag indicating whether to enable key-guessing on this JDBC
	 *            schema or not. This can always be changed later.
	 * @return the created schema.
	 */
	public static JDBCSchema createJDBCSchema(final String driverClassName,
			final String url, final String schemaName, final String username,
			final String password, final String name, final boolean keyGuessing) {
		return new JDBCSchema(driverClassName, url, schemaName, username,
				password, name, keyGuessing);
	}

	/**
	 * Attempts to create a primary key on a table given a set of columns. If
	 * the table already has a primary key, then this one will replace it. The
	 * new key will have a status of {@link ComponentStatus#HANDMADE}.
	 * 
	 * @param table
	 *            the table to create the key on.
	 * @param columns
	 *            the colums, in order, to create the key over.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void createPrimaryKey(final Table table, final List columns)
			throws AssociationException {
		final PrimaryKey pk = new GenericPrimaryKey(columns);
		pk.setStatus(ComponentStatus.HANDMADE);
		table.setPrimaryKey(pk);
	}

	/**
	 * Turns key-guessing off in a given schema.
	 * 
	 * @param schema
	 *            the schema to disable key-guessing in.
	 * @throws SQLException
	 *             if after disabling keyguessing the key sync went wrong.
	 * @throws DataModelException
	 *             if after disabling keyguessing the key sync went wrong.
	 */
	public static void disableKeyGuessing(final Schema schema)
			throws SQLException, DataModelException {
		schema.setKeyGuessing(false);
	}

	/**
	 * Turns key-guessing on in a given schema.
	 * 
	 * @param schema
	 *            the schema to enable key-guessing in.
	 * @throws SQLException
	 *             if after keyguessing the key sync went wrong.
	 * @throws DataModelException
	 *             if after keyguessing the key sync went wrong.
	 */
	public static void enableKeyGuessing(final Schema schema)
			throws DataModelException, SQLException {
		schema.setKeyGuessing(true);
	}

	/**
	 * Synchronises an individual schema against the data source or database it
	 * represents. Datasets using this schema will also be synchronised.
	 * 
	 * @param schema
	 *            the schema to synchronise.
	 * @throws SQLException
	 *             if there was any problem communicating with the data source
	 *             or database.
	 * @throws DataModelException
	 *             if there were any logical problems with synchronisation.
	 */
	public static void synchroniseSchema(final Schema schema)
			throws SQLException, DataModelException {
		schema.synchronise();
	}

	/**
	 * Tests a schema. In most cases, this simply tests the connection between
	 * the schema and the data source or database it represents. The connection
	 * between test failure and throwing an exception describing the failure
	 * means that this routine will (probably) never return <tt>false</tt>,
	 * only ever <tt>true</tt> or an exception.
	 * 
	 * @param schema
	 *            the schema to test.
	 * @return <tt>true</tt> if it passed the test, <tt>false</tt>
	 *         otherwise.
	 * @throws Exception
	 *             if it failed the test. The exception will describe the reason
	 *             for failure.
	 */
	public static boolean testSchema(final Schema schema) throws Exception {
		return schema.test();
	}

	/**
	 * Updates the schema partition regex and naming expression.
	 * 
	 * @param schema
	 *            the schema to update partitions on.
	 * @param regex
	 *            the new regex. <tt>null</tt> to turn it off altogether.
	 * @param expr
	 *            the new expression. Will be ignored if regex is <tt>null</tt>.
	 */
	public static void setSchemaPartition(final Schema schema,
			final String regex, final String expr) {
		if (regex == null) {
			schema.setPartitionRegex(null);
			schema.setPartitionNameExpression(null);
		} else {
			schema.setPartitionRegex(regex);
			schema.setPartitionNameExpression(expr);
		}
	}

	// The tools are static and not intended to be instantiated.
	private CommonUtils() {
	}
}
