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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays arbitrary objects linked in a radial form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.14, 17th May 2006
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
    
    public void addDiagramComponent(DiagramComponent comp) {
        this.componentMap.put(comp.getObject(), comp);
        this.componentMap.putAll(comp.getSubComponents());
        super.add((JComponent)comp);
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
    
    public void findObject(Object object) {
        if (object==null) return;
        JViewport viewport = (JViewport)this.getParent();
        // find out the size and coords of the selected object in our diagram.
        JComponent comp = (JComponent)this.getDiagramComponent(object);
        if (comp==null) return;
        Rectangle compBounds = comp.getBounds();
        Container parent = comp.getParent();
        while (parent != this) {
            compBounds.setLocation(
                    compBounds.x + (int)parent.getX(),
                    compBounds.y + (int)parent.getY()
                    );
            parent = parent.getParent();
        }
        Point compCentre = new Point(compBounds.x+(compBounds.width/2), compBounds.y+(compBounds.height/2));
        Dimension viewSize = viewport.getExtentSize();
        // work out target view point = centre - half view size.
        int newViewPointX = compCentre.x - (viewSize.width/2);
        int newViewPointY = compCentre.y - (viewSize.height/2);
        viewport.setViewPosition(new Point(newViewPointX, newViewPointY));
    }
    
    private Table askUserForTable() {
        Set tables = new TreeSet();
        for (Iterator i = this.componentMap.keySet().iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Table) tables.add(o);
        }
        return (Table)JOptionPane.showInputDialog(this.datasetTabSet,
                BuilderBundle.getString("findTableName"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                tables.toArray(),
                null);
    }
    
    private JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        // Find table menu.
        JMenuItem find = new JMenuItem(BuilderBundle.getString("findTableTitle"));
        find.setMnemonic(BuilderBundle.getString("findTableMnemonic").charAt(0));
        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Table table = askUserForTable();
                if (table!=null) findObject(table);
            }
        });
        contextMenu.add(find);
        
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
            // Extend.
            JPopupMenu contextMenu = this.getContextMenu();
            this.getDiagramContext().populateContextMenu(contextMenu, this.getContextMenuBaseObject());
            if (contextMenu.getComponentCount()>0) {
                // Display.
                contextMenu.show(this, evt.getX(), evt.getY());
            }
            eventProcessed = true;
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
        JViewport viewport = (JViewport)this.getParent();
        Point viewPos = null;
        if (viewport!=null) viewPos = viewport.getViewPosition();
        Map states = new HashMap();
        for (Iterator i = this.componentMap.keySet().iterator(); i.hasNext(); ) {
            Object object = i.next();
            DiagramComponent comp = (DiagramComponent)this.componentMap.get(object);
            if (comp!=null) states.put(object, comp.getState());
            else i.remove();
        }
        this.doRecalculateDiagram();
        for (Iterator i = states.keySet().iterator(); i.hasNext(); ) {
            Object object = i.next();
            DiagramComponent comp = (DiagramComponent)this.componentMap.get(object);
            if (comp!=null) comp.setState(states.get(object));
            else i.remove();
        }
        if (viewport!=null) viewport.setViewPosition(viewPos);
    }
    
    public abstract void doRecalculateDiagram();
    
    public void repaintDiagram() {
        JViewport viewport = (JViewport)this.getParent();
        Point viewPos = null;
        if (viewport!=null) viewPos = viewport.getViewPosition();
        for (Iterator i = this.componentMap.values().iterator(); i.hasNext(); ) {
            DiagramComponent comp = (DiagramComponent)i.next();
            comp.updateAppearance();
        }
        if (viewport!=null) viewport.setViewPosition(viewPos);
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
