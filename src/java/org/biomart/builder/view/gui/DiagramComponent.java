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

import java.util.Map;

import javax.swing.JPopupMenu;

/**
 * An element that can be drawn on a diagram. It can provide a context menu for
 * itself, the diagram it belongs to, its current state (and allow its state to
 * be set), and a list of any components it may contain inside that are of
 * interest to the diagram.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 15th May 2006
 * @since 0.1
 */
public interface DiagramComponent {
	/**
	 * Retrieves the diagram this component belongs to.
	 * 
	 * @return the diagram.
	 */
	public Diagram getDiagram();

	/**
	 * Retrieves the model object this component is a representation of.
	 * 
	 * @return the model object.
	 */
	public Object getObject();

	/**
	 * Construct a context menu for the model object.
	 * 
	 * @return the popup menu.
	 */
	public JPopupMenu getContextMenu();

	/**
	 * Updates the appearance of this component, usually by setting colours.
	 * This may often be handled by delegating calls to a {@link DiagramContext}.
	 */
	public void updateAppearance();

	/**
	 * The current state of the component is returned by this. States are
	 * arbitrary and can be null. States can be set by using
	 * {@link #setState(Object)}
	 * 
	 * @return the current state.
	 */
	public Object getState();

	/**
	 * Sets the current state of the component. See {@link #getState()}.
	 * 
	 * @param state
	 *            the new state for the component.
	 */
	public void setState(Object state);

	/**
	 * This method is called when the component needs to rethink its contents
	 * and layout.
	 */
	public void recalculateDiagramComponent();

	/**
	 * Returns a map of inner components inside the diagram. The keys are model
	 * object references, and the values are the diagram components representing
	 * them inside the current diagram component. This is useful for instance
	 * when wanting to obtain key components for a table.
	 * 
	 * @return the map of inner components.
	 */
	public Map getSubComponents();
}
