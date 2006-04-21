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
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Displays arbitrary objects linked in a radial form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public class RadialLayout implements LayoutManager {
    /**
     * Constant referring to the padding between tables within a ring.
     */
    public static final double INTRA_PADDING = 36.0; // 72 = 1/2 inch at 72 dpi
    
    /**
     * Constant referring to the padding between rings.
     */
    public static final double INTER_PADDING = 36.0; // 72 = 1/2 inch at 72 dpi
    
    /**
     * List of our ring radii.
     */
    private Map ringRadii = new TreeMap();
    
    /**
     * List of our ring member counts.
     */
    private Map ringSizes = new TreeMap();
    
    /**
     * Our minimum size.
     */
    private int minSize;
    
    /**
     * is our size known?
     */
    private boolean sizeUnknown = true;
    
    /**
     * Map of box components to number of relations.
     */
    private Map componentConstraints = new HashMap();
    
    /**
     * Creates a new instance of ComponentDisplay.
     */
    public RadialLayout() {
        super();
    }
    
    /**
     * {@inheritDoc}
     * <p>Use the constraints to note positioning info.</p>
     */
    public void setConstraints(Component comp, Object constraints) {
        this.componentConstraints.put(comp, constraints);
    }
    
    /**
     * {@inheritDoc}
     * <p>Forget the previous layout.</p>
     */
    public void invalidateLayout(Container target) {
        this.sizeUnknown = true;
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension preferredLayoutSize(Container parent) {
        return this.minimumLayoutSize(parent);
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension minimumLayoutSize(Container parent) {
        Dimension dim = new Dimension(0, 0);
        this.setSizes(parent);
        Insets insets = parent.getInsets();
        dim.width = this.minSize
                + insets.left + insets.right;
        dim.height = this.minSize
                + insets.top + insets.bottom;
        return dim;
    }
    
    /**
     * Work out our canvas and ring sizes.
     */
    private void setSizes(Container parent) {
        this.ringRadii.clear();
        this.ringSizes.clear();
        
        // List of members, Circumference, Max shortest side
        Map ringDetails = new TreeMap();
        
        int nComps = parent.getComponentCount();
        for (int i = 0; i < nComps; i++) {
            Component comp = parent.getComponent(i);
            if (!comp.isVisible()) continue;
            Dimension compSize = comp.getPreferredSize();
            Object constraints = this.componentConstraints.get(comp);
            // use constraints to calculate ring number!
            // for now assume everything is a box and goes in ring 0.
            Integer ringNumber = new Integer(0);
            // then add the object to the appropriate ring and update circumference/max shortest side.
            if (!ringDetails.containsKey(ringNumber))
                ringDetails.put(
                        ringNumber,
                        new Object[]{new ArrayList(), new Integer(0), new Integer(0)}
                );
            Object[] details = (Object[])ringDetails.get(ringNumber);
            List ringMembers = (List)details[0];
            int circumference = ((Integer)details[1]).intValue();
            int maxShortestSide = ((Integer)details[2]).intValue();
            ringMembers.add(comp);
            // increment circumference by longest side, plus padding
            circumference +=
                    (int)Math.max(compSize.getWidth(), compSize.getHeight()) +
                    (int)RadialLayout.INTRA_PADDING;
            // update longest shortest side
            maxShortestSide = Math.max(maxShortestSide, (int)Math.min(compSize.getWidth(), compSize.getHeight()));
            // store details back
            ringDetails.put(ringNumber, new Object[]{ringMembers, new Integer(circumference), new Integer(maxShortestSide)});
        }
        
        // Now compute the radii of the rings.
        List ringNumbers = new ArrayList(ringDetails.keySet());
        Collections.reverse(ringNumbers); // Most-linked in centre, please.
        double previousRadius = 0.0;
        for (Iterator i = ringNumbers.iterator(); i.hasNext(); ) {
            Integer ringNumber = (Integer)i.next();
            Object[] details = (Object[])ringDetails.get(ringNumber);
            List ringMembers = (List)details[0];
            double circumference = (double)((Integer)details[1]).intValue();
            double maxShortestSide = (double)((Integer)details[2]).intValue();
            // Work out radius.
            double radius =
                    (circumference / (2.0*Math.PI)) +
                    (maxShortestSide / 2.0) +
                    RadialLayout.INTER_PADDING +
                    previousRadius;
            this.ringRadii.put(ringNumber, new Double(radius));
            // Work out sizes.
            this.ringSizes.put(ringNumber, new Integer(ringMembers.size()));
            // Bump ourselves up so that the next guy points to the outer edge of ourselves.
            previousRadius =
                    radius +
                    (maxShortestSide / 2.0);
        }
        
        // Work out min/max/preferred sizes.
        this.minSize = (int)(previousRadius + RadialLayout.INTER_PADDING) * 2;
        
        // All done.
        this.sizeUnknown = false;
    }
    
    /**
     * {@inheritDoc}
     * <p>Compute the radial layout, unless the previous one is
     * still valid.</p>
     */
    public void layoutContainer(Container parent) {
        // Set our size first.
        if (this.sizeUnknown) this.setSizes(parent);
        double actualSize = Math.max(this.minSize, Math.min(parent.getWidth(), parent.getHeight()));
        double scalar = actualSize / (double)this.minSize;
        
        // Work out our centre point.
        double centreX = Math.max(actualSize, parent.getWidth())/2.0;
        double centreY = Math.max(actualSize, parent.getHeight())/2.0;
        
        // Keep track of the number of items we've put in each ring so far.
        Map ringCounts = new TreeMap();
        
        // Just iterate through components and add them to the various rings.
        int nComps = parent.getComponentCount();
        for (int i = 0; i < nComps; i++) {
            Component comp = parent.getComponent(i);
            if (!comp.isVisible()) continue;
            Dimension compSize = comp.getPreferredSize();
            Object constraints = this.componentConstraints.get(comp);
            // use constraints to calculate ring number!
            // for now assume everything is a box and goes in ring 0.
            Integer ringNumber = new Integer(0);
            // have we seen this ring before?
            int ringCount = 0;
            if (ringCounts.containsKey(ringNumber)) ringCount = ((Integer)ringCounts.get(ringNumber)).intValue()+1;
            ringCounts.put(ringNumber, new Integer(ringCount));
            // Get radius.
            double radius = scalar * ((Double)this.ringRadii.get(ringNumber)).doubleValue();
            // Work out radian position.
            double radianIncrement = (Math.PI*2.0) / ((Integer)this.ringSizes.get(ringNumber)).intValue();
            double positionRadian = radianIncrement * (double)ringCount;
            // Work out offset.
            double widthOffset = compSize.getWidth()/2.0;
            double heightOffset = compSize.getHeight()/2.0;
            // Work out point on circumference for centre of component.
            double x = centreX + (radius * Math.cos(positionRadian));
            double y = centreY + (radius * Math.sin(positionRadian));
            // Place component around centre point using offsets.
            comp.setBounds((int)(x-widthOffset), (int)(y-heightOffset), (int)compSize.getWidth(), (int)compSize.getHeight());
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
}
