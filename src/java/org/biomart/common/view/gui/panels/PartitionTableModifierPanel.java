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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.model.PartitionTable.AbstractPartitionTable.SelectFromTable;
import org.biomart.common.resources.Resources;

/**
 * Knows how to modify partition-table-specific stuff.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public abstract class PartitionTableModifierPanel extends JPanel {

	private final PartitionTable table;

	/**
	 * Internal layout helper.
	 */
	protected final GridBagConstraints labelConstraints;

	/**
	 * Internal layout helper.
	 */
	protected final GridBagConstraints fieldConstraints;

	/**
	 * Internal layout helper.
	 */
	protected final GridBagConstraints labelLastRowConstraints;

	/**
	 * Internal layout helper.
	 */
	protected final GridBagConstraints fieldLastRowConstraints;

	/**
	 * Construct a new panel.
	 * 
	 * @param table
	 *            the partition table to represent. If <tt>null</tt> then it
	 *            should start with a blank sheet, return <tt>false</tt> for
	 *            {@link #isMutable()}, and return something meaningful from
	 *            {@link #create(String)}.
	 */
	public PartitionTableModifierPanel(final PartitionTable table) {
		super(new GridBagLayout());
		this.table = table;

		// Create constraints for labels that are not in the last row.
		this.labelConstraints = new GridBagConstraints();
		this.labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		this.labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		this.labelConstraints.anchor = GridBagConstraints.LINE_END;
		this.labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		this.fieldConstraints = new GridBagConstraints();
		this.fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		this.fieldConstraints.fill = GridBagConstraints.NONE;
		this.fieldConstraints.anchor = GridBagConstraints.LINE_START;
		this.fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		this.labelLastRowConstraints = (GridBagConstraints) this.labelConstraints
				.clone();
		this.labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		this.fieldLastRowConstraints = (GridBagConstraints) this.fieldConstraints
				.clone();
		this.fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
	}

	/**
	 * Obtain the partition table we are working with.
	 * 
	 * @return the table.
	 */
	protected PartitionTable getTable() {
		return this.table;
	}

	/**
	 * If this table is changeable (update/reset are valid) then this should
	 * return <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if it is changeable.
	 */
	public abstract boolean isMutable();

	/**
	 * Reset the fields to the values specified by the original table passed in.
	 */
	public abstract void reset();

	/**
	 * Get a user-readable description of the type of table being edited.
	 * 
	 * @return the description.
	 */
	public abstract String getType();

	/**
	 * Update the original table passed in to adopt the new values from the
	 * fields displayed.
	 * 
	 * @throws PartitionException
	 *             if it cannot.
	 */
	public abstract void update() throws PartitionException;

	/**
	 * Check to see if the contents are usable.
	 * 
	 * @return <tt>true</tt> if they are. The method might inform the user of
	 *         any errors, but it throws no exceptions.
	 */
	public abstract boolean validateInput();

	/**
	 * Create a new table based on the new values from the fields displayed.
	 * 
	 * @param name
	 *            the name to give the table.
	 * @return the new table.
	 * @throws PartitionException
	 *             if it cannot do it.
	 */
	public abstract PartitionTable create(final String name)
			throws PartitionException;

	/**
	 * This panel type represents the modifications possible to a
	 * {@link SelectFromTable} table which gets data from a relational table.
	 */
	public static class SelectFromModifierPanel extends
			PartitionTableModifierPanel {
		private static final long serialVersionUID = 1;

		/**
		 * Construct a new modification panel.
		 * 
		 * @param table
		 *            the table we are displaying.
		 */
		public SelectFromModifierPanel(final SelectFromTable table) {
			super(table);
		}

		public String getType() {
			return Resources.get("selectFromPartitionTableType");
		}

		public boolean isMutable() {
			return false;
		}

		public void reset() {
			// TODO
		}

		public void update() throws PartitionException {
			// TODO
		}

		public boolean validateInput() {
			// TODO
			return true;
		}

		public PartitionTable create(final String name)
				throws PartitionException {
			// TODO Create a new one.
			return null;
		}
	}
}
