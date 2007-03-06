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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.AllSchemasDiagram;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.SchemaDiagram;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.builder.view.gui.dialogs.KeyDialog;
import org.biomart.common.controller.CommonUtils;
import org.biomart.common.model.Column;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.StackTrace;
import org.biomart.common.view.gui.dialogs.SchemaConnectionDialog;
import org.biomart.common.view.gui.dialogs.SchemaPartitionDialog;

/**
 * This tabset has one tab for the diagram which represents all schemas, and one
 * tab each for each schema in the mart. It provides methods for working with a
 * given schema, such as adding or removing them, or grouping them together. It
 * can update itself based on the schemas in the mart on request.
 * <p>
 * Like a diagram, it can have a {@link DiagramContext} associated with it.
 * Whenever this context changes, all {@link Diagram} instances represented in
 * the tabs have the same context applied.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class SchemaTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private AllSchemasDiagram allSchemasDiagram;

	private DiagramContext diagramContext;

	private MartTab martTab;

	// Schema hashcodes change, so we must use a double-list.
	private final List[] schemaToDiagram = new List[] { new ArrayList(),
			new ArrayList() };

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
	public SchemaTabSet(final MartTab martTab) {
		super();

		Log.info(Resources.get("logCreateSchemaTabs"));

		// Remember the mart tabset we are shown inside.
		this.martTab = martTab;

		// Add the all-schemas overview tab. This tab displays a diagram
		// in which all schemas appear, linked where necessary by external
		// relations. This diagram could be quite large, so it is held inside
		// a scrollpane.
		this.allSchemasDiagram = new AllSchemasDiagram(this.martTab);
		final JScrollPane scroller = new JScrollPane(this.allSchemasDiagram);
		scroller.getViewport().setBackground(
				this.allSchemasDiagram.getBackground());
		this.addTab(Resources.get("multiSchemaOverviewTab"), scroller);
		this.setForegroundAt(0, Color.WHITE);

		// Populate the map to hold the relation between schemas and the
		// diagrams representing them.
		this.recalculateSchemaTabs();
	}

	/**
	 * Works out which schema tab is selected, and return it.
	 * 
	 * @return the currently selected schema, or <tt>null</tt> if none is
	 *         selected.
	 */
	public Schema getSelectedSchema() {
		if (this.getSelectedIndex() <= 0 || !this.isShowing())
			return null;
		final SchemaDiagram selectedDiagram = (SchemaDiagram) ((JScrollPane) this
				.getSelectedComponent()).getViewport().getView();
		return selectedDiagram.getSchema();
	}

	private void addSchemaTab(final Schema schema, final boolean selectNewSchema) {
		Log.info(Resources.get("logAddSchemaTab", "" + schema));
		// Create the diagram to represent this schema.
		final SchemaDiagram schemaDiagram = new SchemaDiagram(this.martTab,
				schema);

		// Create a scroller to contain the diagram.
		final JScrollPane scroller = new JScrollPane(schemaDiagram);
		scroller.getViewport().setBackground(schemaDiagram.getBackground());

		// Add a tab containing the scroller, with the same name as the schema.
		this.addTab(schema.getName(), scroller);

		// Remember which diagram the schema is connected with.
		this.schemaToDiagram[0].add(schema);
		this.schemaToDiagram[1].add(schemaDiagram);

		// Set the current context on the diagram to be the same as the
		// current context on this schema tabset.
		schemaDiagram.setDiagramContext(this.getDiagramContext());
		
		this.recalculateOverviewDiagram();

		if (selectNewSchema) {
			// Fake a click on the schema tab and on the button
			// that selects the schema editor in the current mart tabset.
			this.setSelectedIndex(this.indexOfTab(schema.getName()));
			this.martTab.selectSchemaEditor();
		} else {
			// Fake a click on the all-schemas tab and on the button
			// that selects the schema editor in the current mart tabset.
			this.setSelectedIndex(0);
			this.martTab.selectSchemaEditor();
		}
	}

	private String askUserForSchemaName(final String defaultResponse) {
		// Ask user for a name, giving them the default suggestion.
		String name = (String) JOptionPane.showInputDialog(null, Resources
				.get("requestSchemaName"), Resources.get("questionTitle"),
				JOptionPane.QUESTION_MESSAGE, null, null, defaultResponse);

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

	private Key askUserForTargetKey(final Key from) {
		// Given a particular key, work out which other keys, in any schema,
		// this key may be linked to.

		// Start by making a list to contain the candidates.
		final Collection candidates = new ArrayList();

		// We want all keys that have the same number of columns.
		for (final Iterator i = this.schemaToDiagram[0].iterator(); i.hasNext();)
			for (final Iterator j = ((Schema) i.next()).getTables().iterator(); j
					.hasNext();)
				for (final Iterator k = ((Table) j.next()).getKeys().iterator(); k
						.hasNext();) {
					final Key key = (Key) k.next();
					if (key.countColumns() == from.countColumns()
							&& !key.equals(from))
						candidates.add(key);
				}

		// Put up a box asking which key to link this key to, based on the
		// list of candidates we just made. Return the key that the user
		// selects, or null if none was selected.
		return (Key) JOptionPane.showInputDialog(null, Resources
				.get("whichKeyToLinkRelationTo"), Resources
				.get("questionTitle"), JOptionPane.QUESTION_MESSAGE, null,
				candidates.toArray(), null);
	}

	private JPopupMenu getSchemaTabContextMenu(final Schema schema) {
		// This menu will appear when a schema tab is right-clicked on
		// (that is, the tab itself, not the contents of the tab).

		// The empty menu to start with.
		final JPopupMenu contextMenu = new JPopupMenu();

		// Add an option to rename this schema tab and associated schema.
		final JMenuItem rename = new JMenuItem(Resources
				.get("renameSchemaTitle"));
		rename.setMnemonic(Resources.get("renameSchemaMnemonic").charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				SchemaTabSet.this.requestRenameSchema(schema);
			}
		});
		contextMenu.add(rename);

		// Add an option to replicate this schema tab.
		final JMenuItem replicate = new JMenuItem(Resources
				.get("replicateSchemaTitle"));
		replicate.setMnemonic(Resources.get("replicateSchemaMnemonic")
				.charAt(0));
		replicate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				SchemaTabSet.this.requestReplicateSchema(schema);
			}
		});
		contextMenu.add(replicate);

		// Add an option to remove this schema tab, and the
		// associated schema from the mart.
		final JMenuItem close = new JMenuItem(Resources
				.get("removeSchemaTitle"), new ImageIcon(Resources
				.getResourceAsURL("cut.gif")));
		close.setMnemonic(Resources.get("removeSchemaMnemonic").charAt(0));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				SchemaTabSet.this.requestRemoveSchema(schema);
			}
		});
		contextMenu.add(close);

		// Return the menu.
		return contextMenu;
	}

	private void removeSchemaTab(final Schema schema) {
		Log.info(Resources.get("logRemoveSchemaTab", "" + schema));
		// Work out the currently selected tab.
		final int currentTab = this.getSelectedIndex();

		// Work out the tab index for the schema.
		final int tabIndex = this.indexOfTab(schema.getName());

		// Work out which diagram the schema is associated with.
		final int index = this.schemaToDiagram[0].indexOf(schema);

		// Remove the tab. Also remove schema mapping from the schema-to-diagram
		// map.
		this.removeTabAt(tabIndex);
		this.schemaToDiagram[0].remove(index);
		this.schemaToDiagram[1].remove(index);

		// Update the all-schemas diagram.
		this.recalculateOverviewDiagram();

		// Fake a click on the last tab before this one to ensure
		// at least one tab remains visible and up-to-date.
		this.setSelectedIndex(currentTab == 0 ? 0 : Math.max(tabIndex - 1, 0));
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Where was the click?
			final int selectedIndex = this.indexAtLocation(evt.getX(), evt
					.getY());

			// Was the click on a tab?
			if (selectedIndex >= 0) {

				// Work out which tab was selected and which diagram
				// is displayed in that tab.
				final Component selectedComponent = this
						.getComponentAt(selectedIndex);
				if (selectedComponent instanceof JScrollPane) {
					final Component selectedDiagram = ((JScrollPane) selectedComponent)
							.getViewport().getView();
					if (selectedDiagram instanceof SchemaDiagram) {

						// Set the schema diagram as the currently selected one.
						this.setSelectedIndex(selectedIndex);

						// Work out the schema inside the diagram.
						final Schema schema = ((SchemaDiagram) selectedDiagram)
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
	 * Returns the diagram context currently being used by {@link Diagram}s in
	 * this schema tabset.
	 * 
	 * @return the diagram context currently being used.
	 */
	public DiagramContext getDiagramContext() {
		return this.diagramContext;
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
	 * Causes {@link Diagram#recalculateDiagram()} to be called on the tab which
	 * represents all the schemas in the mart.
	 */
	public void recalculateOverviewDiagram() {
		this.allSchemasDiagram.recalculateDiagram();
	}

	/**
	 * Causes {@link Diagram#recalculateDiagram()} to be called on the tab which
	 * represents the specified schema.
	 * 
	 * @param schema
	 *            the schema to recalculate the diagram of.
	 */
	private void recalculateSchemaDiagram(final Schema schema) {
		final int index = this.schemaToDiagram[0].indexOf(schema);
		((Diagram) this.schemaToDiagram[1].get(index)).recalculateDiagram();
	}
	
	public void recalculateAllSchemaDiagrams() {
		for (int index = 0; index < this.schemaToDiagram.length; index++)
		((Diagram) this.schemaToDiagram[1].get(index)).recalculateDiagram();
	}
	
	public void repaintAllSchemaDiagrams() {
		for (int index = 0; index < this.schemaToDiagram.length; index++)
		((Diagram) this.schemaToDiagram[1].get(index)).repaintDiagram();
	}

	/**
	 * Uses the mart to work out what schemas are available, then updates the
	 * tabs that represent the individual schemas to make sure that they show
	 * the same list. Also updates the overview diagram.
	 */
	public void recalculateSchemaTabs() {
		Log.info(Resources.get("logRecalcSchemaTabs"));
		// Add all schemas in the mart that we don't have yet.
		// We work with a copy of the list of schemas else we get
		// concurrent modification exceptions as new ones are added.
		for (final Iterator i = this.martTab.getMart().getSchemas().iterator(); i
				.hasNext();) {
			final Schema schema = (Schema) i.next();
			if (!this.schemaToDiagram[0].contains(schema))
				this.addSchemaTab(schema, false);
		}

		// Remove all schemas we have that are no longer in the mart.
		// We work with a copy of the list of schemas else we get
		// concurrent modification exceptions as old ones are removed.
		final List ourSchemas = new ArrayList(this.schemaToDiagram[0]);
		for (final Iterator i = ourSchemas.iterator(); i.hasNext();) {
			final Schema schema = (Schema) i.next();
			if (!this.martTab.getMart().getSchemas().contains(schema))
				this.removeSchemaTab(schema);
		}
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on the tab which
	 * represents all the schemas in the mart.
	 */
	public void repaintOverviewDiagram() {
		this.allSchemasDiagram.repaintDiagram();
	}

	/**
	 * Causes {@link Diagram#repaintDiagram()} to be called on the diagram which
	 * represents the specified schema.
	 * 
	 * @param schema
	 *            the schema to repaint the diagram of.
	 */
	private void repaintSchemaDiagram(final Schema schema) {
		final int index = this.schemaToDiagram[0].indexOf(schema);
		((Diagram) this.schemaToDiagram[1].get(index)).repaintDiagram();
	}

	/**
	 * Asks user to define a new schema, then adds it.
	 */
	public void requestAddSchema() {
		// Pop up a dialog to get the details of the new schema, then
		// obtain a copy of that schema.
		final Schema schema = SchemaConnectionDialog.createSchema();

		// If no schema was defined, ignore the request.
		if (schema == null)
			return;

		// In the background, add the schema to ourselves.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					try {
						// Add the schema to the mart, then synchronise it.
						MartBuilderUtils.addSchemaToMart(SchemaTabSet.this.martTab
								.getMart(), schema);

						// Synchronise it.
						CommonUtils.synchroniseSchema(schema);

						// If the schema has no relations, then maybe
						// we should turn keyguessing on. The user can always
						// turn it off again later. We need to resynchronise the
						// schema after turning it on.
						if (schema.getRelations().size() == 0)
							CommonUtils.enableKeyGuessing(schema);
					} catch (final Throwable t) {
						throw t;
					} finally {
						// Must use a finally in case the schema gets created
						// but won't sync. We still want to add it so that the
						// user can edit it and retry syncing it, rather than
						// having to add it all over again.
						// Create and add the tab representing this schema.
						SchemaTabSet.this.addSchemaTab(schema, true);
					}

					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
					MartBuilderUtils.changeKeyStatus(SchemaTabSet.this.martTab
							.getMart(), key, status);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.repaintSchemaDiagram(key.getTable().getSchema());
					if (!key.getTable().getExternalRelations().isEmpty())
						SchemaTabSet.this.repaintOverviewDiagram();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();

					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
		// In the background, change the status.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Change the cardinality.
					MartBuilderUtils.changeRelationCardinality(
							SchemaTabSet.this.martTab.getMart(), relation,
							cardinality);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.repaintSchemaDiagram(relation.getFirstKey().getTable().getSchema());
					if (relation.isExternal()) {
						SchemaTabSet.this.repaintSchemaDiagram(relation.getSecondKey().getTable().getSchema());
						SchemaTabSet.this.repaintOverviewDiagram();
					} 
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
					MartBuilderUtils.changeRelationStatus(SchemaTabSet.this.martTab
							.getMart(), relation, status);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.repaintSchemaDiagram(relation.getFirstKey().getTable().getSchema());
					if (relation.isExternal()) {
						SchemaTabSet.this.repaintSchemaDiagram(relation.getSecondKey().getTable().getSchema());
						SchemaTabSet.this.repaintOverviewDiagram();
					} 
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
	public void requestCreateForeignKey(final Table table) {
		// Pop up a dialog to ask which columns to use.
		final KeyDialog dialog = new KeyDialog(table, Resources
				.get("newFKDialogTitle"), Resources.get("addButton"), null);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		final List cols = dialog.getSelectedColumns();

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
					CommonUtils.createForeignKey(table, columns);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(table.getSchema());
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
	public void requestCreatePrimaryKey(final Table table) {
		// Pop up a dialog to ask which columns to use.
		final KeyDialog dialog = new KeyDialog(table, Resources
				.get("newPKDialogTitle"), Resources.get("addButton"), null);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		final List cols = dialog.getSelectedColumns();

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
					CommonUtils.createPrimaryKey(table, columns);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(table.getSchema());
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Given a key, ask the user which other key they want to make a relation to
	 * from this key.
	 * 
	 * @param from
	 *            the key to make a relation from.
	 */
	public void requestCreateRelation(final Key from) {
		// Ask them which key they want to link to.
		final Key to = this.askUserForTargetKey(from);

		// If they selected something, create the relation to it.
		if (to != null)
			this.requestCreateRelation(from, to);
	}

	/**
	 * Establish a relation between two keys.
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
					MartBuilderUtils.createRelation(SchemaTabSet.this.martTab
							.getMart(), from, to);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(from.getTable().getSchema());
					if (!from.getTable().equals(to.getTable())) {
						SchemaTabSet.this.recalculateSchemaDiagram(to.getTable().getSchema());
						SchemaTabSet.this.recalculateOverviewDiagram();
					} 
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
					// Create the key.
					CommonUtils.disableKeyGuessing(schema);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(schema);
					SchemaTabSet.this.recalculateOverviewDiagram();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
	public void requestEditKey(final Key key) {
		// Pop up the dialog which describes the key, and obtain the
		// list of columns they selected in response.
		final KeyDialog dialog = new KeyDialog(key.getTable(), Resources
				.get("editKeyDialogTitle"), Resources.get("modifyButton"), key
				.getColumns());
		dialog.setLocationRelativeTo(null);
		dialog.show();
		final List cols = dialog.getSelectedColumns();

		// If they selected any columns, and those columns are not
		// the same as the ones already in the key, modify the key.
		if (!cols.isEmpty() && !cols.equals(key.getColumns()))
			this.requestEditKey(key, cols);
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
					MartBuilderUtils.editKeyColumns(SchemaTabSet.this.martTab
							.getMart(), key, columns);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(key.getTable().getSchema());
					if (!key.getTable().getExternalRelations().isEmpty()) 
						SchemaTabSet.this.recalculateOverviewDiagram();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
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
					CommonUtils.enableKeyGuessing(schema);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(schema);
					SchemaTabSet.this.recalculateOverviewDiagram();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Pops up a dialog with details of the schema, which allows the user to
	 * modify them.
	 * 
	 * @param schema
	 *            the schema to modify.
	 */
	public void requestModifySchema(final Schema schema) {
		try {
			// If the user actually made any changes, then synchronise the
			// schema to reflect them.
			if (SchemaConnectionDialog.modifySchema(schema))
				this.requestSynchroniseSchema(schema);
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}
	}

	/**
	 * Pops up a dialog with details of the schema partitions, which allows the
	 * user to modify them.
	 * 
	 * @param schema
	 *            the schema to modify partitions for.
	 */
	public void requestModifySchemaPartitions(final Schema schema) {
		try {
			final Map partitions = SchemaPartitionDialog
					.definePartitions(schema);
			if (!partitions.equals(schema.getPartitions())) {
				CommonUtils.setSchemaPartitions(schema, partitions);
				SchemaTabSet.this.martTab.getMartTabSet().setModifiedStatus(
						true);
			}
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
		}
	}

	/**
	 * Remove a key.
	 * 
	 * @param key
	 *            the key to remove.
	 */
	public void requestRemoveKey(final Key key) {
		// In the background, do the synchronisation.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Remove the key.
					MartBuilderUtils.removeKey(SchemaTabSet.this.martTab.getMart(),
							key);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(key.getTable().getSchema());
					if (!key.getTable().getExternalRelations().isEmpty())
						SchemaTabSet.this.recalculateOverviewDiagram();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
					MartBuilderUtils.removeRelation(SchemaTabSet.this.martTab
							.getMart(), relation);
					
					// Repaint the dataset diagram based on the modified
					// dataset.
					SchemaTabSet.this.recalculateSchemaDiagram(relation.getFirstKey().getTable().getSchema());
					if (relation.isExternal()) {
						SchemaTabSet.this.recalculateSchemaDiagram(relation.getSecondKey().getTable().getSchema());
						SchemaTabSet.this.recalculateOverviewDiagram();
					}	
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Confirms with user then removes a schema.
	 * 
	 * @param schema
	 *            the schema to remove.
	 */
	public void requestRemoveSchema(final Schema schema) {
		// Confirm if the user really wants to do it.
		final int choice = JOptionPane.showConfirmDialog(null, Resources
				.get("confirmDelSchema"), Resources.get("questionTitle"),
				JOptionPane.YES_NO_OPTION);

		// If they don't, cancel out.
		if (choice != JOptionPane.YES_OPTION)
			return;

		// In the background, remove it.
		LongProcess.run(new Runnable() {
			public void run() {
				try {
					try {
						// Remove the schema from the mart.
						MartBuilderUtils.removeSchemaFromMart(
								SchemaTabSet.this.martTab.getMart(), schema);
					} catch (final Throwable t) {
						throw t;
					} finally {
						// Remove the schema tab from the schema tabset.
						SchemaTabSet.this.removeSchemaTab(schema);
					}
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks user for a new name, then renames a schema, which is optionally part
	 * of a schema group.
	 * 
	 * @param schema
	 *            the schema to rename.
	 */
	public void requestRenameSchema(final Schema schema) {
		// Ask for a new name, suggesting the schema's existing name
		// as the default response.
		final String newName = this.askUserForSchemaName(schema.getName());

		// If they cancelled or entered the same name, ignore the request.
		if (newName == null || newName.equals(schema.getName()))
			return;

		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Work out which tab the schema is in.
					final int idx = SchemaTabSet.this.indexOfTab(schema.getName());

					// Rename the schema.
					MartBuilderUtils.renameSchema(SchemaTabSet.this.martTab
							.getMart(), schema, newName);

					// Rename the tab displaying it.
					SchemaTabSet.this.setTitleAt(idx, schema.getName());

					SchemaTabSet.this.recalculateSchemaDiagram(schema);
					SchemaTabSet.this.recalculateOverviewDiagram();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Asks user for a name to use, then creates an exact copy of the given
	 * schema, giving the copy the name they chose.
	 * 
	 * @param schema
	 *            the schema to replicate.
	 */
	public void requestReplicateSchema(final Schema schema) {
		// Ask user for the name to use for the copy.
		final String newName = this.askUserForSchemaName(schema.getName());

		// No name entered? Or same name entered? Ignore the request.
		if (newName == null || newName.trim().length() == 0
				|| newName.equals(schema.getName()))
			return;

		LongProcess.run(new Runnable() {
			public void run() {
				try {
					// Create the replicate.
					final Schema newSchema = MartBuilderUtils.replicateSchema(
							SchemaTabSet.this.martTab.getMart(), schema, newName);

					// Add a tab to represent the replicate.
					SchemaTabSet.this.addSchemaTab(newSchema, true);

					// Set the dataset tabset status as modified.
					SchemaTabSet.this.martTab.getMartTabSet().setModifiedStatus(
							true);

					// Pop up a dialog to ask the user if they
					// want to change any of the details of the
					// replicated schema. No finally block as we only
					// want this if successfully replicated.
					if (newSchema != null)
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								SchemaTabSet.this.requestModifySchema(newSchema);
							}
						});
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Shows some rows of the table in a {@link JTable} in a popup dialog.
	 * 
	 * @param table
	 *            the table to show rows from.
	 * @param offset
	 *            where to start from.
	 * @param count
	 *            how many rows to show.
	 */
	public void requestShowRows(final Table table, final int offset,
			final int count) {
		LongProcess.run(new Runnable() {
			public void run() {
				try {
				// Get the rows.
				final Collection rows = MartBuilderUtils.selectRows(table,
						offset, count);
				// Convert to a nested vector.
				final Vector data = new Vector();
				for (final Iterator i = rows.iterator(); i.hasNext();)
					data.add(new Vector((List) i.next()));
				// Get the column names.
				final Vector colNames = new Vector();
				for (final Iterator i = table.getColumns().iterator(); i
						.hasNext();)
					colNames.add(((Column) i.next()).getName());
				// Construct a JTable.
				final JTable jtable = new JTable(new DefaultTableModel(data,
						colNames));
				// Display them.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, new JScrollPane(
								jtable), Resources.get("showRowsTitle"),
								JOptionPane.INFORMATION_MESSAGE);
					}
				});
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
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
					MartBuilderUtils
							.synchroniseMartSchemas(SchemaTabSet.this.martTab
									.getMart());
					
					SchemaTabSet.this.recalculateAllSchemaDiagrams();
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
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
					CommonUtils.synchroniseSchema(schema);
					
					SchemaTabSet.this.recalculateSchemaDiagram(schema);
					SchemaTabSet.this.martTab.getDataSetTabSet()
					.recalculateAllDataSetDiagrams();
					
					// Update the modified status for this tabset.
					SchemaTabSet.this.martTab.getMartTabSet()
							.setModifiedStatus(true);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}
			}
		});
	}

	/**
	 * Sets the diagram context to use for all {@link Diagram}s inside this
	 * schema tabset. Once set,
	 * {@link Diagram#setDiagramContext(DiagramContext)} is called on each
	 * diagram in the tabset in turn so that they are all working with the same
	 * context.
	 * 
	 * @param diagramContext
	 *            the context to use for all {@link Diagram}s in this schema
	 *            tabset.
	 */
	public void setDiagramContext(final DiagramContext diagramContext) {
		this.diagramContext = diagramContext;
		this.allSchemasDiagram.setDiagramContext(diagramContext);
		for (Iterator i = this.schemaToDiagram[1].iterator(); i.hasNext();)
			((Diagram) i.next()).setDiagramContext(diagramContext);
	}
}
