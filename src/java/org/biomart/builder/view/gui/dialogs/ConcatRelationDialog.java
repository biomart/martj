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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.biomart.builder.model.SchemaModificationSet;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel.CRPairStringTablePanel;

/**
 * This dialog asks users to create or modify a restriction over a particular
 * table for this dataset only.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class ConcatRelationDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private TwoColumnTablePanel columnAliasModel;

	private JButton execute;

	private JTextArea expression;

	private JTextField rowSep;

	private JTextField concSep;

	private JComboBox recursionType;

	private JComboBox recursionKey;

	private JComboBox firstRelation;

	private JComboBox secondRelation;

	/**
	 * Creates (but does not open) a dialog requesting details of a restricted
	 * table.
	 * 
	 * @param table
	 *            the table to restrict.
	 * @param template
	 *            the restriction to use as a template, if any.
	 */
	public ConcatRelationDialog(final Table table,
			final SchemaModificationSet.ConcatRelationDefinition template) {
		// Creates the basic dialog.
		super();
		this.setTitle(template == null ? Resources.get("newConcatDialogTitle")
				: Resources.get("editConcatDialogTitle"));
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
		this.rowSep = new JTextField(5);
		this.concSep = new JTextField(5);

		// Work out what column/relation pairs are available to us.
		final List colsAvailable = new ArrayList();
		for (final Iterator j = table.getColumns().iterator(); j.hasNext(); ) 
			colsAvailable.add(new Object[]{null, j.next()});
		for (final Iterator i = table.getRelations().iterator(); i.hasNext(); ) {
			final Relation rel = (Relation)i.next();
			if (rel.isOneToMany() && rel.getManyKey().getTable().equals(table) && rel.getOneKey().getTable().getSchema().equals(table.getSchema()))
				for (final Iterator j = rel.getOneKey().getTable().getColumns().iterator(); j.hasNext(); ) 
					colsAvailable.add(new Object[]{rel, j.next()});
		}
		
		// First table aliases.
		this.columnAliasModel = new CRPairStringTablePanel(
				template == null ? null : template.getAliases(), colsAvailable) {
			private static final long serialVersionUID = 1L;

			private int alias = 1;

			public String getInsertButtonText() {
				return Resources.get("insertAliasButton");
			}

			public String getRemoveButtonText() {
				return Resources.get("removeAliasButton");
			}

			public String getFirstColumnHeader() {
				return Resources.get("columnAliasTableColHeader");
			}

			public String getSecondColumnHeader() {
				return Resources.get("columnAliasTableAliasHeader");
			}

			public Object getNewRowSecondColumn() {
				return Resources.get("defaultAlias") + this.alias++;
			}
		};

		// Recursion stuff.
		final GridBagLayout recursionGrid = new GridBagLayout();
		final JPanel recursionPanel = new JPanel(recursionGrid);
		this.recursionType = new JComboBox(
				new RecursionType[] { RecursionType.NONE,
						RecursionType.PREPEND, RecursionType.APPEND });
		this.recursionKey = new JComboBox(table.getKeys().toArray(new Key[0]));
		this.firstRelation = new JComboBox();
		this.secondRelation = new JComboBox();
		this.recursionType.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				recursionPanel
						.setVisible(ConcatRelationDialog.this.recursionType
								.getSelectedItem() != RecursionType.NONE);
				ConcatRelationDialog.this.pack();
			}
		});
		this.recursionKey.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Collection firstRels = Collections.EMPTY_SET;
				final Key key = (Key) ConcatRelationDialog.this.recursionKey
						.getSelectedItem();
				if (key != null) {
					firstRels = new ArrayList(key.getRelations());
					for (final Iterator i = firstRels.iterator(); i.hasNext();)
						if (((Relation) i.next()).isOneToOne())
							i.remove();
				}
				ConcatRelationDialog.this.firstRelation.removeAllItems();
				for (final Iterator i = firstRels.iterator(); i.hasNext();)
					ConcatRelationDialog.this.firstRelation.addItem(i.next());
				if (firstRels.size() > 0)
					ConcatRelationDialog.this.firstRelation.setSelectedIndex(0);
				ConcatRelationDialog.this.pack();
			}
		});
		this.firstRelation.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Collection secondRels = Collections.EMPTY_SET;
				final Key key = (Key) ConcatRelationDialog.this.recursionKey
						.getSelectedItem();
				final Table tbl = key.getTable();
				final Relation rel = (Relation) ConcatRelationDialog.this.firstRelation
						.getSelectedItem();
				if (rel != null) {
					if (rel.getFirstKey().getTable().equals(
							rel.getSecondKey().getTable()))
						ConcatRelationDialog.this.secondRelation
								.setEnabled(false);
					else {
						ConcatRelationDialog.this.secondRelation
								.setEnabled(true);
						secondRels = new ArrayList(rel.getOtherKey(key)
								.getTable().getRelations());
						for (final Iterator i = secondRels.iterator(); i
								.hasNext();) {
							final Relation candidate = (Relation) i.next();
							if (candidate.isOneToOne()
									|| !(candidate.getFirstKey().getTable()
											.equals(tbl) || candidate
											.getSecondKey().getTable().equals(
													tbl))
									|| candidate.equals(rel))
								i.remove();
						}
					}
				}
				ConcatRelationDialog.this.secondRelation.removeAllItems();
				for (final Iterator i = secondRels.iterator(); i.hasNext();)
					ConcatRelationDialog.this.secondRelation.addItem(i.next());
				if (secondRels.size() > 0)
					ConcatRelationDialog.this.secondRelation
							.setSelectedIndex(0);
				ConcatRelationDialog.this.pack();
			}
		});

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

		// Add the aliases.
		JLabel label = new JLabel(Resources.get("columnAliasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(this.columnAliasModel);
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

		// Add the recursion combo box.
		label = new JLabel(Resources.get("recursionTypeLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.recursionType);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the recursion panel.
		label = new JLabel(Resources.get("recursionKeyLabel"));
		recursionGrid.setConstraints(label, labelConstraints);
		recursionPanel.add(label);
		field = new JPanel();
		field.add(this.recursionKey);
		recursionGrid.setConstraints(field, fieldConstraints);
		recursionPanel.add(field);
		label = new JLabel(Resources.get("firstRelationLabel"));
		recursionGrid.setConstraints(label, labelConstraints);
		recursionPanel.add(label);
		field = new JPanel();
		field.add(this.firstRelation);
		recursionGrid.setConstraints(field, fieldConstraints);
		recursionPanel.add(field);
		label = new JLabel(Resources.get("secondRelationLabel"));
		recursionGrid.setConstraints(label, labelConstraints);
		recursionPanel.add(label);
		field = new JPanel();
		field.add(this.secondRelation);
		recursionGrid.setConstraints(field, fieldConstraints);
		recursionPanel.add(field);
		gridBag.setConstraints(recursionPanel, fieldConstraints);
		label = new JLabel(Resources.get("concSepLabel"));
		recursionGrid.setConstraints(label, labelConstraints);
		recursionPanel.add(label);
		field = new JPanel();
		field.add(this.concSep);
		recursionGrid.setConstraints(field, fieldConstraints);
		recursionPanel.add(field);
		content.add(recursionPanel);

		// Row separator.
		label = new JLabel(Resources.get("rowSepLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.rowSep);
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
				ConcatRelationDialog.this.hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (ConcatRelationDialog.this.validateFields()) {
					ConcatRelationDialog.this.cancelled = false;
					ConcatRelationDialog.this.hide();
				}
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set some nice defaults.
		if (template != null) {
			this.expression.setText(template.getExpression());
			this.rowSep.setText(template.getRowSep());
			this.recursionType.setSelectedItem(template.getRecursionType());
			if (template.getRecursionType() != RecursionType.NONE) {
				this.recursionKey.setSelectedItem(template.getRecursionKey());
				this.firstRelation.setSelectedItem(template.getFirstRelation());
				if (template.getSecondRelation() != null)
					this.secondRelation.setSelectedItem(template
							.getSecondRelation());
				this.concSep.setText(template.getConcSep());
			} else
				this.recursionKey.setSelectedIndex(0);
		} else
			this.recursionType.setSelectedItem(RecursionType.NONE);
		// Aliases were already copied in the JTable constructor above.

		// Set the size of the dialog.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
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

		// We must have a row separator!
		if (this.rowSep.getText().length() == 0)
			messages
					.add(Resources.get("fieldIsEmpty", Resources.get("rowSep")));

		// Validate other fields.
		if (this.columnAliasModel.getValues().isEmpty())
			messages.add(Resources.get("columnAliasMissing"));

		// Recursive?
		if (this.recursionType.getSelectedItem() != RecursionType.NONE) {
			if (this.recursionKey.getSelectedIndex() < 0)
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("recursionKey")));
			if (this.firstRelation.getSelectedIndex() < 0)
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("firstRelation")));
			if (this.secondRelation.isEnabled()
					&& this.secondRelation.getSelectedIndex() < 0)
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("secondRelation")));
			if (this.concSep.getText().length() == 0)
				messages
						.add(Resources.get("fieldIsEmpty", Resources.get("concSep")));
		}

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
		return this.columnAliasModel.getValues();
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
	 * Return the expression the user selected.
	 * 
	 * @return the expression.
	 */
	public String getRowSep() {
		return this.rowSep.getText();
	}
	
	public String getConcSep() {
		return this.concSep.getText();
	}

	public RecursionType getRecursionType() {
		return (RecursionType) this.recursionType.getSelectedItem();
	}

	public Key getRecursionKey() {
		return (Key) this.recursionKey.getSelectedItem();
	}

	public Relation getFirstRelation() {
		return (Relation) this.firstRelation.getSelectedItem();
	}

	public Relation getSecondRelation() {
		return (Relation) this.secondRelation.getSelectedItem();
	}
}
