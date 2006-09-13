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

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.Key;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.diagrams.Diagram;

/**
 * A diagram component that represents a schema. It usually only has a label in
 * it, but if the schema has any external relations, then the tables with those
 * relations will appear in full using {@link TableComponent}s.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.14, 29th August 2006
 * @since 0.1
 */
public class SchemaComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	/**
	 * Constant defining background colour.
	 */
	public static Color BACKGROUND_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Bold font.
	 */
	public static Font BOLD_FONT = Font.decode("SansSerif-BOLD-10");

	/**
	 * Bold italic font.
	 */
	public static Font BOLDITALIC_FONT = Font.decode("SansSerif-PLAIN-10");

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	/**
	 * Constructs a schema diagram component in the given diagram that displays
	 * details of a particular schema.
	 * 
	 * @param schema
	 *            the schema to display details of.
	 * @param diagram
	 *            the diagram to display the details in.
	 */
	public SchemaComponent(final Schema schema, final Diagram diagram) {
		super(schema, diagram);

		// Schema components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each field.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(5, 5, 5, 5);

		// Calculate the components and add them to the list.
		this.recalculateDiagramComponent();
	}

	private JPopupMenu getGroupContextMenu() {
		// First of all, work out what would have been shown by default.
		final JPopupMenu contextMenu = super.getContextMenu();

		// Add a divider if necessary.
		if (contextMenu.getComponentCount() > 0)
			contextMenu.addSeparator();

		// Add the 'show tables' option, which opens the tab representing
		// this schema.
		final JMenuItem showTables = new JMenuItem(Resources
				.get("showTablesTitle"));
		showTables.setMnemonic(Resources.get("showTablesMnemonic").charAt(0));
		showTables.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final int index = SchemaComponent.this
						.getDiagram()
						.getMartTab()
						.getSchemaTabSet()
						.indexOfTab(
								SchemaComponent.this.getSchemaGroup().getName());
				SchemaComponent.this.getDiagram().getMartTab()
						.getSchemaTabSet().setSelectedIndex(index);
			}
		});
		contextMenu.add(showTables);

		contextMenu.addSeparator();

		// Create a submenu containing all the members of the group. Each one
		// of these will have their own submenu providing the usual functions
		// available as if they had schema objects which had been clicked on
		// directly in the diagram.
		final JMenu groupMembers = new JMenu(Resources.get("groupMembersTitle"));
		groupMembers.setMnemonic(Resources.get("groupMembersMnemonic")
				.charAt(0));
		contextMenu.add(groupMembers);

		// Loop through the schemas in the group.
		for (final Iterator i = this.getSchemaGroup().getSchemas().iterator(); i
				.hasNext();) {
			final Schema schema = (Schema) i.next();

			// Name the menu after the schema.
			final JMenu schemaMenu = new JMenu(schema.getName());

			// Rename the schema within the group.
			final JMenuItem renameM = new JMenuItem(Resources
					.get("renameSchemaTitle"));
			renameM
					.setMnemonic(Resources.get("renameSchemaMnemonic")
							.charAt(0));
			renameM.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaComponent.this.getDiagram().getMartTab()
							.getSchemaTabSet().requestRenameSchema(schema,
									SchemaComponent.this.getSchemaGroup());
				}
			});
			schemaMenu.add(renameM);

			// Modify the schema.
			final JMenuItem modifyM = new JMenuItem(Resources
					.get("modifySchemaTitle"));
			modifyM
					.setMnemonic(Resources.get("modifySchemaMnemonic")
							.charAt(0));
			modifyM.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaComponent.this.getDiagram().getMartTab()
							.getSchemaTabSet().requestModifySchema(schema);
				}
			});
			schemaMenu.add(modifyM);

			// Test the schema.
			final JMenuItem testM = new JMenuItem(Resources
					.get("testSchemaTitle"));
			testM.setMnemonic(Resources.get("testSchemaMnemonic").charAt(0));
			testM.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaComponent.this.getDiagram().getMartTab()
							.getSchemaTabSet().requestTestSchema(schema);
				}
			});
			schemaMenu.add(testM);

			// Divide the ungroup option from the others.
			schemaMenu.addSeparator();

			// Remove the schema from the group and reinstate as an individual
			// schema.
			final JMenuItem unGroup = new JMenuItem(Resources
					.get("ungroupMemberTitle"));
			unGroup.setMnemonic(Resources.get("ungroupMemberMnemonic")
					.charAt(0));
			unGroup.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaComponent.this.getDiagram().getMartTab()
							.getSchemaTabSet()
							.requestRemoveSchemaFromSchemaGroup(schema,
									SchemaComponent.this.getSchemaGroup());
				}
			});
			schemaMenu.add(unGroup);

			// Add the submenu to the main menu.
			groupMembers.add(schemaMenu);
		}

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}

	private Schema getSchema() {
		return (Schema) this.getObject();
	}

	private SchemaGroup getSchemaGroup() {
		return (SchemaGroup) this.getObject();
	}

	private JPopupMenu getSingleContextMenu(final Schema schema) {
		// First of all, work out what would have been shown by default.
		final JPopupMenu contextMenu = super.getContextMenu();

		// Add the 'show tables' option, which opens the tab representing
		// this schema.
		final JMenuItem showTables = new JMenuItem(Resources
				.get("showTablesTitle"));
		showTables.setMnemonic(Resources.get("showTablesMnemonic").charAt(0));
		showTables.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final int index = SchemaComponent.this.getDiagram()
						.getMartTab().getSchemaTabSet().indexOfTab(
								schema.getName());
				SchemaComponent.this.getDiagram().getMartTab()
						.getSchemaTabSet().setSelectedIndex(index);
			}
		});
		contextMenu.add(showTables);

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
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

	public void recalculateDiagramComponent() {
		// Remove all our components.
		this.removeAll();

		// Set the background colour.
		this.setBackground(SchemaComponent.BACKGROUND_COLOUR);

		// Add the label for the schema name,
		JLabel label = new JLabel(this.getSchema().getName());
		label.setFont(SchemaComponent.BOLD_FONT);
		this.layout.setConstraints(label, this.constraints);
		this.add(label);

		// Is it a group?
		if (this.getSchema() instanceof SchemaGroup) {
			// Add a 'contains' label.
			label = new JLabel(Resources.get("schemaGroupContains"));
			label.setFont(SchemaComponent.BOLDITALIC_FONT);
			this.layout.setConstraints(label, this.constraints);
			this.add(label);

			// Add a label for each member of the group.
			for (final Iterator i = ((SchemaGroup) this.getSchema())
					.getSchemas().iterator(); i.hasNext();) {
				final Schema s = (Schema) i.next();
				label = new JLabel(s.getName());
				label.setFont(SchemaComponent.BOLDITALIC_FONT);
				this.layout.setConstraints(label, this.constraints);
				this.add(label);
			}
		}

		// Now add any tables with external relations. Loop through the
		// external keys to identify the tables to do this.
		for (final Iterator i = this.getSchema().getExternalKeys().iterator(); i
				.hasNext();) {
			final Key key = (Key) i.next();
			final Table table = key.getTable();

			// Only add the table if it's not already added!
			if (!this.getSubComponents().containsKey(table)) {

				// Create the table component that represents this table.
				final TableComponent tableComponent = new TableComponent(table,
						this.getDiagram());

				// Remember, internally, the subcomponents of this table, as
				// well as the table itself as a subcomponent.
				this.addSubComponent(table, tableComponent);
				this.getSubComponents().putAll(
						tableComponent.getSubComponents());

				// Add the table component to our layout.
				this.layout.setConstraints(tableComponent, this.constraints);
				this.add(tableComponent);
			}
		}
	}
}
