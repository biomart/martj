/*
 * TableDiagram.java
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
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of a {@link TableProvider} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 25th April 2006
 * @since 0.1
 */
public class TableDiagram extends Diagram {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.WHITE;
    
    /**
     * Internal reference to the provider we are viewing.
     */
    private TableProvider tableProvider;
    
    /**
     * Creates a new instance of TableDiagram over a given provider.
     * 
     * @param tableProvider the given table provider.
     */
    public TableDiagram(WindowTabSet windowTabSet, TableProvider tableProvider) {
        super(windowTabSet);
        this.setBackground(TableDiagram.BACKGROUND_COLOUR);
        this.tableProvider = tableProvider;
        this.synchroniseDiagram();
    }
    
    /**
     * Returns our table provider.
     */
    public TableProvider getTableProvider() {
        return this.tableProvider;
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
        if (!(this.getAdaptor() instanceof DataSetDiagramModifier)) {
            contextMenu.addSeparator();
            
            JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseTblProvTitle"));
            sync.setMnemonic(BuilderBundle.getString("synchroniseTblProvMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getWindowTabSet().getTableProviderTabSet().synchroniseTableProvider(tableProvider);
                }
            });
            contextMenu.add(sync);
            
            JMenuItem test = new JMenuItem(BuilderBundle.getString("testTblProvTitle"));
            test.setMnemonic(BuilderBundle.getString("testTblProvMnemonic").charAt(0));
            test.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getWindowTabSet().getTableProviderTabSet().testTableProvider(tableProvider);
                }
            });
            contextMenu.add(test);
            
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeTblProvTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    windowTabSet.getTableProviderTabSet().confirmRemoveTableProvider(tableProvider);
                }
            });
            contextMenu.add(remove);
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameTblProvTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameTblProvMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    windowTabSet.getTableProviderTabSet().renameTableProvider(tableProvider);
                }
            });
            contextMenu.add(rename);
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
        // Add a TableDiagramComponent for each table.
        for (Iterator i = this.getTableProvider().getTables().iterator(); i.hasNext(); ) {
            Table table = (Table)i.next();
            TableDiagramComponent tableComponent = new TableDiagramComponent(table, this);
            this.add(tableComponent);
            if (table.getPrimaryKey()!=null) relations.addAll(table.getPrimaryKey().getRelations()); // All relations link to a PK at some point.
            keyComponents.putAll(tableComponent.getKeyComponents());
        }
        // Add a RelationDiagramComponent for each relation.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation relation = (Relation)i.next();
            // Only interested in relations that link between tables in our own table provider.
            if (relation.getForeignKey().getTable().getTableProvider().equals(this.getTableProvider())) {
                RelationDiagramComponent relationComponent = new RelationDiagramComponent(
                        relation,
                        this,
                        (KeyDiagramComponent)keyComponents.get(relation.getPrimaryKey()),
                        (KeyDiagramComponent)keyComponents.get(relation.getForeignKey()));
                this.add(relationComponent);
            }
        }
        // Delegate upwards.
        super.synchroniseDiagram();
    }
}
