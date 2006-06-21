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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.ZippedDDLMartConstructor;
import org.biomart.builder.controller.ZippedDDLMartConstructor.ZippedDDLGranularity;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.resources.BuilderBundle;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * A dialog which allows the user to choose some options about creating DDL over
 * a given set of datasets, then lets them actually do it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 20th June 2006
 * @since 0.1
 */
public class CreateDDLDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	private Collection datasets;

	private JFileChooser zipFileChooser;

	private JTextField zipFileLocation;

	private JTextField targetSchemaName;

	private JList datasetsList;

	private JComboBox granularity;

	private JButton zipFileLocationButton;

	/**
	 * Creates (but does not display) a dialog centred on the given tab, which
	 * allows construction of the given datasets. When the OK button is chosen,
	 * the datasets are made.
	 * 
	 * @param martTab
	 *            the tab in which this will be displayed.
	 * @param datasets
	 *            the datasets to list.
	 */
	public CreateDDLDialog(MartTab martTab, Collection datasets) {
		// Create the base dialog.
		super(martTab.getMartTabSet().getMartBuilder(), BuilderBundle
				.getString("createDDLDialogTitle"), true);

		// Remember the tabset that the schema we are working with is part of
		// (or will be part of if it's not been created yet).
		this.martTab = martTab;
		this.datasets = datasets;

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

		// Create input fields for target schema name and granularity.
		this.targetSchemaName = new JTextField(15);
		this.granularity = new JComboBox(new Object[] {
				ZippedDDLGranularity.SINGLE, ZippedDDLGranularity.MART,
				ZippedDDLGranularity.DATASET, ZippedDDLGranularity.STEP, });

		// Create the list for choosing datasets.
		this.datasetsList = new JList((DataSet[]) datasets
				.toArray(new DataSet[0]));
		this.datasetsList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.datasetsList.setSelectionInterval(0, this.datasets.size() - 1);
		this.datasetsList.setVisibleRowCount(4); // Arbitrary.
		// Set the list to 30-characters wide. Longer than this and it will
		// show a horizontal scrollbar.
		this.datasetsList.setPrototypeCellValue("012345678901234567890123456789");

		// Create a file chooser for finding the ZIP file where
		// we will save.
		this.zipFileChooser = new JFileChooser();
		this.zipFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".zip".
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().toLowerCase().endsWith(
						".zip"));
			}

			public String getDescription() {
				return BuilderBundle.getString("ZipDDLFileFilterDescription");
			}
		});
		this.zipFileLocation = new JTextField(20);
		this.zipFileLocationButton = new JButton(BuilderBundle
				.getString("browseButton"));

		// Attach the file chooser to the driver class location button.
		this.zipFileLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (zipFileChooser.showSaveDialog(content) == JFileChooser.APPROVE_OPTION) {
					File file = zipFileChooser.getSelectedFile();
					// When a file is chosen, put its name in the driver
					// class location field.
					if (file != null)
						zipFileLocation.setText(file.toString());
				}
			}
		});

		// Lay out the window.

		// Add the dataset lists.
		JLabel label = new JLabel(BuilderBundle
				.getString("selectedDataSetsLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(new JScrollPane(this.datasetsList));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the target schema settings label and field, with
		// the granularity beside it.
		label = new JLabel(BuilderBundle.getString("targetSchemaLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.targetSchemaName);
		label = new JLabel(BuilderBundle.getString("granularityLabel"));
		field.add(label);
		field.add(this.granularity);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the zip DDL location label, field and file chooser button.
		label = new JLabel(BuilderBundle.getString("zipFileLocationLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.zipFileLocation);
		field.add(this.zipFileLocationButton);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// The close and execute buttons.
		JButton cancel = new JButton(BuilderBundle.getString("cancelButton"));
		JButton execute = new JButton(BuilderBundle.getString("saveDDLButton"));

		// Intercept the close button, which closes the dialog
		// without taking any action.
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hide();
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (validateFields()) {
					createDDL();
					hide();
				}
			}
		});

		// Add the buttons.
		label = new JLabel();
		gridBag.setConstraints(label, labelLastRowConstraints);
		content.add(label);
		field = new JPanel();
		field.add(cancel);
		field.add(execute);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		content.add(field);

		// Make execute the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
	}

	private boolean isEmpty(String string) {
		// Strings are empty if they are null or all whitespace.
		return (string == null || string.trim().length() == 0);
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		List messages = new ArrayList();

		// Must have a target schema.
		if (this.isEmpty((String) this.targetSchemaName.getText()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("targetSchema")));

		// Must have an output file.
		if (this.isEmpty((String) this.zipFileLocation.getText()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("zipFileLocation")));

		// Must have at least one dataset selected.
		if (this.datasetsList.getSelectedValues().length == 0)
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("selectedDataSets")));

		// Any messages to display? Show them.
		if (!messages.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), BuilderBundle
							.getString("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}

	private void createDDL() {
		List selectedDataSets = Arrays.asList(this.datasetsList
				.getSelectedValues());
		MartConstructor constructor = new ZippedDDLMartConstructor(
				(ZippedDDLGranularity) this.granularity.getSelectedItem(),
				new File(this.zipFileLocation.getText()));
		try {
			this.martTab.getMartTabSet().requestMonitorConstructorRunnable(
					constructor.getConstructorRunnable(this.targetSchemaName
							.getText(), selectedDataSets));
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}
}
