/*
 * WindowView.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Window;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of a {@link Window} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class WindowView extends SchemaView implements TableProviderListener {    
    /**
     * Internal reference to the provider we are viewing.
     */
    private final Window window;
    
    /**
     * Creates a new instance of TableProviderView over a given window.
     * @param martBuilder the MartBuilder to display the schema for.
     * @param window the window to display.
     */
    public WindowView(MartBuilder martBuilder, Window window) {
        super(martBuilder);
        this.setTableProviderListener(this);
        this.window = window;
    }
    
    /**
     * Returns the window.
     * @return the window.
     */
    public Window getWindow() {
        return this.window;
    }    
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        // Add separator.
        contextMenu.addSeparator();
        // Add our own stuff.
        final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseWindowTitle", this.window.getName()));
        sync.setMnemonic(BuilderBundle.getString("synchroniseWindowMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getTableProviderListener().synchroniseWindow(window);
            }
        });
        contextMenu.add(sync);
        final JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeWindowTitle", this.window.getName()));
        remove.setMnemonic(BuilderBundle.getString("removeWindowMnemonic").charAt(0));
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getTableProviderListener().removeWindow(window);
            }
        });
        contextMenu.add(remove);
    }
}
