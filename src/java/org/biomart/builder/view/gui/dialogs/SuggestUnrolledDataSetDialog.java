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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.common.resources.Resources;

/**
 * This dialog box allows the user to define an unrolled dataset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class SuggestUnrolledDataSetDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private final JComboBox childTable;

	private final JComboBox childRelation;

	private final JComboBox parentRelation;

	private final JComboBox namingColumn;

	private boolean cancelled = true;

	/**
	 * Pop up a suggest unrolled dataset dialog.
	 * 
	 * @param parent
	 *            the parent table we are working with.
	 * @param candidates
	 *            choice of possible children. Keys are tables, values are lists
	 *            of relations to that table.
	 */
	public SuggestUnrolledDataSetDialog(final Table parent, final Map candidates) {
		// Create the basic dialog centred on the main mart builder window.
		super();
		this.setTitle(Resources.get("suggestUnrolledDataSetDialogTitle"));
		this.setModal(true);

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		final JPanel content = new JPanel(new GridBagLayout());
		this.setContentPane(content);

		// Create some constraints for labels, except those on the last row
		// of the dialog.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create some constraints for fields, except those on the last row
		// of the dialog.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create some constraints for labels on the last row of the dialog.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create some constraints for fields on the last row of the dialog.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Build Insert drop downs.
		this.childTable = new JComboBox(candidates.keySet().toArray());
		this.namingColumn = new JComboBox(parent.getColumns().values()
				.toArray());
		this.parentRelation = new JComboBox();
		this.childRelation = new JComboBox();
		// Add listener to update parent and child relations.
		this.childTable.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SuggestUnrolledDataSetDialog.this.parentRelation
						.removeAllItems();
				SuggestUnrolledDataSetDialog.this.childRelation
						.removeAllItems();
				final Table child = (Table) SuggestUnrolledDataSetDialog.this.childTable
						.getSelectedItem();
				if (child != null) {
					for (final Iterator i = ((List) candidates.get(child))
							.iterator(); i.hasNext();) {
						final Relation rel = (Relation) i.next();
						SuggestUnrolledDataSetDialog.this.parentRelation
								.addItem(rel);
						SuggestUnrolledDataSetDialog.this.childRelation
								.addItem(rel);
					}
					SuggestUnrolledDataSetDialog.this.parentRelation
							.setSelectedIndex(0);
					SuggestUnrolledDataSetDialog.this.childRelation
							.setSelectedIndex(0);
				} else {
					SuggestUnrolledDataSetDialog.this.parentRelation
							.setSelectedIndex(-1);
					SuggestUnrolledDataSetDialog.this.childRelation
							.setSelectedIndex(-1);
				}
			}
		});

		JLabel label = new JLabel(Resources.get("childTableLabel"));
		content.add(label, labelConstraints);
		content.add(this.childTable, fieldConstraints);
		label = new JLabel(Resources.get("parentRelationLabel"));
		content.add(label, labelConstraints);
		content.add(this.parentRelation, fieldConstraints);
		label = new JLabel(Resources.get("childRelationLabel"));
		content.add(label, labelConstraints);
		content.add(this.childRelation, fieldConstraints);
		label = new JLabel(Resources.get("namingColumnLabel"));
		content.add(label, labelConstraints);
		content.add(this.namingColumn, fieldConstraints);

		// Add the buttons.
		final JButton cancel = new JButton(Resources.get("cancelButton"));
		final JButton execute = new JButton(Resources.get("suggestButton"));
		label = new JLabel();
		content.add(label, labelLastRowConstraints);
		final JPanel field = new JPanel();
		field.add(cancel);
		field.add(execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the cancel button, which closes the dialog
		// without taking any action.
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SuggestUnrolledDataSetDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button, which causes the
		// schema to be created as a temporary schema object. If
		// successful, the dialog closes.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SuggestUnrolledDataSetDialog.this.validateFields()) {
					SuggestUnrolledDataSetDialog.this.cancelled = false;
					SuggestUnrolledDataSetDialog.this.setVisible(false);
				}
			}
		});

		// Select first available values.
		this.childTable.setSelectedIndex(0);
		this.namingColumn.setSelectedIndex(0);

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(execute);

		// Pack and resize the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	/**
	 * Were we canceled?
	 * 
	 * @return <tt>true</tt> if we were.
	 */
	public boolean isCancelled() {
		return this.cancelled;
	}

	/**
	 * Get the selected parent relation.
	 * 
	 * @return the selected parent relation.
	 */
	public Relation getParentRelation() {
		return (Relation) this.parentRelation.getSelectedItem();
	}

	/**
	 * Get the selected child relation.
	 * 
	 * @return the selected child relation.
	 */
	public Relation getChildRelation() {
		return (Relation) this.childRelation.getSelectedItem();
	}

	/**
	 * Get the selected naming column.
	 * 
	 * @return the selected naming column.
	 */
	public Column getNamingColumn() {
		return (Column) this.namingColumn.getSelectedItem();
	}

	private boolean validateFields() {
		// Make a list to hold messages.
		final List messages = new ArrayList();

		// We don't like missing drop-downs.
		if (this.parentRelation.getSelectedIndex() == -1)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("parentRelation")));
		if (this.childRelation.getSelectedIndex() == -1)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("childRelation")));
		if (this.namingColumn.getSelectedIndex() == -1)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("namingColumn")));

		// We don't like same-as relations.
		if (this.parentRelation.getSelectedIndex() == this.childRelation
				.getSelectedIndex())
			messages.add(Resources.get("childParentRelationSame"));

		// If we have any messages, show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// If there were no messages, then validated OK.
		return messages.isEmpty();
	}
}
