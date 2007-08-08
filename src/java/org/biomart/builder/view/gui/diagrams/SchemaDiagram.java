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

import java.awt.LayoutManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.SchemaLayoutManager.SchemaLayoutConstraint;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;

/**
 * Displays the contents of a schema within a diagram object. It adds a series
 * of {@link TableComponent} and {@link RelationComponent} objects when the
 * diagram is recalculated, and treats the schema object it represents as the
 * basic background object of the diagram.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class SchemaDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private Schema schema;

	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific schema.
	 * 
	 * @param layout
	 *            the layout manager to use to display the diagram.
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param schema
	 *            the schema to draw in this diagram.
	 */
	public SchemaDiagram(final LayoutManager layout, final MartTab martTab,
			final Schema schema) {
		// Call the general diagram constructor first.
		super(layout, martTab);

		// Remember the schema, then lay it out.
		this.schema = schema;
		this.recalculateDiagram();
	}

	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific schema. Uses a default layout specified by {@link Diagram}.
	 * 
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param schema
	 *            the schema to draw in this diagram.
	 */
	public SchemaDiagram(final MartTab martTab, final Schema schema) {
		// Call the general diagram constructor first.
		this(new SchemaLayoutManager(), martTab, schema);
	}

	public void doRecalculateDiagram() {
		// First of all, remove all our existing components.
		this.removeAll();

		// Add a TableComponent for each table in the schema.
		final Set usedRels = new HashSet();
		for (final Iterator i = this.getSchema().getTables().iterator(); i
				.hasNext();) {
			final Table t = (Table) i.next();
			final Collection tRels = t.getInternalRelations();
			this.add(new TableComponent(t, this), new SchemaLayoutConstraint(
					tRels.size()), Diagram.TABLE_LAYER);
			int indent = 0;
			for (final Iterator j = tRels.iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				if (!usedRels.contains(rel)) {
					this.add(new RelationComponent(rel, this),
							new SchemaLayoutConstraint(indent++),
							Diagram.RELATION_LAYER);
					usedRels.add(rel);
				}
			}
		}

		// Resize the diagram to fit our new components.
		this.resizeDiagram();
	}

	/**
	 * Returns the schema that this diagram represents.
	 * 
	 * @return the schema this diagram represents.
	 */
	public Schema getSchema() {
		return this.schema;
	}
}
