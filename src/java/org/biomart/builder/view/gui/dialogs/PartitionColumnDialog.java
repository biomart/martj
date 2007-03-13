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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import org.biomart.builder.controller.dialects.DatabaseDialect;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueList;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueRange;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Schema;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.StackTrace;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel.StringStringTablePanel;

/**
 * This dialog asks users what kind of partitioning they want to set up on a
 * column. According to the type they select, it asks other questions, such as
 * what values to use.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class PartitionColumnDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private JButton execute;

	private PartitionedColumnDefinition partitionType;

	private JComboBox type;

	private JComboBox columns;

	private TwoColumnTablePanel expressionAliasModel;

	private TwoColumnTablePanel valueAliasModel;

	/**
	 * Pop up a dialog asking how to partition a table.
	 * 
	 * @param executeButtonText
	 *            the text to use on the OK button.
	 * @param template
	 *            the template to use to populate the fields of the dialog.
	 * @param dsTable
	 *            the table we are partitioning.
	 * @param dsColumn
	 *            the column to preselect in the dropdown of columns.
	 */
	public PartitionColumnDialog(final String executeButtonText,
			final PartitionedColumnDefinition template,
			final DataSetTable dsTable, final DataSetColumn dsColumn) {
		// Creates the basic dialog.
		super();
		this.setTitle(Resources.get("partitionColumnDialogTitle"));
		this.setModal(true);

		// Create the content pane to store the create dialog panel.
		final GridBagLayout gridBag = new GridBagLayout();
		final JPanel content = new JPanel(gridBag);
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

		// Create the fields that will contain the user's choice and any
		// values they may enter.
		// The multi-values box.
		// The ranges box.
		final JLabel exprLabel = new JLabel(Resources
				.get("rangeExpressionsLabel"));
		this.expressionAliasModel = new StringStringTablePanel(
				(template != null && template instanceof ValueRange) ? ((ValueRange) template)
						.getRanges()
						: null) {
			private static final long serialVersionUID = 1L;

			private int alias = 1;

			public String getInsertButtonText() {
				return Resources.get("insertExprAliasButton");
			}

			public String getRemoveButtonText() {
				return Resources.get("removeExprAliasButton");
			}

			public String getFirstColumnHeader() {
				return Resources.get("expressionAliasTableAliasHeader");
			}

			public String getSecondColumnHeader() {
				return Resources.get("expressionAliasTableExpressionHeader");
			}

			public Object getNewRowFirstColumn() {
				return Resources.get("defaultExprName") + this.alias++;
			}
		};
		// The values box.
		final JLabel valueListLabel = new JLabel(Resources
				.get("valueListsLabel"));
		this.valueAliasModel = new StringStringTablePanel(
				(template != null && template instanceof ValueRange) ? ((ValueRange) template)
						.getRanges()
						: null) {
			private static final long serialVersionUID = 1L;

			private int alias = 1;

			public String getInsertButtonText() {
				return Resources.get("insertValueAliasButton");
			}

			public String getRemoveButtonText() {
				return Resources.get("removeValueAliasButton");
			}

			public String getFirstColumnHeader() {
				return Resources.get("valueAliasTableAliasHeader");
			}

			public String getSecondColumnHeader() {
				return Resources.get("valueAliasTableValueHeader");
			}

			public Object getNewRowFirstColumn() {
				return Resources.get("defaultValueName") + this.alias++;
			}
		};
		// Everything else.
		this.type = new JComboBox(new String[] {
				Resources.get("listPartitionOption"),
				Resources.get("rangePartitionOption") });
		this.columns = new JComboBox();
		this.columns.setRenderer(new ListCellRenderer() {
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final DataSetColumn col = (DataSetColumn) value;
				final JLabel label = new JLabel();
				if (col != null)
					label.setText(col.getModifiedName());
				label.setOpaque(true);
				label.setFont(list.getFont());
				if (isSelected) {
					label.setBackground(list.getSelectionBackground());
					label.setForeground(list.getSelectionForeground());
				} else {
					label.setBackground(list.getBackground());
					label.setForeground(list.getForeground());
				}
				return label;
			}
		});
		final Map sortedCols = new TreeMap();
		for (final Iterator i = dsTable.getColumns().iterator(); i.hasNext();) {
			final DataSetColumn col = (DataSetColumn) i.next();
			sortedCols.put(col.getModifiedName(), col);
		}
		for (final Iterator i = sortedCols.values().iterator(); i.hasNext();)
			this.columns.addItem(i.next());
		if (dsColumn != null)
			this.columns.setSelectedItem(dsColumn);

		// Create the update-from-database button and holder for it and
		// the value list table.
		final JButton updateDB = new JButton(Resources
				.get("updatePartitionButton"));
		updateDB.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				try {
					// Work out what we've already got.
					final Map existingValues = PartitionColumnDialog.this.valueAliasModel
							.getValues();
					// Read the values from the database.
					final Set dbValues = new HashSet();
					// First, make a set of all input schemas. We use a set to
					// prevent duplicates.
					DataSetColumn dsCol = PartitionColumnDialog.this
							.getColumn();
					while (dsCol instanceof InheritedColumn)
						dsCol = ((InheritedColumn) dsCol).getInheritedColumn();
					final Column col = ((WrappedColumn) dsCol)
							.getWrappedColumn();
					final Schema schema = col.getTable().getSchema();
					final DatabaseDialect dd = DatabaseDialect
							.getDialect(schema);
					if (dd != null) {
						if (schema.getPartitions().isEmpty())
							dbValues.addAll(dd.executeSelectDistinct(
									((JDBCSchema) schema).getDatabaseSchema(),
									col));
						else
							for (final Iterator i = schema.getPartitions()
									.keySet().iterator(); i.hasNext();)
								dbValues.addAll(dd.executeSelectDistinct(
										(String) i.next(), col));
					}
					// Combine the two to create an updated list.
					final Map newValues = new TreeMap(existingValues);
					for (final Iterator i = newValues.entrySet().iterator(); i
							.hasNext();) {
						final Map.Entry entry = (Map.Entry) i.next();
						if (!dbValues.contains(entry.getValue()))
							i.remove();
					}
					for (final Iterator i = dbValues.iterator(); i.hasNext();) {
						final String value = (String) i.next();
						if (!newValues.containsValue(value))
							newValues.put(value, value);
					}
					// Update the table contents.
					PartitionColumnDialog.this.valueAliasModel
							.setValues(newValues);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
		final Box valueListPanel = Box.createVerticalBox();
		valueListPanel.add(this.valueAliasModel);
		valueListPanel.add(updateDB);

		// Make the drop-down type choice change which value and nullable
		// options appear.
		this.type.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final String selectedItem = (String) PartitionColumnDialog.this.type
						.getSelectedItem();
				// Range?
				if (selectedItem.equals(Resources.get("rangePartitionOption"))) {
					// Visible range.
					exprLabel.setVisible(true);
					PartitionColumnDialog.this.expressionAliasModel
							.setVisible(true);
					// Invisible list.
					valueListLabel.setVisible(false);
					valueListPanel.setVisible(false);
				}
				// List?
				else if (selectedItem.equals(Resources
						.get("listPartitionOption"))) {
					// Visible range.
					valueListLabel.setVisible(true);
					valueListPanel.setVisible(true);
					// Invisible list.
					exprLabel.setVisible(false);
					PartitionColumnDialog.this.expressionAliasModel
							.setVisible(false);
				}

				// Update the dialog size to fit the new fields.
				PartitionColumnDialog.this.pack();
			}
		});

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(executeButtonText);

		// Add the partition type label and field to the dialog.
		JLabel label = new JLabel(Resources.get("partitionedColumnLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(this.columns);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the partition type label and field to the dialog.
		label = new JLabel(Resources.get("partitionTypeLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.type);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the value list label and field to the dialog.
		gridBag.setConstraints(valueListLabel, labelConstraints);
		content.add(valueListLabel);
		field = new JPanel();
		field.add(valueListPanel);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the expression label and field to the dialog.
		gridBag.setConstraints(exprLabel, labelConstraints);
		content.add(exprLabel);
		field = new JPanel();
		field.add(this.expressionAliasModel);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add a blank label and the buttons to the dialog.
		label = new JLabel();
		gridBag.setConstraints(label, labelLastRowConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.cancel);
		field.add(this.execute);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		content.add(field);

		// Intercept the cancel button and use it to close this
		// dialog without making any changes.
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				PartitionColumnDialog.this.partitionType = null;
				PartitionColumnDialog.this.hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				PartitionColumnDialog.this.partitionType = PartitionColumnDialog.this
						.createPartitionTypeFromSettings();
				if (PartitionColumnDialog.this.partitionType != null)
					PartitionColumnDialog.this.hide();
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Reset the fields to their default values.
		this.copySettingsFromPartitionType(template);

		// Set the size of the dialog.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	private PartitionedColumnDefinition createPartitionTypeFromSettings() {
		// If we can't validate it, we can't create it.
		if (!this.validateFields())
			return null;

		try {
			// Attempt to create the appropriate type.
			final String type = (String) this.type.getSelectedItem();

			// Range?
			if (type.equals(Resources.get("rangePartitionOption"))) {
				final String[] values = (String[]) this.expressionAliasModel
						.getValues().keySet().toArray(new String[0]);
				// Check that none of the range names have non-allowable symbols
				// or start/end in underscores.
				boolean allOK = true;
				for (int i = 0; i < values.length && allOK; i++)
					allOK = values[i].matches("^[^_]\\w+[^_]$");
				// If there any messages, display them.
				if (allOK
						|| JOptionPane.showConfirmDialog(null, Resources
								.get("partValueWithSpecialChar"), Resources
								.get("questionTitle"),
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					return new ValueRange(this.expressionAliasModel.getValues());
			}
			// List?
			else if (type.equals(Resources.get("listPartitionOption"))) {
				final String[] values = (String[]) this.valueAliasModel
						.getValues().keySet().toArray(new String[0]);
				// Check that none of the range names have non-allowable symbols
				// or start/end in underscores.
				boolean allOK = true;
				for (int i = 0; i < values.length && allOK; i++)
					allOK = values[i].matches("^[^_]\\w+[^_]$");
				// If there any messages, display them.
				if (allOK
						|| JOptionPane.showConfirmDialog(null, Resources
								.get("partValueWithSpecialChar"), Resources
								.get("questionTitle"),
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					return new ValueList(this.valueAliasModel.getValues());
			}

			// Eh? Don't know what this is!
			else
				throw new BioMartError();
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}

		// If we get here, we failed, so act as if validation failed.
		return null;
	}

	private void copySettingsFromPartitionType(
			final PartitionedColumnDefinition template) {
		// Range?
		if (template instanceof ValueRange)
			this.type.setSelectedItem(Resources.get("rangePartitionOption"));

		// List is default.
		else
			this.type.setSelectedItem(Resources.get("listPartitionOption"));
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		final List messages = new ArrayList();

		// We must have a partition type!
		if (this.type.getSelectedIndex() == -1)
			messages.add(Resources.get("fieldIsEmpty", Resources.get("type")));

		// Work out which partition type is currently selected.
		final String selectedItem = (String) this.type.getSelectedItem();

		// Validate other fields.
		if (selectedItem.equals(Resources.get("rangePartitionOption")))
			// Check we have an expression.
			if (this.expressionAliasModel.getValues().isEmpty())
				messages.add(Resources.get("expressionAliasMissing"));

			// Validate other fields.
			else if (selectedItem.equals(Resources.get("listPartitionOption")))
				// Check we have an expression.
				if (this.valueAliasModel.getValues().isEmpty())
					messages.add(Resources.get("valueAliasMissing"));

		// If there any messages, display them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	/**
	 * Get the partition definition the user set up in this dialog.
	 * 
	 * @return the partition definition.
	 */
	public PartitionedColumnDefinition getPartitionType() {
		return this.partitionType;
	}

	/**
	 * Get the column the user selected to partition on.
	 * 
	 * @return the column to partition.
	 */
	public DataSetColumn getColumn() {
		return (DataSetColumn) this.columns.getSelectedItem();
	}
}
