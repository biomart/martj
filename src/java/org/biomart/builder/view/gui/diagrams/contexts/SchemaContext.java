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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.TransferHandler;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;

/**
 * Provides the context menus and colour schemes to use when viewing a schema in
 * its plain vanilla form, ie. not a dataset schema, and not a window from a
 * dataset onto a set of masked relations.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.27, 3rd August 2006
 * @since 0.1
 */
public class SchemaContext implements DiagramContext {
	private MartTab martTab;

	/**
	 * This mouse adapter intercepts clicks on objects and enables them to
	 * initiate drag-and-drop events, if nothing else on the mouse event queue
	 * claims them first.
	 */
	public static MouseAdapter dragAdapter = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			JComponent c = (JComponent) e.getSource();
			TransferHandler handler = c.getTransferHandler();
			handler.exportAsDrag(c, e, TransferHandler.COPY);
		}
	};

	/**
	 * Creates a new context which will pass any menu actions onto the given
	 * dataset tabset.
	 * 
	 * @param martTab
	 *            the mart tab which will receive any menu actions the user
	 *            selects.
	 */
	public SchemaContext(final MartTab martTab) {
		this.martTab = martTab;
	}

	/**
	 * Obtain the mart tab to pass menu events onto.
	 * 
	 * @return the mart tab this context is attached to.
	 */
	protected MartTab getMartTab() {
		return this.martTab;
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

		// The background area of the diagram has some simple menu items
		// that refer to all schemas.
		if (object == null) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Synchronise all schemas in the mart.
			final JMenuItem syncAll = new JMenuItem(Resources
					.get("synchroniseAllSchemasTitle"));
			syncAll.setMnemonic(Resources.get("synchroniseAllSchemasMnemonic")
					.charAt(0));
			syncAll.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestSynchroniseAllSchemas();
				}
			});
			contextMenu.add(syncAll);

			// Add a new schema to the mart.
			final JMenuItem add = new JMenuItem(Resources.get("addSchemaTitle"));
			add.setMnemonic(Resources.get("addSchemaMnemonic").charAt(0));
			add.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestAddSchema();
				}
			});
			contextMenu.add(add);

			// Menu option to suggest a bunch of datasets.
			final JMenuItem suggest = new JMenuItem(Resources
					.get("suggestDataSetsTitle"));
			suggest.setMnemonic(Resources.get("suggestDataSetsMnemonic")
					.charAt(0));
			suggest.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getDataSetTabSet()
							.requestSuggestDataSets(null);
				}
			});
			contextMenu.add(suggest);
		}

		// Table objects have their own menus too.
		else if (object instanceof Table) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Work out what table we are using.
			final Table table = (Table) object;

			// Menu option to suggest a bunch of datasets based around that
			// table.
			final JMenuItem suggest = new JMenuItem(Resources.get(
					"suggestDataSetsTableTitle", table.getName()));
			suggest.setMnemonic(Resources.get("suggestDataSetsTableMnemonic")
					.charAt(0));
			suggest.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getDataSetTabSet()
							.requestSuggestDataSets(table);
				}
			});
			contextMenu.add(suggest);

			// Separator.
			contextMenu.addSeparator();

			// Menu item to create a primary key. If it already has one, disable
			// the option.
			final JMenuItem pk = new JMenuItem(Resources
					.get("createPrimaryKeyTitle"));
			pk.setMnemonic(Resources.get("createPrimaryKeyMnemonic").charAt(0));
			pk.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestCreatePrimaryKey(table);
				}
			});
			if (table.getPrimaryKey() != null)
				pk.setEnabled(false);
			contextMenu.add(pk);

			// Menu item to create a foreign key.
			final JMenuItem fk = new JMenuItem(Resources
					.get("createForeignKeyTitle"));
			fk.setMnemonic(Resources.get("createForeignKeyMnemonic").charAt(0));
			fk.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestCreateForeignKey(table);
				}
			});
			contextMenu.add(fk);

			// Show the first 10 rows on a table.
			final JMenuItem showTen = new JMenuItem(Resources
					.get("showFirstTenRowsTitle"));
			showTen.setMnemonic(Resources.get("showFirstTenRowsMnemonic")
					.charAt(0));
			showTen.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.getMartTab().getSchemaTabSet()
							.requestShowRows(table, 0, 10);
				}
			});
			contextMenu.add(showTen);
		}

		// Schema objects have different menus to the background.
		else if (object instanceof Schema) {

			// Add a separator if the menu is not already empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// What schema is this?
			final Schema schema = (Schema) object;

			// Add a checkbox menu item to turn keyguessing on/off.
			final JCheckBoxMenuItem keyguess = new JCheckBoxMenuItem(Resources
					.get("enableKeyGuessingTitle"));
			keyguess.setMnemonic(Resources.get("enableKeyGuessingMnemonic")
					.charAt(0));
			keyguess.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (keyguess.isSelected())
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestEnableKeyGuessing(schema);
					else
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestDisableKeyGuessing(schema);
				}
			});
			contextMenu.add(keyguess);
			if (schema.getKeyGuessing())
				keyguess.setSelected(true);

			// Add an option to rename this schema.
			final JMenuItem rename = new JMenuItem(Resources
					.get("renameSchemaTitle"));
			rename.setMnemonic(Resources.get("renameSchemaMnemonic").charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestRenameSchema(schema, null);
				}
			});
			contextMenu.add(rename);

			// Add an option to synchronise this schema against it's datasource
			// or database.
			final JMenuItem sync = new JMenuItem(Resources
					.get("synchroniseSchemaTitle"));
			sync.setMnemonic(Resources.get("synchroniseSchemaMnemonic").charAt(
					0));
			sync.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestSynchroniseSchema(schema);
				}
			});
			contextMenu.add(sync);

			// If this schema is NOT a schema group, there are more options!
			if (!(schema instanceof SchemaGroup)) {

				// Option to modify the schema details.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifySchemaTitle"));
				modify.setMnemonic(Resources.get("modifySchemaMnemonic")
						.charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestModifySchema(schema);
					}
				});
				contextMenu.add(modify);

				// Option to test the schema to see if it works.
				final JMenuItem test = new JMenuItem(Resources
						.get("testSchemaTitle"));
				test.setMnemonic(Resources.get("testSchemaMnemonic").charAt(0));
				test.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestTestSchema(schema);
					}
				});
				contextMenu.add(test);

				// Option to remove the schema from the mart.
				final JMenuItem remove = new JMenuItem(Resources
						.get("removeSchemaTitle"));
				remove.setMnemonic(Resources.get("removeSchemaMnemonic")
						.charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestRemoveSchema(schema);
					}
				});
				contextMenu.add(remove);

				// Option to replicate the schema.
				final JMenuItem replicate = new JMenuItem(Resources
						.get("replicateSchemaTitle"));
				replicate.setMnemonic(Resources.get("replicateSchemaMnemonic")
						.charAt(0));
				replicate.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestReplicateSchema(schema);
					}
				});
				contextMenu.add(replicate);

				// Option to add the schema to a schema group.
				final JMenuItem addToGroup = new JMenuItem(Resources
						.get("addToGroupTitle"));
				addToGroup.setMnemonic(Resources.get("addToGroupMnemonic")
						.charAt(0));
				addToGroup.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						SchemaContext.this.martTab.getSchemaTabSet()
								.requestAddSchemaToSchemaGroup(schema);
					}
				});
				contextMenu.add(addToGroup);

				// Menu option to suggest a bunch of datasets.
				final JMenuItem suggest = new JMenuItem(Resources
						.get("suggestDataSetsTitle"));
				suggest.setMnemonic(Resources.get("suggestDataSetsMnemonic")
						.charAt(0));
				suggest.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						SchemaContext.this.martTab.getDataSetTabSet()
								.requestSuggestDataSets(null);
					}
				});
				contextMenu.add(suggest);
			}
		}

		// Relations have their own menus too.
		else if (object instanceof Relation) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// What relation is this? And is it correct?
			final Relation relation = (Relation) object;
			final boolean relationIncorrect = relation.getStatus().equals(
					ComponentStatus.INFERRED_INCORRECT);

			// Set up a radio group for the cardinality.
			final ButtonGroup cardGroup = new ButtonGroup();

			// Set the relation to be 1:1, but only if it is correct.
			final JRadioButtonMenuItem oneToOne = new JRadioButtonMenuItem(
					Resources.get("oneToOneTitle"));
			oneToOne.setMnemonic(Resources.get("oneToOneMnemonic").charAt(0));
			oneToOne.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeRelationCardinality(relation,
									Cardinality.ONE);
				}
			});
			cardGroup.add(oneToOne);
			contextMenu.add(oneToOne);
			if (relationIncorrect)
				oneToOne.setEnabled(false);
			if (relation.isOneToOne())
				oneToOne.setSelected(true);

			// Set the relation to be 1:M, but only if it is correct.
			final JRadioButtonMenuItem oneToMany = new JRadioButtonMenuItem(
					Resources.get("oneToManyTitle"));
			oneToMany.setMnemonic(Resources.get("oneToManyMnemonic").charAt(0));
			oneToMany.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeRelationCardinality(relation,
									Cardinality.MANY);
				}
			});
			cardGroup.add(oneToMany);
			contextMenu.add(oneToMany);
			if (relationIncorrect || !relation.isOneToManyAllowed())
				oneToMany.setEnabled(false);
			if (relation.isOneToMany())
				oneToMany.setSelected(true);

			// Set the relation to be M:M, but only if it is correct.
			final JRadioButtonMenuItem manyToMany = new JRadioButtonMenuItem(
					Resources.get("manyToManyTitle"));
			manyToMany.setMnemonic(Resources.get("manyToManyMnemonic")
					.charAt(0));
			manyToMany.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeRelationCardinality(relation,
									Cardinality.MANY);
				}
			});
			cardGroup.add(manyToMany);
			contextMenu.add(manyToMany);
			if (relationIncorrect || !relation.isManyToManyAllowed())
				manyToMany.setEnabled(false);
			if (relation.isManyToMany())
				manyToMany.setSelected(true);

			// Separator.
			contextMenu.addSeparator();

			// Set up a radio button group for the correct/incorrect options.
			final ButtonGroup correctGroup = new ButtonGroup();

			// Mark relation as correct, but only if not handmade.
			final JRadioButtonMenuItem correct = new JRadioButtonMenuItem(
					Resources.get("correctRelationTitle"));
			correct.setMnemonic(Resources.get("correctRelationMnemonic")
					.charAt(0));
			correct.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeRelationStatus(relation,
									ComponentStatus.INFERRED);
				}
			});
			correctGroup.add(correct);
			contextMenu.add(correct);
			if (relation.getStatus().equals(ComponentStatus.INFERRED))
				correct.setSelected(true);
			else if (relation.getStatus().equals(ComponentStatus.HANDMADE))
				correct.setEnabled(false);

			// Mark relation as incorrect, but only if not handmade.
			final JRadioButtonMenuItem incorrect = new JRadioButtonMenuItem(
					Resources.get("incorrectRelationTitle"));
			incorrect.setMnemonic(Resources.get("incorrectRelationMnemonic")
					.charAt(0));
			incorrect.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeRelationStatus(relation,
									ComponentStatus.INFERRED_INCORRECT);
				}
			});
			correctGroup.add(incorrect);
			contextMenu.add(incorrect);
			if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				incorrect.setSelected(true);
			else if (relation.getStatus().equals(ComponentStatus.HANDMADE))
				incorrect.setEnabled(false);

			// Separator
			contextMenu.addSeparator();

			// Remove the relation from the schema, but only if handmade.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeRelationTitle"));
			remove.setMnemonic(Resources.get("removeRelationMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestRemoveRelation(relation);
				}
			});
			contextMenu.add(remove);
			if (!relation.getStatus().equals(ComponentStatus.HANDMADE))
				remove.setEnabled(false);
		}

		// Keys have menus too.
		else if (object instanceof Key) {
			// First, work out what table this key is on, and add options
			// relating to that table.
			final Table table = ((Key) object).getTable();
			this.populateContextMenu(contextMenu, table);

			// Then work out what key this is.
			final Key key = (Key) object;

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Option to edit an existing key.
			final JMenuItem editkey = new JMenuItem(Resources
					.get("editKeyTitle"));
			editkey.setMnemonic(Resources.get("editKeyMnemonic").charAt(0));
			editkey.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestEditKey(key);
				}
			});
			contextMenu.add(editkey);

			// Add a checkbox menu item to turn nullability on/off.
			final JCheckBoxMenuItem nullable = new JCheckBoxMenuItem(Resources
					.get("nullableForeignKeyTitle"));
			nullable.setMnemonic(Resources.get("nullableForeignKeyMnemonic")
					.charAt(0));
			nullable.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeForeignKeyNullability(
									(ForeignKey) key, nullable.isSelected());
				}
			});
			contextMenu.add(nullable);
			if (!(key instanceof ForeignKey)) {
				nullable.setEnabled(false);
				nullable.setSelected(false);
			} else {
				nullable.setEnabled(true);
				nullable.setSelected(((ForeignKey) key).getNullable());
			}

			// Option to establish a relation between this key and another.
			final JMenuItem createrel = new JMenuItem(Resources
					.get("createRelationTitle"));
			createrel.setMnemonic(Resources.get("createRelationMnemonic")
					.charAt(0));
			createrel.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestCreateRelation(key);
				}
			});
			contextMenu.add(createrel);
			if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				createrel.setEnabled(false);

			// Separator.
			contextMenu.addSeparator();

			// Set up a radio group for the correct/incorrect buttons.
			final ButtonGroup correctGroup = new ButtonGroup();

			// Mark the key as correct, but not if handmade.
			final JRadioButtonMenuItem correct = new JRadioButtonMenuItem(
					Resources.get("correctKeyTitle"));
			correct.setMnemonic(Resources.get("correctKeyMnemonic").charAt(0));
			correct.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeKeyStatus(key,
									ComponentStatus.INFERRED);
				}
			});
			correctGroup.add(correct);
			contextMenu.add(correct);
			if (key.getStatus().equals(ComponentStatus.INFERRED))
				correct.setSelected(true);
			else if (key.getStatus().equals(ComponentStatus.HANDMADE))
				correct.setEnabled(false);

			// Mark the key as incorrect, but not if handmade.
			final JRadioButtonMenuItem incorrect = new JRadioButtonMenuItem(
					Resources.get("incorrectKeyTitle"));
			incorrect.setMnemonic(Resources.get("incorrectKeyMnemonic").charAt(
					0));
			incorrect.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestChangeKeyStatus(key,
									ComponentStatus.INFERRED_INCORRECT);
				}
			});
			correctGroup.add(incorrect);
			contextMenu.add(incorrect);
			if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				incorrect.setSelected(true);
			else if (key.getStatus().equals(ComponentStatus.HANDMADE))
				incorrect.setEnabled(false);

			// Separator
			contextMenu.addSeparator();

			// Remove the key from the table, but only if handmade.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeKeyTitle"));
			remove.setMnemonic(Resources.get("removeKeyMnemonic").charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					SchemaContext.this.martTab.getSchemaTabSet()
							.requestRemoveKey(key);
				}
			});
			contextMenu.add(remove);
			if (!key.getStatus().equals(ComponentStatus.HANDMADE))
				remove.setEnabled(false);
		}

		// Columns too, finally.
		else if (object instanceof Column) {
			// Columns just show their table menus.
			final Table table = ((Column) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {
		// Relations get pretty colours if they are incorrect or handmade.
		if (object instanceof Relation) {

			// What relation is this?
			final Relation relation = (Relation) object;

			// Fade out all INFERRED_INCORRECT relations.
			if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				component.setForeground(RelationComponent.INCORRECT_COLOUR);

			// Highlight all HANDMADE relations.
			else if (relation.getStatus().equals(ComponentStatus.HANDMADE))
				component.setForeground(RelationComponent.HANDMADE_COLOUR);

			// All others are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);

			// Do the stroke.
			final RelationComponent relcomp = (RelationComponent) component;
			if (relation.isOptional()) {
				if (relation.isOneToOne())
					relcomp.setStroke(RelationComponent.ONE_ONE_OPTIONAL);
				else if (relation.isManyToMany())
					relcomp.setStroke(RelationComponent.MANY_MANY_OPTIONAL);
				else
					relcomp.setStroke(RelationComponent.ONE_MANY_OPTIONAL);
			} else if (relation.isOneToOne())
				relcomp.setStroke(RelationComponent.ONE_ONE);
			else if (relation.isManyToMany())
				relcomp.setStroke(RelationComponent.MANY_MANY);
			else
				relcomp.setStroke(RelationComponent.ONE_MANY);
		}

		// Keys also get pretty colours for being incorrect or handmade.
		else if (object instanceof Key) {

			// What key is this?
			final Key key = (Key) object;

			// Fade out all INFERRED_INCORRECT relations.
			if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				component.setForeground(KeyComponent.INCORRECT_COLOUR);

			// Highlight all HANDMADE relations.
			else if (key.getStatus().equals(ComponentStatus.HANDMADE))
				component.setForeground(KeyComponent.HANDMADE_COLOUR);

			// All others are normal.
			else
				component.setForeground(KeyComponent.NORMAL_COLOUR);

			// Add drag-and-drop to all keys here.
			component.addMouseListener(SchemaContext.dragAdapter);
		}
	}
}
