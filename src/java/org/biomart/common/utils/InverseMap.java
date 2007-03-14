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

package org.biomart.common.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class defines an inverse view of a map.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class InverseMap implements Map {

	final private Map map;

	/**
	 * Defines an inverse view. Only works if the values in the viewed map are
	 * unique.
	 * 
	 * @param map
	 *            the map to view inversely.
	 */
	public InverseMap(final Map map) {
		this.map = map;
	}

	public void clear() {
		this.map.clear();
	}

	public boolean containsKey(Object key) {
		return this.map.containsValue(key);
	}

	public boolean containsValue(Object value) {
		return this.map.containsKey(value);
	}

	public Set entrySet() {
		final HashSet entries = new HashSet();
		for (final Iterator i = this.map.entrySet().iterator(); i.hasNext();) {
			final Map.Entry me = (Map.Entry) i.next();
			entries.add(new Map.Entry() {
				private final Object key = me.getValue();

				private Object value = me.getKey();

				public Object getKey() {
					return this.key;
				}

				public Object getValue() {
					return this.value;
				}

				public Object setValue(Object value) {
					final Object oldValue = this.value;
					InverseMap.this.map.put(me.getKey(), value);
					this.value = value;
					return oldValue;
				}
			});
		}
		return entries;
	}

	public Object get(Object key) {
		for (final Iterator i = this.map.entrySet().iterator(); i.hasNext();) {
			final Map.Entry me = (Map.Entry) i.next();
			if (me.getValue().equals(key))
				return me.getKey();
		}
		return null;
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public Set keySet() {
		return new HashSet(this.map.values());
	}

	public Object put(Object key, Object value) {
		final Object oldValue = this.get(key);
		this.map.put(value, key);
		return oldValue;
	}

	public void putAll(Map t) {
		for (final Iterator i = t.entrySet().iterator(); i.hasNext();) {
			final Map.Entry me = (Map.Entry) i.next();
			this.map.put(me.getValue(), me.getKey());
		}
	}

	public Object remove(Object key) {
		for (final Iterator i = this.map.entrySet().iterator(); i.hasNext();) {
			final Map.Entry me = (Map.Entry) i.next();
			if (me.getValue().equals(key)) {
				final Object value = me.getKey();
				i.remove();
				return value;
			}
		}
		return null;
	}

	public int size() {
		return this.map.size();
	}

	public Collection values() {
		return this.map.keySet();
	}

}
