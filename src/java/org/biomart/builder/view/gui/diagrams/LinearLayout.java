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
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.SchemaComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * This is a layout manager that lays out components in two rows and runs
 * relations between them along fixed-space tracks between components.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 21st July 2006
 * @since 0.1
 */
public class LinearLayout implements LayoutManager {
	private static final double RELATION_SPACING = 5.0; // 72.0 = 1 inch

	private static final double TABLE_PADDING = 10.0; // 72.0 = 1 inch

	private int minWidth;

	private int minHeight;

	private boolean sizeUnknown;

	private List topRow = new ArrayList();

	private List bottomRow = new ArrayList();

	private List relations = new ArrayList();

	private double topRowHeight;

	private double bottomRowHeight;

	/**
	 * Sets up some defaults for the layout, ready for use.
	 */
	public LinearLayout() {
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
			dim.width = this.minWidth + insets.left + insets.right;
			dim.height = this.minHeight + insets.top + insets.bottom;

			// That's it!
			return dim;
		}
	}

	private void setSizes(Container parent) {
		// This method works out how big each component should be, and
		// then works out in total how much space to allocate to the diagram.
		synchronized (parent.getTreeLock()) {
			// Reset the lists that define the top and bottom rows.
			this.topRow.clear();
			this.bottomRow.clear();

			// Reset the list that holds all the relations to display.
			this.relations.clear();

			// Make one big list to hold them all for now.
			List bothRows = new ArrayList();

			// Reset the heights of the top and bottom rows.
			this.topRowHeight = 0.0;
			this.bottomRowHeight = 0.0;

			// How many components do we have?
			int nComps = parent.getComponentCount();

			// Loop through those components.
			for (int i = 0; i < nComps; i++) {
				Component comp = parent.getComponent(i);

				// We're only interested in visible non-RelationComponents at
				// this stage.
				if (!comp.isVisible())
					continue;
				if (comp instanceof RelationComponent) {
					this.relations.add(comp);
					continue;
				}

				// Add the component to the list to be split into rows.
				bothRows.add(comp);
			}

			// Split the row into top and bottom halves.
			int splitIndex = bothRows.size() / 2;
			this.topRow.addAll(bothRows.subList(0, splitIndex));
			this.bottomRow
					.addAll(bothRows.subList(splitIndex, bothRows.size()));

			// Set up variables to hold the top and bottom row widths.
			// Start both off with padding to the left.
			double topRowWidth = LinearLayout.TABLE_PADDING;
			double bottomRowWidth = LinearLayout.TABLE_PADDING;

			// Work out the heights and widths for top row.
			for (Iterator i = this.topRow.iterator(); i.hasNext();) {
				// Get the component.
				Component comp = (Component) i.next();

				// Work out how many relations lead off this component.
				// If not a TableComponent or SchemaComponent, it's zero.
				int relationCount = 0;
				if (comp instanceof TableComponent)
					relationCount = ((TableComponent) comp)
							.countInternalRelations();
				else if (comp instanceof SchemaComponent)
					relationCount = ((SchemaComponent) comp)
							.countExternalRelations();

				// How big is this component?
				Dimension compSize = comp.getPreferredSize();

				// Work out the maximum height for the top row.
				this.topRowHeight = Math.max(this.topRowHeight, compSize
						.getHeight());

				// Add up the width for this row, including padding
				// and space for relations. Double padding = padding before
				// and after relations segment.
				topRowWidth += compSize.getWidth()
						+ (2.0 * LinearLayout.TABLE_PADDING);
				topRowWidth += ((double) relationCount)
						* LinearLayout.RELATION_SPACING;
			}

			// Do the same for the bottom row.
			for (Iterator i = this.bottomRow.iterator(); i.hasNext();) {
				// Get the component.
				Component comp = (Component) i.next();

				// Work out how many relations lead off this component.
				// If not a TableComponent or SchemaComponent, it's zero.
				int relationCount = 0;
				if (comp instanceof TableComponent)
					relationCount = ((TableComponent) comp)
							.countInternalRelations();
				else if (comp instanceof SchemaComponent)
					relationCount = ((SchemaComponent) comp)
							.countExternalRelations();

				// How big is this component?
				Dimension compSize = comp.getPreferredSize();

				// Work out the maximum height for the top row.
				this.bottomRowHeight = Math.max(this.bottomRowHeight, compSize
						.getHeight());

				// Add up the width for this row, including padding
				// and space for relations. Double padding = padding before
				// and after relations segment.
				bottomRowWidth += compSize.getWidth()
						+ (2.0 * LinearLayout.TABLE_PADDING);
				bottomRowWidth += ((double) relationCount)
						* LinearLayout.RELATION_SPACING;
			}

			// The width of this diagram is equal to the maximum width of
			// the top and bottom rows.
			this.minWidth = (int) Math.max(topRowWidth, bottomRowWidth);

			// The height of this diagram is equal to the sum of the heights
			// of the top and bottom rows, plus four times table padding
			// (above and below each row), plus space for all relations.
			this.minHeight = (int) (this.topRowHeight + this.bottomRowHeight
					+ (4.0 * LinearLayout.TABLE_PADDING) + (((double) this.relations
					.size()) * LinearLayout.RELATION_SPACING));

			// All done.
			this.sizeUnknown = false;
		}
	}

	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			// Calculate our size first using the method above.
			if (this.sizeUnknown)
				this.setSizes(parent);

			// A map is kept for each table indicating which vertical track for
			// that table the next relation linking to it must go on.
			Map nextVerticalTrackX = new HashMap();

			// Lay out the top row of tables.
			double nextX = LinearLayout.TABLE_PADDING;
			double nextY = this.topRowHeight + LinearLayout.TABLE_PADDING;
			for (Iterator i = this.topRow.iterator(); i.hasNext();) {
				Component comp = (Component) i.next();

				// Work out how big this component would like to be.
				Dimension compSize = comp.getPreferredSize();

				// Place component using offsets.
				comp.setBounds((int) nextX,
						(int) (nextY - compSize.getHeight()), (int) compSize
								.getWidth(), (int) compSize.getHeight());

				// Move across table then pad to the right.
				nextX += compSize.getWidth() + LinearLayout.TABLE_PADDING;

				// Specify where the first relation will go.
				nextVerticalTrackX.put(comp, new Double(nextX));

				// Work out how many relations lead off this component.
				// If not a TableComponent or SchemaComponent, it's zero.
				int relationCount = 0;
				if (comp instanceof TableComponent)
					relationCount = ((TableComponent) comp)
							.countInternalRelations();
				else if (comp instanceof SchemaComponent)
					relationCount = ((SchemaComponent) comp)
							.countExternalRelations();

				// Leave space for the vertical relations.
				nextX += ((double) relationCount)
						* LinearLayout.RELATION_SPACING;

				// Make the component update its children (necessary for laying
				// out relations later).
				comp.validate();

				// Move on to the next table.
				nextX += LinearLayout.TABLE_PADDING;
			}

			// Lay out the bottom row.
			nextX = LinearLayout.TABLE_PADDING;
			nextY = this.topRowHeight
					+ (3.0 * LinearLayout.TABLE_PADDING)
					+ (((double) this.relations.size()) * LinearLayout.RELATION_SPACING);
			for (Iterator i = this.bottomRow.iterator(); i.hasNext();) {
				Component comp = (Component) i.next();

				// Work out how big this component would like to be.
				Dimension compSize = comp.getPreferredSize();

				// Place component using offsets.
				comp.setBounds((int) nextX, (int) nextY, (int) compSize
						.getWidth(), (int) compSize.getHeight());

				// Move across table then pad to the right.
				nextX += compSize.getWidth() + LinearLayout.TABLE_PADDING;

				// Specify where the first relation will go.
				nextVerticalTrackX.put(comp, new Double(nextX));

				// Work out how many relations lead off this component.
				// If not a TableComponent or SchemaComponent, it's zero.
				int relationCount = 0;
				if (comp instanceof TableComponent)
					relationCount = ((TableComponent) comp)
							.countInternalRelations();
				else if (comp instanceof SchemaComponent)
					relationCount = ((SchemaComponent) comp)
							.countExternalRelations();

				// Leave space for the vertical relations.
				nextX += ((double) relationCount)
						* LinearLayout.RELATION_SPACING;

				// Make the component update its children (necessary for laying
				// out relations later).
				comp.validate();

				// Move on to the next table.
				nextX += LinearLayout.TABLE_PADDING;
			}

			// Lay out the relations. The first relation goes on the first
			// horizontal track, the second on the second, etc.
			double nextHorizontalY = (2.0 * LinearLayout.TABLE_PADDING)
					+ this.topRowHeight;
			for (Iterator i = this.relations.iterator(); i.hasNext();) {
				RelationComponent comp = (RelationComponent) i.next();

				// Obtain first key and work out position relative to
				// diagram.
				KeyComponent firstKey = comp.getFirstKeyComponent();
				Rectangle firstKeyRectangle = firstKey.getBounds();
				Container firstKeyContainer = firstKey;
				Container checkContainer = firstKey.getParent();
				while (checkContainer != parent) {
					firstKeyRectangle.setLocation(firstKeyRectangle.x
							+ (int) checkContainer.getX(), firstKeyRectangle.y
							+ (int) checkContainer.getY());
					if ((checkContainer instanceof TableComponent)
							|| (checkContainer instanceof SchemaComponent))
						firstKeyContainer = checkContainer;
					checkContainer = checkContainer.getParent();
				}

				// Do the same for the second key.
				KeyComponent secondKey = comp.getSecondKeyComponent();
				Rectangle secondKeyRectangle = secondKey.getBounds();
				Container secondKeyContainer = secondKey;
				checkContainer = secondKey.getParent();
				while (checkContainer != parent) {
					secondKeyRectangle.setLocation(secondKeyRectangle.x
							+ (int) checkContainer.getX(), secondKeyRectangle.y
							+ (int) checkContainer.getY());
					if ((checkContainer instanceof TableComponent)
							|| (checkContainer instanceof SchemaComponent))
						secondKeyContainer = checkContainer;
					checkContainer = checkContainer.getParent();
				}

				// Find RHS of both foreign and primary keys. This
				// is where the relation lines will attach.
				int firstKeyX = firstKeyRectangle.x + firstKeyRectangle.width;
				int secondKeyX = secondKeyRectangle.x
						+ secondKeyRectangle.width;
				int firstKeyY = firstKeyRectangle.y
						+ (firstKeyRectangle.height / 2);
				int secondKeyY = secondKeyRectangle.y
						+ (secondKeyRectangle.height / 2);

				// Work out which vertical track at the first key end the
				// relation fits into, and update the map for the next one.
				int firstVerticalX = (int) (((Double) nextVerticalTrackX
						.get(firstKeyContainer)).doubleValue());
				nextVerticalTrackX.put(firstKeyContainer, new Double(
						firstVerticalX + LinearLayout.RELATION_SPACING));

				// Work out which vertical track at the second key end the
				// relation fits into.
				int secondVerticalX = (int) (((Double) nextVerticalTrackX
						.get(secondKeyContainer)).doubleValue());
				nextVerticalTrackX.put(secondKeyContainer, new Double(
						secondVerticalX + LinearLayout.RELATION_SPACING));

				// Work out which horizontal track we are following.
				int horizontalY = (int) nextHorizontalY;

				// Create a bounding box around which covers the
				// whole relation area.
				int x = Math.min(firstKeyX, secondKeyX);
				int y = Math.min(horizontalY, Math.min(firstKeyY, secondKeyY));
				int width = Math.max(firstVerticalX, secondVerticalX) - x;
				int height = Math.max(horizontalY, Math.max(firstKeyY,
						secondKeyY))
						- y;
				Rectangle bounds = new Rectangle(
						(int) (x - LinearLayout.RELATION_SPACING),
						(int) (y - LinearLayout.RELATION_SPACING),
						(int) (width + LinearLayout.RELATION_SPACING * 2),
						(int) (height + LinearLayout.RELATION_SPACING * 2));
				comp.setBounds(bounds);

				// Modify all the relation coords to be rooted at 0,0 of its
				// bounds.
				firstKeyX -= bounds.x;
				firstKeyY -= bounds.y;
				secondKeyX -= bounds.x;
				secondKeyY -= bounds.y;
				firstVerticalX -= bounds.x;
				secondVerticalX -= bounds.x;
				horizontalY -= bounds.y;

				// Create a path to describe the relation shape.
				GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 6);

				// Move to starting point at primary key.
				path.moveTo(firstKeyX, firstKeyY);

				// Draw from the first key midpoint across to the vertical
				// track.
				path.lineTo(firstVerticalX, firstKeyY);

				// Draw from the first key vertical track up to the horizontal
				// track.
				path.lineTo(firstVerticalX, horizontalY);

				// Draw along the horizontal track to the second key vertical
				// track.
				path.lineTo(secondVerticalX, horizontalY);

				// Draw up the second key vertical track.
				path.lineTo(secondVerticalX, secondKeyY);

				// Draw across to the second key midpoint.
				path.lineTo(secondKeyX, secondKeyY);

				// Done! Tell the relation what shape we made.
				comp.setShape(path);

				// Move the horizontal track down for the next relation.
				nextHorizontalY += LinearLayout.RELATION_SPACING;
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
