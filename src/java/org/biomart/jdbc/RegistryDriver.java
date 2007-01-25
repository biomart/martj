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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class RegistryDriver extends RegistryDataSource implements Driver {
	// The pattern for matching URLs that we accept.
	private static final Pattern URL_PATTERN = Pattern
			.compile("^biomart:(.*)$");

	// The static initializer registers us with JDBC.
	static {
		try {
			DriverManager.registerDriver(new RegistryDriver());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new Driver object.
	 */
	public RegistryDriver() {
		super();
	}

	public boolean acceptsURL(final String url) throws SQLException {
		return RegistryDriver.URL_PATTERN.matcher(url).matches();
	}

	public Connection connect(final String url, final Properties info)
			throws SQLException {
		this.setProperties(info);
		return this.getRegistryConnection();
	}

	public int getMajorVersion() {
		return RegistryDataSource.MAJOR_VERSION;
	}

	public int getMinorVersion() {
		return RegistryDataSource.MINOR_VERSION;
	}

	public DriverPropertyInfo[] getPropertyInfo(final String url,
			final Properties info) throws SQLException {
		this.setProperties(info);
		return this.getMissingProperties();
	}

	// Use JavaBeans stuff to set properties on ourself.
	private void setProperties(final Properties info) throws SQLException {
		for (final Iterator entries = info.entrySet().iterator(); entries.hasNext(); ) {
			final Map.Entry entry = (Map.Entry)entries.next();
			final String fieldName = entry.getKey().toString();
			String methodName = "set" + fieldName.toUpperCase().charAt(0);
			if (fieldName.length() > 1)
				methodName += fieldName.substring(1);
			try {
				final Object value = entry.getValue();
				// If passed a null value, we default to the String setter.
				final Method method = RegistryDataSource.class.getMethod(
						methodName, new Class[] { value == null ? String.class
								: value.getClass() });
				method.invoke(this, new Object[] { value });
			} catch (NoSuchMethodException e) {
				// Ignore this. We don't care if they set non-existent
				// properties.
			} catch (IllegalAccessException e) {
				// Ignore this. We don't care if they set properties
				// that we don't have access to.
			} catch (InvocationTargetException e) {
				// Throw this. The value they passed was dodge.
				final SQLException se = new SQLException(Resources.get(
						"incorrectPropertyValue", fieldName));
				se.initCause(e.getTargetException());
				throw se;
			}
		}
	}

	public boolean jdbcCompliant() {
		// Yeah, like, you wish.
		return false;
	}

}
