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

package org.biomart.builder.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.resources.Resources;

/**
 * <p>
 * The table interface provides the basic idea of what constitutes a database
 * table or an XML document entity. It has an optional primary key, zero or more
 * foreign keys, and one or more columns.
 * <p>
 * The {@link GenericTable} class is provided as a template from which to build
 * up more complex implementations. It is able to keep track of keys and columns
 * but it does not provide any methods that process or analyse these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.15, 4th August 2006
 * @since 0.1
 */
public interface Table extends Comparable {
	/**
	 * Returns the name of this table.
	 * 
	 * @return the name of this table.
	 */
	public String getName();

	/**
	 * Returns the original name of this table. This is useful if the user
	 * renames the table but you need to refer to it by the name it started life
	 * out as.
	 * 
	 * @return the original name of this table.
	 */
	public String getOriginalName();

	/**
	 * Sets a new name for this table. This will also rename the table in the
	 * schema map.
	 * 
	 * @param newName
	 *            the new name.
	 */
	public void setName(String newName);

	/**
	 * Use this to rename a table's original name. Use with extreme caution. The
	 * owning schema doesn't need to know so won't be notified.
	 * 
	 * @param newName
	 *            the new original name to give the table.
	 */
	public void setOriginalName(String newName);

	/**
	 * Returns the schema for this table.
	 * 
	 * @return the schema for this table.
	 */
	public Schema getSchema();

	/**
	 * Returns a reference to the primary key of this table. It may be null,
	 * indicating that the table has no primary key.
	 * 
	 * @return the primary key of this table.
	 */
	public PrimaryKey getPrimaryKey();

	/**
	 * Sets the primary key of this table. It may be null, indicating that the
	 * table has no primary key.
	 * 
	 * @param primaryKey
	 *            the new primary key of this table.
	 * @throws AssociationException
	 *             if the table parameter of the foreign key does not match this
	 *             table.
	 */
	public void setPrimaryKey(PrimaryKey primaryKey)
			throws AssociationException;

	/**
	 * Returns a set of the foreign keys of this table. It may be empty,
	 * indicating that the table has no foreign keys. It will never return null.
	 * 
	 * @return the set of foreign keys for this table.
	 */
	public Collection getForeignKeys();

	/**
	 * Adds a foreign key to this table. It may not be null. The foreign key
	 * must refer to this table else an exception will be thrown.
	 * 
	 * @param foreignKey
	 *            the new foreign key to add to this table.
	 * @throws AssociationException
	 *             if the table parameter of the foreign key does not match this
	 *             table.
	 */
	public void addForeignKey(ForeignKey foreignKey)
			throws AssociationException;

	/**
	 * Removes a foreign key from this table.
	 * 
	 * @param foreignKey
	 *            the new foreign key to remove from this table.
	 */
	public void removeForeignKey(ForeignKey foreignKey);

	/**
	 * Returns a set of the keys on all columns in this table. It may be empty,
	 * indicating that the table has no keys. It will never return null.
	 * 
	 * @return the set of keys for this table.
	 */
	public Collection getKeys();

	/**
	 * Returns a set of the relations on all keys in this table. It may be
	 * empty, indicating that the table has no relations. It will never return
	 * null.
	 * 
	 * @return the set of relations for this table.
	 */
	public Collection getRelations();

	/**
	 * Returns a set of the relations on all keys in this table that refer to
	 * other keys in the same schema as this table. It may be empty, indicating
	 * that the table has no internal relations. It will never return null.
	 * 
	 * @return the set of internal relations for this table.
	 */
	public Collection getInternalRelations();

	/**
	 * Returns a set of the columns of this table. It may be empty, indicating
	 * that the table has no columns, however this is highly unlikely! It will
	 * never return null.
	 * 
	 * @return the set of columns for this table.
	 */
	public Collection getColumns();

	/**
	 * Attempts to locate a column in this table by name. If it finds it, it
	 * returns it. If it doesn't, it returns null.
	 * 
	 * @param name
	 *            the name of the column to look up.
	 * @return the corresponding column, or null if it couldn't be found.
	 */
	public Column getColumnByName(String name);

	/**
	 * Attempts to add a column to this table. The column will already have had
	 * it's table parameter set to match, otherwise an exception will be thrown.
	 * An exception will also get thrown if the column has the same name as an
	 * existing one on this table.
	 * 
	 * @param column
	 *            the column to add.
	 * @throws AssociationException
	 *             if the table parameter of the column does not match this
	 *             table.
	 */
	public void addColumn(Column column) throws AssociationException;

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
	 * Attempts to rename a column. If the new name has already been taken by
	 * another column, an exception is thrown. The rename does not affect the
	 * column itself, only the representation of the column within this table.
	 * If the names are the same, nothing happens.
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
	 * The generic implementation of table provides basic methods for working
	 * with database or XML document tables, including the ability to add a new
	 * column and check for conflicts with existing columns.
	 */
	public class GenericTable implements Table {
		private final Schema schema;

		private PrimaryKey primaryKey;

		private final List foreignKeys = new ArrayList();

		private final Map columns = new TreeMap();

		private String originalName;

		private String name;

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
			// Remember the values.
			this.schema = schema;
			// Make the name unique.
			final String baseName = name;
			for (int i = 1; schema.getTableByName(name) != null; name = baseName
					+ "_" + i++)
				;
			this.name = name;
			this.originalName = name;
			// Add it to the schema.
			try {
				schema.addTable(this);
			} catch (final AssociationException e) {
				// Should never happen, as it is only thrown if schema!=schema.
				throw new MartBuilderInternalError(e);
			}
		}

		public String getName() {
			return this.name;
		}

		public String getOriginalName() {
			return this.originalName;
		}

		public void setOriginalName(final String newName) {
			this.originalName = newName;
		}

		public void setName(String newName) {
			// Sanity check.
			if (newName.equals(this.name))
				return; // Skip unnecessary change.
			// Make the name unique.
			final String baseName = newName;
			for (int i = 1; this.schema.getTableByName(newName) != null; newName = baseName
					+ "_" + i++)
				;
			// Do it.
			this.getSchema().changeTableMapKey(this.name, newName);
			this.name = newName;
		}

		public Schema getSchema() {
			return this.schema;
		}

		public PrimaryKey getPrimaryKey() {
			return this.primaryKey;
		}

		public void setPrimaryKey(final PrimaryKey primaryKey)
				throws AssociationException {
			// Check the key lives in this table first.
			if (primaryKey != null && !primaryKey.getTable().equals(this))
				throw new AssociationException(Resources.get("pkTableMismatch"));

			// If the key is the same, do nothing.
			if (primaryKey != null && this.primaryKey != null
					&& primaryKey.equals(this.primaryKey))
				return;

			// Ensure nobody points to the old primary key
			if (this.primaryKey != null) {
				// Destroy relations on the old primary key.
				// Must use a copy else get concurrent-modification problems.
				final List relations = new ArrayList(this.primaryKey
						.getRelations());
				for (final Iterator i = relations.iterator(); i.hasNext();) {
					final Relation r = (Relation) i.next();
					r.destroy();
				}
			}

			// Update our primary key to the new one.
			this.primaryKey = primaryKey;
		}

		public Collection getForeignKeys() {
			return this.foreignKeys;
		}

		public void addForeignKey(final ForeignKey foreignKey)
				throws AssociationException {
			// Check that the key lives in this table first.
			if (this.foreignKeys.contains(foreignKey))
				throw new AssociationException(Resources.get("fkAlreadyExists"));

			// Add the key.
			this.foreignKeys.add(foreignKey);
		}

		public void removeForeignKey(final ForeignKey foreignKey) {
			this.foreignKeys.remove(foreignKey);
		}

		public Collection getKeys() {
			final List allKeys = new ArrayList(this.foreignKeys);
			if (this.primaryKey != null)
				allKeys.add(this.primaryKey);
			return allKeys;
		}

		public Collection getRelations() {
			final Set allRels = new HashSet(); // enforce uniqueness
			for (final Iterator i = this.getKeys().iterator(); i.hasNext();) {
				final Key k = (Key) i.next();
				allRels.addAll(k.getRelations());
			}
			return allRels;
		}

		public Collection getInternalRelations() {
			final Set relations = new HashSet(); // enforce uniqueness

			// Try the primary key relations first.
			if (this.getPrimaryKey() != null)
				for (final Iterator j = this.getPrimaryKey().getRelations()
						.iterator(); j.hasNext();) {
					// Add all where the FK is the same schema as us.
					final Relation relation = (Relation) j.next();
					if (relation.getOtherKey(this.getPrimaryKey()).getTable()
							.getSchema().equals(this.getSchema()))
						relations.add(relation);
				}

			// Now do the FK relations.
			for (final Iterator i = this.getForeignKeys().iterator(); i
					.hasNext();) {
				final Key fk = (Key) i.next();
				for (final Iterator j = fk.getRelations().iterator(); j
						.hasNext();) {
					// Add all where the PK is the same schema as us.
					final Relation relation = (Relation) j.next();
					if (relation.getOtherKey(fk).getTable().getSchema().equals(
							this.getSchema()))
						relations.add(relation);
				}
			}

			// Return the complete set.
			return relations;
		}

		public Collection getColumns() {
			return this.columns.values();
		}

		public Column getColumnByName(final String name) {
			return (Column) this.columns.get(name);
		}

		public void addColumn(final Column column) throws AssociationException {
			// Refuse to do it if the column belongs to some other table.
			if (column.getTable() != this)
				throw new AssociationException(Resources
						.get("columnTableMismatch"));
			// Add it.
			this.columns.put(column.getName(), column);
		}

		public void removeColumn(final Column column) {
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

		public void changeColumnMapKey(final String oldName,
				final String newName) {
			// If the names are the same, do nothing.
			if (oldName.equals(newName))
				return;
			// Update our mapping but don't rename the columns themselves.
			final Column col = (Column) this.columns.get(oldName);
			this.columns.put(newName, col);
			this.columns.remove(oldName);
		}

		public void destroy() {
			// Remove each column we have. This will recursively cause
			// keys etc. to be removed.
			// Must use a copy else we'll get concurrent modification problems.
			final Set allCols = new HashSet(this.columns.values());
			for (final Iterator i = allCols.iterator(); i.hasNext();) {
				final Column c = (Column) i.next();
				this.removeColumn(c);
			}
		}

		public String toString() {
			return this.schema.toString() + ":" + this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Table t = (Table) o;
			return this.toString().compareTo(t.toString());
		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Table))
				return false;
			final Table t = (Table) o;
			return t.toString().equals(this.toString());
		}
	}
}
