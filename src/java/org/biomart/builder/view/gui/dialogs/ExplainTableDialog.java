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

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.ExplainTableDiagram;
import org.biomart.builder.view.gui.diagrams.ExplainTransformationDiagram;
import org.biomart.builder.view.gui.diagrams.contexts.WindowContext;

/**
 * This simple dialog explains a table by drawing a big diagram of the
 * underlying tables and relations involved in it. If a particular column is
 * selected, then the diagram focuses on that column. Otherwise, the diagram
 * behaves exactly as the window-context diagram does, but without the tables
 * and relations not involved directly in this table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.15, 11th September 2006
 * @since 0.1
 */
public class ExplainTableDialog extends JDialog implements ExplainDialog {
	private static final long serialVersionUID = 1;

	/**
	 * Opens an explanation showing the underlying relations and tables behind a
	 * specific dataset table.
	 * 
	 * @param martTab
	 *            the mart tab which will handle menu events.
	 * @param table
	 *            the table to explain.
	 */
	public static void showTableExplanation(final MartTab martTab,
			final DataSetTable table) {
		final ExplainTableDialog dialog = new ExplainTableDialog(martTab, table);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		martTab.getDataSetTabSet().addCurrentExplanationDialog(dialog);
		dialog.show();
		martTab.getDataSetTabSet().removeCurrentExplanationDialog(dialog);
	}

	private ExplainTableDiagram diagram;

	private DataSetTable dsTable;

	private GridBagConstraints fieldConstraints;

	private GridBagConstraints fieldLastRowConstraints;

	private GridBagLayout gridBag;

	private GridBagConstraints labelConstraints;

	private GridBagConstraints labelLastRowConstraints;

	private MartTab martTab;

	private JPanel transformation;

	private ExplainTableDialog(final MartTab martTab, final DataSetTable dsTable) {
		// Create the blank dialog, and give it an appropriate title.
		super(martTab.getMartTabSet().getMartBuilder(), Resources.get(
				"explainTableDialogTitle", dsTable.getName()), true);
		this.dsTable = dsTable;
		this.martTab = martTab;

		// Make the content pane.
		final JPanel displayArea = new JPanel(new CardLayout());

		// Compute the diagram, and assign it the appropriate context.
		this.diagram = new ExplainTableDiagram(this.martTab, this.dsTable);
		final WindowContext context = new WindowContext(this.martTab,
				(DataSet) this.dsTable.getSchema());
		this.diagram.setDiagramContext(context);
		displayArea.add(new JScrollPane(this.diagram), "WINDOW_CARD");

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
		displayArea.add(new JScrollPane(this.transformation),
				"TRANSFORMATION_CARD");

		// Create panel which contains the buttons.
		final JPanel buttonsPanel = new JPanel();

		// Create the button that selects the window card.
		final JRadioButton windowButton = new JRadioButton(Resources
				.get("windowButtonName"));
		windowButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (e.getSource() == windowButton) {
					final CardLayout cards = (CardLayout) displayArea
							.getLayout();
					cards.show(displayArea, "WINDOW_CARD");
				}
			}
		});

		// Create the button that selects the transformation card.
		final JRadioButton transformationButton = new JRadioButton(Resources
				.get("transformationButtonName"));
		transformationButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (e.getSource() == transformationButton) {
					final CardLayout cards = (CardLayout) displayArea
							.getLayout();
					cards.show(displayArea, "TRANSFORMATION_CARD");
				}
			}
		});

		// Add the card buttons to the panel.
		buttonsPanel.add(windowButton);
		buttonsPanel.add(transformationButton);

		// Make buttons mutually exclusive.
		final ButtonGroup buttons = new ButtonGroup();
		buttons.add(windowButton);
		buttons.add(transformationButton);

		// Set up our content pane.
		final JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);

		// Add the display area to the pane.
		content.add(buttonsPanel, BorderLayout.NORTH);
		content.add(displayArea, BorderLayout.CENTER);

		// Work out what size we want the diagram to be.
		final Dimension size = this.diagram.getPreferredSize();
		final Dimension maxSize = this.martTab.getSize();
		// The +20s in the following are to cater for scrollbar widths
		// and window borders.
		size.width = Math.max(100, Math
				.min(size.width + 20, maxSize.width - 20));
		size.height = Math.max(100, Math.min(size.height + 20,
				maxSize.height - 20));
		content.setPreferredSize(size);

		// Calculate the transform.
		this.recalculateTransformation();

		// Pack the window.
		this.pack();

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
		this.gridBag.setConstraints(label, this.labelConstraints);
		this.transformation.add(label);
		JPanel field = new JPanel();
		Diagram diagram = new ExplainTransformationDiagram(this.martTab,
				this.dsTable);
		field.add(new JScrollPane(diagram));
		this.gridBag.setConstraints(field, this.fieldConstraints);
		this.transformation.add(field);

		// Count our steps.
		int stepNumber = 1;

		// If main table, show underlying table first.
		if (this.dsTable.getType().equals(DataSetTableType.MAIN)) {
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber, Resources.get("explainSelectLabel") }));
			this.gridBag.setConstraints(label, this.labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab,
					this.dsTable.getUnderlyingTable());
			field.add(new JScrollPane(diagram));
			final List includeCols = new ArrayList();
			for (final Iterator i = this.dsTable.getColumns().iterator(); i
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) i.next();
				if (col instanceof SchemaNameColumn)
					includeCols.add(col);
				else if (col instanceof WrappedColumn) {
					final WrappedColumn wcol = (WrappedColumn) col;
					if (wcol.getWrappedColumn().getTable().equals(
							this.dsTable.getUnderlyingTable()))
						includeCols.add(wcol);
				}
			}
			diagram = new ExplainTransformationDiagram(this.martTab,
					includeCols);
			field.add(new JScrollPane(diagram));
			this.gridBag.setConstraints(field, this.fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}
		// Otherwise, show inherited columns.
		else {
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber, Resources.get("explainInheritLabel") }));
			this.gridBag.setConstraints(label, this.labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			final List includeCols = new ArrayList();
			for (final Iterator j = this.dsTable.getColumns().iterator(); j
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) j.next();
				if (col instanceof InheritedColumn)
					includeCols.add(col);
			}
			diagram = new ExplainTransformationDiagram(this.martTab,
					includeCols);
			field.add(new JScrollPane(diagram));
			this.gridBag.setConstraints(field, this.fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}

		// Show underlying key/relation pairs.
		for (int i = 0; i < this.dsTable.getUnderlyingKeys().size(); i++) {
			final Key k = (Key) this.dsTable.getUnderlyingKeys().get(i);
			final Relation r = (Relation) this.dsTable.getUnderlyingRelations()
					.get(i);
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber, Resources.get("explainMergeLabel") }));
			this.gridBag.setConstraints(label, this.labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab, k, r);
			field.add(new JScrollPane(diagram));
			final List includeCols = new ArrayList();
			for (final Iterator j = this.dsTable.getColumns().iterator(); j
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) j.next();
				if (col instanceof WrappedColumn) {
					final WrappedColumn wcol = (WrappedColumn) col;
					if (r.equals(wcol.getUnderlyingRelation()))
						includeCols.add(wcol);
				}
			}
			diagram = new ExplainTransformationDiagram(this.martTab,
					includeCols);
			field.add(new JScrollPane(diagram));
			this.gridBag.setConstraints(field, this.fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}

		// Work out expression and partitioned columns.
		final List expressionCols = new ArrayList();
		final List partCols = new ArrayList();
		for (final Iterator i = this.dsTable.getColumns().iterator(); i
				.hasNext();) {
			final DataSetColumn c = (DataSetColumn) i.next();
			if (c instanceof ExpressionColumn)
				expressionCols.add(c);
			if (c.getPartitionType() != null)
				partCols.add(c);
		}

		// Show expression columns.
		if (!expressionCols.isEmpty()) {
			label = new JLabel(Resources
					.get("stepTableLabel", new String[] { "" + stepNumber,
							Resources.get("explainExpressionsLabel") }));
			this.gridBag.setConstraints(label,
					partCols.isEmpty() ? this.labelLastRowConstraints
							: this.labelConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab,
					expressionCols);
			field.add(new JScrollPane(diagram));
			this.gridBag.setConstraints(field,
					partCols.isEmpty() ? this.fieldLastRowConstraints
							: this.fieldConstraints);
			this.transformation.add(field);
			stepNumber++;
		}

		// Show partitioned columns.
		if (!partCols.isEmpty()) {
			label = new JLabel(Resources.get("stepTableLabel", new String[] {
					"" + stepNumber, Resources.get("explainPartitionsLabel") }));
			this.gridBag.setConstraints(label, this.labelLastRowConstraints);
			this.transformation.add(label);
			field = new JPanel();
			diagram = new ExplainTransformationDiagram(this.martTab, partCols);
			field.add(new JScrollPane(diagram));
			this.gridBag.setConstraints(field, this.fieldLastRowConstraints);
			this.transformation.add(field);
			stepNumber++;
		}
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
	 * Repaint the currently visible diagram.
	 */
	public void repaintDialog() {
		if (this.diagram != null)
			this.diagram.repaintDiagram();
		if (this.transformation != null)
			this.recalculateTransformation();
		this.validate();
	}
}
