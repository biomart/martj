/*
 * TableProviderDiagram.java
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
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This class deals with drawing an overview of all the table providers.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 25th April 2006
 * @since 0.1
 */
public class TableProviderDiagram extends Diagram {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.YELLOW;
    
    /**
     * The constructor rememembers the Collection that this
     * displays.
     */
    public TableProviderDiagram(WindowTabSet windowTabSet) {
        super(windowTabSet);
        this.setBackground(TableProviderDiagram.BACKGROUND_COLOUR);
        this.synchroniseDiagram();
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                windowTabSet.synchroniseSchema();
            }
        });
        contextMenu.add(sync);
        
        JMenuItem add = new JMenuItem(BuilderBundle.getString("addTblProvTitle"));
        add.setMnemonic(BuilderBundle.getString("addTblProvMnemonic").charAt(0));
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                windowTabSet.getTableProviderTabSet().requestAddTableProvider();
            }
        });
        contextMenu.add(add);
        
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
        for (Iterator i = this.getWindowTabSet().getSchema().getTableProviders().iterator(); i.hasNext(); ) {
            TableProvider tableProvider = (TableProvider)i.next();
            TableProviderDiagramComponent tableProviderComponent = new TableProviderDiagramComponent(tableProvider, this);
            this.add(tableProviderComponent);
            relations.addAll(tableProvider.getExternalRelations());
            keyComponents.putAll(tableProviderComponent.getKeyComponents());
        }
        // Add a RelationDiagramComponent for each relation.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation relation = (Relation)i.next();
            RelationDiagramComponent relationComponent = new RelationDiagramComponent(
                    relation,
                    this,
                    (KeyDiagramComponent)keyComponents.get(relation.getPrimaryKey()),
                    (KeyDiagramComponent)keyComponents.get(relation.getForeignKey()));
            this.add(relationComponent);
        }
        // Delegate upwards.
        super.synchroniseDiagram();
    }
}
