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
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;

/**
 * Displays the contents of a {@link Schema} in graphical form.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 17th May 2006
 * @since 0.1
 */
public class ExplainDataSetDiagram extends Diagram {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.WHITE;
    
    /**
     * Internal reference to the dataset table we are viewing.
     */
    private DataSetTable datasetTable;
    
    /**
     * Creates a new instance of SchemaDiagram over a given provider.
     *
     *
     * @param schema the given table provider.
     */
    public ExplainDataSetDiagram(DataSetTabSet datasetTabSet, DataSetTable datasetTable) {
        super(datasetTabSet);
        this.setBackground(ExplainDataSetDiagram.BACKGROUND_COLOUR);
        this.datasetTable = datasetTable;
        this.recalculateDiagram();
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        return new JPopupMenu();
    }
    
    /**
     * {@inheritDoc}
     * Resyncs the table providers with the contents of the set.
     */
    public void doRecalculateDiagram() {
        this.removeAll();
        // Add a TableComponent for the underlying table.
        Table underlyingTable = this.datasetTable.getUnderlyingTable();
        if (underlyingTable!=null) this.addDiagramComponent(new TableComponent(underlyingTable, this));
        // Add a TableComponent for each table.
        for (Iterator i = this.datasetTable.getUnderlyingRelations().iterator(); i.hasNext(); ) {
            Relation relation = (Relation)i.next();
            Table pkTable = relation.getPrimaryKey().getTable();
            if (this.getDiagramComponent(pkTable)==null) {
                TableComponent tableComponent = new TableComponent(pkTable, this);
                this.addDiagramComponent(tableComponent);
            }
            Table fkTable = relation.getForeignKey().getTable();
            if (this.getDiagramComponent(fkTable)==null) {
                TableComponent tableComponent = new TableComponent(fkTable, this);
                this.addDiagramComponent(tableComponent);
            }
        }
        // Add Relations last to prevent overlapping.
        for (Iterator i = this.datasetTable.getUnderlyingRelations().iterator(); i.hasNext(); ) {
            Relation relation = (Relation)i.next();
            RelationComponent relationComponent = new RelationComponent(
                    relation,
                    this,
                    relation.getPrimaryKey(),
                    relation.getForeignKey());
            this.addDiagramComponent(relationComponent);
        }
        // Delegate upwards.
        this.resizeDiagram();
    }
}
