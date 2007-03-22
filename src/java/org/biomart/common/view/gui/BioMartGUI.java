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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.biomart.builder.view.gui.MartBuilder;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.dialogs.AboutDialog;

/**
 * This abstract class provides some useful common stuff for launching any
 * BioMart Java GUI appliaction.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.5
 */
public abstract class BioMartGUI extends JFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of MartBuilder. You can customise the
	 * look-and-feel by speciying a configuration property called
	 * <tt>lookandfeel</tt>, which contains the classname of the
	 * look-and-feel to use. Details of where this file is can be found in
	 * {@link Settings}.
	 */
	protected BioMartGUI() {
		// Create the window.
		super(Resources.get("GUITitle", Resources.BIOMART_VERSION));

		// Set some nice Mac stuff.
		System.setProperty("com.apple.macos.useScreenMenuBar", "true");
		// System.setProperty("com.apple.macos.use-file-dialog-packages","false");
		System.setProperty("com.apple.macos.smallTabs", "true");
		// System.setProperty("com.apple.mrj.application.apple.menu.about.name",
		// Resources.get("plainGUITitle"));
		System
				.setProperty("com.apple.mrj.application.growbox.intrudes",
						"true");
		System.setProperty("com.apple.mrj.application.live-resize", "true");

		// Load our cache of settings.
		Settings.load();

		// Set the look and feel to the one specified by the user, or the system
		// default if not specified by the user. This may be null.
		Log.info(Resources.get("loadingLookAndFeel"));
		String lookAndFeelClass = Settings.getProperty("lookandfeel");
		try {
			UIManager.setLookAndFeel(lookAndFeelClass);
		} catch (final Exception e) {
			// Ignore, as we'll end up with the system one if this one doesn't
			// work.
			if (lookAndFeelClass != null)
				// only worry if we were actually given one.
				Log.warn(Resources.get("badLookAndFeel", lookAndFeelClass), e);
			// Use system default.
			lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
			try {
				UIManager.setLookAndFeel(lookAndFeelClass);
			} catch (final Exception e2) {
				// Ignore, as we'll end up with the cross-platform one if there
				// is no system one.
				Log.warn(Resources.get("badLookAndFeel", lookAndFeelClass), e2);
			}
		}

		// Set up window listener and use it to handle windows closing.
		Log.info(Resources.get("createMainGUI"));
		final BioMartGUI mb = this;
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent e) {
				if (e.getWindow() == mb)
					Log.debug("Main window closing");
				BioMartGUI.this.requestExitApp();
			}
		});
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// Set up our GUI components.
		Log.debug("Initialising main window components");
		this.initComponents();

		// Pack the window.
		this.pack();

		// Set a sensible size.
		this.setSize(this.getToolkit().getScreenSize());
	}

	/**
	 * Starts the application.
	 */
	protected void launch() {
		// Start the application.
		Log.info(Resources.get("launchingGUI"));
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				// Centre it.
				BioMartGUI.this.setLocationRelativeTo(null);
				// Open it.
				BioMartGUI.this.setVisible(true);
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
	public void requestExitApp() {
		Log.info(Resources.get("logRequestNormalExit"));
		if (this.confirmExitApp()) {
			Log.info(Resources.get("logRequestNormalExitGranted"));
			System.exit(0);
		} else
			Log.info(Resources.get("logRequestNormalExitDenied"));
	}

	/**
	 * Requests the app to show an about dialog.
	 */
	public void requestShowAbout() {
		AboutDialog.displayAbout();
	}

	/**
	 * Override this method if you wish to ask the user for confirmation.
	 * 
	 * @return <tt>true</tt> if it is OK to exit, and <tt>false</tt> if the
	 *         user asks not to.
	 */
	public boolean confirmExitApp() {
		return true;
	}

	/**
	 * This is the main menubar.
	 */
	public static class BioMartMenuBar extends JMenuBar implements
			ActionListener {
		private static final long serialVersionUID = 1;

		private JMenuItem exit;

		private MartBuilder martBuilder;

		/**
		 * Constructor calls super then sets up our menu items.
		 * 
		 * @param martBuilder
		 *            the mart builder gui to which we are attached.
		 */
		public BioMartMenuBar(final MartBuilder martBuilder) {
			super();

			// Remember our parent.
			this.martBuilder = martBuilder;

			// Don't do this menu on Macs.
			if (System.getProperty("os.name").toLowerCase().indexOf("mac") < 0) {
				// Exit MartBuilder.
				this.exit = new JMenuItem(Resources.get("exitTitle"));
				this.exit.setMnemonic(Resources.get("exitMnemonic").charAt(0));
				this.exit.addActionListener(this);

				// Construct the mart menu.
				final JMenu fileMenu = new JMenu(Resources.get("fileMenuTitle"));
				fileMenu.setMnemonic(Resources.get("fileMenuMnemonic")
						.charAt(0));
				fileMenu.add(BioMartMenuBar.this.exit);

				// Adds the menus to the menu bar.
				this.add(fileMenu);
			}
		}

		/**
		 * Obtain the {@link MartBuilder} instance which this menubar
		 * is attached to.
		 * @return the instance this is attached to.
		 */
		protected MartBuilder getMartBuilder() {
			return this.martBuilder;
		}

		public void actionPerformed(final ActionEvent e) {
			// File menu.
			if (e.getSource() == this.exit)
				this.getMartBuilder().requestExitApp();
		}
	}
}
