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
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.biomart.builder.view.gui.diagrams.components.DiagramComponent;
import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.SchemaComponent;

/**
 * This layout manager lays out components in grouped lines.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class SchemaLayoutManager implements LayoutManager2 {
	private static final int RELATION_SPACING = 5; // 72 = 1 inch

	private static final int TABLE_PADDING = 10; // 72 = 1 inch

	private Dimension size;

	private boolean sizeKnown;

	private final Map prefSizes = new HashMap();

	private final Map constraints = new HashMap();

	private final List rows;

	private final List relations;

	private final List rowHeights;

	private final List rowWidths;

	private final List rowSpacings;

	private int tableCount;

	/**
	 * Sets up some defaults for the layout, ready for use.
	 */
	public SchemaLayoutManager() {
		this.sizeKnown = true;
		this.size = new Dimension(0, 0);
		this.rowHeights = new ArrayList();
		this.rowSpacings = new ArrayList();
		this.rowWidths = new ArrayList();
		this.rows = new ArrayList();
		this.relations = new ArrayList();
		this.tableCount = 0;
	}

	public float getLayoutAlignmentX(final Container target) {
		return 0.5f;
	}

	public float getLayoutAlignmentY(final Container target) {
		return 0.5f;
	}

	public void invalidateLayout(final Container target) {
		this.sizeKnown = false;
	}

	public void addLayoutComponent(final String name, final Component comp) {
		this.addLayoutComponent(comp, null);
	}

	public Dimension maximumLayoutSize(final Container target) {
		return this.minimumLayoutSize(target);
	}

	public Dimension preferredLayoutSize(final Container parent) {
		// Our preferred size is our minimum size.
		return this.minimumLayoutSize(parent);
	}

	public Dimension minimumLayoutSize(final Container parent) {
		// Work out how big we are.
		this.calculateSize(parent);
		synchronized (parent.getTreeLock()) {

			// Work out our parent's insets.
			final Insets insets = parent.getInsets();

			// The minimum size is our size plus our
			// parent's insets size.
			final Dimension dim = new Dimension(0, 0);
			dim.width = this.size.width + insets.left + insets.right;
			dim.height = this.size.height + insets.top + insets.bottom;

			// That's it!
			return dim;
		}
	}

	private void calculateSize(final Container parent) {
		synchronized (parent.getTreeLock()) {
			if (this.sizeKnown)
				return;

			this.size.height = 0;
			this.size.width = 0;
			this.prefSizes.clear();

			for (int rowNum = 0; rowNum < this.rows.size(); rowNum++) {
				final List row = (List) this.rows.get(rowNum);

				int rowHeight = 0;
				int rowWidth = 0;
				int rowSpacing = 0;

				for (final Iterator i = row.iterator(); i.hasNext();) {
					final Component comp = (Component) i.next();
					final Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					final int compSpacing = ((SchemaLayoutConstraint) this.constraints
							.get(comp)).getRelCount()
							* SchemaLayoutManager.RELATION_SPACING;
					rowHeight = Math.max(rowHeight, prefSize.height
							+ compSpacing * 2);
					rowWidth += prefSize.width + compSpacing * 2;
					rowSpacing = Math.max(rowSpacing, compSpacing);
				}

				this.rowSpacings.set(rowNum, new Integer(rowSpacing));

				rowHeight += SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowHeights.set(rowNum, new Integer(rowHeight));
				this.size.height += rowHeight;

				rowWidth += (row.size() + 1)
						* SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowWidths.set(rowNum, new Integer(rowWidth));
				this.size.width = Math.max(rowWidth, this.size.width);
			}

			this.sizeKnown = true;
		}
	}

	public void addLayoutComponent(final Component comp,
			final Object constraints) {
		synchronized (comp.getTreeLock()) {
			if (comp instanceof RelationComponent)
				this.relations.add(comp);
			else if (comp instanceof DiagramComponent && constraints != null
					&& constraints instanceof SchemaLayoutConstraint) {

				this.constraints.put(comp, constraints);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.put(comp, prefSize);

				final int rowLength = (int) Math.ceil(Math
						.sqrt(++this.tableCount));
				int rowNum = 0;
				while (rowNum < this.rows.size()
						&& ((List) this.rows.get(rowNum)).size() >= rowLength)
					rowNum++;
				((SchemaLayoutConstraint) constraints).setRow(rowNum);

				// Ensure arrays are large enough.
				while (rowNum >= this.rows.size()) {
					this.rowSpacings.add(new Integer(0));
					this.rowHeights.add(new Integer(0));
					this.rowWidths.add(new Integer(0));
					this.rows.add(new ArrayList());
				}

				((List) this.rows.get(rowNum)).add(comp);

				final int compSpacing = SchemaLayoutManager.RELATION_SPACING
						* ((SchemaLayoutConstraint) constraints).getRelCount();

				final int oldRowWidth = ((Integer) this.rowWidths.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth += prefSize.width
						+ SchemaLayoutManager.TABLE_PADDING * 2 + compSpacing
						* 2;
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				final int oldRowHeight = ((Integer) this.rowHeights.get(rowNum))
						.intValue();
				final int newRowHeight = Math.max(oldRowHeight, prefSize.height
						+ SchemaLayoutManager.TABLE_PADDING * 2)
						+ compSpacing * 2;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				this.rowSpacings.set(rowNum, new Integer(Math.max(
						((Integer) this.rowSpacings.get(rowNum)).intValue(),
						compSpacing)));

				this.size.height += newRowHeight - oldRowHeight;
				this.size.width = Math.max(this.size.width, newRowWidth);
			}
		}
	}

	public void removeLayoutComponent(final Component comp) {
		synchronized (comp.getTreeLock()) {
			if (comp instanceof RelationComponent)
				this.relations.remove(comp);
			else {
				final SchemaLayoutConstraint constraints = (SchemaLayoutConstraint) this.constraints
						.remove(comp);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.remove(comp);

				final int compSpacing = SchemaLayoutManager.RELATION_SPACING
						* constraints.getRelCount();

				this.tableCount--;
				final int rowNum = constraints.getRow();

				((List) this.rows.get(rowNum)).remove(comp);

				final int oldRowWidth = ((Integer) this.rowWidths.get(rowNum))
						.intValue();
				final int oldRowHeight = ((Integer) this.rowHeights.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth -= prefSize.width
						+ SchemaLayoutManager.TABLE_PADDING * 2 + compSpacing
						* 2;
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				int newRowHeight = 0;
				for (final Iterator i = ((List) this.rows.get(rowNum))
						.iterator(); i.hasNext();)
					newRowHeight = Math.max(newRowHeight,
							((Component) i.next()).getPreferredSize().height
									+ compSpacing * 2);
				newRowHeight += SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				this.size.height -= oldRowHeight - newRowHeight;

				// While last row is empty, remove last row.
				int lastRow = this.rows.size() - 1;
				while (lastRow >= 0 && this.rows.get(lastRow) == null
						&& ((List) this.rows.get(lastRow)).isEmpty()) {
					// Remove all references to empty row.
					this.rows.remove(lastRow);
					this.rowHeights.remove(lastRow);
					this.rowSpacings.remove(lastRow);
					this.rowWidths.remove(lastRow);
					// Update last row pointer.
					lastRow--;
				}

				// New width needs re-calculating from all rows.
				this.size.width = 0;
				for (final Iterator i = this.rowWidths.iterator(); i.hasNext();)
					this.size.width = Math.max(((Integer) i.next()).intValue(),
							this.size.width);
			}
		}
	}

	public void layoutContainer(final Container parent) {
		// Work out how big we are.
		this.calculateSize(parent);
		synchronized (parent.getTreeLock()) {
			int nextY = SchemaLayoutManager.TABLE_PADDING;
			for (int rowNum = 0; rowNum < this.rows.size(); rowNum++) {
				int x = SchemaLayoutManager.TABLE_PADDING;
				final int y = nextY
						+ ((Integer) this.rowHeights.get(rowNum)).intValue()
						- SchemaLayoutManager.TABLE_PADDING * 2
						- ((Integer) this.rowSpacings.get(rowNum)).intValue();
				for (final Iterator i = ((List) this.rows.get(rowNum))
						.iterator(); i.hasNext();) {
					final Component comp = (Component) i.next();
					final Dimension prefSize = (Dimension) this.prefSizes
							.get(comp);
					final int compSpacing = ((SchemaLayoutConstraint) this.constraints
							.get(comp)).getRelCount()
							* SchemaLayoutManager.RELATION_SPACING;
					x += compSpacing;
					comp.setBounds(x, y - prefSize.height, prefSize.width,
							prefSize.height);
					comp.validate();
					x += prefSize.width + SchemaLayoutManager.TABLE_PADDING * 2
							+ compSpacing;
				}
				nextY += ((Integer) this.rowHeights.get(rowNum)).intValue();
			}

			for (final Iterator i = this.relations.iterator(); i.hasNext();) {
				final RelationComponent comp = (RelationComponent) i.next();
				// Obtain first key and work out position relative to
				// diagram.
				int firstRowNum = 0;
				int firstRowBottom = ((Integer) this.rowHeights
						.get(firstRowNum)).intValue();
				final KeyComponent firstKey = comp.getFirstKeyComponent();
				if (firstKey == null)
					continue;
				Rectangle firstKeyRectangle = firstKey.getBounds();
				int firstKeyInsetX = firstKeyRectangle.x;
				if (firstKey.getParent().getParent() instanceof SchemaComponent)
					firstKeyInsetX += firstKey.getParent().getBounds().x;
				firstKeyRectangle = SwingUtilities.convertRectangle(firstKey
						.getParent(), firstKeyRectangle, parent);
				while (firstKeyRectangle.y >= firstRowBottom)
					firstRowBottom += ((Integer) this.rowHeights
							.get(++firstRowNum)).intValue();

				// Do the same for the second key.
				int secondRowNum = 0;
				int secondRowBottom = ((Integer) this.rowHeights
						.get(secondRowNum)).intValue();
				final KeyComponent secondKey = comp.getSecondKeyComponent();
				if (secondKey == null)
					continue;
				Rectangle secondKeyRectangle = secondKey.getBounds();
				int secondKeyInsetX = secondKeyRectangle.x;
				if (secondKey.getParent().getParent() instanceof SchemaComponent)
					secondKeyInsetX += secondKey.getParent().getBounds().x;
				secondKeyRectangle = SwingUtilities.convertRectangle(secondKey
						.getParent(), secondKeyRectangle, parent);
				while (secondKeyRectangle.y >= secondRowBottom)
					secondRowBottom += ((Integer) this.rowHeights
							.get(++secondRowNum)).intValue();

				// Work out left/right most.
				final Rectangle leftKeyRectangle = firstKeyRectangle.x <= secondKeyRectangle.x ? firstKeyRectangle
						: secondKeyRectangle;
				final Rectangle rightKeyRectangle = firstKeyRectangle.x > secondKeyRectangle.x ? firstKeyRectangle
						: secondKeyRectangle;
				final int leftKeyInsetX = leftKeyRectangle == firstKeyRectangle ? firstKeyInsetX
						: secondKeyInsetX;
				final int rightKeyInsetX = rightKeyRectangle == firstKeyRectangle ? firstKeyInsetX
						: secondKeyInsetX;

				// Work out Y coord for top of relation.
				final int relTopY = (int) Math.min(leftKeyRectangle
						.getCenterY(), rightKeyRectangle.getCenterY());
				int relBottomY, relLeftX, relRightX;
				int leftX, rightX, leftY, rightY, viaX, viaY;
				int leftTagX, rightTagX;

				// Both at same X location?
				if (Math.abs(firstKeyRectangle.x - secondKeyRectangle.x) < 100) {
					relBottomY = (int) Math.max(leftKeyRectangle.getCenterY(),
							rightKeyRectangle.getCenterY());
					relLeftX = leftKeyRectangle.x
							- SchemaLayoutManager.TABLE_PADDING;
					relRightX = rightKeyRectangle.x;

					leftX = leftKeyRectangle.x - leftKeyInsetX;
					leftTagX = leftX - SchemaLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = rightKeyRectangle.x - rightKeyInsetX;
					rightTagX = rightX - SchemaLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX - SchemaLayoutManager.TABLE_PADDING * 2;
					viaY = (leftY + rightY) / 2;
				} else {
					relRightX = (int) Math.max(leftKeyRectangle.getMaxX(),
							rightKeyRectangle.x);
					relLeftX = (int) Math.min(leftKeyRectangle.getMaxX(),
							rightKeyRectangle.x);
					relBottomY = Math.max(firstRowBottom, secondRowBottom);

					leftX = (int) leftKeyRectangle.getMaxX() + leftKeyInsetX;
					leftTagX = leftX + SchemaLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = rightKeyRectangle.x - rightKeyInsetX;
					rightTagX = rightX - SchemaLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = (leftX + rightX) / 2;
					if (Math.abs(rightX - leftX) < 100)
						viaY = (leftY + rightY) / 2;
					else if (Math.abs(rightY - leftY) > 100)
						viaY = (relBottomY + relTopY) / 2;
					else
						viaY = relTopY + (int) ((relBottomY - relTopY) * 1.8);
				}

				// Set overall bounds.
				final Rectangle bounds = new Rectangle(
						(relLeftX - SchemaLayoutManager.RELATION_SPACING * 4),
						(relTopY - SchemaLayoutManager.RELATION_SPACING * 4),
						(Math.abs(relRightX - relLeftX) + SchemaLayoutManager.RELATION_SPACING * 8),
						(Math.abs(relBottomY - relTopY) + SchemaLayoutManager.RELATION_SPACING * 8));
				comp.setBounds(bounds);

				// Create a path to describe the relation shape. It
				// will have 2 components to it - move, curve.
				final GeneralPath path = new GeneralPath(
						GeneralPath.WIND_EVEN_ODD, 4);

				// Move to starting point at primary key.
				path.moveTo(leftX - bounds.x, leftY - bounds.y);

				// Left tag.
				path.lineTo(leftTagX - bounds.x, leftY - bounds.y);

				// Draw from the first key midpoint across to the vertical
				// track.
				path.quadTo(viaX - bounds.x, viaY - bounds.y, rightTagX
						- bounds.x, rightY - bounds.y);

				// Right tag.
				path.lineTo(rightX - bounds.x, rightY - bounds.y);

				// Set the shape.
				comp.setLineShape(path);
			}
		}
	}

	public static class SchemaLayoutConstraint {

		private final int relCount;

		private int row;

		public SchemaLayoutConstraint(final int relCount) {
			this.relCount = relCount;
			this.row = 0;
		}

		public int getRelCount() {
			return this.relCount;
		}

		private void setRow(final int row) {
			this.row = row;
		}

		private int getRow() {
			return this.row;
		}
	}
}
