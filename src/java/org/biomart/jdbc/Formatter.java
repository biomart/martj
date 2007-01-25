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
package org.biomart.jdbc;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public interface Formatter {

	/**
	 * NO_ATTRIBUTES represents an empty properties set to be returned by
	 * {@link #getRequiredAttributes()} if it doesn't need any special
	 * attributes.
	 */
	public static final Properties NO_ATTRIBUTES = new Properties();

	/**
	 * Takes the given results and prepares to format them.
	 * <p>
	 * The formatter will iterate over all results in the {@link QueryResultSet}
	 * and format them to the output stream. The stream will be closed when the
	 * last result is written. The {@link QueryResultSet} will NOT be closed and
	 * must be closed by the caller. Any results obtained from the
	 * {@link QueryResultSet} before this method is called will not be included
	 * in the formatted results.
	 * 
	 * @param results
	 *            the {@link QueryResultSet} producing results to be formatted.
	 * @param stream
	 *            the stream of formatted results. Depending on the formatter,
	 *            this may be either binary or ascii data.
	 * @throws IOException
	 *             if anything goes wrong trying to write out results.
	 * @throws SQLException
	 *             if anything goes wrong with the results or query.
	 */
	public void formatResults(final QueryResultSet results,
			final OutputStream stream) throws IOException, SQLException;

	/**
	 * If intending to use a particular formatter, make sure you call this
	 * method first to see what attributes it requires, and that you have
	 * specified those attributes in your query.
	 * <p>
	 * The required attributes for this formatter are in the form of a
	 * {@link Properties} object where each key is a required attribute and the
	 * values are the names of the attributes it will look for in the query.
	 * <p>
	 * If you want to override the names of the attributes that the formatter
	 * will look for in the query, do so by changing them on this returned
	 * object before formatting the results.
	 * 
	 * @return the required attributes for this formatter, if any. The returned
	 *         object will never be <tt>null</tt> but may be empty - in which
	 *         case it should return {@link #NO_ATTRIBUTES}.
	 */
	public Properties getRequiredAttributes();

}
