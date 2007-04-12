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
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.view.gui.SchemaTabSet;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.contexts.ExplainContext;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * This simple dialog explains a dataset by drawing the schema diagram for the
 * underlying schemas, then applying the {@link ExplainContext} to the diagrams.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class ExplainDataSetDialog extends JDialog implements ExplainDialog {
	private static final long serialVersionUID = 1;

	/**
	 * Opens an explanation showing the underlying relations and tables behind a
	 * specific dataset.
	 * 
	 * @param martTab
	 *            the mart tab which will handle menu events.
	 * @param dataset
	 *            the dataset to explain.
	 */
	public static void showDataSetExplanation(final MartTab martTab,
			final DataSet dataset) {
		final ExplainDataSetDialog dialog = new ExplainDataSetDialog(martTab,
				dataset);
		martTab.getDataSetTabSet().setCurrentExplanationDialog(dialog);
		dialog.show();
		martTab.getDataSetTabSet().clearCurrentExplanationDialog();
	}

	private SchemaTabSet schemaTabSet;

	private DataSet dataset;

	private MartTab martTab;
	
	private ExplainDataSetDialog(final MartTab martTab, final DataSet dataset) {
		// Create the blank dialog, and give it an appropriate title.
		super();
		this.setTitle(Resources.get("explainDataSetDialogTitle", dataset
				.getName()));
		this.setModal(true);
		this.dataset = dataset;
		this.schemaTabSet = martTab.getSchemaTabSet();
		this.martTab = martTab;

		// Make a content pane.
		final JPanel content = new JPanel(new BorderLayout());

		// The content pane is the schema tab set with an explain context.
		content.add(this.schemaTabSet, BorderLayout.CENTER);
		// Must be set visible as previous display location is invisible.
		this.schemaTabSet.setVisible(true);

		// Work out what size we want the diagram to be.
		final Dimension size = this.schemaTabSet.getPreferredSize();
		final Dimension maxSize = martTab.getSize();
		// The +20s in the following are to cater for scrollbar widths
		// and window borders.
		size.width = Math.max(100, Math
				.min(size.width + 20, maxSize.width - 20));
		size.height = Math.max(100, Math.min(size.height + 20,
				maxSize.height - 20));
		content.setPreferredSize(size);
		this.setContentPane(content);

		// Pack the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
		
		// Calculate the schema tabset.
		this.recalculateDialog(null);
	}

	public void recalculateDialog(final Object changedObject) {
		if (this.schemaTabSet != null) {
			// Update explain context.
			final ExplainContext context = new ExplainContext(martTab, this.dataset);
			this.schemaTabSet.setDiagramContext(context);
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
	}

	public void repaintDialog(final Object changedObject) {
		if (this.schemaTabSet != null) {
			// Update explain context.
			final ExplainContext context = new ExplainContext(martTab, this.dataset);
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
	}
}
