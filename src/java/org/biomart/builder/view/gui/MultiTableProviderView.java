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
import java.util.Collection;
import java.util.Collections;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This class deals with drawing an overview of all the table providers.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public class MultiTableProviderView extends RadialComponentDisplay implements MultiView {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.YELLOW;
    
    /**
     * Internal reference to the parent view.
     */
    private Collection tableProviders;
        
    /**
     * The constructor rememembers the Collection that this
     * displays.
     * @param tableProviders the providers collection to display.
     */
    public MultiTableProviderView(Collection tableProviders) {
        super();
        this.tableProviders = tableProviders;
        this.setBackground(MultiTableProviderView.BACKGROUND_COLOUR);
    }
    
    /**
     * Obtains the table providers this view displays.
     * @return the table providers this view displays.
     */
    public Collection getTableProviders() {
        return this.tableProviders;
    }
    
    /**
     * Resyncs the table providers with the contents of the set.
     * 
     * @param newTableProviders the table providers to view.
     */
    public void resyncTableProviders(Collection newTableProviders) {
        this.tableProviders = newTableProviders;
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        final JMenuItem redraw = new JMenuItem(BuilderBundle.getString("redrawTitle"));
        redraw.setMnemonic(BuilderBundle.getString("redrawMnemonic").charAt(0));
        redraw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                recalculateView();
            }
        });
        contextMenu.add(redraw);
        contextMenu.addSeparator();
        final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseAllTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseAllMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getListener().requestSynchroniseAll();
            }
        });
        contextMenu.add(sync);
        // Return.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     */
    protected Collection getDisplayComponents() {     
        // TODO: Construct and return a set of Component.TableProvider objects.
        return Collections.EMPTY_SET;
    }
}
