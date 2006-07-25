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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.biomart.builder.model.Key;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * A dialog which lists all the columns in a key, and all the columns in the
 * table which are available to put in that key. It can then allow the user to
 * move those columns around, thus editing the key.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 20th June 2006
 * @since 0.1
 */
public class KeyEditorDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private DefaultListModel tableColumns;

	private DefaultListModel selectedColumns;

	private KeyEditorDialog(MartTab martTab, Table table, String title,
			String action, List columns) {
		// Create the base dialog.
		super(martTab.getMartTabSet().getMartBuilder(), title, true);

		// The list of table columns is populated with the names of columns.
		this.tableColumns = new DefaultListModel();
		for (Iterator i = table.getColumns().iterator(); i.hasNext();)
			this.tableColumns.addElement(i.next());

		// The list of selected columns is populate with the columns from
		// the existing key. These are also removed from the list of table
		// columns, to prevent duplication.
		this.selectedColumns = new DefaultListModel();
		if (columns != null) {
			for (Iterator i = columns.iterator(); i.hasNext();) {
				Object o = i.next();
				this.tableColumns.removeElement(o);
				this.selectedColumns.addElement(o);
			}
		}

		// The close and execute buttons.
		JButton close = new JButton(Resources.get("closeButton"));
		JButton execute = new JButton(action);

		// Create the table column list, and the buttons
		// to move columns to/from the selected column list.
		final JList tabColList = new JList(this.tableColumns);
		JButton insertButton = new JButton(Resources.get("insertButton"));
		JButton removeButton = new JButton(Resources.get("removeButton"));

		// Create the key column list, and the buttons to
		// move columns to/from the table columns list.
		final JList keyColList = new JList(this.selectedColumns);
		JButton upButton = new JButton(Resources.get("upButton"));
		JButton downButton = new JButton(Resources.get("downButton"));

		// Put the two halves of the dialog side-by-side in a horizontal box.
		Box content = Box.createHorizontalBox();
		this.setContentPane(content);

		// Left-hand side goes the table columns that are unused.
		JPanel leftPanel = new JPanel(new BorderLayout());
		// Label at the top.
		leftPanel.add(new JLabel(Resources.get("columnsAvailableLabel")),
				BorderLayout.PAGE_START);
		// Table columns list in the middle.
		leftPanel.add(new JScrollPane(tabColList), BorderLayout.CENTER);
		leftPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		Box leftButtonPanel = Box.createVerticalBox();
		leftButtonPanel.add(removeButton);
		leftButtonPanel.add(insertButton);
		leftButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		leftPanel.add(leftButtonPanel, BorderLayout.LINE_END);
		content.add(leftPanel);

		// Right-hand side goes the key columns that are used.
		JPanel rightPanel = new JPanel(new BorderLayout());
		// Label at the top.
		rightPanel.add(new JLabel(Resources.get("keyColumnsLabel")),
				BorderLayout.PAGE_START);
		// Key columns in the middle.
		rightPanel.add(new JScrollPane(keyColList), BorderLayout.CENTER);
		rightPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		// Buttons down the right-hand-side, vertically.
		Box rightButtonPanel = Box.createVerticalBox();
		rightButtonPanel.add(upButton);
		rightButtonPanel.add(downButton);
		rightButtonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		rightPanel.add(rightButtonPanel, BorderLayout.LINE_END);
		// Close/Execute buttons at the bottom.
		Box actionButtons = Box.createHorizontalBox();
		actionButtons.add(close);
		actionButtons.add(execute);
		actionButtons.setBorder(new EmptyBorder(2, 2, 2, 2));
		rightPanel.add(actionButtons, BorderLayout.PAGE_END);
		content.add(rightPanel);

		// Intercept the insert/remove buttons
		insertButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selected = tabColList.getSelectedValue();
				if (selected != null) {
					// Move a column from table to key.
					selectedColumns.addElement(selected);
					tableColumns.removeElement(selected);
				}
			}
		});
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					// Move a column from key to table.
					tableColumns.addElement(selected);
					selectedColumns.removeElement(selected);
				}
			}
		});

		// Intercept the up/down buttons
		upButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					int currIndex = selectedColumns.indexOf(selected);
					if (currIndex > 0) {
						// Swap the selected item with the one above it.
						Object swap = selectedColumns.get(currIndex - 1);
						selectedColumns.setElementAt(selected, currIndex - 1);
						selectedColumns.setElementAt(swap, currIndex);
						// Select the selected item again, as it will
						// have moved.
						keyColList.setSelectedIndex(currIndex - 1);
					}
				}
			}
		});
		downButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selected = keyColList.getSelectedValue();
				if (selected != null) {
					int currIndex = selectedColumns.indexOf(selected);
					if (currIndex < (selectedColumns.size() - 1)) {
						// Swap the selected item with the one below it.
						Object swap = selectedColumns.get(currIndex + 1);
						selectedColumns.setElementAt(selected, currIndex + 1);
						selectedColumns.setElementAt(swap, currIndex);
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
			public void actionPerformed(ActionEvent e) {
				hide();
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (validateFields())
					hide();
			}
		});

		// Make execute the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set size of window.
		this.pack();
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		List messages = new ArrayList();

		// Must have at least one column selected.
		if (this.selectedColumns.isEmpty())
			messages.add(Resources.get("keyColumnsEmpty"));

		// Any messages to display? Show them.
		if (!messages.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	/**
	 * Creates a new primary key, or rather creates a list of columns for the
	 * calling code to create a key with.
	 * 
	 * @param martTab
	 *            the mart tab this all belongs to.
	 * @param table
	 *            the table the key is to be created on.
	 * @return the list of columns the user selected.
	 */
	public static List createPrimaryKey(MartTab martTab, Table table) {
		KeyEditorDialog dialog = new KeyEditorDialog(martTab, table, Resources
				.get("newPKDialogTitle"), Resources.get("addButton"), null);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		return Arrays.asList(dialog.selectedColumns.toArray());
	}

	/**
	 * Creates a new foreign key, or rather creates a list of columns for the
	 * calling code to create a key with.
	 * 
	 * @param martTab
	 *            the mart tab this all belongs to.
	 * @param table
	 *            the table the key is to be created on.
	 * @return the list of columns the user selected.
	 */
	public static List createForeignKey(MartTab martTab, Table table) {
		KeyEditorDialog dialog = new KeyEditorDialog(martTab, table, Resources
				.get("newFKDialogTitle"), Resources.get("addButton"), null);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		return Arrays.asList(dialog.selectedColumns.toArray());
	}

	/**
	 * Edits an existing key, or rather creates a list of columns for the
	 * calling code to edit the key with.
	 * 
	 * @param martTab
	 *            the mart tab this all belongs to.
	 * @param key
	 *            the key to be edited.
	 * @return the list of columns the user selected.
	 */
	public static List editKey(MartTab martTab, Key key) {
		KeyEditorDialog dialog = new KeyEditorDialog(martTab, key.getTable(),
				Resources.get("editKeyDialogTitle"), Resources
						.get("modifyButton"), key.getColumns());
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		return Arrays.asList(dialog.selectedColumns.toArray());
	}
}