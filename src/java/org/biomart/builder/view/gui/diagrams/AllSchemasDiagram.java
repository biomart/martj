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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.SchemaLayoutManager.SchemaLayoutConstraint;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.SchemaComponent;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;

/**
 * This diagram draws a {@link SchemaComponent} for each schema in the mart. If
 * any of them have external relations to other schemas, then a
 * {@link RelationComponent} is drawn between them, and implicitly this causes
 * the table that the relation links from to be added inside the appropriate
 * schema component.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class AllSchemasDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	/**
	 * The constructor creates the diagram and associates it with a given mart
	 * tab.
	 * 
	 * @param martTab
	 *            the mart tab to associate with this schema. It will be used to
	 *            work out who receives all user menu events, etc.
	 */
	public AllSchemasDiagram(final MartTab martTab) {
		super(new SchemaLayoutManager(), martTab);

		// Calculate the diagram.
		this.recalculateDiagram();
	}

	public void doRecalculateDiagram() {
		// Remove all existing components.
		this.removeAll();

		// Add a SchemaComponent for each schema.
		final Set usedRels = new HashSet();
		for (final Iterator i = this.getMartTab().getMart().getSchemas()
				.iterator(); i.hasNext();) {
			final Schema schema = (Schema) i.next();
			final SchemaComponent schemaComponent = new SchemaComponent(schema,
					this);
			// Count and remember relations.
			int indent = 0;
			final Collection extRels = schema.getExternalRelations();
			for (final Iterator j = extRels.iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				if (!usedRels.contains(rel)) {
					this.add(new RelationComponent(rel, this),
							new SchemaLayoutConstraint(indent++),
							Diagram.RELATION_LAYER);
					usedRels.add(rel);
				}
			}
			this.add(schemaComponent,
					new SchemaLayoutConstraint(extRels.size()),
					Diagram.TABLE_LAYER);
		}

		// Resize the diagram to fit the components.
		this.resizeDiagram();
	}
}
