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

/**
 * This interface defines a listener which hears events about mart construction.
 * The events are defined as constants in this interface.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 26th June 2006
 * @since 0.1
 */
public interface MartConstructorListener {
	/**
	 * This event will occur when mart construction begins.
	 */
	public static final int CONSTRUCTION_STARTED = 0;

	/**
	 * This event will occur when mart construction ends.
	 */
	public static final int CONSTRUCTION_FINISHED = 1;

	/**
	 * This method will be called when an event occurs.
	 * 
	 * @param event
	 *            the event that occurred. See the constants defined elsewhere
	 *            in this interface for possible events.
	 */
	public void mcEventOccurred(int event);
}
