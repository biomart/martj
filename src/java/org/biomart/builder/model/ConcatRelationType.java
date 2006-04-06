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
 * @version 0.1.2, 6th April 2006
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
    public static final ConcatRelationType COMMA = new ConcatRelationType("COMMA", ",");

    /**
     * Use this constant to refer to value-separation by spaces.
     */
    public static final ConcatRelationType SPACE = new ConcatRelationType("SPACE", " ");

    /**
     * Use this constant to refer to value-separation by tabs.
     */
    public static final ConcatRelationType TAB = new ConcatRelationType("TAB", "\t");
    
    /**
     * Internal reference to the name of this {@link ConcatRelationType}.
     */
    private final String name;
    
    /**
     * Internal reference to the separator for this {@link ConcatRelationType}.
     */
    private final String separator;
    
    /**
     * The private constructor takes two parameters, which define the name
     * this {@link ConcatRelationType} object will display when printed, and the
     * separator to use between values that have been concatenated.
     * @param name the name of the {@link ConcatRelationType}.
     * @param separator the separator for this {@link ConcatRelationType}.
     */
    private ConcatRelationType(String name, String separator) {
        this.name = name;
        this.separator = separator;
    }

    /**
     * Displays the name of this {@link ConcatRelationType} object.
     * @return the name of this {@link ConcatRelationType} object.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Displays the separator for this {@link ConcatRelationType} object.
     * @return the separator for this {@link ConcatRelationType} object.
     */
    public String getSeparator() {
        return this.separator;
    }
    
    /**
     * Displays the name of this {@link ConcatRelationType} object.
     * @return the name of this {@link ConcatRelationType} object.
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
        return o == this;
    }
}
