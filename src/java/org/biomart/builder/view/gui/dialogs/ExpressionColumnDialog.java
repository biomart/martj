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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.view.gui.panels.DataSetColumnStringTablePanel;
import org.biomart.common.model.Key;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel;

/**
 * This dialog asks users to create or modify an expression column.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class ExpressionColumnDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private TwoColumnTablePanel columnAliasModel;

	private JButton execute;

	private JTextArea expression;

	private JCheckBox groupBy;

	private JCheckBox optimiser;

	private DataSetTable table;

	/**
	 * Creates (but does not open) a dialog requesting details of expression
	 * column.
	 * 
	 * @param table
	 *            the table to source columns from.
	 * @param template
	 *            the column to use as a template, if any.
	 * @param skipIncludeCol
	 *            the column not to include in the selection - to prevent
	 *            self-recursion.
	 */
	public ExpressionColumnDialog(final DataSetTable table,
			final ExpressionColumnDefinition template,
			final DataSetColumn skipIncludeCol) {
		// Creates the basic dialog.
		super();
		this.setTitle(template == null ? Resources.get("addExpColDialogTitle")
				: Resources.get("modifyExpColDialogTitle"));
		this.setModal(true);

		// Remembers the dataset tabset this dialog is referring to.
		this.table = table;
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
		final Map defaults = new HashMap();
		if (template != null)
			for (final Iterator i = template.getAliases().entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				defaults.put(table.getColumnByName((String) entry.getKey()),
						entry.getValue());
			}
		this.expression = new JTextArea(10, 40); // Arbitrary size.
		this.columnAliasModel = new DataSetColumnStringTablePanel(defaults,
				table.getColumns(), skipIncludeCol) {
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

		this.groupBy = new JCheckBox(Resources.get("groupbyLabel"));
		this.optimiser = new JCheckBox(Resources.get("optimiserLabel"));

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

		// Add the column aliases table.
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

		// Add the group-by option.
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.groupBy);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the optimiser option.
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.optimiser);
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
		
		// Make sure group-by and optimiser are mutually exclusive.
		this.groupBy.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ExpressionColumnDialog.this.optimiser.setEnabled(!ExpressionColumnDialog.this.groupBy.isSelected());
				ExpressionColumnDialog.this.optimiser.setSelected(!ExpressionColumnDialog.this.groupBy.isSelected());
			}
		});
		this.optimiser.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ExpressionColumnDialog.this.groupBy.setEnabled(!ExpressionColumnDialog.this.optimiser.isSelected());
				ExpressionColumnDialog.this.groupBy.setSelected(!ExpressionColumnDialog.this.optimiser.isSelected());
			}
		});

		// Intercept the cancel button and use it to close this
		// dialog without making any changes.
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ExpressionColumnDialog.this.hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (ExpressionColumnDialog.this.validateFields()) {
					ExpressionColumnDialog.this.cancelled = false;
					ExpressionColumnDialog.this.hide();
				}
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set some nice defaults.
		if (template != null) {
			this.expression.setText(template.getExpression());
			this.groupBy.setSelected(template.isGroupBy());
			this.optimiser.setSelected(template.isOptimiser());
			// Aliases were already copied in the JTable constructor above.
		}

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

		// Validate other fields.
		if (this.columnAliasModel.getValues().isEmpty())
			messages.add(Resources.get("columnAliasMissing"));

		// If group-by is selected, we can't allow them to use columns
		// involved in any keys.
		if (this.getGroupBy()) {
			final Collection aliasCols = this.getColumnAliases().keySet();
			boolean keyFree = true;
			for (final Iterator i = this.table.getKeys().iterator(); i
					.hasNext()
					&& keyFree;) {
				final Key k = (Key) i.next();
				for (final Iterator j = k.getColumns().iterator(); j.hasNext()
						&& keyFree;)
					if (aliasCols.contains(j.next()))
						keyFree = false;
			}
			if (!keyFree)
				messages.add(Resources.get("columnAliasIncludesKeyCols"));
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
	 * Return <tt>true</tt> if the user selected the group-by box.
	 * 
	 * @return the group-by flag.
	 */
	public boolean getGroupBy() {
		return this.groupBy.isSelected();
	}

	/**
	 * Return <tt>true</tt> if the user selected the optimiser box.
	 * 
	 * @return the optimiser flag.
	 */
	public boolean getOptimiser() {
		return this.optimiser.isSelected();
	}
}
