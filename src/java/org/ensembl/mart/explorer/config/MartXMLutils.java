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

package org.ensembl.mart.explorer.config;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartXMLutils {

	private static final String GETSQL = "select stream from _meta_martConfiguration_XML where system_id = ?";
	private static final String EXISTSQL = "select count(system_id) from _meta_martConfiguration_XML where system_id = ?";
	private static final String UPDATEXMLSQL = "update _meta_martConfiguration_XML set stream = ? where system_id = ?"; // if EXISTSQL returns 1
	private static final String INSERTXMLSQL = "insert into _meta_martConfiguration_XML (system_id, stream) values (?,?)";

	public static InputSource getInputSourceFor(Connection conn, String systemID) throws ConfigurationException {
		try {
			PreparedStatement ps = conn.prepareStatement(GETSQL);
			ps.setString(1, systemID);

			ResultSet rs = ps.executeQuery();
			rs.next(); // will only get one result

			InputStream stream = rs.getBinaryStream(1); // will only get one row
			rs.close();

			InputSource is = new InputSource(systemID); // allow the InputSource to carry the systemID with it
			is.setByteStream(stream);
			return is;
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQL Exception during fetch of requested InputSource: " + e.getMessage());
		}
	}

	public static void storeConfiguration(Connection conn, String systemID, Document doc) throws ConfigurationException {
		Logger logger = Logger.getLogger(MartXMLutils.class.getName()); // may need to log some warnings
		int rowsupdated = 0;

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

			xout.output(doc, out);

			byte[] xml = out.toByteArray();
			out.close();

			// check for existence
			boolean exists = false;

			PreparedStatement es = conn.prepareStatement(EXISTSQL);
			es.setString(1, systemID);

			ResultSet er = es.executeQuery();
			er.next(); // only one result
			if (er.getInt(1) > 0)
				exists = true;
			er.close();

			if (exists) {
				PreparedStatement ps = conn.prepareStatement(UPDATEXMLSQL);
				ps.setBytes(1, xml);
				ps.setString(2, systemID);

				rowsupdated = ps.executeUpdate();
			} else {
				PreparedStatement ps = conn.prepareStatement(INSERTXMLSQL);
				ps.setString(1, systemID);
				ps.setBytes(2, xml);

				rowsupdated = ps.executeUpdate();
			}
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQLException updating xml for " + systemID + ": " + e.getMessage());
		}

		if (rowsupdated < 1)
			logger.warn("Warning, xml for " + systemID + " not stored"); //throw an exception?	
	}

	public static void storeDTD(Connection conn, String systemID, URL dtdurl) throws ConfigurationException {
		Logger logger = Logger.getLogger(MartXMLutils.class.getName()); // may need to log some warnings
		int rowsupdated = 0;

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(dtdurl.openStream()));
			StringBuffer buf = new StringBuffer();

			for (String line = in.readLine(); line != null; line = in.readLine())
				buf.append(line);

			in.close();

			String dtd = buf.toString();

			// check for existence
			boolean exists = false;

			PreparedStatement es = conn.prepareStatement(EXISTSQL);
			es.setString(1, systemID);

			ResultSet er = es.executeQuery();
			er.next(); // only one result
			if (er.getInt(1) > 0)
				exists = true;
			er.close();

			if (exists) {
				PreparedStatement ps = conn.prepareStatement(UPDATEXMLSQL);
				ps.setString(1, dtd);
				ps.setString(2, systemID);

				rowsupdated = ps.executeUpdate();
			} else {
				PreparedStatement ps = conn.prepareStatement(INSERTXMLSQL);
				ps.setString(1, systemID);
				ps.setString(2, dtd);

				rowsupdated = ps.executeUpdate();
			}
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQLException updating xml for " + systemID + ": " + e.getMessage());
		}

		if (rowsupdated < 1)
			logger.warn("Warning, xml for " + systemID + " not stored"); //throw an exception?						
	}
}
