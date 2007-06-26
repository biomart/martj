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

package org.biomart.common.view.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.biomart.common.resources.Resources;

/**
 * A dialog which allows the user to choose a partition column, or none at all.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public class PartitionSelectionDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JCheckBox partitionBox;

	private JComboBox partitionMenu;

	/**
	 * Pop up a dialog to choose a column.
	 * 
	 * @param partitionOptions
	 *            a list (possibly empty) of options available for attaching a
	 *            partition table to this compound relation. Each option is a
	 *            fully qualified partition column name.
	 * @param title
	 *            the title to give the dialog.
	 * @param startvalue
	 *            the initial preselected arity.
	 */
	public PartitionSelectionDialog(
			final Collection partitionOptions,final String title, final String startvalue) {
		// Create the base dialog.
		super();
		this.setTitle(title);
		this.setModal(true);

		// Create the layout manager for this panel.
		final JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
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

		// Create the partition stuff.
		this.partitionBox = new JCheckBox(Resources.get("partitionLabel"));
		this.partitionMenu = new JComboBox();
		if (partitionOptions.size() > 0) {
			for (final Iterator i = partitionOptions.iterator(); i.hasNext();)
				this.partitionMenu.addItem((String) i.next());
			this.partitionBox.setSelected(startvalue != null);
			if (startvalue != null)
				this.partitionMenu.setSelectedItem(startvalue);
			else 
				this.partitionMenu.setSelectedIndex(0); // Default.
		} else {
			this.partitionBox.setSelected(false);
			this.partitionBox.setEnabled(false);
			this.partitionMenu.setEnabled(false);
		}

		// The close and execute buttons.
		final JButton close = new JButton(Resources.get("closeButton"));
		final JButton execute = new JButton(Resources.get("updateButton"));

		// Partition stuff.
		JPanel field = new JPanel();
		field.add(this.partitionBox);
		field.add(this.partitionMenu);
		content.add(field, fieldConstraints);

		// Close/Execute buttons at the bottom.
		field = new JPanel();
		field.add(close);
		field.add(execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the partition checkbox.
		this.partitionBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				PartitionSelectionDialog.this.partitionMenu
						.setEnabled(PartitionSelectionDialog.this.partitionBox
								.isSelected());
			}
		});

		// Intercept the close button, which closes the dialog
		// without taking any action.
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				// Reset to default value.
				PartitionSelectionDialog.this.partitionBox.setSelected(startvalue != null);
				if (startvalue != null)
					PartitionSelectionDialog.this.partitionMenu.setSelectedItem(startvalue);
				PartitionSelectionDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (PartitionSelectionDialog.this.validateFields())
					PartitionSelectionDialog.this.setVisible(false);
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
	 * Work out if the user selected a partition.
	 * 
	 * @return the partition column reference they selected, or null if none.
	 */
	public String getPartition() {
		return this.partitionBox.isSelected() ? (String) this.partitionMenu
				.getSelectedItem() : null;
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		final List messages = new ArrayList();

		// No validation required yet.
		
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
