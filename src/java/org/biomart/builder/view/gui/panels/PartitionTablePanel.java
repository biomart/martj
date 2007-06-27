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

package org.biomart.builder.view.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableModel;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.dialogs.AddPartitionColumnDialog;
import org.biomart.builder.view.gui.dialogs.AddPartitionSubdivisionDialog;
import org.biomart.builder.view.gui.panels.PartitionColumnModifierPanel.FixedColumnModifierPanel;
import org.biomart.builder.view.gui.panels.PartitionColumnModifierPanel.RegexColumnModifierPanel;
import org.biomart.builder.view.gui.panels.PartitionTableModifierPanel.SelectFromModifierPanel;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.model.PartitionTable.PartitionColumn;
import org.biomart.common.model.PartitionTable.PartitionRow;
import org.biomart.common.model.PartitionTable.AbstractPartitionTable.SelectPartitionTable;
import org.biomart.common.model.PartitionTable.PartitionColumn.FixedColumn;
import org.biomart.common.model.PartitionTable.PartitionColumn.RegexColumn;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * Displays the contents of a partition table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class PartitionTablePanel extends JPanel {
	private static final long serialVersionUID = 1;

	/**
	 * The number of rows we show in preview.
	 */
	public static final int PREVIEW_ROW_COUNT = 10;

	/**
	 * The color we use for our background.
	 */
	public static final Color BACKGROUND_COLOUR = Color.PINK;

	private final PartitionTable partitionTable;

	private final MartTab martTab;

	private final GridBagConstraints labelConstraints;

	private final GridBagConstraints fieldConstraints;

	private final GridBagConstraints labelLastRowConstraints;

	private final GridBagConstraints fieldLastRowConstraints;

	private PartitionColumnModifierPanel columnMods;

	private PartitionTable selectedSubdivParent;

	/**
	 * Creates a new panel that displays the partition table definition.
	 * 
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param partitionTable
	 *            the partition table to draw in this diagram.
	 */
	public PartitionTablePanel(final MartTab martTab,
			final PartitionTable partitionTable) {
		// Set our layout.
		super(new GridBagLayout());

		// Set our background color.
		this.setBackground(PartitionTablePanel.BACKGROUND_COLOUR);

		// Create constraints for labels that are not in the last row.
		this.labelConstraints = new GridBagConstraints();
		this.labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		this.labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		this.labelConstraints.anchor = GridBagConstraints.LINE_END;
		this.labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		this.fieldConstraints = new GridBagConstraints();
		this.fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		this.fieldConstraints.fill = GridBagConstraints.NONE;
		this.fieldConstraints.anchor = GridBagConstraints.LINE_START;
		this.fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		this.labelLastRowConstraints = (GridBagConstraints) this.labelConstraints
				.clone();
		this.labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		this.fieldLastRowConstraints = (GridBagConstraints) this.fieldConstraints
				.clone();
		this.fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Remember the schema, then lay it out.
		this.martTab = martTab;
		this.partitionTable = partitionTable;
		this.recalculatePanel();
	}

	private void recalculatePanel() {
		// First of all, remove all our existing components.
		this.removeAll();

		// The list of table columns is populated with the names of columns.
		final Set badColumns = new HashSet();
		final DefaultListModel availableColumns = new DefaultListModel();
		final DefaultListModel selectedColumns = new DefaultListModel();

		// Create Preview panel - multi-column display.
		final DefaultTableModel previewData = new DefaultTableModel();

		// Table type.
		final PartitionTableModifierPanel tableMods;
		if (this.getPartitionTable() instanceof SelectPartitionTable)
			tableMods = new SelectFromModifierPanel((SelectPartitionTable) this
					.getPartitionTable(), this.getMartTab().getMart()
					.getAllTables());
		else
			throw new BioMartError(); // Should never happen.
		JLabel label = new JLabel(Resources.get("partitionTableTypeLabel"));
		this.add(label, this.labelConstraints);
		JPanel field = new JPanel();
		field.add(new JLabel(tableMods.getType()));
		// Table modifications panel.
		final JPanel tableModsPanel = new JPanel(new GridBagLayout());
		final JPanel tableModsHolder = new JPanel();
		tableModsHolder.add(tableMods);
		tableModsHolder.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		tableModsPanel.add(tableModsHolder, this.fieldConstraints);
		field.add(tableModsPanel);
		this.add(field, this.fieldConstraints);
		// Update/Reset buttons below modifier panel inside tableModsPanel.
		tableModsPanel.add(new JLabel(), this.labelLastRowConstraints);
		field = new JPanel();
		final JButton resetTableMods = new JButton(Resources.get("resetButton"));
		final JButton updateTableMods = new JButton(Resources
				.get("updateButton"));
		resetTableMods.setEnabled(tableMods.isMutable());
		updateTableMods.setEnabled(tableMods.isMutable());
		resetTableMods.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				tableMods.reset();
			}
		});
		updateTableMods.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				try {
					tableMods.update();
					PartitionTablePanel.this.updatePreview(previewData);
				} catch (final PartitionException pe) {
					StackTrace.showStackTrace(pe);
				}
			}
		});
		field.add(resetTableMods);
		field.add(updateTableMods);
		tableModsPanel.add(field, this.fieldLastRowConstraints);

		// Column selector and modifier panel.
		label = new JLabel(Resources.get("partitionTableColumnLabel"));
		this.add(label, this.labelConstraints);
		field = new JPanel();

		// Sub-panel on right is modifier panel.
		final JPanel columnModsPanel = new JPanel(new GridBagLayout());
		final JPanel columnModsHolder = new JPanel();
		columnModsHolder.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		columnModsPanel.add(columnModsHolder, this.fieldConstraints);
		// Update/Reset buttons below modifier panel inside columnModsPanel.
		columnModsPanel.add(new JLabel(), this.labelLastRowConstraints);
		field = new JPanel();
		final JButton resetColumnMods = new JButton(Resources
				.get("resetButton"));
		final JButton updateColumnMods = new JButton(Resources
				.get("updateButton"));
		resetColumnMods.setEnabled(false);
		updateColumnMods.setEnabled(false);
		resetColumnMods.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTablePanel.this.columnMods.reset();
			}
		});
		updateColumnMods.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				try {
					PartitionTablePanel.this.columnMods.update();
					PartitionTablePanel.this.updatePreview(previewData);
				} catch (final PartitionException pe) {
					StackTrace.showStackTrace(pe);
				}
			}
		});
		field.add(resetColumnMods);
		field.add(updateColumnMods);
		columnModsPanel.add(field, this.fieldLastRowConstraints);

		// Sub-panel on left is selector panel.
		final JPanel columnNamePanel = new JPanel(new GridBagLayout());
		// Add column drop-down.
		final JComboBox columnList = new JComboBox();
		final DefaultComboBoxModel columns = new DefaultComboBoxModel(this
				.getPartitionTable().getColumnNames().toArray());
		columnList.setModel(columns);
		columnNamePanel.add(columnList, this.fieldConstraints);
		// Add add/remove/rename buttons in sub-panel.
		final JPanel buttonPanel = new JPanel();
		// Create and add buttons to buttonPanel.
		final JButton addButton = new JButton(Resources.get("addButton"));
		final JButton renameButton = new JButton(Resources.get("renameButton"));
		final JButton removeButton = new JButton(Resources.get("removeButton"));
		buttonPanel.add(addButton);
		buttonPanel.add(renameButton);
		buttonPanel.add(removeButton);
		// Listeners on buttons.
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final PartitionColumn col = PartitionTablePanel.this
						.showAddColumnDialog();
				if (col == null)
					return;
				try {
					PartitionTablePanel.this.getPartitionTable().addColumn(
							col.getColumnName(), col);
				} catch (final PartitionException e) {
					// Never happens.
					throw new BioMartError(e);
				}
				columns.addElement(col.getColumnName());
				PartitionTablePanel.this.updatePreview(previewData);
				availableColumns.addElement(col.getColumnName());
				badColumns.remove(col.getColumnName());
			}
		});
		renameButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = columns.getSelectedItem();
				if (obj != null) {
					// Pops up a dialog asking for new name.
					final String name = PartitionTablePanel.this
							.askUserForName(Resources
									.get("requestPartitionColumnName"),
									(String) obj);
					final String newName = name == null ? "" : name.trim();
					if (newName.length() == 0 || newName.equals(obj))
						return;
					try {
						PartitionTablePanel.this.getPartitionTable()
								.renameColumn((String) obj, newName);
					} catch (final PartitionException e) {
						// Never happens.
						throw new BioMartError(e);
					}
					int pos = columns.getIndexOf(obj);
					columns.insertElementAt(newName, pos);
					columns.removeElement(obj);
					PartitionTablePanel.this.updatePreview(previewData);
					pos = availableColumns.indexOf(obj);
					availableColumns.insertElementAt(newName, pos);
					availableColumns.removeElement(obj);
					badColumns.add(obj);
					badColumns.remove(newName);
				}
			}
		});
		renameButton.setEnabled(false);
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = columns.getSelectedItem();
				if (obj != null) {
					try {
						PartitionTablePanel.this.getPartitionTable()
								.removeColumn((String) obj);
					} catch (final PartitionException e) {
						// Never happens.
						throw new BioMartError(e);
					}
					columns.removeElement(obj);
					PartitionTablePanel.this.updatePreview(previewData);
					availableColumns.removeElement(obj);
					badColumns.add(obj);
				}
			}
		});
		removeButton.setEnabled(false);
		columnNamePanel.add(buttonPanel, this.fieldLastRowConstraints);
		// Listener on column drop-down to update modifier panel.
		columnList.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = columnList.getSelectedItem();
				if (obj != null) {
					final PartitionColumn col;
					try {
						col = PartitionTablePanel.this.getPartitionTable()
								.getColumn((String) obj);
					} catch (final PartitionException e) {
						// Never happens.
						throw new BioMartError(e);
					}
					if (col instanceof RegexColumn)
						PartitionTablePanel.this.columnMods = new RegexColumnModifierPanel(
								PartitionTablePanel.this.getPartitionTable(),
								(RegexColumn) col);
					else if (col instanceof FixedColumn)
						PartitionTablePanel.this.columnMods = new FixedColumnModifierPanel(
								PartitionTablePanel.this.getPartitionTable(),
								(FixedColumn) col);
					else
						throw new BioMartError(); // Never happens.
					// Remove and replace column mods panel.
					columnModsHolder.removeAll();
					columnModsHolder.add(PartitionTablePanel.this.columnMods);
					PartitionTablePanel.this.columnMods.reset();
					// Update update/reset buttons.
					resetColumnMods
							.setEnabled(PartitionTablePanel.this.columnMods
									.isMutable());
					updateColumnMods
							.setEnabled(PartitionTablePanel.this.columnMods
									.isMutable());
					renameButton.setEnabled(PartitionTablePanel.this.columnMods
							.isMutable());
					removeButton.setEnabled(PartitionTablePanel.this.columnMods
							.isMutable());
				}
			}
		});

		// Add sub-panels to main panel.
		field = new JPanel();
		field.add(columnNamePanel);
		field.add(columnModsPanel);
		this.add(field, this.fieldConstraints);

		// Subdivision panel.
		this.add(new JLabel(Resources.get("subdivisionLabel")),
				this.labelConstraints);
		field = new JPanel();
		final JButton listInsertButton = new BasicArrowButton(
				SwingConstants.EAST);
		final JButton listRemoveButton = new BasicArrowButton(
				SwingConstants.WEST);
		listInsertButton.setEnabled(false);
		listRemoveButton.setEnabled(false);

		// Left hand panel has drop-down and selector.
		final JPanel subdivNamePanel = new JPanel(new GridBagLayout());
		final JComboBox subdivList = new JComboBox();
		final List initialSubdivs = new ArrayList();
		final List initialUsedCols = new ArrayList();
		for (PartitionTable parentDiv = this.getPartitionTable(); parentDiv
				.getSubdivision() != null; parentDiv = parentDiv
				.getSubdivision()) {
			initialSubdivs.add(parentDiv.getSubdivisionName());
			initialUsedCols.addAll(parentDiv.getSubdivisionCols());
		}
		final DefaultComboBoxModel subdivs = new DefaultComboBoxModel(
				initialSubdivs.toArray());
		for (final Iterator i = initialUsedCols.iterator(); i.hasNext();)
			subdivs.addElement(i.next());
		subdivList.setModel(subdivs);
		subdivNamePanel.add(subdivList, this.fieldConstraints);
		// Add add/remove/rename buttons in sub-panel.
		final JPanel subdivButtonPanel = new JPanel();
		// Create and add buttons to buttonPanel.
		final JButton subdivAddButton = new JButton(Resources.get("addButton"));
		final JButton subdivRenameButton = new JButton(Resources
				.get("renameButton"));
		final JButton subdivRemoveButton = new JButton(Resources
				.get("removeButton"));
		subdivButtonPanel.add(subdivAddButton);
		subdivButtonPanel.add(subdivRenameButton);
		subdivButtonPanel.add(subdivRemoveButton);
		// Listeners on buttons.
		subdivAddButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				// Work out new subdivision.
				final String[] newSubdiv = PartitionTablePanel.this
						.showAddSubdivisionDialog((String[]) availableColumns
								.toArray());
				if (newSubdiv == null)
					return;
				final String subdivName = newSubdiv[0];
				final List cols = Arrays.asList(newSubdiv).subList(1,
						newSubdiv.length);
				// Update available list.
				for (final Iterator i = cols.iterator(); i.hasNext();)
					availableColumns.removeElement((String) i.next());
				// Navigate to last existing subdiv.
				PartitionTable parent = PartitionTablePanel.this
						.getPartitionTable();
				while (parent.getSubdivision() != null)
					parent = parent.getSubdivision();
				try {
					// Create subdiv.
					parent.setSubDivision(cols, subdivName);
					// Add to selector.
					subdivs.addElement(subdivName);
					// Select newly added subdiv.
					subdivs.setSelectedItem(subdivName);
				} catch (final PartitionException e) {
					StackTrace.showStackTrace(e);
				}
			}
		});
		subdivRenameButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = subdivList.getSelectedItem();
				if (obj != null) {
					// Pops up a dialog asking for new name.
					final String name = PartitionTablePanel.this
							.askUserForName(Resources.get("requestSubdivName"),
									(String) obj);
					final String newName = name == null ? "" : name.trim();
					if (newName.length() == 0 || newName.equals(obj))
						return;
					// Rename subdivision.
					try {
						PartitionTablePanel.this.selectedSubdivParent
								.renameSubdivision(newName);
						// Update drop-down.
						final int pos = subdivs.getIndexOf(obj);
						subdivs.insertElementAt(newName, pos);
						subdivs.removeElement(obj);
					} catch (final PartitionException e) {
						StackTrace.showStackTrace(e);
					}
				}
			}
		});
		subdivRenameButton.setEnabled(false);
		subdivRemoveButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = subdivList.getSelectedItem();
				if (obj != null) {
					// Update available list.
					for (PartitionTable parentDiv = PartitionTablePanel.this.selectedSubdivParent; parentDiv
							.getSubdivision() != null; parentDiv = parentDiv
							.getSubdivision()) {
						// Remove from list.
						subdivs.removeElement(parentDiv.getSubdivisionName());
						// Identify subdiv cols and add to available list.
						for (final Iterator i = parentDiv.getSubdivisionCols()
								.iterator(); i.hasNext();)
							availableColumns.addElement(i.next());
					}
					// Remove subdivision.
					PartitionTablePanel.this.selectedSubdivParent
							.removeSubDivision();
				}
			}
		});
		subdivRemoveButton.setEnabled(false);
		subdivNamePanel.add(subdivButtonPanel, this.fieldLastRowConstraints);
		// Listener on column drop-down to update modifier panel.
		subdivList.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = subdivList.getSelectedItem();
				if (obj != null) {
					// Locate the selected table.
					PartitionTablePanel.this.selectedSubdivParent = PartitionTablePanel.this
							.getPartitionTable();
					while (!PartitionTablePanel.this.selectedSubdivParent
							.getSubdivisionName().equals(obj))
						PartitionTablePanel.this.selectedSubdivParent = PartitionTablePanel.this.selectedSubdivParent
								.getSubdivision();
					// Set up the selection in the used cols.
					selectedColumns.removeAllElements();
					for (final Iterator i = PartitionTablePanel.this.selectedSubdivParent
							.getSubdivisionCols().iterator(); i.hasNext();)
						selectedColumns.addElement(i.next());
					// Enable list insert (and maybe remove) buttons.
					listInsertButton.setEnabled(true);
					listRemoveButton.setEnabled(selectedColumns.getSize() > 1);
					// Enable rename/remove buttons.
					subdivRenameButton.setEnabled(true);
					subdivRemoveButton.setEnabled(true);
				} else {
					listInsertButton.setEnabled(false);
					listRemoveButton.setEnabled(false);
					subdivRenameButton.setEnabled(false);
					subdivRemoveButton.setEnabled(false);
				}
			}
		});

		// Sub-panel on right describes subdivided columns.
		final Box listPanel = Box.createHorizontalBox();

		// Create the table column list, and the buttons
		// to move columns to/from the selected column list.
		final JList availableColList = new JList(availableColumns);

		// Create the key column list, and the buttons to
		// move columns to/from the table columns list.
		final JList selectedColList = new JList(selectedColumns);

		// Left-hand side goes the table columns that are unused.
		final JPanel leftListPanel = new JPanel(new BorderLayout());
		// Label at the top.
		leftListPanel.add(new JLabel(Resources.get("columnsAvailableLabel")),
				BorderLayout.PAGE_START);
		// Available columns list in the middle.
		leftListPanel.add(new JScrollPane(availableColList),
				BorderLayout.CENTER);
		leftListPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		final Box leftButtonPanel = Box.createVerticalBox();
		leftButtonPanel.add(listInsertButton);
		leftButtonPanel.add(listRemoveButton);
		leftButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		leftListPanel.add(leftButtonPanel, BorderLayout.LINE_END);
		listPanel.add(leftListPanel);

		// Right-hand side goes the key columns that are used.
		final JPanel rightListPanel = new JPanel(new BorderLayout());
		// Label at the top.
		rightListPanel.add(new JLabel(Resources.get("keyColumnsLabel")),
				BorderLayout.PAGE_START);
		// Key columns in the middle.
		rightListPanel.add(new JScrollPane(selectedColList),
				BorderLayout.CENTER);
		rightListPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		listPanel.add(rightListPanel);

		// Intercept the insert/remove buttons
		listInsertButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = availableColList.getSelectedValue();
				if (selected != null) {
					// Move a column from table to key.
					selectedColumns.addElement(selected);
					availableColumns.removeElement(selected);
					PartitionTablePanel.this.selectedSubdivParent
							.getSubdivisionCols().add((String) selected);
					listRemoveButton.setEnabled(true);
				}
			}
		});
		listRemoveButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = selectedColList.getSelectedValue();
				if (selected != null) {
					// Move a column from key to table.
					if (!badColumns.contains(selected))
						availableColumns.addElement(selected);
					selectedColumns.removeElement(selected);
					PartitionTablePanel.this.selectedSubdivParent
							.getSubdivisionCols().remove((String) selected);
					if (selectedColumns.getSize() < 2)
						listRemoveButton.setEnabled(false);
				}
			}
		});

		// Add subdivision panel.
		listPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		field.add(subdivNamePanel);
		field.add(listPanel);
		this.add(field, this.fieldConstraints);

		// Add preview panel and update it too.
		this.add(new JLabel(Resources.get("previewLabel")),
				this.labelLastRowConstraints);
		final JTable table = new JTable(previewData);
		table.setEnabled(false);
		this.add(new JScrollPane(table), this.fieldLastRowConstraints);
	}

	private PartitionColumn showAddColumnDialog() {
		try {
			final AddPartitionColumnDialog dialog = new AddPartitionColumnDialog(
					this.getPartitionTable());
			if (dialog.definePartitionColumn())
				return dialog.create();
		} catch (final PartitionException pe) {
			StackTrace.showStackTrace(pe);
		}
		return null;
	}

	private String[] showAddSubdivisionDialog(final String[] available) {
		try {
			final AddPartitionSubdivisionDialog dialog = new AddPartitionSubdivisionDialog(
					available);
			if (dialog.definePartitionSubdivision())
				return dialog.toStringArray();
		} catch (final PartitionException pe) {
			StackTrace.showStackTrace(pe);
		}
		return null;
	}

	private void updatePreview(final DefaultTableModel previewData) {
		// Remove old data.
		while (previewData.getRowCount() > 0)
			previewData.removeRow(0);
		// Populate new data.
		try {
			this.getPartitionTable().prepareRows(null,
					PartitionTablePanel.PREVIEW_ROW_COUNT);
			while (this.getPartitionTable().nextRow()) {
				final PartitionRow row = this.getPartitionTable().currentRow();
				final List cols = new ArrayList();
				for (final Iterator i = this.getPartitionTable()
						.getColumnNames().iterator(); i.hasNext();) {
					final PartitionColumn col = this.getPartitionTable()
							.getColumn((String) i.next());
					cols.add(col.getValueForRow(row));
				}
				previewData.addRow(cols.toArray());
			}
		} catch (final PartitionException e) {
			StackTrace.showStackTrace(e);
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

	/**
	 * Find out what mart tab we are in.
	 * 
	 * @return the mart tab.
	 */
	public MartTab getMartTab() {
		return this.martTab;
	}

	/**
	 * Returns the partition table that this diagram represents.
	 * 
	 * @return the partition table this diagram represents.
	 */
	public PartitionTable getPartitionTable() {
		return this.partitionTable;
	}
}
