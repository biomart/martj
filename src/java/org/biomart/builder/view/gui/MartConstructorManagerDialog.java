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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.builder.controller.JDBCMartConstructor;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructor.DummyMartConstructor;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This dialog box allows the user to define or modify a mart constructor, by
 * giving it a name, choosing a type, then displaying the appropriate
 * {@link MartConstructorConnectionPanel} according to the type chosen. The
 * connection panel then is given the job of actually creating or modifying the
 * mart constructor, before the result is returned to the caller.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 6th June 2006
 * @since 0.1
 */
public class MartConstructorManagerDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private DataSetTabSet datasetTabSet;

	private MartConstructor martConstructor;

	private JComboBox type;

	private JTextField name;

	private MartConstructorConnectionPanel connectionPanel;

	private JButton test;

	private JButton cancel;

	private JButton execute;

	private MartConstructorManagerDialog(final DataSetTabSet datasetTabSet,
			String title, String executeButtonText,
			final MartConstructor template) {
		// Create the basic dialog centred on the main mart builder window.
		super(datasetTabSet.getMartTabSet().getMartBuilder(), title, true);

		// Remember the tabset that the schema we are working with is part of
		// (or will be part of if it's not been created yet).
		this.datasetTabSet = datasetTabSet;

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
		this.type = new JComboBox(new String[] {
				BuilderBundle.getString("jdbcMartConstructor"),
				BuilderBundle.getString("dummyMartConstructor") });
		final JPanel connectionPanelHolder = new JPanel();
		this.type.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (type.getSelectedItem().equals(
						BuilderBundle.getString("jdbcMartConstructor"))) {
					if (!(connectionPanel instanceof JDBCMartConstructorConnectionPanel)) {
						connectionPanelHolder.removeAll();
						connectionPanel = new JDBCMartConstructorConnectionPanel(
								datasetTabSet, template);
						connectionPanelHolder.add(connectionPanel);
						pack();
					}
				} else if (type.getSelectedItem().equals(
						BuilderBundle.getString("dummyMartConstructor"))) {
					if (!(connectionPanel instanceof DummyMartConstructorConnectionPanel)) {
						connectionPanelHolder.removeAll();
						connectionPanel = new DummyMartConstructorConnectionPanel(
								datasetTabSet, template);
						connectionPanelHolder.add(connectionPanel);
						pack();
					}
				}
			}
		});

		// Make a default selection for the connection panel holder. Use JDBC
		// as it is the most obvious choice. We have to do something here else
		// the box won't size properly without one.
		this.type.setSelectedItem(BuilderBundle
				.getString("jdbcMartConstructor"));

		// Create buttons in dialog.
		this.test = new JButton(BuilderBundle.getString("testButton"));
		this.cancel = new JButton(BuilderBundle.getString("cancelButton"));
		this.execute = new JButton(executeButtonText);

		// Add the name label and name field.
		JLabel label = new JLabel(BuilderBundle.getString("nameLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);

		// In the name field, also include the type label and field, to save
		// space.
		JPanel field = new JPanel();
		field.add(this.name);
		label = new JLabel(BuilderBundle.getString("typeLabel"));
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
				martConstructor = null;
				hide();
			}
		});

		// Intercept the test button, which causes the schema
		// details as currently entered to be used to create
		// a temporary schema object, which is then tested.
		this.test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MartConstructor testMC = createMartConstructor();
				if (testMC != null)
					datasetTabSet.requestTestMartConstructor(testMC);
			}
		});

		// Intercept the execute button, which causes the
		// schema to be created as a temporary schema object. If
		// successful, the dialog closes.
		this.execute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				martConstructor = createMartConstructor();
				if (martConstructor != null)
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

	private void resetFields(MartConstructor template) {
		// If we are modifying something, use it to fill the details
		// in the dialog.
		if (template != null) {
			// Select the type.
			if (template instanceof JDBCMartConstructor)
				this.type.setSelectedItem(BuilderBundle
						.getString("jdbcMartConstructor"));
			else if (template instanceof DummyMartConstructor)
				this.type.setSelectedItem(BuilderBundle
						.getString("dummyMartConstructor"));

			// Unknown type!
			else
				throw new MartBuilderInternalError();

			// Set the name.
			this.name.setText(template.getName());
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
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("name")));

		// We don't like missing types either.
		if (this.type.getSelectedIndex() == -1)
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("type")));

		// If we have any messages, show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), BuilderBundle
							.getString("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// If there were no messages, then validated OK if the connection
		// panel also validated OK.
		return messages.isEmpty() && this.connectionPanel.validateFields();
	}

	private MartConstructor createMartConstructor() {
		// Refuse to create a temporary object if we can't validate it.
		if (!this.validateFields())
			return null;

		try {
			// Look up the type and use the appropriate type to
			// actually create the object.
			String type = (String) this.type.getSelectedItem();
			if (type.equals(BuilderBundle.getString("jdbcMartConstructor")))
				return ((JDBCMartConstructorConnectionPanel) this.connectionPanel)
						.createMartConstructor(this.name.getText());
			else if (type.equals(BuilderBundle
					.getString("dummyMartConstructor")))
				return ((DummyMartConstructorConnectionPanel) this.connectionPanel)
						.createMartConstructor(this.name.getText());

			// What kind of type is it then??
			else
				throw new MartBuilderInternalError();
		} catch (Throwable t) {
			this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(
					t);
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
	 * Pop up a dialog asking the user for details for a new mart constructor,
	 * then create and return that mart constructor.
	 * 
	 * @param datasetTabSet
	 *            the mart constructor tabset to use when creating the schema.
	 *            @param mc the mart constructor to use as a template.
	 * @return the newly created mart constructor, or null if it was cancelled.
	 */
	public static MartConstructor createMartConstructor(DataSetTabSet datasetTabSet, MartConstructor mc) {
		MartConstructorManagerDialog dialog = new MartConstructorManagerDialog(
				datasetTabSet, BuilderBundle
						.getString("modifyMartConstructorDialogTitle"),
				BuilderBundle.getString("modifyButton"), mc);
		dialog.setLocationRelativeTo(datasetTabSet.getMartTabSet()
				.getMartBuilder());
		dialog.show();
		return dialog.martConstructor;
	}
}
