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
import java.util.Collections;
import java.util.List;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataLink.JDBCDataLink;

/**
 * Understands how to create SQL and DDL for a MySQL database.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 16th June 2006
 * @since 0.1
 */
public class MySQLDialect extends DatabaseDialect {

	public boolean understandsDataLink(DataLink dataLink)
			throws ConstructorException {

		// Convert to JDBC version.
		if (!(dataLink instanceof JDBCDataLink))
			return false;		
		JDBCDataLink jddl = (JDBCDataLink) dataLink;

		try {
			return jddl.getConnection().getMetaData().getDatabaseProductName()
					.equals("MySQL");
		} catch (SQLException e) {
			throw new ConstructorException(e);
		}
	}

	public List executeSelectDistinct(Column col) throws SQLException {
		// TODO: implement
		return Collections.EMPTY_LIST;
	}

	public String escapeQuotedString(String string) {
		// TODO: implement
		return string;
	}

}
