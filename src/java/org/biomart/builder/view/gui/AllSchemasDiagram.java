/*
 * AllSchemasDiagram.java
 *
 * Created on 19 April 2006, 09:28
 */

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;

/**
 * This class deals with drawing an overview of all the table providers.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.9, 16th May 2006
 * @since 0.1
 */
public class AllSchemasDiagram extends Diagram {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.YELLOW;
    
    /**
     * The constructor rememembers the Collection that this
     * displays.
     */
    public AllSchemasDiagram(DataSetTabSet datasetTabSet) {
        super(datasetTabSet);
        this.setBackground(AllSchemasDiagram.BACKGROUND_COLOUR);
        this.recalculateDiagram();
    }
    
    /**
     * {@inheritDoc}
     * Resyncs the table providers with the contents of the set.
     */
    public void doRecalculateDiagram() {
        this.removeAll();
        // Make a set of all relations on this table provider.
        Set relations = new HashSet();
        // Add a TableComponent for each table.
        for (Iterator i = this.getDataSetTabSet().getMart().getSchemas().iterator(); i.hasNext(); ) {
            Schema schema = (Schema)i.next();
            SchemaComponent schemaComponent = new SchemaComponent(schema, this);
            this.addDiagramComponent(schemaComponent);
            relations.addAll(schema.getExternalRelations());
        }
        // Add a RelationComponent for each relation.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation relation = (Relation)i.next();
            RelationComponent relationComponent = new RelationComponent(
                    relation,
                    this,
                    (KeyComponent)this.getDiagramComponent(relation.getPrimaryKey()),
                    (KeyComponent)this.getDiagramComponent(relation.getForeignKey()));
            this.addDiagramComponent(relationComponent);
        }
        // Delegate upwards.
        this.resizeDiagram();
    }
}
