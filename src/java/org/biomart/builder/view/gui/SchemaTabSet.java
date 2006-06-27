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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.exceptions.ValidationException;
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
 * <p>
 * This tabset has one tab for the diagram which represents all schemas, and one
 * tab each for each schema in the mart. It provides methods for working with a
 * given schema, such as adding or removing them, or grouping them together. It
 * can update itself based on the schemas in the mart on request.
 * <p>
 * Like a diagram, it can have a {@link DiagramContext} associated with it.
 * Whenever this context changes, all diagrams represented in each of the tabs
 * has the same context applied.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.18, 27th June 2006
 * @since 0.1
 */
public class SchemaTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	// Schema hashcodes change, so we must use a double-list.
	private List[] schemaToDiagram = new List[] { new ArrayList(),
			new ArrayList() };

	private DiagramContext diagramContext;

	private MartTab martTab;

	private AllSchemasDiagram allSchemasDiagram;

	/**
	 * Creates a new set of tabs to represent the schemas in a mart. The mart is
	 * obtained by using methods on the mart tab passed in as a parameter. The
	 * mart tab is the parent tab that this schema tabset will appear inside the
	 * tabs of.
	 * 
	 * @param martTab
	 *            the parent tab this schema tabset will appear inside the tabs
	 *            of.
	 */
	public SchemaTabSet(MartTab martTab) {
		super();

		// Remember the dataset tabset we are shown inside.
		this.martTab = martTab;

		// Add the all-schemas overview tab. This tab displays a diagram
		// in which all schemas appear, linked where necessary by external
		// relations. This diagram could be quite large, so it is held inside
		// a scrollpane.
		this.allSchemasDiagram = new AllSchemasDiagram(this.martTab);
		JScrollPane scroller = new JScrollPane(this.allSchemasDiagram);
		scroller.getViewport().setBackground(
				this.allSchemasDiagram.getBackground());
		this
				.addTab(BuilderBundle.getString("multiSchemaOverviewTab"),
						scroller);

		// Populate the map to hold the relation between schemas and the
		// diagrams
		// representing them.
		this.recalculateSchemaTabs();
	}

	/**
	 * Returns the mart tab that this schema tabset lives inside.
	 * 
	 * @return the parent mart tab.
	 */
	public MartTab getMartTab() {
		return this.martTab;
	}

	/**
	 * Uses the mart to work out what schemas are available, then updates the
	 * tabs that represent the individual schemas to make sure that they show
	 * the same list. Also updates the overview diagram.
	 */
	public void recalculateSchemaTabs() {
		// Add all schemas in the mart that we don't have yet.
		// We work with a copy of the list of schemas else we get
		// concurrent modification exceptions as new ones are added.
		for (Iterator i = this.martTab.getMart().getSchemas().iterator(); i
				.hasNext();) {
			Schema schema = (Schema) i.next();
			if (!this.schemaToDiagram[0].contains(schema))
				this.addSchemaTab(schema);
		}

		// Remove all schemas we have that are no longer in the mart.
		// We work with a copy of the list of schemas else we get
		// concurrent modification exceptions as old ones are removed.
		List ourSchemas = new ArrayList(this.schemaToDiagram[0]);
		for (Iterator i = ourSchemas.iterator(); i.hasNext();) {
			Schema schema = (Schema) i.next();
			if (!this.martTab.getMart().getSchemas().contains(schema))
				this.removeSchemaTab(schema);
		}
		
		// Update the overview diagram.
		this.recalculateOverviewDiagram();
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on the tab which
	 * represents all the schemas in the mart.
	 */
	public void repaintOverviewDiagram() {
		this.allSchemasDiagram.repaintDiagram();
	}

	/**
	 * Causes {@link Diagram#recalculateDiagram()} to be called on the tab which
	 * represents all the schemas in the mart.
	 */
	public void recalculateOverviewDiagram() {
		this.allSchemasDiagram.recalculateDiagram();
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on the tab which
	 * represents the specified schema.
	 * 
	 * @param schema
	 *            the schema to repaint the diagram of.
	 */
	public void repaintSchemaDiagram(Schema schema) {
		int index = this.schemaToDiagram[0].indexOf(schema);
		((Diagram) this.schemaToDiagram[1].get(index)).repaintDiagram();
	}

	/**
	 * Causes {@link Diagram#recalculateDiagram()} to be called on the tab which
	 * represents the specified schema.
	 * 
	 * @param schema
	 *            the schema to recalculate the diagram of.
	 */
	public void recalculateSchemaDiagram(Schema schema) {
		int index = this.schemaToDiagram[0].indexOf(schema);
		((Diagram) this.schemaToDiagram[1].get(index)).recalculateDiagram();
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on all the
	 * individual schema tabs.
	 */
	public void repaintAllSchemaDiagrams() {
		for (Iterator i = this.schemaToDiagram[1].iterator(); i.hasNext();)
			((Diagram) i.next()).repaintDiagram();
	}

	/**
	 * Causes {@link Diagram#recalculateDiagram()} to be called on all the
	 * individual schema tabs.
	 */
	public void recalculateAllSchemaDiagrams() {
		for (Iterator i = this.schemaToDiagram[1].iterator(); i.hasNext();)
			((Diagram) i.next()).recalculateDiagram();
	}

	/**
	 * Asks user to define a new schema, then adds it.
	 */
	public void requestAddSchema() {
		// Pop up a dialog to get the details of the new schema, then
		// obtain a copy of that schema.
		final Schema schema = SchemaManagerDialog.createSchema(this.martTab);

		// If no schema was defined, ignore the request.
		if (schema == null)
			return;

		// In the background, add the schema to ourselves.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Add the schema to the mart, then synchronise it.
					MartBuilderUtils.addSchemaToMart(martTab.getMart(), schema);

					// Synchronise it.
					MartBuilderUtils.synchroniseSchema(schema);

					// If the schema has no internal relations, then maybe
					// we should turn keyguessing on. The user can always
					// turn it off again later. We need to resynchronise the
					// schema after turning it on.
					if (schema.getInternalRelations().size() == 0)
						MartBuilderUtils.enableKeyGuessing(schema);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Create and add the tab representing this schema.
							addSchemaTab(schema);

							// Update the diagram to match the synchronised
							// contents.
							recalculateSchemaDiagram(schema);

							// Set the dataset tabset status as modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks the user which group to add a schema to, or to define a name for a
	 * new group, then adds that schema to the group.
	 * 
	 * @param schema
	 *            the schema to add to a group.
	 */
	public void requestAddSchemaToSchemaGroup(final Schema schema) {
		// Find out what groups already exist and make a list.
		List groupSchemas = new ArrayList();
		for (Iterator i = this.martTab.getMart().getSchemas().iterator(); i
				.hasNext();) {
			Schema groupSchema = (Schema) i.next();
			if (groupSchema instanceof SchemaGroup)
				groupSchemas.add(groupSchema.getName());
		}

		// Add an option to define a new group.
		String newGroupName = BuilderBundle.getString("newSchemaGroup");
		groupSchemas.add(newGroupName);

		// Ask use which group to use.
		String groupName = (String) JOptionPane.showInputDialog(this.martTab
				.getMartTabSet().getMartBuilder(), BuilderBundle
				.getString("requestSchemaGroupName"), BuilderBundle
				.getString("questionTitle"), JOptionPane.QUESTION_MESSAGE,
				null, groupSchemas.toArray(), newGroupName);

		// If they cancelled the choice, cancel the add group request too.
		if (groupName == null)
			return;

		// If they chose 'new group', get the new group name from a second
		// dialog.
		if (groupName.equals(newGroupName)) {
			groupName = (String) JOptionPane.showInputDialog(this.martTab
					.getMartTabSet().getMartBuilder(), BuilderBundle
					.getString("requestNewSchemaGroupName"), BuilderBundle
					.getString("questionTitle"), JOptionPane.QUESTION_MESSAGE);
		}

		// If they cancelled the second dialog, cancel the add group request
		// too.
		if (groupName == null)
			return;

		// Add schema to group
		try {
			// Make sure they actually entered something that wasn't empty.
			if (groupName.trim().length() == 0)
				throw new ValidationException(BuilderBundle
						.getString("schemaGroupNameIsNull"));
			else {
				// Make a final reference to the group name, as we can't make
				// the name itself final, and we can't use non-final variables
				// inside the long process loop below.
				final String groupNameRef = groupName;

				// In the background, add the schema to the group.
				LongProcess.run(new Runnable() {
					public void run() {
						try {
							// Lookup or create the group, add the schema to it,
							// then obtain the group that it was added to.
							final SchemaGroup group = MartBuilderUtils
									.addSchemaToSchemaGroup(martTab.getMart(),
											schema, groupNameRef);

							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									// Remove the tab for the individual schema.
									removeSchemaTab(schema);

									// If the group is a new group, ie. contains
									// only
									// one schema, then add a tab to represent
									// it.
									if (group.getSchemas().size() == 1)
										addSchemaTab(group);
									
									// Recalculate the overview diagram.
									recalculateOverviewDiagram();

									// Some datasets may have referred to the
									// individual
									// schema. As it is no longer individual,
									// they will
									// have been dropped, so the dataset tabset
									// needs to
									// be recalculated.
									martTab.getDataSetTabSet()
											.recalculateDataSetTabs();
									
									// Set the dataset tabset status as
									// modified.
									martTab.getMartTabSet().setModifiedStatus(
											true);
								}
							});
						} catch (final Throwable t) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									martTab.getMartTabSet().getMartBuilder()
											.showStackTrace(t);
								}
							});
						}
					}
				});
			}
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Pops up a dialog with details of the schema, which allows the user to
	 * modify them.
	 * 
	 * @param schema
	 *            the schema to modify.
	 */
	public void requestModifySchema(Schema schema) {
		try {
			// If the user actually made any changes, then synchronise the
			// schema to reflect them.
			if (SchemaManagerDialog.modifySchema(this.martTab, schema))
				this.requestSynchroniseSchema(schema);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Asks user for a name to use, then creates an exact copy of the given
	 * schema, giving the copy the name they chose.
	 * 
	 * @param schema
	 *            the schema to replicate.
	 */
	public void requestReplicateSchema(Schema schema) {
		try {
			// Ask user for the name to use for the copy.
			String newName = this.askUserForSchemaName(schema.getName());

			// No name entered? Or same name entered? Ignore the request.
			if (newName == null || newName.trim().length() == 0
					|| newName.equals(schema.getName()))
				return;

			// Create the replicate.
			Schema newSchema = MartBuilderUtils.replicateSchema(this.martTab
					.getMart(), schema, newName);

			// Add a tab to represent the replicate.
			this.addSchemaTab(newSchema);

			// Set the dataset tabset status as modified.
			this.martTab.getMartTabSet().setModifiedStatus(true);

			// Pop up a dialog to ask the user if they
			// want to change any of the details of the
			// replicated schema.
			this.requestModifySchema(newSchema);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	private void addSchemaTab(Schema schema) {
		// Create the diagram to represent this schema.
		SchemaDiagram schemaDiagram = new SchemaDiagram(this.martTab, schema);

		// Create a scroller to contain the diagram.
		JScrollPane scroller = new JScrollPane(schemaDiagram);
		scroller.getViewport().setBackground(schemaDiagram.getBackground());

		// Add a tab containing the scroller, with the same name as the schema.
		this.addTab(schema.getName(), scroller);

		// Remember which diagram the schema is connected with.
		this.schemaToDiagram[0].add(schema);
		this.schemaToDiagram[1].add(schemaDiagram);

		// Set the current context on the diagram to be the same as the
		// current context on this schema tabset.
		schemaDiagram.setDiagramContext(this.getDiagramContext());

		// Update the all-schemas diagram so that it includes the new
		// schema.
		this.recalculateOverviewDiagram();
		
		// Select the new schema.
		this.setSelectedIndex(this.indexOfComponent(scroller));
		this.martTab.selectSchemaEditor();
	}

	/**
	 * Confirms with user then removes a schema.
	 * 
	 * @param schema
	 *            the schema to remove.
	 */
	public void requestRemoveSchema(final Schema schema) {
		// Confirm if the user really wants to do it.
		int choice = JOptionPane.showConfirmDialog(this, BuilderBundle
				.getString("confirmDelSchema"), BuilderBundle
				.getString("questionTitle"), JOptionPane.YES_NO_OPTION);

		// If they don't, cancel out.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// In the background, remove it.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the schema from the mart.
					MartBuilderUtils.removeSchemaFromMart(martTab.getMart(),
							schema);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Remove the schema tab from the schema tabset.
							removeSchemaTab(schema);

							// Some datasets may have referred to the individual
							// schema. As it no longer exists, they will
							// have been dropped, so the dataset tabset needs to
							// be recalculated.
							martTab.getDataSetTabSet().recalculateDataSetTabs();

							// Recalculate the all-schemas diagram.
							recalculateOverviewDiagram();

							// Set the dataset tabset status as modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Confirms with the user, then removes the schema from the group and
	 * reinstates it as an individual schema.
	 * 
	 * @param schema
	 *            the schema to remove from the group.
	 * @param schemaGroup
	 *            the group to remove it from.
	 */
	public void requestRemoveSchemaFromSchemaGroup(final Schema schema,
			final SchemaGroup schemaGroup) {
		// Confirms with the user.
		int choice = JOptionPane.showConfirmDialog(this, BuilderBundle
				.getString("confirmUngroupSchema"), BuilderBundle
				.getString("questionTitle"), JOptionPane.YES_NO_OPTION);

		// If they didn't say yes, don't do it.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// In the background, remove the schema from the group.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the schema from the group and reinstate
					// it as an individual schema.
					MartBuilderUtils.removeSchemaFromSchemaGroup(martTab
							.getMart(), schema, schemaGroup);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Reinstate the tab for the individual schema.
							addSchemaTab(schema);
							
							// If the group is now empty, remove the tab for it.
							if (schemaGroup.getSchemas().size() == 0)
								removeSchemaTab(schemaGroup);

							// Recalculate the overview diagram.
							recalculateOverviewDiagram();
							
							// Set the dataset tabset status as modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	private void removeSchemaTab(Schema schema) {
		// Work out which tab the schema lives in.
		int index = this.schemaToDiagram[0].indexOf(schema);

		// Work out the tab index.
		int tabIndex = this.indexOfTab(schema.getName());

		// Remove the tab, and it's mapping from the schema-to-tab map.
		this.removeTabAt(tabIndex);
		this.schemaToDiagram[0].remove(index);
		this.schemaToDiagram[1].remove(index);
	}

	/**
	 * Asks the user for a name for a schema.
	 * 
	 * @param defaultResponse
	 *            the intial choice to display, which will get accepted by the
	 *            default action if they don't type any replacement for it.
	 */
	private String askUserForSchemaName(String defaultResponse) {
		// Ask user for a name, giving them the default suggestion.
		String name = (String) JOptionPane.showInputDialog(this.martTab
				.getMartTabSet().getMartBuilder(), BuilderBundle
				.getString("requestSchemaName"), BuilderBundle
				.getString("questionTitle"), JOptionPane.QUESTION_MESSAGE,
				null, null, defaultResponse);

		// If they didn't select anything, return null.
		if (name == null)
			return null;

		// If they entered an empty string, ie. deleted the default
		// but didn't type anything else, make it as though
		// it had not been deleted.
		else if (name.trim().length() == 0)
			name = defaultResponse;

		// Return the response.
		return name;
	}

	/**
	 * Asks user for a new name, then renames a schema, which is optionally part
	 * of a schema group.
	 * 
	 * @param schema
	 *            the schema to rename.
	 * @param schemaGroup
	 *            (optional, set to null if not used) the group in which this
	 *            schema is living.
	 */
	public void requestRenameSchema(Schema schema, Schema schemaGroup) {
		try {
			// Ask for a new name, suggesting the schema's existing name
			// as the default response.
			String newName = this.askUserForSchemaName(schema.getName());

			// If they cancelled or entered the same name, ignore the request.
			if (newName == null || newName.equals(schema.getName()))
				return;

			// Was the schema in a group? Rename it within the group.
			if (schemaGroup != null)
				// Rename it within the group.
				MartBuilderUtils.renameSchemaInSchemaGroup(schema, newName);
			else {
				// Work out which tab the schema is in.
				int idx = this.indexOfTab(schema.getName());

				// Rename the schema.
				MartBuilderUtils.renameSchema(this.martTab.getMart(), schema,
						newName);

				// Rename the tab displaying it.
				this.setTitleAt(idx, schema.getName());
			}

			// As the schema name has changed, the all-schemas diagram needs
			// to be recalculated, as the schema representations may have
			// changed size owing to the new name. The individual schema
			// and dataset diagrams will also need to be recalculated.
			this.recalculateOverviewDiagram();
			this.recalculateSchemaDiagram(schema);
			this.martTab.getDataSetTabSet().recalculateAllDataSetDiagrams();

			// Set the dataset tabset status to modified.
			this.martTab.getMartTabSet().setModifiedStatus(true);
		} catch (Throwable t) {
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}
	}

	/**
	 * Syncs this schema against the database.
	 * 
	 * @param schema
	 *            the schema to synchronise.
	 */
	public void requestSynchroniseSchema(final Schema schema) {
		// In the background, do the synchronisation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Synchronise it.
					MartBuilderUtils.synchroniseSchema(schema);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the diagram.
							recalculateSchemaDiagram(schema);

							// As it may have lost or gained some external
							// relations,
							// the all-schemas diagram should also be
							// recalculated.
							recalculateOverviewDiagram();

							// It may have disappeared altogether, or lost some
							// table
							// upon which a dataset was based, so the datasets
							// may have
							// changed. In which case, recalculate the dataset
							// tabset
							// to reflect this.
							martTab.getDataSetTabSet().recalculateDataSetTabs();

							// Update the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Synchronises all schemas in the mart.
	 */
	public void requestSynchroniseAllSchemas() {
		// In the background, do the synchronisation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Synchronise all schemas in the mart.
					MartBuilderUtils.synchroniseMartSchemas(martTab.getMart());

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate all schema diagrams.
							recalculateAllSchemaDiagrams();

							// As schemas may have lost or gained some external
							// relations, the all-schemas diagram should also be
							// recalculated.
							recalculateOverviewDiagram();

							// Schemas may have disappeared altogether, or lost
							// some
							// table upon which a dataset was based, so the
							// datasets
							// may have changed. In which case, recalculate the
							// dataset
							// tabset to reflect this.
							martTab.getDataSetTabSet().recalculateDataSetTabs();

							// Update the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Test this schema to see if the datasource or database it represents is
	 * contactable.
	 * 
	 * @param schema
	 *            the schema to test.
	 */
	public void requestTestSchema(Schema schema) {
		// Assume we've failed.
		boolean passedTest = false;

		try {
			// Attempt to pass the test.
			passedTest = MartBuilderUtils.testSchema(schema);
		} catch (Throwable t) {
			// If we get an exception, we failed the test, and should
			// tell the user why.
			passedTest = false;
			this.martTab.getMartTabSet().getMartBuilder().showStackTrace(t);
		}

		// Tell the user if we passed or failed.
		if (passedTest)
			JOptionPane.showMessageDialog(this, BuilderBundle
					.getString("schemaTestPassed"), BuilderBundle
					.getString("testTitle"), JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(this, BuilderBundle
					.getString("schemaTestFailed"), BuilderBundle
					.getString("testTitle"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Turn keyguessing on for a schema.
	 * 
	 * @param schema
	 *            the schema to turn keyguessing on for.
	 */
	public void requestEnableKeyGuessing(final Schema schema) {
		// In the background, do the synchronisation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Do it.
					MartBuilderUtils.enableKeyGuessing(schema);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the diagram.
							recalculateSchemaDiagram(schema);

							// As it may have lost or gained some external
							// relations,
							// the all-schemas diagram should also be
							// recalculated.
							recalculateOverviewDiagram();

							// It may have disappeared altogether, or lost some
							// table
							// upon which a dataset was based, so the datasets
							// may have
							// changed. In which case, recalculate the dataset
							// tabset
							// to reflect this.
							martTab.getDataSetTabSet().recalculateDataSetTabs();

							// Update the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Turn keyguessing off for a schema.
	 * 
	 * @param schema
	 *            the schema to turn keyguessing off for.
	 */
	public void requestDisableKeyGuessing(final Schema schema) {
		// In the background, do the synchronisation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Do it.
					MartBuilderUtils.disableKeyGuessing(schema);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the diagram.
							recalculateSchemaDiagram(schema);

							// As it may have lost or gained some external
							// relations,
							// the all-schemas diagram should also be
							// recalculated.
							recalculateOverviewDiagram();

							// It may have disappeared altogether, or lost some
							// table
							// upon which a dataset was based, so the datasets
							// may have
							// changed. In which case, recalculate the dataset
							// tabset
							// to reflect this.
							martTab.getDataSetTabSet().recalculateDataSetTabs();

							// Update the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Update a relation cardinality.
	 * 
	 * @param relation
	 *            the relation to change cardinality of.
	 * @param cardinality
	 *            the new cardinality to give it.
	 */
	public void requestChangeRelationCardinality(final Relation relation,
			final Cardinality cardinality) {
		// In the background, change the cardinality.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Change the cardinality.
					MartBuilderUtils.changeRelationCardinality(martTab
							.getMart(), relation, cardinality);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Internal relation? Repaint the individual schema
							// diagram.
							if (!relation.isExternal())
								repaintSchemaDiagram(relation.getFirstKey()
										.getTable().getSchema());

							// External relation? Repaint the all-schemas
							// diagram.
							else
								repaintOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Update a relation status.
	 * 
	 * @param relation
	 *            the relation to change the status for.
	 * @param status
	 *            the new status to give it.
	 */
	public void requestChangeRelationStatus(final Relation relation,
			final ComponentStatus status) {
		// In the background, change the status.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Change the status.
					MartBuilderUtils.changeRelationStatus(martTab.getMart(),
							relation, status);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Internal relation? Repaint the individual schema
							// diagram.
							if (!relation.isExternal())
								repaintSchemaDiagram(relation.getFirstKey()
										.getTable().getSchema());

							// External relation? Repaint the all-schemas
							// diagram.
							else
								repaintOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Remove a relation.
	 * 
	 * @param relation
	 *            the relation to remove.
	 */
	public void requestRemoveRelation(final Relation relation) {
		// In the background, remove the relation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the relation.
					MartBuilderUtils
							.removeRelation(martTab.getMart(), relation);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Internal relation? Repaint the individual schema
							// diagram.
							if (!relation.isExternal())
								recalculateSchemaDiagram(relation.getFirstKey()
										.getTable().getSchema());

							// External relation? Repaint the all-schemas
							// diagram.
							else
								recalculateOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Remove a key.
	 * 
	 * @param key
	 *            the key to remove.
	 */
	public void requestRemoveKey(final Key key) {
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the key.
					MartBuilderUtils.removeKey(martTab.getMart(), key);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the schema diagram this key appears
							// in, as
							// the table components will have changed size, and
							// some
							// relations may have disappeared.
							recalculateSchemaDiagram(key.getTable().getSchema());

							// The same may have happened in the all-schemas
							// diagram if
							// this key had any external relations, or belonged
							// to a
							// table which had external relations on some other
							// key.
							recalculateOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Update a key status.
	 * 
	 * @param key
	 *            the key to update the status of.
	 * @param status
	 *            the new status to give it.
	 */
	public void requestChangeKeyStatus(final Key key,
			final ComponentStatus status) {
		// In the background, change the status.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Change the status.
					MartBuilderUtils.changeKeyStatus(martTab.getMart(), key,
							status);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the schema diagram this key appears
							// in, as
							// the table components will have changed size, and
							// some
							// relations may have disappeared.
							recalculateSchemaDiagram(key.getTable().getSchema());

							// The same may have happened in the all-schemas
							// diagram if
							// this key had any external relations, or belonged
							// to a
							// table which had external relations on some other
							// key.
							recalculateOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Ask the user to define a primary key on a table, then create it.
	 * 
	 * @param table
	 *            the table to define the key on.
	 */
	public void requestCreatePrimaryKey(Table table) {
		// Pop up a dialog to ask which columns to use.
		List cols = KeyEditorDialog.createPrimaryKey(this.martTab, table);

		// If they chose some columns, create the key.
		if (!cols.isEmpty())
			this.requestCreatePrimaryKey(table, cols);
	}

	/**
	 * Given a set of columns, create a primary key on the given table that
	 * contains those columns in the order they appear in the iterator. This
	 * will replace any existing primary key on the table.
	 * 
	 * @param table
	 *            the table to create the key over.
	 * @param columns
	 *            the columns to include the key.
	 */
	public void requestCreatePrimaryKey(final Table table, final List columns) {
		// In the background, create the key.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Create the key.
					MartBuilderUtils.createPrimaryKey(table, columns);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the schema diagram this key appears
							// in, as
							// the table components will have changed size, and
							// some
							// relations may have disappeared.
							recalculateSchemaDiagram(table.getSchema());

							// The same may have happened in the all-schemas
							// diagram if
							// this key had any external relations, or belonged
							// to a
							// table which had external relations on some other
							// key.
							recalculateOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Ask the user to define a foreign key on a table, then create it.
	 * 
	 * @param table
	 *            the table to define the key on.
	 */
	public void requestCreateForeignKey(Table table) {
		// Pop up a dialog to ask which columns to use.
		List cols = KeyEditorDialog.createPrimaryKey(this.martTab, table);

		// If they chose some columns, create the key.
		if (!cols.isEmpty())
			this.requestCreateForeignKey(table, cols);
	}

	/**
	 * Given a set of columns, create a foreign key on the given table that
	 * contains those columns in the order they appear in the iterator.
	 * 
	 * @param table
	 *            the table to create the key over.
	 * @param columns
	 *            the columns to include the key.
	 */
	public void requestCreateForeignKey(final Table table, final List columns) {
		// In the background, create the key.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Create the key.
					MartBuilderUtils.createForeignKey(table, columns);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the schema diagram this key appears
							// in, as
							// the table components will have changed size.
							recalculateSchemaDiagram(table.getSchema());

							// The same may have happened in the all-schemas
							// diagram if
							// this key belonged to a table which has external
							// relations
							// on some other key.
							recalculateOverviewDiagram();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Pop up a dialog describing the key, and ask the user to modify it, before
	 * carrying out the modification.
	 * 
	 * @param key
	 *            the key to edit.
	 */
	public void requestEditKey(Key key) {
		// Pop up the dialog which describes the key, and obtain the
		// list of columns they selected in response.
		List cols = KeyEditorDialog.editKey(this.martTab, key);

		// If they selected any columns, and those columns are not
		// the same as the ones already in the key, modify the key.
		if (!cols.isEmpty() && !cols.equals(key.getColumns()))
			this.requestEditKey(key, cols);
	}

	/**
	 * Change the nullability of a foreign key.
	 * 
	 * @param key
	 *            the key to change.
	 * @param nullable
	 *            <tt>true</tt> if it should be nullable, <tt>false</tt> if
	 *            not.
	 */
	public void requestChangeForeignKeyNullability(final ForeignKey key,
			final boolean nullable) {
		// In the background, make the change.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Do the changes.
					MartBuilderUtils.changeForeignKeyNullability(key, nullable);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Repaint the schema diagram this key appears in,
							// as
							// the relation components will have changed
							// appearance.
							repaintSchemaDiagram(key.getTable().getSchema());

							// The same may have happened in the all-schemas
							// diagram if
							// this key had any external relations, or belonged
							// to a
							// table which had external relations on some other
							// key.
							repaintOverviewDiagram();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Change the columns that a key uses.
	 * 
	 * @param key
	 *            the key to change.
	 * @param columns
	 *            the new set of columns to assign to the key.
	 */
	public void requestEditKey(final Key key, final List columns) {
		// In the background, make the change.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Do the changes.
					MartBuilderUtils.editKeyColumns(martTab.getMart(), key,
							columns);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// Recalculate the schema diagram this key appears
							// in, as
							// the table components will have changed size, and
							// some
							// relations may have disappeared.
							recalculateSchemaDiagram(key.getTable().getSchema());

							// The same may have happened in the all-schemas
							// diagram if
							// this key had any external relations, or belonged
							// to a
							// table which had external relations on some other
							// key.
							recalculateOverviewDiagram();

							// This may have caused new dimensions or subclass
							// tables to
							// appear in datasets referring to tables in this
							// schema, so
							// we need to recalculate all dataset diagrams just
							// in case.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	private Key askUserForTargetKey(Key from) {
		// Given a particular key, work out which other keys, in any schema,
		// this key may be linked to.

		// Start by making a list to contain the candidates.
		Collection candidates = new ArrayList();

		// We want all keys that have the same number of columns.
		for (Iterator i = this.schemaToDiagram[0].iterator(); i.hasNext();)
			for (Iterator j = ((Schema) i.next()).getTables().iterator(); j
					.hasNext();)
				for (Iterator k = ((Table) j.next()).getKeys().iterator(); k
						.hasNext();) {
					Key key = (Key) k.next();
					if (key.countColumns() == from.countColumns()
							&& !key.equals(from))
						candidates.add(key);
				}

		// Put up a box asking which key to link this key to, based on the
		// list of candidates we just made. Return the key that the user
		// selects, or null if none was selected.
		return (Key) JOptionPane.showInputDialog(this, BuilderBundle
				.getString("whichKeyToLinkRelationTo"), BuilderBundle
				.getString("questionTitle"), JOptionPane.QUESTION_MESSAGE,
				null, candidates.toArray(), null);
	}

	/**
	 * Given a key, ask the user which other key they want to make a relation to
	 * from this key.
	 * 
	 * @param from
	 *            the key to make a relation from.
	 */
	public void requestCreateRelation(Key from) {
		// Ask them which key they want to link to.
		Key to = this.askUserForTargetKey(from);

		// If they selected something, create the relation to it.
		if (to != null)
			this.requestCreateRelation(from, to);
	}

	/**
	 * Establish a relation between two keys. The relation will be 1:M. One of
	 * the keys must be a primary key, and the other must be a foreign key.
	 * 
	 * @param from
	 *            the key at one end of the relation-to-be.
	 * @param to
	 *            the key at the other end of the relation-to-be.
	 */
	public void requestCreateRelation(final Key from, final Key to) {
		// Create the relation in the background.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Create the relation.
					MartBuilderUtils
							.createRelation(martTab.getMart(), from, to);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// If it is an internal relation, recalculate the
							// diagram
							// for the schema it is inside of.
							if (from.getTable().getSchema().equals(
									to.getTable().getSchema()))
								recalculateSchemaDiagram(from.getTable()
										.getSchema());

							// If it is external, recalculate the all-schemas
							// diagram.
							else
								recalculateOverviewDiagram();

							// This may have caused dimensions or subclasses to
							// change
							// in some datasets, so recalculate all the dataset
							// diagrams
							// too.
							martTab.getDataSetTabSet()
									.recalculateAllDataSetDiagrams();

							// Set the dataset tabset status to modified.
							martTab.getMartTabSet().setModifiedStatus(true);
						}
					});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							martTab.getMartTabSet().getMartBuilder()
									.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	private JPopupMenu getSchemaTabContextMenu(final Schema schema) {
		// This menu will appear when a schema tab is right-clicked on
		// (that is, the tab itself, not the contents of the tab).

		// The empty menu to start with.
		JPopupMenu contextMenu = new JPopupMenu();

		// Add an option to remove this schema tab, and the
		// associated schema from the mart.
		JMenuItem close = new JMenuItem(BuilderBundle
				.getString("removeSchemaTitle"));
		close.setMnemonic(BuilderBundle.getString("removeSchemaMnemonic")
				.charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				requestRemoveSchema(schema);
			}
		});
		contextMenu.add(close);

		// Add an option to rename this schema tab and associated schema.
		JMenuItem rename = new JMenuItem(BuilderBundle
				.getString("renameSchemaTitle"));
		rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic")
				.charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				requestRenameSchema(schema, null);
			}
		});
		contextMenu.add(rename);

		// Return the menu.
		return contextMenu;
	}

	protected void processMouseEvent(MouseEvent evt) {
		boolean eventProcessed = false;

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Where was the click?
			int selectedIndex = this.indexAtLocation(evt.getX(), evt.getY());

			// Was the click on a tab?
			if (selectedIndex >= 0) {

				// Work out which tab was selected and which diagram
				// is displayed in that tab.
				Component selectedComponent = this
						.getComponentAt(selectedIndex);
				if (selectedComponent instanceof JScrollPane) {
					Component selectedDiagram = ((JScrollPane) selectedComponent)
							.getViewport().getView();
					if (selectedDiagram instanceof SchemaDiagram) {

						// Set the schema diagram as the currently selected one.
						this.setSelectedIndex(selectedIndex);

						// Work out the schema inside the diagram.
						Schema schema = ((SchemaDiagram) selectedDiagram)
								.getSchema();

						// Show the context-menu for the tab for this schema.
						this.getSchemaTabContextMenu(schema).show(this,
								evt.getX(), evt.getY());

						// We've handled the event so mark it as processed.
						eventProcessed = true;
					}
				}
			}
		}

		// Pass the event on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	/**
	 * Sets the diagram context to use for all diagrams inside this schema
	 * tabset. Once set, {@link Diagram#setDiagramContext(DiagramContext)} is
	 * called on each diagram in the tabset in turn so that they are all working
	 * with the same context.
	 * 
	 * @param diagramContext
	 *            the context to use for all diagrams in this schema tabset.
	 */
	public void setDiagramContext(DiagramContext diagramContext) {
		this.diagramContext = diagramContext;
		for (int i = 0; i < this.getTabCount(); i++) {
			Diagram diagram = (Diagram) ((JScrollPane) this.getComponentAt(i))
					.getViewport().getView();
			diagram.setDiagramContext(diagramContext);
		}
	}

	/**
	 * Returns the diagram context currently being used by diagrams in this
	 * schema tabset.
	 * 
	 * @return the diagram context currently being used.
	 */
	public DiagramContext getDiagramContext() {
		return this.diagramContext;
	}
}
