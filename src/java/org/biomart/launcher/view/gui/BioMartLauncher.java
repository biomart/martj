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

package org.biomart.launcher.view.gui;

import java.awt.EventQueue;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.SettingsCache;
import org.biomart.common.view.gui.StackTrace;

/**
 * Launches various BioMart applications by asking the user via a drop-down menu
 * to select one. It forwards the various command line arguments to the selected
 * application.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class BioMartLauncher {
	private static final long serialVersionUID = 1L;

	private final String[] args;

	/**
	 * Run this application and open the main window. The window stays open and
	 * the application keeps running until the window is closed.
	 * 
	 * @param args
	 *            any command line arguments that the user specified will be in
	 *            this array.
	 */
	public BioMartLauncher(String[] args) {
		// Remember our arguments.
		this.args = args;

		// Load our cache of settings.
		SettingsCache.load();

		// Set the look and feel to the one specified by the user, or the system
		// default if not specified by the user. This may be null.
		String lookAndFeelClass = SettingsCache.getProperty("lookandfeel");
		try {
			UIManager.setLookAndFeel(lookAndFeelClass);
		} catch (final Exception e) {
			// Ignore, as we'll end up with the system one if this one doesn't
			// work.
			if (lookAndFeelClass != null) // only worry if we were actually
				// given one.
				System.err.println(Resources.get("badLookAndFeel",
						lookAndFeelClass));
			// Use system default.
			lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
			try {
				UIManager.setLookAndFeel(lookAndFeelClass);
			} catch (final Exception e2) {
				// Ignore, as we'll end up with the cross-platform one if there
				// is no system one.
				System.err.println(Resources.get("badLookAndFeel",
						lookAndFeelClass));
			}
		}
	}

	private void promptUser() {
		// Make a list of available apps.
		Map classes = new TreeMap();
		classes.put(Resources.get("MartBuilder"),
				"org.biomart.builder.view.gui.MartBuilder");
		classes.put(Resources.get("MartEditor"),
				"org.ensembl.mart.editor.MartEditor");
		classes.put(Resources.get("MartExplorer"),
				"org.ensembl.mart.explorer.MartExplorer");

		// Ask the user to choose one.
		String chosenClass = (String) JOptionPane.showInputDialog(null,
				Resources.get("promptMessage"), Resources.get("questionTitle"),
				JOptionPane.QUESTION_MESSAGE, null, classes.keySet().toArray(),
				null);

		// Load that class and run it.
		if (chosenClass != null)
			this.launchApp((String) classes.get(chosenClass));
		else
			System.exit(0);
	}

	private void launchApp(String className) {
		try {
			Class.forName(className).getMethod("main",
					new Class[] { String[].class }).invoke(null,
					new Object[] { this.args });
		} catch (Throwable t) {
			StackTrace.showStackTrace(t);
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// Initialise resources.
		Resources.setResourceLocation("org/biomart/launcher/resources");
		// Start the application.
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				// Create it.
				final BioMartLauncher bml = new BioMartLauncher(args);
				// Start it.
				bml.promptUser();
			}
		});
	}

}
