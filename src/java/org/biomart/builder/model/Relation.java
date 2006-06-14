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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>
 * A relation represents the association between two keys. Relations between two
 * primary keys are always 1:1. Relations between two foreign keys are either
 * 1:1 or M:M. Relations between a foreign key and a primary key can either be
 * 1:1 or 1:M.
 * <p>
 * Both keys must have the same number of columns, and the related columns
 * should appear in the same order in both keys. If they do not, then results
 * may be unpredictable.
 * <p>
 * An {@link GenericRelation} class is provided to form the basic functionality
 * outlined above.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 13th June 2006
 * @since 0.1
 */
public interface Relation extends Comparable {
	/**
	 * Returns the name of this relation.
	 * 
	 * @return the name of this relation.
	 */
	public String getName();

	/**
	 * Returns the status of this relation. The default value, unless otherwise
	 * specified, is {@link ComponentStatus#INFERRED}.
	 * 
	 * @return the status of this relation.
	 */
	public ComponentStatus getStatus();

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
	 * Returns the first key of this relation.
	 * 
	 * @return the first key.
	 */
	public Key getFirstKey();

	/**
	 * Returns the second key of this relation.
	 * 
	 * @return the second key.
	 */
	public Key getSecondKey();

	/**
	 * Returns whether or not this relation is optional or compulsory. A
	 * relation is optional of one or both of the keys are nullable. Optionality
	 * is set through the keys, not through the relation.
	 * 
	 * @return <tt>true</tt> if this relation is optional, <tt>false</tt> if
	 *         not.
	 */
	public boolean isOptional();

	/**
	 * Given a key that is in this relationship, return the other key.
	 * 
	 * @param key
	 *            the key we know is in this relationship.
	 * @return the other key in this relationship.
	 * @throws IllegalArgumentException
	 *             if the key specified is not in this relationship.
	 */
	public Key getOtherKey(Key key) throws IllegalArgumentException;

	/**
	 * In a 1:M relation, this will return the M end of the relation. In all
	 * other relation types, this will return <tt>null</tt>.
	 * 
	 * @return the key at the many end of the relation, or <tt>null</tt> if
	 *         this is not a 1:M relation.
	 */
	public Key getManyKey();

	/**
	 * In a 1:M relation, this will return the 1 end of the relation. In all
	 * other relation types, this will return <tt>null</tt>.
	 * 
	 * @return the key at the one end of the relation, or <tt>null</tt> if
	 *         this is not a 1:M relation.
	 */
	public Key getOneKey();

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
	 * Sets the cardinality of the foreign key end of this relation, in a 1:M
	 * relation. If used on a 1:1 or M:M relation, then specifying M makes it
	 * M:M and specifying 1 makes it 1:1.
	 * 
	 * @param cardinality
	 *            the cardinality.
	 */
	public void setCardinality(Cardinality cardinality);

	/**
	 * Can this relation be 1:M? Returns true in all cases where both keys are
	 * of different types.
	 * 
	 * @return <tt>true</tt> if this can be 1:M, <tt>false</tt> if not.
	 */
	public boolean isOneToManyAllowed();

	/**
	 * Can this relation be M:M? Returns true where both keys are foreign keys.
	 * 
	 * @return <tt>true</tt> if this can be M:M, <tt>false</tt> if not.
	 */
	public boolean isManyToManyAllowed();

	/**
	 * Deconstructs the relation by removing references to itself from the keys
	 * at both ends.
	 */
	public void destroy();

	/**
	 * Returns <tt>true</tt> if this is a 1:1 relation.
	 * 
	 * @return <tt>true</tt> if this is a 1:1 relation, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isOneToOne();

	/**
	 * Returns <tt>true</tt> if this is a 1:M relation.
	 * 
	 * @return <tt>true</tt> if this is a 1:M relation, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isOneToMany();

	/**
	 * Returns <tt>true</tt> if this is a M:M relation.
	 * 
	 * @return <tt>true</tt> if this is a M:M relation, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isManyToMany();

	/**
	 * Returns <tt>true</tt> if this relation involves keys in two separate
	 * schemas. Those that do are external, those that don't are not.
	 * 
	 * @return <tt>true</tt> if this is external, <tt>false</tt> otherwise.
	 */
	public boolean isExternal();

	/**
	 * This internal singleton class represents the cardinality of a foreign key
	 * involved in a relation. Note that the names of cardinality objects are
	 * case-insensitive.
	 */
	public class Cardinality implements Comparable {
		private static final Map singletons = new HashMap();

		private final String name;

		/**
		 * Use this constant to refer to 1:1 relation.
		 */
		public static final Cardinality ONE = Cardinality.get("1");

		/**
		 * Use this constant to refer to a 1:M relation.
		 */
		public static final Cardinality MANY = Cardinality.get("M");

		/**
		 * The static factory method creates and returns a cardinality with the
		 * given name. It ensures the object returned is a singleton. Note that
		 * the names of cardinality objects are case-insensitive.
		 * 
		 * @param name
		 *            the name of the cardinality object.
		 * @return the cardinality object.
		 */
		public static Cardinality get(String name) {
			// Convert to upper case.
			name = name.toUpperCase();

			// Do we already have this one?
			// If so, then return it.
			if (singletons.containsKey(name))
				return (Cardinality) singletons.get(name);

			// Otherwise, create it, remember it.
			Cardinality c = new Cardinality(name);
			singletons.put(name, c);

			// Return it.
			return c;
		}

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this cardinality object will display when printed.
		 * 
		 * @param name
		 *            the name of the cardinality.
		 */
		private Cardinality(String name) {
			this.name = name;
		}

		/**
		 * Displays the name of this cardinality object.
		 * 
		 * @return the name of this cardinality object.
		 */
		public String getName() {
			return this.name;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			Cardinality c = (Cardinality) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}

	/**
	 * This class provides basic functionality, for instance checking that the
	 * keys at both ends have the same number of columns.
	 */
	public class GenericRelation implements Relation {
		private final Key firstKey;

		private final Key secondKey;

		private Cardinality cardinality;

		private ComponentStatus status;

		/**
		 * This constructor tests that both ends of the relation have keys with
		 * the same number of columns. The default constructor sets the status
		 * to {@link ComponentStatus#INFERRED}.
		 * 
		 * @param key
		 *            the first key.
		 * @param key
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
		 *             the relation already exists, or if one of the keys is a
		 *             foreign key which already has a valid relation elsewhere.
		 */
		public GenericRelation(Key firstKey, Key secondKey,
				Cardinality cardinality) throws AssociationException {
			// Check the keys have the same number of columns.
			if (firstKey.countColumns() != secondKey.countColumns())
				throw new AssociationException(BuilderBundle
						.getString("keyColumnCountMismatch"));

			// Remember the keys etc.
			this.firstKey = firstKey;
			this.secondKey = secondKey;
			this.setCardinality(cardinality);
			this.status = ComponentStatus.INFERRED;

			// Check the relation doesn't already exist.
			if (firstKey.getRelations().contains(this))
				throw new AssociationException(BuilderBundle
						.getString("relationAlreadyExists"));

			// Check that any foreign key doesn't have an active relation
			// elsewhere.
			boolean fkHasOtherRel = false;
			if (firstKey instanceof ForeignKey)
				for (Iterator i = firstKey.getRelations().iterator(); i
						.hasNext()
						&& !fkHasOtherRel;) {
					Relation r = (Relation) i.next();
					if (!r.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
						fkHasOtherRel = true;
				}
			if (secondKey instanceof ForeignKey)
				for (Iterator i = secondKey.getRelations().iterator(); i
						.hasNext()
						&& !fkHasOtherRel;) {
					Relation r = (Relation) i.next();
					if (!r.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
						fkHasOtherRel = true;
				}
			if (fkHasOtherRel)
				throw new AssociationException(BuilderBundle
						.getString("fkHasMultiplePKs"));

			// Add ourselves to the keys at both ends.
			firstKey.addRelation(this);
			secondKey.addRelation(this);
		}

		public String getName() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.getFirstKey().toString());
			sb.append(":");
			sb.append(this.getSecondKey().toString());
			return sb.toString();
		}

		public ComponentStatus getStatus() {
			return this.status;
		}

		public void setStatus(ComponentStatus status)
				throws AssociationException {
			// If the new status is not incorrect, we need to make sure we
			// can legally do this, ie. the two keys have the same number of
			// columns each.
			if (!status.equals(ComponentStatus.INFERRED_INCORRECT)) {
				// Check both keys have same cardinality.
				if (this.firstKey.countColumns() != this.secondKey
						.countColumns())
					throw new AssociationException(BuilderBundle
							.getString("keyColumnCountMismatch"));
			}

			// Make the change.
			this.status = status;
		}

		public Key getFirstKey() {
			return this.firstKey;
		}

		public Key getSecondKey() {
			return this.secondKey;
		}

		public boolean isOptional() {
			if (this.firstKey instanceof ForeignKey)
				return ((ForeignKey) this.firstKey).getNullable();
			else if (this.secondKey instanceof ForeignKey)
				return ((ForeignKey) this.secondKey).getNullable();
			else
				return false;
		}

		public Key getOtherKey(Key key) throws IllegalArgumentException {
			if (key.equals(this.firstKey))
				return this.secondKey;
			else if (key.equals(this.secondKey))
				return this.firstKey;
			else
				throw new IllegalArgumentException(BuilderBundle
						.getString("keyNotInRel"));
		}

		public Key getManyKey() {
			if (!this.isOneToMany())
				return null;
			// The many end is the foreign key end.
			return (this.firstKey instanceof ForeignKey) ? this.firstKey
					: this.secondKey;
		}

		public Key getOneKey() {
			if (!this.isOneToMany())
				return null;
			// The one end is the primary key end.
			return (this.firstKey instanceof PrimaryKey) ? this.firstKey
					: this.secondKey;
		}

		public Cardinality getCardinality() {
			return this.cardinality;
		}

		public void setCardinality(Cardinality cardinality) {
			if ((this.firstKey instanceof PrimaryKey)
					&& (this.secondKey instanceof PrimaryKey))
				cardinality = Cardinality.ONE;
			this.cardinality = cardinality;
		}

		public boolean isOneToManyAllowed() {
			return !this.firstKey.getClass().equals(this.secondKey.getClass());
		}

		public boolean isManyToManyAllowed() {
			return ((this.firstKey instanceof ForeignKey) && (this.secondKey instanceof ForeignKey));
		}

		public void destroy() {
			this.firstKey.removeRelation(this);
			this.secondKey.removeRelation(this);
		}

		public boolean isOneToOne() {
			return this.cardinality.equals(Cardinality.ONE);
		}

		public boolean isOneToMany() {
			return this.cardinality.equals(Cardinality.MANY)
					&& (this.firstKey instanceof PrimaryKey);
		}

		public boolean isManyToMany() {
			return this.cardinality.equals(Cardinality.MANY)
					&& (this.firstKey instanceof ForeignKey);
		}

		public boolean isExternal() {
			return !this.firstKey.getTable().getSchema().equals(
					this.secondKey.getTable().getSchema());
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			Relation r = (Relation) o;
			return this.toString().compareTo(r.toString());
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof Relation))
				return false;
			Relation r = (Relation) o;
			return r.toString().equals(this.toString());
		}
	}
}
