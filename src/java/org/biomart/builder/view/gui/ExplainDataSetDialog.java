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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.resources.BuilderBundle;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This simple dialog explains a dataset by drawing a big diagram of the
 * underlying tables and relations involved in it. If a particular column is
 * selected, then the diagram focuses on that column. Otherwise, the diagram
 * behaves exactly as the window-context diagram does, but without the tables
 * and relations not involved directly in this dataset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 20th June 2006
 * @since 0.1
 */
public class ExplainDataSetDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private ExplainDataSetDialog(MartTab martTab, DataSetTable dsTable,
			DataSetColumn dsColumn) {
		// Create the blank dialog, and give it an appropriate title.
		super(martTab.getMartTabSet().getMartBuilder(),
				(dsColumn == null ? BuilderBundle.getString(
						"explainTableDialogTitle", dsTable.getName())
						: BuilderBundle.getString("explainColumnDialogTitle",
								new String[] { dsTable.getName(),
										dsColumn.getName() })), true);

		// Make the content pane.
		JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);

		// Compute the diagram, and assign it the appropriate context.
		ExplainDataSetDiagram diagram = new ExplainDataSetDiagram(martTab,
				dsTable);
		ExplainDataSetContext context = new ExplainDataSetContext(martTab,
				(DataSet) dsTable.getSchema());
		diagram.setDiagramContext(context);

		// Select the column, if chosen.
		if (dsColumn != null)
			context.setSelectedColumn(dsColumn);

		// Tell the schema tabset about this, so that it gets updated if any
		// changes are made elsewhere.
		martTab.getDataSetTabSet().setCurrentExplanationDiagram(diagram);

		// Work out what size we want the diagram to be.
		Dimension size = diagram.getPreferredSize();
		Dimension maxSize = martTab.getSize();
		size.width = Math.max(100, Math.min(size.width, maxSize.width));
		size.height = Math.max(100, Math.min(size.height, maxSize.height));
		content.setPreferredSize(size);

		// Add the diagram to the pane.
		content.add(new JScrollPane(diagram), BorderLayout.CENTER);

		// Set size of the dialog.
		this.pack();

		// Zoom to the selected column or table.
		if (dsColumn != null)
			diagram.findObject(dsColumn.getUnderlyingRelation());
		else
			diagram.findObject(dsTable.getUnderlyingTable());
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
		ExplainDataSetDialog dialog = new ExplainDataSetDialog(martTab, table,
				null);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		martTab.getDataSetTabSet().setCurrentExplanationDiagram(null);
	}

	/**
	 * Opens an explanation showing the underlying relations and tables behind a
	 * specific dataset column.
	 * 
	 * @param martTab
	 *            the mart tab which will handle menu events.
	 * @param column
	 *            the column to explain.
	 */
	public static void showColumnExplanation(MartTab martTab,
			DataSetColumn column) {
		ExplainDataSetDialog dialog = new ExplainDataSetDialog(martTab,
				(DataSetTable) column.getTable(), column);
		dialog.setLocationRelativeTo(martTab.getMartTabSet().getMartBuilder());
		dialog.show();
		martTab.getDataSetTabSet().setCurrentExplanationDiagram(null);
	}
}
