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

import java.awt.Color;
import java.util.Iterator;

import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * Displays the contents of a dataset, explaining the relations followed to
 * reach each table used in the dataset tables. It is pretty much the same as a
 * {@link WindowDiagram}, except that it doesn't show any tables or relations
 * not involved in the construction of the dataset or dataset table concerned.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 27th June 2006
 * @since 0.1
 */
public class ExplainDataSetDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	/**
	 * Static reference to the background colour to use for components.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	private DataSetTable datasetTable;

	/**
	 * Creates a diagram explaining the underlying relations beneath a given
	 * dataset table.
	 * 
	 * @param martTab
	 *            the mart tab to pass menu events onto.
	 * @param datasetTable
	 *            the dataset table to explain.
	 */
	public ExplainDataSetDiagram(MartTab martTab, DataSetTable datasetTable) {
		super(martTab);

		// Set the background.
		this.setBackground(ExplainDataSetDiagram.BACKGROUND_COLOUR);

		// Remember the table, and calculate the diagram.
		this.datasetTable = datasetTable;
		this.recalculateDiagram();
	}

	public void doRecalculateDiagram() {
		// Removes all existing components.
		this.removeAll();

		// Add a TableComponent for the main underlying table, if it exists.
		this.addDiagramComponent(new TableComponent(this.datasetTable
				.getUnderlyingTable(), this));

		// Add a TableComponent for each other table involved in any relation
		// underlying the dataset table.
		for (Iterator i = this.datasetTable.getUnderlyingRelations().iterator(); i
				.hasNext();) {
			Relation relation = (Relation) i.next();

			// Add the two ends of the relation, only if not done so before.
			Table firstTable = relation.getFirstKey().getTable();
			if (this.getDiagramComponent(firstTable) == null) {
				TableComponent tableComponent = new TableComponent(firstTable,
						this);
				this.addDiagramComponent(tableComponent);
			}
			Table secondTable = relation.getSecondKey().getTable();
			if (this.getDiagramComponent(secondTable) == null) {
				TableComponent tableComponent = new TableComponent(secondTable,
						this);
				this.addDiagramComponent(tableComponent);
			}
		}

		// Add Relations last to prevent overlapping with other components.
		for (Iterator i = this.datasetTable.getUnderlyingRelations().iterator(); i
				.hasNext();) {
			Relation relation = (Relation) i.next();
			RelationComponent relationComponent = new RelationComponent(
					relation, this);
			this.addDiagramComponent(relationComponent);
		}

		// Resize the diagram to fit.
		this.resizeDiagram();
	}
}
