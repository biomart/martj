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

import java.util.HashMap;
import java.util.Map;

import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * A relation represents the association between two keys. Relations between two
 * primary keys are always 1:1. Relations between two foreign keys are either
 * 1:1 or M:M. Relations between a foreign key and a primary key can either be
 * 1:1 or 1:M.
 * <p>
 * Both keys must have the same number of columns, and the related columns
 * should appear in the same order in both keys. If they do not, then results
 * may be unpredictable.
 * <p>
 * A {@link GenericRelation} class is provided to form the basic functionality
 * outlined above.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public interface Relation extends Comparable {
	/**
	 * Deconstructs the relation by removing references to itself from the keys
	 * at both ends.
	 */
	public void destroy();

	/**
	 * Returns the cardinality of the foreign key end of this relation, in a 1:M
	 * relation. In 1:1 relations this will always return 1, and in M:M
	 * relations it will always return M.
	 * 
	 * @return the cardinality of the foreign key end of this relation, in 1:M
	 *         relations only. Otherwise determined by the relation type.
	 */
	public Cardinality getCardinality();

	/**
	 * Returns the first key of this relation. The concept of which key is first
	 * and which is second depends merely on the order they were passed to the
	 * constructor.
	 * 
	 * @return the first key.
	 */
	public Key getFirstKey();

	/**
	 * In a 1:M relation, this will return the M end of the relation. In all
	 * other relation types, this will return <tt>null</tt>.
	 * 
	 * @return the key at the many end of the relation, or <tt>null</tt> if
	 *         this is not a 1:M relation.
	 */
	public Key getManyKey();

	/**
	 * Returns the name of this relation.
	 * 
	 * @return the name of this relation.
	 */
	public String getName();

	/**
	 * In a 1:M relation, this will return the 1 end of the relation. In all
	 * other relation types, this will return <tt>null</tt>.
	 * 
	 * @return the key at the one end of the relation, or <tt>null</tt> if
	 *         this is not a 1:M relation.
	 */
	public Key getOneKey();

	/**
	 * Given a key that is in this relationship, return the other key.
	 * 
	 * @param key
	 *            the key we know is in this relationship.
	 * @return the other key in this relationship, or <tt>null</tt> if the key
	 *         specified is not in this relationship.
	 */
	public Key getOtherKey(Key key);

	/**
	 * Returns the second key of this relation. The concept of which key is
	 * first and which is second depends merely on the order they were passed to
	 * the constructor.
	 * 
	 * @return the second key.
	 */
	public Key getSecondKey();

	/**
	 * Returns the status of this relation. The default value, unless otherwise
	 * specified, is {@link ComponentStatus#INFERRED}.
	 * 
	 * @return the status of this relation.
	 */
	public ComponentStatus getStatus();

	/**
	 * Returns <tt>true</tt> if this relation involves keys in two separate
	 * schemas. Those that do are external, those that don't are not.
	 * 
	 * @return <tt>true</tt> if this is external, <tt>false</tt> otherwise.
	 */
	public boolean isExternal();

	/**
	 * Returns <tt>true</tt> if this is a M:M relation.
	 * 
	 * @return <tt>true</tt> if this is a M:M relation, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isManyToMany();

	/**
	 * Can this relation be M:M? Returns <tt>true</tt> where both keys are
	 * foreign keys.
	 * 
	 * @return <tt>true</tt> if this can be M:M, <tt>false</tt> if not.
	 */
	public boolean isManyToManyAllowed();

	/**
	 * Returns <tt>true</tt> if this is a 1:M relation.
	 * 
	 * @return <tt>true</tt> if this is a 1:M relation, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isOneToMany();

	/**
	 * Can this relation be 1:M? Returns <tt>true</tt> in all cases where the
	 * two keys are of different types.
	 * 
	 * @return <tt>true</tt> if this can be 1:M, <tt>false</tt> if not.
	 */
	public boolean isOneToManyAllowed();

	/**
	 * Returns <tt>true</tt> if this is a 1:1 relation.
	 * 
	 * @return <tt>true</tt> if this is a 1:1 relation, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isOneToOne();

	/**
	 * Returns the key in this relation associated with the given table. If both
	 * keys are on that table, returns the one that is a PK, or the first one if
	 * both are FKs.
	 * 
	 * @param table
	 *            the table to get the key for.
	 * @return the key for that table. <tt>null</tt> if neither key is from
	 *         that table.
	 */
	public Key getKeyForTable(final Table table);

	/**
	 * Returns the key in this relation associated with the given schema. If
	 * both keys are on tables in that schema, returns the first one.
	 * 
	 * @param schema
	 *            the schema to get the key for.
	 * @return the key for that schema. <tt>null</tt> if neither key is from
	 *         that schema.
	 */
	public Key getKeyForSchema(final Schema schema);

	/**
	 * Sets the cardinality of the foreign key end of this relation, in a 1:M
	 * relation. If used on a 1:1 or M:M relation, then specifying M makes it
	 * M:M and specifying 1 makes it 1:1.
	 * 
	 * @param cardinality
	 *            the cardinality.
	 */
	public void setCardinality(Cardinality cardinality);

	/**
	 * Sets the status of this relation.
	 * 
	 * @param status
	 *            the new status of this relation.
	 * @throws AssociationException
	 *             if the keys at either end of the relation are incompatible
	 *             upon attempting to mark an
	 *             {@link ComponentStatus#INFERRED_INCORRECT} relation as
	 *             anything else.
	 */
	public void setStatus(ComponentStatus status) throws AssociationException;

	/**
	 * This internal singleton class represents the cardinality of a key
	 * involved in a relation. Note that the names of cardinality objects are
	 * case-sensitive.
	 */
	public class Cardinality implements Comparable {
		private static final long serialVersionUID = 1L;

		private static final Map singletons = new HashMap();

		/**
		 * Use this constant to refer to a key with many values.
		 */
		public static final Cardinality MANY = Cardinality.get("M");

		/**
		 * Use this constant to refer to a key with one value.
		 */
		public static final Cardinality ONE = Cardinality.get("1");

		/**
		 * The static factory method creates and returns a cardinality with the
		 * given name. It ensures the object returned is a singleton. Note that
		 * the names of cardinality objects are case-sensitive.
		 * 
		 * @param name
		 *            the name of the cardinality object.
		 * @return the cardinality object.
		 */
		public static Cardinality get(final String name) {
			// Do we already have this one?
			// If so, then return it.
			if (Cardinality.singletons.containsKey(name))
				return (Cardinality) Cardinality.singletons.get(name);

			// Otherwise, create it, remember it.
			final Cardinality c = new Cardinality(name);
			Cardinality.singletons.put(name, c);

			// Return it.
			return c;
		}

		private final String name;

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this cardinality object will display when printed.
		 * 
		 * @param name
		 *            the name of the cardinality.
		 */
		private Cardinality(final String name) {
			this.name = name;
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Cardinality c = (Cardinality) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}

		/**
		 * Displays the name of this cardinality object.
		 * 
		 * @return the name of this cardinality object.
		 */
		public String getName() {
			return this.name;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Always returns the name of this cardinality.
		 */
		public String toString() {
			return "" + this.getName();
		}
	}

	/**
	 * This class provides basic functionality, for instance checking that the
	 * keys at both ends have the same number of columns.
	 */
	public class GenericRelation implements Relation {
		private static final long serialVersionUID = 1L;

		private Cardinality cardinality;

		private final Key firstKey;

		private final Key secondKey;

		private Key oneKey;

		private Key manyKey;

		private boolean oneToManyAllowed;

		private boolean manyToManyAllowed;

		private boolean oneToOne;

		private boolean oneToMany;

		private boolean manyToMany;

		private boolean external;

		private ComponentStatus status;

		/**
		 * This constructor tests that both ends of the relation have keys with
		 * the same number of columns. The default constructor sets the status
		 * to {@link ComponentStatus#INFERRED}.
		 * 
		 * @param firstKey
		 *            the first key.
		 * @param secondKey
		 *            the second key.
		 * @param cardinality
		 *            the cardinality of the foreign key end of this relation.
		 *            If both keys are primary keys, then this is ignored and
		 *            defaults to 1 (meaning 1:1). If they are a mixture, then
		 *            this differentiates between 1:1 and 1:M. If they are both
		 *            foreign keys, then this differentiates between 1:1 and
		 *            M:M. See {@link #setCardinality(Cardinality)}.
		 * @throws AssociationException
		 *             if the number of columns in the keys don't match, or if
		 *             the relation already exists.
		 */
		public GenericRelation(final Key firstKey, final Key secondKey,
				final Cardinality cardinality) throws AssociationException {
			Log.debug("Creating relation between " + firstKey + " and "
					+ secondKey + " with cardinality " + cardinality);
			// Remember the keys etc.
			this.firstKey = firstKey;
			this.secondKey = secondKey;
			this.setCardinality(cardinality);
			this.status = ComponentStatus.INFERRED;
			
			// Check the keys have the same number of columns.
			if (firstKey.countColumns() != secondKey.countColumns())
				throw new AssociationException(Resources
						.get("keyColumnCountMismatch"));
			// Check the relation doesn't already exist.
			if (firstKey.getRelations().contains(this))
				throw new AssociationException(Resources
						.get("relationAlreadyExists"));
			// Cannot place a relation on an FK to this table if it
			// already has relations.
			if (firstKey.getTable().equals(secondKey.getTable())
					&& ((firstKey instanceof ForeignKey && firstKey
							.getRelations().size() > 0) || (secondKey instanceof ForeignKey && secondKey
							.getRelations().size() > 0)))
				throw new AssociationException(Resources
						.get("fkToThisOnceOrOthers"));
			// Cannot place a relation on an FK to another table if
			// it already has a relation to this table (it will have
			// only one due to previous check).
			if ((!firstKey.getTable().equals(secondKey.getTable()))
					&& (firstKey.getRelations().size() == 1 && ((Relation) firstKey
							.getRelations().iterator().next()).getOtherKey(
							firstKey).getTable().equals(firstKey.getTable())))
				throw new AssociationException(Resources
						.get("fkToThisOnceOrOthers"));

			// Update flags.
			this.oneToManyAllowed = !this.firstKey.getClass().equals(
					this.secondKey.getClass());
			this.manyToManyAllowed = this.firstKey instanceof ForeignKey
					&& this.secondKey instanceof ForeignKey;
			this.external = !this.firstKey.getTable().getSchema().equals(
					this.secondKey.getTable().getSchema());
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Relation r = (Relation) o;
			return this.toString().compareTo(r.toString());
		}

		public void destroy() {
			Log.debug("Dropping relation " + this.getName());
			this.firstKey.removeRelation(this);
			this.secondKey.removeRelation(this);
		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Relation))
				return false;
			final Relation r = (Relation) o;
			// Check that not same keys are involved.
			return (r.getFirstKey().equals(this.secondKey) && r.getSecondKey()
					.equals(this.firstKey))
					|| (r.getFirstKey().equals(this.firstKey) && r
							.getSecondKey().equals(this.secondKey));
		}

		public Cardinality getCardinality() {
			return this.cardinality;
		}

		public Key getFirstKey() {
			return this.firstKey;
		}

		public Key getManyKey() {
			return this.manyKey;
		}

		public String getName() {
			final StringBuffer sb = new StringBuffer();
			sb.append(this.getFirstKey() == null ? "<undef>" : this
					.getFirstKey().toString());
			sb.append(" -> ");
			sb.append(this.getSecondKey() == null ? "<undef>" : this
					.getSecondKey().toString());
			return sb.toString();
		}

		public Key getOneKey() {
			return this.oneKey;
		}

		public Key getOtherKey(final Key key) {
			return key == this.firstKey ? this.secondKey
					: key == this.secondKey ? this.firstKey : null;
		}

		public Key getSecondKey() {
			return this.secondKey;
		}

		public ComponentStatus getStatus() {
			return this.status;
		}

		public int hashCode() {
			final int firstHash = this.firstKey.hashCode();
			final int secondHash = this.secondKey.hashCode();
			// So that two rels between same keys always match.
			return (Math.min(firstHash, secondHash) + "_" + Math.max(firstHash,
					secondHash)).hashCode();
		}

		public boolean isOneToManyAllowed() {
			return this.oneToManyAllowed;
		}

		public boolean isManyToManyAllowed() {
			return this.manyToManyAllowed;
		}

		public boolean isManyToMany() {
			return this.manyToMany;
		}

		public boolean isOneToMany() {
			return this.oneToMany;
		}

		public boolean isOneToOne() {
			return this.oneToOne;
		}

		public boolean isExternal() {
			return this.external;
		}

		public Key getKeyForTable(final Table table) {
			return (this.firstKey.getTable().equals(table)) ? this.firstKey
					: this.secondKey;
		}

		public Key getKeyForSchema(final Schema schema) {
			return (this.firstKey.getTable().getSchema().equals(schema)) ? this.firstKey
					: this.secondKey;
		}

		public void setCardinality(Cardinality cardinality) {
			Log.debug("Changing cardinality of " + this.getName() + " to "
					+ cardinality);
			if (this.firstKey instanceof PrimaryKey
					&& this.secondKey instanceof PrimaryKey) {
				Log.debug("Overriding cardinality change to ONE");
				cardinality = Cardinality.ONE;
			}
			this.cardinality = cardinality;
			if (this.cardinality.equals(Cardinality.ONE)) {
				this.oneToOne = true;
				this.oneToMany = false;
				this.manyToMany = false;
				this.oneKey = null;
				this.manyKey = null;
			} else {
				this.oneToOne = false;
				this.oneToMany = this.firstKey instanceof PrimaryKey
						|| this.secondKey instanceof PrimaryKey;
				this.manyToMany = !this.oneToMany;
				if (this.oneToMany) {
					this.oneKey = this.firstKey instanceof PrimaryKey ? this.firstKey
							: this.secondKey;
					this.manyKey = this.firstKey instanceof ForeignKey ? this.firstKey
							: this.secondKey;
					;
				} else {
					this.oneKey = null;
					this.manyKey = null;
				}
			}
		}

		public void setStatus(final ComponentStatus status)
				throws AssociationException {
			Log.debug("Changing status of " + this.getName() + " to " + status);
			// If the new status is not incorrect, we need to make sure we
			// can legally do this, ie. the two keys have the same number of
			// columns each.
			if (!status.equals(ComponentStatus.INFERRED_INCORRECT))
				// Check both keys have same cardinality.
				if (this.firstKey.countColumns() != this.secondKey
						.countColumns())
					throw new AssociationException(Resources
							.get("keyColumnCountMismatch"));

			// Make the change.
			this.status = status;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Always returns the output of {@link #getName()}.
		 */
		public String toString() {
			return "" + this.getName();
		}
	}
}
