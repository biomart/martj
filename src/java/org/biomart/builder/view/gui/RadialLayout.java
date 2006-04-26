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
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
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
 * @version 0.1.2, 25th April 2006
 * @since 0.1
 */
public class RadialLayout implements LayoutManager {
    /**
     * Constant referring to the padding between tables within a ring.
     */
    public static final double INTRA_PADDING = 0.0; // 72.0 = 1 inch at 72 dpi
    
    /**
     * Constant referring to the padding between rings.
     */
    public static final double INTER_PADDING = 10.0; // 72.0 = 1 inch at 72 dpi
    
    /**
     * Constant referring to the length of each sector of a relation.
     */
    public static final int RELATION_STEPSIZE = 10; // 72 = 1 inch at 72 dpi
    
    /**
     * Constant referring to the length of the horizontal first and last sector of a relation.
     */
    public static final int RELATION_TAGSIZE = 10; // 72 = 1 inch at 72 dpi
    
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
     * Is our size known?
     */
    private boolean sizeUnknown = true;
    
    /**
     * How many components do we have?
     */
    private int componentCount = 0;
    
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
        synchronized (target.getTreeLock()) {
            this.sizeUnknown = true;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            return this.minimumLayoutSize(parent);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            this.setSizes(parent);
            Insets insets = parent.getInsets();
            dim.width = this.minSize
                    + insets.left + insets.right;
            dim.height = this.minSize
                    + insets.top + insets.bottom;
            return dim;
        }
    }
    
    /**
     * Work out our canvas and ring sizes.
     */
    private void setSizes(Container parent) {
        synchronized (parent.getTreeLock()) {
            this.ringRadii.clear();
            this.ringSizes.clear();
            
            // List of members, Circumference, Max shortest side
            Map ringDetails = new TreeMap();
            
            int nComps = parent.getComponentCount();
            this.componentCount = nComps;
            for (int i = 0; i < nComps; i++) {
                Component comp = parent.getComponent(i);
                // We're only interested in visible non-RelationComponents at this stage.
                if (!comp.isVisible() || comp instanceof RelationDiagramComponent) continue;
                // Calculate ring number! If not a TableDiagramComponent or TableProviderComponent, it's zero.
                Integer ringNumber = new Integer(0);
                if (comp instanceof TableDiagramComponent) ringNumber = new Integer(((TableDiagramComponent)comp).countRelations());
                else if (comp instanceof TableProviderDiagramComponent) ringNumber = new Integer(((TableProviderDiagramComponent)comp).countExternalRelations());
                // then add the object to the appropriate ring and update circumference/max side.
                if (!ringDetails.containsKey(ringNumber))
                    ringDetails.put(
                            ringNumber,
                            new Object[]{new ArrayList(), new Integer(0), new Integer(0)}
                    );
                Object[] details = (Object[])ringDetails.get(ringNumber);
                List ringMembers = (List)details[0];
                int circumference = ((Integer)details[1]).intValue();
                int maxSide = ((Integer)details[2]).intValue();
                ringMembers.add(comp);
                // increment circumference by shortest side, plus padding
                Dimension compSize = comp.getPreferredSize();
                circumference +=
                        (int)Math.min(compSize.getWidth(), compSize.getHeight()) +
                        (int)RadialLayout.INTRA_PADDING;
                // update longest side
                maxSide = Math.max(maxSide, (int)Math.max(compSize.getWidth(), compSize.getHeight()));
                // store details back
                ringDetails.put(ringNumber, new Object[]{ringMembers, new Integer(circumference), new Integer(maxSide)});
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
                double maxSide = (double)((Integer)details[2]).intValue();
                // Work out radius.
                double radius =
                        (circumference / (2.0*Math.PI)) +
                        (maxSide / 2.0) +
                        RadialLayout.INTER_PADDING +
                        previousRadius;
                this.ringRadii.put(ringNumber, new Double(radius));
                // Work out sizes.
                this.ringSizes.put(ringNumber, new Integer(ringMembers.size()));
                // Bump ourselves up so that the next guy points to the outer edge of ourselves.
                previousRadius =
                        radius +
                        (maxSide / 2.0);
            }
            
            // Work out min/max/preferred sizes.
            this.minSize = (int)(previousRadius + RadialLayout.INTER_PADDING) * 2;
            
            // All done.
            this.sizeUnknown = false;
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>Compute the radial layout, unless the previous one is
     * still valid.</p>
     */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            // Calculate our size first.
            if (this.sizeUnknown || this.componentCount != parent.getComponentCount()) this.setSizes(parent);
            
            // Set our size.
            double actualSize = Math.max(this.minSize, Math.min(parent.getWidth(), parent.getHeight()));
            double scalar = actualSize / (double)this.minSize;
            
            // Work out our centre point.
            double centreX = Math.max(actualSize, parent.getWidth())/2.0;
            double centreY = Math.max(actualSize, parent.getHeight())/2.0;
            
            // Keep track of the number of items we've put in each ring so far.
            Map ringCounts = new HashMap();
            
            // A set of relations we come across.
            List relationComponents = new ArrayList();
            
            // Just iterate through components and add them to the various rings.
            int nComps = parent.getComponentCount();
            for (int i = 0; i < nComps; i++) {
                Component comp = parent.getComponent(i);
                // We're only interested in visible non-RelationComponents at this stage.
                if (!comp.isVisible()) continue;
                if (comp instanceof RelationDiagramComponent) {
                    relationComponents.add(comp);
                    continue;
                }
                // Calculate ring number! If not a TableDiagramComponent or TableProviderComponent, it's zero.
                Integer ringNumber = new Integer(0);
                if (comp instanceof TableDiagramComponent) ringNumber = new Integer(((TableDiagramComponent)comp).countRelations());
                else if (comp instanceof TableProviderDiagramComponent) ringNumber = new Integer(((TableProviderDiagramComponent)comp).countExternalRelations());
                // have we seen this ring before?
                int ringCount = 0;
                if (ringCounts.containsKey(ringNumber)) ringCount = ((Integer)ringCounts.get(ringNumber)).intValue()+1;
                ringCounts.put(ringNumber, new Integer(ringCount));
                // Get radius.
                Dimension compSize = comp.getPreferredSize();
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
            for (Iterator i = relationComponents.iterator(); i.hasNext(); ) {
                RelationDiagramComponent relationComponent = (RelationDiagramComponent)i.next();
                KeyDiagramComponent primaryKey = relationComponent.getPrimaryKeyComponent();
                KeyDiagramComponent foreignKey = relationComponent.getForeignKeyComponent();
                BoxShapedDiagramComponent primaryKeyTable = primaryKey.getParentComponent();
                BoxShapedDiagramComponent foreignKeyTable = foreignKey.getParentComponent();
                
                // Force the inner tables to lay themselves out correctly.
                if (!primaryKeyTable.isValid()) primaryKeyTable.validate();
                if (!foreignKeyTable.isValid()) foreignKeyTable.validate();
                
                // Work out locations of primary and foreign key boxes.
                Rectangle primaryKeyRectangle = primaryKey.getBounds();
                primaryKeyRectangle.setLocation(
                        primaryKeyRectangle.x + primaryKeyTable.getX(),
                        primaryKeyRectangle.y + primaryKeyTable.getY()
                        );
                Rectangle foreignKeyRectangle = foreignKey.getBounds();
                foreignKeyRectangle.setLocation(
                        foreignKeyRectangle.x + foreignKeyTable.getX(),
                        foreignKeyRectangle.y + foreignKeyTable.getY()
                        );
                
                // Create a bounding box around the whole lot plus 1 step size each side.
                int x = Math.min(primaryKeyRectangle.x, foreignKeyRectangle.x);
                int y = Math.min(primaryKeyRectangle.y, foreignKeyRectangle.y);
                int width = Math.max(primaryKeyRectangle.x + primaryKeyRectangle.width,
                        foreignKeyRectangle.x + foreignKeyRectangle.width) -
                        x;
                int height = Math.max(
                        primaryKeyRectangle.y + primaryKeyRectangle.height,
                        foreignKeyRectangle.y + foreignKeyRectangle.height) -
                        y;
                Rectangle bounds = new Rectangle(
                        x - (int)RadialLayout.RELATION_STEPSIZE,
                        y - (int)RadialLayout.RELATION_STEPSIZE,
                        width + (int)RadialLayout.RELATION_STEPSIZE,
                        height + (int)RadialLayout.RELATION_STEPSIZE
                        );
                relationComponent.setBounds(bounds);
                
                // Find side of foreign key and side of primary key to join.
                int middleOfPK = primaryKeyRectangle.x + (primaryKeyRectangle.width / 2);
                int middleOfFK = foreignKeyRectangle.x + (foreignKeyRectangle.width / 2);
                int midPoint = (middleOfPK + middleOfFK) / 2;
                int pkStartX =
                        (midPoint <= middleOfPK) ?
                            primaryKeyRectangle.x :
                            primaryKeyRectangle.x + primaryKeyRectangle.width;
                int fkEndX =
                        (midPoint <= middleOfFK) ?
                            foreignKeyRectangle.x :
                            foreignKeyRectangle.x + foreignKeyRectangle.width;
                int pkY = primaryKeyRectangle.y + (primaryKeyRectangle.height / 2);
                int fkY = foreignKeyRectangle.y + (foreignKeyRectangle.height / 2);
                
                // Modify all the relation coords to be rooted at 0,0 of its bounds.
                pkStartX -= bounds.x;
                pkY -= bounds.y;
                fkEndX -= bounds.x;
                fkY -= bounds.y;
                int pkTagX =
                        (pkStartX < middleOfPK) ?
                            pkStartX - RadialLayout.RELATION_TAGSIZE :
                            pkStartX + RadialLayout.RELATION_TAGSIZE;
                int fkTagX =
                        (fkEndX < middleOfFK) ?
                            fkEndX - RadialLayout.RELATION_TAGSIZE :
                            fkEndX + RadialLayout.RELATION_TAGSIZE;
                
                // Create and add to relation the line2d shape which links mid-right
                // of the primary key to mid-left of the foreign key.
                GeneralPath path = new GeneralPath();
                // Move to starting point.
                path.moveTo(pkStartX, pkY);
                // Draw starting tag.
                path.lineTo(pkTagX, pkY);
                // Draw the rest.
                int diffX = fkTagX - pkTagX;
                int diffY = fkY - pkY;
                if (Math.abs(diffX) >= Math.abs(diffY)) {
                    // Move X first, then Y
                    path.lineTo(fkTagX, pkY);
                    path.lineTo(fkTagX, fkY);
                } else {
                    // Move Y first, then X
                    path.lineTo(pkTagX, fkY);
                    path.lineTo(fkTagX, fkY);
                }
                // Draw the closing tag.
                path.lineTo(fkEndX, fkY);
                // Done!
                relationComponent.setShape(path);
            }
        }
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
