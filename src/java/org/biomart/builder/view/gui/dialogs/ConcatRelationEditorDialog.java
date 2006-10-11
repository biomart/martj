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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetConcatRelationType;
import org.biomart.builder.resources.Resources;

/**
 * A dialog which lists all the columns in a concat relation, and all the
 * columns in the table which are available to put in that relation. It can then
 * allow the user to move those columns around, thus e diting the relation. It
 * also allows the user to specify the separators to use during the
 * concatenation operation.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public class ConcatRelationEditorDialog extends JDialog {
	private static final long serialVersionUID = 1;

	/**
	 * Creates a new concat-relation type.
	 * 
	 * @param relation
	 *            the relation the concat is to be created on.
	 * @return the concat type the user defined.
	 */
	public static DataSetConcatRelationType createConcatRelation(
			final Relation relation) {
		final ConcatRelationEditorDialog dialog = new ConcatRelationEditorDialog(
				relation.getManyKey().getTable(), Resources
						.get("newConcatDialogTitle"), Resources
						.get("addButton"), null, null, null);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		return dialog.getConcatRelationType();
	}

	/**
	 * Edits an existing relation.
	 * 
	 * @param relation
	 *            the relation to be edited.
	 * @param type
	 *            the existing concat type for this relation.
	 * @return the new concat type the user defined.
	 */
	public static DataSetConcatRelationType modifyConcatRelation(
			final Relation relation, final DataSetConcatRelationType type) {
		final ConcatRelationEditorDialog dialog = new ConcatRelationEditorDialog(
				relation.getManyKey().getTable(), Resources
						.get("editConcatDialogTitle"), Resources
						.get("modifyButton"), type.getColumnSeparator(), type
						.getRecordSeparator(), type.getConcatColumns());
		dialog.setLocationRelativeTo(null);
		dialog.show();
		return dialog.getConcatRelationType();
	}

	private JTextField columnSep;

	private JTextField rowSep;

	private DefaultListModel selectedColumns;

	private DefaultListModel tableColumns;

	private DataSetConcatRelationType type;

	private ConcatRelationEditorDialog(final Table table, final String title,
			final String action, final String defaultColumnSep,
			final String defaultRowSep, final List columns) {
		// Create the base dialog.
		super();
		this.setTitle(title);
		this.setModal(true);
		this.type = null;

		// Create the layout manager for this panel.
		final GridBagLayout gridBag = new GridBagLayout();
		final JPanel content = new JPanel();
		content.setLayout(gridBag);
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

		// Set up the column and row separator fields.
		this.columnSep = new JTextField(5);
		this.columnSep.setText(defaultColumnSep);
		this.rowSep = new JTextField(5);
		this.rowSep.setText(defaultRowSep);

		// The list of table columns is populated with the names of columns.
		this.tableColumns = new DefaultListModel();
		for (final Iterator i = table.getColumns().iterator(); i.hasNext();)
			this.tableColumns.addElement(i.next());

		// The list of selected columns is populate with the columns from
		// the existing key. These are also removed from the list of table
		// columns, to prevent duplication.
		this.selectedColumns = new DefaultListModel();
		if (columns != null)
			for (final Iterator i = columns.iterator(); i.hasNext();) {
				final Object o = i.next();
				this.tableColumns.removeElement(o);
				this.selectedColumns.addElement(o);
			}

		// The close and execute buttons.
		final JButton close = new JButton(Resources.get("closeButton"));
		final JButton execute = new JButton(action);

		// Create the table column list, and the buttons
		// to move columns to/from the selected column list.
		final JList tabColList = new JList(this.tableColumns);
		final JButton insertButton = new JButton(new ImageIcon(Resources
				.getResourceAsURL("org/biomart/builder/resources/add.gif")));
		final JButton removeButton = new JButton(new ImageIcon(Resources
				.getResourceAsURL("org/biomart/builder/resources/remove.gif")));

		// Create the key column list, and the buttons to
		// move columns to/from the table columns list.
		final JList keyColList = new JList(this.selectedColumns);
		final JButton upButton = new JButton(new ImageIcon(Resources
				.getResourceAsURL("org/biomart/builder/resources/arrowUp.gif")));
		final JButton downButton = new JButton(
				new ImageIcon(
						Resources
								.getResourceAsURL("org/biomart/builder/resources/arrowDown.gif")));

		// Put the two halves of the dialog side-by-side in a horizontal box.
		final Box columnContent = Box.createHorizontalBox();

		// Left-hand side goes the table columns that are unused.
		final JPanel leftPanel = new JPanel(new BorderLayout());
		// Label at the top.
		leftPanel.add(new JLabel(Resources.get("columnsAvailableLabel")),
				BorderLayout.PAGE_START);
		// Table columns list in the middle.
		leftPanel.add(new JScrollPane(tabColList), BorderLayout.CENTER);
		leftPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		final Box leftButtonPanel = Box.createVerticalBox();
		leftButtonPanel.add(removeButton);
		leftButtonPanel.add(insertButton);
		leftButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		leftPanel.add(leftButtonPanel, BorderLayout.LINE_END);
		// Add the left panel.
		columnContent.add(leftPanel);

		// Right-hand side goes the key columns that are used.
		final JPanel rightPanel = new JPanel(new BorderLayout());
		// Label at the top.
		rightPanel.add(new JLabel(Resources.get("concatColumnsLabel")),
				BorderLayout.PAGE_START);
		// Key columns in the middle.
		rightPanel.add(new JScrollPane(keyColList), BorderLayout.CENTER);
		rightPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		final Box rightButtonPanel = Box.createVerticalBox();
		rightButtonPanel.add(upButton);
		rightButtonPanel.add(downButton);
		rightButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		rightPanel.add(rightButtonPanel, BorderLayout.LINE_END);
		// Add the right panel.
		columnContent.add(rightPanel);

		// Add the column panel content.
		gridBag.setConstraints(columnContent, fieldConstraints);
		content.add(columnContent);

		// Row separator.
		JPanel field = new JPanel();
		field.add(new JLabel(Resources.get("rowSepLabel")));
		field.add(this.rowSep);
		// Column separator.
		field.add(new JLabel(Resources.get("columnSepLabel")));
		field.add(this.columnSep);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Close/Execute buttons at the bottom.
		field = new JPanel();
		field.add(close);
		field.add(execute);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		content.add(field);

		// Intercept the insert/remove buttons
		insertButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = tabColList.getSelectedValue();
				if (selected != null) {
					// Move a column from table to key.
					ConcatRelationEditorDialog.this.selectedColumns
							.addElement(selected);
					ConcatRelationEditorDialog.this.tableColumns
							.removeElement(selected);
				}
			}
		});
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					// Move a column from key to table.
					ConcatRelationEditorDialog.this.tableColumns
							.addElement(selected);
					ConcatRelationEditorDialog.this.selectedColumns
							.removeElement(selected);
				}
			}
		});

		// Intercept the up/down buttons
		upButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					final int currIndex = ConcatRelationEditorDialog.this.selectedColumns
							.indexOf(selected);
					if (currIndex > 0) {
						// Swap the selected item with the one above it.
						final Object swap = ConcatRelationEditorDialog.this.selectedColumns
								.get(currIndex - 1);
						ConcatRelationEditorDialog.this.selectedColumns
								.setElementAt(selected, currIndex - 1);
						ConcatRelationEditorDialog.this.selectedColumns
								.setElementAt(swap, currIndex);
						// Select the selected item again, as it will
						// have moved.
						keyColList.setSelectedIndex(currIndex - 1);
					}
				}
			}
		});
		downButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					final int currIndex = ConcatRelationEditorDialog.this.selectedColumns
							.indexOf(selected);
					if (currIndex < ConcatRelationEditorDialog.this.selectedColumns
							.size() - 1) {
						// Swap the selected item with the one below it.
						final Object swap = ConcatRelationEditorDialog.this.selectedColumns
								.get(currIndex + 1);
						ConcatRelationEditorDialog.this.selectedColumns
								.setElementAt(selected, currIndex + 1);
						ConcatRelationEditorDialog.this.selectedColumns
								.setElementAt(swap, currIndex);
						// Select the selected item again, as it will
						// have moved.
						keyColList.setSelectedIndex(currIndex + 1);
					}
				}
			}
		});

		// Intercept the close button, which closes the dialog
		// without taking any action.
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ConcatRelationEditorDialog.this.hide();
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (ConcatRelationEditorDialog.this.validateFields()) {
					ConcatRelationEditorDialog.this.createType();
					ConcatRelationEditorDialog.this.hide();
				}
			}
		});

		// Make execute the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set size of window.
		this.pack();
	}

	private void createType() {
		this.type = new DataSetConcatRelationType(this.columnSep.getText(),
				this.rowSep.getText(), new ArrayList(Arrays
						.asList(this.selectedColumns.toArray())));
	}

	private DataSetConcatRelationType getConcatRelationType() {
		return this.type;
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		final List messages = new ArrayList();

		// Must have at least one column selected.
		if (this.selectedColumns.isEmpty())
			messages.add(Resources.get("concatColumnsEmpty"));

		// Must enter something in the column and row separators.
		if (this.rowSep.getText().length() == 0)
			messages
					.add(Resources.get("fieldIsEmpty", Resources.get("rowSep")));
		if (this.columnSep.getText().length() == 0)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("columnSep")));

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
