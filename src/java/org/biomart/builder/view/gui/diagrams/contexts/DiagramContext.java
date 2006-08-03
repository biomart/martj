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

package org.biomart.builder.view.gui.diagrams.contexts;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * The diagram context received notification to populate context menus in
 * org.biomart.builder.view.gui.diagrams, or to change the colours of objects
 * displayed in the diagram. All objects in the diagram are passed to both
 * methods at some point, so anything displayed can be customised.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.9, 16th May 2006
 * @since 0.1
 */
public interface DiagramContext {
	/**
	 * Add items to a context menu for a given component. Must add separator
	 * first if the menu is not empty.
	 * 
	 * @param contextMenu
	 *            the context menu to add parameters to.
	 * @param object
	 *            the model object we wish to customise this menu to.
	 */
	public void populateContextMenu(JPopupMenu contextMenu, Object object);

	/**
	 * Customise the appearance of a component that represents the given model
	 * object.
	 * 
	 * @param component
	 *            the component that represents the object.
	 * @param object
	 *            the model object we wish to customise this component to.
	 */
	public void customiseAppearance(JComponent component, Object object);
}
