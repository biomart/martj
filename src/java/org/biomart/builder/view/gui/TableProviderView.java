/*
 * TableProviderView.java
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
import java.util.Collection;
import java.util.Collections;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of a {@link TableProvider} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 19th April 2006
 * @since 0.1
 */
public class TableProviderView extends RadialComponentDisplay {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.WHITE;
    
    /**
     * Internal reference to the provider we are viewing.
     */
    private final TableProvider tableProvider;
    
    /**
     * Creates a new instance of TableProviderView over a given provider.
     * @param tableProvider the given table provider.
     */
    public TableProviderView(TableProvider tableProvider) {
        super();
        this.tableProvider = tableProvider;
        this.setBackground(TableProviderView.BACKGROUND_COLOUR);
    }
    
    /**
     * Obtains the table provider this view displays.
     * @return the table provider this view displays.
     */
    protected TableProvider getTableProvider() {
        return this.tableProvider;
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        // The following are applicable to all table provider views.
        final JMenuItem redraw = new JMenuItem(BuilderBundle.getString("redrawTitle"));
        redraw.setMnemonic(BuilderBundle.getString("redrawMnemonic").charAt(0));
        redraw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                recalculateView();
            }
        });
        contextMenu.add(redraw);
        // The following are not applicable to DataSets (we can tell by the listener type).
        if (!(this.getListener() instanceof DataSetListener)) {
            contextMenu.addSeparator();
            final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseTblProvTitle", this.tableProvider.getName()));
            sync.setMnemonic(BuilderBundle.getString("synchroniseTblProvMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getListener().requestSynchroniseTableProvider(tableProvider);
                }
            });
            contextMenu.add(sync);
            final JMenuItem test = new JMenuItem(BuilderBundle.getString("testTblProvTitle", this.tableProvider.getName()));
            test.setMnemonic(BuilderBundle.getString("testTblProvMnemonic").charAt(0));
            test.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getListener().requestTestTableProvider(tableProvider);
                }
            });
            contextMenu.add(test);
            final JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeTblProvTitle", this.tableProvider.getName()));
            remove.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getListener().requestRemoveTableProvider(tableProvider);
                }
            });
            contextMenu.add(remove);
        }
        // Return.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     */
    protected Collection getDisplayComponents() {
        // TODO: Construct and return a set of Component.Table objects.
        return Collections.EMPTY_SET;
    }
}
