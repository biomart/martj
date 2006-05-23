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
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;

/**
 * Displays the contents of a schema within a standard diagram object. It adds a
 * series of {@link TableComponent} and {@link RelationComponent} objects when
 * the diagram is recalculated, and treats the schema object it represents as
 * the basic background object of the diagram.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.12, 17th May 2006
 * @since 0.1
 */
public class SchemaDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private Schema schema;

	/**
	 * The background colour to use for this diagram.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific schema.
	 * 
	 * @param schema
	 *            the schema to draw in this diagram.
	 */
	public SchemaDiagram(DataSetTabSet datasetTabSet, Schema schema) {
		// Call the general diagram constructor first.
		super(datasetTabSet);

		// Set up our background colour.
		this.setBackground(SchemaDiagram.BACKGROUND_COLOUR);

		// Remember the schema, then lay it out.
		this.schema = schema;
		this.recalculateDiagram();
	}

	/**
	 * Returns the schema that this diagram represents.
	 * 
	 * @return the schema this diagram represents.
	 */
	public Schema getSchema() {
		return this.schema;
	}

	protected Object getContextMenuBaseObject() {
		// Use the schema as the general background base object
		// for this diagram. This means that any click on the
		// background will be treated as though the schema object was clicked
		// on.
		return this.getSchema();
	}

	public void doRecalculateDiagram() {
		// First of all, remove all our existing components.
		this.removeAll();

		// Add a TableComponent for each table.
		for (Iterator i = this.getSchema().getTables().iterator(); i.hasNext();)
			this
					.addDiagramComponent(new TableComponent((Table) i.next(),
							this));

		// Add a RelationComponent for each relation. We only work with
		// internal relations because we can't correctly display external ones
		// as they have ends in other schemas.
		for (Iterator i = this.getSchema().getInternalRelations().iterator(); i
				.hasNext();) {
			Relation relation = (Relation) i.next();
			RelationComponent relationComponent = new RelationComponent(
					relation, this);
			this.addDiagramComponent(relationComponent);
		}

		// Resize the diagram to fit our new components.
		this.resizeDiagram();
	}
}
