/*
 * MartConstructorFactory.java
 *
 * Created on 27 March 2006, 11:21
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

import org.biomart.builder.model.MartConstructor.GenericMartConstructor;

/**
 * <p>This interface represents a database supplying {@link MartConstructor} objects
 * which can use it to do their stuff. It should have an underlying JDBC connection of some 
 * kind.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 28th March 2006
 * @since 0.1
 */
public interface MartConstructorFactory extends Comparable {
    /**
     * Sets the name of the factory.
     * @param name the name to give the factory.
     * @throws NullPointerException if the name is null.
     */
    public void setName(String name) throws NullPointerException;
    
    /**
     * Returns the name of the factory.
     * @return name the name of the factory.
     */
    public String getName();
    
    /**
     * Obtains a {@link MartConstructor} which knows how to interact with a database.
     * @param name the name to give the generated mart constructor.
     * @return a {@link MartConstructor} object which can interact with the database.
     */
    public MartConstructor getMartConstructor(String name);
    
    /**
     * The generic implementation returns {@link GenericMartConstructor} instances that
     * aren't associated with any particular database.
     */
    public class GenericMartConstructorFactory implements MartConstructorFactory {
        /**
         * Internal reference to the name of this factory.
         */
        private String name;
        
        /**
         * Creates a factory with a name.
         * @param name the name to give the factory.
         * @throws NullPointerException if the name is null.
         */
        public GenericMartConstructorFactory(String name) throws NullPointerException {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("Factory must have a name!");
            // Do the work.
            this.name = name;
        }
        
        /**
         * Sets the name of the factory.
         * @param name the name to give the factory.
         * @throws NullPointerException if the name is null.
         */
        public void setName(String name) throws NullPointerException {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("Factory must have a name!");
            // Do the work.
            this.name = name;
        }
        
        /**
         * Returns the name of the factory.
         * @return name the name of the factory.
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * Obtains a {@link MartConstructor} which knows how to interact with a database.
         * @param name the name to give the generated table provider.
         * @return a {@link MartConstructor} object which can interact with the database.
         */
        public MartConstructor getMartConstructor(String name) {
            return new GenericMartConstructor(name);
        }
        
        /**
         * Displays the name of this {@link MartConstructor} object.
         * @return the name of this {@link MartConstructor} object.
         */
        public String toString() {
            return this.getName();
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
         * @throws ClassCastException if the object o is not a {@link MartConstructor}.
         */
        public int compareTo(Object o) throws ClassCastException {
            MartConstructor c = (MartConstructor)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link MartConstructor}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof MartConstructor)) return false;
            MartConstructor c = (MartConstructor)o;
            return c.toString().equals(this.toString());
        }
    }
}
