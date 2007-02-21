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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.border.EtchedBorder;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.UniqueValues;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueCollection;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueRange;
import org.biomart.common.exceptions.BioMartError;
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

	private JTextArea multiValue;

	private JPanel multiValueHolder;

	private JCheckBox nullable;

	private PartitionedColumnDefinition partitionType;

	private JComboBox type;

	private JComboBox columns;

	private TwoColumnTablePanel expressionAliasModel;
	
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
		final JLabel valueLabel = new JLabel(Resources.get("valuesLabel"));
		this.multiValue = new JTextArea(5, 30);
		this.multiValueHolder = new JPanel(new BorderLayout());
		this.multiValueHolder.setAlignmentX(LEFT_ALIGNMENT);
		this.multiValueHolder.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		this.multiValueHolder.add(new JScrollPane(this.multiValue),
				BorderLayout.CENTER);
		// The ranges box.
		final JLabel exprLabel = new JLabel(Resources
				.get("rangeExpressionsLabel"));
		this.expressionAliasModel = new StringStringTablePanel(
				(template != null && template instanceof ValueRange) ? ((ValueRange) template).getRanges()
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
				return Resources.get("defaultExprName")+this.alias++;
			}
		};
		// Everything else.
		this.type = new JComboBox(new String[] {
				Resources.get("uniquePartitionOption"),
				Resources.get("collectionPartitionOption"),
				Resources.get("rangePartitionOption") });
		this.nullable = new JCheckBox(Resources.get("includeNullLabel"));
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

		// Make the drop-down type choice change which value and nullable
		// options appear.
		this.type.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final String selectedItem = (String) PartitionColumnDialog.this.type
						.getSelectedItem();

				// Multi-value partitions have a multi value field, with a
				// nullable
				// box saying 'include null'.
				if (selectedItem.equals(Resources
						.get("collectionPartitionOption"))) {
					// Visible value.
					valueLabel.setVisible(true);
					PartitionColumnDialog.this.multiValueHolder
							.setVisible(true);
					PartitionColumnDialog.this.nullable.setVisible(true);
					// Invisible range.
					exprLabel.setVisible(false);
					PartitionColumnDialog.this.expressionAliasModel
							.setVisible(false);
				}

				// Range?
				else if (selectedItem.equals(Resources
						.get("rangePartitionOption"))) {
					// Visible range.
					exprLabel.setVisible(true);
					PartitionColumnDialog.this.expressionAliasModel
							.setVisible(true);
					// Invisible value.
					valueLabel.setVisible(false);
					PartitionColumnDialog.this.multiValueHolder
							.setVisible(false);
					PartitionColumnDialog.this.nullable.setVisible(false);
				}

				// Other kinds of partition have no value or nullable fields.
				else {
					// Invisible value.
					valueLabel.setVisible(false);
					PartitionColumnDialog.this.multiValueHolder
							.setVisible(false);
					PartitionColumnDialog.this.nullable.setVisible(false);
					// Invisible range.
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

		// Add the value label and field to the dialog.
		gridBag.setConstraints(valueLabel, labelConstraints);
		content.add(valueLabel);
		field = new JPanel();
		field.add(this.multiValueHolder);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the expression label and field to the dialog.
		gridBag.setConstraints(exprLabel, labelConstraints);
		content.add(exprLabel);
		field = new JPanel();
		field.add(this.expressionAliasModel);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add a blank label and the nullable checkbox to the dialog.
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.nullable);
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
	}

	private PartitionedColumnDefinition createPartitionTypeFromSettings() {
		// If we can't validate it, we can't create it.
		if (!this.validateFields())
			return null;

		try {
			// Attempt to create the appropriate type.
			final String type = (String) this.type.getSelectedItem();

			// Multi-value uses the multi values, and/or nullable.
			if (type.equals(Resources.get("collectionPartitionOption"))) {
				final String[] values = this.multiValue.getText().trim().split(
						System.getProperty("line.separator"));
				// Check that none of the entries have non-allowable symbols
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
					return new ValueCollection(Arrays.asList(values),
							this.nullable.isSelected());
			}

			// Range?
			else if (type.equals(Resources.get("rangePartitionOption"))) {
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
					return new ValueRange(this.expressionAliasModel
							.getValues());
			}

			// Unique values doesn't require anything.
			else if (type.equals(Resources.get("uniquePartitionOption")))
				return new UniqueValues();

			// Eh? Don't know what this is!
			else
				throw new BioMartError();
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}

		// If we get here, we failed, so act as if validation failed.
		return null;
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	private void copySettingsFromPartitionType(
			final PartitionedColumnDefinition template) {
		// Multi value collection? Copy the values out of it.
		if (template instanceof ValueCollection) {
			final ValueCollection vc = (ValueCollection) template;
			if (vc.getIncludeNull())
				this.nullable.doClick();
			this.type.setSelectedItem(Resources
					.get("collectionPartitionOption"));
			// Values appear one-per-line.
			final StringBuffer sb = new StringBuffer();
			for (final Iterator i = vc.getValues().iterator(); i.hasNext();) {
				sb.append((String) i.next());
				if (i.hasNext())
					sb.append(System.getProperty("line.separator"));
			}
			this.multiValue.setText(sb.toString());
		}

		// Range?
		else if (template instanceof ValueRange)
			this.type.setSelectedItem(Resources.get("rangePartitionOption"));

		// Otherwise, select the unique partition option as default.
		else
			this.type.setSelectedItem(Resources.get("uniquePartitionOption"));
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		final List messages = new ArrayList();

		// We must have a partition type!
		if (this.type.getSelectedIndex() == -1)
			messages.add(Resources.get("fieldIsEmpty", Resources.get("type")));

		// Work out which partition type is currently selected.
		final String selectedItem = (String) this.type.getSelectedItem();

		// If it's multi...
		if (selectedItem.equals(Resources.get("collectionPartitionOption")))
			// Check we have a value, or nullable is selected.
			if (this.isEmpty(this.multiValue.getText())
					&& !this.nullable.isSelected())
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("value")));

		// Validate other fields.
		if (selectedItem.equals(Resources.get("rangePartitionOption")))
			// Check we have an expression.
			if (this.expressionAliasModel.getValues().isEmpty())
				messages.add(Resources.get("expressionAliasMissing"));

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
