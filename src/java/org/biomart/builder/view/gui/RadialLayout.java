/*
 * RadialLayout.java
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

import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager2;

/**
 * Displays arbitrary objects linked in a radial form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public class RadialLayout implements LayoutManager2 {
    /**
     * Constant referring to the padding between rings.
     */
    public static final double PADDING_RADIUS = 36.0; // 72 = 1/2 inch at 72 dpi
    
    /**
     * Creates a new instance of ComponentDisplay.
     */
    public RadialLayout() {
        super();
    }
    
    /**
     * {@inheritDoc}
     * <p>Forget the previous layout.</p>
     */
    public void invalidateLayout(Container target) {
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(400,400);
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(400,400);
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(400,400);
    }
    
    /**
     * {@inheritDoc}
     * <p>Compute the radial layout, unless the previous one is
     * still valid.</p>
     */
    public void layoutContainer(Container parent) {
        /*
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
        // Construct the rings, innermost first, leaving some padding in the middle.
        double innerRingRadius = ComponentDisplay.PADDING_RADIUS;
        for (Iterator i = ringNumbers.iterator(); i.hasNext(); ) {
            List components = (List)ringFamilies.get(i.next());
            int median = components.size() / 2;
            if (components.size() % 2 != 0) median++;
            // Order them by width and find the minimum radius.
            Collections.sort(components, new DescendingWidth());
            double minWidthRadius = 0.0;
            for (int j = 0; j < median; j++) minWidthRadius += ((Component)components.get(j)).getWidth();
            minWidthRadius /= 2.0; // convert from diameter to radius.
            double minWidthPadding = ((Component)components.get(0)).getWidth() / 2.0;
            // Order them by height and find the minimum radius.
            Collections.sort(components, new DescendingHeight());
            double minHeightRadius = 0.0;
            for (int j = 0; j < median; j++) minHeightRadius += ((Component)components.get(j)).getHeight();
            minHeightRadius /= 2.0; // convert from diameter to radius.
            double minHeightPadding = ((Component)components.get(0)).getHeight() / 2.0;
            // Work out the actual minimum radius.
            double minRadius = Math.max(minHeightRadius, minWidthRadius);
            double minPadding = Math.max(minWidthPadding, minHeightPadding);
            minRadius += innerRingRadius + minPadding;
            // Plant them randomly around the ring.
            Collections.shuffle(components);
            this.rings.put(new Double(innerRingRadius), components);
            // Set the inner radius for the next ring.
            innerRingRadius = minRadius + minPadding + ComponentDisplay.PADDING_RADIUS;
        }
        // Work out the size of the largest ring plus a bit of padding, and set it as our own size.
        int canvasCentre = (int)(innerRingRadius + ComponentDisplay.PADDING_RADIUS);
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
        
        // Finally, repaint ourselves.
        this.repaint();
         */
    }
    
    /**
     * {@inheritDoc}
     * <p>Use the constraints to note positioning info about relations.</p>
     */
    public void addLayoutComponent(Component comp, Object constraints) {
    }
    
    /**
     * {@inheritDoc}
     */
    public void addLayoutComponent(String name, Component comp) {
        // Ignore.
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeLayoutComponent(Component comp) {
        // Ignore.
    }
    
    /**
     * {@inheritDoc}
     */
    public float getLayoutAlignmentX(Container target) {
        return 0.0f;
    }
    
    /**
     * {@inheritDoc}
     */
    public float getLayoutAlignmentY(Container target) {
        return 0.0f;
    }
}
