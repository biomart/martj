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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.SchemaComponent;

/**
 * This diagram draws a box for each schema in the mart, as a
 * {@link SchemaComponent}. If any of them have external relations to other
 * schemas, then a {@link RelationComponent} is drawn between them.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author$
 * @since 0.1
 */
public class AllSchemasDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	/**
	 * Static reference to the background colour to use for components.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	/**
	 * The constructor creates the diagram and associates it with a given mart
	 * tab.
	 * 
	 * @param martTab
	 *            the mart tab to associate with this schema. It will be used to
	 *            work out who receives all user menu events, etc.
	 */
	public AllSchemasDiagram(final MartTab martTab) {
		super(martTab);

		// Calculate the diagram.
		this.recalculateDiagram();
	}

	protected void updateAppearance() {
		// Set the background.
		this.setBackground(AllSchemasDiagram.BACKGROUND_COLOUR);
	}

	public void doRecalculateDiagram() {
		// Remove all existing components.
		this.removeAll();

		// Make a set to hold all external relations on this diagram.
		final Set relations = new HashSet();

		// Add a SchemaComponent for each schema.
		for (final Iterator i = this.getMartTab().getMart().getSchemas()
				.iterator(); i.hasNext();) {
			final Schema schema = (Schema) i.next();
			final SchemaComponent schemaComponent = new SchemaComponent(schema,
					this);
			this.addDiagramComponent(schemaComponent);
			// Remember the external relations.
			relations.addAll(schema.getExternalRelations());
		}

		// Add a RelationComponent for each external relation.
		for (final Iterator i = relations.iterator(); i.hasNext();) {
			final Relation relation = (Relation) i.next();
			final RelationComponent relationComponent = new RelationComponent(
					relation, this);
			this.addDiagramComponent(relationComponent);
		}

		// Resize the diagram to fit the components.
		this.resizeDiagram();
	}
}
