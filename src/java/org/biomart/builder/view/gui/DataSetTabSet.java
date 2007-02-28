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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.AllDataSetsDiagram;
import org.biomart.builder.view.gui.diagrams.DataSetDiagram;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.contexts.AllDataSetsContext;
import org.biomart.builder.view.gui.diagrams.contexts.DataSetContext;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.builder.view.gui.dialogs.CompoundRelationDialog;
import org.biomart.builder.view.gui.dialogs.ConcatRelationDialog;
import org.biomart.builder.view.gui.dialogs.ExplainDataSetDialog;
import org.biomart.builder.view.gui.dialogs.ExplainDialog;
import org.biomart.builder.view.gui.dialogs.ExplainTableDialog;
import org.biomart.builder.view.gui.dialogs.ExpressionColumnDialog;
import org.biomart.builder.view.gui.dialogs.PartitionColumnDialog;
import org.biomart.builder.view.gui.dialogs.RestrictedRelationDialog;
import org.biomart.builder.view.gui.dialogs.RestrictedTableDialog;
import org.biomart.builder.view.gui.dialogs.SaveDDLDialog;
import org.biomart.builder.view.gui.dialogs.SuggestDataSetDialog;
import org.biomart.builder.view.gui.dialogs.SuggestInvisibleDataSetDialog;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.Task;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.StackTrace;

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
 * @since 0.1
 */
public class DataSetTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private AllDataSetsDiagram allDataSetsDiagram;

	private final Collection currentExplanationDialogs = new HashSet();

	// Use double-list to prevent problems with hashcodes changing.
	private final List[] datasetToDiagram = new List[] { new ArrayList(),
			new ArrayList() };

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

		Log.info(Resources.get("logCreateDatasetTabs"));

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
		this.addTab(Resources.get("multiDataSetOverviewTab"), scroller);
		this.setForegroundAt(0, Color.WHITE);

		// Calculate the dataset tabs.
		this.recalculateDataSetTabs();
	}

	/**
	 * Works out which dataset tab is selected, and return it.
	 * 
	 * @return the currently selected dataset tab, or <tt>null</tt> if none is
	 *         selected.
	 */
	public DataSet getSelectedDataSet() {
		if (this.getSelectedIndex()<=0 || !this.isShowing()) return null;
		final DataSetDiagram selectedDiagram = (DataSetDiagram)((JScrollPane) this.getSelectedComponent())
				.getViewport().getView();
		return selectedDiagram.getDataSet();
	}

	private void addDataSetTab(final DataSet dataset,
			final boolean selectDataset) {
		Log.info(Resources.get("logAddDatasetTab", "" + dataset));
		// Create the diagram to represent this dataset.
		final DataSetDiagram datasetDiagram = new DataSetDiagram(this.martTab,
				dataset);

		// Create a scroller to contain the diagram.
		final JScrollPane scroller = new JScrollPane(datasetDiagram);
		scroller.getViewport().setBackground(datasetDiagram.getBackground());

		// Add a tab containing the scroller, with the same name as the dataset.
		this.addTab(dataset.getName(), scroller);

		// Remember which diagram the dataset is connected with.
		this.datasetToDiagram[0].add(dataset);
		this.datasetToDiagram[1].add(datasetDiagram);

		// Set the current context on the diagram to be the same as the
		// current context on this dataset tabset.
		datasetDiagram.setDiagramContext(new DataSetContext(this.martTab,
				dataset));

		if (selectDataset) {
			// Fake a click on the dataset tab.
			this.setSelectedIndex(this.indexOfTab(dataset.getName()));
			this.martTab.selectDataSetEditor();
		} else {
			// Fake a click on the overview tab.
			this.setSelectedIndex(0);
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

		// Add an option to replicate this dataset.
		final JMenuItem replicate = new JMenuItem(Resources
				.get("replicateDataSetTitle"));
		replicate
				.setMnemonic(Resources.get("replicateDataSetMnemonic").charAt(
						0));
		replicate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetTabSet.this
						.requestReplicateDataSet(dataset);
			}
		});
		contextMenu.add(replicate);

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

	private void removeDataSetTab(final DataSet dataset) {
		Log.info(Resources.get("logRemoveDatasetTab", "" + dataset));
		// Work out the currently selected tab.
		final int currentTab = this.getSelectedIndex();

		// Work out which tab the dataset lives in.
		final int index = this.datasetToDiagram[0].indexOf(dataset);

		// Work out the tab index.
		final int tabIndex = this.indexOfTab(dataset.getName());

		// Remove the tab, and it's mapping from the dataset-to-tab map.
		this.removeTabAt(tabIndex);
		this.datasetToDiagram[0].remove(index);
		this.datasetToDiagram[1].remove(index);

		// Update the overview diagram.
		this.recalculateOverviewDiagram();

		// Fake a click on the last tab before this one to ensure
		// at least one tab remains visible and up-to-date.
		this.setSelectedIndex(currentTab == 0 ? 0 : Math.max(tabIndex - 1, 0));
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
	 * This method is called by the {@link ExplainDialog} when it is opened.
	 * This dialog is then updated whenever the dataset window is updated.
	 * 
	 * @param dialog
	 *            the dialog being displayed by a current {@link ExplainDialog}.
	 */
	public void addCurrentExplanationDialog(final ExplainDialog dialog) {
		this.currentExplanationDialogs.add(dialog);
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
	 * Asks all dataset {@link Diagram}s in all dataset tabs to recalculate
	 * themselves to match the current contents of the datasets.
	 */
	public void recalculateAllDataSetDiagrams() {
		for (final Iterator i = this.datasetToDiagram[1].iterator(); i
				.hasNext();)
			((Diagram) i.next()).recalculateDiagram();
	}

	/**
	 * Works out which tab is displaying the given dataset, then asks the
	 * diagram in that tab to recalculate itself to match the current contents
	 * of the dataset.
	 * 
	 * @param dataset
	 *            the dataset to recalculate the diagram for.
	 */
	public void recalculateDataSetDiagram(final DataSet dataset) {
		final int index = this.datasetToDiagram[0].indexOf(dataset);
		((Diagram) this.datasetToDiagram[1].get(index)).recalculateDiagram();
	}

	/**
	 * Recalculates the list of dataset tabs, so that they tally with the list
	 * of datasets currently within the mart. This removes any datasets from the
	 * tabs which are no longer in the mart, and adds new tabs for those which
	 * are in the mart but not yet displayed.
	 */
	public void recalculateDataSetTabs() {
		Log.info(Resources.get("logRecalcDatasetTabs"));
		// Synchronise the datasets first.
		try {
			MartBuilderUtils.synchroniseMartDataSets(this.martTab.getMart());
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
			return;
		}

		// Add all datasets in the mart that we don't have yet.
		// We work with a copy of the list of datasets else we get
		// concurrent modification exceptions as new ones are added.
		for (final Iterator i = this.martTab.getMart().getDataSets().iterator(); i
				.hasNext();) {
			final DataSet dataset = (DataSet) i.next();
			if (!this.datasetToDiagram[0].contains(dataset))
				this.addDataSetTab(dataset, false);
		}

		// Remove all datasets we have that are no longer in the mart.
		// We work with a copy of the list of datasets else we get
		// concurrent modification exceptions as old ones are removed.
		final List ourDataSets = new ArrayList(this.datasetToDiagram[0]);
		for (final Iterator i = ourDataSets.iterator(); i.hasNext();) {
			final DataSet dataset = (DataSet) i.next();
			if (!this.martTab.getMart().getDataSets().contains(dataset))
				this.removeDataSetTab(dataset);
		}

		// Update the overview diagram.
		this.recalculateOverviewDiagram();
	}

	/**
	 * Causes {@link ExplainDialog#recalculateDialog()} to be called on the
	 * currently visible explanation dialog, if any.
	 */
	public void recalculateExplanationDialog() {
		for (final Iterator i = this.currentExplanationDialogs.iterator(); i
				.hasNext();)
			((ExplainDialog) i.next()).recalculateDialog();
	}

	/**
	 * Causes {@link Diagram#recalculateDiagram()} to be called on the tab which
	 * represents all the datasets in the mart.
	 */
	public void recalculateOverviewDiagram() {
		this.allDataSetsDiagram.recalculateDiagram();
	}

	/**
	 * This method is called by the {@link ExplainDialog} when it is closed.
	 * 
	 * @param dialog
	 *            the dialog being closed by a current {@link ExplainDialog}.
	 */
	public void removeCurrentExplanationDialog(final ExplainDialog dialog) {
		this.currentExplanationDialogs.remove(dialog);
	}

	/**
	 * Asks all dataset {@link Diagram}s in all dataset tabs to repaint
	 * themselves, in case any components have changed appearance. Do not use
	 * this if the components have changed size - use recalculate instead.
	 */
	public void repaintAllDataSetDiagrams() {
		for (final Iterator i = this.datasetToDiagram[1].iterator(); i
				.hasNext();)
			((Diagram) i.next()).repaintDiagram();
	}

	/**
	 * Works out which tab is displaying the given dataset, then asks the
	 * diagram in that tab to repaint itself, in case any of the components have
	 * changed appearance. Do not use this if the components have changed size -
	 * use recalculate instead.
	 * 
	 * @param dataset
	 *            the dataset to repaint the diagram for.
	 */
	public void repaintDataSetDiagram(final DataSet dataset) {
		final int index = this.datasetToDiagram[0].indexOf(dataset);
		((Diagram) this.datasetToDiagram[1].get(index)).repaintDiagram();
	}

	/**
	 * Causes {@link ExplainDialog#repaintDialog()} to be called on the
	 * currently visible explanation dialog, if any.
	 */
	public void repaintExplanationDialog() {
		for (final Iterator i = this.currentExplanationDialogs.iterator(); i
				.hasNext();)
			((ExplainDialog) i.next()).repaintDialog();
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on the tab which
	 * represents all the datasets in the mart.
	 */
	public void repaintOverviewDiagram() {
		this.allDataSetsDiagram.repaintDiagram();
	}

	private void runOnly(final Task task) {
		this.runOnly(task, null);
	}

	private void runOnly(final Task task, final DataSet ds) {
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					task.run();
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							// Update the modified status for this tabset.
							DataSetTabSet.this.martTab.getMartTabSet()
									.setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	private void runThenRecalculate(final Task task) {
		this.runThenRecalculate(task, null);
	}

	private void runThenRecalculate(final Task task, final DataSet ds) {
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					task.run();
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							// Repaint the dataset diagram based on the modified
							// dataset.
							if (ds != null)
								DataSetTabSet.this
										.recalculateDataSetDiagram(ds);
							else
								DataSetTabSet.this
										.recalculateAllDataSetDiagrams();
							
							// And the overview.
							DataSetTabSet.this.recalculateOverviewDiagram();

							// Update the explanation diagram so that it
							// correctly reflects any changed relation.
							DataSetTabSet.this.recalculateExplanationDialog();

							// Update the modified status for this tabset.
							DataSetTabSet.this.martTab.getMartTabSet()
									.setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	private void runThenRepaint(final Task task) {
		this.runThenRepaint(task, null);
	}

	private void runThenRepaint(final Task task, final DataSet ds) {
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					task.run();
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							// Repaint the dataset diagram based on the modified
							// dataset.
							if (ds != null)
								DataSetTabSet.this.repaintDataSetDiagram(ds);
							else
								DataSetTabSet.this.repaintAllDataSetDiagrams();
							
							// And the overview.
							DataSetTabSet.this.repaintOverviewDiagram();

							// Update the explanation diagram so that it
							// correctly reflects any changed relation.
							DataSetTabSet.this.repaintExplanationDialog();

							// Update the modified status for this tabset.
							DataSetTabSet.this.martTab.getMartTabSet()
									.setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
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
		this.runOnly(new Task() {
			public void run() throws Throwable {
				// Change the type.
				MartBuilderUtils.changeOptimiserType(dataset, type);
			}
		});
	}

	/**
	 * Asks that a relation be restricted.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table we are working with.
	 * @param relation
	 *            the schema relation to mask.
	 */
	public void requestConcatRelation(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation) {
		// Get index offset into compound relation.
		final int index = dataset.getSchemaModifications().isCompoundRelation(
				dsTable, relation) ? this.askUserForCompoundRelationIndex(
				dataset, dsTable, relation) : 0;

		// Cancelled?
		if (index == -1)
			return;

		final ConcatRelationDefinition definition = dataset
				.getSchemaModifications().getConcatRelation(dsTable, relation,
						index);

		final ConcatRelationDialog dialog = new ConcatRelationDialog(relation
				.getManyKey().getTable(), definition);
		dialog.show();

		final String colKey = definition == null ? dataset
				.getSchemaModifications().nextConcatColumn() : definition
				.getColKey();

		// Cancelled?
		if (dialog.getCancelled())
			return;

		// Get updated details from the user.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final String rowSep = dialog.getRowSep();
		final RecursionType recursionType = dialog.getRecursionType();
		final Key recursionKey = dialog.getRecursionKey();
		final Relation firstRelation = dialog.getFirstRelation();
		final Relation secondRelation = dialog.getSecondRelation();
		final String concSep = dialog.getConcSep();
		// Do this in the background.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Update the restriction.
				if (dsTable != null)
					MartBuilderUtils.concatRelation(dsTable, relation, index,
							colKey, aliases, expression, rowSep,
							recursionType, recursionKey, firstRelation, secondRelation,
							concSep);
				else
					MartBuilderUtils.concatRelation(dataset, relation, index,
							colKey, aliases, expression, rowSep,
							recursionType, recursionKey, firstRelation, secondRelation,
							concSep);
			}
		}, dataset);
	}

	/**
	 * On a request to create DDL for the current dataset, open the DDL creation
	 * window with all this dataset selected.
	 * 
	 * @param dataset
	 *            the dataset to show the dialog for.
	 */
	public void requestCreateDDL(final DataSet dataset) {
		// Open the DDL creation dialog and let it do it's stuff.
		(new SaveDDLDialog(this.martTab, Collections.singleton(dataset)))
				.show();
	}

	/**
	 * Ask that an explanation dialog be opened that explains how the given
	 * dataset table was constructed.
	 * 
	 * @param dsTable
	 *            the dataset table that needs to be explained.
	 */
	public void requestExplainTable(final DataSetTable dsTable) {
		try {
			// Open the dialog. The dialog will set a flag in this instance
			// that contains a reference to its diagram, so that the diagram
			// can be updated as the user edits the dataset.
			ExplainTableDialog.showTableExplanation(this.martTab, dsTable);
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}
	}

	/**
	 * Ask that an explanation dialog be opened that explains how the given
	 * dataset was constructed.
	 * 
	 * @param dataset
	 *            the dataset that needs to be explained.
	 */
	public void requestExplainDataSet(final DataSet dataset) {
		try {
			// Open the dialog. The dialog will set a flag in this instance
			// that contains a reference to its diagram, so that the diagram
			// can be updated as the user edits the dataset.
			ExplainDataSetDialog.showDataSetExplanation(this.martTab, dataset);
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}
	}

	/**
	 * Requests that the dataset be made invisible.
	 * 
	 * @param dataset
	 *            the dataset to make invisible.
	 */
	public void requestInvisibleDataSet(final DataSet dataset) {
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Do the invisibility.
				MartBuilderUtils.invisibleDataSet(dataset);
			}
		});
	}

	/**
	 * Requests that all columns be non-inherited.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param table
	 *            the table to non-inherit all columns on.
	 */
	public void requestNonInheritAllColumns(final DataSet ds,
			final DataSetTable table) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask the column.
				MartBuilderUtils.nonInheritAllColumns(ds, table);
			}
		}, ds);
	}

	/**
	 * Requests that a column be non-inherited.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to non-inherit.
	 */
	public void requestNonInheritColumn(final DataSet ds,
			final DataSetColumn column) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask the column.
				MartBuilderUtils.nonInheritColumn(ds, column);
			}
		}, ds);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask the column.
				MartBuilderUtils.maskColumn(ds, column);
			}
		}, ds);
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
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Mask the column.
				MartBuilderUtils.indexColumn(ds, column);
			}
		}, ds);
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
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Mask the column.
				MartBuilderUtils.maskTable(ds, dim);
			}
		}, ds);
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
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Mask the column.
				MartBuilderUtils.unmaskTable(ds, dim);
			}
		}, ds);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Merge the relation.
				MartBuilderUtils.mergeRelation(ds, dst.getFocusRelation());
			}
		}, ds);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Merge the relation.
				MartBuilderUtils.unmergeRelation(ds, dst.getFocusRelation());
			}
		}, ds);
	}

	/**
	 * Asks that a relation be compounded.
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
		int n = 1;
		if (ds.getSchemaModifications().isCompoundRelation(dst, relation))
			n = ds.getSchemaModifications().getCompoundRelation(dst, relation);

		// Pop up a dialog and update 'compound'.
		final CompoundRelationDialog dialog = new CompoundRelationDialog(n,
				Resources.get("replicateDimensionDialogTitle"), Resources
						.get("replicateDimensionNLabel"));
		dialog.setLocationRelativeTo(null);
		dialog.show();
		final int newN = dialog.getArity();

		// Skip altogether if no change.
		if (newN == n)
			return;

		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Do the work.
				if (newN <= 1) {
					// Uncompound the relation.
					MartBuilderUtils.uncompoundRelation(ds, relation);
				} else {
					// Compound the relation.
					MartBuilderUtils.compoundRelation(ds, relation, newN);
				}
			}
		}, ds);
	}

	/**
	 * Asks that a relation be compounded.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param dst
	 *            the table to work with.
	 * @param relation
	 *            the schema relation to mask.
	 */
	public void requestCompoundRelation(final DataSet ds,
			final DataSetTable dst, final Relation relation) {
		// Work out if it is already compounded.
		int n = 1;
		if (ds.getSchemaModifications().isCompoundRelation(dst, relation))
			n = ds.getSchemaModifications().getCompoundRelation(dst, relation);

		// Pop up a dialog and update 'compound'.
		final CompoundRelationDialog dialog = new CompoundRelationDialog(n,
				Resources.get("compoundRelationDialogTitle"), Resources
						.get("compoundRelationNLabel"));
		dialog.setLocationRelativeTo(null);
		dialog.show();
		final int newN = dialog.getArity();

		// Skip altogether if no change.
		if (newN == n)
			return;

		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Do the work.
				if (newN <= 1) {
					// Uncompound the relation.
					if (dst != null)
						MartBuilderUtils.uncompoundRelation(dst, relation);
					else
						MartBuilderUtils.uncompoundRelation(ds, relation);
				} else {
					// Compound the relation.
					if (dst != null)
						MartBuilderUtils.compoundRelation(dst, relation, newN);
					else
						MartBuilderUtils.compoundRelation(ds, relation, newN);
				}
			}
		}, ds);
	}

	private int askUserForCompoundRelationIndex(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation) {
		// Is the relation compound? If not, return 0.
		if (!dataset.getSchemaModifications().isCompoundRelation(dsTable,
				relation))
			return 0;

		// Work out possible options.
		int maxIndex = dataset.getSchemaModifications().getCompoundRelation(
				dsTable, relation);
		final Integer[] options = new Integer[maxIndex];
		for (int i = 0; i < options.length; i++)
			options[i] = new Integer(i + 1);

		// Return -1 if cancelled.
		Integer selIndex = (Integer) JOptionPane.showInputDialog(null,
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
	 *            the schema relation to mask.
	 */
	public void requestRestrictRelation(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation) {
		// Get index offset into compound relation.
		final int index = dataset.getSchemaModifications().isCompoundRelation(
				dsTable, relation) ? this.askUserForCompoundRelationIndex(
				dataset, dsTable, relation) : 0;

		// Cancelled?
		if (index == -1)
			return;

		final RestrictedRelationDialog dialog = new RestrictedRelationDialog(
				relation, dataset.getSchemaModifications()
						.getRestrictedRelation(dsTable, relation, index));
		dialog.show();

		// Cancelled?
		if (dialog.getCancelled())
			return;

		// Get updated details from the user.
		final Map aliasesLHS = dialog.getLHSColumnAliases();
		final Map aliasesRHS = dialog.getRHSColumnAliases();
		final String expression = dialog.getExpression();
		final boolean hard = dialog.getHard();
		// Do this in the background.
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Update the restriction.
				if (dsTable != null)
					MartBuilderUtils.restrictRelation(dsTable, relation, index,
							expression, aliasesLHS, aliasesRHS, hard);
				else
					MartBuilderUtils.restrictRelation(dataset, relation, index,
							expression, aliasesLHS, aliasesRHS, hard);
			}
		}, dataset);
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
	 */
	public void requestUnrestrictRelation(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation) {
		// Get index offset into compound relation.
		final int index = dataset.getSchemaModifications().isCompoundRelation(
				dsTable, relation) ? this.askUserForCompoundRelationIndex(
				dataset, dsTable, relation) : 0;

		// Cancelled?
		if (index == -1)
			return;

		// Do this in the background.
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Remove the restriction.
				if (dsTable != null)
					MartBuilderUtils.unrestrictRelation(dsTable, relation,
							index);
				else
					MartBuilderUtils.unrestrictRelation(dataset, relation,
							index);
			}
		}, dataset);
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
	 */
	public void requestUnconcatRelation(final DataSet dataset,
			final DataSetTable dsTable, final Relation relation) {
		// Get index offset into compound relation.
		final int index = dataset.getSchemaModifications().isCompoundRelation(
				dsTable, relation) ? this.askUserForCompoundRelationIndex(
				dataset, dsTable, relation) : 0;

		// Cancelled?
		if (index == -1)
			return;

		// Do this in the background.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Remove the restriction.
				if (dsTable != null)
					MartBuilderUtils.unconcatRelation(dsTable, relation, index);
				else
					MartBuilderUtils.unconcatRelation(dataset, relation, index);
			}
		}, dataset);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask the relation.
				if (dst != null)
					MartBuilderUtils.maskRelation(dst, relation);
				else
					MartBuilderUtils.maskRelation(ds, relation);
			}
		}, ds);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask the relation.
				if (dst != null)
					MartBuilderUtils.forceRelation(dst, relation);
				else
					MartBuilderUtils.forceRelation(ds, relation);
			}
		}, ds);
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
	public void requestMaskTable(final DataSet ds, final DataSetTable dst,
			final Table table) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask all the relations on the table.
				if (dst != null)
					MartBuilderUtils.maskTable(dst, table);
				else
					MartBuilderUtils.maskTable(ds, table);
			}
		}, ds);
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
			final ExpressionColumn column) {
		final ExpressionColumnDialog dialog = new ExpressionColumnDialog(
				dsTable, column == null ? null : column.getDefinition(), column);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final boolean groupBy = dialog.getGroupBy();
		// Do this in the background.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Update the restriction.
				MartBuilderUtils.setExpressionColumn(dsTable,
						column == null ? null : column.getDefinition(),
						aliases, expression, groupBy);
			}
		}, (DataSet) dsTable.getSchema());
	}

	/**
	 * Asks for a expression column to be modified.
	 * 
	 * @param dsTable
	 *            the table to work with.
	 * @param column
	 *            the existing expression.
	 */
	public void requestRemoveExpressionColumn(final DataSetTable dsTable,
			final ExpressionColumn column) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Update the restriction.
				MartBuilderUtils.removeExpressionColumn(dsTable, column
						.getDefinition());
			}
		}, (DataSet) dsTable.getSchema());
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
				dataset.getSchemaModifications().getRestrictedTable(dsTable,
						table));
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final boolean hard = dialog.getHard();
		// Do this in the background.
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Update the restriction.
				if (dsTable != null)
					MartBuilderUtils.restrictTable(dsTable, table, expression,
							aliases, hard);
				else
					MartBuilderUtils.restrictTable(dataset, table, expression,
							aliases, hard);
			}
		}, dataset);
	}

	/**
	 * Requests that the dataset should be partitioned by the contents of the
	 * specified column. A dialog is put up asking the user how to partition
	 * this column. If it is already partitioned, the dialog will explain how
	 * and allow the user to change it. The user's input is then used to
	 * partition the column appropriately (or re-partition using the new
	 * settings if it was already partitioned).
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table to work with.
	 * @param column
	 *            the column to partition.
	 */
	public void requestPartitionByColumn(final DataSet dataset,
			final DataSetTable dsTable, final DataSetColumn column) {
		PartitionColumnDialog dialog;
		// If the column is already partitioned, open a dialog
		// explaining this and asking the user to edit the settings.
		if (dataset.getDataSetModifications().isPartitionedTable(dsTable))
			dialog = new PartitionColumnDialog(Resources
					.get("updatePartitionButton"),
					dataset.getDataSetModifications().getPartitionedColumnDef(
							dsTable), dsTable,
					column == null ? (DataSetColumn) dsTable
							.getColumnByName(dataset.getDataSetModifications()
									.getPartitionedColumnName(dsTable))
							: column);
		// Otherwise, open a dialog asking the user to define the partitioning
		// scheme.
		else
			dialog = new PartitionColumnDialog(Resources
					.get("createPartitionButton"), null, dsTable, column);
		// Check they didn't cancel the request.
		dialog.setLocationRelativeTo(null);
		dialog.show();
		final PartitionedColumnDefinition type = dialog.getPartitionType();
		final DataSetColumn partCol = dialog.getColumn();
		if (type == null)
			return;
		// Do the partitioning.
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Do the partitioning.
				MartBuilderUtils.partitionByColumn(dataset, partCol, type);
			}
		}, dataset);
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

		// Do it, but in the background.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Remove the dataset from the mart.
				final List datasets = new ArrayList(DataSetTabSet.this.martTab
						.getMart().getDataSets());
				for (final Iterator i = datasets.iterator(); i.hasNext();)
					MartBuilderUtils.removeDataSetFromMart(
							DataSetTabSet.this.martTab.getMart(), (DataSet) i
									.next());
				// Remove the tab.
				for (final Iterator i = datasets.iterator(); i.hasNext();)
					DataSetTabSet.this.removeDataSetTab((DataSet) i.next());
			}
		});
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

		// Do it, but in the background.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Remove the dataset from the mart.
				MartBuilderUtils.removeDataSetFromMart(
						DataSetTabSet.this.martTab.getMart(), dataset);
				// Remove the tab.
				DataSetTabSet.this.removeDataSetTab(dataset);
			}
		});
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
		// Do this in the background.
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Remove the restriction.
				if (dsTable != null)
					MartBuilderUtils.unrestrictTable(dsTable, table);
				else
					MartBuilderUtils.unrestrictTable(dataset, table);
			}
		}, dataset);
	}

	/**
	 * Renames a dataset, then renames the tab too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 */
	public void requestRenameDataSet(final DataSet dataset) {
		// Ask user for the new name.
		final String newName = this.askUserForName(Resources
				.get("requestDataSetName"), dataset.getName());

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName == null || newName.equals(dataset.getName()))
			return;

		// Work out which tab the dataset is in.
		final int idx = this.indexOfTab(dataset.getName());

		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Rename the dataset.
				MartBuilderUtils.renameDataSet(DataSetTabSet.this.martTab
						.getMart(), dataset, newName);

				// Rename the tab displaying it.
				DataSetTabSet.this.setTitleAt(idx, dataset.getName());
			}
		}, dataset);
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
		final String newName = this.askUserForName(Resources
				.get("requestDataSetColumnName"), dsColumn.getModifiedName());

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName == null || newName.equals(dsColumn.getModifiedName()))
			return;
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Rename the dataset.
				MartBuilderUtils.renameDataSetColumn(dsColumn, newName);
			}
		}, (DataSet) dsColumn.getTable().getSchema());

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
		final String newName = this.askUserForName(Resources
				.get("requestDataSetTableName"), dsTable.getModifiedName());

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName == null || newName.equals(dsTable.getModifiedName()))
			return;

		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Rename the dataset.
				MartBuilderUtils.renameDataSetTable(dsTable, newName);
			}
		}, (DataSet) dsTable.getSchema());
	}

	/**
	 * Asks user for a name to use, then creates an exact copy of the given
	 * dataset, giving the copy the name they chose. See also
	 * {@link DataSet#replicate(String)}.
	 * 
	 * @param dataset
	 *            the schema to dataset.
	 */
	public void requestReplicateDataSet(final DataSet dataset) {
		// Ask user for the name to use for the copy.
		final String newName = this.askUserForName(Resources
				.get("requestDataSetName"), dataset.getName());

		// No name entered? Or same name entered? Ignore the request.
		if (newName == null || newName.trim().length() == 0
				|| newName.equals(dataset.getName()))
			return;

		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Create the replicate.
				final DataSet newDataSet = MartBuilderUtils.replicateDataSet(
						DataSetTabSet.this.martTab.getMart(), dataset, newName);

				// Add a tab to represent the replicate.
				DataSetTabSet.this.addDataSetTab(newDataSet, true);
			}
		});
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

		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Subclass the relation.
				MartBuilderUtils.subclassRelation(ds, relation);
			}
		}, ds);
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
				this.martTab.getMart().getSchemas(), table);
		dialog.show();

		// If they cancelled it, return without doing anything.
		if (dialog.getSelectedTables().isEmpty())
			return;

		// In the background, suggest the datasets.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				Collection dss = null;
				try {
					// Suggest them.
					dss = MartBuilderUtils.suggestDataSets(
							DataSetTabSet.this.martTab.getMart(), dialog
									.getSelectedTables());
				} catch (final Throwable t) {
					throw t;
				} finally {
					// Must use a finally in case the dataset gets created
					// but won't sync.
					if (dss != null)
						// For each one suggested, add a dataset tab for
						// it.
						for (final Iterator i = dss.iterator(); i.hasNext();) {
							final DataSet dataset = (DataSet) i.next();
							DataSetTabSet.this.addDataSetTab(dataset, true);
						}
				}
			}
		});
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
		dialog.show();

		// If they cancelled it, return without doing anything.
		if (dialog.getSelectedColumns().isEmpty())
			return;

		// In the background, suggest the datasets.
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				Collection dss = null;
				try {
					// Suggest them.
					dss = MartBuilderUtils.suggestInvisibleDataSets(
							DataSetTabSet.this.martTab.getMart(), dataset,
							dialog.getSelectedColumns());
				} catch (final Throwable t) {
					throw t;
				} finally {
					if (dss != null) {
						// For each one suggested, add a dataset tab for
						// it.
						for (final Iterator i = dss.iterator(); i.hasNext();) {
							final DataSet dataset = (DataSet) i.next();
							DataSetTabSet.this.addDataSetTab(dataset, false);
						}
					}
				}
			}
		});
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Unmask the column.
				MartBuilderUtils.unmaskColumn(ds, column);
			}
		}, ds);
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
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Unmask the column.
				MartBuilderUtils.unindexColumn(ds, column);
			}
		}, ds);
	}

	/**
	 * Requests that a column be un-non-inherited.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to un-non-inherit.
	 */
	public void requestUnNonInheritColumn(final DataSet ds,
			final DataSetColumn column) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Unmask the column.
				MartBuilderUtils.unNonInheritColumn(ds, column);
			}
		}, ds);
	}

	/**
	 * Requests that all columns be un-non-inherited.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param table
	 *            the table to un-non-inherit all columns on.
	 */
	public void requestUnNonInheritAllColumns(final DataSet ds,
			final DataSetTable table) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				MartBuilderUtils.unNonInheritAllColumns(ds, table);
			}
		}, ds);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Unmasks the relation.
				if (dst != null)
					MartBuilderUtils.unforceRelation(dst, relation);
				else
					MartBuilderUtils.unforceRelation(ds, relation);
			}
		}, ds);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Unmasks the relation.
				if (dst != null)
					MartBuilderUtils.unmaskRelation(dst, relation);
				else
					MartBuilderUtils.unmaskRelation(ds, relation);
			}
		}, ds);
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
	public void requestUnmaskTable(final DataSet ds, final DataSetTable dst,
			final Table table) {
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Mask all the relations on the table.
				if (dst != null)
					MartBuilderUtils.unmaskTable(dst, table);
				else
					MartBuilderUtils.unmaskTable(ds, table);
			}
		}, ds);
	}

	/**
	 * Requests that partioning be turned off on the given column.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param dsTable
	 *            the table to work with.
	 */
	public void requestUnpartitionByColumn(final DataSet dataset,
			final DataSetTable dsTable) {
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Unpartition the column.
				MartBuilderUtils.unpartitionByColumn(dataset, dsTable);
			}
		}, dataset);
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
		this.runThenRecalculate(new Task() {
			public void run() throws Throwable {
				// Un-subclass the relation.
				MartBuilderUtils.unsubclassRelation(ds, relation);
			}
		}, ds);
	}

	/**
	 * Requests that the dataset be unindex optimised.
	 * 
	 * @param dataset
	 *            the dataset to make visible.
	 */
	public void requestNoIndexOptimiser(final DataSet dataset) {
		this.runOnly(new Task() {
			public void run() throws Throwable {
				// Do the visibility.
				MartBuilderUtils.noIndexOptimiserDataSet(dataset);
			}
		});
	}

	/**
	 * Requests that the dataset be index optimised.
	 * 
	 * @param dataset
	 *            the dataset to make visible.
	 */
	public void requestIndexOptimiser(final DataSet dataset) {
		this.runOnly(new Task() {
			public void run() throws Throwable {
				// Do the visibility.
				MartBuilderUtils.indexOptimiserDataSet(dataset);
			}
		});
	}

	/**
	 * Requests that the dataset be made visible.
	 * 
	 * @param dataset
	 *            the dataset to make visible.
	 */
	public void requestVisibleDataSet(final DataSet dataset) {
		this.runThenRepaint(new Task() {
			public void run() throws Throwable {
				// Do the visibility.
				MartBuilderUtils.visibleDataSet(dataset);
			}
		});
	}
}
