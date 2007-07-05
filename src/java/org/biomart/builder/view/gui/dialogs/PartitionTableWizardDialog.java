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

package org.biomart.builder.view.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.PartitionTable;
import org.biomart.builder.model.TransformationUnit;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.SchemaModificationSet.CompoundRelationDefinition;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.InverseMap;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * A wizard which makes light work of applying partition tables to simple
 * situation datasets.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class PartitionTableWizardDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton execute;

	private JButton cancel;

	private boolean cancelled;

	private DataSet dataset;

	private DataSetTable dimension;

	private JComboBox partitionTable;

	private JComboBox namingCol;

	private List ptLevels;

	private List dsLevels;

	private final Mart mart;

	/**
	 * This is the wizard for applying partition tables to datasets.
	 * 
	 * @param dataset
	 *            the dataset.
	 */
	public PartitionTableWizardDialog(final DataSet dataset) {
		this(Resources.get("partitionWizardDataSetDialogTitle"), dataset
				.getMainTable().getColumns(), dataset.getMart());
		this.dataset = dataset;
		this.dimension = null;
	}

	/**
	 * This is the wizard for applying partition tables to dimensions.
	 * 
	 * @param dimension
	 *            the dimension.
	 */
	public PartitionTableWizardDialog(final DataSetTable dimension) {
		this(Resources.get("partitionWizardDimensionDialogTitle"), dimension
				.getColumns(), ((DataSet) dimension.getSchema()).getMart());
		this.dataset = (DataSet) dimension.getSchema();
		this.dimension = dimension;
	}

	private PartitionTableWizardDialog(final String title,
			final Collection datasetCols, final Mart mart) {
		// Create the base dialog.
		super();
		this.setTitle(title);
		this.setModal(true);
		this.cancelled = true;
		this.mart = mart;

		// Convert column list to include blank.
		final List dsColList = new ArrayList();
		for (final Iterator i = datasetCols.iterator(); i.hasNext();) {
			final DataSetColumn dsCol = (DataSetColumn) i.next();
			if (dsCol instanceof WrappedColumn
					|| dsCol instanceof InheritedColumn)
				dsColList.add(dsCol);
		}
		Collections.sort(dsColList);
		dsColList.add(0, null);

		// Create the content pane to store the create dialog panel.
		final JPanel content = new JPanel(new GridBagLayout());
		this.setContentPane(content);

		// Create constraints for labels that are not in the last row.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Select partition table.
		this.partitionTable = new JComboBox();
		for (final Iterator i = mart.getPartitionTableNames().iterator(); i
				.hasNext();)
			this.partitionTable.addItem(i.next());
		content.add(new JLabel(Resources.get("wizardChoosePTLabel")),
				labelConstraints);
		content.add(this.partitionTable, fieldConstraints);

		// Select partition column for naming.
		this.namingCol = new JComboBox();
		content.add(new JLabel(Resources.get("wizardChooseNameLabel")),
				labelConstraints);
		content.add(this.namingCol, fieldConstraints);

		// Set up apply-levels table.
		final JPanel applyLevels = new JPanel(new GridBagLayout());
		content.add(applyLevels, fieldConstraints);

		// On partition table change listener.
		this.partitionTable.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final String ptTableName = (String) PartitionTableWizardDialog.this.partitionTable
						.getSelectedItem();
				if (ptTableName != null) {
					// Load partition table.
					final PartitionTable pt = mart
							.getDataSetByName(ptTableName).asPartitionTable();
					// Identify column names.
					final Iterator colNames = pt.getSelectedColumnNames()
							.iterator();
					// Clear naming column choices.
					PartitionTableWizardDialog.this.namingCol.removeAllItems();
					// Clear apply-level table.
					applyLevels.removeAll();
					PartitionTableWizardDialog.this.ptLevels = new ArrayList();
					PartitionTableWizardDialog.this.dsLevels = new ArrayList();
					// Reinsert one pair of entries per remaining subdivision.
					for (List currLevelNames = PartitionTableWizardDialog.this
							.getNextSubdivision(colNames); !currLevelNames
							.isEmpty(); currLevelNames = PartitionTableWizardDialog.this
							.getNextSubdivision(colNames)) {
						// Repopulate naming column choices from top-level only.
						if (PartitionTableWizardDialog.this.namingCol
								.getItemCount() == 0)
							for (final Iterator i = currLevelNames.iterator(); i
									.hasNext();)
								PartitionTableWizardDialog.this.namingCol
										.addItem(i.next());
						// Add combos to apply-levels.
						final JComboBox ptCombo = new JComboBox(currLevelNames
								.toArray());
						PartitionTableWizardDialog.this.ptLevels.add(ptCombo);
						final JComboBox dsCombo = new JComboBox(dsColList
								.toArray());
						PartitionTableWizardDialog.this.dsLevels.add(dsCombo);
						applyLevels.add(new JLabel(Resources
								.get("wizardPTColLabel")), labelConstraints);
						final JPanel subpanel = new JPanel();
						subpanel.add(ptCombo);
						subpanel.add(new JLabel(Resources
								.get("wizardDSColLabel")));
						subpanel.add(dsCombo);
						applyLevels.add(subpanel, fieldConstraints);
						// Restrict to first box only at first.
						if (PartitionTableWizardDialog.this.dsLevels.size() > 1) {
							dsCombo.setEnabled(false);
							ptCombo.setEnabled(false);
						}
						// Disable subsequent combos if this one is
						// set to null.
						dsCombo.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent e) {
								for (int i = PartitionTableWizardDialog.this.dsLevels
										.indexOf(dsCombo) + 1; i < PartitionTableWizardDialog.this.dsLevels
										.size(); i++) {
									final JComboBox currPT = (JComboBox) PartitionTableWizardDialog.this.ptLevels
											.get(i);
									final JComboBox currDS = (JComboBox) PartitionTableWizardDialog.this.dsLevels
											.get(i);
									final JComboBox prevDS = (JComboBox) PartitionTableWizardDialog.this.dsLevels
											.get(i - 1);
									if (prevDS.getSelectedItem() == null)
										currDS.setSelectedItem(null);
									currDS
											.setEnabled(prevDS
													.getSelectedItem() != null);
									currPT
											.setEnabled(prevDS
													.getSelectedItem() != null);
								}
							}
						});
					}
					// Repack dialog.
					PartitionTableWizardDialog.this.pack();
				}
			}
		});

		// Create the cancel and OK buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(Resources.get("updateButton"));

		// Add the buttons to the dialog.
		final JLabel label = new JLabel();
		content.add(label, labelLastRowConstraints);
		final JPanel field = new JPanel();
		field.add(this.cancel);
		field.add(this.execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the cancel button and use it to close this
		// dialog without making any changes.
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				PartitionTableWizardDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (PartitionTableWizardDialog.this.validateFields()) {
					PartitionTableWizardDialog.this.cancelled = false;
					PartitionTableWizardDialog.this.setVisible(false);
				}
			}
		});

		// Set default values.
		this.partitionTable.setSelectedIndex(0);

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		final List messages = new ArrayList();

		// Must have at least one column selected.
		if (((JComboBox) this.dsLevels.get(0)).getSelectedItem() == null)
			messages.add(Resources.get("wizardMissingFirstMapping"));

		// Any messages to display? Show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	private List getNextSubdivision(final Iterator cols) {
		if (!cols.hasNext())
			return Collections.EMPTY_LIST;
		final List nextcols = new ArrayList();
		while (cols.hasNext()) {
			final String colname = (String) cols.next();
			if (!colname.equals(PartitionTable.DIV_COLUMN))
				nextcols.add(this.partitionTable.getSelectedItem() + "."
						+ colname);
			else
				break;
		}
		return nextcols;
	}

	/**
	 * Was the dialog cancelled?
	 * 
	 * @return <tt>true</tt> if it was cancelled.
	 */
	public boolean wasCancelled() {
		return this.cancelled;
	}

	/**
	 * Use the input data to do the partitioning wizard stuff.
	 */
	public void applyPartitioning() {
		try {
			// Prepare first row of partition table for purposes
			// of row counting later.
			final PartitionTable pt = this.mart.getDataSetByName(
					(String) this.partitionTable.getSelectedItem())
					.asPartitionTable();
			pt.prepareRows(null, PartitionTable.UNLIMITED_ROWS);
			pt.nextRow();
			// Apply stuff.
			if (this.dimension != null)
				// Apply PT to dimension.
				MartBuilderUtils.partitionDataSetTable(this.dataset,
						this.dimension, (String) this.namingCol
								.getSelectedItem());
			else
				// Apply PT to dataset.
				MartBuilderUtils.partitionDataSet(this.dataset,
						(String) this.namingCol.getSelectedItem());
			// Build each entry's restrictions.
			for (int nextLevel = 0; nextLevel < this.ptLevels.size(); nextLevel++) {
				// Skip if no complete entry for this in JTable.
				if (((JComboBox) this.dsLevels.get(nextLevel))
						.getSelectedItem() == null)
					continue;
				// Locate ds column and partition to use for that column.
				final DataSetTable dsTab = this.dimension == null ? this.dataset
						.getMainTable()
						: this.dimension;
				final DataSetColumn dsCol = (DataSetColumn) ((JComboBox) this.dsLevels
						.get(nextLevel)).getSelectedItem();
				final String ptCol = (String) ((JComboBox) this.ptLevels
						.get(nextLevel)).getSelectedItem();
				// Find transformation unit providing that column.
				Column realCol = null;
				Table realTbl = null;
				Relation realRel = null;
				for (final Iterator i = dsTab.getTransformationUnits()
						.iterator(); i.hasNext() && realCol == null;) {
					final TransformationUnit tu = (TransformationUnit) i.next();
					if (tu.getNewColumnNameMap().containsValue(dsCol)) {
						if (tu instanceof JoinTable) {
							realRel = ((JoinTable) tu).getSchemaRelation();
							realTbl = ((JoinTable) tu).getTable();
						} else if (tu instanceof SelectFromTable)
							realTbl = ((SelectFromTable) tu).getTable();
						else
							throw new BioMartError(); // Eh??
						realCol = realTbl
								.getColumnByName((String) new InverseMap(tu
										.getNewColumnNameMap()).get(dsCol));
					}
				}
				// Apply table restriction to real table.
				final Map aliases = new HashMap();
				aliases.put(realCol, "partCol");
				MartBuilderUtils.restrictTable(dsTab, realTbl, ":partCol='%"
						+ ptCol + "%'", aliases, true);
				if (realRel != null) {
					// Get real partition table.
					final PartitionTable realPT = pt.getSelectedColumn(
							ptCol.split("\\.")[1]).getPartitionTable();
					int compound = 0;
					realPT.prepareRows(null, PartitionTable.UNLIMITED_ROWS);
					while (realPT.nextRow())
						compound++;
					// Apply compound relation to source relation and attach
					// partition table.
					MartBuilderUtils.compoundRelation(dsTab, realRel,
							new CompoundRelationDefinition(compound, true,
									ptCol));
				}
			}
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}
	}
}
