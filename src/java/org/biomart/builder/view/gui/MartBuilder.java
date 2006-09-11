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

package org.biomart.builder.view.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.biomart.builder.controller.MartBuilderXML;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.resources.SettingsCache;

/**
 * The main window housing the MartBuilder GUI.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.17, 18th August 2006
 * @since 0.1
 */
public class MartBuilder extends JFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Run this application and open the main window. The window stays open and
	 * the application keeps running until the window is closed.
	 * 
	 * @param args
	 *            any command line arguments that the user specified will be in
	 *            this array.
	 */
	public static void main(final String[] args) {
		// Start the application.
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				// Create it.
				final MartBuilder mb = new MartBuilder();
				// Centre it.
				mb.setLocationRelativeTo(null);
				// Open it.
				mb.setVisible(true);
			}
		});
	}

	private MartTabSet martTabSet;

	/**
	 * Creates a new instance of MartBuilder. You can customise the
	 * look-and-feel by speciying a system property on startup called
	 * <tt>martbuilder.laf</tt>, which contains the classname of the
	 * look-and-feel to use.
	 */
	public MartBuilder() {
		// Create the window.
		super(Resources.get("GUITitle", MartBuilderXML.DTD_VERSION));

		// Assign ourselves to the long-process hourglass container.
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

		// Set up our GUI components.
		this.initComponents();

		// Set a sensible size.
		this.setSize(this.getMinimumSize());
	}

	private void initComponents() {
		// Set up window listener and use it to handle windows closing.
		final MartBuilder mb = this;
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent e) {
				if (e.getWindow() == mb)
					MartBuilder.this.requestExitApp();
			}
		});
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// Make a menu bar and add it.
		this.setJMenuBar(new MartBuilderMenuBar(this));

		// Set up the set of tabs to hold the various marts.
		this.martTabSet = new MartTabSet(this);
		this.getContentPane().add(this.martTabSet, BorderLayout.CENTER);

		// Pack the window.
		this.pack();
	}

	public Dimension getMinimumSize() {
		// An arbitrary minimum size.
		return new Dimension(400, 400);
	}

	/**
	 * Exits the application, but only with permission from the mart tabset.
	 */
	public void requestExitApp() {
		// Only do it if the mart tabs say it's OK. They'll probably
		// prompt the user on our behalf.
		if (this.martTabSet.confirmCloseAllMarts())
			System.exit(0);
	}

	/**
	 * Display a nice friendly stack trace window.
	 * 
	 * @param t
	 *            the throwable to display the stack trace for.
	 */
	public void showStackTrace(final Throwable t) {
		// Create the main message.
		final int messageClass = t instanceof Error ? JOptionPane.ERROR_MESSAGE
				: JOptionPane.WARNING_MESSAGE;
		final String mainMessage = t.getLocalizedMessage();

		// Extract the full stack trace.
		final StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		final String stackTraceText = sw.toString();

		// Ask if they want to see the full stack trace (show the first line of
		// the stack trace as a hint).
		final int choice = JOptionPane.showConfirmDialog(this, new Object[] {
				mainMessage, Resources.get("stackTracePrompt") }, Resources
				.get("stackTraceTitle"), JOptionPane.YES_NO_OPTION);

		// Create and show the full stack trace dialog if they said yes.
		if (choice == JOptionPane.YES_OPTION)
			JOptionPane.showMessageDialog(this, stackTraceText, Resources
					.get("stackTraceTitle"), messageClass);
	}

	// This is the main menu bar.
	private class MartBuilderMenuBar extends JMenuBar implements ActionListener {
		private static final long serialVersionUID = 1;

		private JMenuItem closeMart;

		private JMenuItem exit;

		private MartBuilder martBuilder;

		private JMenuItem newMart;

		private JMenuItem openMart;

		private JMenuItem saveDDL;

		private JMenuItem saveMart;

		private JMenuItem saveMartAs;

		/**
		 * Constructor calls super then sets up our menu items.
		 * 
		 * @param martBuilder
		 *            the mart builder gui to which we are attached.
		 */
		public MartBuilderMenuBar(final MartBuilder martBuilder) {
			super();

			// Remember our parent.
			this.martBuilder = martBuilder;

			// File menu.
			final JMenu fileMenu = new JMenu(Resources.get("fileMenuTitle"));
			fileMenu.setMnemonic(Resources.get("fileMenuMnemonic").charAt(0));

			// New mart.
			this.newMart = new JMenuItem(
					Resources.get("newMartTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/new.gif")));
			this.newMart
					.setMnemonic(Resources.get("newMartMnemonic").charAt(0));
			this.newMart.addActionListener(this);

			// Open existing mart.
			this.openMart = new JMenuItem(
					Resources.get("openMartTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/open.gif")));
			this.openMart.setMnemonic(Resources.get("openMartMnemonic").charAt(
					0));
			this.openMart.addActionListener(this);

			// Save current mart.
			this.saveMart = new JMenuItem(
					Resources.get("saveMartTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/save.gif")));
			this.saveMart.setMnemonic(Resources.get("saveMartMnemonic").charAt(
					0));
			this.saveMart.addActionListener(this);

			// Save current mart as.
			this.saveMartAs = new JMenuItem(
					Resources.get("saveMartAsTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/save.gif")));
			this.saveMartAs.setMnemonic(Resources.get("saveMartAsMnemonic")
					.charAt(0));
			this.saveMartAs.addActionListener(this);

			// Create DDL for current mart.
			this.saveDDL = new JMenuItem(
					Resources.get("saveDDLTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/saveText.gif")));
			this.saveDDL
					.setMnemonic(Resources.get("saveDDLMnemonic").charAt(0));
			this.saveDDL.addActionListener(this);

			// Close current mart.
			this.closeMart = new JMenuItem(Resources.get("closeMartTitle"));
			this.closeMart.setMnemonic(Resources.get("closeMartMnemonic")
					.charAt(0));
			this.closeMart.addActionListener(this);

			// Exit MartBuilder.
			this.exit = new JMenuItem(Resources.get("exitTitle"));
			this.exit.setMnemonic(Resources.get("exitMnemonic").charAt(0));
			this.exit.addActionListener(this);

			// Construct the file menu.
			fileMenu.add(this.newMart);
			fileMenu.add(this.openMart);
			fileMenu.addSeparator();
			fileMenu.add(this.saveMart);
			fileMenu.add(this.saveMartAs);
			fileMenu.add(this.saveDDL);
			fileMenu.addSeparator();
			fileMenu.add(this.closeMart);
			fileMenu.addSeparator();
			fileMenu.add(this.exit);

			// Add a listener which checks which options to enable each time the
			// menu is opened. This mean that if no mart is currently selected,
			// save and close will be disabled, and if the current mart is not
			// modified, save will be disabled.
			fileMenu.addMenuListener(new MenuListener() {
				public void menuCanceled(final MenuEvent e) {
				} // Interface requirement.

				public void menuDeselected(final MenuEvent e) {
				} // Interface requirement.

				public void menuSelected(final MenuEvent e) {
					boolean hasMart = true;
					if (martBuilder.martTabSet.getSelectedMartTab() == null)
						hasMart = false;
					MartBuilderMenuBar.this.saveMart.setEnabled(hasMart
							&& martBuilder.martTabSet.getModifiedStatus());
					MartBuilderMenuBar.this.saveMartAs.setEnabled(hasMart);
					MartBuilderMenuBar.this.saveDDL.setEnabled(hasMart);
					MartBuilderMenuBar.this.closeMart.setEnabled(hasMart);
				}
			});

			// Adds the file menu to the menu bar.
			this.add(fileMenu);
		}

		public void actionPerformed(final ActionEvent e) {
			// File menu.
			if (e.getSource() == this.newMart)
				this.martBuilder.martTabSet.requestNewMart();
			else if (e.getSource() == this.openMart)
				this.martBuilder.martTabSet.loadMart();
			else if (e.getSource() == this.saveMart)
				this.martBuilder.martTabSet.saveMart();
			else if (e.getSource() == this.saveMartAs)
				this.martBuilder.martTabSet.saveMartAs();
			else if (e.getSource() == this.saveDDL)
				this.martBuilder.martTabSet.requestCreateDDL();
			else if (e.getSource() == this.closeMart)
				this.martBuilder.martTabSet.confirmCloseMart();
			else if (e.getSource() == this.exit)
				this.martBuilder.requestExitApp();
		}
	}
}
