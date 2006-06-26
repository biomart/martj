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

import java.awt.Dimension;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.SaveDDLMartConstructor;
import org.biomart.builder.controller.SaveDDLMartConstructor.SaveDDLGranularity;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructorListener;
import org.biomart.builder.resources.BuilderBundle;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * A dialog which allows the user to choose some options about creating DDL over
 * a given set of datasets, then lets them actually do it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 26th June 2006
 * @since 0.1
 */
public class SaveDDLDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	private Collection datasets;

	private JFileChooser zipFileChooser;

	private FileFilter zipFileFilter;

	private JTextField zipFileLocation;

	private JTextField targetSchemaName;

	private JList datasetsList;

	private JComboBox granularity;

	private JButton zipFileLocationButton;

	private JCheckBox includeComments;

	private JCheckBox viewDDL;

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
	public SaveDDLDialog(MartTab martTab, Collection datasets) {
		// Create the base dialog.
		super(martTab.getMartTabSet().getMartBuilder(), BuilderBundle
				.getString("saveDDLDialogTitle"), true);

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
		this.targetSchemaName = new JTextField(20);
		this.includeComments = new JCheckBox(BuilderBundle
				.getString("includeCommentsLabel"));
		this.includeComments.setSelected(true);
		this.viewDDL = new JCheckBox(BuilderBundle
				.getString("viewDDLOnCompletion"));
		this.granularity = new JComboBox(new Object[] {
				SaveDDLGranularity.SINGLE, SaveDDLGranularity.MART,
				SaveDDLGranularity.DATASET, SaveDDLGranularity.STEP, });

		// Create the list for choosing datasets.
		this.datasetsList = new JList((DataSet[]) datasets
				.toArray(new DataSet[0]));
		this.datasetsList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.datasetsList.setSelectionInterval(0, this.datasets.size() - 1);
		this.datasetsList.setVisibleRowCount(4); // Arbitrary.
		// Set the list to 30-characters wide. Longer than this and it will
		// show a horizontal scrollbar.
		this.datasetsList
				.setPrototypeCellValue("012345678901234567890123456789");

		// Create a file chooser for finding the ZIP file where
		// we will save.
		this.zipFileChooser = new JFileChooser();
		this.zipFileFilter = new FileFilter() {
			private boolean isZipped() {
				SaveDDLGranularity gran = (SaveDDLGranularity) granularity
						.getSelectedItem();
				return gran != null && gran.getZipped();
			}

			// Accepts only files ending in ".zip".
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().toLowerCase().endsWith(
						this.isZipped() ? ".zip" : ".ddl"));
			}

			public String getDescription() {
				return BuilderBundle
						.getString(this.isZipped() ? "ZipDDLFileFilterDescription"
								: "DDLFileFilterDescription");
			}
		};
		this.zipFileChooser.setFileFilter(this.zipFileFilter);
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

		// Add listeners to view DDL and granularity.
		this.viewDDL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (viewDDL.isSelected()) {
					zipFileLocation.setText(null);
					zipFileLocation.setEnabled(false);
					zipFileLocationButton.setEnabled(false);
				} else {
					zipFileLocation.setEnabled(true);
					zipFileLocationButton.setEnabled(true);
				}
			}
		});
		this.granularity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (granularity.getSelectedItem() != null
						&& granularity.getSelectedItem().equals(
								SaveDDLGranularity.SINGLE)) 
					viewDDL.setEnabled(true);
				else {
					if (viewDDL.isSelected()) viewDDL.doClick();
					viewDDL.setEnabled(false);
				}
			}
		});
		this.granularity.setSelectedItem(SaveDDLGranularity.SINGLE);

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

		// Add the target schema settings label and field.
		label = new JLabel(BuilderBundle.getString("targetSchemaLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.targetSchemaName);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the granularity label and field, and the comments checkbox.
		label = new JLabel(BuilderBundle.getString("granularityLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.granularity);
		field.add(this.includeComments);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the view DDL field.
		label = new JLabel();
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.viewDDL);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the zip DDL location label, field and file chooser button.
		label = new JLabel(BuilderBundle.getString("saveDDLFileLocationLabel"));
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
		if (!this.viewDDL.isSelected()
				&& this.isEmpty((String) this.zipFileLocation.getText()))
			messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle
					.getString("saveDDLFileLocation")));

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
		final StringBuffer sb = new StringBuffer();
		MartConstructor constructor;
		if (this.viewDDL.isSelected())
			constructor = new SaveDDLMartConstructor(
					(SaveDDLGranularity) this.granularity.getSelectedItem(),
					null, sb, this.includeComments.isSelected());
		else
			constructor = new SaveDDLMartConstructor(
					(SaveDDLGranularity) this.granularity.getSelectedItem(),
					new File(this.zipFileLocation.getText()), null,
					this.includeComments.isSelected());
		try {
			this.martTab.getMartTabSet().requestMonitorConstructorRunnable(
					constructor.getConstructorRunnable(this.targetSchemaName
							.getText(), selectedDataSets),
					new MartConstructorListener() {
						public void mcEventOccurred(int event) {
							if (event == MartConstructorListener.CONSTRUCTION_FINISHED)
								displayTextPane(sb);
						}
					});
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
			JOptionPane.showMessageDialog(this.martTab.getMartTabSet()
					.getMartBuilder(), BuilderBundle
					.getString("martConstructionFailed"), BuilderBundle
					.getString("messageTitle"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private void displayTextPane(StringBuffer textBuffer) {
		// Build the text pane.
		JEditorPane editorPane = new JEditorPane("text/plain", textBuffer
				.toString());

		// Put the editor pane in a scroll pane.
		JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// Arbitrarily resize the scrollpane.
		editorScrollPane.setPreferredSize(new Dimension(600, 400));

		// Show the output.
		JOptionPane.showMessageDialog(this.martTab, editorScrollPane,
				BuilderBundle.getString("mcViewDDLWindowTitle"),
				JOptionPane.INFORMATION_MESSAGE);
	}
}
