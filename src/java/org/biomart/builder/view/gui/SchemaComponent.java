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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.Key;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * A diagram component that represents a schema. It usually only has a label in
 * it, but if the schema has any external relations, then the tables with those
 * relations will appear in full using {@link TableComponent}s.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 2nd June 2006
 * @since 0.1
 */
public class SchemaComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constructs a schema diagram component in the given diagram that displays
	 * details of a particular schema.
	 * 
	 * @param schema
	 *            the schema to display details of.
	 * @param diagram
	 *            the diagram to display the details in.
	 */
	public SchemaComponent(Schema schema, Diagram diagram) {
		super(schema, diagram);

		// Schema components are set out in a vertical list.
		this.setLayout(new GridLayout(0, 1));

		// Calculate the components and add them to the list.
		this.recalculateDiagramComponent();
	}

	public void recalculateDiagramComponent() {
		// Remove all our components.
		this.removeAll();

		// Set the background colour.
		this.setBackground(Color.PINK);

		// Add the label for the schema name,
		JLabel label = new JLabel(this.getSchema().getName());
		label.setFont(Font.decode("Serif-BOLD-10"));
		this.add(label);

		// Is it a group?
		if (this.getSchema() instanceof SchemaGroup) {
			// Construct a string containing the names of all the child schemas.
			StringBuffer sb = new StringBuffer();
			sb.append(BuilderBundle.getString("schemaGroupContains"));
			for (Iterator i = ((SchemaGroup) this.getSchema()).getSchemas()
					.iterator(); i.hasNext();) {
				Schema s = (Schema) i.next();
				sb.append(s.getName());
				if (i.hasNext())
					sb.append(", ");
			}

			// Make a label containing these names.
			label = new JLabel(sb.toString());
			label.setFont(Font.decode("Serif-BOLDITALIC-10"));
			this.add(label);
		}

		// Now add any tables with external relations. Loop through the
		// external keys to identify the tables to do this.
		for (Iterator i = this.getSchema().getExternalKeys().iterator(); i
				.hasNext();) {
			Key key = (Key) i.next();
			Table table = key.getTable();

			// Only add the table if it's not already added!
			if (!this.getSubComponents()[0].contains(table)) {

				// Create the table component that represents this table.
				TableComponent tableComponent = new TableComponent(table, this
						.getDiagram());

				// Remember, internally, the subcomponents of this table, as
				// well
				// as the table itself as a subcomponent.
				this.addSubComponent(table, tableComponent);
				this.getSubComponents()[0].addAll(
						tableComponent.getSubComponents()[0]);
				this.getSubComponents()[1].addAll(
						tableComponent.getSubComponents()[1]);

				// Add the table component to our layout.
				this.add(tableComponent);
			}
		}
	}

	private Schema getSchema() {
		return (Schema) this.getObject();
	}

	private SchemaGroup getSchemaGroup() {
		return (SchemaGroup) this.getObject();
	}

	/**
	 * Count the external relations in this schema.
	 * 
	 * @return the number of external relations in this schema.
	 */
	public int countExternalRelations() {
		return this.getSchema().getExternalRelations().size();
	}

	public JPopupMenu getContextMenu() {
		// To obtain the base context menu for this schema object, we
		// need to know if it is a group or not.
		if (this.getObject() instanceof SchemaGroup)
			return this.getGroupContextMenu();
		else
			return this.getSingleContextMenu(this.getSchema());
	}

	private JPopupMenu getSingleContextMenu(final Schema schema) {
		// First of all, work out what would have been shown by default.
		JPopupMenu contextMenu = super.getContextMenu();

		// Add the 'show tables' option, which opens the tab representing
		// this schema.
		JMenuItem showTables = new JMenuItem(BuilderBundle
				.getString("showTablesTitle"));
		showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic")
				.charAt(0));
		showTables.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int index = getDiagram().getDataSetTabSet().getSchemaTabSet()
						.indexOfTab(schema.getName());
				getDiagram().getDataSetTabSet().getSchemaTabSet()
						.setSelectedIndex(index);
			}
		});
		contextMenu.add(showTables);

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}

	private JPopupMenu getGroupContextMenu() {
		// First of all, work out what would have been shown by default.
		JPopupMenu contextMenu = super.getContextMenu();

		// Add the 'show tables' option, which opens the tab representing
		// this schema.
		JMenuItem showTables = new JMenuItem(BuilderBundle
				.getString("showTablesTitle"));
		showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic")
				.charAt(0));
		showTables.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int index = getDiagram().getDataSetTabSet().getSchemaTabSet()
						.indexOfTab(getSchemaGroup().getName());
				getDiagram().getDataSetTabSet().getSchemaTabSet()
						.setSelectedIndex(index);
			}
		});
		contextMenu.add(showTables);

		// Create a submenu containing all the members of the group. Each one
		// of these will have their own submenu providing the usual functions
		// available as if they had schema objects which had been clicked on
		// directly in the diagram.
		JMenu groupMembers = new JMenu(BuilderBundle
				.getString("groupMembersTitle"));
		groupMembers.setMnemonic(BuilderBundle
				.getString("groupMembersMnemonic").charAt(0));
		contextMenu.add(groupMembers);

		// Loop through the schemas in the group.
		for (Iterator i = this.getSchemaGroup().getSchemas().iterator(); i
				.hasNext();) {
			final Schema schema = (Schema) i.next();

			// Name the menu after the schema.
			JMenu schemaMenu = new JMenu(schema.getName());

			// Rename the schema within the group.
			JMenuItem renameM = new JMenuItem(BuilderBundle
					.getString("renameSchemaTitle"));
			renameM.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic")
					.charAt(0));
			renameM.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDiagram().getDataSetTabSet().getSchemaTabSet()
							.requestRenameSchema(schema, getSchemaGroup());
				}
			});
			schemaMenu.add(renameM);

			// Modify the schema.
			JMenuItem modifyM = new JMenuItem(BuilderBundle
					.getString("modifySchemaTitle"));
			modifyM.setMnemonic(BuilderBundle.getString("modifySchemaMnemonic")
					.charAt(0));
			modifyM.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDiagram().getDataSetTabSet().getSchemaTabSet()
							.requestModifySchema(schema);
				}
			});
			schemaMenu.add(modifyM);

			// Test the schema.
			JMenuItem testM = new JMenuItem(BuilderBundle
					.getString("testSchemaTitle"));
			testM.setMnemonic(BuilderBundle.getString("testSchemaMnemonic")
					.charAt(0));
			testM.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDiagram().getDataSetTabSet().getSchemaTabSet()
							.requestTestSchema(schema);
				}
			});
			schemaMenu.add(testM);

			// Remove the schema from the group and reinstate as an individual
			// schema.
			JMenuItem unGroup = new JMenuItem(BuilderBundle
					.getString("ungroupMemberTitle"));
			unGroup.setMnemonic(BuilderBundle
					.getString("ungroupMemberMnemonic").charAt(0));
			unGroup.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDiagram().getDataSetTabSet().getSchemaTabSet()
							.requestRemoveSchemaFromSchemaGroup(schema,
									getSchemaGroup());
				}
			});
			schemaMenu.add(unGroup);

			// Add the submenu to the main menu.
			groupMembers.add(schemaMenu);
		}

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}
}
