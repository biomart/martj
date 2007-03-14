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

package org.biomart.common.view.gui.panels;

import java.util.Properties;

import javax.swing.JPanel;

import org.biomart.common.model.Schema;

/**
 * Connection panels represent all the different options available when
 * establishing a schema. They don't do the common stuff - name, type, etc. -
 * but do the stuff specific to a particular type of schema. They handle
 * validation of input, and can modify or create schemas based on the input.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.5
 */
public abstract class SchemaConnectionPanel extends JPanel {
	/**
	 * Creates a new schema based on the values in the panel's fields.
	 * 
	 * @param schemaName
	 *            the name of the schema to be created.
	 * @return the created schema. It will return <tt>null</tt> if creation
	 *         failed, eg. the currently field values are not valid.
	 */
	public abstract Schema createSchemaFromSettings(String schemaName);

	/**
	 * Modifies a schema based on the values in the panel's fields.
	 * 
	 * @param schema
	 *            the schema to modify.
	 * @return the modified schema. It will return <tt>null</tt> if
	 *         modification failed, eg. the currently field values are not
	 *         valid.
	 */
	public abstract Schema copySettingsToExistingSchema(Schema schema);

	/**
	 * Resets the fields based on a given template. Specify <tt>null</tt> for
	 * the template if you want complete defaults.
	 * 
	 * @param template
	 *            the template to reset the fields for.
	 */
	public abstract void copySettingsFromSchema(Schema template);

	/**
	 * Validates the current values of the fields in the panel.
	 * 
	 * @return <tt>true</tt> if all is well, <tt>false</tt> if not, and may
	 *         possible pop up some messages for the user to read en route.
	 */
	public abstract boolean validateFields();

	/**
	 * Using a properties object from history that matches this class, copy
	 * settings and populate the dialog from it.
	 * 
	 * @param template
	 *            the properties to copy into the dialog.
	 */
	public abstract void copySettingsFromProperties(final Properties template);

	/**
	 * Work out what class of {@link Schema} objects this panel edits.
	 * 
	 * @return the type of {@link Schema} objects this panel edits.
	 */
	public abstract Class getSchemaClass();
}
