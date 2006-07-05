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
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Any diagram component that is box-shaped is derived from this class. It
 * handles all mouse-clicks and painting problems for them, and keeps track of
 * their sub-components in map, so that code can reference them by model object
 * rather than exact component.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.12, 5th July 2006
 * @since 0.1
 */
public abstract class BoxShapedComponent extends JPanel implements
		DiagramComponent {
	private Diagram diagram;

	private Object object;

	// OK to use map, as the components are recreated, not changed.
	private Map subComponents = new HashMap();

	private Object state;

	/**
	 * Constructs a box-shaped component around the given model object to be
	 * represented in the given diagram.
	 * 
	 * @param object
	 *            the model object to represent.
	 * @param diagram
	 *            the diagram to display ourselves in.
	 */
	public BoxShapedComponent(Object object, Diagram diagram) {
		super();

		// Remember settings.
		this.object = object;
		this.diagram = diagram;

		// Turn on the mouse.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);

		// Make sure we're not transparent.
		this.setOpaque(true);
		
		// Repaint ourselves.
		this.updateAppearance();
	}

	public abstract void recalculateDiagramComponent();

	public Map getSubComponents() {
		return this.subComponents;
	}

	/**
	 * Adds a sub-component to the map, but not to the diagram.
	 * 
	 * @param object
	 *            the model object the component represents.
	 * @param component
	 *            the component representing the model object.
	 */
	protected void addSubComponent(Object object, DiagramComponent component) {
		this.subComponents.put(object, component);
	}

	public Object getState() {
		return this.state;
	}

	public void setState(Object state) {
		this.state = state;
	}

	public void updateAppearance() {
		DiagramContext mod = this.getDiagram().getDiagramContext();
		if (mod != null)
			mod.customiseAppearance(this, this.getObject());

		// Draws a border.
		this.setBorder(BorderFactory.createLineBorder(this.getForeground()));
	}

	public Diagram getDiagram() {
		return this.diagram;
	}

	public Object getObject() {
		return this.object;
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

	public int hashCode() {
		return this.getObject().hashCode();
	}

	public boolean equals(Object obj) {
		return (obj instanceof DiagramComponent)
				&& ((DiagramComponent) obj).getObject()
						.equals(this.getObject());
	}
}
