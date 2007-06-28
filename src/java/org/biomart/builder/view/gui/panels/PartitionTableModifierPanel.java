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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.common.model.Column;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.model.Table;
import org.biomart.common.model.PartitionTable.AbstractPartitionTable.SelectPartitionTable;
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
	 * {@link SelectPartitionTable} table which gets data from a relational
	 * table.
	 */
	public static class SelectFromModifierPanel extends
			PartitionTableModifierPanel {
		private static final long serialVersionUID = 1;

		private JComboBox tableChoice;

		private JList columnChoices;
		
		private JCheckBox distinct;

		/**
		 * Construct a new modification panel.
		 * 
		 * @param table
		 *            the table we are displaying.
		 * @param allTables
		 *            the tables to allow choices from.
		 */
		public SelectFromModifierPanel(final SelectPartitionTable table,
				final Collection allTables) {
			super(table);

			// First line is label and table selector.
			this.add(new JLabel(Resources.get("selectFromInitialTableLabel")), this.labelConstraints);
			this.tableChoice = new JComboBox(allTables.toArray());
			this.add(this.tableChoice, this.fieldConstraints);
			// Second line is label and column multiselect.
			this.add(new JLabel(Resources.get("selectFromInitialColsLabel")),
					this.labelConstraints);
			final DefaultListModel columns = new DefaultListModel();
			this.columnChoices = new JList(columns);
			this.columnChoices
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			this.add(new JScrollPane(this.columnChoices), this.fieldConstraints);
			// Listener on table selector changes columns in second line.
			this.tableChoice.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					columns.clear();
					final Object obj = SelectFromModifierPanel.this.tableChoice
							.getSelectedItem();
					if (obj != null)
						for (final Iterator i = ((Table) obj).getColumns()
								.iterator(); i.hasNext();)
							columns.addElement(i.next());
				}
			});
			// Third line is distinct checkbox.
			this.add(new JLabel(), this.labelLastRowConstraints);
			this.distinct = new JCheckBox(Resources.get("selectFromDistinctLabel"));
			this.add(this.distinct, this.fieldLastRowConstraints);
		}

		public String getType() {
			return Resources.get("selectFromPartitionTableType");
		}

		public boolean isMutable() {
			return true;
		}

		public void reset() {
			if (this.getTable() == null) {
				this.tableChoice.setSelectedIndex(0);
			} else {
				this.distinct.setSelected(((SelectPartitionTable) this
						.getTable()).isDistinct());
				this.tableChoice.setSelectedItem(((SelectPartitionTable) this
						.getTable()).getTable());
				final int indexes[] = new int[((SelectPartitionTable) this
						.getTable()).getInitialSelectColumns().size()];
				int pos = 0;
				for (int i = 0; i < this.columnChoices.getModel().getSize(); i++) {
					final Column col = (Column) this.columnChoices.getModel()
							.getElementAt(i);
					if (((SelectPartitionTable) this.getTable())
							.getInitialSelectColumns().contains(col))
						indexes[pos++] = i;
				}
				this.columnChoices.setSelectedIndices(indexes);
			}
		}

		public void update() throws PartitionException {
			final SelectPartitionTable pt = (SelectPartitionTable) this
					.getTable();
			pt.setTable((Table) this.tableChoice.getSelectedItem());
			pt.setInitialSelectColumns(Arrays.asList(this.columnChoices
					.getSelectedValues()));
			pt.setDistinct(this.distinct.isSelected());
		}

		public boolean validateInput() {
			// A placeholder to hold the validation messages, if any.
			final List messages = new ArrayList();

			// Check columns are present.
			if (this.columnChoices.getSelectedValues().length < 1)
				messages.add(Resources.get("initialSelectColumnsEmpty"));

			// If there any messages, display them.
			if (!messages.isEmpty())
				JOptionPane.showMessageDialog(null, messages
						.toArray(new String[0]), Resources
						.get("validationTitle"),
						JOptionPane.INFORMATION_MESSAGE);

			// Validation succeeds if there are no messages.
			return messages.isEmpty();
		}

		public PartitionTable create(final String name)
				throws PartitionException {
			final SelectPartitionTable pt = new SelectPartitionTable(name);
			pt.setTable((Table) this.tableChoice.getSelectedItem());
			pt.setInitialSelectColumns(Arrays.asList(this.columnChoices
					.getSelectedValues()));
			pt.setDistinct(this.distinct.isSelected());
			return pt;
		}
	}
}
