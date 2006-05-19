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
 * A relation represents the association betwen a primary key and a foreign key.
 * Both keys must have the same number of columns, and the related columns must
 * appear in the same order in both keys. If they do not, then results may be
 * unpredictable.
 * <p>
 * An {@link GenericRelation} class is provided to form the basic functionality
 * outlined above.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 16th May 2006
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
	 * Returns the primary key of this relation.
	 * 
	 * @return the primary key
	 */
	public PrimaryKey getPrimaryKey();

	/**
	 * Returns the foreign key of this relation.
	 * 
	 * @return the foreign key
	 */
	public ForeignKey getForeignKey();

	/**
	 * Returns the cardinality of the foreign key end of this relation.
	 * 
	 * @return the cardinality.
	 */
	public Cardinality getFKCardinality();

	/**
	 * Sets the cardinality of the foreign key end of this relation.
	 * 
	 * @param cardinality
	 *            the cardinality.
	 */
	public void setFKCardinality(Cardinality cardinality);

	/**
	 * Deconstructs the relation by removing references to itself from the keys
	 * at both ends.
	 */
	public void destroy();

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
		private final PrimaryKey primaryKey;

		private final ForeignKey foreignKey;

		private Cardinality cardinality;

		private ComponentStatus status;

		/**
		 * This constructor tests that both ends of the relation have keys with
		 * the same number of columns. The default constructor sets the status
		 * to {@link ComponentStatus#INFERRED}.
		 * 
		 * @param primaryKey
		 *            the primary key.
		 * @param foreignKey
		 *            the foreign key.
		 * @param cardinality
		 *            the cardinality of the foreign key.
		 * @throws AssociationException
		 *             if the number of columns in the keys don't match, or if
		 *             the relation already exists, or if some other valid
		 *             relation exists from the specified foreign key.
		 */
		public GenericRelation(PrimaryKey primaryKey, ForeignKey foreignKey,
				Cardinality cardinality) throws AssociationException {
			// Check the keys have the same number of columns.
			if (primaryKey.countColumns() != foreignKey.countColumns())
				throw new AssociationException(BuilderBundle
						.getString("keyColumnCountMismatch"));

			// Remember the keys etc.
			this.primaryKey = primaryKey;
			this.foreignKey = foreignKey;
			this.cardinality = cardinality;
			this.status = ComponentStatus.INFERRED;

			// Check the relation doesn't already exist.
			if (primaryKey.getRelations().contains(this))
				throw new AssociationException(BuilderBundle
						.getString("relationAlreadyExists"));

			// Check that the foreign key end doesn't have an active relation
			// elsewhere.
			boolean fkHasOtherRel = false;
			for (Iterator i = foreignKey.getRelations().iterator(); i.hasNext()
					&& !fkHasOtherRel;) {
				Relation r = (Relation) i.next();
				if (!r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
					fkHasOtherRel = true;
			}
			if (fkHasOtherRel)
				throw new AssociationException(BuilderBundle
						.getString("fkHasMultiplePKs"));

			// Add ourselves to the keys at both ends.
			primaryKey.addRelation(this);
			foreignKey.addRelation(this);
		}

		public String getName() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.getPrimaryKey().toString());
			sb.append(":");
			sb.append(this.getForeignKey().toString());
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
				if (this.primaryKey.countColumns() != this.foreignKey
						.countColumns())
					throw new AssociationException(BuilderBundle
							.getString("keyColumnCountMismatch"));
			}

			// Make the change.
			this.status = status;
		}

		public PrimaryKey getPrimaryKey() {
			return this.primaryKey;
		}

		public ForeignKey getForeignKey() {
			return this.foreignKey;
		}

		public Cardinality getFKCardinality() {
			return this.cardinality;
		}

		public void setFKCardinality(Cardinality cardinality) {
			this.cardinality = cardinality;
		}

		public void destroy() {
			this.getPrimaryKey().removeRelation(this);
			this.getForeignKey().removeRelation(this);
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
