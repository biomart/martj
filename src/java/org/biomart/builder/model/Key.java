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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.resources.Resources;

/**
 * <p>
 * The key interface is core to the way tables get associated. They are involved
 * in relations which link tables together in various ways, and provide
 * information about which columns at each end correspond.
 * <p>
 * The {@link GenericKey} implementation provides the basis for the other types
 * of keys, eg. keeping track of relations etc.
 * <p>
 * Unless otherwise specified, all keys are created with a default status of
 * {@link ComponentStatus#INFERRED}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author$
 * @since 0.1
 */
public interface Key extends Comparable {
	/**
	 * Adds a particular relation to the set this key is involved in. It checks
	 * first to make sure it is actually involved.
	 * 
	 * @param relation
	 *            the relation to add to this key.
	 * @throws AssociationException
	 *             if it is not actually involved in the given relation in any
	 *             way.
	 */
	public void addRelation(Relation relation) throws AssociationException;

	/**
	 * Counts the columns this key is formed over.
	 * 
	 * @return the number of columns this key involves.
	 */
	public int countColumns();

	/**
	 * Deletes this key, and also deletes all relations that use it.
	 */
	public void destroy();

	/**
	 * Returns a list of names of the columns in this key.
	 * 
	 * @return the names of the columns in this key.
	 */
	public Collection getColumnNames();

	/**
	 * Returns the list of columns this key is formed over. It may return an
	 * empty set.
	 * 
	 * @return the list of columns this key involves.
	 */
	public List getColumns();

	/**
	 * Returns the name of this key.
	 * 
	 * @return the name of this key.
	 */
	public String getName();

	/**
	 * Returns all relations this key is involved in. The set may be empty but
	 * it will never be null.
	 * 
	 * @return the set of all relations this key is involved in.
	 */
	public Collection getRelations();

	/**
	 * Returns the status of this key. The default value, unless otherwise
	 * specified, is {@link ComponentStatus#INFERRED}.
	 * 
	 * @return the status of this key.
	 */
	public ComponentStatus getStatus();

	/**
	 * Returns the table this key is formed over.
	 * 
	 * @return the table this key involves.
	 */
	public Table getTable();

	/**
	 * Removes a particular relation from the set this key is involved in.
	 * 
	 * @param relation
	 *            the relation to remove from this key.
	 */
	public void removeRelation(Relation relation);

	/**
	 * Replaces the set of columns this key is formed over with a new set.
	 * 
	 * @param columns
	 *            the replacement columns, in order.
	 * @throws AssociationException
	 *             if any of the columns are not from the same table as this key
	 *             is from.
	 */
	public void setColumns(List columns) throws AssociationException;

	/**
	 * Sets the status of this key.
	 * 
	 * @param status
	 *            the new status of this key.
	 */
	public void setStatus(ComponentStatus status);

	/**
	 * This interface is designed to mark key instances as foreign keys.
	 */
	public interface ForeignKey extends Key {
	}

	/**
	 * This implementation is a simple foreign key.
	 */
	public class GenericForeignKey extends GenericKey implements ForeignKey {
		/**
		 * The constructor passes on all its work to the {@link GenericKey}
		 * constructor. It then adds itself to the set of foreign keys on the
		 * parent table.
		 * 
		 * @param columns
		 *            the list of columns to form the key over.
		 * @throws AssociationException
		 *             if any of the columns do not belong to the table
		 *             specified.
		 */
		public GenericForeignKey(final List columns)
				throws AssociationException {
			super(columns);
		}

		public String getName() {
			final String supername = super.getName();
			return Resources.get("fkPrefix") + supername;
		}
	}

	/**
	 * The generic implementation provides the basics for more complex key types
	 * to extend. It doesn't know anything except which columns and relations
	 * are involved.
	 */
	public class GenericKey implements Key {
		private final List columns = new ArrayList();

		private final List relations = new ArrayList();

		private ComponentStatus status;

		private Table table;

		/**
		 * The constructor constructs a key over a single column. Otherwise, it
		 * is identical to the multi-column constructor.
		 * 
		 * @param column
		 *            the column to form the key over.
		 * @throws AssociationException
		 *             if the column cannot be combined into a single key. See
		 *             {@link Key#setColumns(List)}.
		 */
		public GenericKey(final Column column) throws AssociationException {
			this(Collections.singletonList(column));
		}

		/**
		 * The constructor constructs a key over a set of columns. It doesn't
		 * check to make sure they all come from the same table, nor does it
		 * check to see if they are in a sensible order. The order they are
		 * specified in here is the order in which the key will refer to them in
		 * future. The key will have a status of
		 * {@link ComponentStatus#INFERRED}.
		 * 
		 * @param columns
		 *            the set of columns to form the key over.
		 * @throws AssociationException
		 *             if the set is empty or contains columns which cannot be
		 *             combined into a single key. See
		 *             {@link Key#setColumns(List)}.
		 */
		public GenericKey(final List columns) throws AssociationException {
			this.status = ComponentStatus.INFERRED;
			this.setColumns(columns);
		}

		public void addRelation(final Relation relation)
				throws AssociationException {
			// Does it refer to us?
			try {
				// Will throw an exception if we are not part of the relation.
				relation.getOtherKey(this);
			} catch (final IllegalArgumentException e) {
				throw new AssociationException(Resources
						.get("relationNotOfThisKey"));
			}

			// Add it.
			this.relations.add(relation);
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Key k = (Key) o;
			return this.toString().compareTo(k.toString());
		}

		public int countColumns() {
			return this.columns.size();
		}

		public void destroy() {
			// Destroy all the relations. Work from a copy to prevent
			// concurrent modification exceptions.
			final List relationsCopy = new ArrayList(this.relations);
			for (final Iterator i = relationsCopy.iterator(); i.hasNext();) {
				final Relation r = (Relation) i.next();
				r.destroy();
			}

			// Remove references to us from tables.
			if (this instanceof PrimaryKey)
				try {
					this.getTable().setPrimaryKey(null);
				} catch (final Throwable t) {
					throw new MartBuilderInternalError(t);
				}
			else if (this instanceof ForeignKey)
				this.getTable().removeForeignKey((ForeignKey) this);
			else
				throw new MartBuilderInternalError();

		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Key))
				return false;
			final Key k = (Key) o;
			return k.toString().equals(this.toString());
		}

		public Collection getColumnNames() {
			final List names = new ArrayList();
			for (final Iterator i = this.columns.iterator(); i.hasNext();) {
				final Column c = (Column) i.next();
				names.add(c.getName());
			}
			return names;
		}

		public List getColumns() {
			return this.columns;
		}

		public String getName() {
			final StringBuffer sb = new StringBuffer();
			sb.append(this.getTable().toString());
			sb.append(this.getColumnNames().toString());
			return sb.toString();
		}

		public Collection getRelations() {
			return this.relations;
		}

		public ComponentStatus getStatus() {
			return this.status;
		}

		public Table getTable() {
			return this.table;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public void removeRelation(final Relation relation) {
			this.relations.remove(relation);
		}

		public void setColumns(final List columns) throws AssociationException {
			// Make sure we have at least one column.
			if (columns.size() < 1)
				throw new IllegalArgumentException(Resources
						.get("columnsIsEmpty"));

			// Remove all existing columns.
			this.columns.clear();

			// Iterate over the new set of columns.
			for (final Iterator i = columns.iterator(); i.hasNext();) {
				final Column column = (Column) i.next();

				// Make our table the table of the first column we find.
				if (this.table == null)
					this.table = column.getTable();

				// If the column doesn't match our table, or we already have
				// the same column, throw a wobbly.
				if (!column.getTable().equals(this.table))
					throw new AssociationException(Resources
							.get("multiTableColumns"));
				if (this.columns.contains(column))
					throw new AssociationException(Resources
							.get("duplicateColumnsInKey"));

				// Add the column.
				this.columns.add(column);
			}

			// Invalidate all relations associated with this key. This means
			// dropping all handmade relations, and marking all others as
			// incorrect.
			final List deadRels = new ArrayList();
			for (final Iterator i = this.relations.iterator(); i.hasNext();) {
				final Relation r = (Relation) i.next();
				if (r.getStatus().equals(ComponentStatus.HANDMADE))
					deadRels.add(r);
				else
					r.setStatus(ComponentStatus.INFERRED_INCORRECT);
			}

			// Drop the relations we identified as useless. We have to do this
			// separately to prevent concurrent modification exceptions.
			for (final Iterator i = deadRels.iterator(); i.hasNext();)
				((Relation) i.next()).destroy();
		}

		public void setStatus(final ComponentStatus status) {
			this.status = status;

			// If we are invalidating the key, then we must also
			// invalidate all relations associated with this key. This means
			// dropping all handmade relations, and marking all others as
			// incorrect.
			if (this.status.equals(ComponentStatus.INFERRED_INCORRECT)) {
				final List deadRels = new ArrayList();
				for (final Iterator i = this.relations.iterator(); i.hasNext();) {
					final Relation r = (Relation) i.next();
					if (r.getStatus().equals(ComponentStatus.HANDMADE))
						deadRels.add(r);
					else
						try {
							r.setStatus(ComponentStatus.INFERRED_INCORRECT);
						} catch (final Throwable t) {
							throw new MartBuilderInternalError(t);
						}
				}

				// Drop the relations we identified as useless. We have to do
				// this
				// separately to prevent concurrent modification exceptions.
				for (final Iterator i = deadRels.iterator(); i.hasNext();)
					((Relation) i.next()).destroy();
			}
		}

		public String toString() {
			return this.getName();
		}
	}

	/**
	 * This implementation is a simple primary key.
	 */
	public class GenericPrimaryKey extends GenericKey implements PrimaryKey {
		/**
		 * The constructor passes on all its work to the {@link GenericKey}
		 * constructor.
		 * 
		 * @param columns
		 *            the list of columns to form the key over.
		 * @throws AssociationException
		 *             if any of the columns do not belong to the table
		 *             specified.
		 */
		public GenericPrimaryKey(final List columns)
				throws AssociationException {
			super(columns);
		}

		public String getName() {
			final String supername = super.getName();
			return Resources.get("pkPrefix") + supername;
		}
	}

	/**
	 * This interface is designed to mark key instances as primary keys.
	 */
	public interface PrimaryKey extends Key {
	}
}
