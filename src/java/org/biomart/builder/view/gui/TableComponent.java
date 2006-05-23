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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Table components are box-shaped, and represent an individual table. Inside
 * them may appear a number of key or column components, and a button which
 * shows or hides the columns. They have a label indicating their full name, and
 * a secondary label indicating which schema they belong to.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 22nd May 2006
 * @since 0.1
 */
public class TableComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	private JButton showHide;

	private JComponent columnsListPanel;

	/**
	 * Colour for subclassed tables (in the dataset context).
	 */
	public static final Color SUBCLASS_COLOUR = Color.RED;

	/**
	 * Colour for subclassed tables (in the dataset context).
	 */
	public static final Color DIMENSION_COLOUR = Color.BLUE;

	/**
	 * Colour for all non-subclassed, non-dimension tables.
	 */
	public static final Color NORMAL_COLOUR = Color.BLACK;

	/**
	 * This constructor makes a new table component, associated with a
	 * particular table, and remembers that this component appears in a
	 * particular diagram. All operations on the component will be related back
	 * to that diagram where necessary.
	 * 
	 * @param table
	 *            the table we wish to represent in the diagram.
	 * @param diagram
	 *            the diagram we wish to make the table appear in.
	 */
	public TableComponent(Table table, Diagram diagram) {
		super(table, diagram);

		// Organise ourselves in a vertical box.
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		// Draw our contents.
		this.recalculateDiagramComponent();
	}

	/**
	 * Gets the table this component is representing.
	 * 
	 * @return the table we represent.
	 */
	private Table getTable() {
		return (Table) this.getObject();
	}

	public void recalculateDiagramComponent() {
		// Remove all our existing components.
		this.removeAll();

		// Set the background to pink.
		this.setBackground(Color.PINK);

		// Add the table name label.
		JLabel label = new JLabel(this.getTable().getName());
		label.setFont(Font.decode("Serif-BOLD-10"));
		this.add(label);

		// Add the schema name label below.
		label = new JLabel(this.getTable().getSchema().getName());
		label.setFont(Font.decode("Serif-PLAIN-10"));
		this.add(label);

		// Add a key component as a sub-component of this table
		// for each of the keys in the table.
		for (Iterator i = this.getTable().getKeys().iterator(); i.hasNext();) {
			Key key = (Key) i.next();
			KeyComponent keyComponent = new KeyComponent(key, this.getDiagram());

			// Add it as a sub-component (internal representation only).
			this.addSubComponent(key, keyComponent);

			// Physically add it to the table component layout.
			this.add(keyComponent);
		}

		// Now the columns, as a vertical list.
		this.columnsListPanel = Box.createVerticalBox();

		// Add columns to the list one by one, as column sub-components.
		for (Iterator i = this.getTable().getColumns().iterator(); i.hasNext();) {
			Column col = (Column) i.next();
			ColumnComponent colComponent = new ColumnComponent(col, this
					.getDiagram());

			// Add it as a sub-component (internal representation only).
			this.addSubComponent(col, colComponent);

			// Physically add it to the list of columns.
			columnsListPanel.add(colComponent);
		}

		// Show/hide the columns panel with a button.
		this.showHide = new JButton(BuilderBundle
				.getString("showColumnsButton"));
		this.showHide.setFont(Font.decode("Serif-BOLD-10"));
		this.add(showHide);
		showHide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (getState().equals(Boolean.TRUE))
					setState(Boolean.FALSE);
				else
					setState(Boolean.TRUE);
				// Recalculate the diagram.
				getDiagram().recalculateDiagram();
				// Zoom to this table so that the user doesn't get lost.
				getDiagram().findObject(getTable());
			}
		});

		// Set our initial display state as false, which means columns are
		// hidden.
		this.setState(Boolean.FALSE);
	}

	public void setState(Object state) {
		// For us, state is TRUE if we want the columns panel hidden, FALSE if
		// not.
		if (state != null && state.equals(Boolean.TRUE)) {
			// If the state has changed from FALSE to TRUE, show the columns
			// and change the button to 'hide columns'.
			if (this.getState() == null
					|| !this.getState().equals(Boolean.TRUE))
				this.add(this.columnsListPanel);
			this.showHide.setText(BuilderBundle.getString("hideColumnsButton"));
		} else {
			// If the state has changed from TRUE to FALSE, hide the columns
			// and change the button to 'show columns'.
			if (this.getState() != null && this.getState().equals(Boolean.TRUE))
				this.remove(this.columnsListPanel);
			this.showHide.setText(BuilderBundle.getString("showColumnsButton"));
		}

		// Delegate upwards, so that the state is remembered for later.
		super.setState(state);
	}

	/**
	 * Count the internal relations attached to our table. Internal relations
	 * are those that refer at both ends to tables in the same schema as each
	 * other.
	 * 
	 * @return the number of internal relations linked to this table.
	 */
	public int countInternalRelations() {
		return this.getTable().getInternalRelations().size();
	}
}
