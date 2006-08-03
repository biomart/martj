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

import org.biomart.builder.model.Relation;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;

/**
 * This component represents a relation between two keys, in the form of a line.
 * The line is defined by the layout manager.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.18, 24th July 2006
 * @since 0.1
 */
public class RelationComponent extends JComponent implements DiagramComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to the normal width of a relation line.
	 */
	public static final float RELATION_LINEWIDTH = 1.0f; // 72 = 1 inch

	/**
	 * Constant referring to the dash size of an optional relation line.
	 */
	public static final float RELATION_DASHSIZE = 7.0f; // 72 = 1 inch

	/**
	 * Constant referring to the dot size of a restricted relation line.
	 */
	public static final float RELATION_DOTSIZE = 3.0f; // 72 = 1 inch

	/**
	 * Constant referring to the mitre trim of a relation line.
	 */
	public static final float RELATION_MITRE_TRIM = 10.0f; // 72 = 1 inch

	/**
	 * Constant referring to normal relation colour.
	 */
	public static final Color NORMAL_COLOUR = Color.DARK_GRAY;

	/**
	 * Constant referring to masked relation colour.
	 */
	public static final Color MASKED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to incorrect relation colour.
	 */
	public static final Color INCORRECT_COLOUR = Color.RED;

	/**
	 * Constant referring to handmade relation colour.
	 */
	public static final Color HANDMADE_COLOUR = Color.GREEN;

	/**
	 * Constant referring to concat-only relation colour.
	 */
	public static final Color CONCAT_COLOUR = Color.BLUE;

	/**
	 * Constant referring to subclassed relation colour.
	 */
	public static final Color SUBCLASS_COLOUR = Color.RED;

	/**
	 * Constant defining our 1:M stroke.
	 */
	public static final Stroke ONE_MANY = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM);

	/**
	 * Constant defining our 1:1 stroke.
	 */
	public static final Stroke ONE_ONE = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH * 2.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM);

	/**
	 * Constant defining our M:M stroke.
	 */
	public static final Stroke MANY_MANY = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM);

	/**
	 * Constant defining our optional 1:M stroke.
	 */
	public static final Stroke ONE_MANY_OPTIONAL = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM,
			new float[] { RelationComponent.RELATION_DASHSIZE,
					RelationComponent.RELATION_DASHSIZE }, 0);

	/**
	 * Constant defining our optional 1:1 stroke.
	 */
	public static final Stroke ONE_ONE_OPTIONAL = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH * 2.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM,
			new float[] { RelationComponent.RELATION_DASHSIZE,
					RelationComponent.RELATION_DASHSIZE }, 0);

	/**
	 * Constant defining our optional M:M stroke.
	 */
	public static final Stroke MANY_MANY_OPTIONAL = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM,
			new float[] { RelationComponent.RELATION_DASHSIZE,
					RelationComponent.RELATION_DASHSIZE }, 0);

	/**
	 * Constant defining our restricted 1:M stroke.
	 */
	public static final Stroke ONE_MANY_RESTRICTED = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM,
			new float[] { RelationComponent.RELATION_DASHSIZE,
					RelationComponent.RELATION_DOTSIZE,
					RelationComponent.RELATION_DOTSIZE,
					RelationComponent.RELATION_DOTSIZE }, 0);

	/**
	 * Constant defining our restricted 1:1 stroke.
	 */
	public static final Stroke ONE_ONE_RESTRICTED = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH * 2.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM,
			new float[] { RelationComponent.RELATION_DASHSIZE,
					RelationComponent.RELATION_DOTSIZE,
					RelationComponent.RELATION_DOTSIZE,
					RelationComponent.RELATION_DOTSIZE }, 0);

	/**
	 * Constant defining our restricted M:M stroke.
	 */
	public static final Stroke MANY_MANY_RESTRICTED = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, RelationComponent.RELATION_MITRE_TRIM,
			new float[] { RelationComponent.RELATION_DASHSIZE,
					RelationComponent.RELATION_DOTSIZE,
					RelationComponent.RELATION_DOTSIZE,
					RelationComponent.RELATION_DOTSIZE }, 0);

	private Shape shape;

	private Stroke stroke;

	private Shape outline;

	private Diagram diagram;

	private Relation relation;

	private Object state;

	private RenderingHints renderHints;

	/**
	 * The constructor constructs an object around a given object, and
	 * associates with a given display.
	 * 
	 * @param relation
	 *            the relation to show in the component.
	 * @param diagram
	 *            the diagram to show this component in.
	 */
	public RelationComponent(final Relation relation, final Diagram diagram) {
		super();
		this.relation = relation;
		this.diagram = diagram;
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		this.recalculateDiagramComponent();

		// Set-up rendering hints.
		this.renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		this.renderHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		this.updateAppearance();
	}

	public Map getSubComponents() {
		// We have no sub-components.
		return Collections.EMPTY_MAP;
	}

	public void updateAppearance() {
		final DiagramContext mod = this.getDiagram().getDiagramContext();
		if (mod != null)
			mod.customiseAppearance(this, this.getObject());
		this.setBackground(this.getForeground());
		if (this.stroke != null && this.shape != null) {
			this.outline = new BasicStroke().createStrokedShape(this.shape);
			this.repaint();
		}
	}

	public void recalculateDiagramComponent() {
		// Nothing to do, but we have a tool-tip so update that instead.
		this.setToolTipText(this.relation.getName());
	}

	public Object getState() {
		return this.state;
	}

	public void setState(final Object state) {
		this.state = state;
	}

	/**
	 * Returns the diagram component representing the first end of this
	 * relation.
	 * 
	 * @return the diagram component for the first end.
	 */
	public KeyComponent getFirstKeyComponent() {
		return (KeyComponent) this.diagram.getDiagramComponent(this.relation
				.getFirstKey());
	}

	/**
	 * Returns the diagram component representing the second end of this
	 * relation.
	 * 
	 * @return the diagram component for the second end.
	 */
	public KeyComponent getSecondKeyComponent() {
		return (KeyComponent) this.diagram.getDiagramComponent(this.relation
				.getSecondKey());
	}

	public Diagram getDiagram() {
		return this.diagram;
	}

	public Object getObject() {
		return this.relation;
	}

	/**
	 * Sets the shape for us to display the outline of. This will usually be a
	 * line, however it's up to the layout manager entirely.
	 * 
	 * @param shape
	 *            the shape this relation should take on screen.
	 */
	public void setShape(final Shape shape) {
		this.shape = shape;
		this.updateAppearance();
	}

	public boolean contains(final int x, final int y) {
		// Clicks are on us if they are within a certain distance
		// of the stroked shape.
		return this.outline != null
				&& this.outline.intersects(new Rectangle2D.Double(x
						- RelationComponent.RELATION_LINEWIDTH * 2, y
						- RelationComponent.RELATION_LINEWIDTH * 2,
						RelationComponent.RELATION_LINEWIDTH * 4,
						RelationComponent.RELATION_LINEWIDTH * 4));
	}

	public JPopupMenu getContextMenu() {
		final JPopupMenu contextMenu = new JPopupMenu();
		// No additional entries for us yet.
		// Return it.
		return contextMenu;
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

	/**
	 * Updates the stroke of this relation.
	 * 
	 * @param stroke
	 *            the stroke to use for this relation.
	 */
	public void setStroke(final Stroke stroke) {
		this.stroke = stroke;
	}

	protected void paintComponent(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHints(this.renderHints);
		final Shape clippingArea = g2d.getClip();
		if (clippingArea != null
				&& !this.shape.intersects(clippingArea.getBounds2D()))
			return;
		// Do painting of this component.
		g2d.setColor(this.getForeground());
		g2d.setStroke(this.stroke);
		g2d.draw(this.shape);
		// Clean up.
		g2d.dispose();
	}

	public int hashCode() {
		return this.getObject().hashCode();
	}

	public boolean equals(final Object obj) {
		return obj instanceof DiagramComponent
				&& ((DiagramComponent) obj).getObject()
						.equals(this.getObject());
	}
}
