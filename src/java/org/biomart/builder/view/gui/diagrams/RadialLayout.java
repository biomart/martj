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

package org.biomart.builder.view.gui.diagrams;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.SchemaComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;

/**
 * This layout manager lays out components based on how connected they are to
 * other components. The more connected they are, the further to the centre of
 * the diagram they are placed. Components with the same connectedness are
 * placed in a ring around a centre point.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class RadialLayout implements LayoutManager {
	// This is the space between rings.
	private static final double INTER_PADDING = 10.0; // 72.0 = 1 inch

	// This is the space along the circumference between components in the same
	// ring.
	private static final double INTRA_PADDING = 0.0; // 72.0 = 1 inch

	private static final int RELATION_TAGSIZE = 10; // 72 = 1 inch at 72 dpi

	private int minSize;

	private Map ringNumbers;

	private Map ringRadii;

	private Map ringSizes;

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

	private void setSizes(final Container parent) {
		// This method calculates which components go in which rings.
		synchronized (parent.getTreeLock()) {
			// Ring radii contains the radii of the various rings of components.
			this.ringRadii.clear();

			// Ring sizes contains the number of components in each ring.
			this.ringSizes.clear();

			// Ring numbers contains the ring number for each component.
			// This is an arbitrary sequence of numbers, representing the
			// unique set of connectedness values - ie. the unique set of
			// responses
			// from countRelations on the various database objects represented.
			this.ringNumbers.clear();

			// Ring details contains a map where the keys are the ring numbers,
			// and the values are array objects. The array objects contain, in
			// this order, a List of components in that ring, the circumference
			// of the ring, and the maximum shortest side length of any
			// component in the ring.
			final Map ringDetails = new TreeMap();

			// How many components do we have?
			final int nComps = parent.getComponentCount();

			// Loop through those components.
			for (int i = 0; i < nComps; i++) {
				final Component comp = parent.getComponent(i);

				// We're only interested in visible non-RelationComponents at
				// this stage.
				if (!comp.isVisible() || comp instanceof RelationComponent)
					continue;

				// Calculate ring number! If not a TableComponent or
				// SchemaComponent, it's zero.
				Integer ringNumber = new Integer(0);
				if (comp instanceof TableComponent) {
					final Collection rels = new HashSet();
					for (final Iterator j = ((Table) ((TableComponent) comp)
							.getObject()).getRelations().iterator(); j
							.hasNext();) {
						final Relation rel = (Relation) j.next();
						if (!rel.isExternal())
							rels.add(rel);
					}
					ringNumber = new Integer(rels.size());
				} else if (comp instanceof SchemaComponent) {
					final Collection rels = new HashSet();
					for (final Iterator j = ((Schema) ((SchemaComponent) comp)
							.getObject()).getRelations().iterator(); j
							.hasNext();) {
						final Relation rel = (Relation) j.next();
						if (rel.isExternal())
							rels.add(rel);
					}
					ringNumber = new Integer(rels.size());
				}

				// Create the ring for this ring number if it doesn't already
				// exist.
				if (!ringDetails.containsKey(ringNumber))
					ringDetails.put(ringNumber, new Object[] { new ArrayList(),
							new Integer(0), new Integer(0) });

				// Get the details for the ring.
				final Object[] details = (Object[]) ringDetails.get(ringNumber);

				// Add the object to the ring.
				final List ringMembers = (List) details[0];
				ringMembers.add(comp);

				// Obtain circumference/max shortest side details for that ring.
				int circumference = ((Integer) details[1]).intValue();
				int maxSide = ((Integer) details[2]).intValue();

				// Increment circumference for this ring by adding the shortest
				// side of the new component to the existing circumference, plus
				// padding
				final Dimension compSize = comp.getPreferredSize();
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
			final List ringNumbers = new ArrayList(ringDetails.keySet());
			Collections.reverse(ringNumbers);

			// Keep track of the radius of the previous ring inside the current
			// one.
			double previousRadius = 0.0;

			// Iterate over the rings, most-connected (innermost) first.
			for (final Iterator i = ringNumbers.iterator(); i.hasNext();) {
				final Integer ringNumber = (Integer) i.next();

				// Decode the details for the ring.
				final Object[] details = (Object[]) ringDetails.get(ringNumber);
				final List ringMembers = (List) details[0];
				final double circumference = ((Integer) details[1]).intValue();
				final double maxSide = ((Integer) details[2]).intValue();

				// Work out radius, which is based on the usual circumference
				// calculation, plus the maximum shortest side length, plus
				// some padding, plus the radius of the last ring before this
				// one, which must appear fully inside this one, hence the
				// padding.
				final double radius = circumference / (2.0 * Math.PI) + maxSide
						/ 2.0 + RadialLayout.INTER_PADDING + previousRadius;

				// Store the radius.
				this.ringRadii.put(ringNumber, new Double(radius));

				// Store the number of members in this ring.
				this.ringSizes.put(ringNumber, new Integer(ringMembers.size()));

				// Bump the previous radius up so that the next guy points to
				// the outer edge of ourselves.
				previousRadius = radius + maxSide / 2.0;
			}

			// Work out min/max/preferred sizes for the layout so that
			// the largest ring fits nicely.
			this.minSize = (int) (previousRadius + RadialLayout.INTER_PADDING) * 2;

			// All done.
			this.sizeUnknown = false;
		}
	}

	public void addLayoutComponent(final String name, final Component comp) {
		// Ignore.
	}

	public void layoutContainer(final Container parent) {
		synchronized (parent.getTreeLock()) {
			// Calculate our size first using the method above.
			if (this.sizeUnknown)
				this.setSizes(parent);

			// Work out our size, which is the larger of the parent size,
			// or our minimum size.
			final double actualSize = Math.max(this.minSize, Math.min(parent
					.getWidth(), parent.getHeight()));

			// Work out the scalar required to fit our entire diagram
			// into the space available.
			final double scalar = Math.max(1.0, actualSize / this.minSize);

			// Work out our centre point, which is the centre of the area
			// available to us from our parent.
			final double centreX = Math.max(actualSize, parent.getWidth()) / 2.0;
			final double centreY = Math.max(actualSize, parent.getHeight()) / 2.0;

			// Keep track of the number of items we've put in each ring so far.
			final Map ringCounts = new HashMap();

			// A collection of relations we come across on the way.
			final Collection relationComponents = new HashSet();

			// Just iterate through components and add them to the various
			// rings.
			final int nComps = parent.getComponentCount();
			for (int i = 0; i < nComps; i++) {
				final Component comp = parent.getComponent(i);

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
				final Integer ringNumber = (Integer) this.ringNumbers.get(comp);

				// Have we seen this ring before? If not, create a new map key
				// to hold the number of items we have placed in this ring so
				// far. If we have seen it before, look up that number
				// instead, increment it, and store it back.
				int ringCount = 0;
				if (ringCounts.containsKey(ringNumber))
					ringCount = ((Integer) ringCounts.get(ringNumber))
							.intValue() + 1;
				ringCounts.put(ringNumber, new Integer(ringCount));

				// Work out how big this component would like to be.
				final Dimension compSize = comp.getPreferredSize();

				// Work out the radius we are using to draw this ring.
				final double radius = scalar
						* ((Double) this.ringRadii.get(ringNumber))
								.doubleValue();

				// Work out radian position of component.
				final double radianIncrement = Math.PI * 2.0
						/ ((Integer) this.ringSizes.get(ringNumber)).intValue();
				final double positionRadian = radianIncrement * ringCount;

				// Work out offset from circumference of circle we need to place
				// the component at so that it's centre is on the circumference.
				final double widthOffset = compSize.getWidth() / 2.0;
				final double heightOffset = compSize.getHeight() / 2.0;

				// Work out point on circumference for centre of component.
				final double x = centreX + radius * Math.cos(positionRadian);
				final double y = centreY + radius * Math.sin(positionRadian);

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
			for (final Iterator i = relationComponents.iterator(); i.hasNext();) {
				final RelationComponent relationComponent = (RelationComponent) i
						.next();

				// Obtain first key and work out position relative to
				// diagram.
				final KeyComponent firstKey = relationComponent
						.getFirstKeyComponent();
				final Rectangle firstKeyRectangle = firstKey.getBounds();
				Container keyParent = firstKey.getParent();
				while (keyParent != parent) {
					firstKeyRectangle.setLocation(firstKeyRectangle.x
							+ keyParent.getX(), firstKeyRectangle.y
							+ keyParent.getY());
					keyParent = keyParent.getParent();
				}

				// Do the same for the second key.
				final KeyComponent secondKey = relationComponent
						.getSecondKeyComponent();
				final Rectangle secondKeyRectangle = secondKey.getBounds();
				keyParent = secondKey.getParent();
				while (keyParent != parent) {
					secondKeyRectangle.setLocation(secondKeyRectangle.x
							+ keyParent.getX(), secondKeyRectangle.y
							+ keyParent.getY());
					keyParent = keyParent.getParent();
				}

				// Find midpoints of both keys.
				final Point firstKeyMidpoint = new Point(firstKeyRectangle.x
						+ firstKeyRectangle.width / 2, firstKeyRectangle.y
						+ firstKeyRectangle.height / 2);
				final Point secondKeyMidpoint = new Point(secondKeyRectangle.x
						+ secondKeyRectangle.width / 2, secondKeyRectangle.y
						+ secondKeyRectangle.height / 2);

				// Find average x-coord and y-coord of both midpoints.
				int centreLineX = (firstKeyMidpoint.x + secondKeyMidpoint.x) / 2;
				int centreLineY = (firstKeyMidpoint.y + secondKeyMidpoint.y) / 2;

				// Find start for first key, and tag position. Start
				// will be the end of the key closest to the average
				// coordinates just calculated.
				int firstY = firstKeyMidpoint.y;
				int firstX;
				int firstTagX;
				if (Math.abs(firstKeyRectangle.x - centreLineX) < Math
						.abs(firstKeyRectangle.x + firstKeyRectangle.width
								- centreLineX)) {
					firstX = firstKeyRectangle.x;
					firstTagX = firstX - RadialLayout.RELATION_TAGSIZE;
				} else {
					firstX = firstKeyRectangle.x + firstKeyRectangle.width;
					firstTagX = firstX + RadialLayout.RELATION_TAGSIZE;
				}

				// Find end for second key, and tag position. Start will
				// be the end of the key closest to the average coordinates
				// previously calculated.
				int secondY = secondKeyMidpoint.y;
				int secondX;
				int secondTagX;
				if (Math.abs(secondKeyRectangle.x - centreLineX) < Math
						.abs(secondKeyRectangle.x + secondKeyRectangle.width
								- centreLineX)) {
					secondX = secondKeyRectangle.x;
					secondTagX = secondX - RadialLayout.RELATION_TAGSIZE;
				} else {
					secondX = secondKeyRectangle.x + secondKeyRectangle.width;
					secondTagX = secondX + RadialLayout.RELATION_TAGSIZE;
				}

				// Create a bounding box containing both keys plus 1 step
				// size each side for mouse-sensitivity's sake.
				final int x = Math.min(firstX, secondX);
				final int y = Math.min(firstY, secondY);
				final int width = Math.max(firstX + firstKeyRectangle.width,
						secondX + secondKeyRectangle.width)
						- x;
				final int height = Math.max(firstY + firstKeyRectangle.height,
						secondY + secondKeyRectangle.height)
						- y;
				final Rectangle bounds = new Rectangle(x
						- RadialLayout.RELATION_TAGSIZE, y
						- RadialLayout.RELATION_TAGSIZE, width
						+ RadialLayout.RELATION_TAGSIZE, height
						+ RadialLayout.RELATION_TAGSIZE);
				relationComponent.setBounds(bounds);

				// Modify all the relation coords to be rooted at 0,0 of its
				// bounds.
				centreLineX -= bounds.x;
				centreLineY -= bounds.y;
				firstX -= bounds.x;
				firstTagX -= bounds.x;
				firstY -= bounds.y;
				secondX -= bounds.x;
				secondTagX -= bounds.x;
				secondY -= bounds.y;

				// Create a path to describe the relation shape. It
				// has 6 elements - move, across, down/up, across,
				// down/up, across.
				final GeneralPath path = new GeneralPath(
						GeneralPath.WIND_EVEN_ODD, 6);

				// Move to starting point at primary key.
				path.moveTo(firstX, firstY);

				// Draw starting anchor tag.
				path.lineTo(firstTagX, firstY);

				// Draw the three-step line between.
				path.lineTo(firstTagX, centreLineY);
				path.lineTo(secondTagX, centreLineY);
				path.lineTo(secondTagX, secondY);

				// Draw the closing anchor tag at the foreign key.
				path.lineTo(secondX, secondY);

				// Done! Tell the relation what shape we made.
				relationComponent.setLineShape(path);
			}
		}
	}

	public Dimension minimumLayoutSize(final Container parent) {
		synchronized (parent.getTreeLock()) {
			// Work out how big we are.
			this.setSizes(parent);

			// Work out our parent's insets.
			final Insets insets = parent.getInsets();

			// The minimum size is our size plus our
			// parent's insets size.
			final Dimension dim = new Dimension(0, 0);
			dim.width = this.minSize + insets.left + insets.right;
			dim.height = this.minSize + insets.top + insets.bottom;

			// That's it!
			return dim;
		}
	}

	public Dimension preferredLayoutSize(final Container parent) {
		synchronized (parent.getTreeLock()) {
			// Our preferred size is our minimum size.
			return this.minimumLayoutSize(parent);
		}
	}

	public void removeLayoutComponent(final Component comp) {
		// Ignore.
	}
}
