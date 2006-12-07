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
	 * Takes the given results and prepares to format them. It should be called
	 * before any results are obtained from the {@link QueryResultSet}.
	 * Once attached to a {@link QueryResultSet} by using this method,
	 * the {@link QueryResultSet} should not be touched outside the formatter,
	 * other than to be closed when the user is done with the query.
	 * 
	 * @param results
	 *            the {@link QueryResultSet} producing results to be formatted.
	 * @throws SQLException
	 *             if anything goes wrong with the attaching of the formatter.
	 */
	public void formatResults(final QueryResultSet results) throws SQLException;

	/**
	 * Returns the required attributes for this formatter, in the form of
	 * a {@link Properties} object where each key is a required attribute
	 * and the values are the names of the attributes it will look for in
	 * the query. If you want to override the values, do so by changing
	 * them on this returned object before formatting the results.
	 * 
	 * @return
	 * 		      the required attributes for this formatter, if any. The
	 * 			  returned object will never be <tt>null</tt> but may be
	 * 			  empty.
	 */
	public Properties getRequiredAttributes();

}
