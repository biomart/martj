/*
 * MultiTableProviderView.java
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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This class deals with drawing an overview of all the table providers.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 24th April 2006
 * @since 0.1
 */
public class MultiTableProviderView extends View {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.YELLOW;
        
    /**
     * The constructor rememembers the Collection that this
     * displays.
     */
    public MultiTableProviderView(WindowTabSet windowTabSet) {
        super(windowTabSet);
        this.setBackground(MultiTableProviderView.BACKGROUND_COLOUR);
        this.synchronise();
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.addSeparator();
        final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                windowTabSet.getSchemaManager().synchroniseSchema();
            }
        });
        contextMenu.add(sync);
        // Return.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * Resyncs the table providers with the contents of the set.
     */
    public void synchronise() {
        // TODO: Construct/update our set of Component.TableProvider and Component.Relation objects.
        this.removeAll();
        // Delegate upwards.
        super.synchronise();
    }
}
