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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.ensembl.mart.lib.DatabaseUtil;

import junit.framework.TestCase;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatabaseUtilTest extends TestCase {

	private Logger logger = Logger.getLogger(DatabaseUtilTest.class.getName());

	/**
	 * Constructor for DatabaseUtilTest.
	 * @param arg0
	 */
	public DatabaseUtilTest(String arg0) {
		super(arg0);
	}

	public void testConnectionStringMethodod() throws Exception {
		DataSource ds =
			DatabaseUtil.createDataSource(
				"jdbc:mysql://kaka.sanger.ac.uk/",
				"anonymous",
				null,
				10,
				"com.mysql.jdbc.Driver");

		Connection conn = ds.getConnection();
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables("homo_sapiens_core_16_33", null, null, null);
		assertTrue("Failed to get any metadata from database", rs.next());
    
    if (logger.isLoggable(Level.FINE)) print(rs);
		
    conn.close();
	}

	public void testConvenienceMethod() throws Exception {
		DataSource ds =
			DatabaseUtil.createDataSource(
				"mysql",
				"kaka.sanger.ac.uk",
				"3306",
				"homo_sapiens_core_16_33",
				"anonymous",
				null,
				10,
				"com.mysql.jdbc.Driver");

		Connection conn = ds.getConnection();
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables("homo_sapiens_core_16_33", null, null, null);
		assertTrue("Failed to get any metadata from database", rs.next());

    if (logger.isLoggable(Level.FINE)) print(rs);

		conn.close();
	}

	/**
	 * @param rs
	 */
	private void print(ResultSet rs) throws Exception {
		do {
			int n = rs.getMetaData().getColumnCount();
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i));
				System.out.print('\t');

			}
			System.out.println();
		} while (rs.next());
	}

}
