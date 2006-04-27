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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Schema;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of a {@link Schema} in graphical form.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 27th April 2006
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
        this.synchroniseDiagram();
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
        
        // The following are not applicable to DataSets (we can tell by the listener type).
        if (!(this.getDiagramModifier() instanceof DataSetDiagramModifier)) {
            contextMenu.addSeparator();
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().renameSchema(schema);
                }
            });
            contextMenu.add(rename);
            
            JMenuItem modify = new JMenuItem(BuilderBundle.getString("modifySchemaTitle"));
            modify.setMnemonic(BuilderBundle.getString("modifySchemaMnemonic").charAt(0));
            modify.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestModifySchema(getSchema());
                }
            });
            contextMenu.add(modify);
            
            JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
            sync.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().getSchemaTabSet().synchroniseSchema(schema);
                }
            });
            contextMenu.add(sync);
            
            JMenuItem test = new JMenuItem(BuilderBundle.getString("testSchemaTitle"));
            test.setMnemonic(BuilderBundle.getString("testSchemaMnemonic").charAt(0));
            test.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().getSchemaTabSet().testSchema(schema);
                }
            });
            contextMenu.add(test);
            
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeSchemaTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeSchemaMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().confirmRemoveSchema(schema);
                }
            });
            contextMenu.add(remove);
        }
        // Return.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * Resyncs the table providers with the contents of the set.
     */
    public void synchroniseDiagram() {
        // TODO: Construct/update our set of Component.Table and Component.Relation objects.
        this.removeAll();
        // Make a set of all relations on this table provider.
        List relations = new ArrayList();
        Map keyComponents = new HashMap();
        // Add a TableComponent for each table.
        for (Iterator i = this.getSchema().getTables().iterator(); i.hasNext(); ) {
            Table table = (Table)i.next();
            TableComponent tableComponent = new TableComponent(table, this);
            this.add(tableComponent);
            if (table.getPrimaryKey()!=null) relations.addAll(table.getPrimaryKey().getRelations()); // All relations link to a PK at some point.
            keyComponents.putAll(tableComponent.getKeyComponents());
        }
        // Add a RelationComponent for each relation.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation relation = (Relation)i.next();
            // Only interested in relations that link between tables in our own table provider.
            if (relation.getForeignKey().getTable().getSchema().equals(this.getSchema())) {
                RelationComponent relationComponent = new RelationComponent(
                        relation,
                        this,
                        (KeyComponent)keyComponents.get(relation.getPrimaryKey()),
                        (KeyComponent)keyComponents.get(relation.getForeignKey()));
                this.add(relationComponent);
            }
        }
        // Delegate upwards.
        super.synchroniseDiagram();
    }
}
