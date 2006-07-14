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

package org.biomart.builder.view.gui;

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

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Mart.DataSetSuggestionMode;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This dialog asks users what kind of dataset suggestion they want to do.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 13th July 2006
 * @since 0.1
 */
public class SuggestDataSetDialog extends JDialog {
	private static final long serialVersionUID = 1;
	
	private DataSetSuggestionMode mode;

	private MartTab martTab;

	private JRadioButton identity;
	
	private JRadioButton simple;

	private JRadioButton combined;

	private JList tables;

	private JButton cancel;

	private JButton execute;

	private SuggestDataSetDialog(final MartTab martTab, Table initialTable) {
		// Creates the basic dialog.
		super(martTab.getMartTabSet().getMartBuilder(), Resources
				.get("suggestDataSetDialogTitle"), true);

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

		// Create the fields that will contain the user's table choices
		// and choice of suggestion scheme.
		this.identity = new JRadioButton(Resources
				.get("suggestIdentityDataSetOption"));
		this.simple = new JRadioButton(Resources
				.get("suggestSimpleDataSetOption"));
		this.combined = new JRadioButton(Resources
				.get("suggestCombinedDataSetOption"));
		ButtonGroup radio = new ButtonGroup();
		radio.add(identity);
		radio.add(simple);
		radio.add(combined);

		List availableTables = new ArrayList();
		for (Iterator i = this.martTab.getMart().getSchemas().iterator(); i
				.hasNext();)
			for (Iterator j = ((Schema) i.next()).getTables().iterator(); j
					.hasNext();)
				availableTables.add(j.next());
		this.tables = new JList((Table[]) availableTables.toArray(new Table[0]));
		this.tables.setVisibleRowCount(10); // Arbitrary.
		// Set the list to 50-characters wide. Longer than this and it will
		// show a horizontal scrollbar.
		this.tables
				.setPrototypeCellValue("01234567890123456789012345678901234567890123456789");
		this.tables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		// Make the simple/combined choice change the selectability of the
		// list of tables.
		this.identity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (identity.isSelected()) 
					mode = DataSetSuggestionMode.IDENTITY;
			}
		});
		this.simple.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (simple.isSelected()) 
					mode = DataSetSuggestionMode.SIMPLE;
			}
		});
		this.combined.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (combined.isSelected())
					mode = DataSetSuggestionMode.COMBINED;
			}
		});

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(Resources.get("suggestButton"));

		// Add the list of tables.
		JLabel label = new JLabel(Resources.get("suggestDSTablesLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(new JScrollPane(this.tables));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the radios to the dialog.
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.identity);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.simple);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.combined);
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
				mode = null;
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

		// Activate the single-table option.
		this.tables.setSelectedValue(initialTable, true);
		this.simple.doClick();
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		List messages = new ArrayList();

		// We must have a partition type!
		if (this.tables.getSelectedValues().length == 0)
			messages
					.add(Resources.get("suggestDSTablesEmpty"));

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

	private Collection getSelectedTables() {
		return Arrays.asList(this.tables.getSelectedValues());
	}
	
	private DataSetSuggestionMode getDataSetSuggestionMode() {
		return this.mode;
	}

	/**
	 * This opens a dialog in order for the user to select a suggestion mode and
	 * one or more tables. It populates the selected table(s), and returns the
	 * selection mode. If the user cancelled it, the mode will be null.
	 * 
	 * @param martTab
	 *            the mart tab this dialog is creating a partition type for.
	 * @param initialTable
	 *            the table to select initially.
	 * @param finalTables
	 *            the final list of selected tables. This list will be populated
	 *            by this method. 
	 * @return the suggestion mode selected by the user, or null if they
	 * cancelled it.
	 */
	public static DataSetSuggestionMode suggestDataSet(MartTab martTab, Table initialTable,
			Collection finalTables) {
		SuggestDataSetDialog dialog = new SuggestDataSetDialog(martTab,
				initialTable);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		finalTables.addAll(dialog.getSelectedTables());
		return dialog.getDataSetSuggestionMode();
	}
}
