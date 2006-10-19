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

package org.biomart.common.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Manages the on-disk cache of user settings.
 * <p>
 * Settings are contained in a folder called <tt>.martbuilder</tt> in the
 * user's home directory. In there are two files - one called
 * <tt>properties</tt> which contains general configuration settings such as
 * look and feel, and the other called <tt>cache</tt> which is a directory
 * containing history settings for various classes.
 * <p>
 * You should only ever need to modify the <tt>properties</tt> file, and
 * <tt>cache</tt> should be left alone.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class SettingsCache {

	private static final File homeDir = new File(System
			.getProperty("user.home"), ".biomart");

	private static final Map classCache = new HashMap();

	private static final File classCacheDir = new File(SettingsCache.homeDir,
			"cache");

	private static int classCacheSize = 10;

	private static boolean initialising = true;

	private static final Properties properties = new Properties();

	private static final File propertiesFile = new File(SettingsCache.homeDir,
			"properties");

	private static final Object SAVE_LOCK = new String("__SAVE__LOCK");

	// Create the bits we need on start-up.
	static {
		try {
			if (!SettingsCache.homeDir.exists())
				SettingsCache.homeDir.mkdir();
			if (!SettingsCache.classCacheDir.exists())
				SettingsCache.classCacheDir.mkdir();
			if (!SettingsCache.propertiesFile.exists())
				SettingsCache.propertiesFile.createNewFile();
		} catch (final Throwable t) {
			System.err.println(Resources.get("settingsCacheInitFailed"));
			t.printStackTrace(System.err);
		}
	}

	/**
	 * Saves the current cache of settings to disk as a set of files at
	 * <tt>~/.martbuilder</tt>.
	 */
	private static void save() {
		// Don't save if we're still loading.
		if (SettingsCache.initialising)
			return;

		synchronized (SettingsCache.SAVE_LOCK) {

			try {
				SettingsCache.properties.store(new FileOutputStream(
						SettingsCache.propertiesFile), Resources
						.get("settingsCacheHeader"));
				// Save the class-by-class properties.
				for (final Iterator i = SettingsCache.classCache.entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry classCacheEntry = (Map.Entry) i.next();
					final Class clazz = (Class) classCacheEntry.getKey();
					final File classDir = new File(SettingsCache.classCacheDir,
							clazz.getName());
					classDir.mkdir();
					// Remove existing files.
					File[] files = classDir.listFiles();
					for (int j = 0; j < files.length; j++)
						files[j].delete();
					// Save current set. Must use Map.Entry else each
					// call for map keys and values will change the
					// structure of the LRU cache map, and hence cause
					// ConcurrentModificationExceptions.
					for (final Iterator j = ((Map) classCacheEntry.getValue())
							.entrySet().iterator(); j.hasNext();) {
						final Map.Entry entry = (Map.Entry) j.next();
						final String name = (String) entry.getKey();
						final Properties props = (Properties) entry.getValue();
						props.store(new FileOutputStream(new File(classDir,
								name)), Resources.get("settingsCacheHeader"));
					}
				}
			} catch (final Throwable t) {
				System.err.println(Resources.get("settingsCacheSaveFailed"));
				t.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Given a class, return the set of names of properties from the history map
	 * that correspond to that class.
	 * 
	 * @param clazz
	 *            the class to look up.
	 * @return the names of the properties sets in the history that match that
	 *         class. May be empty but never <tt>null</tt>.
	 */
	public static Collection getHistoryNamesForClass(final Class clazz) {
		final Map map = (Map) SettingsCache.classCache.get(clazz);
		// Use copy of map keys in order to prevent concurrent modifications.
		return map == null ? Collections.EMPTY_SET : new HashSet(map.keySet());
	}

	/**
	 * Given a class and a group name, return the set of properties from history
	 * that match.
	 * 
	 * @param clazz
	 *            the class to look up.
	 * @param name
	 *            the name of the property set in the history.
	 * @return the properties that match. <tt>null</tt> if there is no match.
	 */
	public static Properties getHistoryProperties(final Class clazz,
			final String name) {
		final Map map = (Map) SettingsCache.classCache.get(clazz);
		return map == null ? null : (Properties) map.get(name);
	}

	/**
	 * Given a property name, return that property based on the contents of the
	 * cache file <tt>properties</tt>.
	 * 
	 * @param property
	 *            the property name to look up.
	 * @return the value, or <tt>null</tt> if not found.
	 */
	public static String getProperty(final String property) {
		return SettingsCache.properties.getProperty(property);
	}

	/**
	 * Loads the current cache of settings from disk, from the files in
	 * <tt>~/.martbuilder</tt>.
	 */
	public static synchronized void load() {
		SettingsCache.initialising = true;
		
		// Clear the existing settings.
		SettingsCache.properties.clear();

		// Load the settings.
		try {
			SettingsCache.properties.load(new FileInputStream(
					SettingsCache.propertiesFile));
		} catch (final Throwable t) {
			System.err.println(Resources.get("settingsCacheLoadFailed"));
			t.printStackTrace(System.err);
		}

		// Set up the cache.
		final String newClassCacheSize = SettingsCache.properties
				.getProperty("classCacheSize");
		try {
			SettingsCache.classCacheSize = Integer.parseInt(newClassCacheSize);
		} catch (final NumberFormatException e) {
			// Ignore and use the default.
			SettingsCache.classCacheSize = 10;
			SettingsCache.setProperty("classCacheSize", ""
					+ SettingsCache.classCacheSize);
		}

		// Loop over classCacheDir to find classes.
		final String[] classes = SettingsCache.classCacheDir.list();
		if (classes != null)
			for (int i = 0; i < classes.length; i++)
				try {
					final Class clazz = Class.forName(classes[i]);
					final File classDir = new File(SettingsCache.classCacheDir,
							classes[i]);
					final String[] entries = classDir.list();
					for (int j = 0; j < entries.length; j++) {
						final Properties props = new Properties();
						props.load(new FileInputStream(new File(classDir,
								entries[j])));
						SettingsCache.saveHistoryProperties(clazz, entries[j],
								props);
					}
				} catch (final Throwable t) {
					System.err
							.println(Resources.get("settingsCacheLoadFailed"));
					t.printStackTrace(System.err);
				}

		SettingsCache.initialising = false;
	}

	/**
	 * Given a bunch of properties, save them in the history of the given class
	 * with the given name. If the history contains very old stuff, age it out.
	 * 
	 * @param clazz
	 *            the class of the history properties to store.
	 * @param name
	 *            the name to give the history entry.
	 * @param properties
	 *            the properties to store.
	 */
	public static void saveHistoryProperties(final Class clazz,
			final String name, final Properties properties) {
		if (!SettingsCache.classCache.containsKey(clazz)) {
			final LinkedHashMap history = new LinkedHashMap(
					SettingsCache.classCacheSize, 0.75f, true) {
				private static final long serialVersionUID = 1;

				protected boolean removeEldestEntry(Map.Entry eldest) {
					return this.size() > SettingsCache.classCacheSize;
				}
			};
			SettingsCache.classCache.put(clazz, history);
		}
		((Map) SettingsCache.classCache.get(clazz)).put(name, properties);
		SettingsCache.save();
	}

	/**
	 * Given a property name, sets that property.
	 * 
	 * @param property
	 *            the property name to set.
	 * @param value
	 *            the value to give it.
	 */
	public static void setProperty(final String property, final String value) {
		SettingsCache.properties.setProperty(property, value);
		SettingsCache.save();
	}

	// Private means that this class is a static singleton.
	private SettingsCache() {
	}
}
