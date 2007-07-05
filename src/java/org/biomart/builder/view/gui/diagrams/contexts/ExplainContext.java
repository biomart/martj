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
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.ExplainTransformationDiagram.RealisedRelation;
import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.SchemaComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.model.Column;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * This context applies to the general schema view, as seen via a dataset tab.
 * It allows dataset-specific things such as masked relations to be set up,
 * where those things have to be defined against the source schema rather than
 * the dataset's generated schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class ExplainContext extends SchemaContext {
	private DataSet dataset;

	private DataSetTable datasetTable;

	/**
	 * Creates a new context within a given set of tabs, which applies to a
	 * specific dataset table. All menu options will apply to this dataset, and
	 * operations working with these datasets will be delegated to the methods
	 * specified in the tabset.
	 * 
	 * @param martTab
	 *            the mart tab that the dataset tab appears within.
	 * @param datasetTable
	 *            the dataset table we are attached to.
	 */
	public ExplainContext(final MartTab martTab, final DataSetTable datasetTable) {
		super(martTab);
		this.dataset = (DataSet) datasetTable.getSchema();
		this.datasetTable = datasetTable;
	}

	/**
	 * Creates a new context within a given set of tabs, which applies to a
	 * specific dataset. All menu options will apply to this dataset, and
	 * operations working with these datasets will be delegated to the methods
	 * specified in the tabset.
	 * 
	 * @param martTab
	 *            the mart tab that the dataset tab appears within.
	 * @param dataset
	 *            the dataset we are attached to.
	 */
	public ExplainContext(final MartTab martTab, final DataSet dataset) {
		super(martTab);
		this.dataset = dataset;
		this.datasetTable = null;
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {
		if (object instanceof Relation)
			this.customiseRelationAppearance(component, (Relation) object,
					RealisedRelation.NO_ITERATION);

		// Schema objects.
		else if (object instanceof Schema) {
			((SchemaComponent) component).setRenameable(false);
			((SchemaComponent) component).setSelectable(false);
		}

		// This section customises table objects.
		else if (object instanceof Table) {
			// Fade out UNINCLUDED tables.
			final boolean isFocus = this.datasetTable != null
					&& this.datasetTable.getFocusTable().equals(object);
			final Set included = new HashSet(
					this.datasetTable != null ? this.datasetTable
							.getIncludedRelations() : this.dataset
							.getIncludedRelations());
			included.retainAll(((Table) object).getRelations());
			if (included.isEmpty() && !isFocus)
				component.setForeground(TableComponent.MASKED_COLOUR);
			// All others are normal.
			else
				component.setForeground(TableComponent.NORMAL_COLOUR);

			((TableComponent) component).setRestricted(this.dataset
					.getSchemaModifications().isRestrictedTable(
							this.datasetTable, (Table) object));
		}

		// This section customises the appearance of key objects within
		// table objects in the diagram.
		else if (object instanceof Key) {
			// All are normal.
			component.setForeground(KeyComponent.NORMAL_COLOUR);

			// Remove drag-and-drop from the key as it does not apply in
			// the window context.
			((KeyComponent) component).setDraggable(false);
		}
	}

	/**
	 * See {@link #customiseAppearance(JComponent, Object)} but this applies to
	 * a particular relation iteration.
	 * 
	 * @param component
	 *            See {@link #customiseAppearance(JComponent, Object)}.
	 * @param relation
	 *            the relation.
	 * @param iteration
	 *            the iteration of the relation, or
	 *            {@link RealisedRelation#NO_ITERATION} for all iterations.
	 */
	public void customiseRelationAppearance(final JComponent component,
			final Relation relation, final int iteration) {

		// Is it restricted?
		((RelationComponent) component).setRestricted(this.dataset
				.getSchemaModifications().isRestrictedRelation(
						this.datasetTable, relation)
				&& (iteration == RealisedRelation.NO_ITERATION || this.dataset
						.getSchemaModifications().isRestrictedRelation(
								this.datasetTable, relation, iteration)));

		// Is it compounded?
		((RelationComponent) component).setCompounded(this.dataset
				.getSchemaModifications().isCompoundRelation(this.datasetTable,
						relation));

		// Is it loopback?
		((RelationComponent) component).setLoopback(this.dataset
				.getSchemaModifications().isLoopbackRelation(this.datasetTable,
						relation));

		// Fade out all UNINCLUDED and MASKED relations.
		final boolean included = this.datasetTable == null ? this.dataset
				.getIncludedRelations().contains(relation) : this.datasetTable
				.getIncludedRelations().contains(relation);
		if (!included
				|| this.dataset.getSchemaModifications().isMaskedRelation(
						this.datasetTable, relation))
			component.setForeground(RelationComponent.MASKED_COLOUR);

		// Highlight SUBCLASS relations.
		else if (this.dataset.getSchemaModifications().isSubclassedRelation(
				relation))
			component.setForeground(RelationComponent.SUBCLASS_COLOUR);

		// All others are normal.
		else
			component.setForeground(RelationComponent.NORMAL_COLOUR);

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
		if (object instanceof Relation)
			this.populateRelationContextMenu(contextMenu, (Relation) object,
					RealisedRelation.NO_ITERATION);
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
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestMaskAllRelations(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, table);
				}
			});
			contextMenu.add(mask);

			// The unmask option allows the user to unmask all
			// relations on a table.
			final JMenuItem unmask = new JMenuItem(Resources
					.get("unmaskTableTitle"));
			unmask.setMnemonic(Resources.get("unmaskTableMnemonic").charAt(0));
			unmask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnmaskAllRelations(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, table);
				}
			});
			contextMenu.add(unmask);

			contextMenu.addSeparator();

			// If it's a restricted table...
			if (this.dataset.getSchemaModifications().isRestrictedTable(
					this.datasetTable, table)) {

				// Option to modify restriction.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifyTableRestrictionTitle"), new ImageIcon(
						Resources.getResourceAsURL("filter.gif")));
				modify.setMnemonic(Resources.get(
						"modifyTableRestrictionMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestRestrictTable(
										ExplainContext.this.dataset,
										ExplainContext.this.datasetTable, table);
					}
				});
				contextMenu.add(modify);

			} else {

				// Add a table restriction.
				final JMenuItem restriction = new JMenuItem(Resources
						.get("addTableRestrictionTitle"), new ImageIcon(
						Resources.getResourceAsURL("filter.gif")));
				restriction.setMnemonic(Resources.get(
						"addTableRestrictionMnemonic").charAt(0));
				restriction.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestRestrictTable(
										ExplainContext.this.dataset,
										ExplainContext.this.datasetTable, table);
					}
				});
				contextMenu.add(restriction);
			}

			// Option to remove restriction.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeTableRestrictionTitle"));
			remove.setMnemonic(Resources.get("removeTableRestrictionMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnrestrictTable(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, table);
				}
			});
			contextMenu.add(remove);
			if (!this.dataset.getSchemaModifications().isRestrictedTable(
					this.datasetTable, table))
				remove.setEnabled(false);
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

	/**
	 * See {@link #populateContextMenu(JPopupMenu, Object)} but this applies to
	 * a particular relation iteration.
	 * 
	 * @param contextMenu
	 *            See {@link #populateContextMenu(JPopupMenu, Object)}.
	 * @param relation
	 *            the relation.
	 * @param iteration
	 *            the iteration of the relation, or
	 *            {@link RealisedRelation#NO_ITERATION} for all iterations.
	 */
	public void populateRelationContextMenu(final JPopupMenu contextMenu,
			final Relation relation, final int iteration) {

		// Add a separator if there's other stuff before us.
		if (contextMenu.getComponentCount() > 0)
			contextMenu.addSeparator();

		// Work out what state the relation is already in.
		final boolean incorrect = relation.getStatus().equals(
				ComponentStatus.INFERRED_INCORRECT);
		final boolean relationMasked = this.dataset.getSchemaModifications()
				.isMaskedRelation(this.datasetTable, relation);

		final boolean relationRestricted = this.dataset
				.getSchemaModifications().isRestrictedRelation(
						this.datasetTable, relation)
				&& (iteration == RealisedRelation.NO_ITERATION || this.dataset
						.getSchemaModifications().isRestrictedRelation(
								this.datasetTable, relation, iteration));
		final boolean relationSubclassed = this.dataset
				.getSchemaModifications().isSubclassedRelation(relation);
		final boolean relationCompounded = this.dataset
				.getSchemaModifications().isCompoundRelation(this.datasetTable,
						relation);
		final boolean relationDirectional = this.dataset
				.getSchemaModifications().isDirectionalRelation(
						this.datasetTable, relation);
		final boolean relationForced = this.dataset.getSchemaModifications()
				.isForceIncludeRelation(this.datasetTable, relation);
		final boolean relationIncluded = this.datasetTable == null ? this.dataset
				.getIncludedRelations().contains(relation)
				: this.datasetTable.getIncludedRelations().contains(relation);
		final boolean relationLoopbacked = this.dataset
				.getSchemaModifications().isLoopbackRelation(this.datasetTable,
						relation);

		// The mask/unmask option allows the user to mask/unmask a relation.
		final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(Resources
				.get("maskRelationTitle"));
		mask.setMnemonic(Resources.get("maskRelationMnemonic").charAt(0));
		mask.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				if (mask.isSelected())
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestMaskRelation(ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation);
				else
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnmaskRelation(ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation);
			}
		});
		contextMenu.add(mask);
		if (incorrect || relationCompounded || !relationMasked
				&& !relationIncluded)
			mask.setEnabled(false);
		if (relationMasked)
			mask.setSelected(true);

		// The loopback option allows the user to loopback include a relation
		// that would otherwise only be included once.
		final JCheckBoxMenuItem loopback = new JCheckBoxMenuItem(Resources
				.get("loopbackRelationTitle"));
		loopback.setMnemonic(Resources.get("loopbackRelationMnemonic")
				.charAt(0));
		loopback.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				if (loopback.isSelected())
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestLoopbackRelation(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation);
				else
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnloopbackRelation(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation);
			}
		});
		contextMenu.add(loopback);
		if (incorrect || relationMasked || !relation.isOneToMany()
				&& !relationLoopbacked)
			loopback.setEnabled(false);
		if (relationLoopbacked)
			loopback.setSelected(true);

		// The force option allows the user to forcibly include a relation
		// that would otherwise remain unincluded.
		final JCheckBoxMenuItem force = new JCheckBoxMenuItem(Resources
				.get("forceIncludeRelationTitle"));
		force.setMnemonic(Resources.get("forceIncludeRelationMnemonic").charAt(
				0));
		force.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				if (force.isSelected())
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestForceRelation(ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation);
				else
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnforceRelation(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation);
			}
		});
		contextMenu.add(force);
		if (incorrect || relationMasked || relationIncluded && !relationForced)
			force.setEnabled(false);
		if (relationForced)
			force.setSelected(true);

		// The compound option allows the user to compound a relation.
		final JCheckBoxMenuItem compound = new JCheckBoxMenuItem(Resources
				.get("compoundRelationTitle"));
		compound.setMnemonic(Resources.get("compoundRelationMnemonic")
				.charAt(0));
		compound.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestCompoundRelation(ExplainContext.this.dataset,
								ExplainContext.this.datasetTable, relation);
				compound.setSelected(ExplainContext.this.dataset
						.getSchemaModifications().isCompoundRelation(
								ExplainContext.this.datasetTable, relation));
			}
		});
		contextMenu.add(compound);
		if (incorrect || relationMasked || this.dataset.isPartitionTable())
			compound.setEnabled(false);
		if (relationCompounded)
			compound.setSelected(true);

		// The compound option allows the user to direction a relation.
		final JCheckBoxMenuItem directional = new JCheckBoxMenuItem(Resources
				.get("directionalRelationTitle"));
		directional.setMnemonic(Resources.get("directionalRelationMnemonic")
				.charAt(0));
		directional.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestDirectionalRelation(
								ExplainContext.this.dataset,
								ExplainContext.this.datasetTable, relation);
				directional.setSelected(ExplainContext.this.dataset
						.getSchemaModifications().isDirectionalRelation(
								ExplainContext.this.datasetTable, relation));
			}
		});
		contextMenu.add(directional);
		if (incorrect || relationMasked)
			directional.setEnabled(false);
		if (relationDirectional)
			directional.setSelected(true);

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
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestSubclassRelation(
									ExplainContext.this.dataset, relation);
				else
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnsubclassRelation(
									ExplainContext.this.dataset, relation);
			}
		});
		contextMenu.add(subclass);
		if (incorrect || relationMasked || relation.isOneToOne()
				|| this.datasetTable != null)
			subclass.setEnabled(false);
		if (relationSubclassed)
			subclass.setSelected(true);

		contextMenu.addSeparator();

		// If it's a restricted relation...
		if (relationRestricted) {

			// Option to modify restriction.
			final JMenuItem modify = new JMenuItem(Resources
					.get("modifyRelationRestrictionTitle"), new ImageIcon(
					Resources.getResourceAsURL("filter.gif")));
			modify.setMnemonic(Resources.get(
					"modifyRelationRestrictionMnemonic").charAt(0));
			modify.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestRestrictRelation(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation,
									iteration);
				}
			});
			contextMenu.add(modify);
			if (incorrect || relationMasked)
				modify.setEnabled(false);

		} else {

			// Add a relation restriction.
			final JMenuItem restriction = new JMenuItem(Resources
					.get("addRelationRestrictionTitle"), new ImageIcon(
					Resources.getResourceAsURL("filter.gif")));
			restriction.setMnemonic(Resources.get(
					"addRelationRestrictionMnemonic").charAt(0));
			restriction.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestRestrictRelation(
									ExplainContext.this.dataset,
									ExplainContext.this.datasetTable, relation,
									iteration);
				}
			});
			contextMenu.add(restriction);
		}

		// Option to remove restriction.
		final JMenuItem remove = new JMenuItem(Resources
				.get("removeRelationRestrictionTitle"));
		remove.setMnemonic(Resources.get("removeRelationRestrictionMnemonic")
				.charAt(0));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestUnrestrictRelation(ExplainContext.this.dataset,
								ExplainContext.this.datasetTable, relation,
								iteration);
			}
		});
		contextMenu.add(remove);
		if (!relationRestricted)
			remove.setEnabled(false);
	}
}
