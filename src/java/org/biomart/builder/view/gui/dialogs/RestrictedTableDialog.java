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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.biomart.builder.model.SchemaModificationSet;
import org.biomart.builder.model.SchemaModificationSet.TableRestriction;
import org.biomart.common.model.Table;
import org.biomart.common.model.Column.GenericColumn;
import org.biomart.common.resources.Resources;

/**
 * This dialog asks users to create or modify a restriction over a particular
 * table for this dataset only.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class RestrictedTableDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private ColumnAliasTableModel columnAliasModel;

	private JTable columnAliasTable;

	private JButton execute;

	private JTextArea expression;

	private JButton insert;

	private JButton remove;

	/**
	 * Creates (but does not open) a dialog requesting details of a restricted
	 * table.
	 * 
	 * @param table
	 *            the table to restrict.
	 * @param template
	 *            the restriction to use as a template, if any.
	 */
	public RestrictedTableDialog(final Table table,
			final SchemaModificationSet.TableRestriction template) {
		// Creates the basic dialog.
		super();
		this.setTitle(template == null ? Resources
				.get("addTblRestrictDialogTitle") : Resources
				.get("modifyTblRestrictDialogTitle"));
		this.setModal(true);

		// Remembers the dataset tabset this dialog is referring to.
		this.cancelled = true;

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

		// Create the fields that will contain the user's table choices.
		this.expression = new JTextArea(10, 40); // Arbitrary size.

		// First table aliases.
		this.columnAliasModel = new ColumnAliasTableModel(table, template);
		this.columnAliasTable = new JTable(this.columnAliasModel);
		this.columnAliasTable.setGridColor(Color.LIGHT_GRAY); // Mac OSX fix.
		this.columnAliasTable.setPreferredScrollableViewportSize(new Dimension(
				400, 100));
		// Arbitrary size.
		this.insert = new JButton(Resources.get("insertAliasButton"));
		this.remove = new JButton(Resources.get("removeAliasButton"));

		// Set the column-editor for the first column column.
		final TableColumn columnColumn = this.columnAliasTable.getColumnModel()
				.getColumn(0);
		final JComboBox columnEditor = new JComboBox();
		for (final Iterator i = table.getColumns().iterator(); i.hasNext();)
			columnEditor.addItem(i.next());
		columnColumn.setCellEditor(new DefaultCellEditor(columnEditor));

		// Size the first table columns.
		this.columnAliasTable.getColumnModel().getColumn(0).setPreferredWidth(
				columnEditor.getPreferredSize().width);
		this.columnAliasTable.getColumnModel().getColumn(1).setPreferredWidth(
				this.columnAliasTable.getTableHeader().getDefaultRenderer()
						.getTableCellRendererComponent(
								null,
								this.columnAliasTable.getColumnModel()
										.getColumn(1).getHeaderValue(), false,
								false, 0, 0).getPreferredSize().width);

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

		// Listener for the insert button.
		this.insert.addActionListener(new ActionListener() {
			private int aliasCount = 1;

			public void actionPerformed(final ActionEvent e) {
				RestrictedTableDialog.this.columnAliasModel.insertRow(
						RestrictedTableDialog.this.columnAliasModel
								.getRowCount(), new Object[] {
								columnEditor.getItemAt(0),
								Resources.get("defaultAlias")
										+ (this.aliasCount++) });
			}
		});

		// Listener for the remove button.
		this.remove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final int rows[] = RestrictedTableDialog.this.columnAliasTable
						.getSelectedRows();
				// Reverse order, so we don't end up with changing
				// indices along the way.
				for (int i = rows.length - 1; i >= 0; i--)
					RestrictedTableDialog.this.columnAliasModel
							.removeRow(rows[i]);
			}
		});

		// Add the aliases.
		JLabel label = new JLabel(Resources.get("columnAliasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(new JScrollPane(this.columnAliasTable));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.insert);
		field.add(this.remove);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the expression option.
		label = new JLabel(Resources.get("expressionLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(new JScrollPane(this.expression));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the buttons to the dialog.
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
				RestrictedTableDialog.this.hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (RestrictedTableDialog.this.validateFields()) {
					RestrictedTableDialog.this.cancelled = false;
					RestrictedTableDialog.this.hide();
				}
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set the size of the dialog.
		this.pack();

		// Centre ourselves.
		this.setLocationRelativeTo(null);

		// Set some nice defaults.
		if (template != null)
			this.expression.setText(template.getExpression());
		// Aliases were already copied in the JTable constructor above.
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		final List messages = new ArrayList();

		// We must have an expression!
		if (this.isEmpty(this.expression.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("expression")));

		// Validate other fields.
		if (this.columnAliasModel.getColumnAliases().isEmpty())
			messages.add(Resources.get("columnAliasMissing"));

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
	 * Return <tt>true</tt> if the user cancelled the box.
	 * 
	 * @return <tt>true</tt> if the box was cancelled.
	 */
	public boolean getCancelled() {
		return this.cancelled;
	}

	/**
	 * Return the column aliases the user selected.
	 * 
	 * @return the aliases.
	 */
	public Map getColumnAliases() {
		return this.columnAliasModel.getColumnAliases();
	}

	/**
	 * Return the expression the user selected.
	 * 
	 * @return the expression.
	 */
	public String getExpression() {
		return this.expression.getText().trim();
	}

	/**
	 * This internal class represents a map of dataset columns to aliases.
	 */
	private static class ColumnAliasTableModel extends DefaultTableModel {
		private static final Class[] colClasses = new Class[] {
				GenericColumn.class, String.class };

		private static final long serialVersionUID = 1;

		/**
		 * This constructor sets up a new model, and populates it with the
		 * contents of the given restriction if provided.
		 * 
		 * @param table
		 *            the table we are showing columns from.
		 * @param template
		 *            the model to copy existing settings from.
		 */
		public ColumnAliasTableModel(final Table table,
				final SchemaModificationSet.TableRestriction template) {
			super(new Object[] { Resources.get("columnAliasTableColHeader"),
					Resources.get("columnAliasTableAliasHeader") }, 0);
			// Populate columns, and aliases from template.
			if (template != null)
				for (final Iterator i = template.getAliases().entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final GenericColumn col = (GenericColumn) entry.getKey();
					this.insertRow(this.getRowCount(), new Object[] { col,
							(String) entry.getValue() });
				}
		}

		/**
		 * Find out what aliases the user has given.
		 * 
		 * @return a map of aliases. Keys are column instances, values are the
		 *         aliases the user assigned.
		 */
		public Map getColumnAliases() {
			// Return the map of column to alias.
			final HashMap aliases = new HashMap();
			for (int i = 0; i < this.getRowCount(); i++) {
				final GenericColumn col = (GenericColumn) this.getValueAt(i, 0);
				final String alias = (String) this.getValueAt(i, 1);
				if (col != null
						&& !(alias == null || alias.trim().length() == 0))
					aliases.put(col, alias);
			}
			return aliases;
		}

		public Class getColumnClass(final int column) {
			return ColumnAliasTableModel.colClasses[column];
		}
	}
}
