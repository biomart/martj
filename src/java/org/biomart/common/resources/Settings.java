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
 * Settings are contained in a folder called <tt>.biomart</tt> in the
 * user's home directory, inside which there is a second folder for
 * each of the BioMart applications. In there are two files - one called
 * <tt>properties</tt> which contains general configuration settings such as
 * look and feel, and the other called <tt>cache</tt> which is a directory
 * containing history settings for various classes.
 * <p>
 * You should only ever need to modify the <tt>properties</tt> file, and
 * <tt>cache</tt> should be left alone.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.5
 */
public class Settings {

	/**
	 * App reference for MartBuilder.
	 */
	public static final String MARTBUILDER = "martbuilder";

	// Insert more app references as more apps are built.
	
	private static final File homeDir = new File(System
			.getProperty("user.home"), ".biomart");

	private static final Map classCache = new HashMap();

	private static File classCacheDir;

	private static int classCacheSize = 10;

	private static boolean initialising = true;

	private static final Properties properties = new Properties();

	private static final File propertiesFile = new File(Settings.homeDir,
			"properties");

	private static final Object SAVE_LOCK = new String("__SAVE__LOCK");

	// Create the bits we need on start-up.
	static {
		try {
			if (!Settings.homeDir.exists())
				Settings.homeDir.mkdir();
			if (!Settings.propertiesFile.exists())
				Settings.propertiesFile.createNewFile();
		} catch (final Throwable t) {
			Log.error(Resources.get("settingsCacheInitFailed"));
		}
	}

	/**
	 * Set the current application.
	 * 
	 * @param app
	 *            the current application.
	 */
	public static void setApplication(final String app) {
		// Make the home directory.
		final File appDir = new File(Settings.homeDir, app);
		if (!appDir.exists())
			appDir.mkdir();
		// Set up the logger.
		Log.configure(app, appDir);
		// Use it to log application startup.
		Log.info(Resources.get("appStarted", app));
		// Make the class cache directory.
		Settings.classCacheDir = new File(appDir, "cache");
		if (!Settings.classCacheDir.exists())
			Settings.classCacheDir.mkdir();
	}

	/**
	 * Saves the current cache of settings to disk as a set of files at
	 * <tt>~/.biomart/&lt;appname&gt;</tt>.
	 */
	private static void save() {
		// Don't save if we're still loading.
		if (Settings.initialising) {
			Log.debug("Still loading settings, so won't save settings yet");
			return;
		}

		synchronized (Settings.SAVE_LOCK) {

			Log.info(Resources.get("startingSaveSettings"));

			try {
				Log.debug("Saving settings to "
						+ Settings.propertiesFile.getPath());
				Settings.properties.store(new FileOutputStream(
						Settings.propertiesFile), Resources
						.get("settingsCacheHeader"));
				// Save the class-by-class properties.
				Log.debug("Saving class caches");
				for (final Iterator i = Settings.classCache.entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry classCacheEntry = (Map.Entry) i.next();
					final Class clazz = (Class) classCacheEntry.getKey();
					final File classDir = new File(Settings.classCacheDir,
							clazz.getName());
					Log.debug("Creating class cache directory for "
							+ clazz.getName());
					classDir.mkdir();
					// Remove existing files.
					Log.debug("Clearing existing class cache files");
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
						final File propsFile = new File(classDir, name);
						Log
								.debug("Saving properties to "
										+ propsFile.getPath());
						props.store(new FileOutputStream(propsFile), Resources
								.get("settingsCacheHeader"));
					}
				}
			} catch (final Throwable t) {
				Log.error(Resources.get("settingsCacheSaveFailed"), t);
			}

			Log.info(Resources.get("doneSaveSettings"));
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
		final Map map = (Map) Settings.classCache.get(clazz);
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
		final Map map = (Map) Settings.classCache.get(clazz);
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
		return Settings.properties.getProperty(property);
	}

	/**
	 * Loads the current cache of settings from disk, from the files in
	 * <tt>~/.biomart/&lt;appname&gt;</tt>.
	 */
	public static synchronized void load() {
		Log.info(Resources.get("startingLoadSettings"));
		Settings.initialising = true;

		// Clear the existing settings.
		Log.debug("Clearing existing settings");
		Settings.properties.clear();

		// Load the settings.
		try {
			Log.debug("Loading settings from "
					+ Settings.propertiesFile.getPath());
			Settings.properties.load(new FileInputStream(
					Settings.propertiesFile));
		} catch (final Throwable t) {
			Log.error(Resources.get("settingsCacheLoadFailed"), t);
		}

		// Set up the cache.
		final String newClassCacheSize = Settings.properties
				.getProperty("classCacheSize");
		try {
			Log.debug("Setting class cache size to " + newClassCacheSize);
			Settings.classCacheSize = Integer.parseInt(newClassCacheSize);
		} catch (final NumberFormatException e) {
			// Ignore and use the default.
			Settings.classCacheSize = 10;
			Log.debug("Using default class cache size of "
					+ Settings.classCacheSize);
			Settings
					.setProperty("classCacheSize", "" + Settings.classCacheSize);
		}

		// Loop over classCacheDir to find classes.
		Log.debug("Loading class caches");
		final String[] classes = Settings.classCacheDir.list();
		if (classes != null)
			for (int i = 0; i < classes.length; i++)
				try {
					final Class clazz = Class.forName(classes[i]);
					Log.debug("Loading class cache for " + clazz.getName());
					final File classDir = new File(Settings.classCacheDir,
							classes[i]);
					final String[] entries = classDir.list();
					for (int j = 0; j < entries.length; j++) {
						final Properties props = new Properties();
						final File propsFile = new File(classDir, entries[j]);
						Log.debug("Loading properties from "
								+ propsFile.getPath());
						props.load(new FileInputStream(propsFile));
						Settings
								.saveHistoryProperties(clazz, entries[j], props);
					}
				} catch (final ClassNotFoundException e) {
					// Ignore. We don't care as these settings are
					// now irrelevant if the class no longer exists.
				} catch (final Throwable t) {
					Log.error(Resources.get("settingsCacheLoadFailed"), t);
				}

		Settings.initialising = false;
		Log.info(Resources.get("doneLoadSettings"));
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
		Log.debug("Adding history entry for " + clazz.getName() + ":" + name);
		if (!Settings.classCache.containsKey(clazz)) {
			Log.debug("Creating new cache for class " + clazz.getName());
			final LinkedHashMap history = new LinkedHashMap(
					Settings.classCacheSize, 0.75f, true) {
				private static final long serialVersionUID = 1;

				protected boolean removeEldestEntry(Map.Entry eldest) {
					return this.size() > Settings.classCacheSize;
				}
			};
			Settings.classCache.put(clazz, history);
		}
		Log.debug("History properties are: " + properties);
		((Map) Settings.classCache.get(clazz)).put(name, properties);
		Settings.save();
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
		Log.debug("Setting property " + property + "=" + value);
		Settings.properties.setProperty(property, value);
		Settings.save();
	}

	// Private means that this class is a static singleton.
	private Settings() {
	}
}
