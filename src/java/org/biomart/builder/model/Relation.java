/*
 * Relation.java
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
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>A {@link Relation} represents the association betwen a {@link PrimaryKey}
 * and a {@link ForeignKey}. Both {@link Key}s must have the same number of
 * {@link Column}s, and the related {@link Column}s must appear
 * in the same order in both {@link Key}s. If they do not, then results may
 * be unpredictable.</p>
 * <p>An {@link GenericRelation} class is provided to form the basic
 * functionality outlined above. Subclasses of {@link GenericRelation}
 * represent different kinds of association between {@link Key}s.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 5th May 2006
 * @since 0.1
 */
public interface Relation extends Comparable {
    /**
     * Returns the name of this relation.
     * @return the name of this {@link Relation}.
     */
    public String getName();
    
    /**
     * Returns the {@link ComponentStatus} of this {@link Relation}. The default value,
     * unless otherwise specified, is INFERRED.
     * @return the {@link ComponentStatus} of this {@link Relation}.
     */
    public ComponentStatus getStatus();
    
    /**
     * Sets the {@link ComponentStatus} of this {@link Relation}.
     * @param status the new {@link ComponentStatus} of this {@link Relation}.
     */
    public void setStatus(ComponentStatus status);
    
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
     * Returns the {@link Cardinality} of the {@link ForeignKey} end
     * of this {@link Relationship}.
     * @return the {@link Cardinality}.
     */
    public Cardinality getFKCardinality();
    
    /**
     * Sets the {@link Cardinality} of the {@link ForeignKey} end
     * of this {@link Relationship}.
     * @param cardinality the {@link Cardinality}.
     */
    public void setFKCardinality(Cardinality cardinality);
    
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
         * Internal reference to the set of {@link Cardinality} singletons.
         */
        private static final Map singletons = new HashMap();
        
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
            this.name = name;
        }
        
        /**
         * Displays the name of this {@link Cardinality} object.
         * @return the name of this {@link Cardinality} object.
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return this.getName();
        }
        
        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * {@inheritDoc}
         */
        public int compareTo(Object o) throws ClassCastException {
            Cardinality c = (Cardinality)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            // We are dealing with singletons so can use == happily.
            return o == this;
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
        private final PrimaryKey primaryKey;
        
        /**
         * Internal reference to the {@link ForeignKey} of this {@link Relation}.
         */
        private final ForeignKey foreignKey;
        
        /**
         * Internal reference to the {@link Cardinality} of this {@link Relation}.
         */
        private Cardinality cardinality;
        
        /**
         * Internal reference to the {@link ComponentStatus} of this {@link Relation}.
         */
        private ComponentStatus status;
        
        /**
         * This constructor tests that both ends of the {@link Relation} have
         * {@link Key</code>s with the same number of <code>Column}s.
         * The default constructor sets the status to INFERRED.
         * @param primaryKey the source {@link PrimaryKey}.
         * @param foreignKey the target {@link ForeignKey}.
         * @param cardinality the {@link Cardinality} of the {@link ForeignKey}.
         * @throws AssociationException if the number of {@link Column}s in the {@link Key}s don't match.
         */
        public GenericRelation(PrimaryKey primaryKey, ForeignKey foreignKey, Cardinality cardinality) throws AssociationException {
            // Sanity checks.
            if (primaryKey.countColumns()!=foreignKey.countColumns())
                throw new AssociationException(BuilderBundle.getString("keyColumnCountMismatch"));
            // Remember the keys.
            this.primaryKey = primaryKey;
            this.foreignKey = foreignKey;
            this.cardinality = cardinality;
            this.status = ComponentStatus.INFERRED;
            // Add ourselves at both ends.
            primaryKey.addRelation(this);
            foreignKey.addRelation(this);
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getPrimaryKey().getName());
            sb.append(":");
            sb.append(this.getForeignKey().getName());
            return sb.toString();
        }
        
        /**
         * {@inheritDoc}
         */
        public ComponentStatus getStatus() {
            return this.status;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setStatus(ComponentStatus status) {
            // Do it.
            this.status = status;
        }
        
        /**
         * {@inheritDoc}
         */
        public PrimaryKey getPrimaryKey() {
            return this.primaryKey;
        }
        
        /**
         * {@inheritDoc}
         */
        public ForeignKey getForeignKey() {
            return this.foreignKey;
        }
        
        /**
         * {@inheritDoc}
         */
        public Cardinality getFKCardinality() {
            return this.cardinality;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setFKCardinality(Cardinality cardinality) {
            // Do it.
            this.cardinality = cardinality;
        }
        
        /**
         * {@inheritDoc}
         */
        public void destroy() {
            this.getPrimaryKey().removeRelation(this);
            this.getForeignKey().removeRelation(this);
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return this.getName();
        }
        
        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * {@inheritDoc}
         */
        public int compareTo(Object o) throws ClassCastException {
            Relation r = (Relation)o;
            return this.toString().compareTo(r.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Relation)) return false;
            Relation r = (Relation)o;
            return r.toString().equals(this.toString());
        }
    }
}
