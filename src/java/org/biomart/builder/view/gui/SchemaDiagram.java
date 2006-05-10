/*
 * SchemaDiagram.java
 *
 * Created on 11 April 2006, 16:00
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
import java.util.Iterator;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Schema;

/**
 * Displays the contents of a {@link Schema} in graphical form.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 10th May 2006
 * @since 0.1
 */
public class SchemaDiagram extends Diagram {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.WHITE;
    
    /**
     * Internal reference to the provider we are viewing.
     */
    private Schema schema;
    
    /**
     * Creates a new instance of SchemaDiagram over a given provider.
     *
     *
     * @param schema the given table provider.
     */
    public SchemaDiagram(DataSetTabSet datasetTabSet, Schema schema) {
        super(datasetTabSet);
        this.setBackground(SchemaDiagram.BACKGROUND_COLOUR);
        this.schema = schema;
        this.recalculateDiagram();
    }
    
    /**
     * Returns our table provider.
     */
    public Schema getSchema() {
        return this.schema;
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        // The following are applicable to all table provider views.
        
        // Nothing, yet!
        
        // Return the customised menu.
        this.getDiagramContext().customiseContextMenu(contextMenu, this.getSchema());
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * Resyncs the table providers with the contents of the set.
     */
    public void doRecalculateDiagram() {
        this.removeAll();
        // Add a TableComponent for each table.
        for (Iterator i = this.getSchema().getTables().iterator(); i.hasNext(); ) {
            Table table = (Table)i.next();
            this.addDiagramComponent(new TableComponent(table, this));
        }
        // Add a RelationComponent for each relation.
        for (Iterator i = this.getSchema().getInternalRelations().iterator(); i.hasNext(); ) {
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
