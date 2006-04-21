/*
 * View.java
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

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Displays arbitrary objects linked in a radial form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 21st April 2006
 * @since 0.1
 */
public abstract class View extends JPanel {
    /**
     * Internal reference to our adaptor.
     */
    private Adaptor adaptor;
    
    /**
     * The current display flags.
     */
    private int flags;
    
    /**
     * The window tab set we belong to.
     */
    protected WindowTabSet windowTabSet;
    
    /**
     * Creates a new instance of View.
     */
    public View(WindowTabSet windowTabSet) {
        // GUI stuff.
        super(new RadialLayout());
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        // Business stuff.
        this.windowTabSet = windowTabSet;
        this.synchronise();
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    protected abstract JPopupMenu getContextMenu();
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.isPopupTrigger()) {
            // Only respond to individual table providers, not the overview tab.
            JPopupMenu contextMenu = this.getContextMenu();
            // Extend.
            if (this.adaptor != null) this.getAdaptor().customiseContextMenu(contextMenu, null);
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
    public void setAdaptor(Adaptor adaptor) {
        this.adaptor = adaptor;
    }
    
    /**
     * {@inheritDoc}
     */
    public Adaptor getAdaptor() {
        return this.adaptor;
    }
    
    /**
     * Reset the current display mask.
     */
    public void clearFlags() {
        this.flags = 0;
    }
    
    /**
     * Set a particular flag on the display mask.
     * @param flag the flag to set.
     */
    public void setFlag(int flag) {
        this.flags |= flag;
    }
    
    /**
     * Test for a particular display mask flag.
     * @param flag the flag to test for.
     * @return true if it is set, false if not.
     */
    public boolean getFlag(int flag) {
        return (this.flags & flag) == flag;
    }
    
    /**
     * Get the current display mask.
     * @return the current display mask.
     */
    public int getFlags() {
        return this.flags;
    }
    
    /**
     * Synchronise our display with our object contents.
     */
    public abstract void synchronise();
}
