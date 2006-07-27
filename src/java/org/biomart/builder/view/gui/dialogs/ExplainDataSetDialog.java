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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.ExplainDataSetDiagram;
import org.biomart.builder.view.gui.diagrams.ExplainTransformationDiagram;
import org.biomart.builder.view.gui.diagrams.contexts.WindowContext;

/**
 * This simple dialog explains a dataset by drawing a big diagram of the
 * underlying tables and relations involved in it. If a particular column is
 * selected, then the diagram focuses on that column. Otherwise, the diagram
 * behaves exactly as the window-context diagram does, but without the tables
 * and relations not involved directly in this dataset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 27th July 2006
 * @since 0.1
 */
public class ExplainDataSetDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private MartTab martTab;

	private JPanel transformation;

	private ExplainDataSetDiagram diagram;

	private GridBagLayout gridBag;

	private GridBagConstraints labelConstraints;

	private GridBagConstraints fieldConstraints;

	private GridBagConstraints labelLastRowConstraints;

	private GridBagConstraints fieldLastRowConstraints;

	private DataSet dataset;

	private DataSetTable dsTable;

	private ExplainDataSetDialog(MartTab martTab, DataSetTable dsTable) {
		// Create the blank dialog, and give it an appropriate title.
		super(martTab.getMartTabSet().getMartBuilder(), Resources.get(
				"explainTableDialogTitle", dsTable.getName()), true);
		this.dsTable = dsTable;
		this.martTab = martTab;
		this.dataset = (DataSet) this.dsTable.getSchema();

		// Make the content pane.
		final JPanel displayArea = new JPanel(new CardLayout());

		// Compute the diagram, and assign it the appropriate context.
		this.diagram = new ExplainDataSetDiagram(this.martTab, this.dsTable);
		WindowContext context = new WindowContext(this.martTab,
				(DataSet) this.dsTable.getSchema());
		diagram.setDiagramContext(context);
		displayArea.add(new JScrollPane(diagram), "WINDOW_CARD");

		// Create the content pane to store the create dialog panel.
		this.gridBag = new GridBagLayout();

		// Create constraints for labels that are not in the last row.
		this.labelConstraints = new GridBagConstraints();
		this.labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		this.labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		this.labelConstraints.anchor = GridBagConstraints.LINE_END;
		this.labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		this.fieldConstraints = new GridBagConstraints();
		this.fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		this.fieldConstraints.fill = GridBagConstraints.NONE;
		this.fieldConstraints.anchor = GridBagConstraints.LINE_START;
		this.fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		this.labelLastRowConstraints = (GridBagConstraints) this.labelConstraints
				.clone();
		this.labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		this.fieldLastRowConstraints = (GridBagConstraints) this.fieldConstraints
				.clone();
		this.fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Compute the transformation diagram.
		this.transformation = new JPanel(this.gridBag);
		displayArea.add(new JScrollPane(transformation), "TRANSFORMATION_CARD");

		// Create panel which contains the buttons.
		JPanel buttonsPanel = new JPanel();

		// Create the button that selects the dataset card.
		final JRadioButton windowButton = new JRadioButton(Resources
				.get("windowButtonName"));
		windowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == windowButton) {
					CardLayout cards = (CardLayout) displayArea.getLayout();
					cards.show(displayArea, "WINDOW_CARD");
				}
			}
		});

		// Create the button that selects the window card.
		final JRadioButton transformationButton = new JRadioButton(Resources
				.get("transformationButtonName"));
		transformationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == transformationButton) {
					CardLayout cards = (CardLayout) displayArea.getLayout();
					cards.show(displayArea, "TRANSFORMATION_CARD");
				}
			}
		});

		// Add the card buttons to the panel.
		buttonsPanel.add(windowButton);
		buttonsPanel.add(transformationButton);

		// Make buttons mutually exclusive.
		ButtonGroup buttons = new ButtonGroup();
		buttons.add(windowButton);
		buttons.add(transformationButton);

		// Set up our content pane.
		JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);

		// Add the display area to the pane.
		content.add(buttonsPanel, BorderLayout.NORTH);
		content.add(displayArea, BorderLayout.CENTER);

		// Work out what size we want the diagram to be.
		Dimension size = this.diagram.getPreferredSize();
		Dimension maxSize = this.martTab.getSize();
		// The +20s in the following are to cater for scrollbar widths
		// and window borders.
		size.width = Math.max(100, Math
				.min(size.width + 20, maxSize.width - 20));
		size.height = Math.max(100, Math.min(size.height + 20,
				maxSize.height - 20));
		content.setPreferredSize(size);
		
		// Calculate the transform.
		this.recalculateTransformation();

		// Select the default button (which shows the diagram card).
		// We must physically click on it to make the card show.
		windowButton.doClick();
	}

	private void recalculateTransformation() {
		// Clear the transformation box.
		this.transformation.removeAll();
		// Redisplay it.

		// Add the table definition at the top.
		JLabel label = new JLabel(Resources.get("targetTableLabel"));
		gridBag.setConstraints(label, labelConstraints);
		this.transformation.add(label);
		JPanel field = new JPanel();
		Diagram diagram = new ExplainTransformationDiagram(this.martTab,
				this.dataset, this.dsTable);
		field.add(new JScrollPane(diagram));
		gridBag.setConstraints(field, fieldConstraints);
		this.transformation.add(field);

		// Count our steps.
		int stepNumber = 1;

		// If main table, show underlying table first.
		if (this.dsTable.getType().equals(DataSetTableType.MAIN)) {
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber,
					stepNumber == 1 ? Resources.get("explainSelectLabel")
							: Resources.get("explainMergeLabel") }));
			gridBag.setConstraints(label, labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab,
					this.dataset, this.dsTable.getUnderlyingTable());
			field.add(new JScrollPane(diagram));
			gridBag.setConstraints(field, fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}

		// Show underlying key/relation pairs.
		for (int i = 0; i < this.dsTable.getUnderlyingKeys().size(); i++) {
			Key k = (Key) this.dsTable.getUnderlyingKeys().get(i);
			Relation r = (Relation) this.dsTable.getUnderlyingRelations()
					.get(i);
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber,
					stepNumber == 1 ? Resources.get("explainSelectLabel")
							: Resources.get("explainMergeLabel") }));
			gridBag.setConstraints(label, labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab,
					this.dataset, k, r);
			field.add(new JScrollPane(diagram));
			gridBag.setConstraints(field, fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}

		// Work out expression and partitioned columns.
		List expressionCols = new ArrayList();
		for (Iterator i = this.dsTable.getColumns().iterator(); i.hasNext();) {
			Column c = (Column) i.next();
			if (c instanceof ExpressionColumn)
				expressionCols.add(c);
		}
		List partCols = new ArrayList(((DataSet) this.dsTable.getSchema())
				.getPartitionedWrappedColumns());
		if (((DataSet) this.dsTable.getSchema()).getPartitionOnSchema())
			for (Iterator i = this.dsTable.getColumns().iterator(); i.hasNext();) {
				Column c = (Column) i.next();
				if (c instanceof SchemaNameColumn)
					partCols.add(c);
			}

		// Show expression columns.
		if (!expressionCols.isEmpty()) {
			label = new JLabel(Resources
					.get("stepTableLabel", new String[] { "" + stepNumber,
							Resources.get("explainExpressionsLabel") }));
			gridBag.setConstraints(label,
					partCols.isEmpty() ? labelLastRowConstraints
							: labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab,
					this.dataset, expressionCols);
			field.add(new JScrollPane(diagram));
			gridBag.setConstraints(field,
					partCols.isEmpty() ? fieldLastRowConstraints
							: fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}

		// Show partitioned columns.
		if (!partCols.isEmpty()) {
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber, Resources.get("explainPartitionsLabel") }));
			gridBag.setConstraints(label, labelLastRowConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab,
					this.dataset, partCols);
			field.add(new JScrollPane(diagram));
			gridBag.setConstraints(field, fieldLastRowConstraints);
			this.transformation.add(field);
			stepNumber++;
		}
		
		// Repack the window.
		this.pack();
	}

	/**
	 * Repaint the currently visible diagram.
	 */
	public void repaintDialog() {
		if (this.diagram != null)
			this.diagram.repaintDiagram();
		if (this.transformation != null)
			this.recalculateTransformation();
		this.validate();
	}

	/**
	 * Recalculate the currently visible diagram.
	 */
	public void recalculateDialog() {
		if (this.diagram != null)
			this.diagram.recalculateDiagram();
		if (this.transformation != null)
			this.recalculateTransformation();
		this.validate();
	}

	/**
	 * Opens an explanation showing the underlying relations and tables behind a
	 * specific dataset table.
	 * 
	 * @param martTab
	 *            the mart tab which will handle menu events.
	 * @param table
	 *            the table to explain.
	 */
	public static void showTableExplanation(MartTab martTab, DataSetTable table) {
		ExplainDataSetDialog dialog = new ExplainDataSetDialog(martTab, table);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		martTab.getDataSetTabSet().setCurrentExplanationDialog(dialog);
		dialog.show();
		martTab.getDataSetTabSet().setCurrentExplanationDialog(null);
	}
}
