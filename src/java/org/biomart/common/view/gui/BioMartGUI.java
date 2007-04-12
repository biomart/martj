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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

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
 *          $Author$
 * @since 0.5
 */
public abstract class BioMartGUI extends JFrame {
	private static final long serialVersionUID = 1L;

	private static boolean reallyIsMac = true;

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
		if (this.isMac()) {
			try {
				// Set up a listener proxy.
				final Class listenerClass = Class
						.forName("com.apple.eawt.ApplicationListener");
				final Class eventClass = Class
						.forName("com.apple.eawt.ApplicationEvent");
				final Method eventHandled = eventClass.getMethod("setHandled",
						new Class[] { Boolean.TYPE });
				final Method handleAbout = listenerClass.getMethod(
						"handleAbout", new Class[] { eventClass });
				final Method handleQuit = listenerClass.getMethod("handleQuit",
						new Class[] { eventClass });
				final Object proxy = Proxy.newProxyInstance(listenerClass
						.getClassLoader(), new Class[] { listenerClass },
						new InvocationHandler() {
							public Object invoke(Object proxy, Method method,
									Object[] args) throws Throwable {
								if (method.equals(handleAbout)) {
									// Handle TRUE otherwise it overrides us.
									eventHandled.invoke(args[0],
											new Object[] { Boolean.TRUE });
									BioMartGUI.this.requestShowAbout();
								} else if (method.equals(handleQuit)) {
									// Handle FALSE otherwise it preempts us.
									eventHandled.invoke(args[0],
											new Object[] { Boolean.FALSE });
									BioMartGUI.this.requestExitApp();
								} else
									eventHandled.invoke(args[0],
											new Object[] { Boolean.FALSE });
								// All methods return null.
								return null;
							}
						});

				// Set up application wrapper to include handler methods.
				final Object app = Class.forName("com.apple.eawt.Application")
						.newInstance();
				app.getClass().getMethod("addAboutMenuItem", null).invoke(app,
						null);
				app.getClass().getMethod("addPreferencesMenuItem", null)
						.invoke(app, null);
				app.getClass().getMethod("setEnabledAboutMenu",
						new Class[] { Boolean.TYPE }).invoke(app,
						new Object[] { Boolean.TRUE });
				app.getClass().getMethod("setEnabledPreferencesMenu",
						new Class[] { Boolean.TYPE }).invoke(app,
						new Object[] { Boolean.FALSE });
				app
						.getClass()
						.getMethod(
								"addApplicationListener",
								new Class[] { Class
										.forName("com.apple.eawt.ApplicationListener") })
						.invoke(app, new Object[] { proxy });
			} catch (final Throwable t) {
				Log.warn(t);
				BioMartGUI.reallyIsMac = false;
			}
			// Set up properties.
			System.setProperty("com.apple.macos.useScreenMenuBar", "true");
			System.setProperty("com.apple.macos.use-file-dialog-packages",
					"false");
			System.setProperty("com.apple.macos.smallTabs", "true");
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name",
					Resources.get("plainGUITitle"));
			System.setProperty("com.apple.mrj.application.growbox.intrudes",
					"false");
			System.setProperty("com.apple.mrj.application.live-resize", "true");
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.brushMetalRounded", "true");
			System.setProperty("apple.awt.showGrowBox", "true");
			System.setProperty("apple.awt.antialiasing", "on");
			System.setProperty("apple.awt.textantialiasing", "on");
			System.setProperty("apple.awt.rendering", "VALUE_RENDER_QUALITY");
			System.setProperty("apple.awt.interpolation",
					"VALUE_INTERPOLATION_BICUBIC");
			System.setProperty("apple.awt.fractionalmetrics", "on");
			System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
			System.setProperty("apple.awt.window.position.forceSafeCreation",
					"true");
			System
					.setProperty(
							"apple.awt.window.position.forceSafeProgrammaticPositioning",
							"true");
		}

		// Attach ourselves as the main window for hourglass use.
		LongProcess.setMainWindow(this);

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
	 * Are we on a Mac?
	 * 
	 * @return <tt>true</tt> if we are.
	 */
	protected boolean isMac() {
		return BioMartGUI.reallyIsMac
				&& System.getProperty("mrj.version") != null;
	}

	/**
	 * This is the main menubar.
	 */
	public static abstract class BioMartMenuBar extends JMenuBar implements
			ActionListener {
		private static final long serialVersionUID = 1;

		private JMenuItem exit;

		private JMenuItem about;

		private BioMartGUI gui;

		/**
		 * Constructor calls super then sets up our menu items.
		 * 
		 * @param gui
		 *            the gui to which we are attached.
		 */
		public BioMartMenuBar(final BioMartGUI gui) {
			super();

			// Remember our parent.
			this.gui = gui;

			// Don't do the File menu at all on Macs.
			if (!this.gui.isMac()) {
				// Construct the mart menu.
				final JMenu fileMenu = new JMenu(Resources.get("fileMenuTitle"));
				fileMenu.setMnemonic(Resources.get("fileMenuMnemonic")
						.charAt(0));

				// Exit MartBuilder.
				this.exit = new JMenuItem(Resources.get("exitTitle"));
				this.exit.setMnemonic(Resources.get("exitMnemonic").charAt(0));
				this.exit.addActionListener(this);
				fileMenu.add(BioMartMenuBar.this.exit);

				// Adds the menus to the menu bar.
				this.add(fileMenu);
			}

			this.buildMenus();

			// Construct the help menu.
			final JMenu helpMenu = new JMenu(Resources.get("helpMenuTitle"));
			helpMenu.setMnemonic(Resources.get("helpMenuMnemonic").charAt(0));

			// Don't do the About menu option on Macs.
			if (!this.gui.isMac()) {
				// About.
				this.about = new JMenuItem(Resources.get(
						"aboutTitle", Resources.get("plainGUITitle")));
				this.about.setMnemonic(Resources
						.get("aboutMnemonic").charAt(0));
				this.about.addActionListener(this);
				helpMenu.add(this.about);
			}

			// TODO Add a help page menu item.

			// Adds the menus to the menu bar (if it exists at all - TODO remove
			// this check once help page added).
			if (helpMenu.getItemCount() > 0)
				this.add(helpMenu);
		}

		/**
		 * Override this to provide additional menu items.
		 */
		protected abstract void buildMenus();

		/**
		 * Obtain the {@link BioMartGUI} instance which this menubar is attached
		 * to.
		 * 
		 * @return the instance this is attached to.
		 */
		protected BioMartGUI getBioMartGUI() {
			return this.gui;
		}

		public void actionPerformed(final ActionEvent e) {
			// File menu.
			if (e.getSource() == this.exit)
				this.getBioMartGUI().requestExitApp();
			// Help menu
			else if (e.getSource() == this.about)
				this.getBioMartGUI().requestShowAbout();
		}
	}
}
