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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This context applies to the general schema view, as seen when a dataset tab
 * has been selected. It allows dataset-specific things such as masked relations
 * to be set up, where those things have to be defined against the source schema
 * rather than the dataset's generated schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.12, 16th May 2006
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
	 * @param datasetTabSet
	 *            the dataset tabset that the schema window appears within.
	 * @param dataset
	 *            the dataset we are attached to.
	 */
	public WindowContext(DataSetTabSet datasetTabSet, DataSet dataset) {
		super(datasetTabSet);
		this.dataset = dataset;
	}

	/**
	 * Obtain the dataset that this context is linked with.
	 * 
	 * @return our dataset.
	 */
	protected DataSet getDataSet() {
		return this.dataset;
	}

	public void populateContextMenu(JPopupMenu contextMenu, Object object) {

		// This menu applies to the background area (null) of the window,
		// plus all Schema objects displayed in it. In other words, the
		// background behaves in the same way as the Schema objects in it.
		if (object == null || (object instanceof Schema)) {

			// Add a separator if there's other stuff before us.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// A menu item which allows the schema to be optimised.
			JMenuItem optimise = new JMenuItem(BuilderBundle
					.getString("optimiseDataSetTitle"));
			optimise.setMnemonic(BuilderBundle.getString(
					"optimiseDataSetMnemonic").charAt(0));
			optimise.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDataSetTabSet().requestOptimiseDataSet(dataset);
				}
			});
			contextMenu.add(optimise);
		}

		// This menu is attached to all the relation lines in the schema.
		else if (object instanceof Relation) {

			// Add a separator if there's other stuff before us.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Obtain the relation object we should refer to.
			final Relation relation = (Relation) object;

			// Work out what state the relation is already in.
			boolean incorrect = relation.getStatus().equals(
					ComponentStatus.INFERRED_INCORRECT);
			boolean relationOneToOne = relation.getFKCardinality().equals(
					Cardinality.ONE);
			boolean relationMasked = this.dataset.getMaskedRelations()
					.contains(relation);
			boolean relationConcated = this.dataset.getConcatOnlyRelations()
					.contains(relation);
			boolean relationSubclassed = this.dataset.getSubclassedRelations()
					.contains(relation);

			// The mask/unmask option allows the user to mask/unmask a relation.
			final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(BuilderBundle
					.getString("maskRelationTitle"));
			mask.setMnemonic(BuilderBundle.getString("maskRelationMnemonic")
					.charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (mask.isSelected())
						getDataSetTabSet().requestMaskRelation(dataset,
								relation);
					else
						getDataSetTabSet().requestUnmaskRelation(dataset,
								relation);
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
			final JCheckBoxMenuItem subclass = new JCheckBoxMenuItem(
					BuilderBundle.getString("subclassRelationTitle"));
			subclass.setMnemonic(BuilderBundle.getString(
					"subclassRelationMnemonic").charAt(0));
			subclass.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (subclass.isSelected())
						getDataSetTabSet().requestSubclassRelation(dataset,
								relation);
					else
						getDataSetTabSet().requestUnsubclassRelation(dataset,
								relation);
				}
			});
			contextMenu.add(subclass);
			if (relationSubclassed)
				subclass.setSelected(true);
			if (incorrect || relationOneToOne || relationMasked
					|| relationConcated)
				subclass.setEnabled(false);

			// The concat-only submenu allows concat-only relations.
			JMenu concatSubmenu = new JMenu(BuilderBundle
					.getString("concatOnlyRelationTitle"));
			concatSubmenu.setMnemonic(BuilderBundle.getString(
					"concatOnlyRelationMnemonic").charAt(0));
			ButtonGroup concatGroup = new ButtonGroup();

			// This item in the concat-only relation submenu turns concat-only
			// relations off.
			JRadioButtonMenuItem none = new JRadioButtonMenuItem(BuilderBundle
					.getString("noneConcatTitle"));
			none.setMnemonic(BuilderBundle.getString("noneConcatMnemonic")
					.charAt(0));
			none.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDataSetTabSet().requestUnconcatOnlyRelation(dataset,
							relation);
				}
			});
			concatGroup.add(none);
			concatSubmenu.add(none);
			if (this.dataset.getConcatRelationType(relation) == null)
				none.setSelected(true);

			// This item in the concat-only relation submenu turns concat-only
			// relations into ones separated by commas.
			JRadioButtonMenuItem comma = new JRadioButtonMenuItem(BuilderBundle
					.getString("commaConcatTitle"));
			comma.setMnemonic(BuilderBundle.getString("commaConcatMnemonic")
					.charAt(0));
			comma.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDataSetTabSet().requestConcatOnlyRelation(dataset,
							relation, ConcatRelationType.COMMA);
				}
			});
			concatGroup.add(comma);
			concatSubmenu.add(comma);
			if (this.dataset.getConcatRelationType(relation) != null
					&& dataset.getConcatRelationType(relation).equals(
							ConcatRelationType.COMMA))
				comma.setSelected(true);

			// This item in the concat-only relation submenu turns concat-only
			// relations into ones separated by tabs.
			JRadioButtonMenuItem tab = new JRadioButtonMenuItem(BuilderBundle
					.getString("tabConcatTitle"));
			tab.setMnemonic(BuilderBundle.getString("tabConcatMnemonic")
					.charAt(0));
			tab.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDataSetTabSet().requestConcatOnlyRelation(dataset,
							relation, ConcatRelationType.TAB);
				}
			});
			concatGroup.add(tab);
			concatSubmenu.add(tab);
			if (this.dataset.getConcatRelationType(relation) != null
					&& dataset.getConcatRelationType(relation).equals(
							ConcatRelationType.TAB))
				tab.setSelected(true);

			// This item in the concat-only relation submenu turns concat-only
			// relations into ones separated by spaces.
			JRadioButtonMenuItem space = new JRadioButtonMenuItem(BuilderBundle
					.getString("spaceConcatTitle"));
			space.setMnemonic(BuilderBundle.getString("spaceConcatMnemonic")
					.charAt(0));
			space.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getDataSetTabSet().requestConcatOnlyRelation(dataset,
							relation, ConcatRelationType.SPACE);
				}
			});
			concatGroup.add(space);
			concatSubmenu.add(space);
			if (this.dataset.getConcatRelationType(relation) != null
					&& dataset.getConcatRelationType(relation).equals(
							ConcatRelationType.SPACE))
				space.setSelected(true);

			// Attach the concat-only submenu to the main context menu.
			// It is only usable when the relation is unmasked and not
			// incorrect or already flagged as being in any conflicting state.
			// contextMenu.add(concatSubmenu);
			if (incorrect || relationOneToOne || relationMasked
					|| relationSubclassed)
				concatSubmenu.setEnabled(false);
		}

		// This submenu applies when keys are clicked on.
		else if (object instanceof Key) {
			// Keys simply show the menu for the table they are in.
			Table table = ((Key) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}

		// This submenu applies when columns are clicked on.
		else if (object instanceof Column) {
			// Columns simply show the menu for the table they are in.
			Table table = ((Column) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}
	}

	public void customiseAppearance(JComponent component, Object object) {

		// This section customises the appearance of relation lines within
		// the window schema diagram.
		if (object instanceof Relation) {

			// Work out what relation we are dealing with.
			Relation relation = (Relation) object;

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
		}

		// This section customises the appearance of key objects within
		// table objects in the diagram.
		else if (object instanceof Key) {

			// Work out what key we are dealing with.
			Key key = (Key) object;

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