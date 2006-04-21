/*
 * WindowAdaptor.java
 *
 * Created on 19 April 2006, 09:43
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
 * Adapts listener behaviour by adding in Window-specific stuff.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 21st April 2006
 * @since 0.1
 */
public class WindowAdaptor extends DefaultAdaptor {
    /**
     * Internal reference to our window.
     */
    private Window window;
    
    /**
     * 
     * Creates a new instance of WindowAdaptor over
     * a given window. 
     * 
     * @param window the window we are attached to.
     */
    public WindowAdaptor(WindowTabSet windowTabSet, Window window) {
        super(windowTabSet);
        this.window = window;
    }
    
    /**
     * Retrieve our window.
     * @return our window.
     */
    protected Window getWindow() {
        return this.window;
    }    
      
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        // Add separator.
        contextMenu.addSeparator();
        // Add our own stuff.
        final JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeWindowTitle", this.getWindow().getName()));
        remove.setMnemonic(BuilderBundle.getString("removeWindowMnemonic").charAt(0));
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                windowTabSet.confirmRemoveWindow(window);
            }
        });
        contextMenu.add(remove);
    }
}
