/*
 * RadialComponentDisplay.java
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.biomart.builder.resources.BuilderBundle;
import org.biomart.builder.view.gui.Component.DescendingHeight;
import org.biomart.builder.view.gui.Component.DescendingWidth;

/**
 * Displays arbitrary objects linked in a radial form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public abstract class RadialComponentDisplay extends JComponent implements View, ComponentDisplay {
    /**
     * Constant referring to the padding between rings.
     */
    public static final double PADDING_RADIUS = 36.0; // 72 = 1/2 inch at 72 dpi
    
    /**
     * Internal reference to our event listener.
     */
    private Listener listener;
    
    /**
     * The current display flags.
     */
    private int flags;
    
    /**
     * The current set of rings. Keys are radii, values are lists of components to display.
     */
    private Map rings = new TreeMap();
    
    /**
     * The component/shape/component maps.
     */
    private Map componentToShape = new HashMap();
    private Map shapeToComponent = new HashMap();
    
    /**
     * Creates a new instance of RadialComponentDisplay.
     */
    public RadialComponentDisplay() {
        super();
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
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
            // Where was the click?
            Object component = this.getObjectAtLocation(evt.getPoint());
            // Extend.
            this.getListener().requestCustomiseContextMenu(contextMenu, component);
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
    public void setListener(Listener listener) throws NullPointerException {
        if (listener==null)
            throw new NullPointerException(BuilderBundle.getString("listenerIsNull"));
        this.listener = listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public Listener getListener() {
        return this.listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public JComponent asJComponent() {
        return this;
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
     * {@inheritDoc}
     */
    public void recalculateView() {
        // Clear the rings.
        this.rings.clear();
        this.componentToShape.clear();
        this.shapeToComponent.clear();
        // Work out what's going into them.
        Collection displayComponents = this.getDisplayComponents();
        // First, construct a set of ring families, sorted in ascending order
        // by number of relations.
        Map ringFamilies = new TreeMap();
        for (Iterator i = displayComponents.iterator(); i.hasNext(); ) {
            Component component = (Component)i.next();
            Integer ringNumber = new Integer(component.getRelations().size());
            if (!ringFamilies.containsKey(ringNumber)) ringFamilies.put(ringNumber, new ArrayList());
            ((List)ringFamilies.get(ringNumber)).add(component);
        }
        // Obtain the descending set of number of relations.
        List ringNumbers = new ArrayList(ringFamilies.keySet());
        Collections.reverse(ringNumbers);
        // Construct the rings, innermost first.
        double innerRingRadius = 0.0;
        for (Iterator i = ringNumbers.iterator(); i.hasNext(); ) {
            List components = (List)ringFamilies.get(i.next());
            int median = components.size() / 2;
            if (components.size() % 2 != 0) median++;
            // Order them by width and find the minimum radius.
            Collections.sort(components, new DescendingWidth());
            double minWidthRadius = 0.0;
            for (int j = 0; j < median; j++) minWidthRadius += ((Component)components.get(j)).getWidth();
            minWidthRadius /= 2.0; // convert from diameter to radius.
            // Order them by height and find the minimum radius.
            Collections.sort(components, new DescendingHeight());
            double minHeightRadius = 0.0;
            for (int j = 0; j < median; j++) minHeightRadius += ((Component)components.get(j)).getHeight();
            minHeightRadius /= 2.0; // convert from diameter to radius.
            // Work out the actual minimum radius.
            double minRadius = Math.max(minHeightRadius, minWidthRadius);
            minRadius += innerRingRadius + RadialComponentDisplay.PADDING_RADIUS;
            // Plant them randomly around the ring.
            Collections.shuffle(components);
            this.rings.put(new Double(innerRingRadius), components);
            // Set the inner radius for the next ring.
            innerRingRadius = minRadius;
        }
        // Work out the size of the largest ring plus a bit of padding, and set it as our own size.
        int canvasCentre = (int)(innerRingRadius + RadialComponentDisplay.PADDING_RADIUS);
        this.setSize(canvasCentre*2, canvasCentre*2);
        // Construct component->shape maps.
        for (Iterator i = this.rings.keySet().iterator(); i.hasNext(); ) {
            Double radius = (Double)i.next();
            List components = (List)this.rings.get(radius);
            // Space them equally around the points of a circle centred
            // at (x,y) = (canvasCentre, canvasCentre). The circle is divided into
            // equal segments.
            double currentRadian = 0;
            double radianIncrement = Math.PI*2.0 / components.size();
            for (Iterator j = components.iterator(); j.hasNext(); ) {
                Component component = (Component)j.next();
                // Work out offset.
                double widthOffset = component.getWidth()/2.0;
                double heightOffset = component.getHeight()/2.0;
                // Work out point on circumference for degree.
                double x = canvasCentre + (radius.doubleValue() * Math.cos(currentRadian));
                double y = canvasCentre + (radius.doubleValue() * Math.sin(currentRadian));
                // Construct shape around centre point using offsets.
                Shape shape = new Rectangle2D.Double(
                        x-widthOffset, y-heightOffset,
                        component.getWidth(), component.getHeight()
                        );
                // Remember the values in the component->shape maps.
                this.componentToShape.put(component, shape);
                this.shapeToComponent.put(shape, component);
                // Increment for next component.
                currentRadian += radianIncrement;
            }
        }
        // Add relations to component->shape maps using the key shapes for 
        // anchors and offsetting them against the parent component shape.    

        // DO THIS BY:
        
        // iterate through all components in componentToShape
        // iterate through relations on each one
        // for each relation - work out key bounding box, actual bounding box, 
        // then create map of relation -> [pkbox,fkbox]
        
        // iterate through relations found
        // work out for each relation which side of pkbox and fkbox to use
        // create and add to map line2d shapes for each relation. 
    }
    
    /**
     * Get the components for the rings.
     * @return a collection of components to display.
     */
    protected abstract Collection getDisplayComponents();
    
    /**
     * Works out what's at a given point.
     * @param location the point to look at.
     * @return the display component at that point, or null if nothing there.
     */
    private Object getObjectAtLocation(Point location) {
        // There has to be a better way to do this...
        for (Iterator i = this.shapeToComponent.keySet().iterator(); i.hasNext(); ) {
            Shape shape = (Shape)i.next();
            if (shape.contains(location)) return ((Component)this.shapeToComponent.get(shape)).getObject();
        }
        // Nothing found?
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void paintComponent(Graphics g) {
        // Paint background.
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        Graphics2D g2d = (Graphics2D)g.create();
        // Do painting of this table provider.
        // There has to be a better way to do this...
        for (Iterator i = this.shapeToComponent.keySet().iterator(); i.hasNext(); ) {
            Shape shape = (Shape)i.next();
            if (shape.intersects(g2d.getClipBounds())) {
                Component component = (Component)this.shapeToComponent.get(shape);
                this.clearFlags();
                this.getListener().requestObjectFlags(component.getObject());
                component.paint(g2d, shape.getBounds().getLocation(), this.getFlags());
            }
        }
        // Clean up.
        g2d.dispose();
    }
}
