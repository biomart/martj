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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.biomart.common.model.Column.GenericColumn;

/**
 * This dialog asks users what kind of partitioning they want to set up on a
 * column. According to the type they select, it asks other questions, such as
 * what values to use.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public abstract class TwoColumnTablePanel extends JPanel {
	private static final long serialVersionUID = 1;

	private TwoColumnTableModel tableModel;

	private JButton insert;

	private JButton remove;

	/**
	 * Pop up a dialog asking how to partition a table.
	 * 
	 * @param values
	 *            initial values to display in the two columns of the table.
	 */
	public TwoColumnTablePanel(final Map values, final Collection firstColValues, final Collection secondColValues) {
		// Creates the basic dialog.
		super();

		// Create the content pane to store the create dialog panel.
		final GridBagLayout gridBag = new GridBagLayout();
		this.setLayout(gridBag);

		// Create constraints for labels that are not in the last row.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		this.tableModel = new TwoColumnTableModel(values, this
				.getFirstColumnHeader(), this.getSecondColumnHeader(), this
				.getFirstColumnType(), this.getSecondColumnType());
		final JTable table = new JTable(this.tableModel);
		table.setGridColor(Color.LIGHT_GRAY); // Mac OSX.
		// First column.
		final JComboBox firstEd = this.getFirstColumnEditor(firstColValues);
		if (firstEd != null) {
			table.getColumnModel().getColumn(0).setCellEditor(
					new DefaultCellEditor(firstEd));
			table.getColumnModel().getColumn(0).setPreferredWidth(
					firstEd.getPreferredSize().width);
		} else
			table.getColumnModel().getColumn(0).setPreferredWidth(
					table.getTableHeader().getDefaultRenderer()
							.getTableCellRendererComponent(
									null,
									table.getColumnModel().getColumn(0)
											.getHeaderValue(), false, false, 0,
									0).getPreferredSize().width);
		final TableCellRenderer firstRend = this.getFirstColumnRenderer();
		if (firstRend != null)
			table.getColumnModel().getColumn(0).setCellRenderer(firstRend);
		// Second column.
		final JComboBox secondEd = this.getSecondColumnEditor(secondColValues);
		if (secondEd != null) {
			table.getColumnModel().getColumn(1).setCellEditor(
					new DefaultCellEditor(secondEd));
			table.getColumnModel().getColumn(1).setPreferredWidth(
					secondEd.getPreferredSize().width);
		} else
			table.getColumnModel().getColumn(1).setPreferredWidth(
					table.getTableHeader().getDefaultRenderer()
							.getTableCellRendererComponent(
									null,
									table.getColumnModel().getColumn(1)
											.getHeaderValue(), false, false, 0,
									0).getPreferredSize().width);
		final TableCellRenderer secondRend = this.getSecondColumnRenderer();
		if (secondRend != null)
			table.getColumnModel().getColumn(1).setCellRenderer(secondRend);
		// Buttons.
		table.setPreferredScrollableViewportSize(new Dimension(table.getColumnModel().getColumn(0).getPreferredWidth() + table.getColumnModel().getColumn(1).getPreferredWidth(), 100));
		this.insert = new JButton(this.getInsertButtonText());
		this.remove = new JButton(this.getRemoveButtonText());
		this.insert.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				TwoColumnTablePanel.this.tableModel
						.insertRow(TwoColumnTablePanel.this.tableModel
								.getRowCount(),
								new Object[] {
										TwoColumnTablePanel.this
												.getNewRowFirstColumn(),
										TwoColumnTablePanel.this
												.getNewRowSecondColumn() });
			}
		});
		this.remove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final int rows[] = table.getSelectedRows();
				// Reverse order, so we don't end up with changing
				// indices along the way.
				for (int i = rows.length - 1; i >= 0; i--)
					TwoColumnTablePanel.this.tableModel.removeRow(rows[i]);
			}
		});
		JPanel field = new JPanel();
		field.add(new JScrollPane(table));
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);
		field = new JPanel();
		field.add(this.insert);
		field.add(this.remove);
		gridBag.setConstraints(field, fieldConstraints);
		this.add(field);
	}

	public abstract String getInsertButtonText();

	public abstract String getRemoveButtonText();

	public abstract String getFirstColumnHeader();

	public abstract String getSecondColumnHeader();

	public abstract Class getFirstColumnType();

	public abstract Class getSecondColumnType();

	public abstract Object getNewRowFirstColumn();

	public abstract Object getNewRowSecondColumn();

	public abstract JComboBox getFirstColumnEditor(final Collection values);

	public abstract JComboBox getSecondColumnEditor(final Collection values);

	public abstract TableCellRenderer getFirstColumnRenderer();

	public abstract TableCellRenderer getSecondColumnRenderer();

	public Map getValues() {
		return this.tableModel.getValues();
	}

	/**
	 * This internal class represents a map of dataset columns to aliases.
	 */
	private static class TwoColumnTableModel extends DefaultTableModel {
		private final Class[] colClasses;

		private static final long serialVersionUID = 1;

		/**
		 * Construct a model of aliases for the given table, and copy any
		 * existing aliases from the given template.
		 * 
		 * @param template
		 *            the existing alises to copy.
		 */
		public TwoColumnTableModel(final Map values,
				final String firstColHeader, final String secondColHeader,
				final Class firstColType, final Class secondColType) {
			super(new Object[] { firstColHeader, secondColHeader }, 0);
			this.colClasses = new Class[] { firstColType, secondColType };
			// Populate columns, and aliases from template.
			if (values != null)
				for (final Iterator i = values.entrySet().iterator(); i
						.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					this.insertRow(this.getRowCount(), new Object[] {
							entry.getKey(), entry.getValue() });
				}
		}

		public Map getValues() {
			// Return the map of column to alias.
			final HashMap aliases = new HashMap();
			for (int i = 0; i < this.getRowCount(); i++) {
				final Object alias = this.getValueAt(i, 0);
				final Object expr = this.getValueAt(i, 1);
				if (alias != null && alias.toString().trim().length() > 0
						&& expr != null && expr.toString().trim().length() > 0)
					aliases.put(alias, expr);
			}
			return aliases;
		}

		public Class getColumnClass(final int column) {
			return this.colClasses[column];
		}
	}

	public abstract static class StringStringTablePanel extends
			TwoColumnTablePanel {
		protected StringStringTablePanel(final Map values, final Collection firstColValues, final Collection secondColValues) {
			super(values, firstColValues, secondColValues);
		}
		
		public StringStringTablePanel(final Map values) {
			this(values, null, null);
		}

		public Class getFirstColumnType() {
			return String.class;
		}

		public Class getSecondColumnType() {
			return String.class;
		}

		public Object getNewRowFirstColumn() {
			return "";
		}

		public Object getNewRowSecondColumn() {
			return "";
		}

		public JComboBox getFirstColumnEditor(final Collection values) {
			return null;
		}

		public JComboBox getSecondColumnEditor(final Collection values) {
			return null;
		}

		public TableCellRenderer getFirstColumnRenderer() {
			return null;
		}

		public TableCellRenderer getSecondColumnRenderer() {
			return null;
		}
	}

	public abstract static class ColumnStringTablePanel extends
			StringStringTablePanel {
		private JComboBox editor;
		
		public ColumnStringTablePanel(final Map values, final Collection cols) {
			super(values, cols, null);
		}

		protected Collection getSortedColumns(final Collection columns) {
			final List cols = new ArrayList(columns);
			Collections.sort(cols);
			return cols;
		}
		
		protected JComboBox getFirstColumnEditor() {
			return this.editor;
		}

		public Class getFirstColumnType() {
			return GenericColumn.class;
		}

		public Object getNewRowFirstColumn() {
			return this.editor.getItemAt(0);
		}

		public JComboBox getFirstColumnEditor(final Collection values) {
			if (this.editor==null) {
				this.editor = new JComboBox();
				for (final Iterator i = this.getSortedColumns(values).iterator(); i
						.hasNext();)
					this.editor.addItem(i.next());
			}
			return this.editor;
		}
	}

	public abstract static class CRPairStringTablePanel extends
	StringStringTablePanel {
		private JComboBox editor;
		
		public CRPairStringTablePanel(final Map values, final Collection crPairs) {
			super(values, crPairs, null);
		}

		protected Collection getSortedColumns(final Collection crPairs) {
			final List cols = new ArrayList(crPairs);
			Collections.sort(cols, new Comparator() {
				public int compare(final Object a, final Object b) {
					final String first = ((Object[])a)[1].toString();
					final String second = ((Object[])b)[1].toString();
					final int result = first.compareTo(second);
					return result==0?-1:result;
				}
			});
			return cols;
		}
		
		protected JComboBox getFirstColumnEditor() {
			return this.editor;
		}

		public Class getFirstColumnType() {
			return Object[].class;
		}

		public Object getNewRowFirstColumn() {
			return this.editor.getItemAt(0);
		}

		public TableCellRenderer getFirstColumnRenderer() {
			return new TableCellRenderer() {
				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					final Object[] crPair = (Object[])value;
					final JLabel label = new JLabel();
					if (crPair[0]==null)
						label.setText(crPair[1].toString());
					else
						label.setText(crPair[1].toString() + " ["+crPair[0].toString()+"]");
					label.setOpaque(true);
					label.setFont(table.getFont());
					if (isSelected) {
						label.setBackground(table.getSelectionBackground());
						label.setForeground(table.getSelectionForeground());
					} else {
						label.setBackground(table.getBackground());
						label.setForeground(table.getForeground());
					}
					return label;
				}
			};
		}

		public JComboBox getFirstColumnEditor(final Collection values) {
			if (this.editor==null) {
				this.editor = new JComboBox();
				for (final Iterator i = this.getSortedColumns(values).iterator(); i
						.hasNext();)
					this.editor.addItem(i.next());
				this.editor.setRenderer(new ListCellRenderer() {
					public Component getListCellRendererComponent(final JList list,
							final Object value, final int index,
							final boolean isSelected, final boolean cellHasFocus) {
						final Object[] crPair = (Object[])value;
						final JLabel label = new JLabel();
						if (crPair[0]==null)
							label.setText(crPair[1].toString());
						else
							label.setText(crPair[1].toString() + " ["+crPair[0].toString()+"]");
						label.setOpaque(true);
						label.setFont(list.getFont());
						if (isSelected) {
							label.setBackground(list.getSelectionBackground());
							label.setForeground(list.getSelectionForeground());
						} else {
							label.setBackground(list.getBackground());
							label.setForeground(list.getForeground());
						}
						return label;
					}
				});
			}
			return this.editor;
		}
	}
}
