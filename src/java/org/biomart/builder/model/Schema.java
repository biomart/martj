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

package org.biomart.builder.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>
 * A schema provides one or more table objects with unique names for the user to
 * use. It could be a relational database, or an XML document, or any other
 * source of potentially tabular information.
 * <p>
 * The generic implementation provided should suffice for most tasks involved
 * with keeping track of the tables a schema provides.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.11, 1st June 2006
 * @since 0.1
 */
public interface Schema extends Comparable, DataLink {
	/**
	 * Returns the name of this schema.
	 * 
	 * @return the name of this schema.
	 */
	public String getName();

	/**
	 * Sets the name of this schema.
	 * 
	 * @param name
	 *            the new name of this schema.
	 */
	public void setName(String name);

	/**
	 * <p>
	 * Synchronise this schema with the data source that is providing its
	 * tables. Synchronisation means checking the list of tables available and
	 * drop/add any that have changed, then check each column. and key and
	 * relation and update those too.
	 * <p>
	 * Any key or relation that was created by the user and is still valid, ie.
	 * the underlying columns still exist, will not be affected by this
	 * operation.
	 * 
	 * @throws SQLException
	 *             if there was a problem connecting to the data source.
	 * @throws BuilderException
	 *             if there was any other kind of logical problem.
	 */
	public void synchronise() throws SQLException, BuilderException;

	/**
	 * Adds a table to this schema. The table must not already exist (ie. with
	 * the same name).
	 * 
	 * @param table
	 *            the table to add.
	 * @throws AlreadyExistsException
	 *             if another one with the same name already exists.
	 * @throws AssociationException
	 *             if the table doesn't claim that it belongs to this schema.
	 */
	public void addTable(Table table) throws AlreadyExistsException,
			AssociationException;

	/**
	 * Returns all the tables this schema provides. The set returned may be
	 * empty but it will never be null.
	 * 
	 * @return the set of all tables in this schema.
	 */
	public Collection getTables();

	/**
	 * Returns the tables from this schema with the given name. If there is no
	 * such table, the method will return null.
	 * 
	 * @param name
	 *            the name of the table to retrieve.
	 * @return the matching tables from this provider.
	 */
	public Table getTableByName(String name);

	/**
	 * Attempts to rename a table. If the new name has already been taken by
	 * another table, an exception is thrown. The rename does not affect the
	 * table itself, only the representation of the table within this schema. If
	 * the names are the same, nothing happens.
	 * 
	 * @param oldName
	 *            the old name of the table.
	 * @param newName
	 *            the new name of the table.
	 * @throws AlreadyExistsException
	 *             if the new name has already been used elsewhere.
	 */
	public void changeTableMapKey(String oldName, String newName)
			throws AlreadyExistsException;

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
	 * Enables or disables key-guessing on this schema.
	 * 
	 * @param keyguessing
	 *            <tt>true</tt> to enable it, <tt>false</tt> to disable it.
	 */
	public void setKeyGuessing(boolean keyguessing);

	/**
	 * Checks whether this schema uses key-guessing or not.
	 * 
	 * @return <tt>true</tt> if it does, <tt>false</tt> if it doesn't.
	 */
	public boolean getKeyGuessing();

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
	 * Copies all the tables, keys and relations from this schema into the
	 * target schema. Any relations which are
	 * {@link ComponentStatus#INFERRED_INCORRECT} and have keys with different
	 * numbers of columns at either end will probably not be copied.
	 * 
	 * @param targetSchema
	 *            the schema to copy into.
	 */
	public void replicateContents(Schema targetSchema);

	/**
	 * The generic implementation should suffice as the ground for most complex
	 * implementations. It keeps track of tables it has seen, and performs
	 * simple lookups for them.
	 */
	public class GenericSchema implements Schema {
		protected final Map tables = new TreeMap();

		private String name;

		private boolean keyguessing;

		/**
		 * The constructor creates a schema with the given name. Keyguessing is
		 * turned off.
		 * 
		 * @param name
		 *            the name for this new schema.
		 */
		public GenericSchema(String name) {
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
		public GenericSchema(String name, boolean keyguessing) {
			this.name = name;
			this.keyguessing = keyguessing;
		}

		public void replicateContents(Schema targetSchema) {
			// Remove all the tables from the target schema, in case
			// there are any already.
			targetSchema.getTables().clear();

			// Set up a set to contain all the relations to replicate.
			Set relations = new HashSet();

			// Iterate over all the tables, and copy each one.
			for (Iterator i = this.tables.values().iterator(); i.hasNext();) {
				Table table = (Table) i.next();

				// Create a copy of the table.
				Table newTable;
				try {
					newTable = new GenericTable(table.getName(), targetSchema);
				} catch (Throwable t) {
					throw new MartBuilderInternalError(t);
				}

				// Iterate over all the columns and copy them too.
				for (Iterator j = table.getColumns().iterator(); j.hasNext();) {
					Column col = (Column) j.next();
					try {
						new GenericColumn(col.getName(), newTable);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
				}

				// Copy the primary key, if it exists.
				PrimaryKey pk = table.getPrimaryKey();
				if (pk != null) {
					// Find the equivalents of all the columns by name.
					List columns = new ArrayList();
					for (Iterator k = pk.getColumnNames().iterator(); k
							.hasNext();) {
						columns
								.add(newTable
										.getColumnByName((String) k.next()));
					}

					// Create the key.
					try {
						PrimaryKey newPK = new GenericPrimaryKey(columns);
						newPK.setStatus(pk.getStatus());
						newTable.setPrimaryKey(newPK);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
				}

				// Copy the foreign keys.
				for (Iterator j = table.getForeignKeys().iterator(); j
						.hasNext();) {
					ForeignKey fk = (ForeignKey) j.next();

					// Find the equivalents of all the columns by name.
					List columns = new ArrayList();
					for (Iterator k = fk.getColumnNames().iterator(); k
							.hasNext();) {
						columns
								.add(newTable
										.getColumnByName((String) k.next()));
					}

					// Create the key.
					try {
						ForeignKey newFK = new GenericForeignKey(columns);
						newFK.setStatus(fk.getStatus());
						newTable.addForeignKey(newFK);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
				}

				// Remember the relations on this table for later.
				relations.addAll(table.getRelations());
			}

			// Iterate through all the relations we found and copy them too.
			for (Iterator i = relations.iterator(); i.hasNext();) {
				Relation r = (Relation) i.next();

				// Work out the primary, foreign key and cardinality of
				// the existing relation.
				PrimaryKey pk = r.getPrimaryKey();
				ForeignKey fk = r.getForeignKey();
				Cardinality card = r.getFKCardinality();

				// Find the equivalent PK in the duplicate table
				// by comparing table names.
				PrimaryKey newPK = targetSchema.getTableByName(
						pk.getTable().getName()).getPrimaryKey();

				// Find the equivalent FK in the duplicate table by
				// comparing table names and sets of column names.
				ForeignKey newFK = null;
				for (Iterator j = targetSchema.getTableByName(
						fk.getTable().getName()).getForeignKeys().iterator(); j
						.hasNext()
						&& newFK == null;) {
					ForeignKey candidate = (ForeignKey) j.next();
					if (candidate.getColumnNames().equals(fk.getColumnNames()))
						newFK = candidate;
				}

				// Create the relation in the duplicate schema.
				try {
					Relation newRel = new GenericRelation(newPK, newFK, card);
					newRel.setStatus(r.getStatus());
				} catch (Exception e) {
					// Ignore. This can only happen if incorrect relations are
					// copied across
					// which have different-arity keys at each end.
				}
			}
		}

		public Schema replicate(String newName) {
			return new GenericSchema(newName);
		}

		public void setKeyGuessing(boolean keyguessing) {
			this.keyguessing = keyguessing;
		}

		public boolean getKeyGuessing() {
			return this.keyguessing;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean canCohabit(DataLink partner) {
			return false;
		}

		public boolean test() throws Exception {
			return true;
		}

		public void synchronise() throws SQLException, BuilderException {
		}

		public void addTable(Table table) throws AlreadyExistsException,
				AssociationException {
			// Check the table belongs to us, and has a unique name.
			if (!table.getSchema().equals(this))
				throw new AssociationException(BuilderBundle
						.getString("tableSchemaMismatch"));
			if (this.tables.containsKey(table.getName()))
				throw new AlreadyExistsException(BuilderBundle
						.getString("tableExists"), table.getName());

			// Add the table.
			this.tables.put(table.getName(), table);
		}

		public Collection getTables() {
			return this.tables.values();
		}

		public Table getTableByName(String name) {
			return (Table) this.tables.get(name);
		}

		public void changeTableMapKey(String oldName, String newName)
				throws AlreadyExistsException {
			// If the names are the same, do nothing.
			if (oldName.equals(newName))
				return;

			// Refuse to do it if the new name has been used already.
			if (this.tables.containsKey(newName))
				throw new AlreadyExistsException(BuilderBundle
						.getString("tableExists"), newName);

			// Update our mapping but don't rename the columns themselves.
			Table tbl = (Table) this.tables.get(oldName);
			this.tables.put(newName, tbl);
			this.tables.remove(oldName);
		}

		public Collection getExternalKeys() {
			// Keys are external if they have a relation which
			// points, at the other end, to a key in some schema
			// other than ourselves.
			List keys = new ArrayList();
			Collection relations = this.getExternalRelations();
			for (Iterator i = relations.iterator(); i.hasNext();) {
				Relation relation = (Relation) i.next();
				if (relation.getPrimaryKey().getTable().getSchema()
						.equals(this))
					keys.add(relation.getPrimaryKey());
				else
					keys.add(relation.getForeignKey());
			}
			return keys;
		}

		public Collection getInternalRelations() {
			// Relations are internal if both ends point to keys
			// in this schema.
			List relations = new ArrayList();
			for (Iterator i = this.getTables().iterator(); i.hasNext();) {
				Table table = (Table) i.next();
				if (table.getPrimaryKey() == null)
					continue;
				for (Iterator j = table.getPrimaryKey().getRelations()
						.iterator(); j.hasNext();) {
					Relation relation = (Relation) j.next();
					if (relation.getForeignKey().getTable().getSchema().equals(
							this))
						relations.add(relation);
				}
			}
			return relations;
		}

		public Collection getExternalRelations() {
			// Relations are external if one end points to a key
			// in a schema other than ourselves.
			List relations = new ArrayList();
			for (Iterator i = this.getTables().iterator(); i.hasNext();) {
				Table table = (Table) i.next();
				for (Iterator j = table.getKeys().iterator(); j.hasNext();) {
					Key key = (Key) j.next();
					for (Iterator l = key.getRelations().iterator(); l
							.hasNext();) {
						Relation relation = (Relation) l.next();
						if (!(relation.getPrimaryKey().getTable().getSchema()
								.equals(this) && relation.getForeignKey()
								.getTable().getSchema().equals(this))) {
							relations.add(relation);
						}
					}
				}
			}
			return relations;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			Schema t = (Schema) o;
			return this.toString().compareTo(t.toString());
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof Schema))
				return false;
			Schema t = (Schema) o;
			return t.toString().equals(this.toString());
		}
	}
}
