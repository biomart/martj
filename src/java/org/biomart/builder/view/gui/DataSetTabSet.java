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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.ExpressionColumnDefinition;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.Relation.CompoundRelationDefinition;
import org.biomart.builder.model.Relation.RestrictedRelationDefinition;
import org.biomart.builder.model.Table.RestrictedTableDefinition;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.AllDataSetsDiagram;
import org.biomart.builder.view.gui.diagrams.DataSetDiagram;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.ExplainTransformationDiagram.RealisedRelation;
import org.biomart.builder.view.gui.diagrams.contexts.AllDataSetsContext;
import org.biomart.builder.view.gui.diagrams.contexts.DataSetContext;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.builder.view.gui.dialogs.CompoundRelationDialog;
import org.biomart.builder.view.gui.dialogs.ExplainDataSetDialog;
import org.biomart.builder.view.gui.dialogs.ExplainTableDialog;
import org.biomart.builder.view.gui.dialogs.ExpressionColumnDialog;
import org.biomart.builder.view.gui.dialogs.LoopbackRelationDialog;
import org.biomart.builder.view.gui.dialogs.PartitionTableDialog;
import org.biomart.builder.view.gui.dialogs.RestrictedRelationDialog;
import org.biomart.builder.view.gui.dialogs.RestrictedTableDialog;
import org.biomart.builder.view.gui.dialogs.SaveDDLDialog;
import org.biomart.builder.view.gui.dialogs.SuggestDataSetDialog;
import org.biomart.builder.view.gui.dialogs.SuggestInvisibleDataSetDialog;
import org.biomart.builder.view.gui.dialogs.UnrolledRelationDialog;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.Transaction;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * This tabset contains most of the core functionality of the entire GUI. It has
 * one tab per dataset defined, plus an overview tab which displays an overview
 * of all the datasets in the mart. It handles all changes to any of the
 * datasets in the mart, and handles the assignment of {@link DiagramContext}s
 * to the various {@link Diagram}s inside it, including the schema tabset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class DataSetTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private AllDataSetsDiagram allDataSetsDiagram;

	private final Map datasetToDiagram = new HashMap();

	private MartTab martTab;

	/**
	 * The constructor sets up a new set of tabs which represent all the
	 * datasets in the given mart, plus an overview tab to represent all the
	 * datasets in the mart.
	 * 
	 * @param martTab
	 *            the mart tab to represent the datasets for.
	 */
	public DataSetTabSet(final MartTab martTab) {
		super();

		// Remember the settings.
		this.martTab = martTab;

		// Add the datasets overview tab. This tab displays a diagram
		// in which all datasets appear. This diagram could be quite large,
		// so it is held inside a scrollpane.
		this.allDataSetsDiagram = new AllDataSetsDiagram(this.martTab);
		this.allDataSetsDiagram.setDiagramContext(new AllDataSetsContext(
				martTab));
		final JScrollPane scroller = new JScrollPane(this.allDataSetsDiagram);
		scroller.getViewport().setBackground(
				this.allDataSetsDiagram.getBackground());
		scroller.getHorizontalScrollBar().addAdjustmentListener(
				this.allDataSetsDiagram);
		scroller.getVerticalScrollBar().addAdjustmentListener(
				this.allDataSetsDiagram);
		this.addTab(Resources.get("multiDataSetOverviewTab"), scroller);

		// Make a listener which knows how to handle masking and
		// renaming.
		final PropertyChangeListener renameListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				final DataSet ds = (DataSet) evt.getSource();
				if (evt.getPropertyName().equals("name"))
					// Rename in diagram set.
					DataSetTabSet.this.datasetToDiagram.put(evt.getNewValue(),
							DataSetTabSet.this.datasetToDiagram.remove(evt
									.getOldValue()));
				else if (evt.getPropertyName().equals("masked")) {
					// For masks, if unmasking, add a tab, otherwise
					// remove the tab.
					final boolean masked = ((Boolean) evt.getNewValue())
							.booleanValue();
					if (masked)
						DataSetTabSet.this.removeDataSetTab(ds.getName(), true);
					else
						DataSetTabSet.this.addDataSetTab(ds, false);
				}
			}
		};

		// Populate the map to hold the relation between schemas and the
		// diagrams representing them.
		for (final Iterator i = martTab.getMart().getDataSets().values()
				.iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			// Don't add schemas which are initially masked.
			if (!ds.isMasked())
				this.addDataSetTab(ds, false);
			ds.addPropertyChangeListener("masked", renameListener);
			ds.addPropertyChangeListener("name", renameListener);
		}

		// Listen to add/remove/mass change schema events.
		martTab.getMart().getDataSets().addPropertyChangeListener(
				new PropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent evt) {
						// Listen to masked schema and rename
						// schema events on each new schema added
						// regardless of tab presence.
						// Mass change. Copy to prevent concurrent mods.
						final Set oldDSs = new HashSet(
								DataSetTabSet.this.datasetToDiagram.keySet());
						for (final Iterator i = martTab.getMart().getDataSets()
								.values().iterator(); i.hasNext();) {
							final DataSet ds = (DataSet) i.next();
							if (!oldDSs.remove(ds.getName())) {
								// Single-add.
								if (!ds.isMasked())
									DataSetTabSet.this.addDataSetTab(ds, true);
								ds.addPropertyChangeListener("masked",
										renameListener);
								ds.addPropertyChangeListener("name",
										renameListener);
							}
						}
						for (final Iterator i = oldDSs.iterator(); i.hasNext();)
							DataSetTabSet.this.removeDataSetTab((String) i
									.next(), true);
					}
				});
	}

	/**
	 * Works out which dataset tab is selected, and return it.
	 * 
	 * @return the currently selected dataset tab, or <tt>null</tt> if none is
	 *         selected.
	 */
	public DataSet getSelectedDataSet() {
		if (this.getSelectedIndex() <= 0 || !this.isShowing())
			return null;
		final DataSetDiagram selectedDiagram = (DataSetDiagram) ((JScrollPane) this
				.getSelectedComponent()).getViewport().getView();
		return selectedDiagram.getDataSet();
	}

	private synchronized void addDataSetTab(final DataSet dataset,
			final boolean selectDataset) {
		// Create the diagram to represent this dataset.
		final DataSetDiagram datasetDiagram = new DataSetDiagram(this.martTab,
				dataset);

		// Create a scroller to contain the diagram.
		final JScrollPane scroller = new JScrollPane(datasetDiagram);
		scroller.getViewport().setBackground(datasetDiagram.getBackground());
		scroller.getHorizontalScrollBar().addAdjustmentListener(datasetDiagram);
		scroller.getVerticalScrollBar().addAdjustmentListener(datasetDiagram);

		// Add a tab containing the scroller, with the same name as the dataset.
		this.addTab(dataset.getName(), scroller);

		// Remember which diagram the dataset is connected with.
		this.datasetToDiagram.put(dataset.getName(), datasetDiagram);

		// Set the current context on the diagram to be the same as the
		// current context on this dataset tabset.
		datasetDiagram.setDiagramContext(new DataSetContext(this.martTab,
				dataset));

		if (selectDataset) {
			// Fake a click on the dataset tab.
			this.setSelectedIndex(this.indexOfTab(dataset.getName()));
			this.martTab.selectDataSetEditor();
		}
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

	private JPopupMenu getDataSetTabContextMenu(final DataSet dataset) {
		// Start with an empty menu.
		final JPopupMenu contextMenu = new JPopupMenu();

		// This item allows the user to rename the dataset.
		final JMenuItem rename = new JMenuItem(Resources
				.get("renameDataSetTitle"));
		rename.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetTabSet.this.requestRenameDataSet(dataset);
			}
		});
		contextMenu.add(rename);

		// This item allows the user to remove the dataset from the mart.
		final JMenuItem close = new JMenuItem(Resources
				.get("removeDataSetTitle"), new ImageIcon(Resources
				.getResourceAsURL("cut.gif")));
		close.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetTabSet.this.requestRemoveDataSet(dataset);
			}
		});
		contextMenu.add(close);

		// Return the menu.
		return contextMenu;
	}

	private synchronized void removeDataSetTab(final String datasetName,
			final boolean select) {
		// Work out the currently selected tab.
		final int currentTab = this.getSelectedIndex();

		// Work out the tab index.
		final int tabIndex = this.indexOfTab(datasetName);

		// Remove the tab, and it's mapping from the dataset-to-tab map.
		this.removeTabAt(tabIndex);
		this.datasetToDiagram.remove(datasetName);

		if (select)
			// Fake a click on the last tab before this one to ensure
			// at least one tab remains visible and up-to-date.
			this.setSelectedIndex(currentTab == 0 ? 0 : Math.max(tabIndex - 1,
					0));
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
					final Component selectedDiagram = ((JScrollPane) selectedComponent)
							.getViewport().getView();
					if (selectedDiagram instanceof DataSetDiagram) {

						// Set the dataset diagram as the currently selected
						// one.
						this.setSelectedIndex(selectedIndex);

						// Work out the dataset inside the diagram.
						final DataSet dataset = ((DataSetDiagram) selectedDiagram)
								.getDataSet();

						// Show the context-menu for the tab for this schema.
						this.getDataSetTabContextMenu(dataset).show(this,
								evt.getX(), evt.getY());

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
	 * Request that the optimiser type used post-construction of a dataset be
	 * changed.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param type
	 *            the type of optimiser to use post-construction.
	 */
	public void requestChangeOptimiserType(final DataSet dataset,
			final DataSetOptimiserType type) {
		Transaction.start();
		dataset.setDataSetOptimiserType(type);
		Transaction.end();
	}

	/**
	 * Request that all changes on this dataset table associated with this
	 * target are accepted. See {@link DataSetTable#acceptChanges(Table)}.
	 * 
	 * @param dsTable
	 *            the dataset table to work with.
	 * @param targetTable
	 *            the (optional) target table to accept changes from.
	 */
	public void requestAcceptAll(final DataSetTable dsTable,
			final Table targetTable) {
		Transaction.start();
		dsTable.acceptChanges(targetTable);
		Transaction.end();
	}

	/**
	 * Request that all changes on this dataset table associated with this
	 * target are rejected. See {@link DataSetTable#rejectChanges(Table)}.
	 * 
	 * @param dsTable
	 *            the dataset table to work with.
	 * @param targetTable
	 *            the (optional) target table to reject changes from.
	 */
	public void requestRejectAll(final DataSetTable dsTable,
			final Table targetTable) {
		Transaction.start();
		dsTable.rejectChanges(targetTable);
		Transaction.end();
	}

	/**
	 * On a request to create DDL for the current dataset, open the DDL creation
	 * window with all this dataset selected.
	 * 
	 * @param dataset
	 *            the dataset to show the dialog for.
	 */
	public void requestCreateDDL(final DataSet dataset) {
		// If it is a partition table dataset, refuse.
		if (dataset.isPartitionTable())
			JOptionPane.showMessageDialog(this, Resources
					.get("noDDLForPartitionTable"), Resources
					.get("messageTitle"), JOptionPane.INFORMATION_MESSAGE);
		// Open the DDL creation dialog and let it do it's stuff.
		else
			(new SaveDDLDialog(this.martTab, Collections.singleton(dataset),
					SaveDDLDialog.VIEW_DDL)).setVisible(true);
	}

	/**
	 * Ask that an explanation dialog be opened that explains how the given
	 * dataset table was constructed.
	 * 
	 * @param dsTable
	 *            the dataset table that needs to be explained.
	 */
	public void requestExplainTable(final DataSetTable dsTable) {
		ExplainTableDialog.showTableExplanation(this.martTab, dsTable);
	}

	/**
	 * Ask that an explanation dialog be opened that explains how the given
	 * dataset was constructed.
	 * 
	 * @param dataset
	 *            the dataset that needs to be explained.
	 */
	public void requestExplainDataSet(final DataSet dataset) {
		ExplainDataSetDialog.showDataSetExplanation(this.martTab, dataset);
	}

	/**
	 * Requests that the dataset be made invisible.
	 * 
	 * @param dataset
	 *            the dataset to make invisible.
	 */
	public void requestInvisibleDataSet(final DataSet dataset) {
		Transaction.start();
		dataset.setInvisible(true);
		Transaction.end();
	}

	/**
	 * Run the partition dimension wizard.
	 * 
	 * @param dim
	 *            the dimension to apply the wizard to.
	 */
	public void requestDimensionPartitionWizard(final DataSetTable dim) {
		// Create wizard dialog (specify dimension version)
		PartitionTableDialog.showForDimension(this, dim);
	}

	/**
	 * Run the partition dataset wizard.
	 * 
	 * @param ds
	 *            the dataset to apply the wizard to.
	 */
	public void requestDataSetPartitionWizard(final DataSet ds) {
		PartitionTableDialog.showForDataSet(ds);
	}

	/**
	 * Requests that a column be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to mask.
	 */
	public void requestMaskColumn(final DataSet ds, final DataSetColumn column) {
		this.requestMaskColumns(ds, Collections.singleton(column));
	}

	/**
	 * Requests that a set of columns be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param columns
	 *            the columns to mask.
	 */
	public void requestMaskColumns(final DataSet ds, final Collection columns) {
		try {
			Transaction.start();
			for (final Iterator i = columns.iterator(); i.hasNext();)
				((DataSetColumn) i.next()).setColumnMasked(true);
		} catch (final ValidationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Requests that a column be indexed.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to index.
	 */
	public void requestIndexColumn(final DataSet ds, final DataSetColumn column) {
		Transaction.start();
		column.setColumnIndexed(true);
		Transaction.end();
	}

	/**
	 * Requests that a table be made undistinct.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to make undistinct.
	 */
	public void requestUndistinctTable(final DataSet ds, final DataSetTable dst) {
		Transaction.start();
		dst.setDistinctTable(false);
		Transaction.end();
	}

	/**
	 * Requests that a table be made distinct.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to make distinct.
	 */
	public void requestDistinctTable(final DataSet ds, final DataSetTable dst) {
		Transaction.start();
		dst.setDistinctTable(true);
		Transaction.end();
	}

	/**
	 * Requests that a dimension be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dim
	 *            the dimension to mask.
	 */
	public void requestMaskDimension(final DataSet ds, final DataSetTable dim) {
		try {
			Transaction.start();
			dim.setDimensionMasked(true);
		} catch (final ValidationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Requests that a dimension be unmasked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dim
	 *            the dimension to unmask.
	 */
	public void requestUnmaskDimension(final DataSet ds, final DataSetTable dim) {
		try {
			Transaction.start();
			dim.setDimensionMasked(false);
		} catch (final ValidationException ve) {
			StackTrace.showStackTrace(ve);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Asks that a dimension by merged.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the dimension to merge.
	 */
	public void requestMergeDimension(final DataSet ds, final DataSetTable dst) {
		Transaction.start();
		dst.getFocusRelation().setMergeRelation(ds, true);
		Transaction.end();
	}

	/**
	 * Asks that a dimension by unmerged.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the dimension to unmerge.
	 */
	public void requestUnmergeDimension(final DataSet ds, final DataSetTable dst) {
		Transaction.start();
		dst.getFocusRelation().setMergeRelation(ds, false);
		Transaction.end();
	}

	/**
	 * Asks that a relation be recursively subclassed.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to recursively subclass.
	 */
	public void requestRecurseSubclass(final DataSet ds, final DataSetTable dst) {
		// Work out if it is already compounded.
		final Relation relation = dst.getFocusRelation();
		if (!relation.isSubclassRelation(ds))
			return;
		CompoundRelationDefinition def = relation.getCompoundRelation(ds);
		if (def == null)
			def = new CompoundRelationDefinition(1, false);

		// Pop up a dialog and update 'compound'.
		final CompoundRelationDialog dialog = new CompoundRelationDialog(def,
				Resources.get("recurseSubclassDialogTitle"), Resources
						.get("recurseSubclassNLabel"), true, ds.getMart()
						.getPartitionColumnNames());
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		final int newN = dialog.getArity();
		final boolean newParallel = dialog.getParallel();

		// Skip altogether if no change.
		if (newN == def.getN() && newParallel == def.isParallel())
			return;

		Transaction.start();
		if (newN <= 1)
			// Uncompound the relation.
			relation.setCompoundRelation(ds, null);
		else {
			// Compound the relation.
			def.setN(newN);
			def.setParallel(newParallel);
			if (relation.getCompoundRelation(ds) == null)
				relation.setCompoundRelation(ds, def);
		}
		Transaction.end();
	}

	/**
	 * Asks that a dimension be replicated by compounding a relation.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to replicate.
	 */
	public void requestReplicateDimension(final DataSet ds,
			final DataSetTable dst) {
		// Work out if it is already compounded.
		final Relation relation = dst.getFocusRelation();
		CompoundRelationDefinition def = relation.getCompoundRelation(ds);
		if (def == null)
			def = new CompoundRelationDefinition(1, false);

		// Pop up a dialog and update 'compound'.
		final CompoundRelationDialog dialog = new CompoundRelationDialog(def,
				Resources.get("replicateDimensionDialogTitle"), Resources
						.get("replicateDimensionNLabel"), true, ds.getMart()
						.getPartitionColumnNames());
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		final int newN = dialog.getArity();
		final boolean newParallel = dialog.getParallel();

		// Skip altogether if no change.
		if (newN == def.getN() && newParallel == def.isParallel())
			return;

		Transaction.start();
		if (newN <= 1)
			// Uncompound the relation.
			relation.setCompoundRelation(ds, null);
		else {
			// Compound the relation.
			def.setN(newN);
			def.setParallel(newParallel);
			if (relation.getCompoundRelation(ds) == null)
				relation.setCompoundRelation(ds, def);
		}
		Transaction.end();
	}

	/**
	 * Asks that a relation be unrolled.
	 * 
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to make unrolled.
	 */
	public void requestUnrolledRelation(final DataSetTable dst,
			final Relation relation) {
		// Work out if it is already directional.
		final Column def = relation.getUnrolledRelation(dst.getDataSet(), dst
				.getName());

		// Pop up a dialog and update 'direction'.
		final UnrolledRelationDialog dialog = new UnrolledRelationDialog(def,
				relation);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		final Column newDef = dialog.getChosenColumn();

		// Skip altogether if no change.
		if (newDef == def)
			return;

		Transaction.start();
		relation.setUnrolledRelation(dst.getDataSet(), dst.getName(), newDef);
		Transaction.end();
	}

	/**
	 * Asks that a relation be compounded.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to compound.
	 */
	public void requestCompoundRelation(final DataSet ds,
			final DataSetTable dst, final Relation relation) {
		// Work out if it is already compounded.
		CompoundRelationDefinition def = dst == null ? relation
				.getCompoundRelation(ds) : relation.getCompoundRelation(ds, dst
				.getName());
		if (def == null)
			def = new CompoundRelationDefinition(1, false);

		// Pop up a dialog and update 'compound'.
		final CompoundRelationDialog dialog = new CompoundRelationDialog(def,
				Resources.get("compoundRelationDialogTitle"), Resources
						.get("compoundRelationNLabel"), false, ds.getMart()
						.getPartitionColumnNames());
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		final int newN = dialog.getArity();
		final boolean newParallel = dialog.getParallel();

		// Skip altogether if no change.
		if (newN == def.getN() && newParallel == def.isParallel())
			return;

		Transaction.start();
		// Do the work.
		if (newN <= 1) {
			// Uncompound the relation.
			if (dst != null)
				relation.setCompoundRelation(ds, dst.getName(), null);
			else
				relation.setCompoundRelation(ds, null);
		} else {
			// Compound the relation.
			def.setN(newN);
			def.setParallel(newParallel);
			if (dst != null) {
				if (relation.getCompoundRelation(ds, dst.getName()) == null)
					relation.setCompoundRelation(ds, dst.getName(), def);
			} else if (relation.getCompoundRelation(ds) == null)
				relation.setCompoundRelation(ds, def);
		}
		Transaction.end();
	}

	private int askUserForCompoundRelationIndex(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation) {
		// Is the relation compound? If not, return 0.
		if (relation.getCompoundRelation(dataset) == null
				&& (dsTable == null || relation.getCompoundRelation(dataset,
						dsTable.getName()) == null))
			return 0;

		// Work out possible options.
		final int maxIndex = (dsTable == null ? relation
				.getCompoundRelation(dataset) : relation.getCompoundRelation(
				dataset, dsTable.getName())).getN();
		final Integer[] options = new Integer[maxIndex];
		for (int i = 0; i < options.length; i++)
			options[i] = new Integer(i + 1);

		// Return -1 if cancelled.
		final Integer selIndex = (Integer) JOptionPane.showInputDialog(null,
				Resources.get("compoundRelationIndex"), Resources
						.get("questionTitle"), JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);

		// If they cancelled the request, return null.
		if (selIndex == null)
			return -1;
		else
			return selIndex.intValue() - 1;
	}

	/**
	 * Asks that a relation be restricted.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to restrict.
	 * @param iteration
	 *            the iteration to apply this to, or
	 *            {@link RealisedRelation#NO_ITERATION} to prompt the user.
	 */
	public void requestRestrictRelation(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation,
			final int iteration) {
		// Get index offset into compound relation.
		final int index = iteration != RealisedRelation.NO_ITERATION ? iteration
				: (dsTable == null ? relation.getCompoundRelation(dataset)
						: relation.getCompoundRelation(dataset, dsTable
								.getName())) != null ? this
						.askUserForCompoundRelationIndex(dataset, dsTable,
								relation) : 0;

		// Cancelled?
		if (index == -1)
			return;

		final RestrictedRelationDialog dialog = new RestrictedRelationDialog(
				relation, dsTable == null ? relation.getRestrictRelation(
						dataset, index) : relation.getRestrictRelation(dataset,
						dsTable.getName(), index));
		dialog.setVisible(true);

		// Cancelled?
		if (dialog.getCancelled())
			return;

		// Get updated details from the user.
		final Map aliasesLHS = dialog.getLHSColumnAliases();
		final Map aliasesRHS = dialog.getRHSColumnAliases();
		final String expression = dialog.getExpression();
		final boolean hard = dialog.getHard();

		Transaction.start();
		RestrictedRelationDefinition def = dsTable == null ? relation
				.getRestrictRelation(dataset, index) : relation
				.getRestrictRelation(dataset, dsTable.getName(), index);
		if (def == null) {
			def = new RestrictedRelationDefinition(expression, aliasesLHS,
					aliasesRHS, hard);
			if (dsTable == null)
				relation.setRestrictRelation(dataset, def, iteration);
			else
				relation.setRestrictRelation(dataset, dsTable.getName(), def,
						iteration);
		} else {
			def.setExpression(expression);
			def.setHard(hard);
			def.getLeftAliases().clear();
			def.getLeftAliases().putAll(aliasesLHS);
			def.getRightAliases().clear();
			def.getRightAliases().putAll(aliasesRHS);
		}
		Transaction.end();
	}

	/**
	 * Asks for a relation restriction to be removed.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table to work with.
	 * @param relation
	 *            the relation to unrestrict.
	 * @param iteration
	 *            the iteration to apply this to, or
	 *            {@link RealisedRelation#NO_ITERATION} to prompt the user.
	 */
	public void requestUnrestrictRelation(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation,
			final int iteration) {
		// Get index offset into compound relation.
		final int index = iteration != RealisedRelation.NO_ITERATION ? iteration
				: (dsTable == null ? relation.getCompoundRelation(dataset)
						: relation.getCompoundRelation(dataset, dsTable
								.getName())) != null ? this
						.askUserForCompoundRelationIndex(dataset, dsTable,
								relation) : 0;

		// Cancelled?
		if (index == -1)
			return;

		Transaction.start();
		if (dsTable != null)
			relation.setRestrictRelation(dataset, dsTable.getName(), null,
					iteration);
		else
			relation.setRestrictRelation(dataset, null, iteration);
		Transaction.end();
	}

	/**
	 * Asks that a relation be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to mask.
	 */
	public void requestMaskRelation(final DataSet ds, final DataSetTable dst,
			final Relation relation) {
		Transaction.start();
		if (dst != null)
			relation.setMaskRelation(ds, dst.getName(), true);
		else
			relation.setMaskRelation(ds, true);
		Transaction.end();
	}

	/**
	 * Asks that a relation be loopbacked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to loopback.
	 */
	public void requestLoopbackRelation(final DataSet ds,
			final DataSetTable dst, final Relation relation) {

		// Work out if it is already compounded.
		final Column loopedCol = dst == null ? relation.getLoopbackRelation(ds)
				: relation.getLoopbackRelation(ds, dst.getName());
		final boolean isLooped = loopedCol != null;

		// Pop up a dialog and update 'compound'.
		final LoopbackRelationDialog dialog = new LoopbackRelationDialog(
				isLooped, loopedCol, relation.getManyKey().getTable()
						.getColumns().values());
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		final boolean newIsLooped = dialog.isLoopback();
		final Column newLoopedCol = newIsLooped ? dialog
				.getLoopbackDiffColumn() : null;

		// Skip altogether if no change.
		if (newLoopedCol == loopedCol && newIsLooped)
			return;

		try {
			Transaction.start();
			// Do the work.
			if (dst != null)
				relation.setLoopbackRelation(ds, dst.getName(), loopedCol);
			else
				relation.setLoopbackRelation(ds, loopedCol);
		} catch (final ValidationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();

		}
	}

	/**
	 * Pop up a dialog explaining the current partition table conversion status
	 * of this dataset. The dialog will do any updating necessary and return a
	 * flag indicating this.
	 * 
	 * @param ds
	 *            the dataset to modify partition table info for.
	 */
	public void requestConvertPartitionTable(final DataSet ds) {
		if (ds.isConvertableToPartitionTable())
			new PartitionTableDialog(ds).setVisible(true);
		else
			StackTrace.showStackTrace(new PartitionException(Resources
					.get("partitionTypesLimited")));
	}

	/**
	 * Asks that a dataset be (un)masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param masked
	 *            mask it?
	 */
	public void requestMaskDataSet(final DataSet ds, final boolean masked) {
		Transaction.start();
		ds.setMasked(masked);
		Transaction.end();
	}

	/**
	 * Asks that a relation be forced.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to force.
	 */
	public void requestForceRelation(final DataSet ds, final DataSetTable dst,
			final Relation relation) {
		Transaction.start();
		if (dst != null)
			relation.setForceRelation(ds, dst.getName(), true);
		else
			relation.setForceRelation(ds, true);

		Transaction.end();
	}

	/**
	 * Asks that all relations on a table be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param table
	 *            the schema table to mask all relations for.
	 */
	public void requestMaskAllRelations(final DataSet ds,
			final DataSetTable dst, final Table table) {
		Transaction.start();
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();) {
			final Relation rel = (Relation) i.next();
			if (dst != null)
				rel.setMaskRelation(ds, dst.getName(), true);
			else
				rel.setMaskRelation(ds, true);
		}
		Transaction.end();
	}

	/**
	 * Asks for a expression column to be modified.
	 * 
	 * @param dsTable
	 *            the table to work with.
	 * @param column
	 *            the existing expression.
	 */
	public void requestExpressionColumn(final DataSetTable dsTable,
			ExpressionColumn column) {
		final ExpressionColumnDialog dialog = new ExpressionColumnDialog(
				dsTable, column == null ? null : column.getDefinition(), column);
		dialog.setVisible(true);
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final boolean groupBy = dialog.getGroupBy();

		Transaction.start();
		if (column == null) {
			final String name = dsTable.getNextExpressionColumn();
			column = new ExpressionColumn(name, dsTable,
					new ExpressionColumnDefinition(expression, aliases,
							groupBy, name));
			dsTable.getColumns().put(column.getName(), column);
		} else {
			column.getDefinition().getAliases().clear();
			column.getDefinition().getAliases().putAll(aliases);
			column.getDefinition().setExpression(expression);
			column.getDefinition().setGroupBy(groupBy);
		}
		Transaction.end();
	}

	/**
	 * Asks for a expression column to be removed.
	 * 
	 * @param dsTable
	 *            the table to work with.
	 * @param column
	 *            the existing expression.
	 */
	public void requestRemoveExpressionColumn(final DataSetTable dsTable,
			final ExpressionColumn column) {
		Transaction.start();
		dsTable.getColumns().remove(column.getName());
		Transaction.end();
	}

	/**
	 * Asks for a table restriction to be modified.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table to work with.
	 * @param table
	 *            the table to modify the restriction for.
	 */
	public void requestRestrictTable(final DataSet dataset,
			final DataSetTable dsTable, final Table table) {
		final RestrictedTableDialog dialog = new RestrictedTableDialog(table,
				dsTable == null ? table.getRestrictTable(dataset) : table
						.getRestrictTable(dataset, dsTable.getName()));
		dialog.setVisible(true);
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final boolean hard = dialog.getHard();

		Transaction.start();
		RestrictedTableDefinition def = dsTable == null ? table
				.getRestrictTable(dataset) : table.getRestrictTable(dataset,
				dsTable.getName());
		if (def != null) {
			def.getAliases().clear();
			def.getAliases().putAll(aliases);
			def.setExpression(expression);
			def.setHard(hard);
		} else {
			def = new RestrictedTableDefinition(expression, aliases, hard);
			if (dsTable != null)
				table.setRestrictTable(dataset, dsTable.getName(), def);
			else
				table.setRestrictTable(dataset, def);
		}
		Transaction.end();
	}

	/**
	 * Asks the user if they are sure they want to remove all datasets, then
	 * removes them from the mart (and the tabs) if they agree.
	 */
	public void requestRemoveAllDataSets() {
		// Confirm the decision first.
		final int choice = JOptionPane.showConfirmDialog(null, Resources
				.get("confirmDelAllDatasets"), Resources.get("questionTitle"),
				JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		Transaction.start();
		DataSetTabSet.this.martTab.getMart().getDataSets().clear();
		Transaction.end();
	}

	/**
	 * Asks the user if they are sure they want to remove the dataset, then
	 * removes it from the mart (and the tabs) if they agree.
	 * 
	 * @param dataset
	 *            the dataset to remove.
	 */
	public void requestRemoveDataSet(final DataSet dataset) {
		// Confirm the decision first.
		final int choice = JOptionPane.showConfirmDialog(null, Resources
				.get("confirmDelDataset"), Resources.get("questionTitle"),
				JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		Transaction.start();
		this.martTab.getMart().getDataSets().remove(dataset.getName());
		Transaction.end();
	}

	/**
	 * Asks for a table restriction to be removed.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table to work with.
	 * @param table
	 *            the table to unrestrict.
	 */
	public void requestUnrestrictTable(final DataSet dataset,
			final DataSetTable dsTable, final Table table) {
		Transaction.start();
		if (dsTable != null)
			table.setRestrictTable(dataset, dsTable.getName(), null);
		else
			table.setRestrictTable(dataset, null);
		Transaction.end();
	}

	/**
	 * Renames a dataset, then renames the tab too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 */
	public void requestRenameDataSet(final DataSet dataset) {
		// Ask user for the new name.
		this.requestRenameDataSet(dataset, this.askUserForName(Resources
				.get("requestDataSetName"), dataset.getName()));

	}

	/**
	 * Renames a dataset, then renames the tab too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 * @param name
	 *            the new name to give it.
	 */
	public void requestRenameDataSet(final DataSet dataset, final String name) {
		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		final String newName = name == null ? "" : name.trim();
		if (newName.length() == 0)
			return;

		Transaction.start();
		dataset.setName(newName);
		Transaction.end();
	}

	/**
	 * Renames a column, after prompting the user to enter a new name. By
	 * default, the existing name is used. If the name entered is blank or
	 * matches the existing name, no change is made.
	 * 
	 * @param dsColumn
	 *            the column to rename.
	 */
	public void requestRenameDataSetColumn(final DataSetColumn dsColumn) {
		// Ask user for the new name.
		this.requestRenameDataSetColumn(dsColumn, this.askUserForName(Resources
				.get("requestDataSetColumnName"), dsColumn.getModifiedName()));
	}

	/**
	 * Renames a dataset column to have the given name.
	 * 
	 * @param dsColumn
	 *            the column to rename.
	 * @param name
	 *            the new name to give it.
	 */
	public void requestRenameDataSetColumn(final DataSetColumn dsColumn,
			final String name) {
		// Ask user for the new name.
		final String newName = name == null ? "" : name.trim();

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName.length() == 0)
			return;

		try {
			Transaction.start();
			dsColumn.setColumnRename(newName);
		} catch (final ValidationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Renames a table, after prompting the user to enter a new name. By
	 * default, the existing name is used. If the name entered is blank or
	 * matches the existing name, no change is made.
	 * 
	 * @param dsTable
	 *            the table to rename.
	 */
	public void requestRenameDataSetTable(final DataSetTable dsTable) {
		// Ask user for the new name.
		this.requestRenameDataSetTable(dsTable, this.askUserForName(Resources
				.get("requestDataSetTableName"), dsTable.getModifiedName()));
	}

	/**
	 * Renames a table.
	 * 
	 * @param dsTable
	 *            the table to rename.
	 * @param name
	 *            the new name to give it.
	 */
	public void requestRenameDataSetTable(final DataSetTable dsTable,
			final String name) {
		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		final String newName = name == null ? "" : name.trim();
		if (newName.length() == 0)
			return;

		Transaction.start();
		dsTable.setTableRename(newName);
		Transaction.end();
	}

	/**
	 * Requests that a relation be flagged as a subclass relation.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to subclass.
	 */
	public void requestSubclassRelation(final DataSet ds,
			final Relation relation) {

		try {
			Transaction.start();
			relation.setSubclassRelation(ds, true);
		} catch (final ValidationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Given a table, suggest a series of synchronised datasets that may be
	 * possible for that table.
	 * 
	 * @param table
	 *            the table to suggest datasets for. If <tt>null</tt>, no
	 *            default table is used.
	 */
	public void requestSuggestDataSets(final Table table) {
		// Ask the user what tables they want to work with and what
		// mode they want.
		final SuggestDataSetDialog dialog = new SuggestDataSetDialog(
				this.martTab.getMart().getSchemas().values(), table);
		dialog.setVisible(true);

		// If they cancelled it, return without doing anything.
		if (dialog.getSelectedTables().isEmpty())
			return;

		try {
			Transaction.start();
			this.martTab.getMart().suggestDataSets(dialog.getSelectedTables());
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		} finally {
			Transaction.resetVisibleModified();
			Transaction.end();
		}
	}

	/**
	 * Given a table, suggest a series of synchronised invisible datasets that
	 * may be possible for that table. This requires a set of columns, for which
	 * the user will be prompted as necessary.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param table
	 *            the table to use to obtain columns to suggest invisible
	 *            datasets for.
	 */
	public void requestSuggestInvisibleDatasets(final DataSet dataset,
			final DataSetTable table) {
		// Ask the user what tables they want to work with and what
		// mode they want.
		final SuggestInvisibleDataSetDialog dialog = new SuggestInvisibleDataSetDialog(
				table);
		dialog.setVisible(true);

		// If they cancelled it, return without doing anything.
		if (dialog.getSelectedColumns().isEmpty())
			return;

		try {
			Transaction.start();
			this.martTab.getMart().suggestInvisibleDataSets(dataset,
					dialog.getSelectedColumns());
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		} finally {
			Transaction.resetVisibleModified();
			Transaction.end();
		}
	}

	/**
	 * Requests that a column be unmasked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to unmask.
	 */
	public void requestUnmaskColumn(final DataSet ds, final DataSetColumn column) {
		this.requestUnmaskColumns(ds, Collections.singleton(column));
	}

	/**
	 * Request that a bunch of columns be unmasked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param columns
	 *            the columns to unmask.
	 */
	public void requestUnmaskColumns(final DataSet ds, final Collection columns) {
		try {
			Transaction.start();
			for (final Iterator i = columns.iterator(); i.hasNext();)
				((DataSetColumn) i.next()).setColumnMasked(false);
		} catch (final ValidationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Requests that a column be unindexed.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to unindex.
	 */
	public void requestUnindexColumn(final DataSet ds,
			final DataSetColumn column) {
		Transaction.start();
		column.setColumnIndexed(false);
		Transaction.end();
	}

	/**
	 * Asks that a relation be unforced.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to unforce.
	 */
	public void requestUnforceRelation(final DataSet ds,
			final DataSetTable dst, final Relation relation) {
		Transaction.start();
		if (dst != null)
			relation.setForceRelation(ds, dst.getName(), false);
		else
			relation.setForceRelation(ds, false);
		Transaction.end();
	}

	/**
	 * Asks that a relation be unmasked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to unmask.
	 */
	public void requestUnmaskRelation(final DataSet ds, final DataSetTable dst,
			final Relation relation) {
		Transaction.start();
		if (dst != null)
			relation.setMaskRelation(ds, dst.getName(), false);
		else
			relation.setMaskRelation(ds, false);
		Transaction.end();
	}

	/**
	 * Asks that all relations on a table be unmasked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param table
	 *            the schema table to unmask all relations for.
	 */
	public void requestUnmaskAllRelations(final DataSet ds,
			final DataSetTable dst, final Table table) {
		Transaction.start();
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();) {
			final Relation rel = (Relation) i.next();
			if (dst != null)
				rel.setMaskRelation(ds, dst.getName(), false);
			else
				rel.setMaskRelation(ds, false);
		}
		Transaction.end();
	}

	/**
	 * Requests that the subclass flag be removed from a relation.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to un-subclass.
	 */
	public void requestUnsubclassRelation(final DataSet ds,
			final Relation relation) {
		try {
			Transaction.start();
			relation.setSubclassRelation(ds, false);
		} catch (final ValidationException ve) {
			StackTrace.showStackTrace(ve);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Requests that the dataset be unindex optimised.
	 * 
	 * @param dataset
	 *            the dataset to do this to.
	 */
	public void requestNoIndexOptimiser(final DataSet dataset) {
		Transaction.start();
		dataset.setIndexOptimiser(false);
		Transaction.end();
	}

	/**
	 * Requests that the dataset be index optimised.
	 * 
	 * @param dataset
	 *            the dataset to do this to.
	 */
	public void requestIndexOptimiser(final DataSet dataset) {
		Transaction.start();
		dataset.setIndexOptimiser(true);
		Transaction.end();
	}

	/**
	 * Requests that the dataset be made visible.
	 * 
	 * @param dataset
	 *            the dataset to make visible.
	 */
	public void requestVisibleDataSet(final DataSet dataset) {
		Transaction.start();
		dataset.setInvisible(false);
		Transaction.end();
	}
}
