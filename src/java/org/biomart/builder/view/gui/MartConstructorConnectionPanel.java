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

package org.biomart.builder.view.gui;

import javax.swing.JPanel;

import org.biomart.builder.model.MartConstructor;

/**
 * Connection panels represent all the different options available when
 * establishing a mart constructor. They don't do the common stuff - name, type -
 * but do the stuff specific to a particular type of mart constructor. They
 * handle validation of input, and can modify or create mart constructors based
 * on the input.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 6th June 2006
 * @since 0.1
 */
public abstract class MartConstructorConnectionPanel extends JPanel {
	/**
	 * Creates a new mart constructor based on the values in the panel's fields.
	 * 
	 * @param mcName
	 *            the name of the mart constructor to be created.
	 * @return the created mart constructor. It will return null if creation
	 *         failed, eg. the currently field values are not valid.
	 */
	public abstract MartConstructor createMartConstructor(String mcName);

	/**
	 * Validates the current values of the fields in the panel.
	 * 
	 * @return <tt>true</tt> if all is well, <tt>false</tt> if not, and may
	 *         possible pop up some messages for the user to read en route.
	 */
	public abstract boolean validateFields();
}
