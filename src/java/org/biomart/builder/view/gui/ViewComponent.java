/*
 * ViewComponent.java
 *
 * Created on 19 April 2006, 15:36
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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * An element that can be drawn on a View. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 20th April 2006
 * @since 0.1
 */
public abstract class ViewComponent extends JComponent {
    /**
     * Internal reference to our display parent.
     */
    private View parentDisplay;
    
    /**
     * Internal reference to the object we represent.
     */
    private Object object;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public ViewComponent(Object object, View parentDisplay) {
        this.object = object;
        this.parentDisplay = parentDisplay;
    }
    
    /**
     * Retrieves the parent this component belongs to.
     * @return the parent.
     */
    public View getParentDisplay() {
        return this.parentDisplay;
    }
    
    /**
     * Retrieves the real object this component is a representation of.
     * @return the real object.
     */
    public Object getObject() {
        return this.object;
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        return this.getParentDisplay().getContextMenu();
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.isPopupTrigger()) {
            // Build the basic menu.
            JPopupMenu contextMenu = this.getContextMenu();
            // Extend.
            this.getParentDisplay().getAdaptor().customiseContextMenu(contextMenu, this.getObject());
            // Display.
            contextMenu.show(this, evt.getX(), evt.getY());
            eventProcessed = true;
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void paintComponent(Graphics g) {
        // Paint background, if required.
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        Graphics2D g2d = (Graphics2D)g.create();
        // Do painting of this component.
        this.getParentDisplay().clearFlags();
        this.getParentDisplay().getAdaptor().aboutToDraw(this.getObject());
        this.paintComponent(g2d, this.getParentDisplay().getFlags());
        // Clean up.
        g2d.dispose();
    }
    
    /**
     * Override this to do your painting. The background has already
     * been done for you if you are opaque.
     */
    protected abstract void paintComponent(Graphics2D g2d, int flags);
}
