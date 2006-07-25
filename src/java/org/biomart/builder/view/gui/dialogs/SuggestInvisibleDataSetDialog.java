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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This dialog asks users what kind of invisible dataset suggestion they want to
 * do.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 25th July 2006
 * @since 0.1
 */
public class SuggestInvisibleDataSetDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	private JComboBox tables;

	private JList columns;

	private JButton cancel;

	private JButton execute;

	/**
	 * Creates (but does not open) a dialog requesting details of invisible
	 * dataset suggestion.
	 * 
	 * @param martTab
	 *            the mart tab set to centre ourselves over.
	 * @param table
	 *            the table to source columns from to show in the list.
	 */
	public SuggestInvisibleDataSetDialog(final MartTab martTab,
			final DataSetTable table) {
		// Creates the basic dialog.
		super(martTab.getMartTabSet().getMartBuilder(), Resources
				.get("suggestInvisibleDataSetDialogTitle"), true);

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

		// Create a drop-down list of underlying tables.
		Set underlyingTables = new TreeSet();
		underlyingTables.add(table.getUnderlyingTable());
		this.tables = new JComboBox();
		for (Iterator i = table.getUnderlyingRelations().iterator(); i
				.hasNext();) {
			Relation rel = (Relation) i.next();
			underlyingTables.add(rel.getFirstKey().getTable());
			underlyingTables.add(rel.getSecondKey().getTable());
		}
		for (Iterator i = underlyingTables.iterator(); i.hasNext();)
			this.tables.addItem(i.next());

		// Start with an empty available columns list.
		this.columns = new JList();
		this.columns.setVisibleRowCount(10); // Arbitrary.
		// Set the list to 50-characters wide. Longer than this and it will
		// show a horizontal scrollbar.
		this.columns
				.setPrototypeCellValue("01234567890123456789012345678901234567890123456789");
		this.columns
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		// When underlying table selected, update the
		// available columns list to suit.
		this.tables.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (tables.getSelectedItem() != null) {
					List availableColumns = new ArrayList();
					for (Iterator i = table.getColumns().iterator(); i
							.hasNext();) {
						DataSetColumn col = (DataSetColumn) i.next();
						if ((col instanceof WrappedColumn)
								&& ((WrappedColumn) col).getWrappedColumn().getTable().equals(
										tables.getSelectedItem()))
							availableColumns.add(col);
					}
					columns.setListData((Column[]) availableColumns
							.toArray(new Column[0]));
				}
			}
		});

		// Select the default table.
		this.tables.setSelectedIndex(0);
		
		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(Resources.get("suggestButton"));

		// Add the table name.
		JLabel label = new JLabel(Resources.get("suggestDSTableLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(this.tables);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the list of columns.
		label = new JLabel(Resources.get("suggestDSColumnsLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(new JScrollPane(this.columns));
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
				columns.clearSelection();
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
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		List messages = new ArrayList();

		// We must have a selected column!
		if (this.columns.getSelectedValues().length == 0)
			messages.add(Resources.get("suggestDSColumnsEmpty"));

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

	/**
	 * Return the set of columns the user selected.
	 * 
	 * @return the set of columns the user selected.
	 */
	public Collection getSelectedColumns() {
		return Arrays.asList(this.columns.getSelectedValues());
	}
}
