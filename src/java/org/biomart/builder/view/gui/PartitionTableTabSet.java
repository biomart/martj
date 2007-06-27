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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.AllPartitionTablesDiagram;
import org.biomart.builder.view.gui.diagrams.contexts.AllPartitionTablesContext;
import org.biomart.builder.view.gui.dialogs.AddPartitionTableDialog;
import org.biomart.builder.view.gui.panels.PartitionTablePanel;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * This tabset contains definitions of partition tables.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class PartitionTableTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private AllPartitionTablesDiagram allPartitionTablesDiagram;

	// Use double-list to prevent problems with hashcodes changing.
	private final List[] partitionTableToPanel = new List[] { new ArrayList(),
			new ArrayList() };

	private MartTab martTab;

	/**
	 * The constructor sets up a new set of tabs which represent all the
	 * partitions in the given mart, plus an overview tab to represent all the
	 * partitions in the mart.
	 * 
	 * @param martTab
	 *            the mart tab to represent the partitions for.
	 */
	public PartitionTableTabSet(final MartTab martTab) {
		super();

		// Remember the settings.
		this.martTab = martTab;

		// Add the datasets overview tab. This tab displays a diagram
		// in which all partitions appear. This diagram could be quite large,
		// so it is held inside a scrollpane.
		this.allPartitionTablesDiagram = new AllPartitionTablesDiagram(
				this.martTab);
		this.allPartitionTablesDiagram
				.setDiagramContext(new AllPartitionTablesContext(martTab));
		final JScrollPane scroller = new JScrollPane(
				this.allPartitionTablesDiagram);
		scroller.getViewport().setBackground(
				this.allPartitionTablesDiagram.getBackground());
		this.addTab(Resources.get("multiPartitionTableOverviewTab"), scroller);

		// Calculate the tabs.
		this.recalculatePartitionTableTabs();
	}

	/**
	 * Works out which tab is selected, and return it.
	 * 
	 * @return the currently selected tab, or <tt>null</tt> if none is
	 *         selected.
	 */
	public PartitionTable getSelectedPartitionTable() {
		if (this.getSelectedIndex() <= 0 || !this.isShowing())
			return null;
		final PartitionTablePanel selectedPanel = (PartitionTablePanel) ((JScrollPane) this
				.getSelectedComponent()).getViewport().getView();
		return selectedPanel.getPartitionTable();
	}

	private synchronized void addPartitionTableTab(
			final PartitionTable partition, final boolean selectPartitionTable) {
		// Create the diagram to represent this table.
		final PartitionTablePanel partitionTablePanel = new PartitionTablePanel(
				this.martTab, partition);

		// Create a scroller to contain the diagram.
		final JScrollPane scroller = new JScrollPane(partitionTablePanel);
		scroller.getViewport().setBackground(
				partitionTablePanel.getBackground());

		// Add a tab containing the scroller, with the same name as the
		// partition.
		this.addTab(partition.getName(), scroller);

		// Remember which diagram the partition is connected with.
		this.partitionTableToPanel[0].add(partition);
		this.partitionTableToPanel[1].add(partitionTablePanel);

		if (selectPartitionTable) {
			// Fake a click on the dataset tab.
			this.setSelectedIndex(this.indexOfTab(partition.getName()));
			this.martTab.selectPartitionTableEditor();
		}

		// Update the overview diagram.
		this.recalculateOverviewDiagram();
	}

	private String askUserForName(final String message,
			final String defaultResponse) {
		// Ask the user for a name. Use the default response
		// as the default value in the input field.
		String name = (String) JOptionPane.showInputDialog(null, message,
				Resources.get("questionTitle"), JOptionPane.QUESTION_MESSAGE,
				null, null, defaultResponse);

		// If they cancelled the request, return null.
		if (name == null)
			return null;

		// If they didn't enter anything, use the default response
		// as though they hadn't changed it.
		else if (name.trim().length() == 0)
			name = defaultResponse;

		// Return the response.
		return name;
	}

	private JPopupMenu getPartitionTableTabContextMenu(
			final PartitionTable partition) {
		// Start with an empty menu.
		final JPopupMenu contextMenu = new JPopupMenu();

		// This item allows the user to rename.
		final JMenuItem rename = new JMenuItem(Resources
				.get("renamePartitionTableTitle"));
		rename.setMnemonic(Resources.get("renamePartitionTableMnemonic")
				.charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTableTabSet.this
						.requestRenamePartitionTable(partition);
			}
		});
		contextMenu.add(rename);

		// Add an option to replicate this.
		final JMenuItem replicate = new JMenuItem(Resources
				.get("replicatePartitionTableTitle"));
		replicate.setMnemonic(Resources.get("replicatePartitionTableMnemonic")
				.charAt(0));
		replicate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTableTabSet.this
						.requestReplicatePartitionTable(partition);
			}
		});
		contextMenu.add(replicate);

		// This item allows the user to remove this from the mart.
		final JMenuItem close = new JMenuItem(Resources
				.get("removePartitionTableTitle"), new ImageIcon(Resources
				.getResourceAsURL("cut.gif")));
		close.setMnemonic(Resources.get("removePartitionTableMnemonic").charAt(
				0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTableTabSet.this
						.requestRemovePartitionTable(partition);
			}
		});
		contextMenu.add(close);

		// Return the menu.
		return contextMenu;
	}

	private synchronized void removePartitionTableTab(
			final PartitionTable partition, final boolean select) {
		// Work out the currently selected tab.
		final int currentTab = this.getSelectedIndex();

		// Work out which tab the dataset lives in.
		final int index = this.partitionTableToPanel[0].indexOf(partition);

		// Work out the tab index.
		final int tabIndex = this.indexOfTab(partition.getName());

		// Remove the tab, and it's mapping from the tab map.
		this.removeTabAt(tabIndex);
		this.partitionTableToPanel[0].remove(index);
		this.partitionTableToPanel[1].remove(index);

		// Update the overview diagram.
		this.recalculateOverviewDiagram();

		if (select) {
			// Fake a click on the last tab before this one to ensure
			// at least one tab remains visible and up-to-date.
			this.setSelectedIndex(currentTab == 0 ? 0 : Math.max(tabIndex - 1,
					0));
		}
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.isPopupTrigger()) {
			// Where was the click?
			final int selectedIndex = this.indexAtLocation(evt.getX(), evt
					.getY());
			if (selectedIndex >= 0) {

				// Work out which tab was selected and which diagram
				// is displayed in that tab.
				final Component selectedComponent = this
						.getComponentAt(selectedIndex);
				if (selectedComponent instanceof JScrollPane) {
					final Component selectedPanel = ((JScrollPane) selectedComponent)
							.getViewport().getView();
					if (selectedPanel instanceof PartitionTablePanel) {

						// Set the dataset diagram as the currently selected
						// one.
						this.setSelectedIndex(selectedIndex);

						// Work out the dataset inside the diagram.
						final PartitionTable partition = ((PartitionTablePanel) selectedPanel)
								.getPartitionTable();

						// Show the context-menu for the tab for this schema.
						this.getPartitionTableTabContextMenu(partition).show(
								this, evt.getX(), evt.getY());

						// We've handled the event so mark it as processed.
						eventProcessed = true;
					}
				}
			}
		}
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	/**
	 * Returns the mart tab that this dataset tabset is displaying the contents
	 * of.
	 * 
	 * @return the mart tab that this dataset tabset is viewing.
	 */
	public MartTab getMartTab() {
		return this.martTab;
	}

	/**
	 * Recalculates the list of tabs, so that they tally with the list of
	 * partition tables currently within the mart. This removes any from the
	 * tabs which are no longer in the mart, and adds new tabs for those which
	 * are in the mart but not yet displayed.
	 */
	public synchronized void recalculatePartitionTableTabs() {
		// Add all partition tables in the mart that we don't have yet.
		// We work with a copy of the list of partition tables else we get
		// concurrent modification exceptions as new ones are added.
		for (final Iterator i = this.martTab.getMart().getPartitionTables()
				.iterator(); i.hasNext();) {
			final PartitionTable partition = (PartitionTable) i.next();
			if (!this.partitionTableToPanel[0].contains(partition))
				this.addPartitionTableTab(partition, false);
		}

		// Remove all that we have that are no longer in the mart.
		// We work with a copy of the list of these else we get
		// concurrent modification exceptions as old ones are removed.
		final List ourPartitionTables = new ArrayList(
				this.partitionTableToPanel[0]);
		for (final Iterator i = ourPartitionTables.iterator(); i.hasNext();) {
			final PartitionTable partition = (PartitionTable) i.next();
			if (!this.martTab.getMart().getPartitionTables()
					.contains(partition))
				this.removePartitionTableTab(partition, false);
		}
	}

	private synchronized void recalculateOverviewDiagram() {
		this.allPartitionTablesDiagram.recalculateDiagram();
	}

	private PartitionTable showAddTableDialog() {
		try {
			final AddPartitionTableDialog dialog = new AddPartitionTableDialog(
					this.getMartTab());
			if (dialog.definePartitionTable())
				return dialog.create();
		} catch (final PartitionException pe) {
			StackTrace.showStackTrace(pe);
		}
		return null;
	}

	/**
	 * Asks user to define a new partition table, then adds it.
	 */
	public void requestAddPartitionTable() {
		// Pop up a dialog to get the details of the new table, then
		// obtain a copy of that table.
		final PartitionTable partition = this.showAddTableDialog();

		// If no schema was defined, ignore the request.
		if (partition == null)
			return;

		// In the background, add the partition to ourselves.
		new LongProcess() {
			public void run() throws Exception {
				try {
					// Add the schema to the mart, then synchronise it.
					MartBuilderUtils.addPartitionTableToMart(
							PartitionTableTabSet.this.martTab.getMart(),
							partition);
				} finally {
					// Must use finally in case doesn't succesfully get created.
					// Create and add the tab representing this partition.
					PartitionTableTabSet.this.addPartitionTableTab(partition,
							true);

					// Update the modified status for this tabset.
					PartitionTableTabSet.this.martTab.getMartTabSet()
							.requestChangeModifiedStatus(true);
				}
			}
		}.start();
	}

	/**
	 * Asks the user if they are sure they want to remove all partitions, then
	 * removes them from the mart (and the tabs) if they agree.
	 */
	public void requestRemoveAllPartitionTables() {
		// Confirm the decision first.
		final int choice = JOptionPane.showConfirmDialog(null, Resources
				.get("confirmDelAllPartitionTables"), Resources
				.get("questionTitle"), JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// Do it, but in the background.
		new LongProcess() {
			public void run() throws Exception {
				// Remove the partitions from the mart.
				final List partitions = new ArrayList(
						PartitionTableTabSet.this.martTab.getMart()
								.getPartitionTables());
				for (final Iterator i = partitions.iterator(); i.hasNext();)
					MartBuilderUtils.removePartitionTableFromMart(
							PartitionTableTabSet.this.martTab.getMart(),
							(PartitionTable) i.next());
				// Remove the tab.
				for (final Iterator i = partitions.iterator(); i.hasNext();)
					PartitionTableTabSet.this.removePartitionTableTab(
							(PartitionTable) i.next(), false);

				// Update the modified status for this tabset.
				PartitionTableTabSet.this.martTab.getMartTabSet()
						.requestChangeModifiedStatus(true);
			}
		}.start();
	}

	/**
	 * Asks the user if they are sure they want to remove the table, then
	 * removes it from the mart (and the tabs) if they agree.
	 * 
	 * @param partition
	 *            the table to remove.
	 */
	public void requestRemovePartitionTable(final PartitionTable partition) {
		// Confirm the decision first.
		final int choice = JOptionPane.showConfirmDialog(null, Resources
				.get("confirmDelPartitionTable"), Resources
				.get("questionTitle"), JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// Do it, but in the background.
		new LongProcess() {
			public void run() throws Exception {
				// Remove the dataset from the mart.
				MartBuilderUtils.removePartitionTableFromMart(
						PartitionTableTabSet.this.martTab.getMart(), partition);
				// Remove the tab.
				PartitionTableTabSet.this.removePartitionTableTab(partition,
						true);

				// Update the modified status for this tabset.
				PartitionTableTabSet.this.martTab.getMartTabSet()
						.requestChangeModifiedStatus(true);
			}
		}.start();
	}

	/**
	 * Renames a partition, then renames the tab too.
	 * 
	 * @param partition
	 *            the partition to rename.
	 */
	public void requestRenamePartitionTable(final PartitionTable partition) {
		// Ask user for the new name.
		this.requestRenamePartitionTable(partition, this
				.askUserForName(Resources.get("requestPartitionTableName"),
						partition.getName()));
	}

	/**
	 * Renames a partition, then renames the tab too.
	 * 
	 * @param partition
	 *            the partition to rename.
	 * @param name
	 *            the new name to give it.
	 */
	public void requestRenamePartitionTable(final PartitionTable partition,
			final String name) {
		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		final String newName = name == null ? "" : name.trim();
		if (newName.length() == 0 || newName.equals(partition.getName()))
			return;

		// Work out which tab the dataset is in.
		final int idx = this.indexOfTab(partition.getName());

		new LongProcess() {
			public void run() throws Exception {
				// Rename the dataset.
				MartBuilderUtils.renamePartitionTable(
						PartitionTableTabSet.this.martTab.getMart(), partition,
						newName);

				// Rename the tab displaying it.
				PartitionTableTabSet.this.setTitleAt(idx, partition.getName());

				// And the overview.
				PartitionTableTabSet.this.recalculateOverviewDiagram();

				// Update the modified status for this tabset.
				PartitionTableTabSet.this.martTab.getMartTabSet()
						.requestChangeModifiedStatus(true);
			}
		}.start();
	}

	/**
	 * Asks user for a name to use, then creates an exact copy of the given
	 * partition, giving the copy the name they chose. See also
	 * {@link PartitionTable#replicate(String)}.
	 * 
	 * @param partition
	 *            the schema to dataset.
	 */
	public void requestReplicatePartitionTable(final PartitionTable partition) {
		// Ask user for the name to use for the copy.
		final String newName = this.askUserForName(Resources
				.get("requestPartitionTableName"), partition.getName());

		// No name entered? Or same name entered? Ignore the request.
		if (newName == null || newName.trim().length() == 0
				|| newName.equals(partition.getName()))
			return;

		new LongProcess() {
			public void run() throws Exception {
				// Create the replicate.
				final PartitionTable newPartitionTable = MartBuilderUtils
						.replicatePartitionTable(
								PartitionTableTabSet.this.martTab.getMart(),
								partition, newName);

				// Add a tab to represent the replicate.
				PartitionTableTabSet.this.addPartitionTableTab(
						newPartitionTable, true);

				// Update the modified status for this tabset.
				PartitionTableTabSet.this.martTab.getMartTabSet()
						.requestChangeModifiedStatus(true);
			}
		}.start();
	}
}
