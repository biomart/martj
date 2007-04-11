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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.TransformationUnit;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.TransformationUnit.Concat;
import org.biomart.builder.model.TransformationUnit.Expression;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.builder.view.gui.SchemaTabSet;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.ExplainTransformationDiagram;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.builder.view.gui.diagrams.contexts.ExplainContext;
import org.biomart.builder.view.gui.diagrams.contexts.TransformationContext;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * This simple dialog explains a table by drawing a series of diagrams of the
 * underlying tables and relations involved in it.
 * <p>
 * It has two tabs. In the first tab goes an overview diagram. In the second tab
 * goes a series of smaller diagrams, each one an instance of
 * {@link ExplainTransformationDiagram} which represents a single step in the
 * transformation process required to produce the table being explained.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
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
		martTab.getDataSetTabSet().setCurrentExplanationDialog(dialog);
		dialog.show();
		// We don't get here until the dialog is closed.
		martTab.getDataSetTabSet().clearCurrentExplanationDialog();
	}

	private SchemaTabSet schemaTabSet;

	private DataSet ds;

	private String tableName;

	private GridBagConstraints fieldConstraints;

	private GridBagConstraints fieldLastRowConstraints;

	private GridBagLayout gridBag;

	private GridBagConstraints labelConstraints;

	private GridBagConstraints labelLastRowConstraints;

	private MartTab martTab;

	private JPanel transformation;

	private List transformationTableComponents = new ArrayList();

	private TransformationContext transformationContext;

	private ExplainContext explainContext;

	private ExplainTableDialog(final MartTab martTab, final DataSetTable dsTable) {
		// Create the blank dialog, and give it an appropriate title.
		super();
		this.setTitle(Resources.get("explainTableDialogTitle", dsTable
				.getModifiedName()));
		this.setModal(true);
		this.ds = (DataSet) dsTable.getSchema();
		this.tableName = dsTable.getName();
		this.martTab = martTab;
		this.schemaTabSet = martTab.getSchemaTabSet();

		// Make the content pane.
		final JPanel displayArea = new JPanel(new CardLayout());

		// Compute the overview diagram, and assign it the
		// appropriate context.
		this.explainContext = new ExplainContext(martTab, dsTable);
		this.schemaTabSet.setDiagramContext(this.explainContext);
		// Must be set visible as previous display location is invisible.
		this.schemaTabSet.setVisible(true);
		displayArea.add(this.schemaTabSet, "WINDOW_CARD");

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
		final Dimension size = this.schemaTabSet.getPreferredSize();
		final Dimension maxSize = this.martTab.getSize();
		// The +20s in the following are to cater for scrollbar widths
		// and window borders.
		size.width = Math.max(100, Math
				.min(size.width + 20, maxSize.width - 20));
		size.height = Math.max(100, Math.min(size.height + 20,
				maxSize.height - 20));
		content.setPreferredSize(size);

		// Make a context for our sub-diagrams.
		this.transformationContext = new TransformationContext(this.martTab,
				(DataSet) dsTable.getSchema());

		// Calculate the transform.
		this.recalculateTransformation();

		// Pack the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);

		// Select the default button (which shows the transformation card).
		// We must physically click on it to make the card show.
		transformationButton.doClick();
	}

	private void recalculateTransformation() {
		// Keep a note of shown tables.
		final Map shownTables = new HashMap();
		for (final Iterator i = this.transformationTableComponents.iterator(); i
				.hasNext();) {
			final TableComponent comp = (TableComponent) i.next();
			shownTables.put(((Table) comp.getObject()).getName(), comp
					.getState());
		}

		// Clear the transformation box.
		this.transformation.removeAll();
		this.transformationTableComponents.clear();

		// Keep track of columns counted so far.
		List columnsSoFar = new ArrayList();

		// Count our steps.
		int stepNumber = 1;

		// Iterate over transformation units.
		for (final Iterator i = ((DataSetTable) this.ds
				.getTableByName(this.tableName)).getTransformationUnits()
				.iterator(); i.hasNext();) {
			final TransformationUnit tu = (TransformationUnit) i.next();
			// Holders for our stuff.
			final JLabel label;
			final ExplainTransformationDiagram diagram;
			// Draw the unit.
			if (tu instanceof Expression) {
				// Do an expression column list.
				label = new JLabel(Resources.get("stepTableLabel",
						new String[] { "" + stepNumber,
								Resources.get("explainExpressionLabel") }));
				diagram = new ExplainTransformationDiagram.AdditionalColumns(
						this.martTab, tu, stepNumber, this.explainContext);
			} else if (tu instanceof Concat) {
				// Do an expression column list.
				label = new JLabel(Resources.get("stepTableLabel",
						new String[] { "" + stepNumber,
								Resources.get("explainConcatLabel") }));
				diagram = new ExplainTransformationDiagram.TempReal(
						this.martTab, (Concat) tu, columnsSoFar, stepNumber,
						this.explainContext);
			} else if (tu instanceof JoinTable) {
				// Temp table to schema table join.
				label = new JLabel(Resources.get("stepTableLabel",
						new String[] { "" + stepNumber,
								Resources.get("explainMergeLabel") }));
				diagram = new ExplainTransformationDiagram.TempReal(
						this.martTab, (JoinTable) tu, columnsSoFar, stepNumber,
						this.explainContext);
			} else if (tu instanceof SelectFromTable) {
				// Do a single-step select.
				label = new JLabel(Resources.get("stepTableLabel",
						new String[] { "" + stepNumber,
								Resources.get("explainSelectLabel") }));
				diagram = new ExplainTransformationDiagram.SingleTable(
						this.martTab, (SelectFromTable) tu, stepNumber,
						this.explainContext);
			} else
				throw new BioMartError();
			this.transformationTableComponents.addAll(diagram
					.getTableComponents());
			// Display the diagram.
			this.gridBag.setConstraints(label, this.labelConstraints);
			this.transformation.add(label);
			diagram.setDiagramContext(this.transformationContext);
			JPanel field = new JPanel();
			field.add(diagram);
			this.gridBag.setConstraints(field, this.fieldConstraints);
			this.transformation.add(field);
			// Add columns from this unit to the transformed table.
			columnsSoFar.addAll(tu.getNewColumnNameMap().values());
			stepNumber++;
		}

		// Reinstate shown/hidden columns.
		for (final Iterator i = this.transformationTableComponents.iterator(); i
				.hasNext();) {
			final TableComponent comp = (TableComponent) i.next();
			final Object state = shownTables.get(((Table) comp.getObject())
					.getName());
			if (state != null)
				comp.setState(state);
		}

		// Resize the diagram to fit the components.
		this.transformation.validate();
	}

	private void repaintTransformation() {
		this.transformation.repaint(this.transformation.getVisibleRect());
	}

	public void recalculateDialog(final Object changedObject) {
		if (this.schemaTabSet != null) {
			// Update explain context.
			this.explainContext = new ExplainContext(this.martTab,
					(DataSetTable) this.ds.getTableByName(this.tableName));
			this.schemaTabSet.setDiagramContext(this.explainContext);
			if (changedObject != null)
				if (changedObject instanceof Schema)
					this.schemaTabSet
							.recalculateSchemaDiagram((Schema) changedObject);
				else if (changedObject instanceof Table)
					this.schemaTabSet
							.recalculateSchemaDiagram(((Table) changedObject)
									.getSchema());
				else if (changedObject instanceof Key)
					this.schemaTabSet
							.recalculateSchemaDiagram(((Key) changedObject)
									.getTable().getSchema());
				else if (changedObject instanceof Column)
					this.schemaTabSet
							.recalculateSchemaDiagram(((Column) changedObject)
									.getTable().getSchema());
				else if (changedObject instanceof Relation) {
					this.schemaTabSet
							.recalculateSchemaDiagram(((Relation) changedObject)
									.getFirstKey().getTable().getSchema());
					if (!((Relation) changedObject).getFirstKey().getTable()
							.getSchema().equals(
									((Relation) changedObject).getSecondKey()
											.getTable().getSchema()))
						this.schemaTabSet
								.recalculateSchemaDiagram(((Relation) changedObject)
										.getSecondKey().getTable().getSchema());
				}
			this.schemaTabSet.recalculateOverviewDiagram();
		}
		if (this.transformation != null) {
			this.recalculateTransformation();
			this.repaintTransformation();
		}
	}

	public void repaintDialog(final Object changedObject) {
		if (this.schemaTabSet != null) {
			// Update explain context.
			final ExplainContext context = new ExplainContext(this.martTab,
					(DataSetTable) this.ds.getTableByName(this.tableName));
			this.schemaTabSet.setDiagramContext(context);
			if (changedObject != null)
				if (changedObject instanceof Schema)
					this.schemaTabSet
							.repaintSchemaDiagram((Schema) changedObject);
				else if (changedObject instanceof Table)
					this.schemaTabSet
							.repaintSchemaDiagram(((Table) changedObject)
									.getSchema());
				else if (changedObject instanceof Key)
					this.schemaTabSet
							.repaintSchemaDiagram(((Key) changedObject)
									.getTable().getSchema());
				else if (changedObject instanceof Column)
					this.schemaTabSet
							.repaintSchemaDiagram(((Column) changedObject)
									.getTable().getSchema());
				else if (changedObject instanceof Relation) {
					this.schemaTabSet
							.repaintSchemaDiagram(((Relation) changedObject)
									.getFirstKey().getTable().getSchema());
					if (!((Relation) changedObject).getFirstKey().getTable()
							.getSchema().equals(
									((Relation) changedObject).getSecondKey()
											.getTable().getSchema()))
						this.schemaTabSet
								.repaintSchemaDiagram(((Relation) changedObject)
										.getSecondKey().getTable().getSchema());
				}
			this.schemaTabSet.repaintOverviewDiagram();
		}
		if (this.transformation != null)
			this.repaintTransformation();
	}
}
