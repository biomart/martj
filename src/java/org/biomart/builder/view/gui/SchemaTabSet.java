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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.AllSchemasDiagram;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.SchemaDiagram;
import org.biomart.builder.view.gui.diagrams.contexts.DiagramContext;
import org.biomart.builder.view.gui.dialogs.KeyDialog;
import org.biomart.builder.view.gui.dialogs.PartitionSchemaDialog;
import org.biomart.builder.view.gui.dialogs.SchemaConnectionDialog;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.Transaction;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;

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
 * @since 0.5
 */
public class SchemaTabSet extends JTabbedPane {
	private static final long serialVersionUID = 1;

	private AllSchemasDiagram allSchemasDiagram;

	private DiagramContext diagramContext;

	private MartTab martTab;

	// Schema hashcodes change, so we must use a double-list.
	private final Map schemaToDiagram = new HashMap();

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
		scroller.getHorizontalScrollBar().addAdjustmentListener(
				this.allSchemasDiagram);
		scroller.getVerticalScrollBar().addAdjustmentListener(
				this.allSchemasDiagram);
		this.addTab(Resources.get("multiSchemaOverviewTab"), scroller);

		// Make a listener which knows how to handle masking and
		// renaming.
		final PropertyChangeListener renameListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				final Schema sch = (Schema) evt.getSource();
				if (evt.getPropertyName().equals("name"))
					// Rename in diagram set.
					SchemaTabSet.this.schemaToDiagram.put(evt.getNewValue(),
							SchemaTabSet.this.schemaToDiagram.remove(evt
									.getOldValue()));
				else if (evt.getPropertyName().equals("masked")) {
					// For masks, if unmasking, add a tab, otherwise
					// remove the tab.
					final boolean masked = ((Boolean) evt.getNewValue())
							.booleanValue();
					if (masked)
						SchemaTabSet.this.removeSchemaTab(sch.getName(), true);
					else
						SchemaTabSet.this.addSchemaTab(sch, false);
				}
			}
		};

		// Populate the map to hold the relation between schemas and the
		// diagrams representing them.
		for (final Iterator i = martTab.getMart().getSchemas().values()
				.iterator(); i.hasNext();) {
			final Schema sch = (Schema) i.next();
			// Don't add schemas which are initially masked.
			if (!sch.isMasked())
				this.addSchemaTab(sch, false);
			sch.addPropertyChangeListener("masked", renameListener);
			sch.addPropertyChangeListener("name", renameListener);
		}

		// Listen to add/remove/mass change schema events.
		martTab.getMart().getSchemas().addPropertyChangeListener(
				new PropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent evt) {
						// Listen to masked schema and rename
						// schema events on each new schema added
						// regardless of tab presence.
						// Mass change. Copy to prevent concurrent mods.
						final Set oldSchs = new HashSet(
								SchemaTabSet.this.schemaToDiagram.keySet());
						for (final Iterator i = martTab.getMart().getSchemas()
								.values().iterator(); i.hasNext();) {
							final Schema sch = (Schema) i.next();
							if (!oldSchs.remove(sch.getName())) {
								// Single-add.
								if (!sch.isMasked())
									SchemaTabSet.this.addSchemaTab(sch, true);
								sch.addPropertyChangeListener("masked",
										renameListener);
								sch.addPropertyChangeListener("name",
										renameListener);
							}
						}
						for (final Iterator i = oldSchs.iterator(); i.hasNext();)
							SchemaTabSet.this.removeSchemaTab(
									(String) i.next(), true);
					}
				});
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

	private synchronized void addSchemaTab(final Schema schema,
			final boolean selectNewSchema) {
		// Create the diagram to represent this schema.
		final SchemaDiagram schemaDiagram = new SchemaDiagram(this.martTab,
				schema);

		// Create a scroller to contain the diagram.
		final JScrollPane scroller = new JScrollPane(schemaDiagram);
		scroller.getViewport().setBackground(schemaDiagram.getBackground());
		scroller.getHorizontalScrollBar().addAdjustmentListener(schemaDiagram);
		scroller.getVerticalScrollBar().addAdjustmentListener(schemaDiagram);

		// Add a tab containing the scroller, with the same name as the schema.
		this.addTab(schema.getName(), scroller);

		// Remember which diagram the schema is connected with.
		this.schemaToDiagram.put(schema.getName(), schemaDiagram);

		// Set the current context on the diagram to be the same as the
		// current context on this schema tabset.
		schemaDiagram.setDiagramContext(this.getDiagramContext());

		if (selectNewSchema) {
			// Fake a click on the schema tab and on the button
			// that selects the schema editor in the current mart tabset.
			this.setSelectedIndex(this.indexOfTab(schema.getName()));
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
		for (final Iterator i = this.martTab.getMart().getSchemas().values()
				.iterator(); i.hasNext();)
			for (final Iterator j = ((Schema) i.next()).getTables().values()
					.iterator(); j.hasNext();) {
				final Table tbl = (Table) j.next();
				for (final Iterator k = tbl.getKeys().iterator(); k.hasNext();) {
					final Key key = (Key) k.next();
					if (key.getColumns().length == from.getColumns().length
							&& !key.equals(from))
						candidates.add(key);
				}
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

	private synchronized void removeSchemaTab(final String schemaName,
			final boolean select) {
		// Work out the currently selected tab.
		final int currentTab = this.getSelectedIndex();

		// Work out the tab index for the schema.
		final int tabIndex = this.indexOfTab(schemaName);

		// Remove the tab. Also remove schema mapping from the schema-to-diagram
		// map.
		this.removeTabAt(tabIndex);
		this.schemaToDiagram.remove(schemaName);

		if (select)
			// Fake a click on the last tab before this one to ensure
			// at least one tab remains visible and up-to-date.
			this.setSelectedIndex(currentTab == 0 ? 0 : Math.max(tabIndex - 1,
					0));
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
	 * Asks user to define a new schema, then adds it.
	 */
	public void requestAddSchema() {
		// Pop up a dialog to get the details of the new schema, then
		// obtain a copy of that schema.
		final Schema schema = SchemaConnectionDialog.createSchema(this.martTab
				.getMart());

		// If no schema was defined, ignore the request.
		if (schema == null)
			return;

		// In the background, add the schema to ourselves.
		new LongProcess() {
			public void run() throws Exception {
				Transaction.start();

				// Add the schema to the mart, then synchronise it.
				SchemaTabSet.this.martTab.getMart().getSchemas().put(
						schema.getName(), schema);

				// Sync it.
				schema.synchronise();

				// If the schema has no relations, then maybe
				// we should turn keyguessing on. The user can always
				// turn it off again later.
				if (schema.getRelations().size() == 0)
					schema.setKeyGuessing(true);

				Transaction.end();
			}
		}.start();
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
		Transaction.start();
		key.setStatus(status);
		Transaction.end();
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
		try {
			Transaction.start();
			relation.setCardinality(cardinality);
			relation.setStatus(ComponentStatus.HANDMADE);
		} catch (final AssociationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
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
		try {
			Transaction.start();
			relation.setStatus(status);
		} catch (final AssociationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
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
		dialog.setVisible(true);
		final Column[] cols = dialog.getSelectedColumns();

		// If they chose some columns, create the key.
		if (cols.length > 0)
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
	public void requestCreateForeignKey(final Table table,
			final Column[] columns) {
		Transaction.start();
		table.getForeignKeys().add(new ForeignKey(columns));
		Transaction.end();
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
		dialog.setVisible(true);
		final Column[] cols = dialog.getSelectedColumns();

		// If they chose some columns, create the key.
		if (cols.length > 0)
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
	public void requestCreatePrimaryKey(final Table table,
			final Column[] columns) {
		Transaction.start();
		table.setPrimaryKey(new PrimaryKey(columns));
		Transaction.end();
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
	 * Given a pair of keys, establish a relation between them.
	 * 
	 * @param from
	 *            one end of the relation.
	 * @param to
	 *            the other end.
	 */
	public void requestCreateRelation(final Key from, final Key to) {
		// Create the relation in the background.
		try {
			Transaction.start();
			final Relation rel = new Relation(
					from,
					to,
					from instanceof PrimaryKey && to instanceof PrimaryKey ? Cardinality.ONE
							: Cardinality.MANY);
			from.getRelations().add(rel);
			to.getRelations().add(rel);
			rel.setStatus(ComponentStatus.HANDMADE);
		} catch (final AssociationException e) {
			StackTrace.showStackTrace(e);
		} finally {
			Transaction.end();
		}
	}

	/**
	 * Turn keyguessing off for a schema.
	 * 
	 * @param schema
	 *            the schema to turn keyguessing off for.
	 */
	public void requestDisableKeyGuessing(final Schema schema) {
		Transaction.start();
		schema.setKeyGuessing(false);
		Transaction.end();
	}

	/**
	 * Asks that a table be (un)ignored.
	 * 
	 * @param table
	 *            the table to (un)ignore.
	 * @param ignored
	 *            ignore it?
	 */
	public void requestIgnoreTable(final Table table, final boolean ignored) {
		Transaction.start();
		table.setMasked(ignored);
		Transaction.end();
	}

	/**
	 * Asks that a schema be (un)masked.
	 * 
	 * @param s
	 *            the schema we are working with.
	 * @param masked
	 *            mask it?
	 */
	public void requestMaskSchema(final Schema s, final boolean masked) {
		Transaction.start();
		s.setMasked(masked);
		Transaction.end();
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
		dialog.setVisible(true);
		final Column[] cols = dialog.getSelectedColumns();

		// If they selected any columns, modify the key.
		if (cols.length > 0) {
			Transaction.start();
			key.setColumns(cols);
			Transaction.end();
		}
	}

	/**
	 * Turn keyguessing on for a schema.
	 * 
	 * @param schema
	 *            the schema to turn keyguessing on for.
	 */
	public void requestEnableKeyGuessing(final Schema schema) {
		Transaction.start();
		schema.setKeyGuessing(true);
		Transaction.end();
	}

	/**
	 * Pops up a dialog with details of the schema, which allows the user to
	 * modify them.
	 * 
	 * @param schema
	 *            the schema to modify.
	 */
	public void requestModifySchema(final Schema schema) {
		if (SchemaConnectionDialog.modifySchema(schema))
			this.requestSynchroniseSchema(schema);
	}

	/**
	 * Pops up a dialog with details of the schema partitions, which allows the
	 * user to modify them.
	 * 
	 * @param schema
	 *            the schema to modify partitions for.
	 */
	public void requestModifySchemaPartitions(final Schema schema) {
		final PartitionSchemaDialog dialog = new PartitionSchemaDialog(schema);
		if (dialog.definePartitions()) {
			Transaction.start();
			schema.setPartitionNameExpression(dialog.getExpression());
			schema.setPartitionRegex(dialog.getRegex());
			Transaction.end();
		}
	}

	/**
	 * Remove a key.
	 * 
	 * @param key
	 *            the key to remove.
	 */
	public void requestRemoveKey(final Key key) {
		Transaction.start();
		if (key instanceof PrimaryKey)
			key.getTable().setPrimaryKey(null);
		else
			key.getTable().getForeignKeys().remove(key);
		Transaction.end();
	}

	/**
	 * Remove a relation.
	 * 
	 * @param relation
	 *            the relation to remove.
	 */
	public void requestRemoveRelation(final Relation relation) {
		Transaction.start();
		relation.getFirstKey().getRelations().remove(relation);
		relation.getSecondKey().getRelations().remove(relation);
		Transaction.end();
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

		Transaction.start();
		SchemaTabSet.this.martTab.getMart().getSchemas().remove(
				schema.getName());
		Transaction.end();
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
		this.requestRenameSchema(schema, this.askUserForSchemaName(schema
				.getName()));
	}

	/**
	 * Requests that the schema be given the new name, now, without further
	 * prompting
	 * 
	 * @param schema
	 *            the schema to rename.
	 * @param name
	 *            the new name to give it.
	 */
	public void requestRenameSchema(final Schema schema, final String name) {
		// Ask for a new name, suggesting the schema's existing name
		// as the default response.
		final String newName = name == null ? "" : name.trim();

		// If they cancelled or entered the same name, ignore the request.
		if (newName.length() == 0)
			return;

		Transaction.start();
		schema.setName(newName);
		Transaction.end();
	}

	/**
	 * Shows some rows of the table in a {@link JTable} in a popup dialog.
	 * 
	 * @param table
	 *            the table to show rows from.
	 * @param count
	 *            how many rows to show.
	 */
	public void requestShowRows(final Table table, final int count) {
		new LongProcess() {
			public void run() throws Exception {
				// Get the rows.
				final Collection rows = table.getSchema().getRows(table, count);
				// Convert to a nested vector.
				final Vector data = new Vector();
				for (final Iterator i = rows.iterator(); i.hasNext();)
					data.add(new Vector((List) i.next()));
				// Get the column names.
				final Vector colNames = new Vector(table.getColumns().keySet());
				// Construct a JTable.
				final JTable jtable = new JTable(new DefaultTableModel(data,
						colNames));
				final Dimension size = new Dimension();
				size.width = 0;
				size.height = jtable.getRowHeight() * count;
				for (int i = 0; i < jtable.getColumnCount(); i++)
					size.width += jtable.getColumnModel().getColumn(i)
							.getPreferredWidth();
				size.width = Math.min(size.width, 800); // Arbitrary.
				size.height = Math.min(size.height, 200); // Arbitrary.
				jtable.setPreferredScrollableViewportSize(size);
				// Display them.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, new JScrollPane(
								jtable), Resources.get("showRowsDialogTitle",
								new String[] { "" + count, table.getName() }),
								JOptionPane.INFORMATION_MESSAGE);
					}
				});
			}
		}.start();
	}

	/**
	 * Synchronises all schemas in the mart.
	 */
	public void requestSynchroniseAllSchemas() {
		// In the background, do the synchronisation.
		new LongProcess() {
			public void run() throws Exception {
				Transaction.start();
				for (final Iterator i = SchemaTabSet.this.martTab.getMart()
						.getSchemas().values().iterator(); i.hasNext();)
					((Schema) i.next()).synchronise();
				Transaction.end();
			}
		}.start();
	}

	/**
	 * Syncs this schema against the database.
	 * 
	 * @param schema
	 *            the schema to synchronise.
	 */
	public void requestSynchroniseSchema(final Schema schema) {
		// In the background, do the synchronisation.
		new LongProcess() {
			public void run() throws Exception {
				Transaction.start();
				schema.synchronise();
				Transaction.end();
			}
		}.start();
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
		for (final Iterator i = this.schemaToDiagram.values().iterator(); i
				.hasNext();)
			((Diagram) i.next()).setDiagramContext(diagramContext);
	}
}
