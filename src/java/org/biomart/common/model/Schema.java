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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.biomart.common.exceptions.AssociationException;
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
import org.biomart.common.resources.Settings;

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
 *          $Author$
 * @since 0.5
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

	/**
	 * Returns a collection of all the internal relations in this schema, ie.
	 * those that link two tables in the same schema.
	 * 
	 * @return a set of internal relations.
	 */
	public Collection getInternalRelations();

	/**
	 * Returns a collection of all the external relations in this schema, ie.
	 * those that link two tables in different schemas.
	 * 
	 * @return a set of external relations.
	 */
	public Collection getExternalRelations();

	/**
	 * Adds a relation to the set known about by this schema.
	 * 
	 * @param relation
	 *            the relation to add.
	 */
	public void addRelation(final Relation relation);

	/**
	 * Removes a relation from the set known about by this schema.
	 * 
	 * @param relation
	 *            the relation to remove.
	 */
	public void removeRelation(final Relation relation);

	/**
	 * Checks whether this schema uses key-guessing or not.
	 * 
	 * @return <tt>true</tt> if it does, <tt>false</tt> if it doesn't.
	 */
	public boolean isKeyGuessing();

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
	 * <p>
	 * The entries in the map are the result of applying a combination of
	 * {@link #getPartitionRegex()} and {@link #getPartitionNameExpression()} to
	 * the list of available schemas in the database, as determined by the
	 * appropriate database driver.
	 * 
	 * @return the map of partitions. If empty, then partitioning is not
	 *         required. It will never be <tt>null</tt>.
	 * @throws SQLException
	 *             if the partitions could not be retrieved.
	 */
	public Map getPartitions() throws SQLException;

	/**
	 * Retrieve the regex used to work out schema partitions. If this regex is
	 * <tt>null</tt> then no partitioning will be done.
	 * 
	 * @return the regex used. Groups from this regex will be used to populate
	 *         values in the name expression. See
	 *         {@link #getPartitionNameExpression()}.
	 */
	public String getPartitionRegex();

	/**
	 * Retrieve the expression used to reformat groups from the partition regex
	 * into schema partition names.
	 * 
	 * @return the expression used. See also {@link #getPartitionRegex()}.
	 */
	public String getPartitionNameExpression();

	/**
	 * Set the regex used to work out schema partitions. If this regex is
	 * <tt>null</tt> then no partitioning will be done.
	 * 
	 * @param regex
	 *            the regex used. Groups from this regex will be used to
	 *            populate values in the name expression. See
	 *            {@link #setPartitionNameExpression(String)}.
	 */
	public void setPartitionRegex(final String regex);

	/**
	 * Set the expression used to reformat groups from the partition regex into
	 * schema partition names.
	 * 
	 * @param expr
	 *            the expression used. See also
	 *            {@link #setPartitionRegex(String)}.
	 */
	public void setPartitionNameExpression(final String expr);

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
		private static final long serialVersionUID = 1L;

		private boolean keyguessing;

		private String name;

		private String partitionRegex;

		private String partitionExpression;

		private final Map tables = new TreeMap();

		private final Set relations = new HashSet();

		private final Set internalRelations = new HashSet();

		private final Set externalRelations = new HashSet();

		public String getPartitionRegex() {
			return this.partitionRegex;
		}

		public String getPartitionNameExpression() {
			return this.partitionExpression;
		}

		public void setPartitionRegex(final String regex) {
			this.partitionRegex = regex;
		}

		public void setPartitionNameExpression(final String expr) {
			this.partitionExpression = expr;
		}

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

		public Map getPartitions() throws SQLException {
			return Collections.EMPTY_MAP;
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

		public boolean isKeyGuessing() {
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

		public void removeTableByName(final String tableName) {
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
			targetSchema.setPartitionRegex(this.getPartitionRegex());
			targetSchema.setPartitionNameExpression(this
					.getPartitionNameExpression());

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
			Log.info("Synchronising " + this.getName());
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
			return "" + this.getName();
		}
	}

	/**
	 * This implementation of the {@link Schema} interface connects to a JDBC
	 * data source and loads tables, keys and relations using database metadata.
	 * <p>
	 * If key-guessing is enabled, foreign keys are guessed instead of being
	 * read from the database. Guessing works by iterating through known primary
	 * keys, where the first column of the key matches the name of the table
	 * (optionally with '_id' appended), then iterating through all other tables
	 * looking for sets of columns with identical names, or names that have had
	 * '_key' appended. If it finds a matching set, then it assumes that it has
	 * found a foreign key, and establishes a relation between the two.
	 * <p>
	 * When using keyguessing, primary keys are read from database metadata, but
	 * if this method returns no results, then each table is searched for a
	 * column with the same name as the table, optionally with '_id' appended.
	 * If one is found, then it is assumed that that column is the primary key
	 * for the table.
	 * <p>
	 * This implementation is very careful not to override any hand-made
	 * relations or keys, or to reinstate any that have previously been marked
	 * as incorrect.
	 */
	public class JDBCSchema extends GenericSchema implements JDBCDataLink {
		private static final long serialVersionUID = 1L;

		private Connection connection;

		private String driverClassName;

		private String password;

		private String schemaName;

		private String url;

		private String username;

		/**
		 * <p>
		 * Establishes a JDBC connection from the information provided, and
		 * remembers it. Nothing is read yet - if you want to read the schema
		 * data, you must use the {@link #synchronise()} method to do so.
		 * 
		 * @param driverClassName
		 *            the class name of the JDBC driver, eg.
		 *            <tt>com.mysql.jdbc.Driver</tt>.
		 * @param url
		 *            the JDBC URL of the database server to connect to.
		 * @param schemaName
		 *            the database schema name to read tables from. In MySQL
		 *            this should be the same as the database name specified in
		 *            the JDBC URL. In Oracle and PostgreSQL, it is a distinct
		 *            entity.
		 * @param username
		 *            the username to connect as.
		 * @param password
		 *            the password to connect as. Defaults to no password if the
		 *            empty string is passed in.
		 * @param name
		 *            the name to give this schema after it has been created.
		 * @param keyGuessing
		 *            <tt>true</tt> if you want keyguessing enabled,
		 *            <tt>false</tt> otherwise.
		 */
		public JDBCSchema(final String driverClassName, final String url,
				final String schemaName, final String username,
				final String password, final String name,
				final boolean keyGuessing) {
			// Call the GenericSchema implementation first, to set up our name,
			// and
			// set up keyguessing.
			super(name, keyGuessing);

			Log.debug("Creating JDBC schema");

			// Remember the settings.
			this.driverClassName = driverClassName;
			this.url = url;
			this.username = username;
			this.password = password;
			this.schemaName = schemaName;

			// Add a thread so that any connection established gets closed when
			// the application exits.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						JDBCSchema.this.closeConnection();
					} catch (final Throwable t) {
						// We don't care if it fails, so ignore it.
					}
				}
			});
		}

		protected void finalize() throws Throwable {
			try {
				this.closeConnection();
			} finally {
				super.finalize();
			}
		}

		public Map getPartitions() throws SQLException {
			Log.debug("Looking up JDBC catalogs");
			if (this.getPartitionRegex() == null)
				return Collections.EMPTY_MAP;
			// Valid regex?
			Pattern p;
			try {
				p = Pattern.compile(this.getPartitionRegex());
			} catch (final PatternSyntaxException e) {
				// Ignore and return if invalid.
				return Collections.EMPTY_MAP;
			}
			// Use regex and friends to work out partitions.
			final Map partitions = new TreeMap();
			final Connection conn = this.getConnection(null);
			// List out all catalogs available.
			ResultSet rs = conn.getMetaData().getCatalogs();
			try {
				while (rs.next()) {
					final String schema = rs.getString(1);
					// Match them against the regex, retaining those
					// that match and using the name expression to name them.
					final Matcher m = p.matcher(schema);
					if (m.matches())
						try {
							partitions.put(schema, m.replaceAll(this
									.getPartitionNameExpression()));
						} catch (final IndexOutOfBoundsException e) {
							// We don't care if the expression is invalid.
						}
				}
			} catch (final SQLException e) {
				throw e;
			} finally {
				rs.close();
			}
			if (partitions.isEmpty()) {
				Log.debug("Looking up JDBC schemas instead");
				// Did we get no catalogs? Try schemas instead.
				rs = conn.getMetaData().getSchemas();
				try {
					while (rs.next()) {
						final String schema = rs.getString(1);
						// Match them against the regex, retaining those
						// that match and using the name expression to name
						// them.
						final Matcher m = p.matcher(schema);
						if (m.matches())
							try {
								partitions.put(schema, m.replaceAll(this
										.getPartitionNameExpression()));
							} catch (final IndexOutOfBoundsException e) {
								// We don't care if the expression is invalid.
							}
					}
				} catch (final SQLException e) {
					throw e;
				} finally {
					rs.close();
				}
			}
			// Return the results.
			return partitions;
		}

		/**
		 * Establish foreign keys based purely on database metadata.
		 * 
		 * @param fksToBeDropped
		 *            the list of foreign keys to update as we go along. By the
		 *            end of the method, the only keys left in this list should
		 *            be ones that no longer exist in the database and may be
		 *            dropped.
		 * @param dmd
		 *            the database metadata to obtain the foreign keys from.
		 * @param schema
		 *            the database schema to read metadata from.
		 * @param catalog
		 *            the database catalog to read metadata from.
		 * @throws SQLException
		 *             if there was a problem talking to the database.
		 * @throws DataModelException
		 *             if there was a logical problem during construction of the
		 *             set of foreign keys.
		 */
		private void synchroniseKeysUsingDMD(final Collection fksToBeDropped,
				final DatabaseMetaData dmd, final String schema,
				final String catalog) throws SQLException, DataModelException {
			Log.debug("Running DMD key synchronisation");
			// Loop through all the tables in the database, which is the same
			// as looping through all the primary keys.
			Log.debug("Finding tables");
			for (final Iterator i = this.getTables().iterator(); i.hasNext();) {
				// Obtain the table and its primary key.
				final Table pkTable = (Table) i.next();
				final PrimaryKey pk = pkTable.getPrimaryKey();
				// Skip all tables which have no primary key.
				if (pk == null)
					continue;

				Log.debug("Processing primary key " + pk);

				// Make a list of relations that already exist in this schema,
				// from some previous run. Any relations that are left in this
				// list by the end of the loop for this table no longer exist in
				// the database, and will be dropped.
				final Collection relationsToBeDropped = new HashSet(pk
						.getRelations());

				// Identify all foreign keys in the database metadata that refer
				// to the current primary key.
				Log.debug("Finding referring foreign keys");
				final ResultSet dbTblFKCols = dmd.getExportedKeys(catalog,
						schema, pkTable.getName());

				// Loop through the results. There will be one result row per
				// column per key, so we need to build up a set of key columns
				// in a map.
				// The map keys represent the column position within a key. Each
				// map value is a list of columns. In essence the map is a 2-D
				// representation of the foreign keys which refer to this PK,
				// with the keys of the map (Y-axis) representing the column
				// position in the FK, and the values of the map (X-axis)
				// representing each individual FK. In all cases, FK columns are
				// assumed to be in the same order as the PK columns. The map is
				// sorted by key column position.
				final TreeMap dbFKs = new TreeMap();
				while (dbTblFKCols.next()) {
					final String fkTblName = dbTblFKCols
							.getString("FKTABLE_NAME");
					final String fkColName = dbTblFKCols
							.getString("FKCOLUMN_NAME");
					final Short fkColSeq = new Short(dbTblFKCols
							.getShort("KEY_SEQ"));
					// Note the column.
					if (!dbFKs.containsKey(fkColSeq))
						dbFKs.put(fkColSeq, new ArrayList());
					// In Oracle, FKs can be invalid, so we need to check them.
					final Table fkTbl = this.getTableByName(fkTblName);
					if (fkTbl != null) {
						final Column fkCol = fkTbl.getColumnByName(fkColName);
						if (fkCol != null)
							((List) dbFKs.get(fkColSeq)).add(fkCol);
					}
				}
				dbTblFKCols.close();

				// Only construct FKs if we actually found any.
				if (!dbFKs.isEmpty()) {
					// Identify the sequence of the first column, which may be 0
					// or
					// 1, depending on database implementation.
					final int firstColSeq = ((Short) dbFKs.firstKey())
							.intValue();

					// How many columns are in the PK?
					final int pkColCount = pkTable.getPrimaryKey()
							.countColumns();

					// How many FKs do we have?
					final int fkCount = ((List) dbFKs.get(dbFKs.firstKey()))
							.size();

					// Loop through the FKs, and construct each one at a time.
					for (int j = 0; j < fkCount; j++) {
						// Set up an array to hold the FK columns.
						final Column[] candidateFKColumns = new Column[pkColCount];

						// For each FK column name, look up the actual column in
						// the
						// table.
						for (final Iterator k = dbFKs.entrySet().iterator(); k
								.hasNext();) {
							final Map.Entry entry = (Map.Entry) k.next();
							final Short keySeq = (Short) entry.getKey();
							// Convert the db-specific column index to a
							// 0-indexed
							// figure for the array of fk columns.
							final int fkColSeq = keySeq.intValue()
									- firstColSeq;
							candidateFKColumns[fkColSeq] = (Column) ((List) entry
									.getValue()).get(j);
						}

						// Create a template foreign key based around the set
						// of candidate columns we found.
						ForeignKey fk;
						try {
							fk = new GenericForeignKey(Arrays
									.asList(candidateFKColumns));
						} catch (final Throwable t) {
							throw new BioMartError(t);
						}
						final Table fkTable = fk.getTable();

						// If any FK already exists on the target table with the
						// same columns in the same order, then reuse it.
						boolean fkAlreadyExists = false;
						for (final Iterator f = fkTable.getForeignKeys()
								.iterator(); f.hasNext() && !fkAlreadyExists;) {
							final ForeignKey candidateFK = (ForeignKey) f
									.next();
							if (candidateFK.equals(fk)) {
								// Found one. Reuse it!
								fk = candidateFK;
								// Update the status to indicate that the FK is
								// backed by the database, if previously it was
								// handmade.
								if (fk.getStatus().equals(
										ComponentStatus.HANDMADE))
									fk.setStatus(ComponentStatus.INFERRED);
								// Remove the FK from the list to be dropped
								// later,
								// as it definitely exists now.
								fksToBeDropped.remove(candidateFK);
								// Flag the key as existing.
								fkAlreadyExists = true;
							}
						}

						// Has the key been reused, or is it a new one?
						if (!fkAlreadyExists) {
							// Its brand new, so go ahead and make the relation.
							try {
								fkTable.addForeignKey(fk);
							} catch (final Throwable t) {
								throw new BioMartError(t);
							}

							// Work out whether the relation from the FK to the
							// PK should be 1:M or 1:1. The rule is that it will
							// be 1:M in all cases except where the FK table has
							// a PK with identical columns to the FK, in which
							// case it is 1:1, as the FK is unique.
							Cardinality card = Cardinality.MANY;
							final PrimaryKey fkPK = fkTable.getPrimaryKey();
							if (fkPK != null
									&& fk.getColumns()
											.equals(fkPK.getColumns()))
								card = Cardinality.ONE;

							// Establish the relation.
							try {
								final Relation rel = new GenericRelation(pk,
										fk, card);
								pk.addRelation(rel);
								fk.addRelation(rel);
							} catch (final Throwable t) {
								throw new BioMartError(t);
							}
						} else {
							// If the FK has been reused, check to see if it
							// already has a relation. There are three possible
							// situations here:
							// a) the relation exists between the FK and the PK
							// already, in which case we can reuse it,
							// b) the FK has no existing relation, in which case
							// we can create one, and
							// c) the FK has an existing relation to some other
							// PK, which must be preserved.

							// Iterate through the existing relations on the
							// key.
							boolean relationExists = false;
							for (final Iterator f = fk.getRelations()
									.iterator(); f.hasNext() && !relationExists;) {
								// Obtain the next relation.
								final Relation candidateRel = (Relation) f
										.next();

								// a) a relation already exists between the FK
								// and the PK.
								if (candidateRel.getOtherKey(fk).equals(pk)) {
									// Don't drop it at the end of the loop.
									relationsToBeDropped.remove(candidateRel);
									// Say we've found it.
									relationExists = true;
								}

								// b.i) an incorrect relation exists somewhere
								// else, which we should drop now because the
								// database no longer infers it, so in fact we
								// need do nothing here.

								else if (candidateRel.getStatus().equals(
										ComponentStatus.INFERRED_INCORRECT)) {
									// Do nothing.
								}

								// b.ii) an inferred or handmade relation exists
								// somewhere else, which we can leave intact
								// but make it handmade.
								else {
									// Don't drop it at the end of the loop.
									relationsToBeDropped.remove(candidateRel);
									// Change it to handmade.
									try {
										candidateRel
												.setStatus(ComponentStatus.HANDMADE);
									} catch (final AssociationException e) {
										// Should never happen.
										throw new BioMartError(e);
									}
								}
							}

							// If relation did not already exist, create it.
							if (!relationExists) {
								// Work out whether the relation from the FK to
								// the PK should be 1:M or 1:1. The rule is that
								// it will be 1:M in all cases except where the
								// FK table has a PK with identical columns to
								// the FK, in which case it is 1:1, as the FK
								// is unique.
								Cardinality card = Cardinality.MANY;
								final PrimaryKey fkPK = fkTable.getPrimaryKey();
								if (fkPK != null
										&& fk.getColumns().equals(
												fkPK.getColumns()))
									card = Cardinality.ONE;

								// Establish the relation.
								try {
									new GenericRelation(pk, fk, card);
								} catch (final Throwable t) {
									throw new BioMartError(t);
								}
							}
						}
					}
				}

				// Remove any relations that we didn't find in the database (but
				// leave the handmade ones behind).
				for (final Iterator j = relationsToBeDropped.iterator(); j
						.hasNext();) {
					final Relation r = (Relation) j.next();
					if (r.getStatus().equals(ComponentStatus.HANDMADE))
						continue;
					r.destroy();
				}
			}
		}

		/**
		 * This method implements the key-guessing algorithm for foreign keys.
		 * Basically, it iterates through all known primary keys, and looks for
		 * sets of matching columns in other tables, either with the same names
		 * or with '_key' appended. Any matching sets found are assumed to be
		 * foreign keys with relations to the current primary key.
		 * <p>
		 * Relations are 1:M, except when the table at the FK end has a PK with
		 * identical column to the FK. In this case, the FK is forced to be
		 * unique, which implies that it can only partake in a 1:1 relation, so
		 * the relation is marked as such.
		 * 
		 * @param fksToBeDropped
		 *            the list of foreign keys to update as we go along. By the
		 *            end of the method, the only keys left in this list should
		 *            be ones that no longer exist in the database and may be
		 *            dropped.
		 * @throws SQLException
		 *             if there was a problem talking to the database.
		 * @throws DataModelException
		 *             if there was a logical problem during construction of the
		 *             set of foreign keys.
		 */
		private void synchroniseKeysUsingKeyGuessing(
				final Collection fksToBeDropped) throws SQLException,
				DataModelException {
			Log.debug("Running non-DMD key synchronisation");
			// Loop through all the tables in the database, which is the same
			// as looping through all the primary keys.
			Log.debug("Finding tables");
			for (final Iterator i = this.getTables().iterator(); i.hasNext();) {
				// Obtain the table and its primary key.
				final Table pkTable = (Table) i.next();
				final PrimaryKey pk = pkTable.getPrimaryKey();
				// Skip all tables which have no primary key.
				if (pk == null)
					continue;

				Log.debug("Processing primary key " + pk);

				// If an FK exists on the PK table with the same columns as the
				// PK, then we cannot use this PK to make relations to other
				// tables.
				// This is because the FK shows that this table is not the
				// original source of the data in those columns. Some other
				// table is the original source, so we assume that relations
				// will
				// have been established from that other table instead. So, we
				// skip this table.
				boolean pkIsAlsoAnFK = false;
				for (final Iterator j = pkTable.getForeignKeys().iterator(); j
						.hasNext()
						&& !pkIsAlsoAnFK;) {
					final Key fk = (Key) j.next();
					if (fk.getColumns().equals(pk.getColumns()))
						pkIsAlsoAnFK = true;
				}
				if (pkIsAlsoAnFK)
					continue;

				// To maintain some degree of sanity here, we assume that a PK
				// is the original source of data (and not a copy of data
				// sourced
				// from some other table) if the first column in the PK has the
				// same name as the table it is in, or with '_id' appended, or
				// is
				// just 'id' on its own. Any PK which does not have this
				// property
				// is skipped.
				final Column firstPKCol = (Column) pk.getColumns().get(0);
				String firstPKColName = firstPKCol.getName();
				final int idPrefixIndex = firstPKColName.indexOf(Resources
						.get("primaryKeySuffix"));
				if (idPrefixIndex >= 0)
					firstPKColName = firstPKColName.substring(0, idPrefixIndex);
				if (!firstPKColName.equals(pkTable.getName())
						&& !firstPKColName.equals(Resources.get("idCol")))
					continue;

				// Make a list of relations that already exist in this schema,
				// from some previous run. Any relations that are left in this
				// list by the end of the loop for this table no longer exist in
				// the database, and will be dropped.
				final Collection relationsToBeDropped = new HashSet(pk
						.getRelations());

				// Now we know that we can use this PK for certain, look for all
				// other tables (other than the one the PK itself belongs to),
				// for sets of columns with identical names, or with '_key'
				// appended. Any set that we find is going to be an FK with a
				// relation back to this PK.
				Log.debug("Searching for possible referring foreign keys");
				for (final Iterator l = this.getTables().iterator(); l
						.hasNext();) {
					// Obtain the next table to look at.
					final Table fkTable = (Table) l.next();

					// Make sure the table is not the same as the PK table.
					if (fkTable.equals(pkTable))
						continue;

					// Set up an empty list for the matching columns.
					final Column[] candidateFKColumns = new Column[pk
							.countColumns()];
					int matchingColumnCount = 0;

					// Iterate through the PK columns and find a column in the
					// target FK table with the same name, or with '_key'
					// appended,
					// or with the PK table name and an underscore prepended.
					// If found, add that target column to the candidate FK
					// column
					// set.
					for (int columnIndex = 0; columnIndex < pk.countColumns(); columnIndex++) {
						final String pkColumnName = (String) pk
								.getColumnNames().get(columnIndex);
						// Start out by assuming no match.
						Column candidateFKColumn = null;
						// Don't try to find 'id' or 'id_key' columns as that
						// would be silly and would probably match far too much.
						if (!pkColumnName.equals(Resources.get("idCol"))) {
							// Try equivalent name first.
							candidateFKColumn = fkTable
									.getColumnByName(pkColumnName);
							// Then try with '_key' appended, if not found.
							if (candidateFKColumn == null)
								candidateFKColumn = fkTable
										.getColumnByName(pkColumnName
												+ Resources
														.get("foreignKeySuffix"));
						}
						// Then try with PK tablename+'_' prepended, if not
						// found.
						if (candidateFKColumn == null)
							candidateFKColumn = fkTable.getColumnByName(pkTable
									.getName()
									+ "_" + pkColumnName);
						// Found it? Add it to the candidate list.
						if (candidateFKColumn != null) {
							candidateFKColumns[columnIndex] = candidateFKColumn;
							matchingColumnCount++;
						}
					}

					// We found a matching set, so create a FK on it!
					if (matchingColumnCount == pk.countColumns()) {
						// Create a template foreign key based around the set
						// of candidate columns we found.
						ForeignKey fk;
						try {
							fk = new GenericForeignKey(Arrays
									.asList(candidateFKColumns));
						} catch (final Throwable t) {
							throw new BioMartError(t);
						}

						// If any FK already exists on the target table with the
						// same columns in the same order, then reuse it.
						boolean fkAlreadyExists = false;
						for (final Iterator f = fkTable.getForeignKeys()
								.iterator(); f.hasNext() && !fkAlreadyExists;) {
							final ForeignKey candidateFK = (ForeignKey) f
									.next();
							if (candidateFK.equals(fk)) {
								// Found one. Reuse it!
								fk = candidateFK;
								// Update the status to indicate that the FK is
								// backed by the database, if previously it was
								// handmade.
								if (fk.getStatus().equals(
										ComponentStatus.HANDMADE))
									fk.setStatus(ComponentStatus.INFERRED);
								// Remove the FK from the list to be dropped
								// later,
								// as it definitely exists now.
								fksToBeDropped.remove(fk);
								// Flag the key as existing.
								fkAlreadyExists = true;
							}
						}

						// Has the key been reused, or is it a new one?
						if (!fkAlreadyExists) {
							// Its brand new, so go ahead and make the relation.
							try {
								fkTable.addForeignKey(fk);
							} catch (final Throwable t) {
								throw new BioMartError(t);
							}

							// Work out whether the relation from the FK to the
							// PK should be 1:M or 1:1. The rule is that it will
							// be 1:M in all cases except where the FK table has
							// a PK with identical columns to the FK, in which
							// case it is 1:1, as the FK is unique.
							Cardinality card = Cardinality.MANY;
							final PrimaryKey fkPK = fkTable.getPrimaryKey();
							if (fkPK != null
									&& fk.getColumns()
											.equals(fkPK.getColumns()))
								card = Cardinality.ONE;

							// Establish the relation.
							try {
								final Relation rel = new GenericRelation(pk,
										fk, card);
								pk.addRelation(rel);
								fk.addRelation(rel);
							} catch (final Throwable t) {
								throw new BioMartError(t);
							}
						} else {
							// If the FK has been reused, check to see if it
							// already has a relation. There are three possible
							// situations here:
							// a) the relation exists between the FK and the PK
							// already, in which case we can reuse it,
							// b) the FK has no existing relation, in which case
							// we can create one, and
							// c) the FK has an existing relation to some other
							// PK, which must be preserved.

							// Iterate through the existing relations on the
							// key.
							boolean relationExists = false;
							for (final Iterator f = fk.getRelations()
									.iterator(); f.hasNext() && !relationExists;) {
								// Obtain the next relation.
								final Relation candidateRel = (Relation) f
										.next();

								// a) a relation already exists between the FK
								// and the PK.
								if (candidateRel.getOtherKey(fk).equals(pk)) {
									// Don't drop it at the end of the loop.
									relationsToBeDropped.remove(candidateRel);

									// Say we've found it.
									relationExists = true;
								}

								// b.i) an incorrect relation exists somewhere
								// else, which we should drop now because the
								// database no longer infers it, so in fact we
								// need do nothing here.

								else if (candidateRel.getStatus().equals(
										ComponentStatus.INFERRED_INCORRECT)) {
									// Do nothing.
								}

								// b.ii) an inferred or handmade relation exists
								// somewhere else, which we can leave intact
								// but make it handmade.
								else {
									// Don't drop it at the end of the loop.
									relationsToBeDropped.remove(candidateRel);
									// Change it to handmade.
									try {
										candidateRel
												.setStatus(ComponentStatus.HANDMADE);
									} catch (final AssociationException e) {
										// Should never happen.
										throw new BioMartError(e);
									}
								}
							}

							// If relation did not already exist, create it.
							if (!relationExists) {
								// Work out whether the relation from the FK to
								// the PK should be 1:M or 1:1. The rule is that
								// it will be 1:M in all cases except where the
								// FK table has a PK with identical columns to
								// the FK, in which case it is 1:1, as the FK
								// is unique.
								Cardinality card = Cardinality.MANY;
								final PrimaryKey fkPK = fkTable.getPrimaryKey();
								if (fkPK != null
										&& fk.getColumns().equals(
												fkPK.getColumns()))
									card = Cardinality.ONE;

								// Establish the relation.
								try {
									new GenericRelation(pk, fk, card);
								} catch (final Throwable t) {
									throw new BioMartError(t);
								}
							}
						}
					}
				}

				// Remove any relations that we didn't find in the database (but
				// leave the handmade ones behind).
				for (final Iterator j = relationsToBeDropped.iterator(); j
						.hasNext();) {
					final Relation r = (Relation) j.next();
					if (r.getStatus().equals(ComponentStatus.HANDMADE))
						continue;
					r.destroy();
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In our case, cohabitation means that the partner link is also a
		 * {@link JDBCDataLink} and that its connection is connected to the same
		 * database server listening on the same port and connected with the
		 * same username.
		 */
		public boolean canCohabit(final DataLink partner) {
			Log.debug("Testing " + partner + " against " + this
					+ " for cohabitation");
			// We can't cohabit with non-JDBCDataLink partners.
			if (!(partner instanceof JDBCDataLink))
				return false;
			final JDBCDataLink partnerLink = (JDBCDataLink) partner;

			// Work out the partner's catalogs and schemas.
			final Collection partnerSchemas = new HashSet();
			try {
				final DatabaseMetaData dmd = partnerLink.getConnection(null)
						.getMetaData();
				// We need to compare by catalog only.
				final ResultSet catalogs = dmd.getCatalogs();
				while (catalogs.next())
					partnerSchemas.add(catalogs.getString("TABLE_CAT"));
				return partnerSchemas.contains(this.getConnection(null)
						.getCatalog());
			} catch (final Throwable t) {
				// If get an error, assume can't find anything, thus assume
				// incompatible.
				return false;
			}
		}

		public Connection getConnection(final String partition)
				throws SQLException {
			// If we are already connected, test to see if we are
			// still connected. If not, reset our connection.
			if (this.connection != null && this.connection.isClosed())
				try {
					Log.debug("Closing dead JDBC connection");
					this.connection.close();
				} catch (final SQLException e) {
					// We don't care. Ignore it.
				} finally {
					this.connection = null;
				}

			// If we are not connected, we should attempt to (re)connect now.
			if (this.connection == null) {
				Log.debug("Establishing JDBC connection");
				// Start out with no driver at all.
				Class loadedDriverClass = null;

				// Try the system class loader instead.
				try {
					loadedDriverClass = Class.forName(this.driverClassName);
				} catch (final ClassNotFoundException e) {
					final SQLException e2 = new SQLException();
					e2.initCause(e);
					throw e2;
				}

				// Check it really is an instance of Driver.
				if (!Driver.class.isAssignableFrom(loadedDriverClass))
					throw new ClassCastException(Resources
							.get("driverClassNotJDBCDriver"));

				// Connect!
				final Properties properties = new Properties();
				properties.setProperty("user", this.username);
				if (!this.password.equals(""))
					properties.setProperty("password", this.password);
				this.connection = DriverManager.getConnection(
						partition == null ? this.url : this.url.replaceAll(
								this.schemaName, partition), properties);

				// Check the schema name.
				final DatabaseMetaData dmd = this.connection.getMetaData();
				final String catalog = this.connection.getCatalog();
				ResultSet rs = dmd.getTables(catalog, this.schemaName, "%",
						null);
				if (!rs.isBeforeFirst()) {
					rs = dmd.getTables(catalog, this.schemaName.toUpperCase(),
							"%", null);
					if (rs.isBeforeFirst())
						this.schemaName = this.schemaName.toUpperCase();
				}
				if (!rs.isBeforeFirst()) {
					rs = dmd.getTables(catalog, this.schemaName.toUpperCase(),
							"%", null);
					if (rs.isBeforeFirst())
						this.schemaName = this.schemaName.toLowerCase();
				}
				rs.close();
			}

			// Return the connection.
			return this.connection;
		}

		private void closeConnection() throws SQLException {
			Log.debug("Closing JDBC connection");
			if (this.connection != null)
				try {
					this.connection.close();
				} finally {
					this.connection = null;
				}
		}

		public String getDatabaseSchema() {
			return this.schemaName;
		}

		public String getDriverClassName() {
			return this.driverClassName;
		}

		public String getJDBCURL() {
			return this.url;
		}

		public String getPassword() {
			return this.password;
		}

		public String getUsername() {
			return this.username;
		}

		public Schema replicate(final String newName) {
			Log.debug("Replicating JDBC schema " + this + " as " + newName);
			// Make an empty copy.
			final Schema newSchema = new JDBCSchema(this.driverClassName,
					this.url, this.schemaName, this.username, this.password,
					newName, this.isKeyGuessing());

			// Copy the contents over.
			this.replicateContents(newSchema);

			// Return the copy.
			return newSchema;
		}

		public void setDatabaseSchema(final String schemaName) {
			if (this.schemaName != null && !this.schemaName.equals(schemaName)) {
				this.schemaName = schemaName;
				// Reset the cached database connection.
				try {
					this.closeConnection();
				} catch (final SQLException e) {
					// We don't care.
				}
			}
		}

		public void setDriverClassName(final String driverClassName) {
			if (this.driverClassName != null
					&& !this.driverClassName.equals(driverClassName)) {
				this.driverClassName = driverClassName;
				// Reset the cached database connection.
				try {
					this.closeConnection();
				} catch (final SQLException e) {
					// We don't care.
				}
			}
		}

		public void setJDBCURL(final String url) {
			if (this.url != null && !this.url.equals(url)) {
				this.url = url;
				// Reset the cached database connection.
				try {
					this.closeConnection();
				} catch (final SQLException e) {
					// We don't care.
				}
			}
		}

		public void setPassword(final String password) {
			if (this.password == null && this.password != password
					|| this.password != null && !this.password.equals(password)) {
				this.password = password;
				// Reset the cached database connection.
				try {
					this.closeConnection();
				} catch (final SQLException e) {
					// We don't care.
				}
			}
		}

		public void setUsername(final String username) {
			if (this.username != null && !this.username.equals(username)) {
				this.username = username;
				// Reset the cached database connection.
				try {
					this.closeConnection();
				} catch (final SQLException e) {
					// We don't care.
				}
			}
		}

		public void synchronise() throws SQLException, DataModelException {
			Log.info("Synchronising " + this);
			// Get database metadata, catalog, and schema details.
			final DatabaseMetaData dmd = this.getConnection(null).getMetaData();
			final String catalog = this.getConnection(null).getCatalog();

			// Create a list of existing tables. During this method, we remove
			// from
			// this list all tables that still exist in the database. At the end
			// of
			// the method, the list contains only those tables which no longer
			// exist, so they will be dropped.
			final Collection tablesToBeDropped = new HashSet(this.getTables());

			// Load tables and views from database, then loop over them.
			final ResultSet dbTables = dmd.getTables(catalog, this.schemaName,
					"%", new String[] { "TABLE", "VIEW", "ALIAS", "SYNONYM" });

			// Do the loop.
			while (dbTables.next()) {
				// What is the table called?
				final String dbTableName = dbTables.getString("TABLE_NAME");
				Log.debug("Processing table " + dbTableName);

				// Look to see if we already have a table by this name defined.
				// If
				// we do, reuse it. If not, create a new table.
				Table dbTable = this.getTableByName(dbTableName);
				if (dbTable == null)
					try {
						dbTable = new GenericTable(dbTableName, this);
						this.addTable(dbTable);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}

				// Table exists, so remove it from our list of tables to be
				// dropped
				// at the end of the method.
				tablesToBeDropped.remove(dbTable);

				// Make a list of all the columns in the table. Any columns
				// remaining in this list by the end of the loop will be
				// dropped.
				final Collection colsToBeDropped = new HashSet(dbTable
						.getColumns());

				// Load the table columns from the database, then loop over
				// them.
				Log.debug("Loading table column list");
				final ResultSet dbTblCols = dmd.getColumns(catalog,
						this.schemaName, dbTableName, "%");
				// FIXME: When using Oracle, if the table is a synonym then the
				// above call returns no results.
				while (dbTblCols.next()) {
					// What is the column called, and is it nullable?
					final String dbTblColName = dbTblCols
							.getString("COLUMN_NAME");
					Log.debug("Processing column " + dbTblColName);

					// Look to see if the column already exists on this table.
					// If it
					// does, reuse it. Else, create it.
					Column dbTblCol = dbTable.getColumnByName(dbTblColName);
					if (dbTblCol == null)
						try {
							dbTblCol = new GenericColumn(dbTblColName, dbTable);
							dbTable.addColumn(dbTblCol);
						} catch (final Throwable t) {
							throw new BioMartError(t);
						}

					// Column exists, so remove it from our list of columns to
					// be
					// dropped at the end of the loop.
					colsToBeDropped.remove(dbTblCol);
				}
				dbTblCols.close();

				// Drop all columns that are left in the list, as they no longer
				// exist in the database.
				for (final Iterator i = colsToBeDropped.iterator(); i.hasNext();) {
					final Column column = (Column) i.next();
					Log.debug("Dropping redundant column " + column);
					dbTable.removeColumn(column);
				}
			}
			dbTables.close();

			// Remove from schema all tables not found in the database, using
			// the
			// list we constructed above.
			for (final Iterator i = tablesToBeDropped.iterator(); i.hasNext();) {
				final Table existingTable = (Table) i.next();
				Log.debug("Dropping redundant table " + existingTable);
				final String tableName = existingTable.getName();
				existingTable.destroy();
				this.removeTableByName(tableName);
			}

			// Sync the keys.
			this.synchroniseKeys();
			Log.info("Done synchronising");
		}

		public void synchroniseKeys() throws SQLException, DataModelException {
			Log.debug("Synchronising JDBC schema keys");
			Log.debug("Loading connection metadata");
			final DatabaseMetaData dmd = this.getConnection(null).getMetaData();
			Log.debug("Loading database catalog");
			final String catalog = this.getConnection(null).getCatalog();
			final String schema = this.schemaName;

			// Get and create primary keys.
			// Work out a list of all foreign keys currently existing.
			// Any remaining in this list later will be dropped.
			final Collection fksToBeDropped = new HashSet();
			for (final Iterator i = this.getTables().iterator(); i.hasNext();) {
				final Table t = (Table) i.next();
				fksToBeDropped.addAll(t.getForeignKeys());

				// Obtain the primary key from the database. Even in databases
				// without referential integrity, the primary key is still
				// defined
				// and can be obtained from the metadata.
				Log.debug("Loading table primary keys");
				final ResultSet dbTblPKCols = dmd.getPrimaryKeys(catalog,
						this.schemaName, t.getName());

				// Load the primary key columns into a map keyed by column
				// position.
				// In other words, the first column in the key has a map key of
				// 1,
				// and so on. We do this because we can't guarantee we'll read
				// the
				// key columns from the database in the correct order. We keep
				// the
				// map
				// sorted, so that when we iterate over it later we get back the
				// columns in the correct order.
				final Map pkCols = new TreeMap();
				while (dbTblPKCols.next()) {
					final String pkColName = dbTblPKCols
							.getString("COLUMN_NAME");
					final Short pkColPosition = new Short(dbTblPKCols
							.getShort("KEY_SEQ"));
					pkCols.put(pkColPosition, t.getColumnByName(pkColName));
				}
				dbTblPKCols.close();

				// Did DMD find a PK? If not, which is really unusual but
				// potentially may happen, attempt to find one by looking for a
				// single
				// column with the same name as the table or with '_id'
				// appended.
				// Only do this if we are using key-guessing.
				if (pkCols.isEmpty() && this.isKeyGuessing()) {
					Log
							.debug("Found no primary key, so attempting to guess one");
					// Plain version first.
					Column candidateCol = t.getColumnByName(t.getName());
					// Try with '_id' appended if plain version turned up
					// nothing.
					if (candidateCol == null)
						candidateCol = t.getColumnByName(t.getName()
								+ Resources.get("primaryKeySuffix"));
					// Found something? Add it to the primary key columns map,
					// with
					// a dummy key of 1. (Use Short for the key because that is
					// what
					// DMD would have used had it found anything itself).
					if (candidateCol != null)
						pkCols.put(Short.valueOf("1"), candidateCol);
				}

				// Obtain the existing primary key on the table, if the table
				// previously existed and even had one in the first place.
				final PrimaryKey existingPK = t.getPrimaryKey();

				// Did we find a PK on the database copy of the table?
				if (!pkCols.isEmpty()) {

					// Yes, we found a PK on the database copy of the table. So,
					// create a new key based around the columns we identified.
					PrimaryKey candidatePK;
					try {
						candidatePK = new GenericPrimaryKey(new ArrayList(
								pkCols.values()));
					} catch (final Throwable th) {
						throw new BioMartError(th);
					}

					// If the existing table has no PK, or has a PK which
					// matches
					// and is handmade, or has a PK which does not match and is
					// not
					// handmade, replace that PK with the one we found. This way
					// we
					// preserve any existing handmade PKs, and don't override
					// any
					// marked as incorrect.
					if (existingPK == null
							|| existingPK.equals(candidatePK)
							&& existingPK.getStatus().equals(
									ComponentStatus.HANDMADE)
							|| !existingPK.equals(candidatePK)
							&& !existingPK.getStatus().equals(
									ComponentStatus.HANDMADE))
						try {
							t.setPrimaryKey(candidatePK);
						} catch (final Throwable th) {
							throw new BioMartError(th);
						}
				} else // No, we did not find a PK on the database copy of the
				// table, so that table should not have a PK at all. So if the
				// existing table has a PK which is not handmade, remove it.
				if (existingPK != null
						&& !existingPK.getStatus().equals(
								ComponentStatus.HANDMADE))
					try {
						t.setPrimaryKey(null);
					} catch (final Throwable th) {
						throw new BioMartError(th);
					}
			}

			// Are we key-guessing? Key guess the foreign keys, passing in a
			// reference to the list of existing foreign keys. After this call
			// has
			// completed, the list will contain all those foreign keys which no
			// longer exist, and can safely be dropped.
			if (this.isKeyGuessing())
				this.synchroniseKeysUsingKeyGuessing(fksToBeDropped);
			// Otherwise, use DMD to do the same, also passing in the list of
			// existing foreign keys to be updated as the call progresses. Also
			// pass
			// in the DMD details so it doesn't have to work them out for
			// itself.
			else
				this.synchroniseKeysUsingDMD(fksToBeDropped, dmd, schema,
						catalog);

			// Drop any foreign keys that are left over (but not handmade ones).
			for (final Iterator i = fksToBeDropped.iterator(); i.hasNext();) {
				final Key k = (Key) i.next();
				if (k.getStatus().equals(ComponentStatus.HANDMADE))
					continue;
				Log.debug("Dropping redundant foreign key " + k);
				k.destroy();
			}
			Log.debug("Done synchronising JDBC schema keys");
		}

		public void storeInHistory() {
			// Store the schema settings in the history file.
			final Properties history = new Properties();
			history.setProperty("driverClass", this.getDriverClassName());
			history.setProperty("jdbcURL", this.getJDBCURL());
			history.setProperty("username", this.getUsername());
			history.setProperty("password", this.getPassword() == null ? ""
					: this.getPassword());
			history.setProperty("schema", this.getDatabaseSchema());
			Settings.saveHistoryProperties(JDBCSchema.class, this.getName(),
					history);
		}

		public boolean test() throws Exception {
			// Establish the JDBC connection. May throw an exception of its own,
			// which is fine, just let it go.
			final Connection connection = this.getConnection(null);
			// If we have no connection, we can't test it!
			if (connection == null)
				return false;

			// Get the metadata.
			Log.debug("Loading connection metadata");
			final DatabaseMetaData dmd = connection.getMetaData();

			// By opening, executing, then closing a DMD query we will test
			// the connection fully without actually having to read anything
			// from
			// it.
			Log.debug("Loading database catalog");
			final String catalog = connection.getCatalog();
			Log.debug("Loading list of database tables");
			final ResultSet rs = dmd.getTables(catalog, this.schemaName, "%",
					null);
			final boolean worked = rs.isBeforeFirst();
			rs.close();

			// If we get here, it worked.
			return worked;
		}
	}
}
