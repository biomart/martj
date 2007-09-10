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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.biomart.builder.model.Schema;
import org.biomart.builder.view.gui.panels.TwoColumnTablePanel.StringStringTablePanel;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * This dialog asks users to create or modify a restriction over a particular
 * table for this dataset only.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class PartitionSchemaDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private JTextField regex;

	private JTextField expression;

	private JButton execute;

	private StringStringTablePanel preview;

	/**
	 * Pops up a dialog to manage the internal partitions of the given schema.
	 * 
	 * @return <tt>true</tt> if the user's actions led to a change in the
	 *         partitioning method.
	 */
	public boolean definePartitions() {
		// Centre the dialog.
		this.setLocationRelativeTo(null);

		// Show the dialog.
		this.setVisible(true);

		// Return true if not cancelled - ie. values changed.
		return !this.cancelled;
	}

	/**
	 * Creates but does not show a schema partition dialog populated with the
	 * partition regex of the specified schema template.
	 * 
	 * @param template
	 *            the existing schema to copy partition regexes from.
	 */
	public PartitionSchemaDialog(final Schema template) {
		// Creates the basic dialog.
		super();
		this.setTitle(Resources.get("schemaPartitionDialogTitle"));
		this.setModal(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Remembers the dataset tabset this dialog is referring to.
		this.cancelled = true;

		// Create the content pane to store the create dialog panel.
		final JPanel content = new JPanel(new GridBagLayout());
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

		// Create the template name.
		final JTextField templateName = new JTextField(template
				.getDataLinkSchema());
		templateName.setEnabled(false);

		// Create the regex and expression fields.
		this.regex = new JTextField(50);
		this.regex.setText(template.getPartitionRegex());
		this.expression = new JTextField(20);
		this.expression.setText(template.getPartitionNameExpression());

		// Two-column string/string panel of matches
		Map partitions;
		try {
			partitions = template.getPartitions();
		} catch (final SQLException e) {
			StackTrace.showStackTrace(e);
			partitions = Collections.EMPTY_MAP;
		}
		this.preview = new StringStringTablePanel(partitions) {
			private static final long serialVersionUID = 1L;

			public String getFirstColumnHeader() {
				return Resources.get("partitionedSchemaHeader");
			}

			public String getSecondColumnHeader() {
				return Resources.get("partitionedSchemaPrefixHeader");
			}
		};

		// On-change listener for regex+expression to update panel of matches
		// by creating a temporary dummy schema with the specified regexes and
		// seeing what it produces. Alerts if nothing produced.
		final DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				this.changed();
			}

			public void insertUpdate(DocumentEvent e) {
				this.changed();
			}

			public void removeUpdate(DocumentEvent e) {
				this.changed();
			}

			private void changed() {
				final String oldRegex = template.getPartitionRegex();
				final String oldExpr = template.getPartitionNameExpression();
				final String newRegex = PartitionSchemaDialog.this.getRegex();
				final String newExpr = PartitionSchemaDialog.this
						.getExpression();
				if (newRegex != null && newExpr != null)
					try {
						template.setPartitionRegex(newRegex);
						template.setPartitionNameExpression(newExpr);
						Map partitions;
						try {
							partitions = template.getPartitions();
						} catch (final SQLException e) {
							StackTrace.showStackTrace(e);
							partitions = Collections.EMPTY_MAP;
						}
						PartitionSchemaDialog.this.preview
								.setValues(partitions);
						PartitionSchemaDialog.this.pack();
					} catch (final Throwable t) {
						StackTrace.showStackTrace(t);
					} finally {
						template.setPartitionRegex(oldRegex);
						template.setPartitionNameExpression(oldExpr);
					}
			}
		};
		this.regex.getDocument().addDocumentListener(dl);
		this.expression.getDocument().addDocumentListener(dl);

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

		// Add the aliases.
		JLabel label = new JLabel(Resources.get("templateSchemaLabel"));
		content.add(label, labelConstraints);
		JPanel field = new JPanel();
		field.add(templateName);
		content.add(field, fieldConstraints);

		// Fields for the regex and expression
		label = new JLabel(Resources.get("schemaRegexLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(this.regex);
		content.add(field, fieldConstraints);
		label = new JLabel(Resources.get("schemaExprLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(this.expression);
		content.add(field, fieldConstraints);

		// Two-column string/string panel of matches
		label = new JLabel(Resources.get("partitionedSchemasLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(this.preview);
		content.add(field, fieldConstraints);

		// Add the buttons to the dialog.
		label = new JLabel();
		content.add(label, labelLastRowConstraints);
		field = new JPanel();
		field.add(this.cancel);
		field.add(this.execute);
		content.add(field, fieldLastRowConstraints);

		// Intercept the cancel button and use it to close this
		// dialog without making any changes.
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				PartitionSchemaDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (PartitionSchemaDialog.this.validateFields()) {
					PartitionSchemaDialog.this.cancelled = false;
					PartitionSchemaDialog.this.setVisible(false);
				}
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(this.execute);

		// Set the size of the dialog.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);

		// Aliases were already copied in the JTable constructor above.
	}

	private boolean validateFields() {
		// A placeholder to hold the validation messages, if any.
		final List messages = new ArrayList();

		// Check regex+expression are both missing or both present (EOR).
		if (this.isEmpty(this.regex.getText())
				^ this.isEmpty(this.expression.getText()))
			messages.add(Resources.get("schemaRegexExprEmpty"));

		// Check that it generated any at all.
		if (!this.isEmpty(this.regex.getText())
				&& this.preview.getValues().isEmpty())
			messages.add(Resources.get("schemaRegexExprNoMatch"));

		// If there any messages, display them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Find out what regex the user wants. If null, then no partitioning is
	 * required.
	 * 
	 * @return the regex, or null.
	 */
	public String getRegex() {
		return this.isEmpty(this.regex.getText()) ? null : this.regex.getText()
				.trim();
	}

	/**
	 * Find out what expression the user typed. Even if they typed something, if
	 * regex is null then this will be too.
	 * 
	 * @return the expression, or null if no regex is being used.
	 */
	public String getExpression() {
		return this.getRegex() == null ? null : this.expression.getText()
				.trim();
	}
}
