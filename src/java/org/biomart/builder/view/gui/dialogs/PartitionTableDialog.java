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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableModel;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.PartitionTable;
import org.biomart.builder.model.PartitionTable.PartitionColumn;
import org.biomart.builder.model.PartitionTable.PartitionRow;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * A dialog which allows the user to turn a dataset into a partition table and
 * modify the way it behaves as such.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class PartitionTableDialog extends JDialog {
	private static final long serialVersionUID = 1;

	/**
	 * How many rows to show in the preview panel.
	 */
	public static int PREVIEW_ROWS = 10;

	private DefaultListModel availableColumns;

	private DefaultListModel selectedColumns;

	private DefaultTableModel previewData;

	private JCheckBox partition;

	private JButton execute;

	private JButton cancel;

	private JTextField previewRowCount;

	private boolean cancelled;

	/**
	 * Pop up a dialog to define or edit a dataset partition table data.
	 * 
	 * @param dataset
	 *            the dataset.
	 */
	public PartitionTableDialog(final DataSet dataset) {
		// Create the base dialog.
		super();
		this.setTitle(Resources.get("partitionTableDialogTitle"));
		this.setModal(true);
		this.cancelled = true;

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

		// Show/hide pane.
		final JPanel showHide = new JPanel(new GridBagLayout());
		showHide.setVisible(false);

		// First line is a checkbox. If unchecked, partitioning is off
		// on this table. If checked, then it is on. This shows/hides
		// the remaining two panels.
		this.partition = new JCheckBox(Resources.get("partitionTableCheckbox"));
		content.add(this.partition, fieldConstraints);
		content.add(showHide, fieldConstraints);

		// Put the two halves of the dialog side-by-side in a horizontal box.
		final Box colPanel = Box.createHorizontalBox();
		showHide.add(new JLabel(Resources.get("partitionTableColumnLabel")),
				labelConstraints);
		showHide.add(colPanel, fieldConstraints);

		// Create the available column list, and the buttons
		// to move columns to/from the selected column list.
		this.availableColumns = new DefaultListModel();
		final JList tabColList = new JList(this.availableColumns);
		final JButton insertButton = new BasicArrowButton(SwingConstants.EAST);
		final JButton removeButton = new BasicArrowButton(SwingConstants.WEST);

		// Create the selected column list, and the buttons to
		// move columns to/from the table columns list.
		this.selectedColumns = new DefaultListModel();
		final JList keyColList = new JList(this.selectedColumns);
		final JButton upButton = new BasicArrowButton(SwingConstants.NORTH);
		final JButton downButton = new BasicArrowButton(SwingConstants.SOUTH);

		// Create the regex fields.
		final JTextField matchRegex = new JTextField(20);
		final JTextField replaceRegex = new JTextField(20);
		final JButton regexUpdateButton = new JButton(Resources
				.get("updateButton"));
		final JButton regexResetButton = new JButton(Resources
				.get("resetButton"));

		// Left-hand side goes the table columns that are unused.
		final JPanel leftPanel = new JPanel(new BorderLayout());
		// Label at the top.
		leftPanel.add(new JLabel(Resources.get("columnsAvailableLabel")),
				BorderLayout.PAGE_START);
		// Table columns list in the middle.
		leftPanel.add(new JScrollPane(tabColList), BorderLayout.CENTER);
		leftPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		final Box leftButtonPanel = Box.createVerticalBox();
		leftButtonPanel.add(insertButton);
		leftButtonPanel.add(removeButton);
		leftButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		leftPanel.add(leftButtonPanel, BorderLayout.LINE_END);
		colPanel.add(leftPanel);

		// Right-hand side goes the key columns that are used.
		final JPanel rightPanel = new JPanel(new BorderLayout());
		// Label at the top.
		rightPanel.add(new JLabel(Resources.get("selectedColumnsLabel")),
				BorderLayout.PAGE_START);
		// Key columns in the middle.
		rightPanel.add(new JScrollPane(keyColList), BorderLayout.CENTER);
		rightPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		final Box rightButtonPanel = Box.createVerticalBox();
		rightButtonPanel.add(upButton);
		rightButtonPanel.add(downButton);
		rightButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		rightPanel.add(rightButtonPanel, BorderLayout.LINE_END);
		colPanel.add(rightPanel);

		// Far side lists regex info.
		final JPanel regexPanel = new JPanel(new BorderLayout());
		// Label at the top.
		regexPanel.add(new JLabel(Resources.get("regexColumnsLabel")),
				BorderLayout.PAGE_START);
		regexPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Regex fields in their own sub panel.
		final JPanel subRegexPanel = new JPanel(new GridBagLayout());
		subRegexPanel.add(new JLabel(Resources
				.get("regexColumnMatchRegexLabel")), labelConstraints);
		subRegexPanel.add(matchRegex, fieldConstraints);
		subRegexPanel.add(new JLabel(Resources
				.get("regexColumnReplaceRegexLabel")), labelLastRowConstraints);
		subRegexPanel.add(replaceRegex, fieldLastRowConstraints);
		regexPanel.add(subRegexPanel, BorderLayout.CENTER);
		// Buttons at bottom.
		final JPanel regexButtonPanel = new JPanel();
		regexButtonPanel.add(regexResetButton);
		regexButtonPanel.add(regexUpdateButton);
		regexButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		regexPanel.add(regexButtonPanel, BorderLayout.PAGE_END);
		colPanel.add(regexPanel);

		// On select of column in third column, update regex data,
		// or disable regex fields.
		keyColList.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(final ListSelectionEvent e) {
						// Ignore multiple events.
						if (e.getValueIsAdjusting())
							return;
						// Check if selection valid (single selection only).
						final boolean valid = keyColList.getSelectedValue() != null
								&& !keyColList.getSelectedValue().equals(
										PartitionTable.DIV_COLUMN);
						matchRegex.setEnabled(valid);
						replaceRegex.setEnabled(valid);
						regexResetButton.setEnabled(valid);
						regexUpdateButton.setEnabled(valid);
						// If selection valid, update details by clicking on
						// reset button.
						if (valid)
							regexResetButton.doClick();
						else {
							// If selection invalid, clear fields.
							matchRegex.setText(null);
							replaceRegex.setText(null);
						}
					}
				});
		// Default is all disabled.
		matchRegex.setEnabled(false);
		replaceRegex.setEnabled(false);
		regexResetButton.setEnabled(false);
		regexUpdateButton.setEnabled(false);

		// Intercept the insert/remove buttons
		insertButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = tabColList.getSelectedValue();
				if (selected != null) {
					// Move a column from table to key.
					PartitionTableDialog.this.selectedColumns
							.addElement(selected);
					try {
						dataset.asPartitionTable().setSelectedColumnNames(
								PartitionTableDialog.this
										.getNewSelectedColumns());
					} catch (final PartitionException pe) {
						StackTrace.showStackTrace(pe);
					}
					PartitionTableDialog.this.updateAvailableColumns(dataset);
					PartitionTableDialog.this.updatePreviewPanel(dataset);
				}
			}
		});
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					// Move a column from key to table.
					PartitionTableDialog.this.selectedColumns
							.removeElement(selected);
					try {
						dataset.asPartitionTable().setSelectedColumnNames(
								PartitionTableDialog.this
										.getNewSelectedColumns());
					} catch (final PartitionException pe) {
						StackTrace.showStackTrace(pe);
					}
					PartitionTableDialog.this.updateAvailableColumns(dataset);
					PartitionTableDialog.this.updatePreviewPanel(dataset);
				}
			}
		});

		// Intercept the up/down buttons
		upButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					final int currIndex = keyColList.getSelectedIndex();
					if (currIndex > 0) {
						// Swap the selected item with the one above it.
						final Object swap = PartitionTableDialog.this.selectedColumns
								.get(currIndex - 1);
						PartitionTableDialog.this.selectedColumns.setElementAt(
								selected, currIndex - 1);
						PartitionTableDialog.this.selectedColumns.setElementAt(
								swap, currIndex);
						// Select the selected item again, as it will
						// have moved.
						keyColList.setSelectedIndex(currIndex - 1);
						PartitionTableDialog.this.updatePreviewPanel(dataset);
					}
				}
			}
		});
		downButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					final int currIndex = keyColList.getSelectedIndex();
					if (currIndex < PartitionTableDialog.this.selectedColumns
							.size() - 1) {
						// Swap the selected item with the one below it.
						final Object swap = PartitionTableDialog.this.selectedColumns
								.get(currIndex + 1);
						PartitionTableDialog.this.selectedColumns.setElementAt(
								selected, currIndex + 1);
						PartitionTableDialog.this.selectedColumns.setElementAt(
								swap, currIndex);
						// Select the selected item again, as it will
						// have moved.
						keyColList.setSelectedIndex(currIndex + 1);
						PartitionTableDialog.this.updatePreviewPanel(dataset);
					}
				}
			}
		});

		// Regex update/reset buttons.
		regexUpdateButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				try {
					final PartitionColumn col = dataset.asPartitionTable()
							.getSelectedColumn(
									(String) keyColList.getSelectedValue());
					col.setRegexMatch(matchRegex.getText());
					col.setRegexReplace(replaceRegex.getText());
				} catch (final PartitionException pe) {
					StackTrace.showStackTrace(pe);
				}
				// Regex update button also updates preview.
				PartitionTableDialog.this.updatePreviewPanel(dataset);
			}
		});
		regexResetButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				try {
					final PartitionColumn col = dataset.asPartitionTable()
							.getSelectedColumn(
									(String) keyColList.getSelectedValue());
					matchRegex.setText(col.getRegexMatch());
					replaceRegex.setText(col.getRegexReplace());
				} catch (final PartitionException pe) {
					StackTrace.showStackTrace(pe);
				}
			}
		});

		// Next area is preview row count.
		this.previewRowCount = new JTextField(5);
		this.previewRowCount.setText("" + PartitionTableDialog.PREVIEW_ROWS);
		showHide.add(new JLabel(Resources.get("previewRowCountLabel")),
				labelConstraints);
		showHide.add(this.previewRowCount, fieldConstraints);
		this.previewRowCount.getDocument().addDocumentListener(
				new DocumentListener() {
					public void changedUpdate(final DocumentEvent e) {
						this.changed();
					}

					public void insertUpdate(final DocumentEvent e) {
						this.changed();
					}

					public void removeUpdate(final DocumentEvent e) {
						this.changed();
					}

					private void changed() {
						PartitionTableDialog.this.updatePreviewPanel(dataset);
					}
				});

		// Second area (bottom) is preview panel. Populated on opening,
		// and on each change.
		this.previewData = new DefaultTableModel();
		final JTable previewTable = new JTable(this.previewData);
		previewTable.setGridColor(Color.LIGHT_GRAY); // Mac OSX.
		previewTable.setEnabled(false);
		previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		previewTable.setPreferredScrollableViewportSize(colPanel
				.getPreferredSize());
		showHide.add(new JLabel(Resources.get("previewLabel")),
				labelLastRowConstraints);
		showHide.add(new JScrollPane(previewTable), fieldLastRowConstraints);

		// If the user rearranges the columns in the preview data,
		// mirror in the selected columns.
		previewTable.getColumnModel().addColumnModelListener(
				new TableColumnModelListener() {

					public void columnAdded(final TableColumnModelEvent e) {
						// Don't care.
					}

					public void columnMarginChanged(final ChangeEvent e) {
						// Don't care.
					}

					public void columnMoved(final TableColumnModelEvent e) {
						if (e.getFromIndex() == e.getToIndex())
							return;
						PartitionTableDialog.this.selectedColumns.clear();
						for (int i = 0; i < previewTable.getColumnCount(); i++)
							PartitionTableDialog.this.selectedColumns
									.addElement(previewTable.getColumnName(i));
						try {
							dataset.asPartitionTable().setSelectedColumnNames(
									PartitionTableDialog.this
											.getNewSelectedColumns());
						} catch (final PartitionException pe) {
							StackTrace.showStackTrace(pe);
						}
					}

					public void columnRemoved(final TableColumnModelEvent e) {
						// Don't care.
					}

					public void columnSelectionChanged(
							final ListSelectionEvent e) {
						// Don't care.
					}
				});

		// Intercept the partition checkbox to update the available cols
		// list.
		this.partition.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				showHide.setVisible(PartitionTableDialog.this.partition
						.isSelected());
				try {
					dataset
							.setPartitionTable(PartitionTableDialog.this.partition
									.isSelected());
					if (PartitionTableDialog.this.partition.isSelected()) {
						// Update available/selected cols and preview.
						PartitionTableDialog.this
								.updateSelectedColumns(dataset);
						PartitionTableDialog.this
								.updateAvailableColumns(dataset);
						PartitionTableDialog.this.updatePreviewPanel(dataset);
					}
				} catch (final PartitionException pe) {
					StackTrace.showStackTrace(pe);
					try {
						dataset.setPartitionTable(false);
					} catch (final PartitionException pe2) {
						StackTrace.showStackTrace(pe2);
					} finally {
						PartitionTableDialog.this.setVisible(false);
					}
				} finally {
					PartitionTableDialog.this.pack();
				}
			}
		});
		// Default view of data.
		if (dataset.isPartitionTable()) {
			this.partition.setSelected(true);
			showHide.setVisible(true);
			PartitionTableDialog.this.updateSelectedColumns(dataset);
			PartitionTableDialog.this.updateAvailableColumns(dataset);
			PartitionTableDialog.this.updatePreviewPanel(dataset);
		}

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
				PartitionTableDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				PartitionTableDialog.this.cancelled = false;
				PartitionTableDialog.this.setVisible(false);
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	private void updateAvailableColumns(final DataSet ds) {
		this.availableColumns.clear();
		if (ds.isPartitionTable())
			for (final Iterator i = ds.asPartitionTable().getAllColumnNames()
					.iterator(); i.hasNext();)
				this.availableColumns.addElement(i.next());
		for (final Iterator i = this.getNewSelectedColumns().iterator(); i
				.hasNext();)
			this.availableColumns.removeElement(i.next());
		this.availableColumns.addElement(PartitionTable.DIV_COLUMN);
	}

	private void updateSelectedColumns(final DataSet ds) {
		this.selectedColumns.clear();
		if (ds.isPartitionTable())
			for (final Iterator i = ds.asPartitionTable()
					.getSelectedColumnNames().iterator(); i.hasNext();)
				this.selectedColumns.addElement(i.next());
	}

	private List getNewSelectedColumns() {
		return Arrays.asList(this.selectedColumns.toArray());
	}

	private void updatePreviewPanel(final DataSet ds) {
		try {
			// Update ds with new ones.
			final List selectedCols = this.getNewSelectedColumns();
			ds.asPartitionTable().setSelectedColumnNames(selectedCols);
			// Update preview data column headers.
			this.previewData.setColumnIdentifiers(selectedCols.toArray());
			// Clear preview data model.
			while (this.previewData.getRowCount() > 0)
				this.previewData.removeRow(0);
			// No new cols? Don't go any further.
			final List trueSelectedCols = new ArrayList();
			for (final Iterator i = selectedCols.iterator(); i.hasNext();) {
				final String col = (String) i.next();
				if (!col.equals(PartitionTable.DIV_COLUMN))
					trueSelectedCols.add(col);
			}
			if (trueSelectedCols.size() < 1)
				return;
			// Get the rows.
			try {
				ds.asPartitionTable().prepareRows(null,
						Integer.parseInt(this.previewRowCount.getText()));
			} catch (final NumberFormatException nfe) {
				ds.asPartitionTable().prepareRows(null,
						PartitionTableDialog.PREVIEW_ROWS);
			}
			while (ds.asPartitionTable().nudgeRow()) {
				final PartitionRow row = ds.asPartitionTable().currentRow();
				final List rowData = new ArrayList();
				for (final Iterator i = selectedCols.iterator(); i.hasNext();) {
					final String col = (String) i.next();
					if (col.equals(PartitionTable.DIV_COLUMN))
						rowData.add("->");
					else
						rowData.add(row.getPartitionTable().getSelectedColumn(
								col).getValueForRow(row));
				}
				// Add an entry to the data model using list.toArray();
				this.previewData.addRow(rowData.toArray());
			}
		} catch (final PartitionException pe) {
			StackTrace.showStackTrace(pe);
		}
	}

	/**
	 * Open the dialog with the dataset details in it, allow the user to change
	 * those details, then return true if they were changed.
	 * 
	 * @param ds
	 *            the dataset to edit.
	 * @return <tt>true</tt> if changes were made.
	 */
	public static boolean modifyDataSet(final DataSet ds) {
		final boolean wasPartitionTable = ds.isPartitionTable();
		final List oldSelectedCols = wasPartitionTable ? new ArrayList(ds
				.asPartitionTable().getSelectedColumnNames())
				: Collections.EMPTY_LIST;
		final PartitionTableDialog pt = new PartitionTableDialog(ds);
		pt.setVisible(true);
		if (pt.cancelled)
			try {
				ds.setPartitionTable(wasPartitionTable);
				if (wasPartitionTable)
					ds.asPartitionTable().setSelectedColumnNames(
							oldSelectedCols);
			} catch (final PartitionException pe) {
				StackTrace.showStackTrace(pe);
			}
		return !pt.cancelled;
	}
}
