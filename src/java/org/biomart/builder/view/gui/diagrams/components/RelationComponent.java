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

package org.biomart.builder.view.gui.diagrams.components;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.common.model.Relation;

/**
 * This component represents a relation between two keys, in the form of a line.
 * The path of the line is defined by one of the layout managers provided with
 * MartBuilder.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class RelationComponent extends JComponent implements DiagramComponent {
	private static final float RELATION_LINEWIDTH = 1.0f; // 72 = 1 inch

	private static final float RELATION_MITRE_TRIM = 10.0f; // 72 = 1 inch

	private static final Stroke MANY_MANY = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM);

	private static final Stroke ONE_MANY = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM);

	private static final Stroke ONE_ONE = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH * 2.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM);

	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to concat-only relation colour.
	 */
	public static Color CONCAT_COLOUR = Color.BLUE;

	/**
	 * Constant referring to handmade relation colour.
	 */
	public static Color HANDMADE_COLOUR = Color.GREEN;

	/**
	 * Constant referring to incorrect relation colour.
	 */
	public static Color INCORRECT_COLOUR = Color.RED;

	/**
	 * Constant referring to masked relation colour.
	 */
	public static Color MASKED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to normal relation colour.
	 */
	public static Color NORMAL_COLOUR = Color.DARK_GRAY;

	/**
	 * Constant referring to subclassed relation colour.
	 */
	public static Color SUBCLASS_COLOUR = Color.RED;

	private Diagram diagram;

	private Shape lineShape;

	private Shape outline;

	private Relation relation;

	private RenderingHints renderHints;

	private Object state;

	private Stroke stroke;

	/**
	 * The constructor constructs a component around a given relation, and
	 * associates the component with the given diagram.
	 * 
	 * @param relation
	 *            the relation to show in the component.
	 * @param diagram
	 *            the diagram to show this component in.
	 */
	public RelationComponent(final Relation relation, final Diagram diagram) {
		super();

		// Remember settings.
		this.relation = relation;
		this.diagram = diagram;

		// Turn on the mouse.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);

		// Make sure we're transparent.
		this.setOpaque(false);

		// Set-up rendering hints.
		this.renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		this.renderHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		// Draw our contents, as we don't have any child classes that
		// do this for us unfortunately.
		this.recalculateDiagramComponent();

		// Repaint ourselves.
		this.updateAppearance();
	}

	protected void paintComponent(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHints(this.renderHints);
		// Only bother redrawing if we actually intersect the area being
		// redrawn.
		final Shape clippingArea = g2d.getClip();
		if (clippingArea != null
				&& !this.lineShape.intersects(clippingArea.getBounds2D()))
			return;
		// Fill in opaque background.
		if (this.isOpaque()) {
			g2d.setColor(this.getBackground());
			g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
		// Do painting of this component.
		g2d.setColor(this.getForeground());
		g2d.setStroke(this.stroke);
		g2d.draw(this.lineShape);
		// Clean up.
		g2d.dispose();
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Build the basic menu.
			final JPopupMenu contextMenu = this.getContextMenu();

			// Customise it using the diagram context.
			if (this.getDiagram().getDiagramContext() != null)
				this.getDiagram().getDiagramContext().populateContextMenu(
						contextMenu, this.getObject());

			// Display.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.show(this, evt.getX(), evt.getY());

			// We have successfully handled the event.
			eventProcessed = true;
		}

		// Pass the event on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	public boolean contains(final int x, final int y) {
		// Clicks are on us if they are within a certain distance
		// of the outline shape.
		return this.outline != null
				&& this.outline.intersects(new Rectangle2D.Double(x
						- RelationComponent.RELATION_LINEWIDTH * 2, y
						- RelationComponent.RELATION_LINEWIDTH * 2,
						RelationComponent.RELATION_LINEWIDTH * 4,
						RelationComponent.RELATION_LINEWIDTH * 4));
	}

	public boolean equals(final Object obj) {
		return obj instanceof DiagramComponent
				&& ((DiagramComponent) obj).getObject()
						.equals(this.getObject());
	}

	public JPopupMenu getContextMenu() {
		final JPopupMenu contextMenu = new JPopupMenu();
		// No additional entries for us yet.
		// Return it.
		return contextMenu;
	}

	public Diagram getDiagram() {
		return this.diagram;
	}

	/**
	 * Returns the diagram component representing the first key of this
	 * relation.
	 * 
	 * @return the diagram component for the first key.
	 */
	public KeyComponent getFirstKeyComponent() {
		return (KeyComponent) this.diagram.getDiagramComponent(this.relation
				.getFirstKey());
	}

	public Object getObject() {
		return this.relation;
	}

	/**
	 * Returns the diagram component representing the second key of this
	 * relation.
	 * 
	 * @return the diagram component for the second key.
	 */
	public KeyComponent getSecondKeyComponent() {
		return (KeyComponent) this.diagram.getDiagramComponent(this.relation
				.getSecondKey());
	}

	public Object getState() {
		return this.state;
	}

	public Map getSubComponents() {
		// We have no sub-components.
		return Collections.EMPTY_MAP;
	}

	public int hashCode() {
		return this.getObject().hashCode();
	}

	public void recalculateDiagramComponent() {
		// Nothing to do, but we have a tool-tip so update that instead.
		this.setToolTipText(this.relation.getName());
	}

	public void repaintDiagramComponent() {
		this.repaint(this.getVisibleRect());
	}

	/**
	 * Sets the shape for us to display the outline of. This will usually be a
	 * line, however it's up to the layout manager entirely.
	 * 
	 * @param shape
	 *            the shape this relation should take on screen.
	 */
	public void setLineShape(final Shape shape) {
		// Only change if the shape has changed.
		if (this.lineShape != shape
				|| (this.lineShape != null && !this.lineShape.equals(shape))) {
			this.lineShape = shape;
			// Update the outline of the relation shape accordingly.
			if (this.lineShape != null)
				this.outline = new BasicStroke()
						.createStrokedShape(this.lineShape);
		}
		// Update our appearance.
		this.updateAppearance();
	}

	public void setState(final Object state) {
		this.state = state;
	}

	public void updateAppearance() {
		// Use the context to alter us first.
		final DiagramContext mod = this.getDiagram().getDiagramContext();
		if (mod != null)
			mod.customiseAppearance(this, this.getObject());
		// Work out what style to draw the relation line.
		if (this.relation.isOneToOne())
			this.stroke = RelationComponent.ONE_ONE;
		else if (this.relation.isOneToMany())
			this.stroke = RelationComponent.ONE_MANY;
		else if (this.relation.isManyToMany())
			this.stroke = RelationComponent.MANY_MANY;
	}
}
