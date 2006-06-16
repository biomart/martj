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
package org.biomart.builder.controller;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.resources.BuilderBundle;

/**
 * DatabaseDialect provides methods which generate atomic DDL or SQL statements. Each
 * implementation should register itself with {@link #registerDialect(DatabaseDialect)}
 * in a static initializer so that it can be used.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 16th June 2006
 * @since 0.1
 */
public abstract class DatabaseDialect {
	private static final Set dialects = new HashSet();

	/**
	 * Registers all known dialects for use with this system. Need only
	 * be called once, but doesn't hurt to call multiple times.
	 */
	public static void registerDialects() {
		dialects.add(new MySQLDialect());
	}

	/**
	 * Work out what kind of dialect to use for the given data link.
	 * 
	 * @param dataLink
	 *            the data link to work out the dialect for.
	 * @return the appropriate DatabaseDialect.
	 * @throws ConstructorException
	 *             if there is no appropriate dialect.
	 */
	public static DatabaseDialect getDialect(DataLink dataLink)
			throws ConstructorException {
		for (Iterator i = dialects.iterator(); i.hasNext();) {
			DatabaseDialect d = (DatabaseDialect) i.next();
			if (d.understandsDataLink(dataLink))
				return d;
		}
		throw new ConstructorException(BuilderBundle
				.getString("mcUnknownDataLink"));
	}

	/**
	 * Test to see whether this particular dialect implementation can understand
	 * the data link given, ie. it knows how to interact with it and speak the
	 * appropriate version of SQL or DDL.
	 * 
	 * @param dataLink
	 *            the data link to test compatibility with.
	 * @return <tt>true</tt> if it understands it, <tt>false</tt> if not.
	 * @throws ConstructorException
	 *             if there was any problem trying to establish whether or not
	 *             the data link is compatible with this dialect.
	 */
	public abstract boolean understandsDataLink(DataLink dataLink)
			throws ConstructorException;
	
	/**
	 * Gets the distinct values in the given column. This must be a real column,
	 * not an instance of {@link DataSetColumn}. This method actually performs
	 * the select and returns the results as a list.
	 * 
	 * @param col
	 *            the column to get the distinct values from.
	 * @return the distinct values in the column.
	 * @throws SQLException
	 *             in case of problems.
	 */
	public abstract List executeSelectDistinct(Column col) throws SQLException;

	/**
	 * Escapes a string so that it may safely be contained as a quoted literal
	 * within a statement. The quotes themselves should not be added by this
	 * method.
	 * 
	 * @param string
	 *            the string to escape.
	 * @return the escaped string.
	 */
	public abstract String escapeQuotedString(String string);
	
	//TODO: further methods.
}
