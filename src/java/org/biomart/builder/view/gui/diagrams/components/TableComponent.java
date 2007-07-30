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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * Table components are box-shaped, and represent an individual table. Inside
 * them may appear a number of key or column components, and a button which
 * shows or hides the columns. They have a label indicating their full name, and
 * a secondary label indicating which schema they belong to.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class TableComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	private static final Color BACKGROUND_COLOUR = Color.PINK;

	/**
	 * Border colour for all unpartitioned tables.
	 */
	public static Color NORMAL_COLOUR = Color.BLACK;
	
	/**
	 * Border colour for all ignored tables.
	 */
	public static Color IGNORE_COLOUR = Color.RED;

	/**
	 * Border colour for masked tables.
	 */
	public static Color MASKED_COLOUR = Color.LIGHT_GRAY;

	private static final Font ITALIC_FONT = Font.decode("SansSerif-ITALIC-10");

	private static final Font BOLD_FONT = Font.decode("SansSerif-BOLD-10");

	private JComponent columnsListPanel;

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	private JButton showHide;

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
	public TableComponent(final Table table, final Diagram diagram) {
		super(table, diagram);

		// Table components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each component within the table component.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 2, 0, 2);

		// Set the background colour.
		this.setBackground(TableComponent.BACKGROUND_COLOUR);

		// Draw our contents.
		this.recalculateDiagramComponent();
	}

	/**
	 * Gets the table this component is representing.
	 * 
	 * @return the table we represent.
	 */
	public Table getTable() {
		return (Table) this.getObject();
	}

	public void recalculateDiagramComponent() {
		// Remove all our existing components.
		this.removeAll();

		// Add the table name label.
		final JTextField name = new JTextField();
		name.setFont(TableComponent.BOLD_FONT);
		this.setRenameTextField(name);
		this.layout.setConstraints(name, this.constraints);
		this.add(name);

		// Add the schema name label.
		final JLabel label = new JLabel(this.getTable().getSchema().getName());
		label.setFont(TableComponent.ITALIC_FONT);
		this.layout.setConstraints(label, this.constraints);
		this.add(label);

		// Add a key component as a sub-component of this table,
		// for each of the foreign keys in the table.
		for (final Iterator i = this.getTable().getKeys().iterator(); i
				.hasNext();) {
			final Key key = (Key) i.next();
			final KeyComponent keyComponent = new KeyComponent(key, this
					.getDiagram());

			// Add it as a sub-component (internal representation only).
			this.addSubComponent(key, keyComponent);
			this.getSubComponents().putAll(keyComponent.getSubComponents());

			// Physically add it to the table component layout.
			this.layout.setConstraints(keyComponent, this.constraints);
			this.add(keyComponent);
		}

		// Now the columns, as a vertical list in their own panel.
		this.columnsListPanel = new JPanel();
		final GridBagLayout columnsListPanelLayout = new GridBagLayout();
		this.columnsListPanel.setLayout(columnsListPanelLayout);

		// Do a bit of sorting to make them alphabetical first.
		final Map sortedColMap = new TreeMap();
		for (final Iterator i = this.getTable().getColumns().iterator(); i
				.hasNext();) {
			final Column col = (Column) i.next();
			sortedColMap.put(
					col instanceof DataSetColumn ? ((DataSetColumn) col)
							.getModifiedName() : col.getName(), col);
		}

		// GridBagLayout has a maximum number of components (512).
		if (sortedColMap.size() <= 500)
			// Add columns to the list one by one, as column sub-components.
			for (final Iterator i = sortedColMap.values().iterator(); i
					.hasNext();) {
				final Column col = (Column) i.next();
				final ColumnComponent colComponent = new ColumnComponent(col,
						this.getDiagram());

				// Add it as a sub-component (internal representation only).
				this.addSubComponent(col, colComponent);
				this.getSubComponents().putAll(colComponent.getSubComponents());

				// Physically add it to the list of columns.
				columnsListPanelLayout.setConstraints(colComponent,
						this.constraints);
				this.columnsListPanel.add(colComponent);
			}
		else
			this.columnsListPanel.add(new JLabel(Resources.get(
					"tooManyColsToDisplay", "500")));
		this.layout.setConstraints(this.columnsListPanel, this.constraints);

		// Show/hide the columns panel with a button.
		this.showHide = new JButton(Resources.get("showColumnsButton"));
		this.showHide.setFont(TableComponent.BOLD_FONT);
		this.layout.setConstraints(this.showHide, this.constraints);
		this.add(this.showHide);
		this.showHide.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (TableComponent.this.getState().equals(Boolean.TRUE))
					TableComponent.this.setState(Boolean.FALSE);
				else
					TableComponent.this.setState(Boolean.TRUE);
				// Recalculate the diagram.
				TableComponent.this.getDiagram().revalidate();
			}
		});

		// Set our initial display state as false, which means columns are
		// hidden.
		this.setState(Boolean.FALSE);
	}

	public void performRename(final String newName) {
		this.getDiagram().getMartTab().getDataSetTabSet()
				.requestRenameDataSetTable((DataSetTable) this.getTable(),
						newName);
	}

	public String getEditableName() {
		return this.getTable() instanceof DataSetTable ? ((DataSetTable) this
				.getTable()).getModifiedName() : this.getTable().getName();
	}

	public String getName() {
		final Table table = this.getTable();
		final StringBuffer name = new StringBuffer();
		if (table != null && table instanceof DataSetTable) {
			final String originalName = this.getTable().getName();
			final String modifiedName = this.getEditableName();
			name.append(modifiedName);
			if (!modifiedName.equals(originalName)) {
				name.append(" (");
				name.append(originalName);
				name.append(')');
			}
		} else
			name.append(this.getEditableName());
		return name.toString();
	}

	public void setState(final Object state) {
		// For us, state is TRUE if we want the columns panel visible.
		if (state != null && state.equals(Boolean.TRUE)) {
			if (this.getState() != null
					&& this.getState().equals(Boolean.FALSE))
				this.add(this.columnsListPanel);
			this.showHide.setText(Resources.get("hideColumnsButton"));
		} else {
			if (this.getState() != null && this.getState().equals(Boolean.TRUE))
				this.remove(this.columnsListPanel);
			this.showHide.setText(Resources.get("showColumnsButton"));
		}

		// Delegate upwards, so that the state is remembered for later.
		super.setState(state);
	}
}
