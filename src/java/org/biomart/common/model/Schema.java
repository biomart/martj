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

package org.biomart.common.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.model.DataLink;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Column.GenericColumn;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.GenericForeignKey;
import org.biomart.common.model.Key.GenericPrimaryKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.model.Relation.GenericRelation;
import org.biomart.common.model.Table.GenericTable;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * A schema provides one or more table objects with unique names for the user to
 * use. It could be a relational database, or an XML document, or any other
 * source of potentially tabular information.
 * <p>
 * The generic implementation provided should suffice for most tasks involved in
 * keeping track of the tables a schema provides.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public interface Schema extends Comparable, DataLink {
	/**
	 * Adds a table to this schema.
	 * 
	 * @param table
	 *            the table to add.
	 */
	public void addTable(Table table);

	/**
	 * Attempts to rename a table. The rename does not affect the table itself,
	 * only the representation of the table within this schema. If the names are
	 * the same, nothing happens.
	 * 
	 * @param oldName
	 *            the old name of the table.
	 * @param newName
	 *            the new name of the table.
	 */
	public void changeTableMapKey(String oldName, String newName);

	/**
	 * Returns a collection of all the keys in this schema which have relations
	 * referring to keys in other schemas.
	 * 
	 * @return a set of keys with relations linking to keys in other schemas.
	 */
	public Collection getExternalKeys();

	/**
	 * Returns a collection of all the relations in this schema which refer to
	 * keys in other schemas.
	 * 
	 * @return a set of relations linking to keys in other schemas.
	 */
	public Collection getExternalRelations();

	/**
	 * Returns a collection of all the relations in this schema which refer to
	 * keys in the same schema.
	 * 
	 * @return a set of relations linking to keys in the same schema.
	 */
	public Collection getInternalRelations();

	/**
	 * Checks whether this schema uses key-guessing or not.
	 * 
	 * @return <tt>true</tt> if it does, <tt>false</tt> if it doesn't.
	 */
	public boolean getKeyGuessing();

	/**
	 * Returns the name of this schema.
	 * 
	 * @return the name of this schema.
	 */
	public String getName();

	/**
	 * Returns the table from this schema with the given name. If there is no
	 * such table, the method will return <tt>null</tt>.
	 * 
	 * @param name
	 *            the name of the table to retrieve.
	 * @return the matching tables from this provider.
	 */
	public Table getTableByName(String name);

	/**
	 * Returns all the tables this schema provides. The set returned may be
	 * empty but it will never be <tt>null</tt>.
	 * 
	 * @return the set of all tables in this schema.
	 */
	public Collection getTables();

	/**
	 * Creates an exact replica of this schema, with the given name. It then
	 * calls {@link #replicateContents(Schema)} to copy the contents over.
	 * 
	 * @param newName
	 *            the name to give the new copy of the schema.
	 * @return the copy of the schema.
	 */
	public Schema replicate(String newName);

	/**
	 * Drops the table from this schema.
	 * 
	 * @param tableName
	 *            the table to drop.
	 */
	public void removeTableByName(String tableName);

	/**
	 * Drops all the tables in this schema.
	 */
	public void removeAllTables();

	/**
	 * Copies all the tables, keys and relations from this schema into the
	 * target schema. Reuses any which already exist. Any relations which are
	 * {@link ComponentStatus#INFERRED_INCORRECT} and have keys with different
	 * numbers of columns at either end will probably not be copied, but this
	 * depends on the implementation.
	 * 
	 * @param targetSchema
	 *            the schema to copy into.
	 */
	public void replicateContents(Schema targetSchema);

	/**
	 * Enables or disables key-guessing on this schema. Changing this value will
	 * cause {@link #synchroniseKeys()} to be called.
	 * 
	 * @param keyguessing
	 *            <tt>true</tt> to enable it, <tt>false</tt> to disable it.
	 * @throws SQLException
	 *             See {@link #synchroniseKeys()}.
	 * @throws DataModelException
	 *             See {@link #synchroniseKeys()}.
	 */
	public void setKeyGuessing(boolean keyguessing) throws SQLException,
			DataModelException;

	/**
	 * Sets the name of this schema.
	 * 
	 * @param name
	 *            the new name of this schema.
	 */
	public void setName(String name);

	/**
	 * Synchronise this schema with the data source that is providing its
	 * tables. Synchronisation means checking the list of tables available and
	 * drop/add any that have changed, then check each column. and key and
	 * relation and update those too.
	 * <p>
	 * After this method completes, it will call {@link #synchroniseKeys()}
	 * before returning.
	 * 
	 * @throws SQLException
	 *             if there was a problem connecting to the data source.
	 * @throws DataModelException
	 *             if there was any other kind of logical problem.
	 */
	public void synchronise() throws SQLException, DataModelException;

	/**
	 * This method can be called at any time to recalculate the foreign keys and
	 * relations in the schema.
	 * <p>
	 * Any key or relation that was created by the user and is still valid, ie.
	 * the underlying columns still exist, will not be affected by this
	 * operation.
	 * 
	 * @throws DataModelException
	 *             if anything went wrong to do with the calculation of keys and
	 *             relations.
	 * @throws SQLException
	 *             if anything went wrong whilst talking to the database.
	 */
	public void synchroniseKeys() throws SQLException, DataModelException;

	/**
	 * Call this method if you want the settings from this schema to be stored
	 * in the history file for later user.
	 */
	public void storeInHistory();

	/**
	 * The generic implementation should suffice as the ground for most complex
	 * implementations. It keeps track of tables it has seen, and performs
	 * simple lookups for them.
	 * <p>
	 * This generic implementation obviously isn't connected to anything, and so
	 * it's {@link #synchronise()} and {@link #synchroniseKeys()} methods do
	 * nothing. It also will not store anything when {@link #storeInHistory()}
	 * is called.
	 */
	public class GenericSchema implements Schema {
		private boolean keyguessing;

		private String name;

		private final Map tables = new TreeMap();

		/**
		 * The constructor creates a schema with the given name. Keyguessing is
		 * turned off.
		 * 
		 * @param name
		 *            the name for this new schema.
		 */
		public GenericSchema(final String name) {
			this(name, false);
		}

		/**
		 * This constructor creates a schema with the given name, and with
		 * keyguessing set to the given value.
		 * 
		 * @param name
		 *            the name for the new schema.
		 * @param keyguessing
		 *            <tt>true</tt>if you want keyguessing, <tt>false</tt>
		 *            if not.
		 */
		public GenericSchema(final String name, final boolean keyguessing) {
			Log.info(Resources.get("logCreatingSchema",
					new String[] { name, "" + keyguessing }));
			this.name = name;
			this.keyguessing = keyguessing;
		}

		public void addTable(final Table table) {
			Log.debug("Adding " + table + " to " + this.getName());
			// Add the table.
			this.tables.put(table.getName(), table);
		}

		public boolean canCohabit(final DataLink partner) {
			return false;
		}

		public void changeTableMapKey(final String oldName, final String newName) {
			Log.debug("Remapping table " + oldName + " to "
					+ newName + " in " + this.getName());
			// If the names are the same, do nothing.
			if (oldName.equals(newName))
				return;
			// Update our mapping but don't rename the columns themselves.
			final Table tbl = (Table) this.tables.get(oldName);
			this.tables.put(newName, tbl);
			this.tables.remove(oldName);
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Schema t = (Schema) o;
			return this.toString().compareTo(t.toString());
		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Schema))
				return false;
			final Schema t = (Schema) o;
			return t.toString().equals(this.toString());
		}

		public Collection getExternalKeys() {
			// Keys are external if they have a relation which
			// points, at the other end, to a key in some schema
			// other than ourselves.
			final List keys = new ArrayList();
			final Collection relations = this.getExternalRelations();
			for (final Iterator i = relations.iterator(); i.hasNext();) {
				final Relation relation = (Relation) i.next();
				if (relation.getFirstKey().getTable().getSchema().equals(this))
					keys.add(relation.getFirstKey());
				else
					keys.add(relation.getSecondKey());
			}
			return keys;
		}

		public Collection getExternalRelations() {
			// Relations are external if one end points to a key
			// in a schema other than ourselves.
			final Set relations = new HashSet();
			for (final Iterator i = this.getTables().iterator(); i.hasNext();) {
				final Table table = (Table) i.next();
				for (final Iterator j = table.getKeys().iterator(); j.hasNext();) {
					final Key key = (Key) j.next();
					for (final Iterator l = key.getRelations().iterator(); l
							.hasNext();) {
						final Relation relation = (Relation) l.next();
						if (relation.isExternal())
							relations.add(relation);
					}
				}
			}
			return relations;
		}

		public Collection getInternalRelations() {
			// Relations are internal if both ends point to keys
			// in this schema.
			final Set relations = new HashSet();
			for (final Iterator i = this.getTables().iterator(); i.hasNext();) {
				final Table table = (Table) i.next();
				for (final Iterator j = table.getRelations().iterator(); j
						.hasNext();) {
					final Relation relation = (Relation) j.next();
					if (!relation.isExternal())
						relations.add(relation);
				}
			}
			return relations;
		}

		public boolean getKeyGuessing() {
			return this.keyguessing;
		}

		public String getName() {
			return this.name;
		}

		public Table getTableByName(final String name) {
			return (Table) this.tables.get(name);
		}

		public Collection getTables() {
			return this.tables.values();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public void removeTableByName(String tableName) {
			Log.debug("Removing table " + tableName + " from "
					+ this.getName());
			this.tables.remove(tableName);
		}

		public void removeAllTables() {
			Log.debug("Removing all tables from " + this.getName());
			this.tables.clear();
		}

		public Schema replicate(final String newName) {
			Log.debug("Replicating " + this.getName() + " as " + newName);
			// Create a new schema.
			final Schema newSchema = new GenericSchema(newName);

			// Copy the contents over.
			this.replicateContents(newSchema);

			// Return.
			return newSchema;
		}

		public void replicateContents(final Schema targetSchema) {
			Log.debug("Replicating contents from " + this.getName() + " to "
					+ targetSchema);
			// List all the tables we should drop at the end.
			final List tablesToDrop = new ArrayList(targetSchema.getTables());

			// Set up a set to contain all the relations to replicate.
			final Set relations = new HashSet();

			// Iterate over all the tables, and copy each one.
			for (final Iterator i = this.tables.values().iterator(); i
					.hasNext();) {
				final Table table = (Table) i.next();

				// Create a copy of the table if it doesn't already exist.
				Table newTable = targetSchema.getTableByName(table.getName());
				if (newTable == null)
					try {
						newTable = new GenericTable(table.getName(),
								targetSchema);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				else
					tablesToDrop.remove(newTable);

				// List all the columns we should drop at the end.
				final List colsToDrop = new ArrayList(newTable.getColumns());

				// Iterate over all the columns and copy them too.
				for (final Iterator j = table.getColumns().iterator(); j
						.hasNext();) {
					final Column col = (Column) j.next();
					Column newCol = newTable.getColumnByName(col.getName());
					if (newCol == null)
						try {
							newCol = new GenericColumn(col.getName(), newTable);
						} catch (final Throwable t) {
							throw new BioMartError(t);
						}
					else
						colsToDrop.remove(newCol);
				}

				// Drop the columns that have disappeared.
				for (final Iterator j = colsToDrop.iterator(); j.hasNext();) {
					final Column col = (Column) j.next();
					Log.debug("Dropping redundant column " + col);
					col.getTable().removeColumn(col);
				}

				// Copy the primary key, if it exists.
				final PrimaryKey pk = table.getPrimaryKey();
				if (pk != null) {
					// Find the equivalents of all the columns by name.
					final List columns = new ArrayList();
					for (final Iterator k = pk.getColumnNames().iterator(); k
							.hasNext();)
						columns
								.add(newTable
										.getColumnByName((String) k.next()));

					// Create the key.
					try {
						// Check to see if PK already set.
						PrimaryKey newPK = newTable.getPrimaryKey();
						if (newPK == null
								|| !newPK.getColumns().equals(columns)) {
							newPK = new GenericPrimaryKey(columns);
							newTable.setPrimaryKey(newPK);
						}
						newPK.setStatus(pk.getStatus());
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				}
				// Otherwise, drop the primary key.
				else
					try {
						newTable.setPrimaryKey(null);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}

				// List all the foreign keys to be dropped later.
				final List fksToDrop = new ArrayList(newTable.getForeignKeys());

				// Copy the foreign keys.
				for (final Iterator j = table.getForeignKeys().iterator(); j
						.hasNext();) {
					final ForeignKey fk = (ForeignKey) j.next();

					// Find the equivalents of all the columns by name.
					final List columns = new ArrayList();
					for (final Iterator k = fk.getColumnNames().iterator(); k
							.hasNext();)
						columns
								.add(newTable
										.getColumnByName((String) k.next()));

					// Create the key.
					try {
						ForeignKey newFK = null;
						// Lookup existing key.
						for (final Iterator k = newTable.getForeignKeys()
								.iterator(); k.hasNext() && newFK == null;) {
							final ForeignKey candKey = (ForeignKey) k.next();
							if (candKey.getColumns().equals(columns))
								newFK = candKey;
						}
						// Reuse it, or create it?
						if (newFK == null) {
							newFK = new GenericForeignKey(columns);
							newTable.addForeignKey(newFK);
						} else
							fksToDrop.remove(newFK);
						newFK.setStatus(fk.getStatus());
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				}

				// Drop all the unused foreign keys.
				for (final Iterator j = fksToDrop.iterator(); j.hasNext();) {
					final ForeignKey key = (ForeignKey) j.next();
					Log.debug("Dropping redundant foreign key "
							+ key);
					key.destroy();
				}

				// Remember the relations on this table for later.
				relations.addAll(table.getRelations());
			}

			// Drop the tables that have disappeared.
			for (final Iterator j = tablesToDrop.iterator(); j.hasNext();) {
				final Table table = (Table) j.next();
				Log.debug("Dropping redundant table " + table);
				table.destroy();
			}

			// Copy internal and external relations. Drop any existing
			// internal relations that were not copied. Do not drop existing
			// external relations as the target schema may have them undefined
			// to prevent multipled-relation-per-foreign-key exceptions.

			// Make a list of all the existing internal relations that can
			// be dropped.
			final List intRelsToDrop = new ArrayList(targetSchema
					.getInternalRelations());

			// Iterate through all the relations we found and
			// copy them too.
			for (final Iterator i = relations.iterator(); i.hasNext();) {
				final Relation r = (Relation) i.next();

				// Work out the keys and cardinality of
				// the existing relation.
				final Key firstKey = r.getFirstKey();
				final Key secondKey = r.getSecondKey();
				final Cardinality card = r.getCardinality();
				final ComponentStatus status = r.getStatus();

				// External relations need to identify which end is ours
				// and which end is not.
				if (r.isExternal()) {
					// Which key is external?
					final Key externalKey = firstKey.getTable().getSchema()
							.equals(this) ? secondKey : firstKey;
					final Key internalKey = r.getOtherKey(externalKey);

					// Find the equivalent keys in the duplicate table
					// by comparing table names and sets of column names.
					Key newInternalKey = null;
					for (final Iterator j = targetSchema.getTableByName(
							internalKey.getTable().getName()).getKeys()
							.iterator(); j.hasNext() && newInternalKey == null;) {
						final Key candidate = (Key) j.next();
						if (candidate.getColumnNames().equals(
								internalKey.getColumnNames())
								&& candidate.getClass().equals(
										internalKey.getClass()))
							newInternalKey = candidate;
					}

					// Create the relation in the duplicate schema.
					// Drop the original relation first to prevent duplicate
					// relation problems.
					try {
						r.destroy();
						final Relation newRel = new GenericRelation(
								newInternalKey, externalKey, card);
						newRel.setStatus(status);
					} catch (final Exception e) {
						// Ignore. This can only happen if incorrect relations
						// are copied across which have different-arity keys at
						// each end, or if the foreign keys involved already
						// have relations elsewhere, or the relation already
						// exists and therefore can be reused.
					}
				}

				// Internal relations are easy - just copy.
				else {
					// Find the equivalent keys in the duplicate table
					// by comparing table names and sets of column names.
					Key newFirstKey = null;
					for (final Iterator j = targetSchema.getTableByName(
							firstKey.getTable().getName()).getKeys().iterator(); j
							.hasNext()
							&& newFirstKey == null;) {
						final Key candidate = (Key) j.next();
						if (candidate.getColumnNames().equals(
								firstKey.getColumnNames()))
							newFirstKey = candidate;
					}
					Key newSecondKey = null;
					for (final Iterator j = targetSchema.getTableByName(
							secondKey.getTable().getName()).getKeys()
							.iterator(); j.hasNext() && newSecondKey == null;) {
						final Key candidate = (Key) j.next();
						if (candidate.getColumnNames().equals(
								secondKey.getColumnNames()))
							newSecondKey = candidate;
					}

					// Does the relation already exist? If so, reuse it.
					Relation newRel = null;
					for (final Iterator j = newFirstKey.getRelations()
							.iterator(); j.hasNext() && newRel == null;) {
						final Relation candRel = (Relation) j.next();
						if (candRel.getOtherKey(newFirstKey).equals(
								newSecondKey))
							newRel = candRel;
					}
					// Otherwise, create the relation in the duplicate schema.
					if (newRel == null)
						try {
							newRel = new GenericRelation(newFirstKey,
									newSecondKey, card);
						} catch (final Exception e) {
							// Ignore. This can only happen if incorrect
							// relations are copied across which have
							// different-arity keys at each end, or if the
							// foreign keys involved already have relations
							// elsewhere.
						}
					else
						intRelsToDrop.remove(newRel);
					// Update the relation's status. Only do this if not null.
					// May be null if the existing relation was not in a valid
					// state for copying.
					if (newRel != null)
						try {
							newRel.setCardinality(card);
							newRel.setStatus(status);
						} catch (final Throwable t) {
							throw new BioMartError(t);
						}
				}
			}

			// Drop the redundant internal relations from the target schema.
			for (final Iterator j = intRelsToDrop.iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				Log.debug("Dropping redundant relation " + rel);
				rel.destroy();
			}
		}

		public void setKeyGuessing(final boolean keyguessing)
				throws SQLException, DataModelException {
			Log.info(Resources.get("logChangeKeyguessing",
					new String[] { "" + keyguessing, this.getName() }));
			this.keyguessing = keyguessing;
			this.synchroniseKeys();
		}

		public void setName(final String name) {
			Log.debug("Renaming " + this.getName() + " to " + name);
			// Don't duplicate effort.
			if (name.equals(this.name))
				return;
			this.name = name;
		}

		public void synchronise() throws SQLException, DataModelException {
			Log.info(Resources.get("logSynchronising", this.getName()));
			this.synchroniseKeys();
		}

		public void synchroniseKeys() throws SQLException, DataModelException {
		}

		public void storeInHistory() {
		}

		public boolean test() throws Exception {
			return true;
		}

		public String toString() {
			return this.getName();
		}
	}
}
