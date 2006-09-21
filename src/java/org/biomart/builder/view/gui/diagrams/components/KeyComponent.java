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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.TransferHandler;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.view.gui.diagrams.Diagram;

/**
 * Represents a key by listing out in a set of labels each column in the key.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author$
 * @since 0.1
 */
public class KeyComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to foreign key colour.
	 */
	public static Color FK_BACKGROUND_COLOUR = Color.YELLOW;

	/**
	 * Constant referring to handmade key colour.
	 */
	public static Color HANDMADE_COLOUR = Color.GREEN;

	/**
	 * Constant referring to incorrect key colour.
	 */
	public static Color INCORRECT_COLOUR = Color.RED;

	/**
	 * Italic font.
	 */
	public static Font ITALIC_FONT = Font.decode("SansSerif-PLAIN-10");

	/**
	 * Constant referring to masked key colour.
	 */
	public static Color MASKED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to normal key colour.
	 */
	public static Color NORMAL_COLOUR = Color.DARK_GRAY;

	/**
	 * Constant referring to primary key colour.
	 */
	public static Color PK_BACKGROUND_COLOUR = Color.CYAN;

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	/**
	 * The constructor constructs a key component around a given key object, and
	 * associates it with the given display.
	 * 
	 * @param key
	 *            the key to represent.
	 * @param diagram
	 *            the diagram to draw the key on.
	 */
	public KeyComponent(final Key key, final Diagram diagram) {
		super(key, diagram);

		// Key components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each field.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 1, 0, 2);

		// Calculate the component layout.
		this.recalculateDiagramComponent();

		// Mark ourselves as handling 'draggedKey' events, for
		// drag-and-drop capabilities.
		this.setTransferHandler(new TransferHandler("draggedKey"));
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

	public void recalculateDiagramComponent() {
		// Removes all columns.
		this.removeAll();

		// Create the background colour.
		if (this.getKey() instanceof PrimaryKey)
			this.setBackground(KeyComponent.PK_BACKGROUND_COLOUR);
		else
			this.setBackground(KeyComponent.FK_BACKGROUND_COLOUR);

		// Add the labels for each column.
		for (final Iterator i = this.getKey().getColumns().iterator(); i
				.hasNext();) {
			final JLabel label = new JLabel(((Column) i.next()).getName());
			label.setFont(KeyComponent.ITALIC_FONT);
			this.layout.setConstraints(label, this.constraints);
			this.add(label);
		}
	}

	/**
	 * For drag-and-drop, this receives an object that has been dragged from
	 * another key, and creates a 1:M relation between the two.
	 * 
	 * @param key
	 *            the key the user dropped on us with the mouse.
	 */
	public void setDraggedKey(final Key key) {
		// Refuse to do it to ourselves.
		if (!key.equals(this))
			this.getDiagram().getMartTab().getSchemaTabSet()
					.requestCreateRelation(key, this.getKey());
	}
}
