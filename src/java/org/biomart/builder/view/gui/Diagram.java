/*
 * Diagram.java
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Displays arbitrary objects linked in a radial form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 5th May 2006
 * @since 0.1
 */
public abstract class Diagram extends JPanel {    
    /**
     * Internal reference to our diagramModifier.
     */
    private DiagramModifier diagramModifier;
    
    /**
     * The window tab set we belong to.
     */
    protected DataSetTabSet datasetTabSet;
    
    private Map componentMap = new HashMap();
    
    /**
     * Creates a new instance of Diagram.
     */
    public Diagram(DataSetTabSet datasetTabSet) {
        // GUI stuff.
        super(new RadialLayout());
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        // Business stuff.
        this.datasetTabSet = datasetTabSet;
    }

    public void removeAll() {
        super.removeAll();
        this.componentMap.clear();
    }

    public void addDiagramComponent(DiagramComponent component) {
        this.componentMap.put(component.getObject(), component);
        if (component instanceof TableComponent) {
            this.componentMap.putAll(((TableComponent)component).getKeyComponents());
        } else if (component instanceof SchemaComponent) {
            this.componentMap.putAll(((SchemaComponent)component).getKeyComponents());
        }
        super.add((JComponent)component);
    }
    
    public DiagramComponent getDiagramComponent(Object object) {
        return (DiagramComponent) this.componentMap.get(object);
    }
    
    /**
     * The window tab set.
     */
    protected DataSetTabSet getDataSetTabSet() {
        return this.datasetTabSet;
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
            if (this.diagramModifier != null) this.getDiagramModifier().customiseContextMenu(contextMenu, null);
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
    public void setDiagramModifier(DiagramModifier adaptor) {
        this.diagramModifier = adaptor;
        for (Iterator i = this.componentMap.values().iterator(); i.hasNext(); ) {
            ((DiagramComponent)i.next()).updateAppearance();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DiagramModifier getDiagramModifier() {
        return this.diagramModifier;
    }
    
    public abstract void recalculateDiagram();
    
    public void redrawDiagramComponent(Object object) {
        DiagramComponent comp = (DiagramComponent)this.componentMap.get(object);
        comp.updateAppearance();
        ((JComponent)comp).repaint();
    }
    
    /**
     * Synchronise our display with our object contents.
     */
    public void resizeDisplay() {
        // Reset our size to the minimum.
        this.setSize(this.getMinimumSize());
        // Update ourselves.
        this.validate();
    }
}
