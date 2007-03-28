/*
 * Table.java
 * Created on 23 March 2006, 14:34
 */

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * The table interface provides the basic idea of what constitutes a database
 * table or an XML document entity. It has an optional primary key, zero or more
 * foreign keys, and one or more columns.
 * <p>
 * The {@link GenericTable} class is provided as a template from which to build
 * up more complex implementations. It is able to keep track of keys and columns
 * but it does not provide any methods that process or analyse these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.5
 */
public interface Table extends Comparable {

	/**
	 * Adds a relation to the set known by this table.
	 * 
	 * @param relation
	 *            the relation to add.
	 */
	public void addRelation(final Relation relation);

	/**
	 * Removes a relation from the set known by this table.
	 * 
	 * @param relation
	 *            the relation to remove.
	 */
	public void removeRelation(final Relation relation);

	/**
	 * Attempts to add a column to this table.
	 * 
	 * @param column
	 *            the column to add.
	 */
	public void addColumn(Column column);

	/**
	 * Adds a foreign key to this table. The foreign key must refer to this
	 * table else an exception will be thrown.
	 * 
	 * @param foreignKey
	 *            the new foreign key to add to this table.
	 * @throws AssociationException
	 *             if the key already exists.
	 */
	public void addForeignKey(ForeignKey foreignKey)
			throws AssociationException;

	/**
	 * Attempts to rename a column. The rename does not affect the column
	 * itself, only the representation of the column within this table. If the
	 * names are the same, nothing happens.
	 * 
	 * @param oldName
	 *            the old name of the column.
	 * @param newName
	 *            the new name of the column.
	 */
	public void changeColumnMapKey(String oldName, String newName);

	/**
	 * Drops all columns on a table so that it can safely be dropped itself.
	 */
	public void destroy();

	/**
	 * Attempts to locate a column in this table by name. If it finds it, it
	 * returns it. If it doesn't, it returns <tt>null</tt>.
	 * 
	 * @param name
	 *            the name of the column to look up.
	 * @return the corresponding column, or <tt>null</tt> if it couldn't be
	 *         found.
	 */
	public Column getColumnByName(String name);

	/**
	 * Returns a set of the columns of this table. It may be empty, indicating
	 * that the table has no columns, however this is highly unlikely! It will
	 * never return <tt>null</tt>.
	 * 
	 * @return the set of columns for this table.
	 */
	public Collection getColumns();

	/**
	 * Returns a set of the foreign keys of this table. It may be empty,
	 * indicating that the table has no foreign keys. It will never return
	 * <tt>null</tt>.
	 * 
	 * @return the set of foreign keys for this table.
	 */
	public Collection getForeignKeys();

	/**
	 * Returns a set of the relations on all keys in this table that refer to
	 * other keys in the same schema as this table. It may be empty, indicating
	 * that the table has no internal relations. It will never return
	 * <tt>null</tt>.
	 * 
	 * @return the set of internal relations for this table.
	 */
	public Collection getInternalRelations();

	/**
	 * Returns a set of the relations on all keys in this table that refer to
	 * other keys in tables in other schemas. It may be empty, indicating that
	 * the table has no external relations. It will never return <tt>null</tt>.
	 * 
	 * @return the set of external relations for this table.
	 */
	public Collection getExternalRelations();

	/**
	 * Returns a set of the keys on all columns in this table. It may be empty,
	 * indicating that the table has no keys. It will never return <tt>null</tt>.
	 * If the table has a primary key, it will always appear first in the
	 * iterator obtained from the returned collection.
	 * 
	 * @return the set of keys for this table.
	 */
	public Collection getKeys();

	/**
	 * Returns the name of this table.
	 * 
	 * @return the name of this table.
	 */
	public String getName();

	/**
	 * Returns a reference to the primary key of this table. It may be
	 * <tt>null</tt>, indicating that the table has no primary key.
	 * 
	 * @return the primary key of this table.
	 */
	public PrimaryKey getPrimaryKey();

	/**
	 * Returns a set of the relations on all keys in this table. It may be
	 * empty, indicating that the table has no relations. It will never return
	 * <tt>null</tt>.
	 * 
	 * @return the set of relations for this table.
	 */
	public Collection getRelations();

	/**
	 * Returns the schema for this table.
	 * 
	 * @return the schema for this table.
	 */
	public Schema getSchema();

	/**
	 * Attempts to remove a column from this table. If the column does not exist
	 * on this table the operation will be quietly ignored. Any key involving
	 * that column will also be dropped along with all associated relations.
	 * 
	 * @param column
	 *            the column to remove.
	 */
	public void removeColumn(Column column);

	/**
	 * Removes a foreign key from this table.
	 * 
	 * @param foreignKey
	 *            the new foreign key to remove from this table.
	 */
	public void removeForeignKey(ForeignKey foreignKey);

	/**
	 * Sets the primary key of this table. It may be <tt>null</tt>,
	 * indicating that the table has no primary key.
	 * 
	 * @param primaryKey
	 *            the new primary key of this table.
	 */
	public void setPrimaryKey(PrimaryKey primaryKey);

	/**
	 * The generic implementation of table provides basic methods for working
	 * with database or XML document tables, including the ability to add a new
	 * column and check for conflicts with existing columns.
	 */
	public class GenericTable implements Table {
		private static final long serialVersionUID = 1L;

		private final Map columns = new HashMap();

		// We must use a list as key hash codes can change.
		private final List foreignKeys = new ArrayList();

		private String name;

		private PrimaryKey primaryKey;

		private final Schema schema;

		private final Set relations = new HashSet();

		private final Set internalRelations = new HashSet();

		private final Set externalRelations = new HashSet();

		/**
		 * The constructor sets up an empty table representation with the given
		 * name that lives within the given schema.
		 * 
		 * @param name
		 *            the table name.
		 * @param schema
		 *            the schema this table is associated with.
		 */
		public GenericTable(String name, final Schema schema) {
			Log.debug("Creating table " + name + " in " + schema);
			// Remember the values.
			this.schema = schema;
			// Make the name unique.
			final String baseName = name;
			for (int i = 1; schema.getTableByName(name) != null; name = baseName
					+ "_" + i++)
				;
			Log.debug("Unique name is " + name);
			this.name = name;
		}

		public void addRelation(final Relation relation) {
			this.relations.add(relation);
			if (relation.isExternal())
				this.externalRelations.add(relation);
			else
				this.internalRelations.add(relation);
			this.schema.addRelation(relation);
		}

		public void removeRelation(final Relation relation) {
			this.relations.remove(relation);
			if (relation.isExternal())
				this.externalRelations.remove(relation);
			else
				this.internalRelations.remove(relation);
			this.schema.removeRelation(relation);
		}

		public void addColumn(final Column column) {
			Log.debug("Adding column " + column + " to " + this.getName());
			// Add it.
			this.columns.put(column.getName(), column);
		}

		public void addForeignKey(final ForeignKey foreignKey)
				throws AssociationException {
			Log.debug("Adding foreign key " + foreignKey + " to "
					+ this.getName());
			// Check that the key lives in this table first.
			if (this.foreignKeys.contains(foreignKey))
				throw new AssociationException(Resources.get("fkAlreadyExists"));

			// Add the key.
			this.foreignKeys.add(foreignKey);
		}

		public void changeColumnMapKey(final String oldName,
				final String newName) {
			Log.debug("Remapping column " + oldName + " as " + newName
					+ " in table " + this.getName());
			// If the names are the same, do nothing.
			if (oldName.equals(newName))
				return;
			// Update our mapping but don't rename the columns themselves.
			final Column col = (Column) this.columns.get(oldName);
			this.columns.put(newName, col);
			this.columns.remove(oldName);
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Table t = (Table) o;
			return this.toString().compareTo(t.toString());
		}

		public void destroy() {
			Log.debug("Dropping table " + this.getName());
			for (final Iterator i = this.relations.iterator(); i.hasNext();)
				this.schema.removeRelation((Relation) i.next());
			// Remove each column we have. This will recursively cause
			// keys etc. to be removed.
			// Must use a copy else we'll get concurrent modification problems.
			final Set allCols = new HashSet(this.columns.values());
			for (final Iterator i = allCols.iterator(); i.hasNext();) {
				final Column c = (Column) i.next();
				this.removeColumn(c);
			}
		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Table))
				return false;
			final Table t = (Table) o;
			return t.toString().equals(this.toString());
		}

		public Column getColumnByName(final String name) {
			return (Column) this.columns.get(name);
		}

		public Collection getColumns() {
			return this.columns.values();
		}

		public Collection getForeignKeys() {
			return this.foreignKeys;
		}

		public Collection getExternalRelations() {
			return this.externalRelations;
		}

		public Collection getInternalRelations() {
			return this.internalRelations;
		}

		public Collection getKeys() {
			final Collection allKeys = new LinkedHashSet();
			if (this.primaryKey != null)
				allKeys.add(this.primaryKey);
			allKeys.addAll(this.foreignKeys);
			return allKeys;
		}

		public String getName() {
			return this.name;
		}

		public PrimaryKey getPrimaryKey() {
			return this.primaryKey;
		}

		public Collection getRelations() {
			return this.relations;
		}

		public Schema getSchema() {
			return this.schema;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public void removeColumn(final Column column) {
			Log.debug("Removing column " + column + " from " + this.getName());
			// Remove all keys involving this column
			for (final Iterator i = this.getKeys().iterator(); i.hasNext();) {
				final Key k = (Key) i.next();
				if (k.getColumns().contains(column)) {
					k.destroy();
					i.remove(); // to make sure
				}
			}

			// Remove the column itself
			this.columns.remove(column.getName());
		}

		public void removeForeignKey(final ForeignKey foreignKey) {
			Log.debug("Removing foreign key " + foreignKey + " from "
					+ this.getName());
			this.foreignKeys.remove(foreignKey);
		}

		public void setPrimaryKey(final PrimaryKey primaryKey) {
			Log.debug("Setting primary key on " + this.getName() + " to "
					+ primaryKey);
			// If the key is the same, do nothing.
			if (primaryKey != null && this.primaryKey != null
					&& primaryKey.equals(this.primaryKey))
				return;

			// Ensure nobody points to the old primary key
			if (this.primaryKey != null) {
				// Destroy relations on the old primary key.
				// Must use a copy else get concurrent-modification problems.
				final Collection relations = new HashSet(this.primaryKey
						.getRelations());
				for (final Iterator i = relations.iterator(); i.hasNext();) {
					final Relation r = (Relation) i.next();
					r.destroy();
				}
			}

			// Update our primary key to the new one.
			this.primaryKey = primaryKey;
		}

		public String toString() {
			return this.getName() + " (" + this.schema + ")";
		}
	}
}
