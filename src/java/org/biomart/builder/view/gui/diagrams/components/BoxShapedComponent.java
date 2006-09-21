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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;

/**
 * Any diagram component that is box-shaped is derived from this class. It
 * handles all mouse-clicks and painting problems for them, and keeps track of
 * their sub-components in map, so that code can reference them by model object
 * rather than exact component.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author$
 * @since 0.1
 */
public abstract class BoxShapedComponent extends JPanel implements
		DiagramComponent {

	private static final float BOX_DASHSIZE = 7.0f; // 72 = 1 inch

	private static final float BOX_DOTSIZE = 3.0f; // 72 = 1 inch

	private static final float BOX_LINEWIDTH = 1.0f; // 72 = 1 inch

	private static final float BOX_MITRE_TRIM = 10.0f; // 72 = 1 inch

	private static final Stroke DOTTED_OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM,
			new float[] { BoxShapedComponent.BOX_DASHSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE }, 0);

	private static final Stroke OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM);

	private Diagram diagram;

	private boolean dotted = false;

	private Object object;

	private RenderingHints renderHints;

	private Object state;

	private Stroke stroke;

	// OK to use map, as the components are recreated, not changed.
	private final Map subComponents = new HashMap();

	/**
	 * Constructs a box-shaped component around the given model object to be
	 * represented in the given diagram.
	 * 
	 * @param object
	 *            the model object to represent.
	 * @param diagram
	 *            the diagram to display ourselves in.
	 */
	public BoxShapedComponent(final Object object, final Diagram diagram) {
		super();

		// Remember settings.
		this.object = object;
		this.diagram = diagram;

		// Turn on the mouse.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);

		// Make sure we're not transparent.
		this.setOpaque(true);

		// Set-up rendering hints.
		this.renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		this.renderHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		// Repaint ourselves.
		this.updateAppearance();
	}

	/**
	 * Adds a sub-component to the map, but not to the diagram.
	 * 
	 * @param object
	 *            the model object the component represents.
	 * @param component
	 *            the component representing the model object.
	 */
	protected void addSubComponent(final Object object,
			final DiagramComponent component) {
		this.subComponents.put(object, component);
	}

	protected void paintBorder(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setStroke(this.stroke);
		super.paintBorder(g2d);
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.isPopupTrigger()) {
			// Build the basic menu.
			final JPopupMenu contextMenu = this.getContextMenu();
			// Customise the context menu for this box's model object.
			if (this.getDiagram().getDiagramContext() != null)
				this.getDiagram().getDiagramContext().populateContextMenu(
						contextMenu, this.getObject());
			// Display.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.show(this, evt.getX(), evt.getY());
			// Mark as handled.
			eventProcessed = true;
		}
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
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

	public Object getObject() {
		return this.object;
	}

	public Object getState() {
		return this.state;
	}

	public Map getSubComponents() {
		return this.subComponents;
	}

	public int hashCode() {
		return this.getObject().hashCode();
	}

	public void paintComponent(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHints(this.renderHints);
		super.paintComponent(g2d);
	}

	public abstract void recalculateDiagramComponent();

	public void repaintDiagramComponent() {
		this.repaint(this.getVisibleRect());
	}

	/**
	 * If this is set to <tt>true</tt> then the component will appear with a
	 * dotted/dashed outline.
	 * 
	 * @param dotted
	 *            <tt>true</tt> if the component is to appear with a dotted
	 *            outline. The default is <tt>false</tt>.
	 */
	public void setDotted(final boolean dotted) {
		this.dotted = dotted;
	}

	public void setState(final Object state) {
		this.state = state;
	}

	public void updateAppearance() {
		final DiagramContext mod = this.getDiagram().getDiagramContext();
		if (mod != null)
			mod.customiseAppearance(this, this.getObject());
		if (this.dotted)
			this.stroke = BoxShapedComponent.DOTTED_OUTLINE;
		else
			this.stroke = BoxShapedComponent.OUTLINE;
		this.setBorder(BorderFactory.createLineBorder(this.getForeground()));
	}
}
