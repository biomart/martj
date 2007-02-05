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
import org.biomart.common.model.Relation;
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
public class RestrictedRelationDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private ColumnAliasTableModel lcolumnAliasModel;

	private JTable lcolumnAliasTable;

	private ColumnAliasTableModel rcolumnAliasModel;

	private JTable rcolumnAliasTable;

	private JButton execute;

	private JTextArea expression;

	private JButton linsert;

	private JButton lremove;

	private JButton rinsert;

	private JButton rremove;

	/**
	 * Creates (but does not open) a dialog requesting details of a restricted
	 * table.
	 * 
	 * @param relation
	 *            the relation to restrict.
	 * @param template
	 *            the restriction to use as a template, if any.
	 */
	public RestrictedRelationDialog(final Relation relation,
			final SchemaModificationSet.RestrictedRelationDefinition template) {
		// Creates the basic dialog.
		super();
		this.setTitle(template == null ? Resources
				.get("addRelRestrictDialogTitle") : Resources
				.get("modifyRelRestrictDialogTitle"));
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
		this.lcolumnAliasModel = new ColumnAliasTableModel(template, true);
		this.lcolumnAliasTable = new JTable(this.lcolumnAliasModel);
		this.lcolumnAliasTable.setGridColor(Color.LIGHT_GRAY); // Mac OSX fix.
		this.lcolumnAliasTable
				.setPreferredScrollableViewportSize(new Dimension(400, 100));
		// Arbitrary size.
		this.linsert = new JButton(Resources.get("insertAliasButton"));
		this.lremove = new JButton(Resources.get("removeAliasButton"));

		// Set the column-editor for the first column column.
		final TableColumn lcolumnColumn = this.lcolumnAliasTable
				.getColumnModel().getColumn(0);
		final JComboBox lcolumnEditor = new JComboBox();
		for (final Iterator i = relation.getFirstKey().getTable().getColumns()
				.iterator(); i.hasNext();)
			lcolumnEditor.addItem(i.next());
		lcolumnColumn.setCellEditor(new DefaultCellEditor(lcolumnEditor));

		// Size the first table columns.
		this.lcolumnAliasTable.getColumnModel().getColumn(0).setPreferredWidth(
				lcolumnEditor.getPreferredSize().width);
		this.lcolumnAliasTable.getColumnModel().getColumn(1).setPreferredWidth(
				this.lcolumnAliasTable.getTableHeader().getDefaultRenderer()
						.getTableCellRendererComponent(
								null,
								this.lcolumnAliasTable.getColumnModel()
										.getColumn(1).getHeaderValue(), false,
								false, 0, 0).getPreferredSize().width);

		// Listener for the insert button.
		this.linsert.addActionListener(new ActionListener() {
			private int aliasCount = 1;

			public void actionPerformed(final ActionEvent e) {
				RestrictedRelationDialog.this.lcolumnAliasModel.insertRow(
						RestrictedRelationDialog.this.lcolumnAliasModel
								.getRowCount(), new Object[] {
								lcolumnEditor.getItemAt(0),
								Resources.get("defaultFirstAlias")
										+ (this.aliasCount++) });
			}
		});

		// Listener for the remove button.
		this.lremove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final int rows[] = RestrictedRelationDialog.this.lcolumnAliasTable
						.getSelectedRows();
				// Reverse order, so we don't end up with changing
				// indices along the way.
				for (int i = rows.length - 1; i >= 0; i--)
					RestrictedRelationDialog.this.lcolumnAliasModel
							.removeRow(rows[i]);
			}
		});

		// Add the aliases.
		JLabel label = new JLabel(Resources.get("lcolumnAliasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(new JScrollPane(this.lcolumnAliasTable));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.linsert);
		field.add(this.lremove);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Second table aliases.
		this.rcolumnAliasModel = new ColumnAliasTableModel(template, false);
		this.rcolumnAliasTable = new JTable(this.rcolumnAliasModel);
		this.rcolumnAliasTable.setGridColor(Color.LIGHT_GRAY); // Mac OSX fix.
		this.rcolumnAliasTable
				.setPreferredScrollableViewportSize(new Dimension(400, 100));
		// Arbitrary size.
		this.rinsert = new JButton(Resources.get("insertAliasButton"));
		this.rremove = new JButton(Resources.get("removeAliasButton"));

		// Set the column-editor for the first column column.
		final TableColumn rcolumnColumn = this.rcolumnAliasTable
				.getColumnModel().getColumn(0);
		final JComboBox rcolumnEditor = new JComboBox();
		for (final Iterator i = relation.getSecondKey().getTable().getColumns()
				.iterator(); i.hasNext();)
			rcolumnEditor.addItem(i.next());
		rcolumnColumn.setCellEditor(new DefaultCellEditor(rcolumnEditor));

		// Size the first table columns.
		this.rcolumnAliasTable.getColumnModel().getColumn(0).setPreferredWidth(
				rcolumnEditor.getPreferredSize().width);
		this.rcolumnAliasTable.getColumnModel().getColumn(1).setPreferredWidth(
				this.rcolumnAliasTable.getTableHeader().getDefaultRenderer()
						.getTableCellRendererComponent(
								null,
								this.rcolumnAliasTable.getColumnModel()
										.getColumn(1).getHeaderValue(), false,
								false, 0, 0).getPreferredSize().width);

		// Listener for the insert button.
		this.rinsert.addActionListener(new ActionListener() {
			private int aliasCount = 1;

			public void actionPerformed(final ActionEvent e) {
				RestrictedRelationDialog.this.rcolumnAliasModel.insertRow(
						RestrictedRelationDialog.this.rcolumnAliasModel
								.getRowCount(), new Object[] {
								rcolumnEditor.getItemAt(0),
								Resources.get("defaultSecondAlias")
										+ (this.aliasCount++) });
			}
		});

		// Listener for the remove button.
		this.rremove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final int rows[] = RestrictedRelationDialog.this.rcolumnAliasTable
						.getSelectedRows();
				// Reverse order, so we don't end up with changing
				// indices along the way.
				for (int i = rows.length - 1; i >= 0; i--)
					RestrictedRelationDialog.this.rcolumnAliasModel
							.removeRow(rows[i]);
			}
		});

		// Add the aliases.
		label = new JLabel(Resources.get("rcolumnAliasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(new JScrollPane(this.rcolumnAliasTable));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.rinsert);
		field.add(this.rremove);
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

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

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
				RestrictedRelationDialog.this.hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (RestrictedRelationDialog.this.validateFields()) {
					RestrictedRelationDialog.this.cancelled = false;
					RestrictedRelationDialog.this.hide();
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
		if (this.lcolumnAliasModel.getColumnAliases().isEmpty()
				|| this.rcolumnAliasModel.getColumnAliases().isEmpty())
			messages.add(Resources.get("lrcolumnAliasMissing"));

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
	public Map getLHSColumnAliases() {
		return this.lcolumnAliasModel.getColumnAliases();
	}

	/**
	 * Return the column aliases the user selected.
	 * 
	 * @return the aliases.
	 */
	public Map getRHSColumnAliases() {
		return this.rcolumnAliasModel.getColumnAliases();
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
		 * @param template
		 *            the model to copy existing settings from.
		 * @param left
		 *            <tt>true</tt> if this set of columns is the left-hand 
		 *            relation table.
		 */
		public ColumnAliasTableModel(
				final SchemaModificationSet.RestrictedRelationDefinition template,
				final boolean left) {
			super(new Object[] { Resources.get("columnAliasTableColHeader"),
					Resources.get("columnAliasTableAliasHeader") }, 0);
			// Populate columns, and aliases from template.
			if (template != null)
				for (final Iterator i = (left ? template.getLeftAliases()
						: template.getRightAliases()).entrySet().iterator(); i
						.hasNext();) {
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
