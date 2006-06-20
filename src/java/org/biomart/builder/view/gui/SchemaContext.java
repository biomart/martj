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
import org.biomart.builder.resources.BuilderBundle;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * Provides the context menus and colour schemes to use when viewing a schema in
 * its plain vanilla form, ie. not a dataset schema, and not a window from a
 * dataset onto a set of masked relations.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.19, 20th June 2006
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
	public SchemaContext(MartTab martTab) {
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

	public void populateContextMenu(JPopupMenu contextMenu, Object object) {

		// The background area of the diagram has some simple menu items
		// that refer to all schemas.
		if (object == null) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Synchronise all schemas in the mart.
			JMenuItem syncAll = new JMenuItem(BuilderBundle
					.getString("synchroniseAllSchemasTitle"));
			syncAll.setMnemonic(BuilderBundle.getString(
					"synchroniseAllSchemasMnemonic").charAt(0));
			syncAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestSynchroniseAllSchemas();
				}
			});
			contextMenu.add(syncAll);

			// Add a new schema to the mart.
			JMenuItem add = new JMenuItem(BuilderBundle
					.getString("addSchemaTitle"));
			add.setMnemonic(BuilderBundle.getString("addSchemaMnemonic")
					.charAt(0));
			add.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestAddSchema();
				}
			});
			contextMenu.add(add);

		}

		// Table objects have their own menus too.
		else if (object instanceof Table) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Work out what table we are using.
			final Table table = (Table) object;

			// Menu option to create a dataset based around that table.
			JMenuItem create = new JMenuItem(BuilderBundle.getString(
					"createDataSetTitle", table.getName()));
			create.setMnemonic(BuilderBundle.getString("createDataSetMnemonic")
					.charAt(0));
			create.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getDataSetTabSet().requestCreateDataSet(table);
				}
			});
			contextMenu.add(create);

			// Menu option to suggest a bunch of datasets based around that
			// table.
			JMenuItem suggest = new JMenuItem(BuilderBundle.getString(
					"suggestDataSetsTitle", table.getName()));
			suggest.setMnemonic(BuilderBundle.getString(
					"suggestDataSetsMnemonic").charAt(0));
			suggest.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getDataSetTabSet().requestSuggestDataSets(table);
				}
			});
			contextMenu.add(suggest);

			// Separator.
			contextMenu.addSeparator();

			// Menu item to create a primary key. If it already has one, disable
			// the option.
			JMenuItem pk = new JMenuItem(BuilderBundle
					.getString("createPrimaryKeyTitle"));
			pk.setMnemonic(BuilderBundle.getString("createPrimaryKeyMnemonic")
					.charAt(0));
			pk.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestCreatePrimaryKey(table);
				}
			});
			if (table.getPrimaryKey() != null)
				pk.setEnabled(false);
			contextMenu.add(pk);

			// Menu item to create a foreign key.
			JMenuItem fk = new JMenuItem(BuilderBundle
					.getString("createForeignKeyTitle"));
			fk.setMnemonic(BuilderBundle.getString("createForeignKeyMnemonic")
					.charAt(0));
			fk.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestCreateForeignKey(table);
				}
			});
			contextMenu.add(fk);
		}

		// Schema objects have different menus to the background.
		else if (object instanceof Schema) {

			// Add a separator if the menu is not already empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// What schema is this?
			final Schema schema = (Schema) object;

			// Add a checkbox menu item to turn keyguessing on/off.
			final JCheckBoxMenuItem keyguess = new JCheckBoxMenuItem(
					BuilderBundle.getString("enableKeyGuessingTitle"));
			keyguess.setMnemonic(BuilderBundle.getString(
					"enableKeyGuessingMnemonic").charAt(0));
			keyguess.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (keyguess.isSelected())
						martTab.getSchemaTabSet().requestEnableKeyGuessing(
								schema);
					else
						martTab.getSchemaTabSet().requestDisableKeyGuessing(
								schema);
				}
			});
			contextMenu.add(keyguess);
			if (schema.getKeyGuessing())
				keyguess.setSelected(true);

			// Add an option to rename this schema.
			JMenuItem rename = new JMenuItem(BuilderBundle
					.getString("renameSchemaTitle"));
			rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic")
					.charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestRenameSchema(schema, null);
				}
			});
			contextMenu.add(rename);

			// Add an option to synchronise this schema against it's datasource
			// or database.
			JMenuItem sync = new JMenuItem(BuilderBundle
					.getString("synchroniseSchemaTitle"));
			sync.setMnemonic(BuilderBundle.getString(
					"synchroniseSchemaMnemonic").charAt(0));
			sync.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestSynchroniseSchema(schema);
				}
			});
			contextMenu.add(sync);

			// If this schema is NOT a schema group, there are more options!
			if (!(schema instanceof SchemaGroup)) {

				// Option to modify the schema details.
				JMenuItem modify = new JMenuItem(BuilderBundle
						.getString("modifySchemaTitle"));
				modify.setMnemonic(BuilderBundle.getString(
						"modifySchemaMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						martTab.getSchemaTabSet().requestModifySchema(schema);
					}
				});
				contextMenu.add(modify);

				// Option to test the schema to see if it works.
				JMenuItem test = new JMenuItem(BuilderBundle
						.getString("testSchemaTitle"));
				test.setMnemonic(BuilderBundle.getString("testSchemaMnemonic")
						.charAt(0));
				test.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						martTab.getSchemaTabSet().requestTestSchema(schema);
					}
				});
				contextMenu.add(test);

				// Option to remove the schema from the mart.
				JMenuItem remove = new JMenuItem(BuilderBundle
						.getString("removeSchemaTitle"));
				remove.setMnemonic(BuilderBundle.getString(
						"removeSchemaMnemonic").charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						martTab.getSchemaTabSet().requestRemoveSchema(schema);
					}
				});
				contextMenu.add(remove);

				// Option to replicate the schema.
				JMenuItem replicate = new JMenuItem(BuilderBundle
						.getString("replicateSchemaTitle"));
				replicate.setMnemonic(BuilderBundle.getString(
						"replicateSchemaMnemonic").charAt(0));
				replicate.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						martTab.getSchemaTabSet()
								.requestReplicateSchema(schema);
					}
				});
				contextMenu.add(replicate);

				// Option to add the schema to a schema group.
				JMenuItem addToGroup = new JMenuItem(BuilderBundle
						.getString("addToGroupTitle"));
				addToGroup.setMnemonic(BuilderBundle.getString(
						"addToGroupMnemonic").charAt(0));
				addToGroup.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						martTab.getSchemaTabSet()
								.requestAddSchemaToSchemaGroup(schema);
					}
				});
				contextMenu.add(addToGroup);
			}
		}

		// Relations have their own menus too.
		else if (object instanceof Relation) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// What relation is this? And is it correct?
			final Relation relation = (Relation) object;
			boolean relationIncorrect = relation.getStatus().equals(
					ComponentStatus.INFERRED_INCORRECT);

			// Set up a radio group for the cardinality.
			ButtonGroup cardGroup = new ButtonGroup();

			// Set the relation to be 1:1, but only if it is correct.
			JRadioButtonMenuItem oneToOne = new JRadioButtonMenuItem(
					BuilderBundle.getString("oneToOneTitle"));
			oneToOne.setMnemonic(BuilderBundle.getString("oneToOneMnemonic")
					.charAt(0));
			oneToOne.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeRelationCardinality(
							relation, Cardinality.ONE);
				}
			});
			cardGroup.add(oneToOne);
			contextMenu.add(oneToOne);
			if (relationIncorrect)
				oneToOne.setEnabled(false);
			if (relation.isOneToOne())
				oneToOne.setSelected(true);

			// Set the relation to be 1:M, but only if it is correct.
			JRadioButtonMenuItem oneToMany = new JRadioButtonMenuItem(
					BuilderBundle.getString("oneToManyTitle"));
			oneToMany.setMnemonic(BuilderBundle.getString("oneToManyMnemonic")
					.charAt(0));
			oneToMany.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeRelationCardinality(
							relation, Cardinality.MANY);
				}
			});
			cardGroup.add(oneToMany);
			contextMenu.add(oneToMany);
			if (relationIncorrect || !relation.isOneToManyAllowed())
				oneToMany.setEnabled(false);
			if (relation.isOneToMany())
				oneToMany.setSelected(true);

			// Set the relation to be M:M, but only if it is correct.
			JRadioButtonMenuItem manyToMany = new JRadioButtonMenuItem(
					BuilderBundle.getString("manyToManyTitle"));
			manyToMany.setMnemonic(BuilderBundle
					.getString("manyToManyMnemonic").charAt(0));
			manyToMany.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeRelationCardinality(
							relation, Cardinality.MANY);
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
			ButtonGroup correctGroup = new ButtonGroup();

			// Mark relation as correct, but only if not handmade.
			JRadioButtonMenuItem correct = new JRadioButtonMenuItem(
					BuilderBundle.getString("correctRelationTitle"));
			correct.setMnemonic(BuilderBundle.getString(
					"correctRelationMnemonic").charAt(0));
			correct.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeRelationStatus(
							relation, ComponentStatus.INFERRED);
				}
			});
			correctGroup.add(correct);
			contextMenu.add(correct);
			if (relation.getStatus().equals(ComponentStatus.INFERRED))
				correct.setSelected(true);
			else if (relation.getStatus().equals(ComponentStatus.HANDMADE))
				correct.setEnabled(false);

			// Mark relation as incorrect, but only if not handmade.
			JRadioButtonMenuItem incorrect = new JRadioButtonMenuItem(
					BuilderBundle.getString("incorrectRelationTitle"));
			incorrect.setMnemonic(BuilderBundle.getString(
					"incorrectRelationMnemonic").charAt(0));
			incorrect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeRelationStatus(
							relation, ComponentStatus.INFERRED_INCORRECT);
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
			JMenuItem remove = new JMenuItem(BuilderBundle
					.getString("removeRelationTitle"));
			remove.setMnemonic(BuilderBundle
					.getString("removeRelationMnemonic").charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestRemoveRelation(relation);
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
			JMenuItem editkey = new JMenuItem(BuilderBundle
					.getString("editKeyTitle"));
			editkey.setMnemonic(BuilderBundle.getString("editKeyMnemonic")
					.charAt(0));
			editkey.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestEditKey(key);
				}
			});
			contextMenu.add(editkey);

			// Add a checkbox menu item to turn nullability on/off.
			final JCheckBoxMenuItem nullable = new JCheckBoxMenuItem(
					BuilderBundle.getString("nullableForeignKeyTitle"));
			nullable.setMnemonic(BuilderBundle.getString(
					"nullableForeignKeyMnemonic").charAt(0));
			nullable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet()
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
			JMenuItem createrel = new JMenuItem(BuilderBundle
					.getString("createRelationTitle"));
			createrel.setMnemonic(BuilderBundle.getString(
					"createRelationMnemonic").charAt(0));
			createrel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestCreateRelation(key);
				}
			});
			contextMenu.add(createrel);
			if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				createrel.setEnabled(false);

			// Separator.
			contextMenu.addSeparator();

			// Set up a radio group for the correct/incorrect buttons.
			ButtonGroup correctGroup = new ButtonGroup();

			// Mark the key as correct, but not if handmade.
			JRadioButtonMenuItem correct = new JRadioButtonMenuItem(
					BuilderBundle.getString("correctKeyTitle"));
			correct.setMnemonic(BuilderBundle.getString("correctKeyMnemonic")
					.charAt(0));
			correct.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeKeyStatus(key,
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
			JRadioButtonMenuItem incorrect = new JRadioButtonMenuItem(
					BuilderBundle.getString("incorrectKeyTitle"));
			incorrect.setMnemonic(BuilderBundle.getString(
					"incorrectKeyMnemonic").charAt(0));
			incorrect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestChangeKeyStatus(key,
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
			JMenuItem remove = new JMenuItem(BuilderBundle
					.getString("removeKeyTitle"));
			remove.setMnemonic(BuilderBundle.getString("removeKeyMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getSchemaTabSet().requestRemoveKey(key);
				}
			});
			contextMenu.add(remove);
			if (!key.getStatus().equals(ComponentStatus.HANDMADE))
				remove.setEnabled(false);
		}

		// Columns too, finally.
		else if (object instanceof Column) {
			// Columns just show their table menus.
			Table table = ((Column) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}
	}

	public void customiseAppearance(JComponent component, Object object) {
		// Relations get pretty colours if they are incorrect or handmade.
		if (object instanceof Relation) {

			// What relation is this?
			Relation relation = (Relation) object;

			// Fade out all INFERRED_INCORRECT relations.
			if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				component.setForeground(RelationComponent.INCORRECT_COLOUR);

			// Highlight all HANDMADE relations.
			else if (relation.getStatus().equals(ComponentStatus.HANDMADE))
				component.setForeground(RelationComponent.HANDMADE_COLOUR);

			// All others are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);
		}

		// Keys also get pretty colours for being incorrect or handmade.
		else if (object instanceof Key) {

			// What key is this?
			Key key = (Key) object;

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
