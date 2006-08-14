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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.BoxShapedComponent;
import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * This context applies to the general schema view, as seen when a dataset tab
 * has been selected. It allows dataset-specific things such as masked relations
 * to be set up, where those things have to be defined against the source schema
 * rather than the dataset's generated schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.22, 14th August 2006
 * @since 0.1
 */
public class WindowContext extends SchemaContext {
	private DataSet dataset;

	/**
	 * Creates a new context within a given set of tabs, which applies to a
	 * specific dataset. All menu options will apply to this dataset, and
	 * operations working with these datasets will be delegated to the methods
	 * specified in the tabset.
	 * 
	 * @param martTab
	 *            the mart tab that the schema window appears within.
	 * @param dataset
	 *            the dataset we are attached to.
	 */
	public WindowContext(final MartTab martTab, final DataSet dataset) {
		super(martTab);
		this.dataset = dataset;
	}

	/**
	 * Obtain the dataset that this context is linked with.
	 * 
	 * @return our dataset.
	 */
	public DataSet getDataSet() {
		return this.dataset;
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

		// This menu applies to the background area (null) of the window,
		// plus all Schema objects displayed in it. In other words, the
		// background behaves in the same way as the Schema objects in it.
		if (object == null || object instanceof Schema) {

			// None, yet.

		}

		// This menu is attached to all table objects.
		else if (object instanceof Table) {
			// Add a separator if there's other stuff before us.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Obtain the table object we should refer to.
			final Table table = (Table) object;

			// The mask option allows the user to mask all
			// relations on a table.
			final JMenuItem mask = new JMenuItem(Resources
					.get("maskTableTitle"));
			mask.setMnemonic(Resources.get("maskTableMnemonic").charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					WindowContext.this
							.getMartTab()
							.getDataSetTabSet()
							.requestMaskTable(WindowContext.this.dataset, table);
				}
			});
			contextMenu.add(mask);

			// Show the first 10 rows on a table.
			final JMenuItem showTen = new JMenuItem(Resources
					.get("showFirstTenRowsTitle"));
			showTen.setMnemonic(Resources.get("showFirstTenRowsMnemonic")
					.charAt(0));
			showTen.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					WindowContext.this.getMartTab().getSchemaTabSet()
							.requestShowRows(table, 0, 10);
				}
			});
			contextMenu.add(showTen);

			// If it's a restricted table...
			if (this.dataset.getRestrictedTables().contains(table)) {

				// Option to modify restriction.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifyTableRestrictionTitle"));
				modify.setMnemonic(Resources.get(
						"modifyTableRestrictionMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestModifyTableRestriction(
										WindowContext.this.dataset,
										table,
										WindowContext.this.dataset
												.getRestrictedTableType(table));
					}
				});
				contextMenu.add(modify);

				// Option to remove restriction.
				final JMenuItem remove = new JMenuItem(Resources
						.get("removeTableRestrictionTitle"));
				remove.setMnemonic(Resources.get(
						"removeTableRestrictionMnemonic").charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestRemoveTableRestriction(
										WindowContext.this.dataset, table);
					}
				});
				contextMenu.add(remove);

			} else {

				// Add a table restriction.
				final JMenuItem restriction = new JMenuItem(Resources
						.get("addTableRestrictionTitle"));
				restriction.setMnemonic(Resources.get(
						"addTableRestrictionMnemonic").charAt(0));
				restriction.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestAddTableRestriction(
										WindowContext.this.dataset, table);
					}
				});
				contextMenu.add(restriction);
			}
		}

		// This menu is attached to all the relation lines in the schema.
		else if (object instanceof Relation) {

			// Add a separator if there's other stuff before us.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Obtain the relation object we should refer to.
			final Relation relation = (Relation) object;

			// Work out what state the relation is already in.
			final boolean incorrect = relation.getStatus().equals(
					ComponentStatus.INFERRED_INCORRECT);
			final boolean relationMasked = this.dataset.getMaskedRelations()
					.contains(relation);
			final boolean relationConcated = this.dataset
					.getConcatOnlyRelations().contains(relation);
			final boolean relationSubclassed = this.dataset
					.getSubclassedRelations().contains(relation);

			// The mask/unmask option allows the user to mask/unmask a relation.
			final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(Resources
					.get("maskRelationTitle"));
			mask.setMnemonic(Resources.get("maskRelationMnemonic").charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (mask.isSelected())
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestMaskRelation(
										WindowContext.this.dataset, relation);
					else
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestUnmaskRelation(
										WindowContext.this.dataset, relation);
				}
			});
			contextMenu.add(mask);
			if (incorrect)
				mask.setEnabled(false);
			if (relationMasked)
				mask.setSelected(true);

			// The subclass/unsubclass option allows subclassing, but is
			// only selectable when the relation is unmasked and not
			// incorrect or already flagged as being in any conflicting state.
			final JCheckBoxMenuItem subclass = new JCheckBoxMenuItem(Resources
					.get("subclassRelationTitle"));
			subclass.setMnemonic(Resources.get("subclassRelationMnemonic")
					.charAt(0));
			subclass.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (subclass.isSelected())
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestSubclassRelation(
										WindowContext.this.dataset, relation);
					else
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestUnsubclassRelation(
										WindowContext.this.dataset, relation);
				}
			});
			contextMenu.add(subclass);
			if (relationSubclassed)
				subclass.setSelected(true);
			if (incorrect || relation.isOneToOne() || relationMasked
					|| relationConcated)
				subclass.setEnabled(false);

			// If it's a concat column...
			if (this.dataset.getConcatOnlyRelations().contains(relation)) {

				// Option to modify concat.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifyConcatRelationTitle"));
				modify.setMnemonic(Resources
						.get("modifyConcatRelationMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestModifyConcatOnlyRelation(
										WindowContext.this.dataset,
										relation,
										WindowContext.this.dataset
												.getConcatRelationType(relation));
					}
				});
				contextMenu.add(modify);

				// Option to remove concat.
				final JMenuItem remove = new JMenuItem(Resources
						.get("removeConcatRelationTitle"));
				remove.setMnemonic(Resources
						.get("removeConcatRelationMnemonic").charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestUnconcatOnlyRelation(
										WindowContext.this.dataset, relation);
					}
				});
				contextMenu.add(remove);

			} else {

				// Add a relation concat.
				final JMenuItem concat = new JMenuItem(Resources
						.get("addConcatRelationTitle"));
				concat.setMnemonic(Resources.get("addConcatRelationMnemonic")
						.charAt(0));
				concat.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestCreateConcatOnlyRelation(
										WindowContext.this.dataset, relation);
					}
				});
				contextMenu.add(concat);
			}

			// If it's a restricted column...
			if (this.dataset.getRestrictedRelations().contains(relation)) {

				// Option to modify restriction.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifyRelationRestrictionTitle"));
				modify.setMnemonic(Resources.get(
						"modifyRelationRestrictionMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestModifyRelationRestriction(
										WindowContext.this.dataset,
										relation,
										WindowContext.this.dataset
												.getRestrictedRelationType(relation));
					}
				});
				contextMenu.add(modify);

				// Option to remove restriction.
				final JMenuItem remove = new JMenuItem(Resources
						.get("removeRelationRestrictionTitle"));
				remove.setMnemonic(Resources.get(
						"removeRelationRestrictionMnemonic").charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestRemoveRelationRestriction(
										WindowContext.this.dataset, relation);
					}
				});
				contextMenu.add(remove);

			} else {

				// Add a relation restriction.
				final JMenuItem restriction = new JMenuItem(Resources
						.get("addRelationRestrictionTitle"));
				restriction.setMnemonic(Resources.get(
						"addRelationRestrictionMnemonic").charAt(0));
				restriction.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						WindowContext.this.getMartTab().getDataSetTabSet()
								.requestAddRelationRestriction(
										WindowContext.this.dataset, relation);
					}
				});
				contextMenu.add(restriction);
			}
		}

		// This submenu applies when keys are clicked on.
		else if (object instanceof Key) {
			// Keys simply show the menu for the table they are in.
			final Table table = ((Key) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}

		// This submenu applies when columns are clicked on.
		else if (object instanceof Column) {
			// Columns simply show the menu for the table they are in.
			final Table table = ((Column) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {

		// This bit adds a restricted outline to restricted tables.
		if (object instanceof Table) {
			final Table table = (Table) object;
			if (this.dataset.getRestrictedTables().contains(table))
				((TableComponent) component)
						.setStroke(BoxShapedComponent.RESTRICTED_OUTLINE);
			else
				((TableComponent) component)
						.setStroke(BoxShapedComponent.NORMAL_OUTLINE);

		}

		// This section customises the appearance of relation lines within
		// the window schema diagram.
		if (object instanceof Relation) {

			// Work out what relation we are dealing with.
			final Relation relation = (Relation) object;

			// Fade out all INFERRED_INCORRECT and MASKED relations.
			if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
					|| this.dataset.getMaskedRelations().contains(relation))
				component.setForeground(RelationComponent.MASKED_COLOUR);

			// Highlight CONCAT-ONLY relations.
			else if (this.dataset.getConcatOnlyRelations().contains(relation))
				component.setForeground(RelationComponent.CONCAT_COLOUR);

			// Highlight SUBCLASS relations.
			else if (this.dataset.getSubclassedRelations().contains(relation))
				component.setForeground(RelationComponent.SUBCLASS_COLOUR);

			// All others are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);

			// Do the stroke.
			final RelationComponent relcomp = (RelationComponent) component;
			if (this.dataset.getRestrictedRelations().contains(relation)) {
				if (relation.isOneToOne())
					relcomp.setStroke(RelationComponent.ONE_ONE_RESTRICTED);
				else if (relation.isManyToMany())
					relcomp.setStroke(RelationComponent.MANY_MANY_RESTRICTED);
				else
					relcomp.setStroke(RelationComponent.ONE_MANY_RESTRICTED);
			} else {
				if (relation.isOneToOne())
					relcomp.setStroke(RelationComponent.ONE_ONE);
				else if (relation.isManyToMany())
					relcomp.setStroke(RelationComponent.MANY_MANY);
				else
					relcomp.setStroke(RelationComponent.ONE_MANY);
			}
		}

		// This section customises the appearance of key objects within
		// table objects in the diagram.
		else if (object instanceof Key) {

			// Work out what key we are dealing with.
			final Key key = (Key) object;

			// Fade out all INFERRED_INCORRECT keys.
			if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				component.setForeground(KeyComponent.MASKED_COLOUR);

			// Highlight all HANDMADE keys.
			else if (key.getStatus().equals(ComponentStatus.HANDMADE))
				component.setForeground(KeyComponent.HANDMADE_COLOUR);

			// All others are normal.
			else
				component.setForeground(KeyComponent.NORMAL_COLOUR);

			// Remove drag-and-drop from the key as it does not apply in
			// the window context.
			component.removeMouseListener(SchemaContext.dragAdapter);
		}
	}
}
