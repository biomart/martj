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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.resources.Resources;

/**
 * A dialog which allows the user to choose which end of a relation it must be
 * followed from. The transformation will then always follow this relation from
 * that end and ignore it from the other.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class DirectionalRelationDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JComboBox chosenKey;

	/**
	 * Pop up a dialog to define the direction of a relation.
	 * 
	 * @param initialChoice
	 *            the initial preselected key.
	 * @param relation
	 *            the relation we are working with.
	 */
	public DirectionalRelationDialog(final Key initialChoice,
			final Relation relation) {
		// Create the base dialog.
		super();
		this.setTitle(Resources.get("directionalRelationDialogTitle"));
		this.setModal(true);

		// Create the layout manager for this panel.
		final JPanel content = new JPanel(new GridBagLayout());
		this.setContentPane(content);

		// Create constraints for fields that are not in the last row.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are in the last row.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Set up the check box to turn it on and off.
		final JCheckBox checkbox = new JCheckBox();
		this.chosenKey = new JComboBox();
		this.chosenKey.addItem(null);
		this.chosenKey.addItem(relation.getFirstKey());
		this.chosenKey.addItem(relation.getSecondKey());
		this.chosenKey.setSelectedItem(initialChoice);
		if (initialChoice != null)
			checkbox.setSelected(true);

		// The close and execute buttons.
		final JButton close = new JButton(Resources.get("closeButton"));
		final JButton execute = new JButton(Resources.get("updateButton"));

		// Key field.
		JPanel field = new JPanel();
		field.add(checkbox);
		field.add(new JLabel(Resources.get("directionalRelationKeyLabel")));
		field.add(this.chosenKey);
		content.add(field, fieldConstraints);

		// Close/Execute buttons at the bottom.
		field = new JPanel();
		field.add(close);
		field.add(execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the drop-down.
		this.chosenKey.addItemListener(new ItemListener() {
			public void itemStateChanged(final ItemEvent e) {
				if (DirectionalRelationDialog.this.getChosenKey() == null)
					checkbox.setSelected(false);
				else
					checkbox.setSelected(true);
			}
		});
		// Intercept the checkbox.
		checkbox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (checkbox.isSelected()
						&& DirectionalRelationDialog.this.getChosenKey() == null)
					DirectionalRelationDialog.this.chosenKey
							.setSelectedItem(relation.getFirstKey());
				else
					DirectionalRelationDialog.this.chosenKey
							.setSelectedItem(null);
			}
		});

		// Intercept the close button, which closes the dialog
		// without taking any action.
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				// Reset to default value.
				DirectionalRelationDialog.this.chosenKey
						.setSelectedItem(initialChoice);
				DirectionalRelationDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (DirectionalRelationDialog.this.validateFields())
					DirectionalRelationDialog.this.setVisible(false);
			}
		});

		// Make execute the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	/**
	 * Get the key the user selected.
	 * 
	 * @return the selected key.
	 */
	public Key getChosenKey() {
		return (Key) this.chosenKey.getSelectedItem();
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		final List messages = new ArrayList();

		// Nothing to do here.

		// Any messages to display? Show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}
}
