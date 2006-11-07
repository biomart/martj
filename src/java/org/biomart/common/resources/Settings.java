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
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

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
public class Settings {

	/**
	 * App reference for MartBuilder.
	 */
	public static final String MARTBUILDER = "martbuilder";

	private static final File homeDir = new File(System
			.getProperty("user.home"), ".biomart");

	/**
	 * Reference this logger to log to the application-wide logger. It will
	 * write logs to a folder called "log" under a folder specific to the
	 * current application, inside ".biomart" in the user's home directory.
	 * Rolling logs are used. The default logging level is {@link Level#INFO}.
	 * You can override this by specifying the "log4j.level" property in the
	 * environment for this application. This setting is passed to
	 * {@link Level#toLevel(String)} for processing.
	 * <p>
	 * The logger is initialised by the {@link #setApplication(String)} method.
	 * Until that point, the root logger is used along with its default
	 * configuration.
	 */
	public static Logger logger = Logger.getRootLogger();
	
	private static final String DEFAULT_LOGGER_LEVEL = "INFO";

	private static File logDir;	

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
			System.err.println(Resources.get("settingsCacheInitFailed"));
			t.printStackTrace(System.err);
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
		// Make the log directory.
		Settings.logDir = new File(appDir, "log");
		if (!Settings.logDir.exists())
			Settings.logDir.mkdir();
		// Set up the logger.
		Settings.logger = Logger.getLogger(app);
		try {
			Settings.logger.addAppender(new RollingFileAppender(
					new PatternLayout("%d{ISO8601} %-5p [%t:%F:%L]: %m%n"),
					(new File(logDir, "error.log")).getPath(), true));
		} catch (IOException e) {
			// Fall-back to the console if we can't write to file.
			Settings.logger.addAppender(new ConsoleAppender());
			Settings.logger.warn(Resources.get("noRollingLogger"), e);
		}
		// Set the logger level.
		Settings.logger.setLevel(Level.toLevel(System.getProperty(
				"log4j.level", Settings.DEFAULT_LOGGER_LEVEL)));
		// Use it to log application startup.
		Settings.logger.info(Resources.get("appStarted", app));
		// Make the class cache directory.
		Settings.classCacheDir = new File(appDir, "cache");
		if (!Settings.classCacheDir.exists())
			Settings.classCacheDir.mkdir();
		Settings.logger.debug("Created class cache directory");
	}

	/**
	 * Saves the current cache of settings to disk as a set of files at
	 * <tt>~/.martbuilder</tt>.
	 */
	private static void save() {
		// Don't save if we're still loading.
		if (Settings.initialising) {
			Settings.logger
					.debug("Still loading settings, so won't save settings yet");
			return;
		}

		synchronized (Settings.SAVE_LOCK) {

			Settings.logger.info(Resources.get("startingSaveSettings"));

			try {
				Settings.logger.debug("Saving settings to "
						+ Settings.propertiesFile.getPath());
				Settings.properties.store(new FileOutputStream(
						Settings.propertiesFile), Resources
						.get("settingsCacheHeader"));
				// Save the class-by-class properties.
				Settings.logger.debug("Saving class caches");
				for (final Iterator i = Settings.classCache.entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry classCacheEntry = (Map.Entry) i.next();
					final Class clazz = (Class) classCacheEntry.getKey();
					final File classDir = new File(Settings.classCacheDir,
							clazz.getName());
					Settings.logger.debug("Creating class cache directory for "
							+ clazz.getName());
					classDir.mkdir();
					// Remove existing files.
					Settings.logger
							.debug("Clearing existing class cache files");
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
						Settings.logger.debug("Saving properties to "
								+ propsFile.getPath());
						props.store(new FileOutputStream(propsFile), Resources
								.get("settingsCacheHeader"));
					}
				}
			} catch (final Throwable t) {
				Settings.logger.error(Resources.get("settingsCacheSaveFailed"),
						t);
			}

			Settings.logger.info(Resources.get("doneSaveSettings"));
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
	 * <tt>~/.martbuilder</tt>.
	 */
	public static synchronized void load() {
		Settings.logger.info(Resources.get("startingLoadSettings"));
		Settings.initialising = true;

		// Clear the existing settings.
		Settings.logger.debug("Clearing existing settings");
		Settings.properties.clear();

		// Load the settings.
		try {
			Settings.logger.debug("Loading settings from "
					+ Settings.propertiesFile.getPath());
			Settings.properties.load(new FileInputStream(
					Settings.propertiesFile));
		} catch (final Throwable t) {
			Settings.logger.error(Resources.get("settingsCacheLoadFailed"), t);
		}

		// Set up the cache.
		final String newClassCacheSize = Settings.properties
				.getProperty("classCacheSize");
		try {
			Settings.logger.debug("Setting class cache size to "
					+ newClassCacheSize);
			Settings.classCacheSize = Integer.parseInt(newClassCacheSize);
		} catch (final NumberFormatException e) {
			// Ignore and use the default.
			Settings.classCacheSize = 10;
			Settings.logger.debug("Using default class cache size of "
					+ classCacheSize);
			Settings
					.setProperty("classCacheSize", "" + Settings.classCacheSize);
		}

		// Loop over classCacheDir to find classes.
		Settings.logger.debug("Loading class caches");
		final String[] classes = Settings.classCacheDir.list();
		if (classes != null)
			for (int i = 0; i < classes.length; i++)
				try {
					final Class clazz = Class.forName(classes[i]);
					Settings.logger.debug("Loading class cache for "
							+ clazz.getName());
					final File classDir = new File(Settings.classCacheDir,
							classes[i]);
					final String[] entries = classDir.list();
					for (int j = 0; j < entries.length; j++) {
						final Properties props = new Properties();
						final File propsFile = new File(classDir, entries[j]);
						Settings.logger.debug("Loading properties from "
								+ propsFile.getPath());
						props.load(new FileInputStream(propsFile));
						Settings
								.saveHistoryProperties(clazz, entries[j], props);
					}
				} catch (final Throwable t) {
					Settings.logger.error(Resources
							.get("settingsCacheLoadFailed"), t);
				}

		Settings.initialising = false;
		Settings.logger.info(Resources.get("doneLoadSettings"));
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
		Settings.logger.debug("Adding history entry for " + clazz.getName()
				+ ":" + name);
		if (!Settings.classCache.containsKey(clazz)) {
			Settings.logger.debug("Creating new cache for class "
					+ clazz.getName());
			final LinkedHashMap history = new LinkedHashMap(
					Settings.classCacheSize, 0.75f, true) {
				private static final long serialVersionUID = 1;

				protected boolean removeEldestEntry(Map.Entry eldest) {
					return this.size() > Settings.classCacheSize;
				}
			};
			Settings.classCache.put(clazz, history);
		}
		Settings.logger.debug("History properties are: " + properties);
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
		Settings.logger.debug("Setting property " + property + "=" + value);
		Settings.properties.setProperty(property, value);
		Settings.save();
	}

	// Private means that this class is a static singleton.
	private Settings() {
	}
}
