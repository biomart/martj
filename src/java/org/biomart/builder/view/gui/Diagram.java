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
 * @version 0.1.11, 12th May 2006
 * @since 0.1
 */
public abstract class Diagram extends JPanel {
    /**
     * Internal reference to our diagramContext.
     */
    private DiagramContext diagramContext;
    
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
            this.componentMap.putAll(((TableComponent)component).getSubComponents());
        } else if (component instanceof SchemaComponent) {
            this.componentMap.putAll(((SchemaComponent)component).getSubComponents());
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
    protected Object getContextMenuBaseObject() {
        return null;
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.isPopupTrigger() && this.diagramContext.isRightClickAllowed()) {
            // Extend.
            JPopupMenu contextMenu = new JPopupMenu();
            this.getDiagramContext().populateContextMenu(contextMenu, this.getContextMenuBaseObject());
            if (contextMenu.getComponentCount()>0) {
                // Display.
                contextMenu.show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDiagramContext(DiagramContext diagramContext) {
        this.diagramContext = diagramContext;
        for (Iterator i = this.componentMap.values().iterator(); i.hasNext(); ) {
            ((DiagramComponent)i.next()).updateAppearance();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DiagramContext getDiagramContext() {
        return this.diagramContext;
    }
    
    public void recalculateDiagram() {
        Map states = new HashMap();
        for (Iterator i = this.componentMap.keySet().iterator(); i.hasNext(); ) {
            Object object = i.next();
            states.put(object, ((DiagramComponent)this.componentMap.get(object)).getState());
        }
        this.doRecalculateDiagram();
        for (Iterator i = states.keySet().iterator(); i.hasNext(); ) {
            Object object = i.next();
            DiagramComponent component = (DiagramComponent)this.componentMap.get(object);
            if (component!=null) component.setState(states.get(object));
        }
    }
    
    public abstract void doRecalculateDiagram();
    
    public void redrawDiagramComponent(Object object) {
        DiagramComponent comp = (DiagramComponent)this.componentMap.get(object);
        comp.updateAppearance();
        ((JComponent)comp).repaint();
    }
    
    public void redrawAllDiagramComponents() {
        for (Iterator i = this.componentMap.keySet().iterator(); i.hasNext(); ) this.redrawDiagramComponent(i.next());
    }
    
    /**
     * Synchronise our display with our object contents.
     */
    public void resizeDiagram() {
        // Reset our size to the minimum.
        this.setSize(this.getMinimumSize());
        // Update ourselves.
        this.validate();
    }
}
