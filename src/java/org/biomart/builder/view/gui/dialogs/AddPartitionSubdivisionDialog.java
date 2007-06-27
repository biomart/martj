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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.common.resources.Resources;

/**
 * This dialog asks users to create a partition table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class AddPartitionSubdivisionDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private JButton execute;

	private JList cols;

	private JTextField name;

	/**
	 * Pops up a dialog to manage the partitions.
	 * 
	 * @return <tt>true</tt> if the user's actions led to a change in the
	 *         partitioning method.
	 */
	public boolean definePartitionSubdivision() {
		// Centre the dialog.
		this.setLocationRelativeTo(null);

		// Show the dialog.
		this.setVisible(true);

		// Return true if not cancelled - ie. values changed.
		return !this.cancelled;
	}

	/**
	 * Creates but does not show a table partition dialog.
	 * 
	 * @param availableCols
	 *            the list of available columns to show.
	 */
	public AddPartitionSubdivisionDialog(final String[] availableCols) {
		// Creates the basic dialog.
		super();
		this.setTitle(Resources.get("createPartitionSubdivisionDialogTitle"));
		this.setModal(true);

		// Remembers the dataset tabset this dialog is referring to.
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

		// Create name field.
		content.add(new JLabel(Resources.get("partitionSubdivisionNameLabel")),
				labelConstraints);
		this.name = new JTextField(20);
		content.add(this.name, fieldConstraints);

		// Create the column selector.
		this.cols = new JList(availableCols);
		this.cols.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		content.add(new JLabel(Resources.get("partitionSubdivisionColsLabel")),
				labelConstraints);
		content.add(this.cols, fieldConstraints);

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(Resources.get("addButton"));

		// Add the buttons to the dialog.
		content.add(new JLabel(), labelLastRowConstraints);
		final JPanel field = new JPanel();
		field.add(this.cancel);
		field.add(this.execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the cancel button and use it to close this
		// dialog without making any changes.
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				AddPartitionSubdivisionDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (AddPartitionSubdivisionDialog.this.validateFields()) {
					AddPartitionSubdivisionDialog.this.cancelled = false;
					AddPartitionSubdivisionDialog.this.setVisible(false);
				}
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set the size of the dialog.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		final List messages = new ArrayList();

		// Check name is present.
		if (this.isEmpty(this.name.getText()))
			messages.add(Resources.get("partitionSubdivisionNameEmpty"));

		// Cols > 0.
		if (this.cols.getSelectedValues().length < 1)
			messages.add(Resources.get("partitionSubdivisionColsEmpty"));

		// If there any messages, display them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Create the partition subdivision representation based on our input.
	 * 
	 * @return the array of parts - first bit is the name, remaining bits are
	 *         column names.
	 * @throws PartitionException
	 *             if it could not be created.
	 */
	public String[] toStringArray() throws PartitionException {
		final List parts = new ArrayList();
		parts.add(name.getText().trim());
		final Object[] sel = cols.getSelectedValues();
		for (int i = 0; i < sel.length; i++)
			parts.add((String) sel[i]);
		return (String[]) parts.toArray(new String[0]);
	}
}
