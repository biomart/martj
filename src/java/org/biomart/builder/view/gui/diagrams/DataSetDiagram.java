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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.DataSetLayoutManager.DataSetLayoutConstraint;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;

/**
 * Displays the contents of a dataset within a standard diagram object. This is
 * identical to {@link SchemaDiagram} except that it shows {@link DataSet}
 * objects, instead of plain {@link Schema} objects. As {@link DataSet} extends
 * {@link Schema}, this means that almost all of the code in
 * {@link SchemaDiagram} can be reused for displaying datasets.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class DataSetDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private final DataSet dataset;

	/**
	 * The background colour to use for this diagram.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific dataset.
	 * 
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param dataset
	 *            the dataset to draw in this diagram.
	 */
	public DataSetDiagram(final MartTab martTab, final DataSet dataset) {
		// Call the general diagram constructor first.
		super(new DataSetLayoutManager(), martTab);

		// Set up our background colour.
		this.setBackground(SchemaDiagram.BACKGROUND_COLOUR);

		// Remember the schema, then lay it out.
		this.dataset = dataset;
		this.recalculateDiagram();
	}

	protected void doUpdateAppearance() {
		// Set the background.
		this.setBackground(SchemaDiagram.BACKGROUND_COLOUR);
	}

	public void doRecalculateDiagram() {
		// First of all, remove all our existing components.
		this.removeAll();

		// Add stuff.
		final List relations = new ArrayList();
		final List mainTables = new ArrayList();
		mainTables.add(this.getDataSet().getMainTable());
		for (int i = 0; i < mainTables.size(); i++) {
			final DataSetTable table = (DataSetTable) mainTables.get(i);
			// Create constraint.
			final DataSetLayoutConstraint constraint = new DataSetLayoutConstraint(
					DataSetLayoutConstraint.MAIN, i);
			// Add main table.
			this.add(new TableComponent(table, this), constraint);
			// Add dimension tables.
			for (final Iterator r = table.getRelations().iterator(); r
					.hasNext();) {
				final Relation relation = (Relation) r.next();
				if (relation.getOneKey().getTable().equals(table)) {
					final DataSetTable target = (DataSetTable) relation
							.getManyKey().getTable();
					if (target.getType().equals(DataSetTableType.DIMENSION)) {
						// Create constraint.
						final DataSetLayoutConstraint dimConstraint = new DataSetLayoutConstraint(
								DataSetLayoutConstraint.DIMENSION, i);
						// Add dimension table.
						this.add(new TableComponent(target, this),
								dimConstraint);
					} else {
						mainTables.add(target);
					}
					// Add relation.
					relations.add(relation);
				}
			}
		}

		// Add relations. Must be done last otherwise they can't find their
		// keys.
		for (final Iterator r = relations.iterator(); r.hasNext();)
			this.add(new RelationComponent((Relation) r.next(), this));

		// Resize the diagram to fit our new components.
		this.resizeDiagram();
	}

	/**
	 * Returns the dataset that this diagram represents.
	 * 
	 * @return the dataset this diagram represents.
	 */
	public DataSet getDataSet() {
		return this.dataset;
	}
}
