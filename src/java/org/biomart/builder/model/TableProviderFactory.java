/*
 * TableProviderFactory.java
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

import org.biomart.builder.model.TableProvider.GenericTableProvider;

/**
 * <p>This interface represents a source  from which {@link TableProvider} object
 * can be made. It could be a JDBC connection (either with intelligent (DMD) or guessed
 * (non-DMD) keys), or an XML document with a DTD and a data document.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 28th March 2006
 * @since 0.1
 */
public interface TableProviderFactory extends Comparable {
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
     * Obtains a {@link TableProvider} which knows how to interact with a data source.
     * @param name the name to give the generated table provider.
     * @return a {@link TableProvider} object which can interact with the data source.
     */
    public TableProvider getTableProvider(String name);
    
    /**
     * The generic implementation returns {@link GenericTableProvider} instances that
     * aren't associated with any particular data source.
     */
    public class GenericTableProviderFactory implements TableProviderFactory {
        /**
         * Internal reference to the name of this factory.
         */
        private String name;
        
        /**
         * Creates a factory with a name.
         * @param name the name to give the factory.
         * @throws NullPointerException if the name is null.
         */
        public GenericTableProviderFactory(String name) throws NullPointerException {
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
         * Obtains a {@link TableProvider} which knows how to interact with a data source.
         * @param name the name to give the generated table provider.
         * @return a {@link TableProvider} object which can interact with the data source.
         */
        public TableProvider getTableProvider(String name) {
            return new GenericTableProvider(name);
        }
        
        /**
         * Displays the name of this {@link TableProvider} object.
         * @return the name of this {@link TableProvider} object.
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
         * @throws ClassCastException if the object o is not a {@link TableProvider}.
         */
        public int compareTo(Object o) throws ClassCastException {
            TableProvider t = (TableProvider)o;
            return this.toString().compareTo(t.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link TableProvider}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof TableProvider)) return false;
            TableProvider t = (TableProvider)o;
            return t.toString().equals(this.toString());
        }
    }
}
