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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.DataSetLayoutManager.DataSetLayoutConstraint;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.utils.Transaction.WeakPropertyChangeListener;

/**
 * Displays the contents of a dataset within a standard diagram object. This is
 * identical to {@link SchemaDiagram} except that it shows {@link DataSet}
 * objects, instead of plain {@link Schema} objects. As {@link DataSet} extends
 * {@link Schema}, this means that almost all of the code in
 * {@link SchemaDiagram} can be reused for displaying datasets.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class DataSetDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private final DataSet dataset;

	private final Collection listeningTables = new HashSet(); 

	private final PropertyChangeListener listener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent evt) {
			DataSetDiagram.this.needsRecalc = true;
		}
	};

	private final PropertyChangeListener repaintListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent evt) {
			DataSetDiagram.this.needsRepaint = true;
		}
	};

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
		this.setBackground(Diagram.BACKGROUND_COLOUR);

		// Remember the schema, then lay it out.
		this.dataset = dataset;
		this.recalculateDiagram();

		// If any tables or relations change, whole diagram needs
		// redoing from scratch, and new listeners need setting up.
		dataset.getTables().addPropertyChangeListener(
				new WeakPropertyChangeListener(dataset.getTables(),
						this.listener));
		dataset.getRelations().addPropertyChangeListener(
				new WeakPropertyChangeListener(dataset.getRelations(),
						this.listener));

		// Listen to when hide masked gets changed.
		dataset.addPropertyChangeListener("hideMasked",
				new WeakPropertyChangeListener(dataset, "hideMasked",
						this.repaintListener));
		dataset.addPropertyChangeListener("name",
				new WeakPropertyChangeListener(dataset, "name",
						this.listener));

		this.setHideMasked(dataset.isHideMasked());
	}

	protected void hideMaskedChanged(final boolean newHideMasked) {
		this.dataset.setHideMasked(newHideMasked);
	}

	public void doRecalculateDiagram() {
		// Add stuff.
		final Set usedTables = new HashSet();
		final List mainTables = new ArrayList();
		mainTables.add(this.getDataSet().getMainTable());
		for (int i = 0; i < mainTables.size(); i++) {
			final DataSetTable table = (DataSetTable) mainTables.get(i);
			// Create constraint.
			final DataSetLayoutConstraint constraint = new DataSetLayoutConstraint(
					DataSetLayoutConstraint.MAIN, i);
			// Add main table.
			this.add(new TableComponent(table, this), constraint,
					Diagram.TABLE_LAYER);
			if (!this.listeningTables.contains(table.getName())) {
				table.addPropertyChangeListener("type",
						new WeakPropertyChangeListener(table, "type",
								this.listener));
				this.listeningTables.add(table.getName());
			}
			usedTables.add(table.getName());
			// Add dimension tables.
			if (table.getPrimaryKey() != null)
				for (final Iterator r = table.getPrimaryKey().getRelations()
						.iterator(); r.hasNext();) {
					final Relation relation = (Relation) r.next();
					final DataSetTable target = (DataSetTable) relation
							.getManyKey().getTable();
					if (target.getType().equals(DataSetTableType.DIMENSION)) {
						// Create constraint.
						final DataSetLayoutConstraint dimConstraint = new DataSetLayoutConstraint(
								DataSetLayoutConstraint.DIMENSION, i);
						// Add dimension table.
						this.add(new TableComponent(target, this),
								dimConstraint, Diagram.TABLE_LAYER);
						if (!this.listeningTables.contains(table.getName())) {
							table.addPropertyChangeListener("type",
									new WeakPropertyChangeListener(table,
											"type", this.listener));
							this.listeningTables.add(table.getName());
						}
						usedTables.add(table.getName());
					} else
						mainTables.add(target);
					// Add relation.
					this.add(new RelationComponent(relation, this),
							Diagram.RELATION_LAYER);
				}
		}
		this.listeningTables.retainAll(usedTables);
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
