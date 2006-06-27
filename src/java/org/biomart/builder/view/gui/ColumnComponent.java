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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

import org.biomart.builder.model.Column;

/**
 * This simple component represents a single column within a table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 27th June 2006
 * @since 0.1
 */
public class ColumnComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	private GridBagLayout layout;

	private GridBagConstraints constraints;

	/**
	 * Constant referring to normal column colour.
	 */
	public static final Color NORMAL_COLOUR = Color.DARK_GRAY;

	/**
	 * Constant referring to faded column colour.
	 */
	public static final Color FADED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to partitioned column colour.
	 */
	public static final Color PARTITIONED_COLOUR = Color.BLUE;

	/**
	 * The constructor creates a new column component representing the given
	 * column. The diagram the column component is part of is also required.
	 * 
	 * @param column
	 *            the column to represent graphically.
	 * @param diagram
	 *            the diagram to display it in.
	 */
	public ColumnComponent(Column column, Diagram diagram) {
		super(column, diagram);

		// Column components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each field.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 1, 0, 2);

		// Calculate the diagram.
		this.recalculateDiagramComponent();
	}

	public void recalculateDiagramComponent() {
		// Remove everything.
		this.removeAll();

		// Set the background.
		this.setBackground(Color.ORANGE);

		// Add the label for the column name.
		JLabel label = new JLabel(this.getColumn().getName());
		label.setFont(Font.decode("Serif-ITALIC-10"));
		this.layout.setConstraints(label, this.constraints);
		this.add(label);
	}

	private Column getColumn() {
		return (Column) this.getObject();
	}
}
