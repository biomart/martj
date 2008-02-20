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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.common.resources.Resources;

/**
 * A dialog which allows the user to specify a column to split the optimiser
 * column for.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class SplitOptimiserColumnDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JComboBox column;

	private JCheckBox split;

	/**
	 * Pop up a dialog to define the split of an optimiser.
	 * 
	 * @param isSplit
	 *            is it already split?
	 * @param splitColumn
	 *            the existing split column, if any.
	 * @param columnOptions
	 *            the columns the user can choose from.
	 */
	public SplitOptimiserColumnDialog(final boolean isSplit,
			final DataSetColumn splitColumn, final Collection columnOptions) {
		// Create the base dialog.
		super();
		this.setTitle(Resources.get("splitOptimiserColumnDialogTitle"));
		this.setModal(true);

		final Object colSelect = splitColumn == null ? Resources
				.get("splitOptimiserNoSplit") : (Object) splitColumn;

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

		// Set up the arity spinner field.
		this.split = new JCheckBox(Resources
				.get("splitOptimiserColumnEnableLabel"));
		this.split.setSelected(isSplit);

		// Set up the combo box of columns.
		this.column = new JComboBox();
		this.column.addItem(Resources.get("splitOptimiserNoSplit"));
		for (final Iterator i = columnOptions.iterator(); i.hasNext();)
			this.column.addItem(i.next());
		this.column.setRenderer(new ListCellRenderer() {
			// Map ds cols to user-friendly names.
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final JLabel label = new JLabel();
				if (value instanceof DataSetColumn) {
					final DataSetColumn col = (DataSetColumn) value;
					if (col != null)
						label.setText(col.getModifiedName());
				} else if (value != null)
					label.setText(value.toString());
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
		this.column.setEnabled(isSplit);
		this.column.setSelectedItem(colSelect);

		// The close and execute buttons.
		final JButton close = new JButton(Resources.get("closeButton"));
		final JButton execute = new JButton(Resources.get("updateButton"));

		// Input fields.
		JPanel field = new JPanel();
		field.add(this.split);
		content.add(field, fieldConstraints);

		// Parallel button.
		field = new JPanel();
		field.add(new JLabel(Resources.get("splitOptimiserColumnLabel")));
		field.add(this.column);
		content.add(field, fieldConstraints);

		// Close/Execute buttons at the bottom.
		field = new JPanel();
		field.add(close);
		field.add(execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the checkbox.
		this.split.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SplitOptimiserColumnDialog.this.column
						.setEnabled(SplitOptimiserColumnDialog.this.split
								.isSelected());
				if (!SplitOptimiserColumnDialog.this.split.isSelected())
					SplitOptimiserColumnDialog.this.column
							.setSelectedItem(null);
			}
		});

		// Intercept the close button, which closes the dialog
		// without taking any action.
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				// Reset to default value.
				SplitOptimiserColumnDialog.this.split.setSelected(isSplit);
				SplitOptimiserColumnDialog.this.column
						.setSelectedItem(colSelect);
				SplitOptimiserColumnDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SplitOptimiserColumnDialog.this.validateFields())
					SplitOptimiserColumnDialog.this.setVisible(false);
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
	 * Get the column the user selected.
	 * 
	 * @return the selected column.
	 */
	public DataSetColumn getSplitOptimiserColumn() {
		return this.column.getSelectedItem().equals(
				Resources.get("splitOptimiserNoSplit")) ? null
				: (DataSetColumn) this.column.getSelectedItem();
	}

	/**
	 * If the user ticked the split box, this will return <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if the user ticked the split box.
	 */
	public boolean isSplit() {
		return this.split.isSelected();
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
