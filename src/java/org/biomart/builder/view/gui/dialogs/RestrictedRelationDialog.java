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

import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This dialog asks users to create or modify a restriction over a particular
 * relation for this dataset only.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 24th July 2006
 * @since 0.1
 */
public class RestrictedRelationDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	private boolean cancelled;

	private ColumnAliasTableModel firstColumnAliasModel;

	private JTable firstColumnAliasTable;

	private JButton firstInsert;

	private JButton firstRemove;

	private ColumnAliasTableModel secondColumnAliasModel;

	private JTable secondColumnAliasTable;

	private JButton secondInsert;

	private JButton secondRemove;

	private JTextArea expression;

	private JButton cancel;

	private JButton execute;

	/**
	 * Creates (but does not open) a dialog requesting details of a restricted
	 * relation.
	 * 
	 * @param martTab
	 *            the mart tab set to centre ourselves over.
	 * @param relation
	 *            the relation to restrict.
	 * @param template
	 *            the restriction to use as a template, if any.
	 */
	public RestrictedRelationDialog(final MartTab martTab, Relation relation,
			DataSetRelationRestriction template) {
		// Creates the basic dialog.
		super(martTab.getMartTabSet().getMartBuilder(),
				template == null ? Resources.get("addRelRestrictDialogTitle")
						: Resources.get("modifyRelRestrictDialogTitle"), true);

		// Remembers the dataset tabset this dialog is referring to.
		this.martTab = martTab;

		// Create the content pane to store the create dialog panel.
		GridBagLayout gridBag = new GridBagLayout();
		final JPanel content = new JPanel(gridBag);
		this.setContentPane(content);

		// Create constraints for labels that are not in the last row.
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create the fields that will contain the user's table choices.
		this.cancelled = false;
		this.expression = new JTextArea(10, 40); // Arbitrary size.

		// First table aliases.
		this.firstColumnAliasModel = new ColumnAliasTableModel(relation
				.getFirstKey().getTable(), template, true);
		this.firstColumnAliasTable = new JTable(this.firstColumnAliasModel);
		this.firstColumnAliasTable
				.setPreferredScrollableViewportSize(new Dimension(400, 100));
		// Arbitrary size.
		this.firstInsert = new JButton(Resources.get("insertAliasButton"));
		this.firstRemove = new JButton(Resources.get("removeAliasButton"));

		// Second table aliases.
		this.secondColumnAliasModel = new ColumnAliasTableModel(relation
				.getSecondKey().getTable(), template, false);
		this.secondColumnAliasTable = new JTable(this.secondColumnAliasModel);
		this.secondColumnAliasTable
				.setPreferredScrollableViewportSize(new Dimension(400, 100));
		// Arbitrary size.
		this.secondInsert = new JButton(Resources.get("insertAliasButton"));
		this.secondRemove = new JButton(Resources.get("removeAliasButton"));

		// Set the column-editor for the first column column.
		TableColumn columnColumn = this.firstColumnAliasTable.getColumnModel()
				.getColumn(0);
		JComboBox columnEditor = new JComboBox();
		for (Iterator i = relation.getFirstKey().getTable().getColumns()
				.iterator(); i.hasNext();)
			columnEditor.addItem((GenericColumn) i.next());
		columnColumn.setCellEditor(new DefaultCellEditor(columnEditor));

		// Size the first table columns.
		this.firstColumnAliasTable.getColumnModel().getColumn(0)
				.setPreferredWidth(columnEditor.getPreferredSize().width);
		this.firstColumnAliasTable.getColumnModel().getColumn(1)
				.setPreferredWidth(
						this.firstColumnAliasTable.getTableHeader()
								.getDefaultRenderer()
								.getTableCellRendererComponent(
										null,
										this.firstColumnAliasTable
												.getColumnModel().getColumn(1)
												.getHeaderValue(), false,
										false, 0, 0).getPreferredSize().width);

		// Set the column-editor for the second column column.
		columnColumn = this.secondColumnAliasTable.getColumnModel()
				.getColumn(0);
		columnEditor = new JComboBox();
		for (Iterator i = relation.getSecondKey().getTable().getColumns()
				.iterator(); i.hasNext();)
			columnEditor.addItem((GenericColumn) i.next());
		columnColumn.setCellEditor(new DefaultCellEditor(columnEditor));

		// Size the second table columns.
		this.secondColumnAliasTable.getColumnModel().getColumn(0)
				.setPreferredWidth(columnEditor.getPreferredSize().width);
		this.secondColumnAliasTable.getColumnModel().getColumn(1)
				.setPreferredWidth(
						this.secondColumnAliasTable.getTableHeader()
								.getDefaultRenderer()
								.getTableCellRendererComponent(
										null,
										this.secondColumnAliasTable
												.getColumnModel().getColumn(1)
												.getHeaderValue(), false,
										false, 0, 0).getPreferredSize().width);

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

		// Listener for the insert buttons.
		this.firstInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				firstColumnAliasModel.insertRow(firstColumnAliasModel
						.getRowCount(), new Object[] { null, null });
			}
		});
		this.secondInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				secondColumnAliasModel.insertRow(secondColumnAliasModel
						.getRowCount(), new Object[] { null, null });
			}
		});

		// Listener for the remove buttons.
		this.firstRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int rows[] = firstColumnAliasTable.getSelectedRows();
				// Reverse order, so we don't end up with changing
				// indices along the way.
				for (int i = rows.length - 1; i >= 0; i--)
					firstColumnAliasModel.removeRow(rows[i]);
			}
		});
		this.secondRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int rows[] = secondColumnAliasTable.getSelectedRows();
				// Reverse order, so we don't end up with changing
				// indices along the way.
				for (int i = rows.length - 1; i >= 0; i--)
					secondColumnAliasModel.removeRow(rows[i]);
			}
		});

		// Add the first table aliases.
		JLabel label = new JLabel(Resources.get("firstKeyLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(new JLabel(relation.getFirstKey().toString()));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel(Resources.get("columnAliasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(new JScrollPane(this.firstColumnAliasTable));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.firstInsert);
		field.add(this.firstRemove);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the second table aliases.
		label = new JLabel(Resources.get("secondKeyLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(new JLabel(relation.getSecondKey().toString()));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel(Resources.get("columnAliasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(new JScrollPane(this.secondColumnAliasTable));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.secondInsert);
		field.add(this.secondRemove);
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
			public void actionPerformed(ActionEvent e) {
				cancelled = true;
				hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (validateFields())
					hide();
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set the size of the dialog.
		this.pack();

		// Centre ourselves.
		this.setLocationRelativeTo(this.martTab.getMartTabSet()
				.getMartBuilder());

		// Set some nice defaults.
		if (template != null) {
			this.expression.setText(template.getExpression());
			// Aliases were already copied in the JTable constructor above.
		}
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		List messages = new ArrayList();

		// We must have an expression!
		if (this.isEmpty(this.expression.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("expression")));

		// Validate other fields.
		if (this.firstColumnAliasModel.getColumnAliases().isEmpty()
				&& this.secondColumnAliasModel.getColumnAliases().isEmpty())
			messages.add(Resources.get("columnAliasMissing"));

		// If there any messages, display them.
		if (!messages.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	private boolean isEmpty(String string) {
		// Strings are empty if they are null or all whitespace.
		return (string == null || string.trim().length() == 0);
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
	 * Return the column aliases the user selected for the first table.
	 * 
	 * @return the aliases.
	 */
	public Map getFirstColumnAliases() {
		return this.firstColumnAliasModel.getColumnAliases();
	}

	/**
	 * Return the column aliases the user selected for the second table.
	 * 
	 * @return the aliases.
	 */
	public Map getSecondColumnAliases() {
		return this.secondColumnAliasModel.getColumnAliases();
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
	 * This internal class represents a map of dataset columns to aliases.
	 */
	private static class ColumnAliasTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1;

		private static final Class[] colClasses = new Class[] {
				GenericColumn.class, String.class };

		public ColumnAliasTableModel(Table table,
				DataSetRelationRestriction template, boolean first) {
			super(new Object[] { Resources.get("columnAliasTableColHeader"),
					Resources.get("columnAliasTableAliasHeader") }, 0);
			// Populate columns, and aliases from template.
			if (template != null) {
				Map aliases = first ? template.getFirstTableAliases()
						: template.getSecondTableAliases();
				for (Iterator i = aliases.keySet().iterator(); i.hasNext();) {
					GenericColumn col = (GenericColumn) i.next();
					this.insertRow(this.getRowCount(), new Object[] { col,
							(String) aliases.get(col) });
				}
			}
		}

		public Class getColumnClass(int column) {
			return ColumnAliasTableModel.colClasses[column];
		}

		public Map getColumnAliases() {
			// Return the map of column to alias.
			HashMap aliases = new HashMap();
			for (int i = 0; i < this.getRowCount(); i++) {
				GenericColumn col = (GenericColumn) this.getValueAt(i, 0);
				String alias = (String) this.getValueAt(i, 1);
				if (col != null
						&& !(alias == null || alias.trim().length() == 0))
					aliases.put(col, alias);
			}
			return aliases;
		}
	}
}