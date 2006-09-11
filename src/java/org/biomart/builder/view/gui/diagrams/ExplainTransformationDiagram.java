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
import java.util.Collection;
import java.util.Iterator;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.ColumnComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * Displays a transformation step, depending on what is passed to the
 * constructor. The results is always a diagram containing only those components
 * which are involved in the current transformation.
 * <p>
 * Note how sub-diagrams do not have contexts, in order to prevent user
 * interaction with them.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 29th August 2006
 * @since 0.1
 */
public class ExplainTransformationDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	/**
	 * Static reference to the background colour to use for components.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	private Collection columns;

	private DataSetTable datasetTable;

	private Key key;

	private Relation relation;

	private Table table;

	/**
	 * Creates a diagram showing the given set of columns.
	 * 
	 * @param martTab
	 *            the mart tab to pass menu events onto.
	 * @param columns
	 *            the columns to explain.
	 */
	public ExplainTransformationDiagram(final MartTab martTab,
			final Collection columns) {
		super(martTab);

		// Remember the columns, and calculate the diagram.
		this.columns = columns;
		this.recalculateDiagram();
	}

	/**
	 * Creates a diagram showing the given table and possibly it's parent too.
	 * 
	 * @param martTab
	 *            the mart tab to pass menu events onto.
	 * @param datasetTable
	 *            the dataset table to explain.
	 */
	public ExplainTransformationDiagram(final MartTab martTab,
			final DataSetTable datasetTable) {
		super(martTab);

		// Set the background.
		this.setBackground(ExplainTransformationDiagram.BACKGROUND_COLOUR);

		// Remember the table, and calculate the diagram.
		this.datasetTable = datasetTable;
		this.recalculateDiagram();
	}

	/**
	 * Creates a diagram showing the given key/relation pair explanation.
	 * 
	 * @param martTab
	 *            the mart tab to pass menu events onto.
	 * @param key
	 *            the key to explain the relation from.
	 * @param relation
	 *            the relation to explain.
	 */
	public ExplainTransformationDiagram(final MartTab martTab, final Key key,
			final Relation relation) {
		super(martTab);

		// Set the background.
		this.setBackground(ExplainTransformationDiagram.BACKGROUND_COLOUR);

		// Remember the key and relation, and calculate the diagram.
		this.key = key;
		this.relation = relation;
		this.recalculateDiagram();
	}

	/**
	 * Creates a diagram showing the given table.
	 * 
	 * @param martTab
	 *            the mart tab to pass menu events onto.
	 * @param table
	 *            the table to explain.
	 */
	public ExplainTransformationDiagram(final MartTab martTab, final Table table) {
		super(martTab);

		// Set the background.
		this.setBackground(ExplainTransformationDiagram.BACKGROUND_COLOUR);

		// Remember the table, and calculate the diagram.
		this.table = table;
		this.recalculateDiagram();
	}

	protected void updateAppearance() {
		// Set the background.
		this.setBackground(ExplainTransformationDiagram.BACKGROUND_COLOUR);
	}

	public void doRecalculateDiagram() {
		// Removes all existing components.
		this.removeAll();

		// Explain a dataset table?
		if (this.datasetTable != null) {
			// Has a parent?
			Relation parentRelation = null;
			if (!this.datasetTable.getType().equals(DataSetTableType.MAIN)) {
				parentRelation = (Relation) ((Key) this.datasetTable
						.getForeignKeys().iterator().next()).getRelations()
						.iterator().next();
				this.addDiagramComponent(new TableComponent(parentRelation
						.getOneKey().getTable(), this));
			}
			// Add table itself.
			this
					.addDiagramComponent(new TableComponent(this.datasetTable,
							this));
			// Add parent relation, if any.
			if (parentRelation != null) {
				final RelationComponent relationComponent = new RelationComponent(
						parentRelation, this);
				this.addDiagramComponent(relationComponent);
			}
		}

		// Explain a normal table?
		else if (this.table != null)
			this.addDiagramComponent(new TableComponent(this.table, this));
		else if (this.key != null && this.relation != null) {
			final Table source = this.key.getTable();
			final Table target = this.relation.getOtherKey(this.key).getTable();
			// Add source and target tables.
			this.addDiagramComponent(new TableComponent(source, this));
			this.addDiagramComponent(new TableComponent(target, this));
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					this.relation, this);
			this.addDiagramComponent(relationComponent);
		}

		// Explain a set of columns?
		else if (this.columns != null)
			for (final Iterator i = this.columns.iterator(); i.hasNext();) {
				final ColumnComponent columnComponent = new ColumnComponent(
						(Column) i.next(), this);
				this.addDiagramComponent(columnComponent);
			}

		// Resize the diagram to fit.
		this.resizeDiagram();
	}
}
