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

package org.biomart.builder.view.gui.diagrams;

import java.awt.Color;
import java.util.Iterator;

import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * Given a {@link DataSetTable}, this diagram displays all the underlying
 * relations from that table, and all the tables that those underlying relations
 * link, allowing the user to see exactly which tables and relations are
 * involved in the construction of the {@link DataSetTable}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public class ExplainTableDiagram extends Diagram {
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
	public ExplainTableDiagram(final MartTab martTab,
			final DataSetTable datasetTable) {
		super(martTab);

		// Remember the table, and calculate the diagram.
		this.datasetTable = datasetTable;
		this.recalculateDiagram();
	}

	protected void updateAppearance() {
		// Set the background.
		this.setBackground(ExplainTableDiagram.BACKGROUND_COLOUR);
	}

	public void doRecalculateDiagram() {
		// Removes all existing components.
		this.removeAll();

		// Add a TableComponent for the main underlying table.
		this.addDiagramComponent(new TableComponent(this.datasetTable
				.getUnderlyingTable(), this));

		// Add a TableComponent for each other table involved in any relation
		// underlying the dataset table.
		for (final Iterator i = this.datasetTable.getUnderlyingRelations()
				.iterator(); i.hasNext();) {
			final Relation relation = (Relation) i.next();

			// Add the tables at the two ends of the relation, but only if
			// not done so before.
			final Table firstTable = relation.getFirstKey().getTable();
			if (this.getDiagramComponent(firstTable) == null) {
				final TableComponent tableComponent = new TableComponent(
						firstTable, this);
				this.addDiagramComponent(tableComponent);
			}
			final Table secondTable = relation.getSecondKey().getTable();
			if (this.getDiagramComponent(secondTable) == null) {
				final TableComponent tableComponent = new TableComponent(
						secondTable, this);
				this.addDiagramComponent(tableComponent);
			}
		}

		// Add Relations last to prevent overlapping with other components.
		for (final Iterator i = this.datasetTable.getUnderlyingRelations()
				.iterator(); i.hasNext();) {
			final Relation relation = (Relation) i.next();
			final RelationComponent relationComponent = new RelationComponent(
					relation, this);
			this.addDiagramComponent(relationComponent);
		}

		// Resize the diagram to fit.
		this.resizeDiagram();
	}
}
