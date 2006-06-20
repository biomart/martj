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

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.TransferHandler;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.PrimaryKey;

/**
 * Represents a key by listing out in a set of labels each column in the key.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 20th June 2006
 * @since 0.1
 */
public class KeyComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to normal key colour.
	 */
	public static final Color NORMAL_COLOUR = Color.DARK_GRAY;

	/**
	 * Constant referring to masked key colour.
	 */
	public static final Color MASKED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to incorrect key colour.
	 */
	public static final Color INCORRECT_COLOUR = Color.RED;

	/**
	 * Constant referring to handmade key colour.
	 */
	public static final Color HANDMADE_COLOUR = Color.GREEN;

	/**
	 * The constructor constructs a key component around a given key object, and
	 * associates it with the given display.
	 * 
	 * @param key
	 *            the key to represent.
	 * @param diagram
	 *            the diagram to draw the key on.
	 */
	public KeyComponent(Key key, Diagram diagram) {
		super(key, diagram);

		// Keys lay out their columns vertically.
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		// Calculate the component layout.
		this.recalculateDiagramComponent();

		// Mark ourselves as handling 'draggedKey' events, for
		// drag-and-drop capabilities.
		this.setTransferHandler(new TransferHandler("draggedKey"));
	}

	public void recalculateDiagramComponent() {
		// Removes all columns.
		this.removeAll();

		// Create the background colour.
		if (this.getKey() instanceof PrimaryKey)
			this.setBackground(Color.CYAN);
		else
			this.setBackground(Color.GREEN);

		// Add the labels for each column.
		for (Iterator i = this.getKey().getColumns().iterator(); i.hasNext();) {
			JLabel label = new JLabel(((Column) i.next()).getName());
			label.setFont(Font.decode("Serif-ITALIC-10"));
			this.add(label);
		}
	}

	private Key getKey() {
		return (Key) this.getObject();
	}

	/**
	 * For drag-and-drop, this returns the object that will be dropped onto the
	 * target when drag-and-drop starts from this key.
	 * 
	 * @return the key the user 'picked up' with the mouse.
	 */
	public Key getDraggedKey() {
		return this.getKey();
	}

	/**
	 * For drag-and-drop, this receives an object that has been dragged from
	 * another key, and creates a 1:M relation between the two.
	 * 
	 * @param key
	 *            the key the user dropped on us with the mouse.
	 */
	public void setDraggedKey(Key key) {
		// Refuse to do it to ourselves.
		if (!key.equals(this))
			this.getDiagram().getMartTab().getSchemaTabSet()
					.requestCreateRelation(key, this.getKey());
	}
}
