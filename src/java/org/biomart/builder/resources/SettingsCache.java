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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.biomart.builder.resources.Resources;

/**
 * Manages the on-disk cache of user settings.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 2nd August 2006
 * @since 0.1
 */
public class SettingsCache {

	private static boolean initialising = true;

	private static File homeDir = new File(System.getProperty("user.home"),
			".martbuilder");

	private static int classCacheSize = 10;

	private static File propertiesFile = new File(homeDir, "properties");

	private static Properties properties = new Properties();

	private static File classCacheDir = new File(homeDir, "cache");

	private static Map classCache = new HashMap();

	// Private means that this class is a static singleton.
	private SettingsCache() {
	}

	// Create the bits we need on start-up.
	static {
		try {
			if (!homeDir.exists())
				homeDir.mkdir();
			if (!classCacheDir.exists())
				classCacheDir.mkdir();
			if (!propertiesFile.exists())
				propertiesFile.createNewFile();
		} catch (Throwable t) {
			System.err.println(Resources.get("settingsCacheInitFailed"));
			t.printStackTrace(System.err);
		}
	}

	/**
	 * Loads the current cache of settings from disk, from the files in
	 * <tt>~/.martbuilder</tt>.
	 */
	public static synchronized void load() {
		initialising = true;

		try {
			properties.load(new FileInputStream(propertiesFile));
		} catch (Throwable t) {
			System.err.println(Resources.get("settingsCacheLoadFailed"));
			t.printStackTrace(System.err);
		}

		String newClassCacheSize = properties.getProperty("classCacheSize");
		try {
			classCacheSize = Integer.parseInt(newClassCacheSize);
		} catch (NumberFormatException e) {
			// Ignore and use the default.
			classCacheSize = 10;
			setProperty("classCacheSize", "" + classCacheSize);
		}

		// Loop over classCacheDir to find classes.
		String[] classes = classCacheDir.list();
		if (classes != null)
			for (int i = 0; i < classes.length; i++) {
				try {
					Class clazz = Class.forName(classes[i]);
					File classDir = new File(classCacheDir, classes[i]);
					String[] entries = classDir.list();
					for (int j = 0; j < entries.length; j++) {
						Properties props = new Properties();
						props.load(new FileInputStream(new File(classDir,
								entries[j])));
						saveHistoryProperties(clazz, entries[j], props);
					}
				} catch (Throwable t) {
					System.err
							.println(Resources.get("settingsCacheLoadFailed"));
					t.printStackTrace(System.err);
				}
			}

		initialising = false;
	}

	private static Object SAVE_LOCK = new String("__SAVE__LOCK");

	/**
	 * Saves the current cache of settings to disk as a set of files at
	 * <tt>~/.martbuilder</tt>.
	 */
	private static void save() {
		// Don't save if we're still loading.
		if (initialising)
			return;

		synchronized (SAVE_LOCK) {

			try {
				properties.store(new FileOutputStream(propertiesFile),
						Resources.get("settingsCacheHeader"));
				// Save the class-by-class properties.
				for (Iterator i = classCache.entrySet().iterator(); i.hasNext();) {
					Map.Entry classCacheEntry = (Map.Entry) i.next();
					Class clazz = (Class) classCacheEntry.getKey();
					File classDir = new File(classCacheDir, clazz.getName());
					classDir.mkdir();
					// List existing ones.
					String[] files = classDir.list();
					List existingFiles = files == null ? new ArrayList()
							: new ArrayList(Arrays.asList(files));
					// Save current set. Must use Map.Entry else each
					// call for map keys and values will change the
					// structure of the LRU cache map, and hence cause
					// ConcurrentModificationExceptions.
					for (Iterator j = ((Map) classCacheEntry.getValue())
							.entrySet().iterator(); j.hasNext();) {
						Map.Entry entry = (Map.Entry) j.next();
						String name = (String) entry.getKey();
						existingFiles.remove(name);
						Properties props = (Properties) entry.getValue();
						props.store(new FileOutputStream(new File(classDir,
								name)), Resources.get("settingsCacheHeader"));
					}
					// Remove old files that were not updated.
					for (Iterator j = existingFiles.iterator(); j.hasNext();)
						(new File(classDir, (String) j.next())).delete();
				}
			} catch (Throwable t) {
				System.err.println(Resources.get("settingsCacheSaveFailed"));
				t.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Given a property name, return that property based on the contents of the
	 * cache file <tt>properties</tt>.
	 * 
	 * @param property
	 *            the property name to look up.
	 * @return the value, or <tt>null</tt> if not found.
	 */
	public static String getProperty(String property) {
		return properties.getProperty(property);
	}

	/**
	 * Given a property name, sets that property.
	 * 
	 * @param property
	 *            the property name to set.
	 * @param value
	 *            the value to give it.
	 */
	public static void setProperty(String property, String value) {
		properties.setProperty(property, value);
		save();
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
	public static Collection getHistoryNamesForClass(Class clazz) {
		Map map = (Map) classCache.get(clazz);
		return map == null ? Collections.EMPTY_SET : map.keySet();
	}

	/**
	 * Given a class and a group name, return the set of properties from history
	 * that match.
	 * 
	 * @param clazz
	 *            the class to look up.
	 * @param name
	 *            the name of the property set in the history.
	 * @return the properties that match. May be empty but never <tt>null</tt>.
	 */
	public static Properties getHistoryProperties(Class clazz, String name) {
		Map map = (Map) classCache.get(clazz);
		return map == null ? new Properties() : (Properties) map.get(name);
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
	public static void saveHistoryProperties(Class clazz, String name,
			Properties properties) {
		if (!classCache.containsKey(clazz)) {
			LinkedHashMap history = new LinkedHashMap(classCacheSize, 0.75f,
					true) {
				private static final long serialVersionUID = 1;

				protected boolean removeEldestEntry(Map.Entry eldest) {
					return size() > classCacheSize;
				}
			};
			classCache.put(clazz, history);
		}
		((Map) classCache.get(clazz)).put(name, properties);
		save();
	}
}
