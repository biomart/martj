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

import java.awt.Component;
import java.awt.Container;
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
 * This rather awkward piece of code will be replaced in the final version. It
 * is a placeholder that lays out components, based on how connected they are to
 * other components. The more connected they are, the further to the centre of
 * the diagram they are placed. Components with the same connectedness form a
 * circle around a centre point.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 5th June 2006
 * @since 0.1
 */
public class RadialLayout implements LayoutManager {
	private static final double INTRA_PADDING = 0.0; // 72.0 = 1 inch at 72

	// dpi

	private static final double INTER_PADDING = 10.0; // 72.0 = 1 inch at 72

	// dpi

	private static final int RELATION_STEPSIZE = 10; // 72 = 1 inch at 72 dpi

	private static final int RELATION_TAGSIZE = 10; // 72 = 1 inch at 72 dpi

	private Map ringRadii;

	private Map ringSizes;

	private Map ringNumbers;

	private int minSize;

	private boolean sizeUnknown;

	/**
	 * Sets up some defaults for the layout, ready for use.
	 */
	public RadialLayout() {
		this.ringRadii = new HashMap();
		this.ringSizes = new HashMap();
		this.ringNumbers = new HashMap();
		this.sizeUnknown = true;
	}

	public Dimension preferredLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			// Our preferred size is our minimum size.
			return this.minimumLayoutSize(parent);
		}
	}

	public Dimension minimumLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			// Work out how big we are.
			this.setSizes(parent);

			// Work out our parent's insets.
			Insets insets = parent.getInsets();

			// The minimum size is our size plus our
			// parent's insets size.
			Dimension dim = new Dimension(0, 0);
			dim.width = this.minSize + insets.left + insets.right;
			dim.height = this.minSize + insets.top + insets.bottom;

			// That's it!
			return dim;
		}
	}

	private void setSizes(Container parent) {
		// This method calculates which components go in which rings, and
		// the route that relations should take between them.
		synchronized (parent.getTreeLock()) {
			// Ring radii contains the radii of the various rings of components.
			this.ringRadii.clear();

			// Ring sizes contains the number of components in each ring.
			this.ringSizes.clear();

			// Ring numbers contains the ring number for each component.
			this.ringNumbers.clear();

			// Ring details contains a map where they keys are the ring numbers,
			// and the values are array objects. The array objects contain, in
			// this order, a List of components in that ring, the circumference
			// of the ring, and the maximum shortest side length of any
			// component in the ring.
			Map ringDetails = new TreeMap();

			// How many components do we have?
			int nComps = parent.getComponentCount();

			// Loop through those components.
			for (int i = 0; i < nComps; i++) {
				Component comp = parent.getComponent(i);

				// We're only interested in visible non-RelationComponents at
				// this stage.
				if (!comp.isVisible() || comp instanceof RelationComponent)
					continue;

				// Calculate ring number! If not a TableComponent or
				// SchemaComponent, it's zero.
				Integer ringNumber = new Integer(0);
				if (comp instanceof TableComponent)
					ringNumber = new Integer(((TableComponent) comp)
							.countInternalRelations());
				else if (comp instanceof SchemaComponent)
					ringNumber = new Integer(((SchemaComponent) comp)
							.countExternalRelations());

				// Add the object to the appropriate ring and obtain
				// circumference/max shortest side details for that ring.
				if (!ringDetails.containsKey(ringNumber))
					ringDetails.put(ringNumber, new Object[] { new ArrayList(),
							new Integer(0), new Integer(0) });
				Object[] details = (Object[]) ringDetails.get(ringNumber);
				List ringMembers = (List) details[0];
				int circumference = ((Integer) details[1]).intValue();
				int maxSide = ((Integer) details[2]).intValue();
				ringMembers.add(comp);

				// Increment circumference for this ring by adding the shortest
				// side of the new component to the existing circumference, plus
				// padding
				Dimension compSize = comp.getPreferredSize();
				circumference += (int) Math.min(compSize.getWidth(), compSize
						.getHeight())
						+ (int) RadialLayout.INTRA_PADDING;

				// Update the maximum shortest side seen so far in this ring.
				maxSide = Math.max(maxSide, (int) Math.max(compSize.getWidth(),
						compSize.getHeight()));

				// Store the ring details back.
				ringDetails.put(ringNumber, new Object[] { ringMembers,
						new Integer(circumference), new Integer(maxSide) });

				// Store the ring number for the object.
				this.ringNumbers.put(comp, ringNumber);
			}

			// Now compute the radii of the rings. The keys are sorted, but with
			// the most-linked last, so we need to reverse that before iterating
			// over them.
			List ringNumbers = new ArrayList(ringDetails.keySet());
			Collections.reverse(ringNumbers);

			// Keep track of the radius of the last ring inside the current one.
			double previousRadius = 0.0;

			// Iterate over the rings, most-connected (innermost) first.
			for (Iterator i = ringNumbers.iterator(); i.hasNext();) {
				Integer ringNumber = (Integer) i.next();

				// Decode the details for the ring.
				Object[] details = (Object[]) ringDetails.get(ringNumber);
				List ringMembers = (List) details[0];
				double circumference = (double) ((Integer) details[1])
						.intValue();
				double maxSide = (double) ((Integer) details[2]).intValue();

				// Work out radius, which is based on the usual circumference
				// calculation, plus the maximum shortest side length, plus
				// some padding, plus the radius of the last ring before this
				// one,
				// which must appear fully inside this one, hence the padding.
				double radius = (circumference / (2.0 * Math.PI))
						+ (maxSide / 2.0) + RadialLayout.INTER_PADDING
						+ previousRadius;

				// Store the radius.
				this.ringRadii.put(ringNumber, new Double(radius));

				// Store the number of members in this ring.
				this.ringSizes.put(ringNumber, new Integer(ringMembers.size()));

				// Bump the previous radius up so that the next guy points to
				// the outer edge of ourselves.
				previousRadius = radius + (maxSide / 2.0);
			}

			// Work out min/max/preferred sizes.
			this.minSize = (int) (previousRadius + RadialLayout.INTER_PADDING) * 2;

			// All done.
			this.sizeUnknown = false;
		}
	}

	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			// Calculate our size first using the method above.
			if (this.sizeUnknown)
				this.setSizes(parent);

			// Work out our size, which is the larger of the parent size,
			// or our minimum size.
			double actualSize = Math.max(this.minSize, Math.min(parent
					.getWidth(), parent.getHeight()));

			// Work out the scalar required to fit our entire diagram
			// into the space available.
			double scalar = Math.max(1.0, actualSize / (double) this.minSize);

			// Work out our centre point, which is the centre of the area
			// available to us from our parent.
			double centreX = Math.max(actualSize, parent.getWidth()) / 2.0;
			double centreY = Math.max(actualSize, parent.getHeight()) / 2.0;

			// Keep track of the number of items we've put in each ring so far.
			Map ringCounts = new HashMap();

			// A collection of relations we come across on the way.
			List relationComponents = new ArrayList();

			// Just iterate through components and add them to the various
			// rings.
			int nComps = parent.getComponentCount();
			for (int i = 0; i < nComps; i++) {
				Component comp = parent.getComponent(i);

				// We're only interested in visible non-RelationComponents at
				// this stage.
				// If they're not visible, ignore them. If they're relations,
				// add them to the list for processing later.
				if (!comp.isVisible())
					continue;
				if (comp instanceof RelationComponent) {
					relationComponents.add(comp);
					continue;
				}

				// Look up the ring number!
				Integer ringNumber = (Integer) this.ringNumbers.get(comp);

				// Have we seen this ring before? If not, create a new map key
				// to
				// hold the number of items we have placed in this ring so far.
				// If
				// we have seen it before, look up that number instead,
				// increment
				// it, and store it back.
				int ringCount = 0;
				if (ringCounts.containsKey(ringNumber))
					ringCount = ((Integer) ringCounts.get(ringNumber))
							.intValue() + 1;
				ringCounts.put(ringNumber, new Integer(ringCount));

				// Work out how big this component would like to be.
				Dimension compSize = comp.getPreferredSize();

				// Work out the radius we are using to draw this ring.
				double radius = scalar
						* ((Double) this.ringRadii.get(ringNumber))
								.doubleValue();

				// Work out radian position of component.
				double radianIncrement = (Math.PI * 2.0)
						/ ((Integer) this.ringSizes.get(ringNumber)).intValue();
				double positionRadian = radianIncrement * (double) ringCount;

				// Work out offset from circumference of circle we need to place
				// the component at so that it's centre is on the circumference.
				double widthOffset = compSize.getWidth() / 2.0;
				double heightOffset = compSize.getHeight() / 2.0;

				// Work out point on circumference for centre of component.
				double x = centreX + (radius * Math.cos(positionRadian));
				double y = centreY + (radius * Math.sin(positionRadian));

				// Place component around centre point using offsets.
				comp.setBounds((int) (x - widthOffset),
						(int) (y - heightOffset), (int) compSize.getWidth(),
						(int) compSize.getHeight());

				// Make the component update its children (necessary for laying
				// out relations later).
				comp.validate();
			}

			// Add relations to the diagram by locating each key component the
			// relation refers to, and constructing anchor lines against those
			// and a simple two-part line between each of the anchors.
			for (Iterator i = relationComponents.iterator(); i.hasNext();) {
				RelationComponent relationComponent = (RelationComponent) i
						.next();

				// Obtain first key and work out position relative to
				// ourselves.
				KeyComponent firstKey = relationComponent
						.getFirstKeyComponent();
				Rectangle firstKeyRectangle = firstKey.getBounds();
				Container firstKeyParent = firstKey.getParent();
				while (firstKeyParent != parent) {
					firstKeyRectangle.setLocation(firstKeyRectangle.x
							+ (int) firstKeyParent.getX(),
							firstKeyRectangle.y
									+ (int) firstKeyParent.getY());
					firstKeyParent = firstKeyParent.getParent();
				}

				// Do the same for the second key.
				KeyComponent secondKey = relationComponent
						.getSecondKeyComponent();
				Rectangle secondKeyRectangle = secondKey.getBounds();
				Container secondKeyParent = secondKey.getParent();
				while (secondKeyParent != parent) {
					secondKeyRectangle.setLocation(secondKeyRectangle.x
							+ (int) secondKeyParent.getX(),
							secondKeyRectangle.y
									+ (int) secondKeyParent.getY());
					secondKeyParent = secondKeyParent.getParent();
				}

				// Create a bounding box around the whole lot plus 1 step size
				// each side for mouse-sensitivity's sake.
				int x = Math.min(firstKeyRectangle.x, secondKeyRectangle.x);
				int y = Math.min(firstKeyRectangle.y, secondKeyRectangle.y);
				int width = Math.max(firstKeyRectangle.x
						+ firstKeyRectangle.width, secondKeyRectangle.x
						+ secondKeyRectangle.width)
						- x;
				int height = Math.max(firstKeyRectangle.y
						+ firstKeyRectangle.height, secondKeyRectangle.y
						+ secondKeyRectangle.height)
						- y;
				Rectangle bounds = new Rectangle(x
						- (int) RadialLayout.RELATION_STEPSIZE, y
						- (int) RadialLayout.RELATION_STEPSIZE, width
						+ (int) RadialLayout.RELATION_STEPSIZE, height
						+ (int) RadialLayout.RELATION_STEPSIZE);
				relationComponent.setBounds(bounds);

				// Find midpoint of sides of foreign key and primary key. This
				// is
				// where the anchor lines will attach.
				int middleOfFirst = firstKeyRectangle.x
						+ (firstKeyRectangle.width / 2);
				int middleOfSecond = secondKeyRectangle.x
						+ (secondKeyRectangle.width / 2);
				int midPoint = (middleOfFirst + middleOfSecond) / 2;
				int firstStartX = (midPoint <= middleOfFirst) ? firstKeyRectangle.x
						: firstKeyRectangle.x + firstKeyRectangle.width;
				int secondEndX = (midPoint <= middleOfSecond) ? secondKeyRectangle.x
						: secondKeyRectangle.x + secondKeyRectangle.width;
				int firstY = firstKeyRectangle.y
						+ (firstKeyRectangle.height / 2);
				int secondY = secondKeyRectangle.y
						+ (secondKeyRectangle.height / 2);

				// Modify all the relation coords to be rooted at 0,0 of its
				// bounds.
				firstStartX -= bounds.x;
				firstY -= bounds.y;
				secondEndX -= bounds.x;
				secondY -= bounds.y;

				// Work out the X-offsets of the end of the key anchors that is
				// not attached to the key itself.
				int firstTagX = (firstStartX < middleOfFirst) ? firstStartX
						- RadialLayout.RELATION_TAGSIZE : firstStartX
						+ RadialLayout.RELATION_TAGSIZE;
				int secondTagX = (secondEndX < middleOfSecond) ? secondEndX
						- RadialLayout.RELATION_TAGSIZE : secondEndX
						+ RadialLayout.RELATION_TAGSIZE;

				// Create a path to describe the relation shape.
				GeneralPath path = new GeneralPath();

				// Move to starting point at primary key.
				path.moveTo(firstStartX, firstY);

				// Draw starting anchor tag.
				path.lineTo(firstTagX, firstY);

				// Draw the two-step line between.
				int diffX = secondTagX - firstTagX;
				int diffY = secondY - firstY;
				if (Math.abs(diffX) >= Math.abs(diffY)) {
					// Move X first, then Y
					path.lineTo(secondTagX, firstY);
					path.lineTo(secondTagX, secondY);
				} else {
					// Move Y first, then X
					path.lineTo(firstTagX, secondY);
					path.lineTo(secondTagX, secondY);
				}

				// Draw the closing anchor tag at the foreign key.
				path.lineTo(secondEndX, secondY);

				// Done! Tell the relation what shape we made.
				relationComponent.setShape(path);
			}
		}
	}

	public void addLayoutComponent(String name, Component comp) {
		// Ignore.
	}

	public void removeLayoutComponent(Component comp) {
		// Ignore.
	}
}
