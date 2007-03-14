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
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
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
	 * Returns a collection of all the relations in this schema.
	 * 
	 * @return a set of relations.
	 */
	public Collection getRelations();

	public Collection getInternalRelations();

	public Collection getExternalRelations();

	public void addRelation(final Relation relation);

	public void removeRelation(final Relation relation);

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
	 * If this schema is identical across multiple source schemas, and the user
	 * wants to process each of those sequentially using the same schema
	 * settings, then the map returned by this call should be used to set up
	 * those partitions.
	 * <p>
	 * Note that the schema itself does not necessarily have to appear in the
	 * partition map - it is only a template by which each partition will be
	 * created.
	 * <p>
	 * The keys of the maps are strings - they can mean different things
	 * according to whether this is a JDBC schema, an XML schema, etc. The
	 * values are the prefix to stick on table names in datasets generated from
	 * this schema.
	 * 
	 * @return the map of partitions. If empty, then partitioning is not
	 *         required. It will never be <tt>null</tt>.
	 */
	public Map getPartitions();

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

		private final Map partitions = new TreeMap();

		private final Set relations = new HashSet();

		private final Set internalRelations = new HashSet();

		private final Set externalRelations = new HashSet();

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
			Log.info(Resources.get("logCreatingSchema", new String[] { name,
					"" + keyguessing }));
			this.name = name;
			this.keyguessing = keyguessing;
		}

		public void addRelation(final Relation relation) {
			this.relations.add(relation);
			if (relation.isExternal())
				this.externalRelations.add(relation);
			else
				this.internalRelations.add(relation);
		}

		public void removeRelation(final Relation relation) {
			this.relations.remove(relation);
			if (relation.isExternal())
				this.externalRelations.remove(relation);
			else
				this.internalRelations.remove(relation);
		}

		public Map getPartitions() {
			return this.partitions;
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
			Log.debug("Remapping table " + oldName + " to " + newName + " in "
					+ this.getName());
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

		public Collection getRelations() {
			return this.relations;
		}

		public Collection getExternalRelations() {
			return this.externalRelations;
		}

		public Collection getInternalRelations() {
			return this.internalRelations;
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
			Log
					.debug("Removing table " + tableName + " from "
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
			// Copy partitions.
			targetSchema.getPartitions().clear();
			targetSchema.getPartitions().putAll(this.getPartitions());

			// Drop all tables in target schema.
			targetSchema.removeAllTables();

			// Set up a set to contain all the relations to replicate.
			final Set relations = new HashSet();

			// Iterate over all the tables, and copy each one.
			for (final Iterator i = this.tables.values().iterator(); i
					.hasNext();) {
				final Table table = (Table) i.next();

				// Create a copy of the table.
				final Table newTable = new GenericTable(table.getName(),
						targetSchema);
				try {
					targetSchema.addTable(newTable);
				} catch (final Throwable t) {
					throw new BioMartError(t);
				}

				// Iterate over all the columns and copy them too.
				for (final Iterator j = table.getColumns().iterator(); j
						.hasNext();) {
					final Column col = (Column) j.next();
					try {
						final Column newCol = new GenericColumn(col.getName(),
								newTable);
						newTable.addColumn(newCol);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
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
						final PrimaryKey newPK = new GenericPrimaryKey(columns);
						newPK.setStatus(pk.getStatus());
						newTable.setPrimaryKey(newPK);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				}

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
						final ForeignKey newFK = new GenericForeignKey(columns);
						newFK.setStatus(fk.getStatus());
						newTable.addForeignKey(newFK);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				}

				// Remember the relations on this table for later.
				relations.addAll(table.getRelations());
			}

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
					final Key internalKey = r.getKeyForSchema(this);
					final Key externalKey = r.getOtherKey(internalKey);

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
					try {
						final Relation newRel = new GenericRelation(
								newInternalKey, externalKey, card);
						newRel.setStatus(status);
						newInternalKey.addRelation(newRel);
						externalKey.addRelation(newRel);
					} catch (final Throwable t) {
						throw new BioMartError(t);
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

					try {
						final Relation newRel = new GenericRelation(
								newFirstKey, newSecondKey, card);
						newRel.setCardinality(card);
						newRel.setStatus(status);
						newFirstKey.addRelation(newRel);
						newSecondKey.addRelation(newRel);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				}
			}
		}

		public void setKeyGuessing(final boolean keyguessing)
				throws SQLException, DataModelException {
			Log.info(Resources.get("logChangeKeyguessing", new String[] {
					"" + keyguessing, this.getName() }));
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
