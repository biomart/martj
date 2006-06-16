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
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructor.ConstructorRunnable;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This tabset contains most of the core functionality of the entire GUI. It has
 * one tab per dataset defined, plus an overview tab which displays an overview
 * of all the schemas in the mart. It handles all changes to any of the datasets
 * in the mart, and handles the assignment of {@link DiagramContext}s to the
 * various {@link Diagram}s inside it, including the schema tabset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.22, 16th June 2006
 * @since 0.1
 */
public class DataSetTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private SchemaTabSet schemaTabSet;

	// Use double-list to prevent problems with hashcodes changing.
	private List[] datasetToTab = new List[] { new ArrayList(), new ArrayList() };

	private MartTabSet martTabSet;

	private Mart mart;

	private Diagram currentExplanationDiagram;

	/**
	 * The constructor sets up a new set of tabs which represent all the
	 * datasets in the given mart, plus an overview tab to represent all the
	 * schemas in the mart.
	 * 
	 * @param martTabSet
	 *            the mart tabset to which this dataset tabset belongs.
	 * @param mart
	 *            the mart to represent the datasets for.
	 */
	public DataSetTabSet(MartTabSet martTabSet, Mart mart) {
		super();

		// Remember the settings.
		this.martTabSet = martTabSet;
		this.mart = mart;

		// Create a tabset to contain all the schemas in the mart.
		this.schemaTabSet = new SchemaTabSet(this);

		// Set up a dummy placeholder tab which will contain the
		// schema overview tab. It contains a JLabel because that's the
		// simplest thing there is. Each time the tab is selected,
		// the schema tabset will be assigned to it and given an
		// appropriate DiagramContext. When a dataset tab is selected,
		// the schema tabset will move into the window part of the dataset
		// tab, and be given a different DiagramContext. This is necessary
		// because the schema tabset, as a JComponent, can be only assigned
		// to one parent at once, and so can only appear in one tab at
		// once despite being common across all tabs in this tabset.
		this.addTab(BuilderBundle.getString("schemaTabName"), new JLabel());

		// Calculate the dataset tabs.
		this.recalculateDataSetTabs();
	}

	/**
	 * Works out which tab is currently selected. If it is a dataset tab, and
	 * not the schema overview tab, then return the tab contents. Otherwise,
	 * return null.
	 * 
	 * @return the selected dataset tab contents, or null if none is currently
	 *         selected.
	 */
	public DataSetTab getSelectedDataSetTab() {
		Object obj = this.getSelectedComponent();
		if (obj != null && (obj instanceof DataSetTab))
			return (DataSetTab) obj;
		else
			return null;
	}

	/**
	 * Returns the mart that this dataset tabset is displaying the contents of.
	 * 
	 * @return the mart that this dataset tabset is viewing.
	 */
	public Mart getMart() {
		return this.mart;
	}

	/**
	 * Returns the mart tabset that this dataset tabset appears inside of.
	 * 
	 * @return the parent mart tabset.
	 */
	public MartTabSet getMartTabSet() {
		return this.martTabSet;
	}

	/**
	 * Returns the schema tabset that lives inside this dataset tabset, and at
	 * any one moment is either attached to the schema overview tab, or the
	 * window part of a dataset tab.
	 * 
	 * @return the schema tabset for this dataset tabset.
	 */
	public SchemaTabSet getSchemaTabSet() {
		return this.schemaTabSet;
	}

	public void setSelectedIndex(int selectedIndex) {
		// First of all, select the tab as normal. This must be
		// done in order to get the diagram visible. If we do
		// it later, having attempted to set up the diagram without
		// making it visible first, we get weird null pointer
		// exceptions from half-constructed diagrams.
		super.setSelectedIndex(selectedIndex);

		// Then, work out which tab is the schema overview tab.
		int schemaTabIndex = this.indexOfTab(BuilderBundle
				.getString("schemaTabName"));

		// Then, work out which tab is currently being displayed.
		Component selectedComponent = this.getComponentAt(selectedIndex);
		if (selectedComponent instanceof DataSetTab) {
			// We are currently displaying a dataset tab. Display the
			// contents of the dataset tab, and attach the schema tabset
			// to the window part of the display. Put a placeholder in the
			// schema overview tab so that the tab doesn't disappear whilst
			// the schema tabset makes its visit elsewhere. The schema tabset
			// will have an appropriate diagram context attached to it
			// by the dataset tab once it is attached.
			this.setComponentAt(schemaTabIndex, new JLabel());
			DataSetTab datasetTab = (DataSetTab) selectedComponent;
			datasetTab.attachSchemaTabSet(this.schemaTabSet);
		} else {
			// We are currently displaying the schema overview tab.
			// Reattach the schema tabset to this overview tab, replacing
			// the placeholder label that was already there. Update the
			// diagram context on the schema tabset to display the normal
			// schema context.
			this.schemaTabSet.setDiagramContext(new SchemaContext(this));
			this.setComponentAt(schemaTabIndex, this.schemaTabSet);
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
			MartBuilderUtils.synchroniseMartDataSets(mart);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
			return;
		}

		// Add all datasets that we don't have yet.
		List martDataSets = new ArrayList(mart.getDataSets());
		for (Iterator i = martDataSets.iterator(); i.hasNext();) {
			DataSet dataset = (DataSet) i.next();
			if (!datasetToTab[0].contains(dataset))
				addDataSetTab(dataset);
		}

		// Remove all the datasets in our tabs that are not in the mart.
		List ourDataSets = new ArrayList(datasetToTab[0]);
		for (Iterator i = ourDataSets.iterator(); i.hasNext();) {
			DataSet dataset = (DataSet) i.next();
			if (!martDataSets.contains(dataset))
				removeDataSetTab(dataset);
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
	 * Asks all dataset diagrams in all dataset tabs to recalculate themselves
	 * to match the current contents of the datasets.
	 */
	public void recalculateAllDataSetDiagrams() {
		for (Iterator i = this.datasetToTab[1].iterator(); i.hasNext();)
			((DataSetTab) i.next()).getDataSetDiagram().recalculateDiagram();
	}

	/**
	 * Asks all dataset diagrams in all dataset tabs to repaint themselves, in
	 * case any components have changed appearance. Do not use this if the
	 * components have changed size - use recalculate instead.
	 */
	public void repaintAllDataSetDiagrams() {
		for (Iterator i = this.datasetToTab[1].iterator(); i.hasNext();)
			((DataSetTab) i.next()).getDataSetDiagram().repaintDiagram();
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
		int choice = JOptionPane.showConfirmDialog(this, BuilderBundle
				.getString("confirmDelDataset"), BuilderBundle
				.getString("questionTitle"), JOptionPane.YES_NO_OPTION);

		// Refuse to do it if they said no.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// Do it, but in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the dataset from the mart.
					MartBuilderUtils.removeDataSetFromMart(mart, dataset);

					// Remove the tab.
					removeDataSetTab(dataset);

					// Set our modified status to true.
					martTabSet.setModifiedStatus(true);
				} catch (Throwable t) {
					martTabSet.getMartBuilder().showStackTrace(t);
				}
			}
		});
	}

	private void removeDataSetTab(DataSet dataset) {
		// Work out which tab the dataset lives in.
		int index = this.datasetToTab[0].indexOf(dataset);
		DataSetTab datasetTab = (DataSetTab) this.datasetToTab[1].get(index);

		// Work out the tab index.
		int tabIndex = this.indexOfComponent(datasetTab);

		// Remove the tab, and it's mapping from the dataset-to-tab map.
		this.remove(datasetTab);
		this.datasetToTab[0].remove(index);
		this.datasetToTab[1].remove(index);

		// Fake a click on the last tab before this one to ensure
		// at least one tab remains visible and up-to-date.
		this.setSelectedIndex(Math.max(tabIndex - 1, 0));
	}

	private void addDataSetTab(DataSet dataset) {
		// Create a tab for the dataset.
		DataSetTab datasetTab = new DataSetTab(this, dataset);

		// Add the tab, and remember the mapping between dataset and tab.
		this.addTab(dataset.getName(), datasetTab);
		this.datasetToTab[0].add(dataset);
		this.datasetToTab[1].add(datasetTab);
	}

	private String askUserForName(String message, String defaultResponse) {
		// Ask the user for a name. Use the default response
		// as the default value in the input field.
		String name = (String) JOptionPane.showInputDialog(this.martTabSet
				.getMartBuilder(), message, BuilderBundle
				.getString("questionTitle"), JOptionPane.QUESTION_MESSAGE,
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

	/**
	 * Renames a dataset, then renames the tab too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 */
	public void requestRenameDataSet(DataSet dataset) {
		try {
			// Ask user for the new name.
			String newName = this.askUserForName(BuilderBundle
					.getString("requestDataSetName"), dataset.getName());

			// If the new name is null (user cancelled), or has
			// not changed, don't rename it.
			if (newName == null || newName.equals(dataset.getName()))
				return;

			// Work out which tab the dataset is in.
			int idx = this.indexOfTab(dataset.getName());

			// Rename the dataset.
			MartBuilderUtils.renameDataSet(this.mart, dataset, newName);

			// Rename the tab displaying it.
			this.setTitleAt(idx, dataset.getName());

			// Set the tabset as modified.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
		try {
			// Ask user for the new name.
			String newName = this.askUserForName(BuilderBundle
					.getString("requestDataSetColumnName"), dsColumn.getName());

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

			// Set the tabset as modified.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
	public void requestRenameDataSetTable(DataSetTable dsTable) {
		try {
			// Ask user for the new name.
			String newName = this.askUserForName(BuilderBundle
					.getString("requestDataSetTableName"), dsTable.getName());

			// If the new name is null (user cancelled), or has
			// not changed, don't rename it.
			if (newName == null || newName.equals(dsTable.getName()))
				return;

			// Rename the dataset.
			MartBuilderUtils.renameDataSetTable(dsTable, newName);

			// Recalculate the dataset diagram as the table name will have
			// caused the table to resize itself.
			this.recalculateDataSetDiagram((DataSet) dsTable.getSchema());

			// Set the tabset as modified.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Request that a single dataset be created, unoptimised, around the given
	 * table.
	 * 
	 * @param table
	 *            the table to create the dataset around.
	 */
	public void requestCreateDataSet(final Table table) {
		// Ask user for a name to use.
		final String name = this.askUserForName(BuilderBundle
				.getString("requestDataSetName"), table.getName());

		// If they cancelled it, cancel the operation.
		if (name == null)
			return;

		// In the background, do the dataset creation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Create the dataset.
					DataSet dataset = MartBuilderUtils.createDataSet(mart,
							table, name);

					// Add a tab for it.
					addDataSetTab(dataset);

					// Update the modified status.
					martTabSet.setModifiedStatus(true);
				} catch (Throwable t) {
					martTabSet.getMartBuilder().showStackTrace(t);
				}
			}
		});
	}

	/**
	 * Given a table, suggest a series of optimised datasets that may be
	 * possible for that table.
	 * 
	 * @param table
	 *            the table to suggest datasets for.
	 */
	public void requestSuggestDataSets(final Table table) {
		// Ask the user for a name for the table.
		final String name = this.askUserForName(BuilderBundle
				.getString("requestDataSetName"), table.getName());

		// If they cancelled it, return without doing anything.
		if (name == null)
			return;

		// In the background, suggest the datasets.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Suggest them.
					Collection dss = MartBuilderUtils.suggestDataSets(mart,
							table, name);

					// For each one suggested, add a dataset tab for it.
					for (Iterator i = dss.iterator(); i.hasNext();) {
						DataSet dataset = (DataSet) i.next();
						addDataSetTab(dataset);
					}

					// Update the modified status for this tabset.
					martTabSet.setModifiedStatus(true);
				} catch (Throwable t) {
					martTabSet.getMartBuilder().showStackTrace(t);
				}
			}
		});
	}

	/**
	 * Pops up a dialog with details of the constructor, which allows the user
	 * to modify them.
	 * 
	 * @param mc
	 *            the mart constructor to modify.
	 */
	public void requestModifyMartConstructor(DataSet ds, MartConstructor mc) {
		try {
			MartConstructor modMC = MartConstructorManagerDialog
					.createMartConstructor(this, mc);
			if (modMC != null)
				MartBuilderUtils.setMartConstructor(ds, modMC);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Test this mart constructor to see if the datasource or database it
	 * represents is contactable.
	 * 
	 * @param mc
	 *            the mart constructor to test.
	 */
	public void requestTestMartConstructor(MartConstructor mc) {
		// Assume we've failed.
		boolean passedTest = false;

		try {
			// Attempt to pass the test.
			passedTest = MartBuilderUtils.testMartConstructor(mc);
		} catch (Throwable t) {
			// If we get an exception, we failed the test, and should
			// tell the user why.
			passedTest = false;
			this.martTabSet.getMartBuilder().showStackTrace(t);
		}

		// Tell the user if we passed or failed.
		if (passedTest)
			JOptionPane.showMessageDialog(this, BuilderBundle
					.getString("martConstructorTestPassed"), BuilderBundle
					.getString("testTitle"), JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(this, BuilderBundle
					.getString("martConstructorTestFailed"), BuilderBundle
					.getString("testTitle"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Ask that a dataset be optimised.
	 * 
	 * @param dataset
	 *            the dataset to optimise.
	 */
	public void requestOptimiseDataSet(final DataSet dataset) {
		// Do this in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Optimise the dataset.
					MartBuilderUtils.optimiseDataSet(dataset);

					// Repaint the schema diagrams as they may be currently
					// in a window, and the colours for that window may
					// have changed depending on optimisation results.
					schemaTabSet.repaintAllSchemaDiagrams();

					// Recalculate the dataset diagram based on the newly
					// optimised dataset.
					recalculateDataSetDiagram(dataset);

					// If we are showing an explanation diagram, repaint
					// that too for the same reasons as the schema diagram.
					if (currentExplanationDiagram != null)
						currentExplanationDiagram.repaintDiagram();

					// Update the modified status for the tabset.
					martTabSet.setModifiedStatus(true);
				} catch (Throwable t) {
					martTabSet.getMartBuilder().showStackTrace(t);
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
			this.schemaTabSet.repaintSchemaDiagram(table.getSchema());
			this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
				this.schemaTabSet.repaintSchemaDiagram(relation.getFirstKey()
						.getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
				this.schemaTabSet.repaintSchemaDiagram(relation.getFirstKey()
						.getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
				this.schemaTabSet.repaintSchemaDiagram(relation.getFirstKey()
						.getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
				this.schemaTabSet.repaintSchemaDiagram(relation.getFirstKey()
						.getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
		}
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
				this.schemaTabSet.repaintSchemaDiagram(relation.getFirstKey()
						.getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
				this.schemaTabSet.repaintSchemaDiagram(relation.getFirstKey()
						.getTable().getSchema());

			// Otherwise, it is external, so repaint the schema overview
			// diagram.
			else
				this.schemaTabSet.repaintOverviewDiagram();

			// Recalculate the dataset diagram based on the modified dataset.
			this.recalculateDataSetDiagram(ds);

			// Update the explanation diagram so that it correctly
			// reflects the masked relation.
			if (this.currentExplanationDiagram != null)
				this.currentExplanationDiagram.repaintDiagram();

			// Update the modified status.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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

			// Update the modification status for this tabset.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
			this.recalculateDataSetDiagram(ds);

			// Update the modification status for this tabset.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
			ExplainDataSetDialog.showTableExplanation(this.schemaTabSet,
					dsTable);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Ask that an explanation dialog be opened that explains how the given
	 * dataset column was constructed.
	 * 
	 * @param dsColumn
	 *            the dataset column that needs to be explained.
	 */
	public void requestExplainColumn(DataSetColumn dsColumn) {
		try {
			// Open the dialog. The dialog will set a flag in this instance
			// that contains a reference to its diagram, so that the diagram
			// can be updated as the user edits the dataset.
			ExplainDataSetDialog.showColumnExplanation(this.schemaTabSet,
					dsColumn);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * This method is called by the {@link ExplainDataSetDialog} when it is
	 * opened, and tells the dataset tabset about the diagram it is using. This
	 * diagram is then updated whenever the dataset window is updated. It is
	 * called again, with a null value, when the dialog is closed.
	 * 
	 * @param diagram
	 *            the diagram being displayed by the current
	 *            {@link ExplainDataSetDialog}, or null if none is currently
	 *            being displayed.
	 */
	public void setCurrentExplanationDiagram(Diagram diagram) {
		this.currentExplanationDiagram = diagram;
	}

	/**
	 * Requests that the dataset be partitioned by schema.
	 * 
	 * @param dataset
	 *            the dataset to partition.
	 */
	public void requestPartitionBySchema(DataSet dataset) {
		// Do the partitioning.
		MartBuilderUtils.partitionBySchema(dataset);

		// Repaint the dataset diagram, as the schema name
		// column will have changed colour.
		this.repaintDataSetDiagram(dataset);

		// Update the modified status.
		this.martTabSet.setModifiedStatus(true);
	}

	/**
	 * Requests that the dataset should not be partitioned by schema.
	 * 
	 * @param dataset
	 *            the dataset to not partition.
	 */
	public void requestUnpartitionBySchema(DataSet dataset) {
		// Do the unpartitioning.
		MartBuilderUtils.unpartitionBySchema(dataset);

		// Repaint the dataset diagram, as the schema name
		// column will have changed colour.
		this.repaintDataSetDiagram(dataset);

		// Update the modified status.
		this.martTabSet.setModifiedStatus(true);
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
	public void requestPartitionByColumn(DataSet dataset, WrappedColumn column) {
		PartitionedColumnType type;

		// If the column is already partitioned, open a dialog
		// explaining this and asking the user to edit the settings.
		if (dataset.getPartitionedWrappedColumns().contains(column)) {
			PartitionedColumnType oldType = dataset
					.getPartitionedWrappedColumnType(column);
			type = PartitionColumnDialog.updatePartitionedColumnType(this,
					oldType);

			// Check they didn't cancel the request, or left the
			// scheme unchanged.
			if (type == null || type.equals(oldType))
				return;
		}

		// Otherwise, open a dialog asking the user to define the partitioning
		// scheme.
		else {
			type = PartitionColumnDialog.createPartitionedColumnType(this);

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
	public void requestPartitionByColumn(DataSet dataset, WrappedColumn column,
			PartitionedColumnType type) {
		try {
			// Do the partitioning.
			MartBuilderUtils.partitionByColumn(dataset, column, type);

			// Repaint the dataset, as the partitioned column
			// will have changed colour.
			this.repaintDataSetDiagram(dataset);

			// Update the modified status on this tabset.
			this.martTabSet.setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
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
	public void requestUnpartitionByColumn(DataSet dataset, WrappedColumn column) {
		// Unpartition the column.
		MartBuilderUtils.unpartitionByColumn(dataset, column);

		// Repaint the dataset, as the partitioned column
		// will have changed colour.
		this.repaintDataSetDiagram(dataset);

		// Update the modified status on this tabset.
		this.martTabSet.setModifiedStatus(true);
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
		this.martTabSet.setModifiedStatus(true);
	}

	/**
	 * Constructs the mart for the given dataset, using the mart constructor
	 * currently assigned to that dataset.
	 * 
	 * @param dataset
	 *            the dataset to construct into a mart.
	 */
	public void requestConstructMart(DataSet dataset) {
		// Obtain a thread for constructing the dataset.
		final ConstructorRunnable constructor;
		
		try {
			constructor = dataset.getMartConstructor()
				.getConstructorRunnable(dataset);
		} catch (Throwable t) {
			this.martTabSet.getMartBuilder().showStackTrace(t);
			return;
		}

		// Create a progress monitor.
		final ProgressMonitor progressMonitor = new ProgressMonitor(this
				.getMartTabSet().getMartBuilder(), BuilderBundle.getString(
				"creatingMart", dataset.getName()), "", 0, 100);
		progressMonitor.setProgress(0); // Start with 0% complete.
		progressMonitor.setMillisToPopup(1000); // Open after 1 second.

		// Start the construction in a thread. It does not need to be
		// Swing-thread-safe because it will never access the GUI. All
		// GUI interaction is done through the Timer below.
		final Thread thread = new Thread(constructor);
		thread.start();

		// Create a timer thread that will update the progress dialog.
		// We use the Swing Timer to make it Swing-thread-safe. (1000 millis
		// equals 1 second.)
		final Timer timer = new Timer(500, null); 
		timer.setInitialDelay(0); // Start immediately upon request.
		timer.setCoalesce(true); // Coalesce delayed events.
		timer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						monitorConstructionProgress(thread, timer, progressMonitor, constructor);
					}
				});
			}
		});

		// Start the timer.
		timer.start();
	}
	
	private void monitorConstructionProgress(Thread thread, Timer timer, ProgressMonitor progressMonitor, ConstructorRunnable constructor) {
		// Did the job complete yet?
		if (thread.isAlive() && !progressMonitor.isCanceled()) {
			// If not, update the progress report.
			progressMonitor.setNote(constructor.getStatusMessage());
			progressMonitor.setProgress(constructor
					.getPercentComplete());
		} else {
			// If it completed, close the task and tidy up.
			// Stop the timer.
			timer.stop();
			// Stop the thread.
			constructor.cancel();
			// Close the progress dialog.
			progressMonitor.close();
			// If it failed, show the exception.
			Exception failure = constructor.getFailureException();
			if (failure != null)
				getMartTabSet().getMartBuilder()
						.showStackTrace(failure);
			// Inform user of success or otherwise.
			if (failure == null)
				JOptionPane.showMessageDialog(getMartTabSet()
						.getMartBuilder(), BuilderBundle
						.getString("martConstructionComplete"),
						BuilderBundle.getString("messageTitle"),
						JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(getMartTabSet()
						.getMartBuilder(), BuilderBundle
						.getString("martConstructionFailed"),
						BuilderBundle.getString("messageTitle"),
						JOptionPane.WARNING_MESSAGE);
		}
	}

	private JPopupMenu getDataSetTabContextMenu(final DataSet dataset) {
		// Start with an empty menu.
		JPopupMenu contextMenu = new JPopupMenu();

		// This item allows the user to remove the dataset from the mart.
		JMenuItem close = new JMenuItem(BuilderBundle
				.getString("removeDataSetTitle"));
		close.setMnemonic(BuilderBundle.getString("removeDataSetMnemonic")
				.charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				requestRemoveDataSet(dataset);
			}
		});
		contextMenu.add(close);

		// This item allows the user to rename the dataset.
		JMenuItem rename = new JMenuItem(BuilderBundle
				.getString("renameDataSetTitle"));
		rename.setMnemonic(BuilderBundle.getString("renameDataSetMnemonic")
				.charAt(0));
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

		private DataSetTabSet datasetTabSet;

		private JPanel displayArea;

		private JRadioButton windowButton;

		private SchemaDiagram datasetDiagram;

		/**
		 * The constructor constructs a new tab containing two parts. The first
		 * part is a window diagram, which views the schema tabset through a
		 * {@link WindowContext}. The second part is the dataset diagram, which
		 * is viewed through a {@link DataSetContext}. The two are shown
		 * alternately in the panel, depending on which button the user selects.
		 * 
		 * @param datasetTabSet
		 *            the dataset tabset this tab belongs in.
		 * @param dataset
		 *            the dataset this tab will display.
		 */
		public DataSetTab(DataSetTabSet datasetTabSet, DataSet dataset) {
			// Set up our layout.
			super(new BorderLayout());

			// Remember which dataset and tabset we are working with.
			this.dataset = dataset;
			this.datasetTabSet = datasetTabSet;

			// Create display part of the tab. The display area consists of
			// two cards - one for the window, one for the dataset. Buttons
			// in another area switch between the cards.
			this.displayArea = new JPanel(new CardLayout());

			// Dataset card first. Create a diagram, then place it inside
			// a scrollpane. This scrollpane becomes the dataset card. Don't
			// forget to set the context too.
			this.datasetDiagram = new SchemaDiagram(datasetTabSet, dataset);
			this.datasetDiagram.setDiagramContext(new DataSetContext(
					datasetTabSet, dataset));
			JScrollPane scroller = new JScrollPane(this.datasetDiagram);
			scroller.getViewport().setBackground(
					this.datasetDiagram.getBackground());
			displayArea.add(scroller, "DATASET_CARD");

			// Create panel which contains the buttons.
			JPanel buttonsPanel = new JPanel();

			// Create the button that selects the dataset card.
			final JRadioButton datasetButton = new JRadioButton(BuilderBundle
					.getString("datasetButtonName"));
			datasetButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == datasetButton) {
						CardLayout cards = (CardLayout) displayArea.getLayout();
						cards.show(displayArea, "DATASET_CARD");
					}
				}
			});
			buttonsPanel.add(datasetButton);

			// Create the button that selects the window card.
			this.windowButton = new JRadioButton(BuilderBundle
					.getString("windowButtonName"));
			windowButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == windowButton) {
						CardLayout cards = (CardLayout) displayArea.getLayout();
						cards.show(displayArea, "WINDOW_CARD");
					}
				}
			});
			buttonsPanel.add(windowButton);

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
		 * display its diagrams, instead of whatever context it was using
		 * before. Be warned - once the schema tabset is attached here, it will
		 * disappear from wherever it was before, because of the way that
		 * JComponents can only have one parent.
		 * 
		 * @param datasetTabSet
		 *            the dataset tabset we are part of.
		 * @param schemaTabSet
		 *            the schema tabset to attach to ourselves.
		 */
		public void attachSchemaTabSet(SchemaTabSet schemaTabSet) {
			// Set the context on the schema tabset, and set the
			// tabset to be the window card.
			WindowContext context = new WindowContext(this.datasetTabSet,
					this.dataset);
			schemaTabSet.setDiagramContext(context);
			this.displayArea.add(schemaTabSet, "WINDOW_CARD");

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
