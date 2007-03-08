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

	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	public void invalidateLayout(Container target) {
		this.sizeKnown = false;
	}

	public void addLayoutComponent(String name, Component comp) {
		this.addLayoutComponent(comp, null);
	}

	public Dimension maximumLayoutSize(Container target) {
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
				List row = (List) this.rows.get(rowNum);

				int rowHeight = 0;
				int rowWidth = 0;
				int rowSpacing = 0;

				for (final Iterator i = row.iterator(); i.hasNext();) {
					Component comp = (Component) i.next();
					Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					int compSpacing = ((SchemaLayoutConstraint) this.constraints
							.get(comp)).getRelCount()
							* SchemaLayoutManager.RELATION_SPACING;
					rowHeight = (int) Math.max(rowHeight, prefSize.getHeight()
							+ (compSpacing * 2));
					rowWidth += prefSize.getWidth() + (compSpacing * 2);
					rowSpacing = (int) Math.max(rowSpacing, compSpacing);
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

	public void addLayoutComponent(Component comp, Object constraints) {
		synchronized (comp.getTreeLock()) {
			if (comp instanceof RelationComponent) {
				this.relations.add(comp);
			} else if (comp instanceof DiagramComponent && constraints != null
					&& (constraints instanceof SchemaLayoutConstraint)) {

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

				final int oldRowWidth = ((Integer) rowWidths.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth += prefSize.getWidth()
						+ (SchemaLayoutManager.TABLE_PADDING * 2)
						+ (compSpacing * 2);
				rowWidths.set(rowNum, new Integer(newRowWidth));

				final int oldRowHeight = ((Integer) rowHeights.get(rowNum))
						.intValue();
				int newRowHeight = (int) (Math.max(oldRowHeight, prefSize
						.getHeight()
						+ (SchemaLayoutManager.TABLE_PADDING * 2)) + (compSpacing * 2));
				rowHeights.set(rowNum, new Integer(newRowHeight));

				rowSpacings.set(rowNum, new Integer((int) Math.max(
						((Integer) rowSpacings.get(rowNum)).intValue(),
						compSpacing)));

				this.size.height += (newRowHeight - oldRowHeight);
				this.size.width = Math.max(this.size.width, newRowWidth);
			}
		}
	}

	public void removeLayoutComponent(Component comp) {
		synchronized (comp.getTreeLock()) {
			if (comp instanceof RelationComponent)
				this.relations.remove(comp);
			else {
				final SchemaLayoutConstraint constraints = (SchemaLayoutConstraint) this.constraints
						.remove(comp);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.remove(comp);

				this.tableCount--;
				final int rowNum = constraints.getRow();

				((List) this.rows.get(rowNum)).remove(comp);

				final int oldRowWidth = ((Integer) rowWidths.get(rowNum))
						.intValue();
				final int oldRowHeight = ((Integer) rowHeights.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth -= prefSize.getWidth()
						+ (SchemaLayoutManager.TABLE_PADDING * 2)
						+ (SchemaLayoutManager.RELATION_SPACING * 2 * constraints
								.getRelCount());
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				int newRowHeight = 0;
				for (final Iterator i = ((List) this.rows.get(rowNum))
						.iterator(); i.hasNext();) {
					newRowHeight = (int) Math
							.max(
									newRowHeight,
									((Component) i.next()).getPreferredSize()
											.getHeight()
											+ (SchemaLayoutManager.RELATION_SPACING * 2 * constraints
													.getRelCount()));
				}
				newRowHeight += SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				this.size.height -= (oldRowHeight - newRowHeight);

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
				int y = nextY
						+ ((Integer) this.rowHeights.get(rowNum)).intValue()
						- (SchemaLayoutManager.TABLE_PADDING * 2)
						- ((Integer) this.rowSpacings.get(rowNum)).intValue();
				for (final Iterator i = ((List) rows.get(rowNum)).iterator(); i
						.hasNext();) {
					final Component comp = (Component) i.next();
					final Dimension prefSize = (Dimension) this.prefSizes
					.get(comp);
					final int compSpacing = ((SchemaLayoutConstraint) this.constraints
							.get(comp)).getRelCount()
							* SchemaLayoutManager.RELATION_SPACING;
					x += compSpacing;
					comp.setBounds(x, y - (int) prefSize.getHeight(),
							(int) prefSize.getWidth(), (int) prefSize
									.getHeight());
					comp.validate();
					x += prefSize.getWidth()
							+ (SchemaLayoutManager.TABLE_PADDING * 2)
							+ compSpacing;
				}
				nextY += ((Integer) this.rowHeights.get(rowNum)).intValue();
			}

			for (final Iterator i = this.relations.iterator(); i.hasNext();) {
				final RelationComponent comp = (RelationComponent) i.next();
				// Obtain first key and work out position relative to
				// diagram.
				int firstRowNum = 0;
				int firstRowBottom = ((Integer)this.rowHeights.get(firstRowNum)).intValue();
				final KeyComponent firstKey = comp.getFirstKeyComponent();
				Rectangle firstKeyRectangle = firstKey.getBounds();
				int firstKeyInsetX = firstKeyRectangle.x;
				if (SwingUtilities.getAncestorOfClass(SchemaComponent.class, firstKey)!=null)
					firstKeyInsetX += firstKey.getParent().getBounds().getX();
				firstKeyRectangle = SwingUtilities.convertRectangle(firstKey.getParent(), firstKeyRectangle, SwingUtilities.getAncestorOfClass(Diagram.class, firstKey));
				while ((int)firstKeyRectangle.getY() >= firstRowBottom) { firstRowBottom += ((Integer)this.rowHeights.get(++firstRowNum)).intValue(); }

				// Do the same for the second key.
				int secondRowNum = 0;
				int secondRowBottom = ((Integer)this.rowHeights.get(secondRowNum)).intValue();
				final KeyComponent secondKey = comp.getSecondKeyComponent();
				Rectangle secondKeyRectangle = secondKey.getBounds();
				int secondKeyInsetX = secondKeyRectangle.x;
				if (SwingUtilities.getAncestorOfClass(SchemaComponent.class, secondKey)!=null)
					secondKeyInsetX += secondKey.getParent().getBounds().getX();
				secondKeyRectangle = SwingUtilities.convertRectangle(secondKey.getParent(), secondKeyRectangle, SwingUtilities.getAncestorOfClass(Diagram.class, secondKey));
				while ((int)secondKeyRectangle.getY() >= secondRowBottom) { secondRowBottom += ((Integer)this.rowHeights.get(++secondRowNum)).intValue(); }

				// Work out left/right most.
				final Rectangle leftKeyRectangle = firstKeyRectangle.getX() <= secondKeyRectangle
						.getX() ? firstKeyRectangle : secondKeyRectangle;
				final Rectangle rightKeyRectangle = firstKeyRectangle.getX() > secondKeyRectangle
						.getX() ? firstKeyRectangle : secondKeyRectangle;
				final int leftKeyInsetX = leftKeyRectangle==firstKeyRectangle?firstKeyInsetX:secondKeyInsetX;
				final int rightKeyInsetX = rightKeyRectangle==firstKeyRectangle?firstKeyInsetX:secondKeyInsetX;

				// Work out Y coord for top of relation.
				int relTopY = (int) Math.min(leftKeyRectangle.getCenterY(),
						rightKeyRectangle.getCenterY());
				int relBottomY, relLeftX, relRightX;
				int leftX, rightX, leftY, rightY, viaX, viaY;
				int leftTagX, rightTagX;

				// Both at same X location?
				if (Math.abs(firstKeyRectangle.getX()-secondKeyRectangle.getX())<100) {
					relBottomY = (int) Math.max(leftKeyRectangle.getCenterY(),
							rightKeyRectangle.getCenterY());
					relLeftX = (int) (leftKeyRectangle.getX() - SchemaLayoutManager.TABLE_PADDING);
					relRightX = (int) rightKeyRectangle.getX();

					leftX = (int) leftKeyRectangle.getX() - leftKeyInsetX;
					leftTagX = leftX - SchemaLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = (int) rightKeyRectangle.getX() - rightKeyInsetX;
					rightTagX = rightX - SchemaLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX
							- (int) (SchemaLayoutManager.TABLE_PADDING * 2);
					viaY = ((leftY + rightY) / 2);
				} else {
					relRightX = (int) Math.max(leftKeyRectangle.getMaxX(), rightKeyRectangle.getX());
					relLeftX = (int) Math.min(leftKeyRectangle.getMaxX(), rightKeyRectangle.getX());
					relBottomY = Math.max(firstRowBottom, secondRowBottom);

					leftX = (int) leftKeyRectangle.getMaxX() + leftKeyInsetX;
					leftTagX = leftX + SchemaLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = (int) rightKeyRectangle.getX() - rightKeyInsetX;
					rightTagX = rightX - SchemaLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = (leftX + rightX) / 2;
					if (Math.abs(rightX - leftX) < 100)
						viaY = (leftY + rightY) / 2;
					else if (Math.abs(rightY - leftY) > 100)
						viaY = (relBottomY + relTopY) / 2;
					else
						viaY = relTopY
								+ (int) ((double) (relBottomY - relTopY) * 1.8);
				}

				// Set overall bounds.
				final Rectangle bounds = new Rectangle(
						(int) (relLeftX - SchemaLayoutManager.RELATION_SPACING * 4),
						(int) (relTopY - SchemaLayoutManager.RELATION_SPACING * 4),
						(int) ((Math.abs(relRightX - relLeftX)) + (SchemaLayoutManager.RELATION_SPACING * 8)),
						(int) ((Math.abs(relBottomY - relTopY)) + (SchemaLayoutManager.RELATION_SPACING * 8)));
				comp.setBounds(bounds);

				// Create a path to describe the relation shape. It
				// will have 2 components to it - move, curve.
				final GeneralPath path = new GeneralPath(
						GeneralPath.WIND_EVEN_ODD, 4);

				// Move to starting point at primary key.
				path.moveTo(leftX - (int) bounds.getX(), leftY
						- (int) bounds.getY());
				
				// Left tag.
				path.lineTo(leftTagX - (int)bounds.getX(), leftY
						- (int) bounds.getY());
				
				// Draw from the first key midpoint across to the vertical
				// track.
				path.quadTo(viaX - (int) bounds.getX(), viaY
						- (int) bounds.getY(), rightTagX - (int) bounds.getX(),
						rightY - (int) bounds.getY());
				
				// Right tag.
				path.lineTo(rightX - (int)bounds.getX(), rightY
						- (int) bounds.getY());

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

		private void setRow(int row) {
			this.row = row;
		}

		private int getRow() {
			return this.row;
		}
	}
}
