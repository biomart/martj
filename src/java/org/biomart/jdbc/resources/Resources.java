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

package org.biomart.jdbc.resources;

import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Simple wrapper for locating file resources within this package, and for
 * reading internationalisation messages from the messages file.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class Resources {
	private static ResourceBundle bundle = ResourceBundle
			.getBundle("org/biomart/jdbc/resources/messages");

	private static String getValue(final String key) {
		String value = null;
		try {
			value = Resources.bundle.getString(key);
		} catch (MissingResourceException e) {
			value = null;
		}
		return value;
	}

	/**
	 * Obtains a string from the resource bundle
	 * "org/biomart/jdbc/resources/messages.properties". Runs it through
	 * MessageFormat before returning. See
	 * {@link ResourceBundle#getString(String)} for full description of
	 * behaviour.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the matching string.
	 */
	public static String get(final String key) {
		return MessageFormat.format(Resources.getValue(key), new Object[] {});
	}

	/**
	 * Obtains a string from the resource bundle
	 * "org/biomart/jdbc/resources/messages.properties". Substitutes the first
	 * parameter in the resulting string for the specified value using
	 * MessageFormat. See {@link ResourceBundle#getString(String)} for full
	 * description of behaviour.
	 * 
	 * @param key
	 *            the key to look up.
	 * @param value
	 *            the value to substitute in the first placeholder of the string
	 *            we looked up.
	 * @return the matching string.
	 */
	public static String get(final String key, final String value) {
		return MessageFormat.format(Resources.getValue(key),
				new Object[] { value });
	}

	/**
	 * Obtains a string from the resource bundle
	 * "org/biomart/jdbc/resources/messages.properties". Substitutes all
	 * parameters in the resulting string for the specified values using
	 * MessageFormat. See {@link ResourceBundle#getString(String)} for full
	 * description of behaviour.
	 * 
	 * @param key
	 *            the key to look up.
	 * @param values
	 *            the values to substitute in the placeholders in the looked-up
	 *            string. There should be the same number of values as there are
	 *            placeholders.
	 * @return the matching string.
	 */
	public static String get(final String key, final String[] values) {
		return MessageFormat.format(Resources.getValue(key), values);
	}

	/**
	 * Given a resource name (a file inside some package somewhere), return a
	 * stream that will read the contents of that file.
	 * 
	 * @param resource
	 *            the classpath of the resource to lookup, e.g. "myfile.txt".
	 * @return a stream that will read that file.
	 */
	public static InputStream getResourceAsStream(final String resource) {
		final String resourceLocation = "org/biomart/jdbc/resources/"
				+ resource;
		final ClassLoader cl = Resources.class.getClassLoader();
		return (InputStream) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						InputStream resource;
						if (cl != null)
							resource = cl.getResourceAsStream(resourceLocation);
						else
							resource = ClassLoader
									.getSystemResourceAsStream(resourceLocation);
						return resource;
					}
				});
	}

	/**
	 * Given a resource name (a file inside some package somewhere), return a
	 * URL pointing to it.
	 * 
	 * @param resource
	 *            the classpath of the resource to lookup, e.g. "myfile.txt".
	 * @return a URL pointing to that file.
	 */
	public static URL getResourceAsURL(final String resource) {
		final String resourceLocation = "org/biomart/jdbc/resources/"
				+ resource;
		final ClassLoader cl = Resources.class.getClassLoader();
		return (URL) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				URL resource;
				if (cl != null)
					resource = cl.getResource(resourceLocation);
				else
					resource = ClassLoader.getSystemResource(resourceLocation);
				return resource;
			}
		});
	}
}
