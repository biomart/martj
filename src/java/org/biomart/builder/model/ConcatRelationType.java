/*
 * ConcatRelationType.java
 *
 * Created on 27 March 2006, 16:23
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

/**
 * Represents a method of concatenating values in a key referenced by a
 * concat-only {@link Relation}. It simply represents the separator to use.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 29th March 2006
 * @since 0.1
 */
public class ConcatRelationType implements Comparable {
    
    /**
     * Internal reference to the set of {@link ConcatRelationType} singletons.
     */
    private static final Map singletons = new HashMap();
    /**
     * Use this constant to refer to value-separation by commas.
     */
    public static final ConcatRelationType COMMA = ConcatRelationType.get(",");

    /**
     * Use this constant to refer to value-separation by spaces.
     */
    public static final ConcatRelationType SPACE = ConcatRelationType.get(" ");

    /**
     * Use this constant to refer to value-separation by tabs.
     */
    public static final ConcatRelationType TAB = ConcatRelationType.get("\t");
    
    /**
     * Internal reference to the name of this {@link ConcatRelationType}.
     */
    private final String name;
    
    /**
     * The static factory method creates and returns a {@link ConcatRelationType}
     * with the given name. It ensures the object returned is a singleton.
     * Note that the names of {@link ConcatRelationType} objects are case-insensitive.
     * @param name the name of the {@link ConcatRelationType} object.
     * @return the {@link ConcatRelationType} object.
     */
    public static ConcatRelationType get(String name) {
        // Convert to upper case.
        name = name.toUpperCase();
        // Do we already have this one?
        // If so, then return it.
        if (singletons.containsKey(name)) return (ConcatRelationType)singletons.get(name);
        // Otherwise, create it, remember it, then return it.
        ConcatRelationType pct = new ConcatRelationType(name);
        singletons.put(name,pct);
        return pct;
    }
    
    /**
     * The private constructor takes a single parameter, which defines the name
     * this {@link ConcatRelationType} object will display when printed.
     * @param name the name of the {@link ConcatRelationType}.
     */
    private ConcatRelationType(String name) {
        this.name=name;
    }
    
    /**
     * Displays the name of this {@link ConcatRelationType} object.
     * @return the name of this {@link ConcatRelationType} object.
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
     * @throws ClassCastException if the object o is not a {@link ConcatRelationType}.
     */
    public int compareTo(Object o) throws ClassCastException {
        ConcatRelationType pct = (ConcatRelationType)o;
        return this.toString().compareTo(pct.toString());
    }
    
    /**
     * Return true if the objects are identical.
     * @param o the object to compare to.
     * @return true if the names are the same and both are {@link ConcatRelationType} instances,
     * otherwise false.
     */
    public boolean equals(Object o) {
        // We are dealing with singletons so can use == happily.
        return o==this;
    }
}
