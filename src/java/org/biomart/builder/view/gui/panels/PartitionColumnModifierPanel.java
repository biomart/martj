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

package org.biomart.builder.view.gui.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.model.PartitionTable.PartitionColumn;
import org.biomart.common.model.PartitionTable.PartitionColumn.FixedColumn;
import org.biomart.common.model.PartitionTable.PartitionColumn.RegexColumn;
import org.biomart.common.resources.Resources;

/**
 * Knows how to modify partition-column-specific stuff.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public abstract class PartitionColumnModifierPanel extends JPanel {

	private final PartitionColumn column;

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
	 *            the table to attach to.
	 * @param column
	 *            the partition column to represent. If <tt>null</tt> then it
	 *            should start with a blank sheet, return <tt>false</tt> for
	 *            {@link #isMutable()}, and return something meaningful from
	 *            {@link #create(String)}.
	 */
	public PartitionColumnModifierPanel(final PartitionTable table,
			final PartitionColumn column) {
		super(new GridBagLayout());
		this.column = column;
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
	 * Obtain the partition column we are working with.
	 * 
	 * @return the column.
	 */
	protected PartitionColumn getColumn() {
		return this.column;
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
	 * If this column is changeable (update/reset are valid) then this should
	 * return <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if it is changeable.
	 */
	public abstract boolean isMutable();

	/**
	 * Reset the fields to the values specified by the original column passed
	 * in.
	 */
	public abstract void reset();

	/**
	 * Update the original column passed in to adopt the new values from the
	 * fields displayed.
	 * 
	 * @throws PartitionException
	 *             if it cannot do it.
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
	 * Create a new column based on the new values from the fields displayed,
	 * which belongs to the given table.
	 * 
	 * @param name
	 *            the name to give the column.
	 * @return the new column.
	 * @throws PartitionException
	 *             if it cannot do it.
	 */
	public abstract PartitionColumn create(final String name)
			throws PartitionException;

	/**
	 * A panel for modifiying fixed columns.
	 */
	public static class FixedColumnModifierPanel extends
			PartitionColumnModifierPanel {

		private static final long serialVersionUID = 1L;

		/**
		 * Construct a new panel.
		 * 
		 * @param table
		 *            the parent table.
		 * @param column
		 *            the partition column to represent.
		 */
		public FixedColumnModifierPanel(final PartitionTable table,
				final FixedColumn column) {
			super(table, column);
			this.add(new JLabel(Resources.get("fixedColumnNoChangeLabel")),
					this.fieldLastRowConstraints);
		}

		public boolean isMutable() {
			return false;
		}

		public void reset() {
			// Nothing to do.
		}

		public void update() throws PartitionException {
			// Nothing to do.
		}

		public boolean validateInput() {
			// Nothing to do.
			return true;
		}

		public PartitionColumn create(final String name)
				throws PartitionException {
			// Don't even think about it.
			throw new BioMartError();
		}
	}

	/**
	 * A panel for modifiying regex columns.
	 */
	public static class RegexColumnModifierPanel extends
			PartitionColumnModifierPanel {

		private static final long serialVersionUID = 1L;

		private JComboBox sourceColumn;

		private JTextField matchRegex;

		private JTextField replaceRegex;

		/**
		 * Construct a new panel.
		 * 
		 * @param table
		 *            the parent table for this column.
		 * @param column
		 *            the partition column to represent.
		 */
		public RegexColumnModifierPanel(final PartitionTable table,
				final RegexColumn column) {
			super(table, column);

			// First line = source column.
			this.add(new JLabel(Resources.get("regexColumnSourceColumnLabel")),
					this.labelConstraints);
			this.sourceColumn = new JComboBox();
			for (final Iterator i = table.getColumnNames().iterator(); i
					.hasNext();)
				this.sourceColumn.addItem((String) i.next());
			this.add(this.sourceColumn, this.fieldConstraints);
			// Second line = matching regex.
			this.add(new JLabel(Resources.get("regexColumnMatchRegexLabel")),
					this.labelConstraints);
			this.matchRegex = new JTextField(30);
			this.add(this.matchRegex, this.fieldConstraints);
			// Third line = replacing regex.
			this.add(new JLabel(Resources.get("regexColumnReplaceRegexLabel")),
					this.labelLastRowConstraints);
			this.replaceRegex = new JTextField(30);
			this.add(this.replaceRegex, this.fieldLastRowConstraints);
		}

		public boolean isMutable() {
			return true;
		}

		public void reset() {
			if (this.getColumn() == null) {
				this.sourceColumn.setSelectedIndex(0);
				this.matchRegex.setText(".*");
				this.replaceRegex.setText("$1");
			} else {
				final RegexColumn rcol = (RegexColumn) this.getColumn();
				this.sourceColumn.setSelectedItem(rcol.getSourceColumn());
				this.matchRegex.setText(rcol.getRegexMatch());
				this.replaceRegex.setText(rcol.getRegexReplace());
			}
		}

		public void update() throws PartitionException {
			final RegexColumn rcol = (RegexColumn) this.getColumn();
			rcol.setSourceColumn((String) this.sourceColumn.getSelectedItem());
			rcol.setRegexMatch(this.matchRegex.getText().trim());
			rcol.setRegexReplace(this.replaceRegex.getText().trim());
		}

		public boolean validateInput() {
			// A placeholder to hold the validation messages, if any.
			final List messages = new ArrayList();

			// Check name is present.
			if (this.isEmpty(this.matchRegex.getText()))
				messages.add(Resources.get("regexColumnMatchRegexEmpty"));

			// Check name is present.
			if (this.isEmpty(this.replaceRegex.getText()))
				messages.add(Resources.get("regexColumnReplaceRegexEmpty"));

			// If there any messages, display them.
			if (!messages.isEmpty())
				JOptionPane.showMessageDialog(null, messages
						.toArray(new String[0]), Resources
						.get("validationTitle"),
						JOptionPane.INFORMATION_MESSAGE);

			// Validation succeeds if there are no messages.
			return messages.isEmpty();
		}

		private boolean isEmpty(final String string) {
			// Strings are empty if they are null or all whitespace.
			return string == null || string.trim().length() == 0;
		}

		public PartitionColumn create(final String name)
				throws PartitionException {
			final RegexColumn rcol = new RegexColumn(this.getTable(), name);
			rcol.setSourceColumn((String) this.sourceColumn.getSelectedItem());
			rcol.setRegexMatch(this.matchRegex.getText().trim());
			rcol.setRegexReplace(this.replaceRegex.getText().trim());
			return rcol;
		}
	}
}
