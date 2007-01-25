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
package org.biomart.jdbc.formatters;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.biomart.jdbc.Formatter;
import org.biomart.jdbc.QueryResultSet;

/**
 * Tab-separated values. Embedded tabs will be escaped using backslash.
 * Embedded backslashes will be escaped with an extra backslash.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class TSV implements Formatter {

	public void formatResults(final QueryResultSet results,
			final OutputStream stream) throws IOException, SQLException {
		final int colCount = results.getMetaData().getColumnCount();
		while (results.next()) {
			for (int i = 0; i < colCount; i++) {
				// Separate values after first one with tabs.
				if (i>0)
					stream.write('\t');
				// Get the value to write.
				String value = results.getString(i+1);
				// Don't print anything for null strings. Note
				// use of wasNull to make doubly sure.
				if (results.wasNull())
					continue;
				// Escape backslashes.
				if (value.indexOf('\\')>=0)
					value = value.replaceAll("\\", "\\\\");
				// Escape tabs.
				if (value.indexOf('\t')>=0)
					value = value.replaceAll("\t","\\\t");
				// Write the field.
				stream.write(value.getBytes());
			}
			// Write out line separator if there is going to be another line.
			if (results.isLast())
				stream.write(System.getProperty("line.separator").getBytes());
		}
	}

	public Properties getRequiredAttributes() {
		return Formatter.NO_ATTRIBUTES;
	}
}
