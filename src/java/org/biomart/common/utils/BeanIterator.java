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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

/**
 * This class wraps an existing iterator, and causes {@link PropertyChangeEvent}
 * events to be fired whenever it changes.
 * <p>
 * Removing values will result in events where the before value is they value
 * being removed and the after value is null.
 * <p>
 * All events will have a property of {@link BeanIterator#propertyName}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public class BeanIterator extends WeakPropertyChangeSupport implements Iterator {

	private static final long serialVersionUID = 1L;

	/**
	 * This field is intended for use by subclasses so that they can access the
	 * delegate directly.
	 */
	protected final Iterator delegate;

	/**
	 * The property key used in events generated by this map.
	 */
	public static final String propertyName = "IteratorEntry";

	/**
	 * This is intended for use by subclasses that can move in both directions.
	 */
	protected Object currentObj;

	/**
	 * Construct a new instance that wraps the delegate iterator and produces
	 * {@link PropertyChangeEvent} events whenever the delegate iterator
	 * changes.
	 * 
	 * @param delegate
	 *            the delegate iterator.
	 */
	public BeanIterator(final Iterator delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	/**
	 * Construct a new instance that wraps the delegate iterator and produces
	 * {@link PropertyChangeEvent} events whenever the delegate iterator
	 * changes.
	 * <p>
	 * As the delegate is a {@link BeanIterator} instance, it doesn't wrap the
	 * iterator directly, instead it finds out the delegate's delegate and wraps
	 * that instead. It will also notify all listeners that are already
	 * listening to the delegate.
	 * 
	 * @param delegate
	 *            the delegate iterator.
	 */
	public BeanIterator(final BeanIterator delegate) {
		this(delegate.delegate);
		final PropertyChangeListener[] listeners = delegate
				.getPropertyChangeListeners();
		for (int i = 0; i < listeners.length; i++)
			this.addPropertyChangeListener(listeners[i]);
	}

	public boolean hasNext() {
		return this.delegate.hasNext();
	}

	public Object next() {
		this.currentObj = this.delegate.next();
		return this.currentObj;
	}

	public void remove() {
		this.delegate.remove();
		this.firePropertyChange(BeanIterator.propertyName, this.currentObj,
				null);
	}

	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		else if (obj instanceof BeanIterator)
			return this.delegate.equals(((BeanIterator) obj).delegate);
		else if (obj instanceof Iterator)
			return this.delegate.equals(obj);
		else
			return false;
	}

	public int hashCode() {
		return this.delegate.hashCode();
	}

	public String toString() {
		return this.delegate.toString();
	}
}
