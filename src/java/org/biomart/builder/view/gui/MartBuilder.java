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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.BioMartGUI;

/**
 * The main window housing the MartBuilder GUI. The {@link #main(String[])}
 * method starts the GUI and opens this window.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class MartBuilder extends BioMartGUI {
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
		// Initialise resources.
		Settings.setApplication(Settings.MARTBUILDER);
		Resources.setResourceLocation("org/biomart/builder/resources");
		// Start the application.
		new MartBuilder().launch();
	}

	private MartTabSet martTabSet;

	protected void initComponents() {
		// Make a menu bar and add it.
		this.setJMenuBar(new MartBuilderMenuBar(this));

		// Set up the set of tabs to hold the various marts.
		this.martTabSet = new MartTabSet(this);
		this.getContentPane().add(this.martTabSet, BorderLayout.CENTER);
		
		// Go straight to the 'New' page.
		this.martTabSet.requestNewMart();
	}

	/**
	 * Exits the application, but only with permission from the mart tabset.
	 */
	public boolean confirmExitApp() {
		return this.martTabSet.confirmCloseAllMarts();
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
			this.newMart = new JMenuItem(Resources.get("newMartTitle"),
					new ImageIcon(Resources.getResourceAsURL("new.gif")));
			this.newMart
					.setMnemonic(Resources.get("newMartMnemonic").charAt(0));
			this.newMart.addActionListener(this);

			// Open existing mart.
			this.openMart = new JMenuItem(Resources.get("openMartTitle"),
					new ImageIcon(Resources.getResourceAsURL("open.gif")));
			this.openMart.setMnemonic(Resources.get("openMartMnemonic").charAt(
					0));
			this.openMart.addActionListener(this);

			// Save current mart.
			this.saveMart = new JMenuItem(Resources.get("saveMartTitle"),
					new ImageIcon(Resources.getResourceAsURL("save.gif")));
			this.saveMart.setMnemonic(Resources.get("saveMartMnemonic").charAt(
					0));
			this.saveMart.addActionListener(this);

			// Save current mart as.
			this.saveMartAs = new JMenuItem(Resources.get("saveMartAsTitle"),
					new ImageIcon(Resources.getResourceAsURL("save.gif")));
			this.saveMartAs.setMnemonic(Resources.get("saveMartAsMnemonic")
					.charAt(0));
			this.saveMartAs.addActionListener(this);

			// Create DDL for current mart.
			this.saveDDL = new JMenuItem(Resources.get("saveDDLTitle"),
					new ImageIcon(Resources.getResourceAsURL("saveText.gif")));
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
					MartBuilderMenuBar.this.saveDDL.setEnabled(hasMart
							&& martBuilder.martTabSet.getSelectedMartTab()
									.getMart().getDataSets().size() > 0);
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
