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

package org.ensembl.mart.lib.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartXMLutils {

	//TODO: Factor out old getInputSourceFor methods
	private static final String GETSQL = "select stream, compressed_stream from _meta_martConfiguration_XML where system_id = ?";

	private static final String GETALLDSVDATA = "select internalName, displayName, stream, compressed_stream from _meta_martConfiguration_XML"; //TODO: md5sum
	private static final String GETBYDNAMESQL = "select stream, compressed_stream from _meta_martConfiguration_XML where displayName = ?";
	private static final String GETBYINAMESQL = "select stream, compressed_stream from _meta_martConfiguration_XML where internalName = ?";

	private static final String EXISTSQL = "select count(system_id) from _meta_martConfiguration_XML where system_id = ?";

	//if EXISTSQL returns 1
	private static final String DELETEOLDXML = "delete from _meta_martConfiguration_XML where system_id = ?";
	private static final String INSERTXMLSQL = "insert into _meta_martConfiguration_XML (system_id, stream) values (?,?)";
	private static final String INSERTCOMPRESSEDXMLSQL = "insert into _meta_martConfiguration_XML (system_id, compressed_stream) values (?,?)";
	private static Logger logger = Logger.getLogger(MartXMLutils.class.getName());

	public static InputSource getInputSourceFor(Connection conn, String systemID) throws ConfigurationException {
		try {
			PreparedStatement ps = conn.prepareStatement(GETSQL);
			ps.setString(1, systemID);

			ResultSet rs = ps.executeQuery();
			rs.next(); // will only get one result

			byte[] stream = rs.getBytes(1);
			byte[] cstream = rs.getBytes(2);
			rs.close();

			InputStream rstream = null;
			if (cstream != null)
				rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
			else
				rstream = new ByteArrayInputStream(stream);

			InputSource is = new InputSource(systemID); // allow the InputSource to carry the systemID with it
			is.setByteStream(rstream);
			return is;
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQL Exception during fetch of requested InputSource: " + e.getMessage());
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException during fetch of requested inputSource: " + e.getMessage());
		}
	}

	public static InputSource getInputSourceFor(URL systemID) throws ConfigurationException {
		try {
			InputSource is = new InputSource(systemID.toString()); // use the URL as the system id
			is.setByteStream(systemID.openStream());
			return is;
		} catch (Exception e) {
			throw new ConfigurationException("Caught Exception during fetch of requested InputSource: " + e.getMessage());
		}
	}

	//TODO: store by internalName, displayName, and MD5SUM
	public static void storeConfiguration(Connection conn, String systemID, Document doc, boolean compress) throws ConfigurationException {
		int rowsupdated = 0;

		if (compress)
			rowsupdated = storeCompressedXML(conn, systemID, doc);
		else
			rowsupdated = storeUncompressedXML(conn, systemID, doc);

		if (rowsupdated < 1)
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Warning, xml for " + systemID + " not stored"); //throw an exception?	
	}

	private static int storeUncompressedXML(Connection conn, String systemID, Document doc) throws ConfigurationException {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

			xout.output(doc, bout);

			byte[] xml = bout.toByteArray();
			bout.close();

			// check for existence
			boolean exists = false;

			PreparedStatement es = conn.prepareStatement(EXISTSQL);
			es.setString(1, systemID);

			ResultSet er = es.executeQuery();
			er.next(); // only one result
			int rowstodelete = er.getInt(1);

			if (rowstodelete > 0) {
				PreparedStatement ds = conn.prepareStatement(DELETEOLDXML);
				ds.setString(1, systemID);
				int rowsdeleted = ds.executeUpdate();

				if (!(rowsdeleted == rowstodelete))
					throw new ConfigurationException("Did not delete old XML data rows for " + systemID + "\n");
			}

			PreparedStatement ps = conn.prepareStatement(INSERTXMLSQL);
			ps.setString(1, systemID);
			ps.setBytes(2, xml);

			return ps.executeUpdate();
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQLException updating xml for " + systemID + ": " + e.getMessage());
		}
	}

	private static int storeCompressedXML(Connection conn, String systemID, Document doc) throws ConfigurationException {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			GZIPOutputStream out = new GZIPOutputStream(bout);
			XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

			xout.output(doc, out);
			out.finish();

			byte[] xml = bout.toByteArray();

			bout.close();
			out.close();

			// check for existence
			boolean exists = false;

			PreparedStatement es = conn.prepareStatement(EXISTSQL);
			es.setString(1, systemID);

			ResultSet er = es.executeQuery();
			er.next();
			int rowstodelete = er.getInt(1);

			if (rowstodelete > 0) {
				PreparedStatement ds = conn.prepareStatement(DELETEOLDXML);
				ds.setString(1, systemID);
				int rowsdeleted = ds.executeUpdate();

				if (!(rowsdeleted == rowstodelete))
					throw new ConfigurationException("Did not delete old XML data rows for " + systemID + "\n");
			}

			PreparedStatement ps = conn.prepareStatement(INSERTCOMPRESSEDXMLSQL);
			ps.setString(1, systemID);
			ps.setBytes(2, xml);

			return ps.executeUpdate();

		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQLException updating xml for " + systemID + ": " + e.getMessage());
		}
	}

	public static void storeDTD(Connection conn, String systemID, URL dtdurl, boolean compress) throws ConfigurationException {
		int rowsupdated = 0;

		if (compress)
			rowsupdated = storeCompressedDTD(conn, systemID, dtdurl);
		else
			rowsupdated = storeUncompressedDTD(conn, systemID, dtdurl);

		if (rowsupdated < 1)
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Warning, xml for " + systemID + " not stored"); //throw an exception?						
	}

	private static int storeUncompressedDTD(Connection conn, String systemID, URL dtdurl) throws ConfigurationException {
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
			int rowstodelete = er.getInt(1);

			if (rowstodelete > 0) {
				PreparedStatement ds = conn.prepareStatement(DELETEOLDXML);
				ds.setString(1, systemID);
				int rowsdeleted = ds.executeUpdate();

				if (!(rowsdeleted == rowstodelete))
					throw new ConfigurationException("Did not delete old XML data rows for " + systemID + "\n");
			}

			PreparedStatement ps = conn.prepareStatement(INSERTXMLSQL);
			ps.setString(1, systemID);
			ps.setString(2, dtd);

			return ps.executeUpdate();
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQLException updating xml for " + systemID + ": " + e.getMessage());
		}
	}

	private static int storeCompressedDTD(Connection conn, String systemID, URL dtdurl) throws ConfigurationException {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			GZIPOutputStream zout = new GZIPOutputStream(bout);

			InputStream in = dtdurl.openStream();

			int b;
			while ((b = in.read()) != -1)
				zout.write(b);

			zout.finish();
			zout.close();

			in.close();

			byte[] dtd = bout.toByteArray();
			bout.close();

			// check for existence
			boolean exists = false;

			PreparedStatement es = conn.prepareStatement(EXISTSQL);
			es.setString(1, systemID);

			ResultSet er = es.executeQuery();
			er.next(); // only one result
			int rowstodelete = er.getInt(1);

			if (rowstodelete > 0) {
				PreparedStatement ds = conn.prepareStatement(DELETEOLDXML);
				ds.setString(1, systemID);
				int rowsdeleted = ds.executeUpdate();

				if (!(rowsdeleted == rowstodelete))
					throw new ConfigurationException("Did not delete old XML data rows for " + systemID + "\n");
			}

			PreparedStatement ps = conn.prepareStatement(INSERTCOMPRESSEDXMLSQL);
			ps.setString(1, systemID);
			ps.setBytes(2, dtd);

			return ps.executeUpdate();
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQLException updating xml for " + systemID + ": " + e.getMessage());
		}
	}

	//TODO:impliment user/fallback tables
  //TODO:impliment MD5SUM in DatasetView
	public static InputStream getDatasetViewXMLStreamByInternalName(DataSource dsvsource, String user, String internalName) throws ConfigurationException {
		try {
			Connection conn = dsvsource.getConnection();
			PreparedStatement ps = conn.prepareStatement(GETBYINAMESQL);
			ps.setString(1, internalName);

			ResultSet rs = ps.executeQuery();
			rs.next(); // will only get one result

			byte[] stream = rs.getBytes(1);
			byte[] cstream = rs.getBytes(2);
			rs.close();
      conn.close();
      
			InputStream rstream = null;
			if (cstream != null)
				rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
			else
				rstream = new ByteArrayInputStream(stream);

			return rstream;
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQL Exception during fetch of requested stream: " + e.getMessage());
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException during fetch of requested stream: " + e.getMessage());
		}
	}

	//TODO:impliment user/fallback tables
  //TODO:impliment MD5SUM in DatasetView
	public static InputStream getDatasetViewXMLStreamByDisplayName(DataSource dsvsource, String user, String displayName) throws ConfigurationException {
		try {
			Connection conn = dsvsource.getConnection();
			PreparedStatement ps = conn.prepareStatement(GETBYDNAMESQL);
			ps.setString(1, displayName);

			ResultSet rs = ps.executeQuery();
			rs.next(); // will only get one result

			byte[] stream = rs.getBytes(1);
			byte[] cstream = rs.getBytes(2);
			rs.close();
      conn.close();

			InputStream rstream = null;
			if (cstream != null)
				rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
			else
				rstream = new ByteArrayInputStream(stream);

			return rstream;
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQL Exception during fetch of requested stream: " + e.getMessage());
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException during fetch of requested stream: " + e.getMessage());
		}
	}

	//TODO:impliment user/fallback tables
  //TODO:create stub DatasetViews instead of full DatasetViews
  //TODO:impliment MD5SUM in DatasetView
	public static DSViewDatabaseAdaptor getDSViewDatabaseAdaptorFor(DataSource dsource, String user) throws ConfigurationException {

		try {
			DSViewDatabaseAdaptor dsva = new DSViewDatabaseAdaptor(dsource, user);
			Connection conn = dsource.getConnection();
			PreparedStatement ps = conn.prepareStatement(GETALLDSVDATA);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
        String internalName = rs.getString(1); // ignore for now, but will use
        String displayName = rs.getString(2); // these to create stubs later
				byte[] stream = rs.getBytes(3);
				byte[] cstream = rs.getBytes(4);


				InputStream rstream = null;
				if (cstream != null)
					rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
				else
					rstream = new ByteArrayInputStream(stream);
        
        DatasetView dsv = ConfigurationUtils.getDatasetView(rstream, false);
        dsva.addDatasetView(dsv);
			}
      rs.close();
      conn.close();
      
      return dsva;
		} catch (SQLException e) {
			throw new ConfigurationException("Caught SQL Exception during fetch of requested stream: " + e.getMessage());
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException during fetch of requested stream: " + e.getMessage());
		}
	}
}