/*
 * ComponentStatus.java
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
import org.biomart.builder.resources.BuilderBundle;

/**
 * Represents the status of any {@link Key} or {@link Relation} with regard to how
 * the system came to know about it.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 28th March 2006
 * @since 0.1
 */
public class ComponentStatus implements Comparable {
    /**
     * Internal reference to the set of {@link ComponentStatus} singletons.
     */
    private static final Map singletons = new HashMap();
    
    /**
     * Use this constant to refer to a component that was inferred from the database
     * and is assumed to be correct.
     */
    public static final ComponentStatus INFERRED = ComponentStatus.get("INFERRED");
    
    /**
     * Use this constant to refer to a component that was incorrectly inferred from
     * the database and should be ignored.
     */
    public static final ComponentStatus INFERRED_INCORRECT = ComponentStatus.get("INFERRED_INCORRECT");
    
    /**
     * Use this constant to refer to a component that was specified by the user.
     */
    public static final ComponentStatus HANDMADE = ComponentStatus.get("HANDMADE");
    
    /**
     * Internal reference to the name of this {@link ComponentStatus}.
     */
    private final String name;
    
    /**
     * The static factory method creates and returns a {@link ComponentStatus}
     * with the given name. It ensures the object returned is a singleton.
     * Note that the names of {@link ComponentStatus} objects are case-insensitive.
     * @param name the name of the {@link ComponentStatus} object.
     * @return the {@link ComponentStatus} object.
     * @throws NullPointerException if the name is null.
     */
    public static ComponentStatus get(String name) throws NullPointerException {
        // Sanity check.
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        // Convert to upper case.
        name = name.toUpperCase();
        // Do we already have this one?
        // If so, then return it.
        if (singletons.containsKey(name)) return (ComponentStatus)singletons.get(name);
        // Otherwise, create it, remember it, then return it.
        ComponentStatus s = new ComponentStatus(name);
        singletons.put(name,s);
        return s;
    }
    
    /**
     * The private constructor takes a single parameter, which defines the name
     * this {@link ComponentStatus} object will display when printed.
     * @param name the name of the {@link ComponentStatus}.
     */
    private ComponentStatus(String name) {
        this.name=name;
    }
    
    /**
     * Displays the name of this {@link ComponentStatus} object.
     * @return the name of this {@link ComponentStatus} object.
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
        ComponentStatus c = (ComponentStatus)o;
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
