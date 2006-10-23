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

package org.biomart.common.view.gui;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.SettingsCache;

/**
 * This abstract class provides some useful common stuff for launcing any
 * BioMart Java appliaction.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public abstract class BioMartApplication extends JFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of MartBuilder. You can customise the
	 * look-and-feel by speciying a configuration property called
	 * <tt>lookandfeel</tt>, which contains the classname of the
	 * look-and-feel to use. Details of where this file is can be found in
	 * {@link SettingsCache}.
	 */
	protected BioMartApplication() {
		// Create the window.
		super(Resources.get("GUITitle", Resources.BIOMART_VERSION));

		// Assign ourselves to the long-process hourglass container.
		// This means that whenever the hourglass is on, it appears
		// over our entire window, not just the component that
		// called for it.
		LongProcess.setContainer(this);

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

		// Set up window listener and use it to handle windows closing.
		final BioMartApplication mb = this;
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent e) {
				if (e.getWindow() == mb)
					BioMartApplication.this.requestExitApp();
			}
		});
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// Set up our GUI components.
		this.initComponents();

		// Pack the window.
		this.pack();

		// Set a sensible size.
		this.setSize(this.getMinimumSize());
	}

	/**
	 * Starts the application.
	 */
	protected void launch() {
		// Start the application.
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				// Centre it.
				BioMartApplication.this.setLocationRelativeTo(null);
				// Open it.
				BioMartApplication.this.setVisible(true);
			}
		});
	}

	/**
	 * Adds GUI components to the window.
	 */
	protected abstract void initComponents();

	/**
	 * Requests the application to exit, allowing it to ask for permission from
	 * the user first if necessary.
	 */
	public abstract void requestExitApp();
}
