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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.biomart.builder.controller.MartBuilderUtils;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructor.DummyMartConstructor;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This connection panel implementation allows a user to define nothing in
 * particular, and ultimately creates a {@link DummyMartConstructor}
 * implementation which represents this nothingness.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 6th June 2006
 * @since 0.1
 */
public class DummyMartConstructorConnectionPanel extends
		MartConstructorConnectionPanel {
	private static final long serialVersionUID = 1;

	private DataSetTabSet datasetTabSet;

	/**
	 * This constructor creates a panel with all the fields necessary to
	 * construct a {@link DummyMartConstructor} instance, save the name which
	 * will be passed in elsewhere. It optionally takes a template which is used
	 * to populate the fields of the panel. If this template is null, then
	 * defaults are used instead.
	 * 
	 * @param datasetTabSet
	 *            the tabset we belong to.
	 * @param template
	 *            the template to use to fill the values in the panel with. If
	 *            this is null, defaults are used.
	 */
	public DummyMartConstructorConnectionPanel(DataSetTabSet datasetTabSet,
			MartConstructor template) {
		super();

		// Remember our parents.
		this.datasetTabSet = datasetTabSet;

		// Reset the fields to their defaults, based on the
		// template provided (if any).
		this.resetFields(template);
	}

	private void resetFields(MartConstructor template) {
		// If the template is a dummy one, copy the settings
		// from it.
		if (template instanceof DummyMartConstructor)
			this.copySettingsFrom(template);

		// Otherwise, set some sensible defaults.
		else {
			// Nada.
		}
	}

	private void copySettingsFrom(MartConstructor template) {
		// Test to make sure the template is a dummy one.
		if (template instanceof DummyMartConstructor) {
			// Nada.
		}
	}

	/**
	 * Validates the fields. If any are invalid, it pops up a message saying so.
	 * 
	 * @return <tt>true</tt> if all is well, <tt>false</tt> if not.
	 */
	public boolean validateFields() {
		// Make a list to hold any validation messages that may occur.
		List messages = new ArrayList();

		// Nada.

		// If there any messages to show the user, show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(this,
					messages.toArray(new String[0]), BuilderBundle
							.getString("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there were no messages.
		return messages.isEmpty();
	}

	/**
	 * Creates a {@link DummyMartConstructor} with the given name, based on the
	 * user input inside the panel.
	 * 
	 * @param name
	 *            the name to give the mart constructor.
	 * @return the created mart constructor.
	 */
	public MartConstructor createMartConstructor(String name) {
		// If the fields aren't valid, we can't create it.
		if (!this.validateFields())
			return null;

		try {
			// Record the user's specifications.

			// Nada.

			// Construct a dummy mart constructor based on them.
			DummyMartConstructor mc = MartBuilderUtils
					.createDummyMartConstructor(name);

			// Return that mart constructor.
			return mc;
		} catch (Throwable t) {
			this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(
					t);
		}

		// If we got here, something went wrong, so behave
		// as though validation failed.
		return null;
	}
}
