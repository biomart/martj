/*
 * DiagramComponent.java
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

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 5th May 2006
 * @since 0.1
 */
public abstract class BoxShapedComponent extends JPanel implements DiagramComponent {
    /**
     * Internal reference to our display parent.
     */
    private Diagram diagram;
    
    /**
     * Internal reference to the object we represent.
     */
    private Object object;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public BoxShapedComponent(Object object, Diagram diagram) {
        super();
        this.object = object;
        this.diagram = diagram;
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.updateAppearance();
    }
    
    /**
     * Updates the tooltip.
     */
    public void updateAppearance() {
        DiagramModifier mod = this.getDiagram().getDiagramModifier();
        if (mod != null) mod.customiseAppearance(this, this.getObject()); 
        this.setBorder(BorderFactory.createLineBorder(this.getForeground()));
    }
    
    /**
     * Retrieves the parent this component belongs to.
     * @return the parent.
     */
    public Diagram getDiagram() {
        return this.diagram;
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
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = this.getDiagram().getContextMenu();
        // No additional entries for us yet.
        // Return it.
        return contextMenu;
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
            this.getDiagram().getDiagramModifier().customiseContextMenu(contextMenu, this.getObject());
            // Display.
            contextMenu.show(this, evt.getX(), evt.getY());
            eventProcessed = true;
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
}
