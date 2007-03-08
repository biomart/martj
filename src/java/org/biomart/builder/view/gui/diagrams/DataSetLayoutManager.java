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

/**
 * This layout manager lays out components in grouped lines.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class DataSetLayoutManager implements LayoutManager2 {
	private static final int RELATION_SPACING = 5; // 72 = 1 inch

	private static final int TABLE_PADDING = 10; // 72 = 1 inch

	private Dimension size;

	private boolean sizeKnown;

	private final Map prefSizes = new HashMap();

	private final Map constraints = new HashMap();

	private final List mainTables;

	private final List dimensionTables;

	private final List relations;

	private final List rowHeights;

	private final List rowWidths;

	/**
	 * Sets up some defaults for the layout, ready for use.
	 */
	public DataSetLayoutManager() {
		this.sizeKnown = true;
		this.size = new Dimension(0, 0);
		this.mainTables = new ArrayList();
		this.rowHeights = new ArrayList();
		this.rowWidths = new ArrayList();
		this.dimensionTables = new ArrayList();
		this.relations = new ArrayList();
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

			for (int rowNum = 0; rowNum < this.mainTables.size(); rowNum++) {
				int rowHeight = 0;
				int rowWidth = 0;
				Component comp = (Component) this.mainTables.get(rowNum);
				

				if (comp != null) {
					Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					rowHeight = (int) prefSize.getHeight();
					rowWidth = (int) prefSize.getWidth();
				}

				for (final Iterator i = ((List) this.dimensionTables
						.get(rowNum)).iterator(); i.hasNext();) {
					comp = (Component) i.next();
					if (!comp.isVisible())
						continue;
					Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					rowHeight = (int) Math.max(rowHeight, prefSize.getHeight());
					rowWidth += prefSize.getWidth();
				}

				rowHeight += DataSetLayoutManager.TABLE_PADDING * 2;
				rowHeight += ((List) this.dimensionTables.get(rowNum)).size()
						* DataSetLayoutManager.RELATION_SPACING;
				this.rowHeights.set(rowNum, new Integer(rowHeight));
				this.size.height += rowHeight;

				rowWidth += DataSetLayoutManager.TABLE_PADDING * 2;
				rowWidth += (((List) this.dimensionTables.get(rowNum)).size() + 1)
						* DataSetLayoutManager.TABLE_PADDING * 2;
				rowWidth += ((List) this.dimensionTables.get(rowNum)).size()
						* DataSetLayoutManager.RELATION_SPACING * 2;
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
					&& constraints instanceof DataSetLayoutConstraint) {
				this.constraints.put(comp, constraints);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.put(comp, prefSize);

				final int rowNum = ((DataSetLayoutConstraint) constraints)
						.getRow();

				// Ensure arrays are large enough.
				while (this.mainTables.size() - 1 < rowNum) {
					this.mainTables.add(null);
					this.rowHeights.add(new Integer(0));
					this.rowWidths.add(new Integer(DataSetLayoutManager.TABLE_PADDING * 2));
					this.dimensionTables.add(new ArrayList());
				}

				if (((DataSetLayoutConstraint) constraints).getType() == DataSetLayoutConstraint.MAIN)
					this.mainTables.set(rowNum, comp);
				else
					((List) this.dimensionTables.get(rowNum)).add(comp);

				final int oldRowWidth = ((Integer) rowWidths.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth += prefSize.getWidth()
						+ (DataSetLayoutManager.TABLE_PADDING * 2)
						+ (DataSetLayoutManager.RELATION_SPACING * 2);
				rowWidths.set(rowNum, new Integer(newRowWidth));

				final int oldRowHeight = ((Integer) rowHeights.get(rowNum))
						.intValue();
				int newRowHeight = (int) (Math.max(oldRowHeight, prefSize
						.getHeight()
						+ (DataSetLayoutManager.TABLE_PADDING * 2)) + DataSetLayoutManager.RELATION_SPACING);
				rowHeights.set(rowNum, new Integer(newRowHeight));

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
				final DataSetLayoutConstraint constraints = (DataSetLayoutConstraint) this.constraints
						.remove(comp);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.remove(comp);

				final int rowNum = constraints.getRow();

				if (constraints.getType() == DataSetLayoutConstraint.MAIN)
					this.mainTables.set(rowNum, null);
				else
					((List) this.dimensionTables.get(rowNum)).remove(comp);

				final int oldRowWidth = ((Integer) rowWidths.get(rowNum))
						.intValue();
				final int oldRowHeight = ((Integer) rowHeights.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth -= prefSize.getWidth()
						+ (DataSetLayoutManager.TABLE_PADDING * 2)
						+ (DataSetLayoutManager.RELATION_SPACING * 2);
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				int newRowHeight = (int) (this.mainTables.get(rowNum) != null ? ((Component) this.mainTables
						.get(rowNum)).getPreferredSize().getHeight()
						: 0);
				for (final Iterator i = ((List) this.dimensionTables
						.get(rowNum)).iterator(); i.hasNext();) {
					newRowHeight = (int) Math.max(newRowHeight, ((Component) i
							.next()).getPreferredSize().getHeight());
				}
				newRowHeight += (DataSetLayoutManager.TABLE_PADDING * 2)
						+ (((List) this.dimensionTables.get(rowNum)).size() * DataSetLayoutManager.RELATION_SPACING);
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				this.size.height -= (oldRowHeight - newRowHeight);

				// While last row is empty, remove last row.
				int lastRow = this.mainTables.size() - 1;
				while (lastRow >= 0 && this.mainTables.get(lastRow) == null
						&& ((List) this.dimensionTables.get(lastRow)).isEmpty()) {
					// Remove all references to empty row.
					this.mainTables.remove(lastRow);
					this.rowHeights.remove(lastRow);
					this.rowWidths.remove(lastRow);
					this.dimensionTables.remove(lastRow);
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

			int nextY = DataSetLayoutManager.TABLE_PADDING;
			for (int rowNum = 0; rowNum < this.mainTables.size(); rowNum++) {
				int x = DataSetLayoutManager.TABLE_PADDING * 3;
				int y = nextY
						+ ((Integer) this.rowHeights.get(rowNum)).intValue()
						- (((List) this.dimensionTables.get(rowNum)).size() * DataSetLayoutManager.RELATION_SPACING)
						- DataSetLayoutManager.TABLE_PADDING;
				if (this.mainTables.get(rowNum) != null) {
					final Component comp = (Component) this.mainTables
							.get(rowNum);
					final Dimension prefSize = (Dimension) this.prefSizes
							.get(comp);
					comp.setBounds(x, y - (int) prefSize.getHeight(),
							(int) prefSize.getWidth(), (int) prefSize
									.getHeight());
					comp.validate();
					x += prefSize.getWidth()
							+ (DataSetLayoutManager.TABLE_PADDING * 2)
							+ (((List) this.dimensionTables.get(rowNum)).size() * DataSetLayoutManager.RELATION_SPACING);
				}
				for (final Iterator i = ((List) this.dimensionTables
						.get(rowNum)).iterator(); i.hasNext();) {
					final Component comp = (Component) i.next();
					if (!comp.isVisible())
						continue;
					final Dimension prefSize = (Dimension) this.prefSizes
							.get(comp);
					comp.setBounds(x, y - (int) prefSize.getHeight(),
							(int) prefSize.getWidth(), (int) prefSize
									.getHeight());
					comp.validate();
					x += prefSize.getWidth()
							+ (DataSetLayoutManager.TABLE_PADDING * 2)
							+ DataSetLayoutManager.RELATION_SPACING;
				}
				nextY += ((Integer) this.rowHeights.get(rowNum)).intValue();
			}

			for (final Iterator i = this.relations.iterator(); i.hasNext();) {
				final RelationComponent comp = (RelationComponent) i.next();
				// Obtain first key and work out position relative to
				// diagram.
				int rowNum = 0;
				int rowBottom = ((Integer)this.rowHeights.get(rowNum)).intValue();
				final KeyComponent firstKey = comp.getFirstKeyComponent();
				if (!firstKey.isVisible())
					continue;
				Rectangle firstKeyRectangle = firstKey.getBounds();
				int firstKeyInsetX = firstKeyRectangle.x;
				firstKeyRectangle = SwingUtilities.convertRectangle(firstKey.getParent(), firstKeyRectangle, parent);
				while ((int)firstKeyRectangle.getY() >= rowBottom) { rowBottom += ((Integer)this.rowHeights.get(++rowNum)).intValue(); }

				// Do the same for the second key.
				final KeyComponent secondKey = comp.getSecondKeyComponent();
				if (!secondKey.isVisible())
					continue;
				Rectangle secondKeyRectangle = secondKey.getBounds();
				int secondKeyInsetX = secondKeyRectangle.x;
				secondKeyRectangle = SwingUtilities.convertRectangle(secondKey.getParent(), secondKeyRectangle, parent);
				
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
				if (firstKeyRectangle.getX() == secondKeyRectangle.getX()) {
					// Main/Subclass -> Subclass
					relBottomY = (int) Math.max(leftKeyRectangle.getCenterY(),
							rightKeyRectangle.getCenterY());
					relLeftX = (int) (leftKeyRectangle.getX() - DataSetLayoutManager.TABLE_PADDING);
					relRightX = (int) rightKeyRectangle.getX();

					leftX = (int) leftKeyRectangle.getX() - leftKeyInsetX;
					leftTagX = leftX - DataSetLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = (int) rightKeyRectangle.getX() - rightKeyInsetX;
					rightTagX = rightX - DataSetLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX
							- (int) (DataSetLayoutManager.TABLE_PADDING * 2);
					viaY = ((leftY + rightY) / 2);
				} else {
					// Main/Subclass -> Dimension
					relRightX = (int) rightKeyRectangle.getX();
					relLeftX = (int) leftKeyRectangle.getMaxX();
					relBottomY = rowBottom;

					leftX = (int) leftKeyRectangle.getMaxX() + leftKeyInsetX ;
					leftTagX = leftX + DataSetLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = (int) rightKeyRectangle.getX() - rightKeyInsetX;
					rightTagX = rightX - DataSetLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX
							+ (int) (((List) this.dimensionTables.get(rowNum))
									.size()
									* DataSetLayoutManager.RELATION_SPACING / 2);
					if (Math.abs(rightY - leftY) > 20)
						viaY = (relBottomY + relTopY) / 2;
					else
						viaY = relTopY
								+ (int) ((double) (relBottomY - relTopY) * 1.8);
				}

				// Set overall bounds.
				final Rectangle bounds = new Rectangle(
						(int) (relLeftX - DataSetLayoutManager.RELATION_SPACING * 2),
						(int) (relTopY - DataSetLayoutManager.RELATION_SPACING * 2),
						(int) ((relRightX - relLeftX) + (DataSetLayoutManager.RELATION_SPACING * 4)),
						(int) ((relBottomY - relTopY) + (DataSetLayoutManager.RELATION_SPACING * 4)));
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

	public static class DataSetLayoutConstraint {
		public static final int MAIN = 1;

		public static final int DIMENSION = 2;

		private final int type;

		private final int row;

		public DataSetLayoutConstraint(final int type, final int row) {
			this.type = type;
			this.row = row;
		}

		public int getType() {
			return this.type;
		}

		public int getRow() {
			return this.row;
		}
	}
}
