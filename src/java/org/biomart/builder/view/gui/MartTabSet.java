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
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.MartBuilderXML;
import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.MartConstructor.ConstructorRunnable;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.diagrams.contexts.SchemaContext;
import org.biomart.builder.view.gui.dialogs.SaveDDLDialog;

/**
 * Displays a set of tabs, one per mart currently loaded. Each tab keeps track
 * of the mart inside it, including all datasets and schemas.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class MartTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private MartBuilder martBuilder;

	// Mart hashcodes don't change, so it is safe to use a Map.
	private Map martModifiedStatus;

	// Mart hashcodes don't change, so it is safe to use a Map.
	private Map martXMLFile;

	private JFileChooser xmlFileChooser;

	/**
	 * Creates a new set of tabs and associates them with a given MartBuilder
	 * GUI.
	 * 
	 * @param martBuilder
	 *            the GUI these tabs belong to.
	 */
	public MartTabSet(final MartBuilder martBuilder) {
		// Tabbed-pane stuff first.
		super();

		// Create the file chooser for opening MartBuilder XML files.
		this.xmlFileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					String filename = file.getName();
					String extension = Resources.get("xmlExtension");
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		this.xmlFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".xml".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								Resources.get("xmlExtension"));
			}

			public String getDescription() {
				return Resources.get("XMLFileFilterDescription");
			}
		});
		this.xmlFileChooser.setMultiSelectionEnabled(true);

		// Now set up and remember our variables.
		this.martBuilder = martBuilder;
		this.martModifiedStatus = new HashMap();
		this.martXMLFile = new HashMap();
	}

	/**
	 * Adds a new tab to the tabset representing a new mart.
	 * 
	 * @param mart
	 *            the mart to put in the tab.
	 * @param martXMLFile
	 *            the file the mart came from. May be <tt>null</tt> if the
	 *            mart is new.
	 * @param initialState
	 *            <tt>true</tt> if the mart should start out modified,
	 *            <tt>false</tt> if not.
	 */
	private void addMartTab(final Mart mart, final File martXMLFile,
			final boolean initialState) {
		this.martXMLFile.put(mart, martXMLFile);
		this.martModifiedStatus.put(mart, Boolean.valueOf(initialState));
		final MartTab martTab = new MartTab(this, mart);
		this.addTab(this.suggestTabName(mart), martTab);

		// Select the tab we just created.
		this.setSelectedIndex(this.getTabCount() - 1);

		// Within that tab, select the all-schemas and all-datasets tabs.
		martTab.getDataSetTabSet().setSelectedIndex(0);
		martTab.getSchemaTabSet().setSelectedIndex(0);
	}

	/**
	 * Construct a context menu for a given mart tab. This is the context menu
	 * on the tab itself, not it's contents.
	 * 
	 * @return the popup menu.
	 */
	private JPopupMenu getMartTabContextMenu() {
		final JPopupMenu contextMenu = new JPopupMenu();

		// The close option closes the selected mart, confirming first
		// that it's OK to do so.
		final JMenuItem close = new JMenuItem(Resources.get("closeMartTitle"));
		close.setMnemonic(Resources.get("closeMartMnemonic").charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				MartTabSet.this.confirmCloseMart();
			}
		});
		contextMenu.add(close);

		// Return the menu.
		return contextMenu;
	}

	/**
	 * This method monitors a {@link ConstructorRunnable} and pops up a progress
	 * monitor for the user so they can see how it is doing. It notes if the
	 * process fails and throws up an exception message if necessary, or a
	 * success message if all goes well.
	 * 
	 * @param thread
	 *            the process to monitor.
	 * @param timer
	 *            the timer which triggers events to update the monitor.
	 * @param progressMonitor
	 *            the monitor displaying the current status of events.
	 * @param constructor
	 *            the {@link ConstructorRunnable} being run inside the thread
	 *            being monitored.
	 */
	private void monitorConstructionProgress(final Thread thread,
			final Timer timer, final ProgressMonitor progressMonitor,
			final ConstructorRunnable constructor) {
		// Did the job complete yet?
		if (thread.isAlive() && !progressMonitor.isCanceled()) {
			// If not, update the progress report.
			progressMonitor.setNote(constructor.getStatusMessage());
			progressMonitor.setProgress(constructor.getPercentComplete());
		} else {
			// If it completed, close the task and tidy up.
			// Stop the timer.
			timer.stop();
			// Stop the thread.
			constructor.cancel();
			// Close the progress dialog.
			progressMonitor.close();
			// If it failed, show the exception.
			final Exception failure = constructor.getFailureException();
			if (failure != null)
				this.getMartBuilder().showStackTrace(
						new ConstructorException(Resources
								.get("martConstructionFailed"), failure));
			// Inform user of success, if it succeeded.
			else
				JOptionPane.showMessageDialog(null, Resources
						.get("martConstructionComplete"), Resources
						.get("messageTitle"), JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Suggests a tab name based on a mart's filename. If the mart has no
	 * filename, "unsaved" is used.
	 */
	private String suggestTabName(final Mart mart) {

		// Start with "unsaved".
		String basename = Resources.get("unsavedMart");

		// See if this mart came from a file. If so, use the filename.
		final File filename = (File) this.martXMLFile.get(mart);
		if (filename != null)
			basename = filename.getName();

		// If it's modified, append a "*" to make it obvious.
		return basename
				+ (this.martModifiedStatus.get(mart).equals(Boolean.TRUE) ? " *"
						: "");
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.isPopupTrigger()) {
			// Where was the click?
			final int selectedIndex = this.indexAtLocation(evt.getX(), evt
					.getY());
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

	/**
	 * On a request to close all marts, check that none of them are modified. If
	 * any of them are, ask the user if they're sure they want to close them
	 * all.
	 * 
	 * @return <tt>true</tt> if its OK to close all the marts, <tt>false</tt>
	 *         if not.
	 */
	public boolean confirmCloseAllMarts() {
		for (final Iterator i = this.martModifiedStatus.values().iterator(); i
				.hasNext();)
			if (i.next().equals(Boolean.TRUE)) {
				final int choice = JOptionPane.showConfirmDialog(null,
						Resources.get("okToCloseAll"), Resources
								.get("questionTitle"),
						JOptionPane.YES_NO_OPTION);
				return choice == JOptionPane.YES_OPTION;
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
		if (this.getSelectedMartTab() == null)
			return;

		// Work out the current selected mart.
		final Mart currentMart = this.getSelectedMartTab().getMart();

		// Is it modified? If so, ask user for confirmation.
		boolean canClose = true;
		if (this.martModifiedStatus.get(currentMart).equals(Boolean.TRUE)) {
			// Modified, so must confirm action first.
			final int choice = JOptionPane.showConfirmDialog(null, Resources
					.get("okToClose"), Resources.get("questionTitle"),
					JOptionPane.YES_NO_OPTION);
			canClose = choice == JOptionPane.YES_OPTION;
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
	 * Retrieves the parent MartBuilder GUI.
	 * 
	 * @return the parent mart builder GUI.
	 */
	public MartBuilder getMartBuilder() {
		return this.martBuilder;
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
	 * Works out which mart tab is selected, and return it.
	 * 
	 * @return the currently selected mart tab, or <tt>null</tt> if none is
	 *         selected.
	 */
	public MartTab getSelectedMartTab() {
		return (MartTab) this.getSelectedComponent();
	}

	/**
	 * Loads a schema from a user-specified file(s), by popping up a dialog
	 * allowing them to choose the file(s). If they choose a file, it is loaded
	 * and parsed and a new tab is added representing its contents.
	 */
	public void loadMart() {
		// Open the file chooser.
		if (this.xmlFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			// Find out which files they selected.
			final File[] loadFiles = this.xmlFileChooser.getSelectedFiles();

			// If they selected any at all, load them in turn.
			if (loadFiles != null)
				// In the background, load them in turn.
				LongProcess.run(new Runnable() {
					public void run() {
						try {
							for (int i = 0; i < loadFiles.length; i++) {
								final File file = loadFiles[i];
								final Mart mart = MartBuilderXML.load(file);
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										MartTabSet.this.addMartTab(mart, file,
												false);
									}
								});
							}
						} catch (final Throwable t) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									MartTabSet.this.martBuilder
											.showStackTrace(t);
								}
							});
						}
					}
				});
		}
	}

	/**
	 * On a request to create DDL for the current mart, open the DDL creation
	 * window with all the datasets for this mart selected.
	 */
	public void requestCreateDDL() {

		// If nothing is selected, forget it, they can't close!
		if (this.getSelectedMartTab() == null)
			return;

		// Work out the current selected mart.
		final MartTab currentMartTab = this.getSelectedMartTab();

		// Open the DDL creation dialog and let it do it's stuff.
		(new SaveDDLDialog(currentMartTab, currentMartTab.getMart()
				.getDataSets())).show();
	}

	/**
	 * Runs the given {@link ConstructorRunnable} and monitors it's progress.
	 * See also
	 * {@link #monitorConstructionProgress(Thread, Timer, ProgressMonitor, ConstructorRunnable)}.
	 * 
	 * @param constructor
	 *            the constructor that will build a mart.
	 */
	public void requestMonitorConstructorRunnable(
			final ConstructorRunnable constructor) {
		// Create a progress monitor.
		final ProgressMonitor progressMonitor = new ProgressMonitor(this
				.getMartBuilder(), Resources.get("creatingMart"), "", 0, 100);
		progressMonitor.setProgress(0); // Start with 0% complete.
		progressMonitor.setMillisToPopup(0); // Open immediately.

		// Start the construction in a thread. It does not need to be
		// Swing-thread-safe because it will never access the GUI. All
		// GUI interaction is done through the Timer below.
		final Thread thread = new Thread(constructor);
		thread.start();

		// Create a timer thread that will update the progress dialog.
		// We use the Swing Timer to make it Swing-thread-safe. (1000 millis
		// equals 1 second.)
		final Timer timer = new Timer(300, null);
		timer.setInitialDelay(0); // Start immediately upon request.
		timer.setCoalesce(true); // Coalesce delayed events.
		timer.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						MartTabSet.this.monitorConstructionProgress(thread,
								timer, progressMonitor, constructor);
					}
				});
			}
		});

		// Start the timer.
		timer.start();
	}

	/**
	 * Creates a new, empty mart and adds a tab for it.
	 */
	public void requestNewMart() {
		this.addMartTab(new Mart(), null, true);
	}

	/**
	 * Saves the current mart to the file currently defined for it.
	 */
	public void saveMart() {
		// If nothing selected, refuse.
		if (this.getSelectedMartTab() == null)
			return;

		// Work out if we already have a file for this mart. If not,
		// do a save-as instead.
		final Mart currentMart = this.getSelectedMartTab().getMart();
		if (this.martXMLFile.get(currentMart) == null)
			this.saveMartAs();
		else
			// Save it in the background to the existing file.
			LongProcess.run(new Runnable() {
				public void run() {
					try {
						// Save it.
						MartBuilderXML.save(currentMart,
								(File) MartTabSet.this.martXMLFile
										.get(currentMart));

						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// We're not modified any more!
								MartTabSet.this.setModifiedStatus(false);
							}
						});
					} catch (final Throwable t) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								MartTabSet.this.martBuilder.showStackTrace(t);
							}
						});
					}
				}
			});
	}

	/**
	 * Saves the mart to a user-specified file, by popping up a file-chooser.
	 */
	public void saveMartAs() {
		// If nothing selected at present, refuse.
		if (this.getSelectedMartTab() == null)
			return;

		// Work out the current mart.
		final Mart currentMart = this.getSelectedMartTab().getMart();

		// Show a file chooser. If the user didn't cancel it, process the
		// response.
		if (this.xmlFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

			// Find out the file the user chose.
			final File saveAsFile = this.xmlFileChooser.getSelectedFile();

			// Skip the rest if they cancelled the save box.
			if (saveAsFile == null)
				return;

			// Save it, and save the reference to the XML file for later.
			this.martXMLFile.put(currentMart, saveAsFile);
			this.saveMart();
		}
	}

	/**
	 * Sets the current modified status. This applies to the currently selected
	 * mart.
	 * 
	 * @param status
	 *            <tt>true</tt> for modified, <tt>false</tt> for unmodified.
	 */
	public void setModifiedStatus(final boolean status) {
		// If nothing selected, ignore it.
		if (this.getSelectedMartTab() == null)
			return;

		// Work out the current mart.
		final Mart currentMart = this.getSelectedMartTab().getMart();

		// Update the status for it.
		this.martModifiedStatus.put(currentMart, Boolean.valueOf(status));

		// Update the tab title to indicate modification status.
		this.setTitleAt(this.getSelectedIndex(), this
				.suggestTabName(currentMart));
	}

	/**
	 * This represents a single mart XML file as a tab in the top-level tabbed
	 * pane set.
	 */
	public class MartTab extends JPanel {
		private static final long serialVersionUID = 1;

		private JRadioButton datasetButton;

		private DataSetTabSet datasetTabSet;

		private JPanel displayArea;

		private Mart mart;

		private MartTabSet martTabSet;

		private JRadioButton schemaButton;

		private SchemaTabSet schemaTabSet;

		/**
		 * Constructs a new tab in the tabbed pane that represents the given
		 * mart.
		 * 
		 * @param martTabSet
		 *            the tabbed pane set we are adding ourselves to.
		 * @param mart
		 *            the mart we represent.
		 */
		public MartTab(final MartTabSet martTabSet, final Mart mart) {
			// Set up our layout.
			super(new BorderLayout());

			// Remember which mart and tabset we are working with.
			this.martTabSet = martTabSet;
			this.mart = mart;

			// Create the schema tabset.
			this.schemaTabSet = new SchemaTabSet(this);

			// Create display part of the tab. The display area consists of
			// two cards - one for the schema editor, one for the dataset
			// editor. Buttons in another area switch between the cards.
			this.displayArea = new JPanel(new CardLayout());

			// Create panel which contains the buttons.
			final JPanel buttonsPanel = new JPanel();

			// Create the button that selects the window card. It reattaches
			// it every time in case it has been attached somewhere else
			// whilst we weren't looking.
			this.schemaButton = new JRadioButton(Resources
					.get("schemaEditorButtonName"));
			this.schemaButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					if (e.getSource() == MartTab.this.schemaButton) {
						final SchemaContext context = new SchemaContext(
								MartTab.this);
						MartTab.this.schemaTabSet.setDiagramContext(context);
						MartTab.this.displayArea.add(MartTab.this.schemaTabSet,
								"SCHEMA_EDITOR_CARD");
						final CardLayout cards = (CardLayout) MartTab.this.displayArea
								.getLayout();
						cards.show(MartTab.this.displayArea,
								"SCHEMA_EDITOR_CARD");
					}
				}
			});
			buttonsPanel.add(this.schemaButton);

			// Add the Biomart logo to the buttons panel.
			buttonsPanel
					.add(new JLabel(
							new ImageIcon(
									Resources
											.getResourceAsURL("org/biomart/builder/resources/biomart-logo.gif"))));

			// Create the dataset tabset.
			this.datasetTabSet = new DataSetTabSet(this);

			// Dataset card.
			this.displayArea.add(this.datasetTabSet, "DATASET_EDITOR_CARD");

			// Create the button that selects the dataset card.
			this.datasetButton = new JRadioButton(Resources
					.get("datasetEditorButtonName"));
			this.datasetButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					if (e.getSource() == MartTab.this.datasetButton) {
						final CardLayout cards = (CardLayout) MartTab.this.displayArea
								.getLayout();
						cards.show(MartTab.this.displayArea,
								"DATASET_EDITOR_CARD");
					}
				}
			});
			buttonsPanel.add(this.datasetButton);

			// Make buttons mutually exclusive.
			final ButtonGroup buttons = new ButtonGroup();
			buttons.add(this.schemaButton);
			buttons.add(this.datasetButton);

			// Add the buttons panel, and the display area containing the cards,
			// to the panel.
			this.add(buttonsPanel, BorderLayout.NORTH);
			this.add(this.displayArea, BorderLayout.CENTER);

			// Select the default button (which shows the schema card).
			// We must physically click on it to make the card show.
			this.schemaButton.doClick();
		}

		/**
		 * Obtain the tabbed pane set inside this one that represents the
		 * datasets in this mart.
		 * 
		 * @return the tabbed pane set showing the datasets in this mart.
		 */
		public DataSetTabSet getDataSetTabSet() {
			return this.datasetTabSet;
		}

		/**
		 * Find out what mart we represent.
		 * 
		 * @return the mart we represent.
		 */
		public Mart getMart() {
			return this.mart;
		}

		/**
		 * Find out what tabbed pane set we belong to.
		 * 
		 * @return the tabbed pane set we belong to.
		 */
		public MartTabSet getMartTabSet() {
			return this.martTabSet;
		}

		/**
		 * Obtain the tabbed pane set inside this one that represents the
		 * schemas in this mart.
		 * 
		 * @return the tabbed pane set showing the schemas in this mart.
		 */
		public SchemaTabSet getSchemaTabSet() {
			return this.schemaTabSet;
		}

		/**
		 * Fakes a click on the dataset editor radio button.
		 */
		public void selectDataSetEditor() {
			// May get called before button has been created.
			if (this.datasetButton != null && !this.datasetButton.isSelected())
				this.datasetButton.doClick();
		}

		/**
		 * Fakes a click on the schema editor radio button.
		 */
		public void selectSchemaEditor() {
			// May get called before button has been created.
			if (this.schemaButton != null && !this.schemaButton.isSelected())
				this.schemaButton.doClick();
		}
	}
}