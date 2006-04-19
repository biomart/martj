/*
 * WindowListener.java
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
 * @version 0.0.1, 19th April 2006
 * @since 0.1
 */
public class WindowListener extends DefaultListener {
    /**
     * Internal reference to our window.
     */
    private Window window;
    
    /** 
     * Creates a new instance of WindowListener over
     * a given window. 
     * @param martBuilder the MartBuilder we are attached to.
     * @param window the window we are attached to.
     */
    public WindowListener(MartBuilder martBuilder, Window window) {
        super(martBuilder);
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
     * A signal to synchronise some window. Should be passed back up to
     * {@link MartBuilder#requestSynchroniseWindow(Window)}. No other
     * action should be necessary.
     * 
     * @param window the window to synchronise.
     */
    public void requestSynchroniseWindow(Window window) {
        this.getMartBuilder().synchroniseWindow(window);
    }
    
    /**
     * A signal to delete some window. Should be passed back up to
     * {@link MartBuilder#requestRemoveWindow(Window, boolean)}. No other
     * action should be necessary.
     * 
     * @param window the window to delete.
     */
    public void requestRemoveWindow(Window window) {
        this.getMartBuilder().removeWindow(window, true);
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestCustomiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        // Add separator.
        contextMenu.addSeparator();
        // Add our own stuff.
        final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseWindowTitle", this.getWindow().getName()));
        sync.setMnemonic(BuilderBundle.getString("synchroniseWindowMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                requestSynchroniseWindow(window);
            }
        });
        contextMenu.add(sync);
        final JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeWindowTitle", this.getWindow().getName()));
        remove.setMnemonic(BuilderBundle.getString("removeWindowMnemonic").charAt(0));
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                requestRemoveWindow(window);
            }
        });
        contextMenu.add(remove);
    }
}
