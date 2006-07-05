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

package org.biomart.builder.resources;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Simple wrapper for resources.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 5th July 2006
 * @since 0.1
 */
public class Resources {
	private static ResourceBundle bundle = ResourceBundle
			.getBundle("org/biomart/builder/resources/messages");

	/**
	 * Obtains a string from the resource bundle
	 * "org/biomart/builder/resources/messages.properties". Runs it through
	 * MessageFormat before returning. See
	 * {@link ResourceBundle#getString(String)} for full description of
	 * behaviour.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the matching string.
	 */
	public static String get(String key) {
		return MessageFormat.format(bundle.getString(key), new Object[] {});
	}

	/**
	 * Obtains a string from the resource bundle
	 * "org/biomart/builder/resources/messages.properties". Substitutes the
	 * first parameter in the resulting string for the specified value using
	 * MessageFormat. See {@link ResourceBundle#getString(String)} for full
	 * description of behaviour.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the matching string.
	 */
	public static String get(String key, String value) {
		return MessageFormat.format(bundle.getString(key),
				new Object[] { value });
	}

	/**
	 * Obtains a string from the resource bundle
	 * "org/biomart/builder/resources/messages.properties". Substitutes all
	 * parameters in the resulting string for the specified values using
	 * MessageFormat. See {@link ResourceBundle#getString(String)} for full
	 * description of behaviour.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the matching string.
	 */
	public static String get(String key, String[] values) {
		return MessageFormat.format(bundle.getString(key), values);
	}

	/**
	 * Given a resource name (a file inside some package somewhere), return a
	 * stream that will read the contents of that file.
	 * 
	 * @param resource
	 *            the classpath of the resource to lookup, e.g.
	 *            "org/biomart/builder/resources/myfile.txt".
	 * @return a stream that will read that file.
	 */
	public static InputStream getResource(String resource) {
		ClassLoader cl = Resources.class.getClassLoader();
		if (cl == null)
			cl = ClassLoader.getSystemClassLoader();
		return cl.getResourceAsStream(resource);
	}
}
