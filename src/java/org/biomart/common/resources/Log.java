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
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;

/**
 * Logs messages. This static class is configured by the
 * {@link Settings#setApplication(String)} method.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public class Log {

	private static final Level DEFAULT_LOGGER_LEVEL = Level.INFO;

	private static Logger logger;
	static {
		// Set up a simple configuration that logs on the console.
		BasicConfigurator.configure();
		// Create the default logger.
		Log.logger = Logger.getRootLogger();
		Log.logger.setLevel(Log.DEFAULT_LOGGER_LEVEL);
	}

	/**
	 * Configures the logger for the given application, which has an application
	 * settings directory in the given location. The logger will log at
	 * {@link Level#INFO} or above by default, using a
	 * {@link RollingFileAppender}. You can override these settings and any
	 * others by placing a file named <tt>log4j.properties</tt> in the
	 * application settings directory. Settings in that file will be used in
	 * preference to the defaults. The logger name will be the same as the
	 * application name in lower-case, eg. <tt>martbuilder</tt>.
	 * <p>
	 * Until this method is called, the default root logger is used, and is
	 * configured using {@link BasicConfigurator#configure()}. The default
	 * logging level is {@link Level#INFO}.
	 * 
	 * @param app
	 *            the name of the application we are logging for.
	 * @param appDir
	 *            the application settings directory for that application.
	 */
	public static void configure(final String app, final File appDir) {
		// Make the log directory.
		final File logDir = new File(appDir, "log");
		if (!logDir.exists())
			logDir.mkdir();
		// Set up the default logger.
		try {
			Log.logger = Logger.getLogger(app);
			Log.logger.setLevel(Log.DEFAULT_LOGGER_LEVEL);
			Log.logger.addAppender(new RollingFileAppender(new PatternLayout(
					"%d{ISO8601} %-5p [%t:%F:%L]: %m%n"), (new File(logDir,
					"error.log")).getPath(), true));
		} catch (Throwable t) {
			// Fall-back to the console if we can't write to file.
			Log.logger.addAppender(new ConsoleAppender());
			Log.warn(Resources.get("noRollingLogger"), t);
		}
		// Attempt to load any user-defined settings.
		try {
			final File log4jPropsFile = new File(appDir, "log4j.properties");
			if (log4jPropsFile.exists()) {
				final Properties log4jProps = new Properties();
				log4jProps.load(new FileInputStream(log4jPropsFile));
				PropertyConfigurator.configure(log4jProps);
			}
		} catch (Throwable t) {
			Log.warn(Resources.get("noCustomLogger"), t);
		}
	}

	/**
	 * See {@link Logger#debug(Object)}.
	 * 
	 * @param message
	 *            the message to log.
	 */
	public static void debug(Object message) {
		Log.logger.debug(message);
	}

	/**
	 * See {@link Logger#debug(Object,Throwable)}.
	 * 
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the error stack trace.
	 */
	public static void debug(Object message, Throwable t) {
		Log.logger.debug(message, t);
	}

	/**
	 * See {@link Logger#info(Object)}.
	 * 
	 * @param message
	 *            the message to log.
	 */
	public static void info(Object message) {
		Log.logger.info(message);
	}

	/**
	 * See {@link Logger#info(Object,Throwable)}.
	 * 
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the error stack trace.
	 */
	public static void info(Object message, Throwable t) {
		Log.logger.debug(message, t);
	}

	/**
	 * See {@link Logger#warn(Object)}.
	 * 
	 * @param message
	 *            the message to log.
	 */
	public static void warn(Object message) {
		Log.logger.warn(message);
	}

	/**
	 * See {@link Logger#warn(Object,Throwable)}.
	 * 
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the error stack trace.
	 */
	public static void warn(Object message, Throwable t) {
		Log.logger.warn(message, t);
	}

	/**
	 * See {@link Logger#error(Object)}.
	 * 
	 * @param message
	 *            the message to log.
	 */
	public static void error(Object message) {
		Log.logger.error(message);
	}

	/**
	 * See {@link Logger#error(Object,Throwable)}.
	 * 
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the error stack trace.
	 */
	public static void error(Object message, Throwable t) {
		Log.logger.error(message, t);
	}

	/**
	 * See {@link Logger#fatal(Object)}.
	 * 
	 * @param message
	 *            the message to log.
	 */
	public static void fatal(Object message) {
		Log.logger.fatal(message);
	}

	/**
	 * See {@link Logger#fatal(Object,Throwable)}.
	 * 
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the error stack trace.
	 */
	public static void fatal(Object message, Throwable t) {
		Log.logger.fatal(message, t);
	}

	// Private means that this class is a static singleton.
	private Log() {
	}
}
