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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;

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
 * a given set of datasets, then lets them actually do it. The options include
 * granularity of statements generated, and whether to output to file or to
 * screen.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
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

	private JFileChooser outputFileChooser;

	private JTextField outputFileLocation;

	private JButton outputFileLocationButton;

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

		// Create a file chooser for finding the DDL/ZIP file we will save.
		this.outputFileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					final String filename = file.getName();
					final SaveDDLGranularity gran = (SaveDDLGranularity) SaveDDLDialog.this.granularity
							.getSelectedItem();
					final String extension = gran != null && gran.getZipped() ? Resources
							.get("zipExtension")
							: Resources.get("ddlExtension");
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		this.outputFileChooser.setFileFilter(new FileFilter() {
			private boolean isZipped() {
				final SaveDDLGranularity gran = (SaveDDLGranularity) SaveDDLDialog.this.granularity
						.getSelectedItem();
				return gran != null && gran.getZipped();
			}

			// Accepts only files ending in ".zip" or ".ddl".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								this.isZipped() ? Resources.get("zipExtension")
										: Resources.get("ddlExtension"));
			}

			public String getDescription() {
				return Resources
						.get(this.isZipped() ? "ZipDDLFileFilterDescription"
								: "DDLFileFilterDescription");
			}
		});
		this.outputFileLocation = new JTextField(20);
		this.outputFileLocationButton = new JButton(Resources
				.get("browseButton"));

		// Attach the file chooser to the output file location button.
		this.outputFileLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.outputFileChooser
						.showSaveDialog(content) == JFileChooser.APPROVE_OPTION) {
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

		// Add listeners to view DDL and granularity options.
		this.viewDDL.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.viewDDL.isSelected()) {
					SaveDDLDialog.this.outputFileLocation.setText(null);
					SaveDDLDialog.this.outputFileLocation.setEnabled(false);
					SaveDDLDialog.this.outputFileLocationButton
							.setEnabled(false);
				} else {
					SaveDDLDialog.this.outputFileLocation.setEnabled(true);
					SaveDDLDialog.this.outputFileLocationButton
							.setEnabled(true);
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

		// Set a default granularity.
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

		// Add the output location label, field and file chooser button.
		label = new JLabel(Resources.get("saveDDLFileLocationLabel"));
		gridBag.setConstraints(label, labelConstraints);
		content.add(label);
		field = new JPanel();
		field.add(this.outputFileLocation);
		field.add(this.outputFileLocationButton);
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
		final List selectedDataSets = Arrays.asList(this.datasetsList
				.getSelectedValues());
		// Make a stringbuffer in case we want screen output.
		final StringBuffer sb = new StringBuffer();
		// Make the constructor object which will create the DDL.
		MartConstructor constructor;
		if (this.viewDDL.isSelected())
			constructor = new SaveDDLMartConstructor(
					(SaveDDLGranularity) this.granularity.getSelectedItem(),
					sb, this.includeComments.isSelected());
		else
			constructor = new SaveDDLMartConstructor(
					(SaveDDLGranularity) this.granularity.getSelectedItem(),
					new File(this.outputFileLocation.getText()),
					this.includeComments.isSelected());
		try {
			// Obtain the DDL generator from the constructor object.
			final ConstructorRunnable cr = constructor.getConstructorRunnable(
					this.targetSchemaName.getText(), selectedDataSets);
			// If we want screen output, add a listener that listens for
			// completion of construction. When completed, use the
			// stringbuffer, which will contain the DDL, to pop up a simple
			// text dialog for the user to view it with.
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
			JOptionPane.showMessageDialog(null, Resources
					.get("martConstructionFailed"), Resources
					.get("messageTitle"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private void displayTextPane(final StringBuffer textBuffer) {
		// Build the text editor pane.
		final JTextArea editorPane = new JTextArea(textBuffer.toString());

		// Make it read-only and word-wrapped.
		editorPane.setEditable(false);
		editorPane.setWrapStyleWord(true);
		editorPane.setLineWrap(true);

		// Create a simple copy/select-all/wrap menu.
		final JPopupMenu menu = new JPopupMenu();

		// Copy.
		final JMenuItem copy = new JMenuItem(editorPane.getActionMap().get(
				DefaultEditorKit.copyAction));
		copy.setText(Resources.get("copy"));
		copy.setMnemonic(Resources.get("copyMnemonic").charAt(0));
		menu.add(copy);

		menu.addSeparator();

		// Select-all.
		final JMenuItem selectAll = new JMenuItem(editorPane.getActionMap()
				.get(DefaultEditorKit.selectAllAction));
		selectAll.setText(Resources.get("selectAll"));
		selectAll.setMnemonic(Resources.get("selectAllMnemonic").charAt(0));
		menu.add(selectAll);

		// Wrap.
		final JCheckBoxMenuItem wrap = new JCheckBoxMenuItem(Resources
				.get("wordWrap"));
		wrap.setMnemonic(Resources.get("wordWrapMnemonic").charAt(0));
		wrap.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				editorPane.setLineWrap(wrap.isSelected());
			}
		});
		wrap.setSelected(true);
		menu.add(wrap);

		// Attach a mouse listener to the editor pane that
		// will open the menu on demand.
		editorPane.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
				this.handleMouse(e);
			}

			public void mouseClicked(MouseEvent e) {
				this.handleMouse(e);
			}

			public void mousePressed(MouseEvent e) {
				this.handleMouse(e);
			}

			public void mouseEntered(MouseEvent e) {
				this.handleMouse(e);
			}

			public void mouseExited(MouseEvent e) {
				this.handleMouse(e);
			}

			private void handleMouse(MouseEvent e) {
				if (e.isPopupTrigger()) {
					copy.setEnabled(editorPane.getSelectedText() != null);
					menu.show(e.getComponent(), e.getX(), e.getY());
					e.consume();
				}
			}
		});

		// Put the editor pane in a scroll pane.
		final JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		// Arbitrarily resize the scrollpane.
		editorScrollPane.setPreferredSize(new Dimension(600, 400));

		// Create a frame around the scrollpane.
		final JFrame editorFrame = new JFrame(Resources
				.get("mcViewDDLWindowTitle"));
		editorFrame.setContentPane(editorScrollPane);

		// Show the frame.
		editorFrame.pack();
		editorFrame.setLocationRelativeTo(null);
		editorFrame.show();
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
				&& this.isEmpty(this.outputFileLocation.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("saveDDLFileLocation")));

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
