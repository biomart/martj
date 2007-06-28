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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.panels.PartitionTableModifierPanel;
import org.biomart.builder.view.gui.panels.PartitionTableModifierPanel.SelectFromModifierPanel;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.resources.Resources;

/**
 * This dialog asks users to create a partition table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class AddPartitionTableDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private JButton execute;

	private PartitionTableModifierPanel panel;

	private JTextField name;

	/**
	 * Pops up a dialog to manage the partitions.
	 * 
	 * @return <tt>true</tt> if the user's actions led to a change in the
	 *         partitioning method.
	 */
	public boolean definePartitionTable() {
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
	 * @param martTab
	 *            the mart tab we are working within.
	 */
	public AddPartitionTableDialog(final MartTab martTab) {
		// Creates the basic dialog.
		super();
		this.setTitle(Resources.get("createPartitionTableDialogTitle"));
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

		// Create type chooser, name field, and panel holder.
		content.add(new JLabel(Resources.get("partitionTableTypeLabel")),
				labelConstraints);
		final JComboBox typeChooser = new JComboBox();
		// Populate type list.
		typeChooser.addItem(Resources.get("selectFromPartitionTableType"));
		// Add to dialog.
		content.add(typeChooser, fieldConstraints);
		content.add(new JLabel(Resources.get("partitionTableNameLabel")),
				labelConstraints);
		this.name = new JTextField(20);
		content.add(this.name, fieldConstraints);
		final JPanel panelHolder = new JPanel();
		content.add(panelHolder, fieldConstraints);
		// Listener on type chooser updates panel holder.
		typeChooser.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final Object obj = typeChooser.getSelectedItem();
				panelHolder.removeAll();
				// Add new sub-panel based on selection.
				if (Resources.get("selectFromPartitionTableType").equals(obj)) {
					AddPartitionTableDialog.this.panel = new SelectFromModifierPanel(
							null, martTab.getMart().getAllTables());
					panelHolder.add(AddPartitionTableDialog.this.panel);
				}
				AddPartitionTableDialog.this.pack();
			}
		});

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
				AddPartitionTableDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (AddPartitionTableDialog.this.validateFields()) {
					AddPartitionTableDialog.this.cancelled = false;
					AddPartitionTableDialog.this.setVisible(false);
				}
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Select first option on type chooser.
		typeChooser.setSelectedItem(Resources.get("selectFromPartitionTableType"));

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
			messages.add(Resources.get("partitionTableNameEmpty"));

		// If there any messages, display them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty() && this.panel.validateInput();
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Create the partition table based on our input.
	 * 
	 * @return the table.
	 * @throws PartitionException
	 *             if it could not be created.
	 */
	public PartitionTable create() throws PartitionException {
		return this.panel.create(this.name.getText().trim());
	}
}
