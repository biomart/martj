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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.AllDataSetsDiagram;
import org.biomart.builder.view.gui.diagrams.DataSetDiagram;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.SchemaDiagram;
import org.biomart.builder.view.gui.diagrams.contexts.AllDataSetsContext;
import org.biomart.builder.view.gui.diagrams.contexts.DataSetContext;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.builder.view.gui.diagrams.contexts.WindowContext;
import org.biomart.builder.view.gui.dialogs.ExplainDataSetDialog;
import org.biomart.builder.view.gui.dialogs.ExpressionColumnDialog;
import org.biomart.builder.view.gui.dialogs.PartitionColumnDialog;
import org.biomart.builder.view.gui.dialogs.RestrictedRelationDialog;
import org.biomart.builder.view.gui.dialogs.RestrictedTableDialog;
import org.biomart.builder.view.gui.dialogs.SaveDDLDialog;
import org.biomart.builder.view.gui.dialogs.SuggestDataSetDialog;
import org.biomart.builder.view.gui.dialogs.SuggestInvisibleDataSetDialog;

/**
 * This tabset contains most of the core functionality of the entire GUI. It has
 * one tab per dataset defined, plus an overview tab which displays an overview
 * of all the datasets in the mart. It handles all changes to any of the
 * datasets in the mart, and handles the assignment of {@link DiagramContext}s
 * to the various {@link Diagram}s inside it, including the schema tabset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.37, 2nd August 2006
 * @since 0.1
 */
public class DataSetTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	// Use double-list to prevent problems with hashcodes changing.
	private List[] datasetToTab = new List[] { new ArrayList(), new ArrayList() };

	private List currentExplanationDialogs = new ArrayList();

	private AllDataSetsDiagram allDataSetsDiagram;

	/**
	 * The constructor sets up a new set of tabs which represent all the
	 * datasets in the given mart, plus an overview tab to represent all the
	 * datasets in the mart.
	 * 
	 * @param martTab
	 *            the mart tab to represent the datasets for.
	 */
	public DataSetTabSet(MartTab martTab) {
		super();

		// Remember the settings.
		this.martTab = martTab;

		// Add the datasets overview tab. This tab displays a diagram
		// in which all datasets appear. This diagram could be quite large,
		// so it is held inside a scrollpane.
		this.allDataSetsDiagram = new AllDataSetsDiagram(this.martTab);
		this.allDataSetsDiagram.setDiagramContext(new AllDataSetsContext(
				martTab));
		JScrollPane scroller = new JScrollPane(this.allDataSetsDiagram);
		scroller.getViewport().setBackground(
				this.allDataSetsDiagram.getBackground());
		this.addTab(Resources.get("multiDataSetOverviewTab"), scroller);

		// Calculate the dataset tabs.
		this.recalculateDataSetTabs();

		// Update the overview diagram.
		this.recalculateOverviewDiagram();
	}

	/**
	 * Works out which tab is currently selected. If it is a dataset tab, and
	 * not the schema overview tab, then return the tab contents. Otherwise,
	 * return null.
	 * 
	 * @return the selected dataset tab contents, or null if none is currently
	 *         selected. Will also return null if the overview tab is selected.
	 */
	public DataSetTab getSelectedDataSetTab() {
		Object obj = this.getSelectedComponent();
		if (obj != null && (obj instanceof DataSetTab))
			return (DataSetTab) obj;
		else
			return null;
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

	public void setSelectedIndex(int selectedIndex) {
		// First of all, select the tab as normal. This must be
		// done in order to get the diagram visible. If we do
		// it later, having attempted to set up the diagram without
		// making it visible first, we get weird null pointer
		// exceptions from half-constructed
		// org.biomart.builder.view.gui.diagrams.
		super.setSelectedIndex(selectedIndex);

		// Then, work out which tab is currently being displayed.
		Component selectedComponent = this.getComponentAt(selectedIndex);
		if (selectedComponent instanceof DataSetTab) {
			// We are currently displaying a dataset tab. Display the
			// contents of the dataset tab, and attach the schema tabset
			// to the window part of the display. The schema tabset
			// will have an appropriate diagram context attached to it
			// by the dataset tab once it is attached.
			DataSetTab datasetTab = (DataSetTab) selectedComponent;
			datasetTab.attachSchemaTabSet();
		}
	}

	/**
	 * Recalculates the list of dataset tabs, so that they tally with the list
	 * of datasets currently within the mart. This removes any datasets from the
	 * tabs which are no longer in the mart, and adds new tabs for those which
	 * are in the mart but not yet displayed.
	 */
	public void recalculateDataSetTabs() {
		// Synchronise the datasets first.
		try {
			MartBuilderUtils.synchroniseMartDataSets(this.martTab.getMart());
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
			return;
		}

		// What datasets should we have?
		List martDataSets = new ArrayList(this.martTab.getMart().getDataSets());

		// Remove all the datasets in our tabs that are not in the mart.
		List ourDataSets = new ArrayList(datasetToTab[0]);
		for (Iterator i = ourDataSets.iterator(); i.hasNext();) {
			DataSet dataset = (DataSet) i.next();
			if (!martDataSets.contains(dataset))
				removeDataSetTab(dataset);
		}

		// Add all datasets that we don't have yet.
		for (Iterator i = martDataSets.iterator(); i.hasNext();) {
			DataSet dataset = (DataSet) i.next();
			if (!datasetToTab[0].contains(dataset))
				addDataSetTab(dataset);
		}
	}

	/**
	 * Works out which tab is displaying the given dataset, then asks the
	 * diagram in that tab to recalculate itself to match the current contents
	 * of the dataset.
	 * 
	 * @param dataset
	 *            the dataset to recalculate the diagram for.
	 */
	public void recalculateDataSetDiagram(DataSet dataset) {
		int index = this.datasetToTab[0].indexOf(dataset);
		DataSetTab datasetTab = (DataSetTab) this.datasetToTab[1].get(index);
		datasetTab.getDataSetDiagram().recalculateDiagram();
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
	public void repaintDataSetDiagram(DataSet dataset) {
		int index = this.datasetToTab[0].indexOf(dataset);
		DataSetTab datasetTab = (DataSetTab) this.datasetToTab[1].get(index);
		datasetTab.getDataSetDiagram().repaintDiagram();
	}

	/**
	 * Asks all dataset org.biomart.builder.view.gui.diagrams in all dataset
	 * tabs to recalculate themselves to match the current contents of the
	 * datasets.
	 */
	public void recalculateAllDataSetDiagrams() {
		for (Iterator i = this.datasetToTab[1].iterator(); i.hasNext();)
			((DataSetTab) i.next()).getDataSetDiagram().recalculateDiagram();
	}

	/**
	 * Asks all dataset org.biomart.builder.view.gui.diagrams in all dataset
	 * tabs to repaint themselves, in case any components have changed
	 * appearance. Do not use this if the components have changed size - use
	 * recalculate instead.
	 */
	public void repaintAllDataSetDiagrams() {
		for (Iterator i = this.datasetToTab[1].iterator(); i.hasNext();)
			((DataSetTab) i.next()).getDataSetDiagram().repaintDiagram();
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on the tab which
	 * represents all the datasets in the mart.
	 */
	public void repaintOverviewDiagram() {
		this.allDataSetsDiagram.repaintDiagram();
	}

	/**
	 * Causes {@link Diagram#recalculateDiagram()} to be called on the tab which
	 * represents all the datasets in the mart.
	 */
	public void recalculateOverviewDiagram() {
		this.allDataSetsDiagram.recalculateDiagram();
	}

	/**
	 * Causes {@link ExplainDataSetDialog#repaintDialog()} to be called on the
	 * currently visible explanation dialog, if any.
	 */
	public void repaintExplanationDialog() {
		for (Iterator i = this.currentExplanationDialogs.iterator(); i
				.hasNext();)
			((ExplainDataSetDialog) i.next()).repaintDialog();
	}

	/**
	 * Causes {@link ExplainDataSetDialog#recalculateDialog()} to be called on
	 * the currently visible explanation dialog, if any.
	 */
	public void recalculateExplanationDialog() {
		for (Iterator i = this.currentExplanationDialogs.iterator(); i
				.hasNext();)
			((ExplainDataSetDialog) i.next()).recalculateDialog();
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
		int choice = JOptionPane.showConfirmDialog(this, Resources
				.get("confirmDelDataset"), Resources.get("questionTitle"),
				JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// Do it, but in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the dataset from the mart.
					MartBuilderUtils.removeDataSetFromMart(martTab.getMart(),
							dataset);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Remove the tab.
							removeDataSetTab(dataset);

							// Update the overview diagram.
							recalculateOverviewDiagram();

							// Set our modified status to true.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks the user if they are sure they want to remove all datasets, then
	 * removes it from the mart (and the tabs) if they agree.
	 */
	public void requestRemoveAllDataSets() {
		// Confirm the decision first.
		int choice = JOptionPane.showConfirmDialog(this, Resources
				.get("confirmDelAllDatasets"), Resources.get("questionTitle"),
				JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// Do it, but in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the dataset from the mart.
					final List datasets = new ArrayList(martTab.getMart()
							.getDataSets());
					for (Iterator i = datasets.iterator(); i.hasNext();)
						MartBuilderUtils.removeDataSetFromMart(martTab
								.getMart(), (DataSet) i.next());

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Remove the tab.
							for (Iterator i = datasets.iterator(); i.hasNext();)
								removeDataSetTab((DataSet) i.next());

							// Update the overview diagram.
							recalculateOverviewDiagram();

							// Set our modified status to true.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	private void removeDataSetTab(DataSet dataset) {
		// Work out the currently selected tab.
		int currentTab = this.getSelectedIndex();

		// Work out which tab the dataset lives in.
		int index = this.datasetToTab[0].indexOf(dataset);
		DataSetTab datasetTab = (DataSetTab) this.datasetToTab[1].get(index);

		// Work out the tab index.
		int tabIndex = this.indexOfComponent(datasetTab);

		// Remove the tab, and it's mapping from the dataset-to-tab map.
		this.remove(datasetTab);
		this.datasetToTab[0].remove(index);
		this.datasetToTab[1].remove(index);

		// Update the overview diagram.
		this.recalculateOverviewDiagram();

		// Fake a click on the last tab before this one to ensure
		// at least one tab remains visible and up-to-date.
		this.setSelectedIndex(currentTab == 0 ? 0 : Math.max(tabIndex - 1, 0));
	}

	private void addDataSetTab(DataSet dataset) {
		// Create a tab for the dataset.
		DataSetTab datasetTab = new DataSetTab(this.martTab, dataset);

		// Add the tab, and remember the mapping between dataset and tab.
		this.addTab(dataset.getName(), datasetTab);
		this.datasetToTab[0].add(dataset);
		this.datasetToTab[1].add(datasetTab);

		// Update the overview diagram.
		this.recalculateOverviewDiagram();

		// Fake a click on the new tab.
		this.setSelectedIndex(this.indexOfComponent(datasetTab));
		this.martTab.selectDataSetEditor();
	}

	private String askUserForName(String message, String defaultResponse) {
		// Ask the user for a name. Use the default response
		// as the default value in the input field.
		String name = (String) JOptionPane.showInputDialog(this.martTab
				.getMartTabSet().getMartBuilder(), message, Resources
				.get("questionTitle"), JOptionPane.QUESTION_MESSAGE, null,
				null, defaultResponse);

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

	/**
	 * Renames a dataset, then renames the tab too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 */
	public void requestRenameDataSet(DataSet dataset) {
		// Ask user for the new name.
		String newName = this.askUserForName(Resources
				.get("requestDataSetName"), dataset.getName());

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName == null || newName.equals(dataset.getName()))
			return;

		// Work out which tab the dataset is in.
		int idx = this.indexOfTab(dataset.getName());

		// Rename the dataset.
		MartBuilderUtils
				.renameDataSet(this.martTab.getMart(), dataset, newName);

		// Rename the tab displaying it.
		this.setTitleAt(idx, dataset.getName());

		// Update the overview diagram. (Recalc not repaint
		// as the name will have changed the size of components).
		this.recalculateOverviewDiagram();

		// And update the explanation diagram.
		this.recalculateExplanationDialog();

		// Set the tabset as modified.
		this.martTab.getMartTabSet().setModifiedStatus(true);
	}

	/**
	 * Asks user for a name to use, then creates an exact copy of the given
	 * dataset, giving the copy the name they chose.
	 * 
	 * @param dataset
	 *            the schema to dataset.
	 */
	public void requestReplicateDataSet(DataSet dataset) {
		try {
			// Ask user for the name to use for the copy.
			String newName = this.askUserForName(Resources
					.get("requestDataSetName"), dataset.getName());

			// No name entered? Or same name entered? Ignore the request.
			if (newName == null || newName.trim().length() == 0
					|| newName.equals(dataset.getName()))
				return;

			// Create the replicate.
			DataSet newDataSet = MartBuilderUtils.replicateDataSet(this.martTab
					.getMart(), dataset, newName);

			// Add a tab to represent the replicate.
			this.addDataSetTab(newDataSet);

			// Update the overview diagram.
			recalculateOverviewDiagram();

			// Set the dataset tabset status as modified.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Renames a column, after prompting the user to enter a new name. By
	 * default, the existing name is used. If the name entered is blank or
	 * matches the existing name, no change is made.
	 * 
	 * @param dsColumn
	 *            the column to rename.
	 */
	public void requestRenameDataSetColumn(DataSetColumn dsColumn) {
		// Ask user for the new name.
		String newName = this.askUserForName(Resources
				.get("requestDataSetColumnName"), dsColumn.getName());

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName == null || newName.equals(dsColumn.getName()))
			return;

		// Rename the dataset.
		MartBuilderUtils.renameDataSetColumn(dsColumn, newName);

		// Recalculate the dataset diagram as the column name will have
		// caused the column and the table to resize themselves.
		this.recalculateDataSetDiagram((DataSet) dsColumn.getTable()
				.getSchema());

		// Recalc the explanation too.
		this.recalculateExplanationDialog();

		// Set the tabset as modified.
		this.martTab.getMartTabSet().setModifiedStatus(true);
	}

	/**
	 * Renames a table, after prompting the user to enter a new name. By
	 * default, the existing name is used. If the name entered is blank or
	 * matches the existing name, no change is made.
	 * 
	 * @param dsTable
	 *            the table to rename.
	 */
	public void requestRenameDataSetTable(DataSetTable dsTable) {
		// Ask user for the new name.
		String newName = this.askUserForName(Resources
				.get("requestDataSetTableName"), dsTable.getName());

		// If the new name is null (user cancelled), or has
		// not changed, don't rename it.
		if (newName == null || newName.equals(dsTable.getName()))
			return;

		// Rename the dataset.
		MartBuilderUtils.renameDataSetTable(dsTable, newName);

		// Recalculate the dataset diagram as the table name will have
		// caused the table to resize itself.
		this.recalculateDataSetDiagram((DataSet) dsTable.getSchema());

		// Recalc the explanation diagram too.
		this.recalculateExplanationDialog();

		// Set the tabset as modified.
		this.martTab.getMartTabSet().setModifiedStatus(true);
	}

	/**
	 * Given a table, suggest a series of synchronised datasets that may be
	 * possible for that table.
	 * 
	 * @param table
	 *            the table to suggest datasets for. If null, no default table
	 *            is used.
	 */
	public void requestSuggestDataSets(final Table table) {
		// Ask the user what tables they want to work with and what
		// mode they want.
		final SuggestDataSetDialog dialog = new SuggestDataSetDialog(martTab,
				table);
		dialog.show();

		// If they cancelled it, return without doing anything.
		if (dialog.getSelectedTables().isEmpty())
			return;

		// In the background, suggest the datasets.
		LongProcess.run(new Runnable() {
			public void run() {
				Collection dss = null;
				try {
					// Suggest them.
					dss = MartBuilderUtils.suggestDataSets(martTab.getMart(),
							dialog.getSelectedTables());
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				} finally {
					// Must use a finally in case the dataset gets created
					// but won't sync.
					final Collection dssRef = dss;
					if (dssRef != null)
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// For each one suggested, add a dataset tab for
								// it.
								for (Iterator i = dssRef.iterator(); i
										.hasNext();) {
									DataSet dataset = (DataSet) i.next();
									addDataSetTab(dataset);
								}

								// Update the overview diagram.
								recalculateOverviewDiagram();

								// Update the modified status for this tabset.
								martTab.getMartTabSet().setModifiedStatus(true);
							}
						});
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
				martTab, table);
		dialog.show();

		// If they cancelled it, return without doing anything.
		if (dialog.getSelectedColumns().isEmpty())
			return;

		// In the background, suggest the datasets.
		LongProcess.run(new Runnable() {
			public void run() {
				Collection dss = null;
				try {
					// Suggest them.
					dss = MartBuilderUtils.suggestInvisibleDataSets(martTab
							.getMart(), dataset, dialog.getSelectedColumns());
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				} finally {
					// Must use a finally in case the dataset gets created
					// but won't sync.
					final Collection dssRef = dss;
					if (dssRef != null)
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// For each one suggested, add a dataset tab for
								// it.
								for (Iterator i = dssRef.iterator(); i
										.hasNext();) {
									DataSet dataset = (DataSet) i.next();
									addDataSetTab(dataset);
								}

								// Update the overview diagram.
								recalculateOverviewDiagram();

								// Update the modified status for this tabset.
								martTab.getMartTabSet().setModifiedStatus(true);
							}
						});
				}
			}
		});
	}

	/**
	 * Asks for an expression column to be added to the table.
	 * 
	 * @param table
	 *            the table to add the expression column to.
	 */
	public void requestAddExpressionColumn(final DataSetTable table) {
		ExpressionColumnDialog dialog = new ExpressionColumnDialog(
				this.martTab, table, null);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get the details.
		final String columnName = dialog.getColumnName();
		final Map columnAliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final boolean groupBy = dialog.getGroupBy();
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Add the column.
					MartBuilderUtils.addExpressionColumn(table, columnName,
							columnAliases, expression, groupBy);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the dataset diagram based on the
							// newly modified dataset.
							recalculateDataSetDiagram((DataSet) table
									.getSchema());

							// Recalculate the explanation diagram too.
							recalculateExplanationDialog();

							// Update the modified status for the tabset.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for an expression column to be modified.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param column
	 *            the expression column to modify.
	 */
	public void requestModifyExpressionColumn(final ExpressionColumn column) {
		ExpressionColumnDialog dialog = new ExpressionColumnDialog(
				this.martTab, (DataSetTable) column.getTable(), column);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map columnAliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		final boolean groupBy = dialog.getGroupBy();
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Modify the column.
					MartBuilderUtils.modifyExpressionColumn(column,
							columnAliases, expression, groupBy);

					// Update the modified status for the tabset.
					martTab.getMartTabSet().setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for an expression column to be removed from the table.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param column
	 *            the expression column to remove.
	 */
	public void requestRemoveExpressionColumn(final ExpressionColumn column) {
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the column.
					MartBuilderUtils.removeExpressionColumn(column);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the dataset diagram based on the
							// newly modified dataset.
							recalculateDataSetDiagram((DataSet) column
									.getTable().getSchema());

							// Recalculate the explanation diagram too.
							recalculateExplanationDialog();

							// Update the modified status for the tabset.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks that all relations on a table be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param table
	 *            the schema table to mask all relations for.
	 */
	public void requestMaskTable(DataSet ds, Table table) {
		try {
			// Mask all the relations on the table.
			MartBuilderUtils.maskTable(ds, table);

			// Some of the relations are internal, and some are
			// external, so we must repaint both the schema diagram
			// and the all-schemas diagram.
			this.martTab.getSchemaTabSet().repaintSchemaDiagram(
					table.getSchema());
			this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed table.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Asks that a relation be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the schema relation to mask.
	 */
	public void requestMaskRelation(DataSet ds, Relation relation) {
		try {
			// Mask the relation.
			MartBuilderUtils.maskRelation(ds, relation);

			// If it is an internal relation, repaint the schema diagram.
			if (!relation.isExternal())
				this.martTab.getSchemaTabSet().repaintSchemaDiagram(
						relation.getFirstKey().getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed relation.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Asks that a relation be unmasked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the schema relation to unmask.
	 */
	public void requestUnmaskRelation(DataSet ds, Relation relation) {
		try {
			// Unmasks the relation.
			MartBuilderUtils.unmaskRelation(ds, relation);

			// If it is an internal relation, repaint the schema diagram.
			if (!relation.isExternal())
				this.martTab.getSchemaTabSet().repaintSchemaDiagram(
						relation.getFirstKey().getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed relation.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Requests that a relation be flagged as a subclass relation.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to subclass.
	 */
	public void requestSubclassRelation(DataSet ds, Relation relation) {
		try {
			// Subclass the relation.
			MartBuilderUtils.subclassRelation(ds, relation);

			// If it is an internal relation, repaint the schema diagram.
			if (!relation.isExternal())
				this.martTab.getSchemaTabSet().repaintSchemaDiagram(
						relation.getFirstKey().getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed relation.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Requests that the subclass flag be removed from a relation.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to un-subclass.
	 */
	public void requestUnsubclassRelation(DataSet ds, Relation relation) {
		try {
			// Un-subclass the relation.
			MartBuilderUtils.unsubclassRelation(ds, relation);

			// If it is an internal relation, repaint the schema diagram.
			if (!relation.isExternal())
				this.martTab.getSchemaTabSet().repaintSchemaDiagram(
						relation.getFirstKey().getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed relation.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Asks for a table restriction to be added to the dataset.
	 * 
	 * @param dataset
	 *            the dataset we are dealing with.
	 * @param table
	 *            the table to add a restriction to.
	 */
	public void requestAddTableRestriction(final DataSet dataset,
			final Table table) {
		RestrictedTableDialog dialog = new RestrictedTableDialog(this.martTab,
				table, null);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get the details.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Add the restriction.
					MartBuilderUtils.restrictTable(dataset, table, expression,
							aliases);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Repaint the schema diagram.
							martTab.getSchemaTabSet().repaintSchemaDiagram(
									table.getSchema());

							// Repaint the overview diagram too.
							martTab.getSchemaTabSet().repaintOverviewDiagram();

							// Update the explanation diagram so that it
							// correctly reflects the changed table.
							repaintExplanationDialog();

							// Update the modified status for the tabset.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for a table restriction to be modified.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param table
	 *            the table to modify the restriction for.
	 * @param restriction
	 *            the existing restriction.
	 */
	public void requestModifyTableRestriction(final DataSet dataset,
			final Table table, DataSetTableRestriction restriction) {
		RestrictedTableDialog dialog = new RestrictedTableDialog(this.martTab,
				table, restriction);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map aliases = dialog.getColumnAliases();
		final String expression = dialog.getExpression();
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Update the restriction.
					MartBuilderUtils.restrictTable(dataset, table, expression,
							aliases);

					// Update the modified status for the tabset.
					martTab.getMartTabSet().setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for a table restriction to be removed.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param table
	 *            the table to unrestrict.
	 */
	public void requestRemoveTableRestriction(final DataSet dataset,
			final Table table) {
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the restriction.
					MartBuilderUtils.unrestrictTable(dataset, table);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Repaint the schema diagram.
							martTab.getSchemaTabSet().repaintSchemaDiagram(
									table.getSchema());

							// Repaint the overview diagram too.
							martTab.getSchemaTabSet().repaintOverviewDiagram();

							// Update the explanation diagram so that it
							// correctly reflects the changed table.
							repaintExplanationDialog();

							// Update the modified status for the tabset.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for a relation restriction to be added to the dataset.
	 * 
	 * @param dataset
	 *            the dataset we are dealing with.
	 * @param relation
	 *            the relation to add a restriction to.
	 */
	public void requestAddRelationRestriction(final DataSet dataset,
			final Relation relation) {
		RestrictedRelationDialog dialog = new RestrictedRelationDialog(
				this.martTab, relation, null);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get the details.
		final Map firstColumnAliases = dialog.getFirstColumnAliases();
		final Map secondColumnAliases = dialog.getSecondColumnAliases();
		final String expression = dialog.getExpression();
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Add the restriction.
					MartBuilderUtils
							.restrictRelation(dataset, relation, expression,
									firstColumnAliases, secondColumnAliases);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// If it is an internal relation, repaint the schema
							// diagram.
							if (!relation.isExternal())
								martTab.getSchemaTabSet().repaintSchemaDiagram(
										relation.getFirstKey().getTable()
												.getSchema());

							// Otherwise, it is external, so repaint the schema
							// overview diagram.
							else
								martTab.getSchemaTabSet()
										.repaintOverviewDiagram();

							// Update the explanation diagram so that it
							// correctly reflects the changed relation.
							repaintExplanationDialog();

							// Update the modified status for the tabset.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for a relation restriction to be modified.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to modify the restriction for.
	 * @param restriction
	 *            the existing restriction.
	 */
	public void requestModifyRelationRestriction(final DataSet dataset,
			final Relation relation, DataSetRelationRestriction restriction) {
		RestrictedRelationDialog dialog = new RestrictedRelationDialog(
				this.martTab, relation, restriction);
		dialog.show();
		// Cancelled?
		if (dialog.getCancelled())
			return;
		// Get updated details from the user.
		final Map firstColumnAliases = dialog.getFirstColumnAliases();
		final Map secondColumnAliases = dialog.getSecondColumnAliases();
		final String expression = dialog.getExpression();
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Update the restriction.
					MartBuilderUtils
							.restrictRelation(dataset, relation, expression,
									firstColumnAliases, secondColumnAliases);

					// Update the modified status for the tabset.
					martTab.getMartTabSet().setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks for a relation restriction to be removed.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to unrestrict.
	 */
	public void requestRemoveRelationRestriction(final DataSet dataset,
			final Relation relation) {
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the restriction.
					MartBuilderUtils.unrestrictRelation(dataset, relation);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// If it is an internal relation, repaint the schema
							// diagram.
							if (!relation.isExternal())
								martTab.getSchemaTabSet().repaintSchemaDiagram(
										relation.getFirstKey().getTable()
												.getSchema());

							// Otherwise, it is external, so repaint the schema
							// overview diagram.
							else
								martTab.getSchemaTabSet()
										.repaintOverviewDiagram();

							// Update the explanation diagram so that it
							// correctly reflects the changed relation.
							repaintExplanationDialog();

							// Update the modified status for the tabset.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Request that a relation be marked as concat-only.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to mark.
	 * @param type
	 *            the type of concatenation to use on this relation.
	 */
	public void requestConcatOnlyRelation(DataSet ds, Relation relation,
			ConcatRelationType type) {
		try {
			// Mark the relation concat-only.
			MartBuilderUtils.concatOnlyRelation(ds, relation, type);

			// If it is an internal relation, repaint the schema diagram.
			if (!relation.isExternal())
				this.martTab.getSchemaTabSet().repaintSchemaDiagram(
						relation.getFirstKey().getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed relation.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Requests that a relation have it's concat-only flag removed.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param relation
	 *            the relation to un-concat-only.
	 */
	public void requestUnconcatOnlyRelation(DataSet ds, Relation relation) {
		try {
			// Remove the concat-only flag.
			MartBuilderUtils.unconcatOnlyRelation(ds, relation);

			// If it is an internal relation, repaint the schema diagram.
			if (!relation.isExternal())
				this.martTab.getSchemaTabSet().repaintSchemaDiagram(
						relation.getFirstKey().getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.martTab.getSchemaTabSet().repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it
			// correctly reflects the changed relation.
			this.recalculateExplanationDialog();

			// Update the modified status.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Requests that a column be masked.
	 * 
	 * @param ds
	 *            the dataset we are working with.
	 * @param column
	 *            the column to mask.
	 */
	public void requestMaskColumn(DataSet ds, DataSetColumn column) {
		try {
			// Mask the column.
			MartBuilderUtils.maskColumn(ds, column);

			// Recalculate the dataset diagram to reflect the changes.
			this.recalculateDataSetDiagram(ds);

			// Recalculate the overview too.
			this.recalculateExplanationDialog();

			// Update the modification status for this tabset.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
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
	public void requestUnmaskColumn(DataSet ds, DataSetColumn column) {
		try {
			// Unmask the column.
			MartBuilderUtils.unmaskColumn(ds, column);

			// Recalculate the dataset diagram to reflect the changes.
			this.repaintDataSetDiagram(ds);

			// Update the explanation too.
			this.repaintExplanationDialog();

			// Update the modification status for this tabset.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Ask that an explanation dialog be opened that explains how the given
	 * dataset table was constructed.
	 * 
	 * @param dsTable
	 *            the dataset table that needs to be explained.
	 */
	public void requestExplainTable(DataSetTable dsTable) {
		try {
			// Open the dialog. The dialog will set a flag in this instance
			// that contains a reference to its diagram, so that the diagram
			// can be updated as the user edits the dataset.
			ExplainDataSetDialog.showTableExplanation(this.martTab, dsTable);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * This method is called by the {@link ExplainDataSetDialog} when it is
	 * opened. This dialog is then updated whenever the dataset window is
	 * updated.
	 * 
	 * @param dialog
	 *            the dialog being displayed by a current
	 *            {@link ExplainDataSetDialog}.
	 */
	public void addCurrentExplanationDialog(ExplainDataSetDialog dialog) {
		this.currentExplanationDialogs.add(dialog);
	}

	/**
	 * This method is called by the {@link ExplainDataSetDialog} when it is
	 * closed.
	 * 
	 * @param dialog
	 *            the dialog being closed by a current
	 *            {@link ExplainDataSetDialog}.
	 */
	public void removeCurrentExplanationDialog(ExplainDataSetDialog dialog) {
		this.currentExplanationDialogs.remove(dialog);
	}

	/**
	 * Requests that the dataset be made invisible.
	 * 
	 * @param dataset
	 *            the dataset to make invisible.
	 */
	public void requestInvisibleDataSet(DataSet dataset) {
		// Do the invisibility.
		MartBuilderUtils.invisibleDataSet(dataset);

		// Repaint the dataset overview diagram.
		this.repaintOverviewDiagram();

		// Update the modified status.
		this.martTab.getMartTabSet().setModifiedStatus(true);
	}

	/**
	 * Requests that the dataset be made visible.
	 * 
	 * @param dataset
	 *            the dataset to make visible.
	 */
	public void requestVisibleDataSet(DataSet dataset) {
		// Do the visibility.
		MartBuilderUtils.visibleDataSet(dataset);

		// Repaint the dataset overview diagram.
		this.repaintOverviewDiagram();

		// Update the modified status.
		this.martTab.getMartTabSet().setModifiedStatus(true);
	}

	/**
	 * Requests that the dataset should be partitioned by the contents of the
	 * specified column. A dialog is put up asking the user how to partition
	 * this column. If it is already partitioned, the dialog will explain how
	 * and allow the user to change it. The users input is then used to
	 * partition the column appropriately (or re-partition using the new
	 * settings if it was already partitioned).
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param column
	 *            the column to partition.
	 */
	public void requestPartitionByColumn(DataSet dataset, DataSetColumn column) {
		PartitionedColumnType type;

		// If the column is already partitioned, open a dialog
		// explaining this and asking the user to edit the settings.
		if (dataset.getPartitionedDataSetColumns().contains(column)) {
			PartitionedColumnType oldType = dataset
					.getPartitionedDataSetColumnType(column);
			type = PartitionColumnDialog.updatePartitionedColumnType(
					this.martTab, oldType);

			// Check they didn't cancel the request, or left the
			// scheme unchanged.
			if (type == null || type.equals(oldType))
				return;
		}

		// Otherwise, open a dialog asking the user to define the partitioning
		// scheme.
		else {
			type = PartitionColumnDialog
					.createPartitionedColumnType(this.martTab);

			// Check they didn't cancel the request.
			if (type == null)
				return;
		}

		// Do the partitioning.
		this.requestPartitionByColumn(dataset, column, type);
	}

	/**
	 * Requests that a column be partitioned using the given partitioning
	 * scheme.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param column
	 *            the column to partition.
	 * @param type
	 *            how to partition it.
	 */
	public void requestPartitionByColumn(DataSet dataset, DataSetColumn column,
			PartitionedColumnType type) {
		try {
			// Do the partitioning.
			MartBuilderUtils.partitionByColumn(dataset, column, type);

			// Repaint the dataset, as the partitioned column
			// will have changed colour.
			this.repaintDataSetDiagram(dataset);

			// Repaint the explanation, too.
			this.repaintExplanationDialog();

			// Update the modified status on this tabset.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Requests that partioning be turned off on the given column.
	 * 
	 * @param dataset
	 *            the dataset we are working with.
	 * @param column
	 *            the column to turn partioning off for.
	 */
	public void requestUnpartitionByColumn(DataSet dataset, DataSetColumn column) {
		try {
			// Unpartition the column.
			MartBuilderUtils.unpartitionByColumn(dataset, column);

			// Repaint the dataset, as the partitioned column
			// will have changed colour.
			this.repaintDataSetDiagram(dataset);

			// Repaint the explanation too.
			this.repaintExplanationDialog();

			// Update the modified status on this tabset.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
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
	public void requestChangeOptimiserType(DataSet dataset,
			DataSetOptimiserType type) {
		// Change the type.
		MartBuilderUtils.changeOptimiserType(dataset, type);

		// Update the modified status on this tabset.
		this.martTab.getMartTabSet().setModifiedStatus(true);
	}

	/**
	 * On a request to create DDL for the current dataset, open the DDL creation
	 * window with all this dataset selected.
	 * 
	 * @param dataset
	 *            the dataset to show the dialog for.
	 */
	public void requestCreateDDL(DataSet dataset) {
		// Open the DDL creation dialog and let it do it's stuff.
		(new SaveDDLDialog(this.martTab, Collections.singleton(dataset)))
				.show();
	}

	private JPopupMenu getDataSetTabContextMenu(final DataSet dataset) {
		// Start with an empty menu.
		JPopupMenu contextMenu = new JPopupMenu();

		// This item allows the user to remove the dataset from the mart.
		JMenuItem close = new JMenuItem(Resources.get("removeDataSetTitle"));
		close.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				requestRemoveDataSet(dataset);
			}
		});
		contextMenu.add(close);

		// This item allows the user to rename the dataset.
		JMenuItem rename = new JMenuItem(Resources.get("renameDataSetTitle"));
		rename.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				requestRenameDataSet(dataset);
			}
		});
		contextMenu.add(rename);

		// Return the menu.
		return contextMenu;
	}

	protected void processMouseEvent(MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.isPopupTrigger()) {
			// Where was the click?
			int selectedIndex = this.indexAtLocation(evt.getX(), evt.getY());
			if (selectedIndex >= 0) {
				Component selectedComponent = this
						.getComponentAt(selectedIndex);
				// Was it a dataset tab?
				if (selectedComponent instanceof DataSetTab) {
					// If so, select the tab first.
					this.setSelectedIndex(selectedIndex);
					// Then, work out which dataset it is displaying.
					DataSet dataset = ((DataSetTab) selectedComponent)
							.getDataSet();
					// Then, construct and show a tab context menu for that
					// dataset.
					this.getDataSetTabContextMenu(dataset).show(this,
							evt.getX(), evt.getY());
					// Mark the event as processed.
					eventProcessed = true;
				}
			}
		}
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	public class DataSetTab extends JPanel {
		private static final long serialVersionUID = 1;

		private DataSet dataset;

		private MartTab martTab;

		private JPanel displayArea;

		private JRadioButton windowButton;

		private JRadioButton datasetButton;

		private SchemaDiagram datasetDiagram;

		/**
		 * The constructor constructs a new tab containing two parts. The first
		 * part is a window diagram, which views the schema tabset through a
		 * {@link WindowContext}. The second part is the dataset diagram, which
		 * is viewed through a {@link DataSetContext}. The two are shown
		 * alternately in the panel, depending on which button the user selects.
		 * 
		 * @param martTab
		 *            the mart tab this tab belongs in.
		 * @param dataset
		 *            the dataset this tab will display.
		 */
		public DataSetTab(MartTab martTab, DataSet dataset) {
			// Set up our layout.
			super(new BorderLayout());

			// Remember which dataset and tabset we are working with.
			this.dataset = dataset;
			this.martTab = martTab;

			// Create display part of the tab. The display area consists of
			// two cards - one for the window, one for the dataset. Buttons
			// in another area switch between the cards.
			this.displayArea = new JPanel(new CardLayout());

			// Dataset card first. Create a diagram, then place it inside
			// a scrollpane. This scrollpane becomes the dataset card. Don't
			// forget to set the context too.
			this.datasetDiagram = new DataSetDiagram(martTab, dataset);
			this.datasetDiagram.setDiagramContext(new DataSetContext(
					this.martTab, dataset));
			JScrollPane scroller = new JScrollPane(this.datasetDiagram);
			scroller.getViewport().setBackground(
					this.datasetDiagram.getBackground());
			displayArea.add(scroller, "DATASET_CARD");

			// Create panel which contains the buttons.
			JPanel buttonsPanel = new JPanel();

			// Create the button that selects the dataset card.
			this.datasetButton = new JRadioButton(Resources
					.get("datasetButtonName"));
			this.datasetButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == datasetButton) {
						CardLayout cards = (CardLayout) displayArea.getLayout();
						cards.show(displayArea, "DATASET_CARD");
					}
				}
			});

			// Create the button that selects the window card.
			this.windowButton = new JRadioButton(Resources
					.get("windowButtonName"));
			windowButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == windowButton) {
						CardLayout cards = (CardLayout) displayArea.getLayout();
						cards.show(displayArea, "WINDOW_CARD");
					}
				}
			});

			// Add the card buttons to the panel.
			buttonsPanel.add(windowButton);
			buttonsPanel.add(this.datasetButton);

			// Make buttons mutually exclusive.
			ButtonGroup buttons = new ButtonGroup();
			buttons.add(this.windowButton);
			buttons.add(datasetButton);

			// Add the buttons panel, and the display area containing the cards,
			// to the panel.
			this.add(buttonsPanel, BorderLayout.NORTH);
			this.add(displayArea, BorderLayout.CENTER);

			// Set our preferred size to the dataset diagram size plus a bit on
			// top for the buttons panel.
			Dimension preferredSize = datasetDiagram.getPreferredSize();
			double extraHeight = datasetButton.getHeight();
			preferredSize.setSize(preferredSize.getWidth(), preferredSize
					.getHeight()
					+ extraHeight);
			this.setPreferredSize(preferredSize);

			// Select the default button (which shows the dataset card).
			// We must physically click on it to make the card show.
			datasetButton.doClick();
		}

		/**
		 * Returns the dataset that this tab is displaying.
		 * 
		 * @return the displayed dataset.
		 */
		public DataSet getDataSet() {
			return this.dataset;
		}

		/**
		 * Attaches the schema tabset to this dataset. Attaching means that the
		 * window card is created, and set to contain the schema tabset. The
		 * schema tabset is then asked to use the {@link WindowContext} to
		 * display its org.biomart.builder.view.gui.diagrams, instead of
		 * whatever context it was using before. Be warned - once the schema
		 * tabset is attached here, it will disappear from wherever it was
		 * before, because of the way that JComponents can only have one parent.
		 */
		public void attachSchemaTabSet() {
			// Set the context on the schema tabset, and set the
			// tabset to be the window card.
			WindowContext context = new WindowContext(this.martTab,
					this.dataset);
			this.martTab.getSchemaTabSet().setDiagramContext(context);
			this.displayArea.add(this.martTab.getSchemaTabSet(), "WINDOW_CARD");

			// Nasty hack to force schema tabset to display correctly, by
			// simulating a click on the window button.
			if (this.windowButton.isSelected())
				this.windowButton.doClick();
		}

		/**
		 * Obtain the diagram for the dataset currently being displayed.
		 * 
		 * @return the dataset diagram currently being displayed.
		 */
		public Diagram getDataSetDiagram() {
			return this.datasetDiagram;
		}
	}
}
