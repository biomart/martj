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

import javax.swing.JLabel;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.common.model.Column;

/**
 * This simple component represents a single column within a table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class ColumnComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant referring to background colour.
	 */
	public static Color BACKGROUND_COLOUR = Color.ORANGE;

	/**
	 * Constant referring to expression column colour.
	 */
	public static Color EXPRESSION_COLOUR = Color.MAGENTA;

	/**
	 * Constant referring to faded column colour.
	 */
	public static Color FADED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to inherited column colour.
	 */
	public static Color INHERITED_COLOUR = Color.RED;

	/**
	 * Normal font.
	 */
	public static Font NORMAL_FONT = Font.decode("SansSerif-PLAIN-10");

	/**
	 * Italic font.
	 */
	public static Font ITALIC_FONT = Font.decode("SansSerif-ITALIC-10");

	/**
	 * Constant referring to normal column colour.
	 */
	public static Color NORMAL_COLOUR = Color.ORANGE;

	/**
	 * Constant referring to partitioned column colour.
	 */
	public static Color PARTITIONED_COLOUR = Color.BLUE;

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	/**
	 * The constructor creates a new column component representing the given
	 * column. The diagram the column component is part of is also required.
	 * 
	 * @param column
	 *            the column to represent graphically.
	 * @param diagram
	 *            the diagram to display it in.
	 */
	public ColumnComponent(final Column column, final Diagram diagram) {
		super(column, diagram);

		// Column components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each label within the column.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 1, 0, 2);

		// Calculate the diagram.
		this.recalculateDiagramComponent();
	}

	private Column getColumn() {
		return (Column) this.getObject();
	}

	public void recalculateDiagramComponent() {
		// Remove everything.
		this.removeAll();

		// Set the background.
		this.setBackground(ColumnComponent.NORMAL_COLOUR);

		// Add the label for the column name.
		final String name = (this.getColumn() instanceof DataSetColumn) ? ((DataSetColumn) this
				.getColumn()).getModifiedName()
				: this.getColumn().getName();
		JLabel label = new JLabel(name);
		label.setFont(ColumnComponent.NORMAL_FONT);
		this.layout.setConstraints(label, this.constraints);
		this.add(label);

		// Is it a wrapped column? Add the original name too if different.
		if (this.getColumn() instanceof WrappedColumn) {
			final String wrappedName = ((WrappedColumn) this.getColumn())
					.getWrappedColumn().getName();
			if (!this.getColumn().getName().equals(wrappedName)) {
				label = new JLabel("(" + wrappedName + ")");
				label.setFont(ColumnComponent.ITALIC_FONT);
				this.layout.setConstraints(label, this.constraints);
				this.add(label);
			}
		}
	}
}
