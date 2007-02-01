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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.SingleValue;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.UniqueValues;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueCollection;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.StackTrace;

/**
 * This dialog asks users what kind of partitioning they want to set up on a
 * column. According to the type they select, it asks other questions, such as
 * what values to use.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public class PartitionColumnDialog extends JDialog {
	private static final long serialVersionUID = 1;

	/**
	 * This opens a dialog in order for the user to create a new partition type.
	 * It returns that type, or null if they cancelled it.
	 * 
	 * @return the newly created partition type, or null if the dialog was
	 *         cancelled.
	 */
	public static PartitionedColumnDefinition createPartitionedColumn() {
		final PartitionColumnDialog dialog = new PartitionColumnDialog(
				Resources.get("createPartitionButton"), null);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		return dialog.partitionType;
	}

	/**
	 * This opens a dialog in order for the user to edit an existing partition
	 * type. Actually what it does is use an existing type to provide a template
	 * to create a new type, so it is an entirely new type that is returned -
	 * the existing one is untouched. If it returns null, the user cancelled the
	 * dialog.
	 * 
	 * @param template
	 *            the template to copy settings from. Can be null.
	 * @return the replacement, updated, partition type, or null if the dialog
	 *         was cancelled.
	 */
	public static PartitionedColumnDefinition updatePartitionedColumn(
			final PartitionedColumnDefinition template) {
		final PartitionColumnDialog dialog = new PartitionColumnDialog(
				Resources.get("updatePartitionButton"), template);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		return dialog.partitionType;
	}

	private JButton cancel;

	private JButton execute;

	private JTextArea multiValue;

	private JCheckBox nullable;

	private PartitionedColumnDefinition partitionType;

	private JTextField singleValue;

	private JComboBox type;

	private PartitionColumnDialog(final String executeButtonText, 
			final PartitionedColumnDefinition template) {
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
		final JLabel valueLabel = new JLabel(Resources.get("valuesLabel"));
		this.singleValue = new JTextField(30);
		this.multiValue = new JTextArea(5, 30);
		this.type = new JComboBox(new String[] {
				Resources.get("singlePartitionOption"),
				Resources.get("collectionPartitionOption"),
				Resources.get("uniquePartitionOption") });
		this.nullable = new JCheckBox();

		// Make the drop-down type choice change which value and nullable
		// options appear. 
		this.type.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final String selectedItem = (String) PartitionColumnDialog.this.type
						.getSelectedItem();

				// Single partitions have a single value field, with a nullable
				// box saying 'use null'.
				if (selectedItem.equals(Resources.get("singlePartitionOption"))) {
					valueLabel.setVisible(true);
					PartitionColumnDialog.this.singleValue.setVisible(true);
					PartitionColumnDialog.this.multiValue.setVisible(false);
					PartitionColumnDialog.this.nullable.setText(Resources
							.get("useNullLabel"));
					PartitionColumnDialog.this.nullable.setVisible(true);
				}

				// Multi-value partitions have a multi value field, with a
				// nullable
				// box saying 'include null'.
				else if (selectedItem.equals(Resources
						.get("collectionPartitionOption"))) {
					valueLabel.setVisible(true);
					PartitionColumnDialog.this.singleValue.setVisible(false);
					PartitionColumnDialog.this.multiValue.setVisible(true);
					PartitionColumnDialog.this.nullable.setText(Resources
							.get("includeNullLabel"));
					PartitionColumnDialog.this.nullable.setVisible(true);
				}

				// Other kinds of partition have no value or nullable fields.
				else {
					valueLabel.setVisible(false);
					PartitionColumnDialog.this.singleValue.setVisible(false);
					PartitionColumnDialog.this.multiValue.setVisible(false);
					PartitionColumnDialog.this.nullable.setVisible(false);
				}
				
				// Update the dialog size to fit the new fields.
				PartitionColumnDialog.this.pack();
			}
		});

		// Whenever nullable is selected, the values box may change.
		this.nullable.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {

				// If it selected, the single value field can't be used.
				if (PartitionColumnDialog.this.nullable.isSelected()) {
					PartitionColumnDialog.this.singleValue.setText(null);
					PartitionColumnDialog.this.singleValue.setEnabled(false);
				}

				// Otherwise, it can.
				else
					PartitionColumnDialog.this.singleValue.setEnabled(true);
			}
		});

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(executeButtonText);

		// Add the partition type label and field to the dialog.
		JLabel label = new JLabel(Resources.get("partitionTypeLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(this.type);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the value label and field to the dialog.
		gridBag.setConstraints(valueLabel, labelConstraints);
		content.add(valueLabel);
		field = new JPanel();
		field.add(this.singleValue);
		field.add(this.multiValue);
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

			// Single-value uses the single value and/or nullable.
			if (type.equals(Resources.get("singlePartitionOption")))
				return new SingleValue(this.singleValue.getText().trim(),
						this.nullable.isSelected());

			// Multi-value uses the multi values, and/or nullable.
			else if (type.equals(Resources.get("collectionPartitionOption"))) {
				final String[] values = this.multiValue.getText().trim().split(
						System.getProperty("line.separator"));
				return new ValueCollection(Arrays.asList(values), this.nullable
						.isSelected());
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

	private void copySettingsFromPartitionType(final PartitionedColumnDefinition template) {
		// If an existing single partition has been specified, populate
		// its details into the box.
		if (template instanceof SingleValue) {
			final SingleValue sv = (SingleValue) template;
			this.type.setSelectedItem(Resources.get("singlePartitionOption"));
			this.singleValue.setText(sv.getValue());
			if (sv.getIncludeNull())
				this.nullable.doClick();
		}

		// Else, do the same for an existing multi-value collection partition.
		else if (template instanceof ValueCollection) {
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

		// If it's single...
		if (selectedItem.equals(Resources.get("singlePartitionOption"))) {
			// Check we have a value, or nullable is selected.
			if (this.isEmpty(this.singleValue.getText())
					&& !this.nullable.isSelected())
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("value")));
		}

		// If it's multi...
		else if (selectedItem
				.equals(Resources.get("collectionPartitionOption")))
			// Check we have a value, or nullable is selected.
			if (this.isEmpty(this.multiValue.getText())
					&& !this.nullable.isSelected())
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("value")));

		// If there any messages, display them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}
}
