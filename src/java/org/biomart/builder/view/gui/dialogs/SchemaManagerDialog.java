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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.builder.controller.JDBCSchema;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Schema;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This dialog box allows the user to define or modify a schema, by giving it a
 * name, choosing a type, then displaying the appropriate
 * {@link SchemaConnectionPanel} according to the type chosen. The connection
 * panel then is given the job of actually creating or modifying the schema,
 * before the result is returned to the caller.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 20th June 2006
 * @since 0.1
 */
public class SchemaManagerDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	private Schema schema;

	private JComboBox type;

	private JTextField name;

	private SchemaConnectionPanel connectionPanel;

	private JButton test;

	private JButton cancel;

	private JButton execute;

	private SchemaManagerDialog(final MartTab martTab, String title,
			String executeButtonText, final Schema template) {
		// Create the basic dialog centred on the main mart builder window.
		super(martTab.getMartTabSet().getMartBuilder(), title, true);

		// Remember the tabset that the schema we are working with is part of
		// (or will be part of if it's not been created yet).
		this.martTab = martTab;

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		GridBagLayout gridBag = new GridBagLayout();
		final JPanel content = new JPanel(gridBag);
		this.setContentPane(content);

		// Create some constraints for labels, except those on the last row
		// of the dialog.
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create some constraints for fields, except those on the last row
		// of the dialog.
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create some constraints for labels on the last row of the dialog.
		GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create some constraints for fields on the last row of the dialog.
		GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create the input fields for the name and type, and the
		// holder for the connection panel details.
		this.name = new JTextField(20);
		this.type = new JComboBox(new String[] { Resources.get("jdbcSchema") });
		final JPanel connectionPanelHolder = new JPanel();
		this.type.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (type.getSelectedItem().equals(Resources.get("jdbcSchema"))) {
					if (!(connectionPanel instanceof JDBCSchemaConnectionPanel)) {
						connectionPanelHolder.removeAll();
						connectionPanel = new JDBCSchemaConnectionPanel(
								martTab, template);
						connectionPanelHolder.add(connectionPanel);
						pack();
					}
				}
			}
		});

		// Make a default selection for the connection panel holder. Use JDBC
		// as it is the most obvious choice. We have to do something here else
		// the
		// box won't size properly without one.
		this.type.setSelectedItem(Resources.get("jdbcSchema"));

		// Create buttons in dialog.
		this.test = new JButton(Resources.get("testButton"));
		this.cancel = new JButton(Resources.get("cancelButton"));
		this.execute = new JButton(executeButtonText);

		// Add the name label and name field.
		JLabel label = new JLabel(Resources.get("nameLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);

		// In the name field, also include the type label and field, to save
		// space.
		JPanel field = new JPanel();
		field.add(this.name);
		label = new JLabel(Resources.get("typeLabel"));
		field.add(label);
		field.add(this.type);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the connection panel holder.
		gridBag.setConstraints(connectionPanelHolder, fieldConstraints);
		content.add(connectionPanelHolder);

		// Add the buttons.
		label = new JLabel();
		gridBag.setConstraints(label, labelLastRowConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.test);
		field.add(this.cancel);
		field.add(this.execute);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		content.add(field);

		// Intercept the cancel button, which closes the dialog
		// without taking any action.
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				schema = null;
				hide();
			}
		});

		// Intercept the test button, which causes the schema
		// details as currently entered to be used to create
		// a temporary schema object, which is then tested.
		this.test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Schema testSchema = createSchema();
				if (testSchema != null)
					martTab.getSchemaTabSet().requestTestSchema(testSchema);
			}
		});

		// Intercept the execute button, which causes the
		// schema to be created as a temporary schema object. If
		// successful, the dialog closes.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				schema = createSchema();
				if (schema != null)
					hide();
			}
		});

		// Make the execute button the default button.
		this.getRootPane().setDefaultButton(execute);

		// Reset the fields to their default values.
		this.resetFields(template);

		// Pack and resize the window.
		this.pack();
	}

	private void resetFields(Schema template) {
		// If we are modifying something, use it to fill the details
		// in the dialog.
		if (template != null) {
			// Select the schema type.
			if (template instanceof JDBCSchema)
				this.type.setSelectedItem(Resources.get("jdbcSchema"));

			// Unknown schema type!
			else
				throw new MartBuilderInternalError();
			this.type.setEnabled(false); // Gray out as we can't change this.

			// Set the name.
			this.name.setText(template.getName());
			this.name.setEnabled(false); // Gray out as we can't change this.
		}

		// Otherwise, use some sensible defaults.
		else {
			this.type.setSelectedIndex(0);
			this.name.setText(null);
		}
	}

	private boolean validateFields() {
		// Make a list to hold messages.
		List messages = new ArrayList();

		// We don't like missing names.
		if (this.isEmpty(this.name.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources.get("name")));

		// We don't like missing types either.
		if (this.type.getSelectedIndex() == -1)
			messages.add(Resources.get("fieldIsEmpty", Resources.get("type")));

		// If we have any messages, show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// If there were no messages, then validated OK if the connection
		// panel also validated OK.
		return messages.isEmpty() && this.connectionPanel.validateFields();
	}

	private Schema createSchema() {
		// Refuse to create a temporary schema object if we can't validate it.
		if (!this.validateFields())
			return null;

		try {
			// Look up the type and use the appropriate schema type to
			// actually create the object.
			String type = (String) this.type.getSelectedItem();
			if (type.equals(Resources.get("jdbcSchema")))
				return ((JDBCSchemaConnectionPanel) this.connectionPanel)
						.createSchema(this.name.getText());

			// What kind of type is it then??
			else
				throw new MartBuilderInternalError();
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}

		// If we got here, something went wrong with creation, so we
		// have nothing to return.
		return null;
	}

	private boolean isEmpty(String string) {
		// Return true if the string is null or empty.
		return (string == null || string.trim().length() == 0);
	}

	/**
	 * Pop up a dialog asking the user for details for a new schema, then create
	 * and return that schema.
	 * 
	 * @param martTab
	 *            the mart tab to use when creating the schema.
	 * @return the newly created schema, or null if it was cancelled.
	 */
	public static Schema createSchema(MartTab martTab) {
		SchemaManagerDialog dialog = new SchemaManagerDialog(martTab, Resources
				.get("newSchemaDialogTitle"), Resources.get("addButton"), null);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		return dialog.schema;
	}

	/**
	 * Pop up a dialog asking the user to modify details for a schema, then
	 * modify that schema. Returns whether it was successful or not.
	 * 
	 * @param martTab
	 *            the mart tab to use when creating the schema.
	 * @param schema
	 *            the schema to modify.
	 * @return <tt>true</tt> if modification was successful, <tt>false</tt>
	 *         if not.
	 */
	public static boolean modifySchema(MartTab martTab, Schema schema) {
		SchemaManagerDialog dialog = new SchemaManagerDialog(martTab, Resources
				.get("modifySchemaDialogTitle"), Resources.get("modifyButton"),
				schema);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		if (dialog.schema != null && dialog.schema instanceof JDBCSchema) {
			return (((JDBCSchemaConnectionPanel) dialog.connectionPanel)
					.modifySchema(schema) != null);
		} else {
			return false;
		}
	}
}
