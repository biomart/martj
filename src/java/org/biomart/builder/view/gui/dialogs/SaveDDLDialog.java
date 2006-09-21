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
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;

import org.biomart.builder.controller.SaveDDLMartConstructor;
import org.biomart.builder.controller.SaveDDLMartConstructor.SaveDDLGranularity;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.MartConstructor.ConstructorRunnable;
import org.biomart.builder.model.MartConstructor.MartConstructorListener;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * A dialog which allows the user to choose some options about creating DDL over
 * a given set of datasets, then lets them actually do it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author$
 * @since 0.1
 */
public class SaveDDLDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private Collection datasets;

	private JList datasetsList;

	private JComboBox granularity;

	private JCheckBox includeComments;

	private MartTab martTab;

	private JTextField targetSchemaName;

	private JCheckBox viewDDL;

	private JFileChooser zipFileChooser;

	private FileFilter zipFileFilter;

	private JTextField zipFileLocation;

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
	public SaveDDLDialog(final MartTab martTab, final Collection datasets) {
		// Create the base dialog.
		super(martTab.getMartTabSet().getMartBuilder(), Resources
				.get("saveDDLDialogTitle"), true);

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

		// Create input fields for target schema name and granularity.
		this.targetSchemaName = new JTextField(20);
		this.includeComments = new JCheckBox(Resources
				.get("includeCommentsLabel"));
		this.includeComments.setSelected(true);
		this.viewDDL = new JCheckBox(Resources.get("viewDDLOnCompletion"));
		this.granularity = new JComboBox(new Object[] {
				SaveDDLGranularity.SINGLE, SaveDDLGranularity.MART,
				SaveDDLGranularity.DATASET, SaveDDLGranularity.TABLE,
				SaveDDLGranularity.STEP });

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

		// Create a file chooser for finding the ZIP file where
		// we will save.
		this.zipFileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					final String filename = file.getName();
					final SaveDDLGranularity gran = (SaveDDLGranularity) SaveDDLDialog.this.granularity
							.getSelectedItem();
					final String extension = gran != null && gran.getZipped() ? ".zip"
							: ".ddl";
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		this.zipFileFilter = new FileFilter() {
			private boolean isZipped() {
				final SaveDDLGranularity gran = (SaveDDLGranularity) SaveDDLDialog.this.granularity
						.getSelectedItem();
				return gran != null && gran.getZipped();
			}

			// Accepts only files ending in ".zip".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								this.isZipped() ? ".zip" : ".ddl");
			}

			public String getDescription() {
				return Resources
						.get(this.isZipped() ? "ZipDDLFileFilterDescription"
								: "DDLFileFilterDescription");
			}
		};
		this.zipFileChooser.setFileFilter(this.zipFileFilter);
		this.zipFileLocation = new JTextField(20);
		this.zipFileLocationButton = new JButton(Resources.get("browseButton"));

		// Attach the file chooser to the driver class location button.
		this.zipFileLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.zipFileChooser.showSaveDialog(content) == JFileChooser.APPROVE_OPTION) {
					final File file = SaveDDLDialog.this.zipFileChooser
							.getSelectedFile();
					// When a file is chosen, put its name in the driver
					// class location field.
					if (file != null)
						SaveDDLDialog.this.zipFileLocation.setText(file
								.toString());
				}
			}
		});

		// Add listeners to view DDL and granularity.
		this.viewDDL.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.viewDDL.isSelected()) {
					SaveDDLDialog.this.zipFileLocation.setText(null);
					SaveDDLDialog.this.zipFileLocation.setEnabled(false);
					SaveDDLDialog.this.zipFileLocationButton.setEnabled(false);
				} else {
					SaveDDLDialog.this.zipFileLocation.setEnabled(true);
					SaveDDLDialog.this.zipFileLocationButton.setEnabled(true);
				}
			}
		});
		this.granularity.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.granularity.getSelectedItem() != null
						&& SaveDDLDialog.this.granularity.getSelectedItem()
								.equals(SaveDDLGranularity.SINGLE))
					SaveDDLDialog.this.viewDDL.setEnabled(true);
				else {
					if (SaveDDLDialog.this.viewDDL.isSelected())
						SaveDDLDialog.this.viewDDL.doClick();
					SaveDDLDialog.this.viewDDL.setEnabled(false);
				}
			}
		});
		this.granularity.setSelectedItem(SaveDDLGranularity.SINGLE);

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

		// Add the granularity label and field, and the comments checkbox.
		label = new JLabel(Resources.get("granularityLabel"));
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
		label = new JLabel(Resources.get("saveDDLFileLocationLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.zipFileLocation);
		field.add(this.zipFileLocationButton);
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
		// then closes the dialog.
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

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
	}

	private void createDDL() {
		final List selectedDataSets = Arrays.asList(this.datasetsList
				.getSelectedValues());
		final StringBuffer sb = new StringBuffer();
		MartConstructor constructor;
		if (this.viewDDL.isSelected())
			constructor = new SaveDDLMartConstructor(
					(SaveDDLGranularity) this.granularity.getSelectedItem(),
					sb, this.includeComments.isSelected());
		else
			constructor = new SaveDDLMartConstructor(
					(SaveDDLGranularity) this.granularity.getSelectedItem(),
					new File(this.zipFileLocation.getText()),
					this.includeComments.isSelected());
		try {
			final ConstructorRunnable cr = constructor.getConstructorRunnable(
					this.targetSchemaName.getText(), selectedDataSets);
			if (this.viewDDL.isSelected())
				cr.addMartConstructorListener(new MartConstructorListener() {
					public void martConstructorEventOccurred(final int event,
							final MartConstructorAction action)
							throws Exception {
						if (event == MartConstructorListener.CONSTRUCTION_ENDED
								&& cr.getFailureException() == null)
							SaveDDLDialog.this.displayTextPane(sb);
					}
				});
			this.martTab.getMartTabSet().requestMonitorConstructorRunnable(cr);
		} catch (final Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
			JOptionPane.showMessageDialog(this.martTab.getMartTabSet()
					.getMartBuilder(), Resources.get("martConstructionFailed"),
					Resources.get("messageTitle"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private void displayTextPane(final StringBuffer textBuffer) {
		// Build the text pane.
		final JEditorPane editorPane = new JEditorPane("text/plain", textBuffer
				.toString());

		// Put the editor pane in a scroll pane.
		final JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		// Arbitrarily resize the scrollpane.
		editorScrollPane.setPreferredSize(new Dimension(600, 400));

		// Show the output.
		JOptionPane.showMessageDialog(this.martTab, editorScrollPane, Resources
				.get("mcViewDDLWindowTitle"), JOptionPane.INFORMATION_MESSAGE);
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
		if (!this.viewDDL.isSelected()
				&& this.isEmpty(this.zipFileLocation.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("saveDDLFileLocation")));

		// Must have at least one dataset selected.
		if (this.datasetsList.getSelectedValues().length == 0)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("selectedDataSets")));

		// Any messages to display? Show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}
}
