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
 * Represents the status of any component with regard to
 * how the system came to know about it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 21st June 2006
 * @since 0.1
 */
public class ComponentStatus implements Comparable {
	private static final Map singletons = new HashMap();

	private final String name;

	/**
	 * Use this constant to refer to a component that was inferred from the
	 * database and is assumed to be correct.
	 */
	public static final ComponentStatus INFERRED = ComponentStatus
			.get("INFERRED");

	/**
	 * Use this constant to refer to a component that was incorrectly inferred
	 * from the database and should be ignored.
	 */
	public static final ComponentStatus INFERRED_INCORRECT = ComponentStatus
			.get("INFERRED_INCORRECT");

	/**
	 * Use this constant to refer to a component that was specified by the user.
	 */
	public static final ComponentStatus HANDMADE = ComponentStatus
			.get("HANDMADE");

	/**
	 * The static factory method creates and returns a status object with the
	 * given name. It ensures the object returned is a singleton. Note that the
	 * names of status objects are case-sensitive.
	 * 
	 * @param name
	 *            the name of the status object.
	 * @return the status object.
	 */
	public static ComponentStatus get(String name) {
		// Do we already have this one?
		// If so, then return it.
		if (singletons.containsKey(name))
			return (ComponentStatus) singletons.get(name);

		// Otherwise, create it, remember it.
		ComponentStatus s = new ComponentStatus(name);
		singletons.put(name, s);

		// Return it.
		return s;
	}

	/**
	 * The private constructor takes a single parameter, which defines the name
	 * this status object will display when printed.
	 * 
	 * @param name
	 *            the name of the status.
	 */
	private ComponentStatus(String name) {
		this.name = name;
	}

	/**
	 * Displays the name of this status object.
	 * 
	 * @return the name of this status object.
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
		ComponentStatus c = (ComponentStatus) o;
		return this.toString().compareTo(c.toString());
	}

	public boolean equals(Object o) {
		// We are dealing with singletons so can use == happily.
		return o == this;
	}
}