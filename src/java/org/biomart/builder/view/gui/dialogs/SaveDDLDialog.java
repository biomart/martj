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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.MartConstructor;
import org.biomart.builder.controller.SaveDDLMartConstructor;
import org.biomart.builder.controller.MartConstructor.ConstructorRunnable;
import org.biomart.builder.controller.MartConstructor.MartConstructorListener;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.common.view.gui.dialogs.ViewTextDialog;

/**
 * A dialog which allows the user to choose some options about creating DDL over
 * a given set of datasets, then lets them actually do it. The options include
 * whether to output to file or to screen.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class SaveDDLDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private Collection datasets;

	private JList datasetsList;

	private MartTab martTab;

	private JTextField targetSchemaName;

	private JComboBox outputFormat;

	private JFileChooser outputFileChooser;

	private JTextField outputFileLocation;

	private JButton outputFileLocationButton;

	private JTextField runDDLHost;

	private JTextField runDDLPort;

	/**
	 * Creates (but does not display) a dialog centred on the given tab, which
	 * allows DDL generation for the given datasets. When the OK button is
	 * chosen, the DDL is generated in the background.
	 * 
	 * @param martTab
	 *            the tab in which this will be displayed.
	 * @param datasets
	 *            the datasets to list.
	 */
	public SaveDDLDialog(final MartTab martTab, final Collection datasets) {
		// Create the base dialog.
		super();
		this.setTitle(Resources.get("saveDDLDialogTitle"));
		this.setModal(true);

		// Remember the tabset that the schema we are working with is part of
		// (or will be part of if it's not been created yet).
		this.martTab = martTab;
		this.datasets = datasets;

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		final GridBagLayout gridBag = new GridBagLayout();
		final JPanel content = new JPanel(gridBag);
		this.setContentPane(content);

		// Create some constraints for labels, except those on the last row
		// of the dialog.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create some constraints for fields, except those on the last row
		// of the dialog.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create some constraints for labels on the last row of the dialog.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create some constraints for fields on the last row of the dialog.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create input fields for target schema name and granularity,
		// and for run ddl host/port.
		this.targetSchemaName = new JTextField(20);
		this.targetSchemaName.setText(martTab.getMart().getOutputSchema());

		this.outputFormat = new JComboBox();
		this.outputFormat.addItem(Resources.get("filePerTableDDL"));
		this.outputFormat.addItem(Resources.get("viewDDL"));
		this.outputFormat.addItem(Resources.get("runDDL"));

		// Create the list for choosing datasets.
		this.datasetsList = new JList(datasets.toArray(new DataSet[0]));
		this.datasetsList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.datasetsList.setSelectionInterval(0, this.datasets.size() - 1);
		this.datasetsList.setVisibleRowCount(4); // Arbitrary.
		// Set the list to 30-characters wide. Longer than this and it will
		// show a horizontal scrollbar.
		this.datasetsList
				.setPrototypeCellValue("012345678901234567890123456789");

		// Create a file chooser for finding the DDL/ZIP file we will save.
		this.outputFileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					final String filename = file.getName();
					final String extension = Resources.get("zipExtension");
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		final String currentDir = Settings.getProperty("currentSaveDir");
		this.outputFileChooser.setCurrentDirectory(currentDir == null ? null
				: new File(currentDir));
		this.outputFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".zip" or ".ddl".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								Resources.get("zipExtension"));
			}

			public String getDescription() {
				return Resources.get("ZipDDLFileFilterDescription");
			}
		});
		final JLabel outputFileLabel = new JLabel(Resources
				.get("saveDDLFileLocationLabel"));
		this.outputFileLocation = new JTextField(20);
		this.outputFileLocationButton = new JButton(Resources
				.get("browseButton"));

		// Attach the file chooser to the output file location button.
		this.outputFileLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.outputFileChooser
						.showSaveDialog(content) == JFileChooser.APPROVE_OPTION) {
					Settings.setProperty("currentSaveDir",
							SaveDDLDialog.this.outputFileChooser
									.getCurrentDirectory().getPath());
					final File file = SaveDDLDialog.this.outputFileChooser
							.getSelectedFile();
					// When a file is chosen, put its name in the driver
					// class location field.
					if (file != null)
						SaveDDLDialog.this.outputFileLocation.setText(file
								.toString());
				}
			}
		});

		// Create the host/port label/fields.
		final JLabel outputHostLabel = new JLabel(Resources
				.get("runDDLHostLabel"));
		this.runDDLHost = new JTextField(20);
		this.runDDLHost.setText(martTab.getMart().getOutputHost());
		final JLabel outputPortLabel = new JLabel(Resources
				.get("runDDLPortLabel"));
		this.runDDLPort = new JFormattedTextField(new DecimalFormat("0"));
		this.runDDLPort.setColumns(5);
		this.runDDLPort.setText(martTab.getMart().getOutputPort());

		// Add listeners to view DDL options which show/hide additional stuff.
		this.outputFormat.addItemListener(new ItemListener() {
			public void itemStateChanged(final ItemEvent e) {
				if (SaveDDLDialog.this.outputFormat.getSelectedItem().equals(
						Resources.get("filePerTableDDL"))) {
					outputFileLabel.setVisible(true);
					SaveDDLDialog.this.outputFileLocation.setVisible(true);
					SaveDDLDialog.this.outputFileLocationButton
							.setVisible(true);
				} else {
					outputFileLabel.setVisible(false);
					SaveDDLDialog.this.outputFileLocation.setVisible(false);
					SaveDDLDialog.this.outputFileLocationButton
							.setVisible(false);
				}
				if (SaveDDLDialog.this.outputFormat.getSelectedItem().equals(
						Resources.get("runDDL"))) {
					outputHostLabel.setVisible(true);
					outputPortLabel.setVisible(true);
					SaveDDLDialog.this.runDDLHost.setVisible(true);
					SaveDDLDialog.this.runDDLPort.setVisible(true);
				} else {
					outputHostLabel.setVisible(false);
					outputPortLabel.setVisible(false);
					SaveDDLDialog.this.runDDLHost.setVisible(false);
					SaveDDLDialog.this.runDDLPort.setVisible(false);
				}
				SaveDDLDialog.this.pack();
			}
		});

		// Lay out the window.

		// Add the dataset lists.
		JLabel label = new JLabel(Resources.get("selectedDataSetsLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		JPanel field = new JPanel();
		field.add(new JScrollPane(this.datasetsList));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the target schema settings label and field.
		label = new JLabel(Resources.get("targetSchemaLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.targetSchemaName);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the format field.
		label = new JLabel(Resources.get("outputFormatLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.outputFormat);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the output location label, field and file chooser button.
		gridBag.setConstraints(outputFileLabel, labelConstraints);
		content.add(outputFileLabel);
		field = new JPanel();
		field.add(this.outputFileLocation);
		field.add(this.outputFileLocationButton);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Add the output host/port etc..
		gridBag.setConstraints(outputHostLabel, labelConstraints);
		content.add(outputHostLabel);
		field = new JPanel();
		field.add(this.runDDLHost);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);
		gridBag.setConstraints(outputPortLabel, labelConstraints);
		content.add(outputPortLabel);
		field = new JPanel();
		field.add(this.runDDLPort);
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// The close and execute buttons.
		final JButton cancel = new JButton(Resources.get("cancelButton"));
		final JButton execute = new JButton(Resources.get("saveDDLButton"));

		// Intercept the close button, which closes the dialog
		// without taking any action.
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SaveDDLDialog.this.hide();
			}
		});

		// Intercept the execute button, which validates the fields
		// then creates the DDL and closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.validateFields()) {
					SaveDDLDialog.this.createDDL();
					SaveDDLDialog.this.hide();
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

		// Set a default value of View SQL.
		SaveDDLDialog.this.outputFormat.setSelectedItem(Resources
				.get("viewDDL"));

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	/**
	 * This method takes the settings from the dialog, having already been
	 * validated, and uses them to set up and start the DDL generation process.
	 */
	private void createDDL() {
		// What datasets are we making DDL for?
		final Collection selectedDataSets = Arrays.asList(this.datasetsList
				.getSelectedValues());
		// Make a stringbuffer in case we want screen output.
		final StringBuffer sb = new StringBuffer();
		// Make the constructor object which will create the DDL.
		MartConstructor constructor;
		if (this.outputFormat.getSelectedItem().equals(
				Resources.get("filePerTableDDL")))
			constructor = new SaveDDLMartConstructor(new File(
					this.outputFileLocation.getText()));
		else if (this.outputFormat.getSelectedItem().equals(
				Resources.get("runDDL")))
			constructor = new SaveDDLMartConstructor(this.runDDLHost.getText(),
					this.runDDLPort.getText());
		else
			constructor = new SaveDDLMartConstructor(sb);

		try {
			// Obtain the DDL generator from the constructor object.
			this.martTab.getMartTabSet().requestSetOutputHost(
					this.runDDLHost.getText());
			this.martTab.getMartTabSet().requestSetOutputPort(
					this.runDDLPort.getText());
			final String outputSchema = this.targetSchemaName.getText();
			this.martTab.getMartTabSet().requestSetOutputSchema(outputSchema);
			final ConstructorRunnable cr = constructor.getConstructorRunnable(
					outputSchema, selectedDataSets);
			// If we want screen output, add a listener that listens for
			// completion of construction. When completed, use the
			// stringbuffer, which will contain the DDL, to pop up a simple
			// text dialog for the user to view it with. Also if we want
			// remote host output, a remote host monitor dialog pops up instead.
			cr.addMartConstructorListener(new MartConstructorListener() {
				public void martConstructorEventOccurred(final int event,
						final Object data, final MartConstructorAction action)
						throws Exception {
					if (event == MartConstructorListener.CONSTRUCTION_ENDED
							&& cr.getFailureException() == null) {
						if (SaveDDLDialog.this.outputFormat.getSelectedItem()
								.equals(Resources.get("viewDDL")))
							ViewTextDialog
									.displayText(Resources
											.get("mcViewDDLWindowTitle"), sb
											.toString());
						else if (SaveDDLDialog.this.outputFormat
								.getSelectedItem().equals(
										Resources.get("runDDL")))
							SaveDDLDialog.this.martTab.getMartTabSet()
									.requestMonitorRemoteHost(
											SaveDDLDialog.this.runDDLHost
													.getText(),
											SaveDDLDialog.this.runDDLPort
													.getText(),
											true);
					}
				}
			});
			this.martTab.getMartTabSet().requestMonitorConstructorRunnable(cr);
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
			JOptionPane.showMessageDialog(null, Resources
					.get("martConstructionFailed"), Resources
					.get("messageTitle"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		final List messages = new ArrayList();

		// Must have a target schema.
		if (this.isEmpty(this.targetSchemaName.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("targetSchema")));

		// Must have an output file.
		if (this.outputFormat.getSelectedItem().equals(
				Resources.get("filePerTableDDL"))
				&& this.isEmpty(this.outputFileLocation.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("saveDDLFileLocation")));

		// Must have an output host/port.
		if (this.outputFormat.getSelectedItem().equals(Resources.get("runDDL"))) {
			if (this.isEmpty(this.runDDLHost.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("runDDLHost")));
			if (this.isEmpty(this.runDDLPort.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("runDDLPort")));
		}

		// Must have at least one dataset selected.
		if (this.datasetsList.getSelectedValues().length == 0)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("selectedDataSets")));

		// Any messages to display? Show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}
}
