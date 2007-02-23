/**
 * 
 */
package org.biomart.builder.view.gui.panels;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel;
import org.biomart.common.view.gui.panels.TwoColumnTablePanel.ColumnStringTablePanel;

public abstract class DataSetColumnStringTablePanel extends
		ColumnStringTablePanel {
	private final DataSetColumn dontIncludeThis;

	public DataSetColumnStringTablePanel(final Map values,
			final Collection columns, final DataSetColumn dontIncludeThis) {
		super(values, columns);
		this.getFirstColumnEditor().setRenderer(new ListCellRenderer() {
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final DataSetColumn col = (DataSetColumn) value;
				final JLabel label = new JLabel();
				if (col != null)
					label.setText(col.getModifiedName());
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
		this.dontIncludeThis = dontIncludeThis;
	}

	public Collection getSortedColumns(final Collection columns) {
		final Map sortedCols = new TreeMap();
		for (final Iterator i = columns.iterator(); i.hasNext();) {
			final DataSetColumn col = (DataSetColumn) i.next();
			if (this.dontIncludeThis == null
					|| !col.equals(this.dontIncludeThis))
				sortedCols.put(col.getModifiedName(), col);
		}
		return sortedCols.values();
	}

	public Class getFirstColumnType() {
		return DataSetColumn.class;
	}

	public TableCellRenderer getFirstColumnRenderer() {
		return new TableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				final DataSetColumn col = (DataSetColumn) value;
				final JLabel label = new JLabel();
				if (col != null)
					label.setText(col.getModifiedName());
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

	public Map getValues() {
		final Map values = new HashMap();
		for (final Iterator i = super.getValues().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			values.put(((DataSetColumn) entry.getKey()).getModifiedName(),
					entry.getValue());
		}
		return values;
	}
}