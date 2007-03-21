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

package org.biomart.common.view.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.common.model.Schema;
import org.biomart.common.model.Schema.JDBCSchema;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel.StringStringTablePanel;

/**
 * This dialog asks users to create or modify a restriction over a particular
 * table for this dataset only.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class SchemaPartitionDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JButton cancel;

	private boolean cancelled;

	private TwoColumnTablePanel partitionModel;

	private JButton execute;

	/**
	 * Pops up a dialog to manage the internal partitions of the given schema.
	 * 
	 * @param schema
	 *            the schema to manage the partitions for.
	 * @return the updated partition map. Keys are actual schema names, and
	 *         values are the aliases to use in dataset table names.
	 */
	public static Map definePartitions(final Schema schema) {
		final SchemaPartitionDialog dialog = new SchemaPartitionDialog(schema);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		if (dialog.cancelled)
			return schema.getPartitions();
		else
			return dialog.getPartitions();
	}

	private SchemaPartitionDialog(final Schema template) {
		// Creates the basic dialog.
		super();
		this.setTitle(Resources.get("schemaPartitionDialogTitle"));
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

		// First table aliases.
		this.partitionModel = new StringStringTablePanel(template
				.getPartitions()) {
			private static final long serialVersionUID = 1L;

			public String getInsertButtonText() {
				return Resources.get("insertPartitionButton");
			}

			public String getRemoveButtonText() {
				return Resources.get("removePartitionButton");
			}

			public String getFirstColumnHeader() {
				return Resources.get("partitionedSchemaHeader");
			}

			public String getSecondColumnHeader() {
				return Resources.get("partitionedSchemaPrefixHeader");
			}

			public Object getNewRowSecondColumn() {
				return "";
			}
		};

		// Create the template name.
		final JTextField templateName = new JTextField(((JDBCSchema) template)
				.getDatabaseSchema());
		templateName.setEnabled(false);

		// Create the buttons.
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = template == null ? new JButton(Resources
				.get("addButton")) : new JButton(Resources.get("modifyButton"));

		// Add the aliases.
		JLabel label = new JLabel(Resources.get("templateSchemaLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(templateName);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the partitions.
		label = new JLabel(Resources.get("partitionedSchemasLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.partitionModel);
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
				SchemaPartitionDialog.this.hide();
			}
		});

		// Intercept the execute button and use it to create
		// the appropriate partition type, then close the dialog.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SchemaPartitionDialog.this.validateFields()) {
					SchemaPartitionDialog.this.cancelled = false;
					SchemaPartitionDialog.this.hide();
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

		// (There is no validation in this box.)

		// If there any messages, display them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	private Map getPartitions() {
		return this.partitionModel.getValues();
	}
}
