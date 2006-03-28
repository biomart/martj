/*
 * Relation.java
 *
 * Created on 23 March 2006, 13:10
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

import java.util.HashMap;
import java.util.Map;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;

/**
 * <p>A {@link Relation} represents the association betwen a {@link PrimaryKey}
 * and a {@link ForeignKey}. Both {@link Key}s must have the same number of
 * {@link Column}s, and the related {@link Column}s must appear
 * in the same order in both {@link Key}s. If they do not, then results may
 * be unpredictable.</p>
 *
 * <p>An {@link GenericRelation} class is provided to form the basic
 * functionality outlined above. Subclasses of {@link GenericRelation}
 * represent different kinds of association between {@link Key}s.</p>
 *
 * <p>Two reference implementations are provided which should suffice for most
 * purposes. Both extend {@link GenericRelation}. They are 1:M and 1:1. Note
 * that a M:1 is simply the reverse of a 1:M and therefore needs no additional
 * representation. M:M cannot be simply represented in the Java object model
 * so is not included here.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2 28th March 2006
 * @since 0.1
 */
public interface Relation extends Comparable {
    /**
     * Returns the {@link PrimaryKey} of this {@link Relationship}.
     * @return the {@link PrimaryKey}
     */
    public PrimaryKey getPrimaryKey();
    
    /**
     * Returns the {@link ForeignKey} of this {@link Relationship}.
     * @return the {@link ForeignKey}
     */
    public ForeignKey getForeignKey();
    
    /**
     * Returns the {@link Cardinality} of this {@link Relationship}.
     * @return the {@link Cardinality}
     */
    public Cardinality getFKCardinality();
    
    /**
     * Deconstructs the {@link Relation} by removing references to
     * itself from the {@link Key}s at both ends.
     */
    public void destroy();
    
    /**
     * This internal singleton class represents the cardinality of a {@link ForeignKey}
     * involved in a {@link Relationship}. Note that the names of {@link Cardinality} objects
     * are case-insensitive.
     */
    public class Cardinality implements Comparable {
        /**
         * Use this constant to refer to the '1' end of a 1:M or 1:1 {@link Relationship}.
         */
        public static final Cardinality ONE = Cardinality.get("1");
        
        /**
         * Use this constant to refer to the M1' end of a 1:M or 1:1 {@link Relationship}.
         */
        public static final Cardinality MANY = Cardinality.get("M");
        
        /**
         * Internal reference to the name of this {@link Cardinality}.
         */
        private final String name;
        
        /**
         * Internal reference to the set of {@link Cardinality} singletons.
         */
        private static final Map singletons = new HashMap();
        
        /**
         * The static factory method creates and returns a {@link Cardinality}
         * with the given name. It ensures the object returned is a singleton.
         * Note that the names of {@link Cardinality} objects are case-insensitive.
         * @param name the name of the {@link Cardinality} object.
         * @return the {@link Cardinality} object.
         */
        public static Cardinality get(String name) {
            // Convert to upper case.
            name = name.toUpperCase();
            // Do we already have this one?
            // If so, then return it.
            if (singletons.containsKey(name)) return (Cardinality)singletons.get(name);
            // Otherwise, create it, remember it, then return it.
            Cardinality c = new Cardinality(name);
            singletons.put(name,c);
            return c;
        }
        
        /**
         * The private constructor takes a single parameter, which defines the name
         * this {@link Cardinality} object will display when printed.
         * @param name the name of the {@link Cardinality}.
         */
        private Cardinality(String name) {
            this.name=name;
        }
        
        /**
         * Displays the name of this {@link Cardinality} object.
         * @return the name of this {@link Cardinality} object.
         */
        public String toString() {
            return this.name;
        }
        
        /**
         * Displays the hashcode of this object.
         * @return the hashcode of this object.
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * Sorts by comparing the toString() output.
         * @param o the object to compare to.
         * @return -1 if we are smaller, +1 if we are larger, 0 if we are equal.
         * @throws ClassCastException if the object o is not a {@link Cardinality}.
         */
        public int compareTo(Object o) throws ClassCastException {
            Cardinality c = (Cardinality)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * Return true if the objects are identical.
         * @param o the object to compare to.
         * @return true if the names are the same and both are {@link Cardinality} instances,
         * otherwise false.
         */
        public boolean equals(Object o) {
            // We are dealing with singletons so can use == happily.
            return o==this;
        }
    }
    
    /**
     * This class provides basic functionality, for instance checking that the
     * {@link Key}s at both ends have the same number of columns.
     */
    public class GenericRelation implements Relation {
        /**
         * Internal reference to the {@link PrimaryKey} of this {@link Relation}.
         */
        private final PrimaryKey pk;
        
        /**
         * Internal reference to the {@link ForeignKey} of this {@link Relation}.
         */
        private final ForeignKey fk;
        
        /**
         * Internal reference to the {@link Cardinality} of this {@link Relation}.
         */
        private final Cardinality fkc;
        
        /**
         * This constructor tests that both ends of the {@link Relation} have
         * {@link Key</code>s with the same number of <code>Column}s.
         * @param pk the source {@link PrimaryKey}.
         * @param fk the target {@link ForeignKey}.
         * @param fkc the {@link Cardinality} of the {@link ForeignKey}.
         * @throws AssociationException if the number of {@link Column}s in the {@link Key}s don't match.
         * @throws NullPointerException if either {@link Key} is null or the {@link Cardinality} is null.
         */
        public GenericRelation(PrimaryKey pk, ForeignKey fk, Cardinality fkc) throws AssociationException, NullPointerException {
            // Sanity checks.
            if (fkc==null)
                throw new NullPointerException("The cardinality must not be null.");
            if (pk==null || fk==null)
                throw new NullPointerException("Both primary and foreign Keys must not be null.");
            if (pk.countColumns()!=fk.countColumns())
                throw new AssociationException("Column count in primary and foreign Keys must match.");
            // Remember the keys.
            this.pk = pk;
            this.fk = fk;
            this.fkc = fkc;
        }
        
        /**
         * Returns the {@link PrimaryKey} of this {@link Relationship}.
         * @return the {@link PrimaryKey}
         */
        public PrimaryKey getPrimaryKey() {
            return this.pk;
        }
        
        /**
         * Returns the {@link ForeignKey} of this {@link Relationship}.
         * @return the {@link ForeignKey}
         */
        public ForeignKey getForeignKey() {
            return this.fk;
        }
        
        /**
         * Returns the {@link Cardinality} of this {@link Relationship}.
         * @return the {@link Cardinality}
         */
        public Cardinality getFKCardinality() {
            return this.fkc;
        }
        
        /**
         * Deconstructs the {@link Relation} by removing references to
         * itself from the {@link Key}s at both ends.
         */
        public void destroy() {
            this.getPrimaryKey().removeRelation(this);
            this.getForeignKey().removeRelation(this);
        }
        
        /**
         * <p>The textual representation of a {@link Relation} is the name of the {@link PrimaryKey} followed
         * by a colon and the name of the {@link ForeignKey}, followed by a space then an indication of cardinality
         * in brackets. By name of a {@link Key} we mean the output of the {@link Key#toString() toString()} method
         * on that {@link Key}.</p>
         *
         * <p>e.g.: <pre>pk_sometable:fk_sometable (1:M)</pre></p>
         * @return the textual representation of this {@link Relation} as outlined above.
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getPrimaryKey().toString());
            sb.append(":");
            sb.append(this.getForeignKey().toString());
            sb.append(" (1:");
            sb.append(this.getFKCardinality());
            sb.append(")");
            return sb.toString();
        }
        
        /**
         * Displays the hashcode of this object.
         * @return the hashcode of this object.
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * Sorts by comparing the toString() output.
         * @param o the object to compare to.
         * @return -1 if we are smaller, +1 if we are larger, 0 if we are equal.
         * @throws ClassCastException if the object o is not a {@link Relation}.
         */
        public int compareTo(Object o) throws ClassCastException {
            Relation r = (Relation)o;
            return this.toString().compareTo(r.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link Relation}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof Relation)) return false;
            Relation r = (Relation)o;
            return r.toString().equals(this.toString());
        }
    }
    
    /**
     * This utility class represents a 1:1 {@link Relationship} between two {@link Key}s.
     */
    public class OneToOne extends GenericRelation {
        /**
         * This constructor tests that both ends of the {@link Relation} have
         * {@link Key</code>s with the same number of <code>Column}s.
         * @param pk the source {@link PrimaryKey}.
         * @param fk the target {@link ForeignKey}.
         * @throws AssociationException if the number of {@link Column}s in the {@link Key}s don't match.
         * @throws NullPointerException if either {@link Key} is null.
         */
        public OneToOne(PrimaryKey pk, ForeignKey fk) throws AssociationException, NullPointerException {
            super(pk, fk, Cardinality.ONE);
        }
    }
    
    /**
     * This utility class represents a 1:M {@link Relationship} between two {@link Key}s.
     */
    public class OneToMany extends GenericRelation {
        /**
         * This constructor tests that both ends of the {@link Relation} have
         * {@link Key</code>s with the same number of <code>Column}s.
         * @param pk the source {@link PrimaryKey}.
         * @param fk the target {@link ForeignKey}.
         * @throws AssociationException if the number of {@link Column}s in the {@link Key}s don't match.
         * @throws NullPointerException if either {@link Key} is null.
         */
        public OneToMany(PrimaryKey pk, ForeignKey fk) throws AssociationException, NullPointerException {
            super(pk, fk, Cardinality.MANY);
        }
    }
}
