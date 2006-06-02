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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.MartBuilderXML;
import org.biomart.builder.model.Mart;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays a set of tabs, one per mart currently loaded. Each tab keeps track
 * of the mart inside it, including all datasets and schemas.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 2nd June 2006
 * @since 0.1
 */
public class MartTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private MartBuilder martBuilder;

	private JFileChooser xmlFileChooser;

	// Mart hashcodes don't change, so it is safe to use a Map.
	private Map martModifiedStatus;

	// Mart hashcodes don't change, so it is safe to use a Map.
	private Map martXMLFile;

	/**
	 * Creates a new set of tabs and associates them with a given MartBuilder
	 * GUI.
	 * 
	 * @param martBuilder
	 *            the GUI these tabs belong to.
	 */
	public MartTabSet(MartBuilder martBuilder) {
		// Tabbed-pane stuff first.
		super();

		// Create the file chooser.
		this.xmlFileChooser = new JFileChooser();
		this.xmlFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".xml".
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().toLowerCase().endsWith(
						".xml"));
			}

			public String getDescription() {
				return BuilderBundle.getString("XMLFileFilterDescription");
			}
		});
		this.xmlFileChooser.setMultiSelectionEnabled(true);

		// Now set up and remember our variables.
		this.martBuilder = martBuilder;
		this.martModifiedStatus = new HashMap();
		this.martXMLFile = new HashMap();
	}

	/**
	 * Retrieves the parent MartBuilder GUI.
	 * 
	 * @return the parent mart builder GUI.
	 */
	public MartBuilder getMartBuilder() {
		return this.martBuilder;
	}

	/**
	 * Works out which dataset tabset is selected (the dataset tabset is the tab
	 * contents for each mart), and return it.
	 * 
	 * @return the currently selected dataset tabset, or null if none is
	 *         selected.
	 */
	public DataSetTabSet getSelectedDataSetTabSet() {
		if (this.getSelectedComponent() != null)
			return (DataSetTabSet) this.getSelectedComponent();
		else
			return null;
	}

	/**
	 * On a request to close all marts, check that none of them are modified. If
	 * any of them are, ask the user if they're sure they want to close them
	 * all.
	 * 
	 * @return <tt>true</tt> if its OK to close all the marts, <tt>false</tt>
	 *         if not.
	 */
	public boolean confirmCloseAllMarts() {
		for (Iterator i = this.martModifiedStatus.values().iterator(); i
				.hasNext();) {
			if (i.next().equals(Boolean.TRUE)) {
				int choice = JOptionPane.showConfirmDialog(this, BuilderBundle
						.getString("okToCloseAll"), BuilderBundle
						.getString("questionTitle"), JOptionPane.YES_NO_OPTION);
				return choice == JOptionPane.YES_OPTION;
			}
		}
		return true;
	}

	/**
	 * On a request to close the current mart, check that it is not modified. If
	 * it is, ask the user if they're sure they want to close it. If they say
	 * yes, or if it is not modified, close it.
	 */
	public void confirmCloseMart() {

		// If nothing is selected, forget it, they can't close!
		if (this.getSelectedDataSetTabSet() == null)
			return;

		// Work out the current selected mart.
		Mart currentMart = this.getSelectedDataSetTabSet().getMart();

		// Is it modified? If so, ask user for confirmation.
		boolean canClose = true;
		if (this.martModifiedStatus.get(currentMart).equals(Boolean.TRUE)) {
			// Modified, so must confirm action first.
			int choice = JOptionPane.showConfirmDialog(this, BuilderBundle
					.getString("okToClose"), BuilderBundle
					.getString("questionTitle"), JOptionPane.YES_NO_OPTION);
			canClose = (choice == JOptionPane.YES_OPTION);
		}

		// If it's OK to close, remove the tab and the mart itself.
		if (canClose) {
			// Remove the tab.
			this
					.removeTabAt(this.indexOfComponent(this
							.getSelectedComponent()));

			// Remove the mart from the modified map.
			this.martModifiedStatus.remove(currentMart);

			// Remove the XML file the mart came from from the file map.
			this.martXMLFile.remove(currentMart);
		}
	}

	/**
	 * Sets the current modified status. This applies to the currently selected
	 * mart.
	 * 
	 * @param status
	 *            <tt>true</tt> for modified, <tt>false</tt> for unmodified.
	 */
	public void setModifiedStatus(boolean status) {
		// If nothing selected, ignore it.
		if (this.getSelectedDataSetTabSet() == null)
			return;

		// Work out the current mart.
		Mart currentMart = this.getSelectedDataSetTabSet().getMart();

		// Update the status for it.
		this.martModifiedStatus.put(currentMart, Boolean.valueOf(status));

		// Update the tab title to indicate modification status.
		this.setTitleAt(this.getSelectedIndex(), this
				.suggestTabName(currentMart));
	}

	/**
	 * Gets whether any currently open mart is modified.
	 * 
	 * @return <tt>true</tt> if any of them are, <tt>false</tt> if not.
	 */
	public boolean getModifiedStatus() {
		return this.martModifiedStatus.values().contains(Boolean.TRUE);
	}

	/**
	 * Suggests a tab name based on a filename.
	 */
	private String suggestTabName(Mart mart) {

		// Start with "unsaved".
		String basename = BuilderBundle.getString("unsavedMart");

		// See if this mart came from a file. If so, use the filename.
		File filename = (File) this.martXMLFile.get(mart);
		if (filename != null)
			basename = filename.getName();

		// If it's modified, append a "*" to make it obvious.
		return basename
				+ (this.martModifiedStatus.get(mart).equals(Boolean.TRUE) ? " *"
						: "");
	}

	/**
	 * Adds a new tab to the tabset representing a new mart.
	 * 
	 * @param mart
	 *            the mart to put in the tab.
	 * @param martXMLFile
	 *            the file the mart came from. May be null if the mart is new.
	 * @param initialState
	 *            <tt>true</tt> if the mart should start out modified,
	 *            <tt>false</tt> if not.
	 */
	private void addMartTab(Mart mart, File martXMLFile, boolean initialState) {
		this.martXMLFile.put(mart, martXMLFile);
		this.martModifiedStatus.put(mart, Boolean.valueOf(initialState));
		this.addTab(this.suggestTabName(mart), new DataSetTabSet(this, mart));

		// Select the tab we just created.
		this.setSelectedIndex(this.getTabCount() - 1);
	}

	/**
	 * Creates a new, empty mart.
	 */
	public void requestNewMart() {
		this.addMartTab(new Mart(), null, true);
	}

	/**
	 * Saves the current mart to the current file.
	 */
	public void saveMart() {
		// If nothing selected, refise.
		if (this.getSelectedDataSetTabSet() == null)
			return;

		// Work out if we already have a file for this mart. If not,
		// do a save-as instead.
		final Mart currentMart = this.getSelectedDataSetTabSet().getMart();
		if (this.martXMLFile.get(currentMart) == null)
			this.saveMartAs();
		else {
			// Save it in the background to the existing file.
			LongProcess.run(this, new Runnable() {
				public void run() {
					try {
						// Save it.
						MartBuilderXML.save(currentMart, (File) martXMLFile
								.get(currentMart));

						// We're not modified any more!
						setModifiedStatus(false);
					} catch (Throwable t) {
						martBuilder.showStackTrace(t);
					}
				}
			});
		}
	}

	/**
	 * Saves the schema to a user-specified file, by popping up a file-chooser.
	 */
	public void saveMartAs() {
		// If nothing selected at present, refuse.
		if (this.getSelectedDataSetTabSet() == null)
			return;

		// Work out the current mart.
		Mart currentMart = this.getSelectedDataSetTabSet().getMart();

		// Show a file chooser. If the user didn't cancel it, process the
		// response.
		if (this.xmlFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

			// Find out the file the user chose.
			File saveAsFile = this.xmlFileChooser.getSelectedFile();

			// Skip the rest if they cancelled the save box.
			if (saveAsFile == null)
				return;

			// Save it, and save the reference to the XML file for later.
			this.martXMLFile.put(currentMart, saveAsFile);
			this.saveMart();
		}
	}

	/**
	 * Loads a schema from a user-specified file(s), by popping up a dialog
	 * allowing them to choose the file(s).
	 */
	public void loadMart() {
		// Open the file chooser.
		if (this.xmlFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			// Find out which files they selected.
			final File[] loadFiles = this.xmlFileChooser.getSelectedFiles();

			// If they selected any at all, load them in turn.
			if (loadFiles != null) {
				// In the background, load them in turn.
				LongProcess.run(this, new Runnable() {
					public void run() {
						try {
							for (int i = 0; i < loadFiles.length; i++)
								addMartTab(MartBuilderXML.load(loadFiles[i]),
										loadFiles[i], false);
						} catch (Throwable t) {
							martBuilder.showStackTrace(t);
						}
					}
				});
			}
		}
	}

	/**
	 * Construct a context menu for a given mart tab.
	 * 
	 * @return the popup menu.
	 */
	private JPopupMenu getMartTabContextMenu() {
		JPopupMenu contextMenu = new JPopupMenu();

		// The close option closes the selected mart, confirming first
		// that it's OK to do so.
		JMenuItem close = new JMenuItem(BuilderBundle
				.getString("closeMartTitle"));
		close.setMnemonic(BuilderBundle.getString("closeMartMnemonic")
				.charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				confirmCloseMart();
			}
		});
		contextMenu.add(close);

		// Return the menu.
		return contextMenu;
	}

	protected void processMouseEvent(MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.isPopupTrigger()) {
			// Where was the click?
			int selectedIndex = this.indexAtLocation(evt.getX(), evt.getY());
			// Did we actually click on any tab?
			if (selectedIndex >= 0) {
				// Select that tab.
				this.setSelectedIndex(selectedIndex);
				// Pop up the context menu for it.
				this.getMartTabContextMenu().show(this, evt.getX(), evt.getY());
				// We've processed the mouse event.
				eventProcessed = true;
			}
		}
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}
}