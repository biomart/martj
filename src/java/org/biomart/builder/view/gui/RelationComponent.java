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

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;

/**
 * This component represents a relation between two keys, in the form of a line.
 * The line is defined by the layout manager.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.12, 2nd June 2006
 * @since 0.1
 */
public class RelationComponent extends JComponent implements DiagramComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to the normal width of a relation line.
	 */
	public static final float RELATION_LINEWIDTH = 1.0f; // 72 = 1 inch at 72
															// dpi

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
			BasicStroke.JOIN_ROUND);

	/**
	 * Constant defining our 1:1 stroke.
	 */
	public static final Stroke ONE_ONE = new BasicStroke(
			RelationComponent.RELATION_LINEWIDTH * 2.0f, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);

	private Shape shape;

	private Diagram diagram;

	private Relation relation;

	private Object state;

	/**
	 * The constructor constructs an object around a given object, and
	 * associates with a given display.
	 */
	public RelationComponent(Relation relation, Diagram diagram) {
		super();
		this.relation = relation;
		this.diagram = diagram;
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		this.recalculateDiagramComponent();
		this.updateAppearance();
	}

	public Map getSubComponents() {
		// We have no sub-components.
		return Collections.EMPTY_MAP;
	}

	public void updateAppearance() {
		DiagramContext mod = this.getDiagram().getDiagramContext();
		if (mod != null)
			mod.customiseAppearance(this, this.getObject());
		this.setBackground(this.getForeground());
	}

	public void recalculateDiagramComponent() {
		// Nothing to do, but we have a tool-tip so update that instead.
		this.setToolTipText(this.relation.getName());
	}

	public Object getState() {
		return this.state;
	}

	public void setState(Object state) {
		this.state = state;
	}

	/**
	 * Returns the diagram component representing the primary key end of this
	 * relation.
	 * 
	 * @return the diagram component for the PK end.
	 */
	public KeyComponent getPrimaryKeyComponent() {
		return (KeyComponent) this.diagram.getDiagramComponent(this.relation
				.getPrimaryKey());
	}

	/**
	 * Returns the diagram component representing the foreign key end of this
	 * relation.
	 * 
	 * @return the diagram component for the FK end.
	 */
	public KeyComponent getForeignKeyComponent() {
		return (KeyComponent) this.diagram.getDiagramComponent(this.relation
				.getForeignKey());
	}

	public Diagram getDiagram() {
		return this.diagram;
	}

	public Object getObject() {
		return this.relation;
	}

	private Relation getRelation() {
		return this.relation;
	}

	/**
	 * Sets the shape for us to display the outline of. This will usually be a
	 * line, however it's up to the layout manager entirely.
	 * 
	 * @param shape
	 *            the shape this relation should take on screen.
	 */
	public void setShape(Shape shape) {
		Stroke stroke = this.getStroke();
		this.shape = stroke.createStrokedShape(shape);
	}

	public boolean contains(int x, int y) {
		// Clicks are on us if they are within a certain distance
		// of the stroked shape.
		return this.shape != null
				&& this.shape.intersects(new Rectangle2D.Double(x
						- RelationComponent.RELATION_LINEWIDTH, y
						- RelationComponent.RELATION_LINEWIDTH,
						RelationComponent.RELATION_LINEWIDTH * 2,
						RelationComponent.RELATION_LINEWIDTH * 2));
	}

	public JPopupMenu getContextMenu() {
		JPopupMenu contextMenu = new JPopupMenu();
		// No additional entries for us yet.
		// Return it.
		return contextMenu;
	}

	protected void processMouseEvent(MouseEvent evt) {
		boolean eventProcessed = false;

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Build the basic menu.
			JPopupMenu contextMenu = this.getContextMenu();

			// Customise it using the diagram context.
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

	private Stroke getStroke() {
		if (this.getRelation().getFKCardinality().equals(Cardinality.MANY))
			return RelationComponent.ONE_MANY;
		else
			return RelationComponent.ONE_ONE;
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		// Do painting of this component.
		g2d.setColor(this.getForeground());
		g2d.setStroke(this.getStroke());
		g2d.draw(this.shape);
		g2d.setColor(this.getBackground());
		g2d.fill(this.shape);
		// Clean up.
		g2d.dispose();
	}
}
