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

import java.awt.Color;

import javax.swing.JComponent;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * The explain-dataset context highlights relations and tables that explain how
 * a particular table or column came to be in a dataset. If no specific column
 * or table was selected, it behaves identically to the window context. Menus
 * are the same in all cases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 20th June 2006
 * @since 0.1
 */
public class ExplainDataSetContext extends WindowContext {
	private DataSetColumn selectedColumn;

	private static final Color ENROUTE_COLOUR = Color.ORANGE;

	private static final Color TARGET_COLOUR = Color.MAGENTA;

	private static final Color FADED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Creates a new explanation diagram for a given dataset.
	 * 
	 * @param martTab 
	 *            the mart tab that will handle menu events.
	 * @param dataset
	 *            the dataset we are explaning.
	 */
	public ExplainDataSetContext(MartTab martTab, DataSet dataset) {
		super(martTab, dataset);
	}

	/**
	 * Asks the diagram to explain a specific column, rather than the whole
	 * dataset.
	 * 
	 * @param selectedColumn
	 *            the column to explain.
	 */
	public void setSelectedColumn(DataSetColumn selectedColumn) {
		this.selectedColumn = selectedColumn;
	}

	public void customiseAppearance(JComponent component, Object object) {
		// If no column is selected, behave just as a window context would do.
		if (this.selectedColumn == null)
			super.customiseAppearance(component, object);

		// Otherwise, treat relations, tables and columns specially.
		else {

			// Relations are highlighted if they are the underlying relation
			// of a table, or are the relation that caused a particular column
			// to be added to the dataset.
			if (object instanceof Relation) {
				// What relation is this?
				Relation relation = (Relation) object;

				// What is the underlying relation of the selected column?
				Relation underlyingRelation = this.selectedColumn
						.getUnderlyingRelation();

				// If it has an underlying relation, and that's the one we're
				// drawing...
				if (underlyingRelation != null
						&& underlyingRelation.equals(relation)) {
					// If it's concat-only, then the relation is the target.
					if (selectedColumn instanceof ConcatRelationColumn)
						component
								.setForeground(ExplainDataSetContext.TARGET_COLOUR);
					// Otherwise, the relation is en-route.
					else
						component
								.setForeground(ExplainDataSetContext.ENROUTE_COLOUR);
				}

				// If this relation is not the underlying relation, then fade
				// this relation out.
				else
					component.setForeground(ExplainDataSetContext.FADED_COLOUR);
			}

			// Tables are highlighted if they contain the selected column.
			else if (object instanceof Table) {

				// What table is this?
				Table table = (Table) object;

				// If the table contains the wrapped column of our selected
				// column,
				// then it is the target.
				if ((selectedColumn instanceof WrappedColumn)
						&& table.getColumns().contains(
								((WrappedColumn) selectedColumn)
										.getWrappedColumn()))
					component
							.setForeground(ExplainDataSetContext.TARGET_COLOUR);

				// If the table is the underlying table of the column, and this
				// is the schema name column, it is also the target.
				else if (selectedColumn instanceof SchemaNameColumn
						&& table.equals(((DataSetTable) this.selectedColumn
								.getTable()).getUnderlyingTable()))
					component
							.setForeground(ExplainDataSetContext.TARGET_COLOUR);

				// If the table is at the source end of the underlying relation
				// of this column, it is en-route.
				else if (selectedColumn.getUnderlyingRelation() != null
						&& table.getRelations().contains(
								selectedColumn.getUnderlyingRelation()))
					component
							.setForeground(ExplainDataSetContext.ENROUTE_COLOUR);

				// Otherwise, it is faded.
				else
					component.setForeground(ExplainDataSetContext.FADED_COLOUR);

			}

			// Columns are highlighted if they _are_ the selected column.
			else if (object instanceof Column) {

				// What column is this?
				Column column = (Column) object;

				// If the selected column is a wrapped column, and the column it
				// wraps is this one, highlight it.
				if ((selectedColumn instanceof WrappedColumn)
						&& column.equals(((WrappedColumn) selectedColumn)
								.getWrappedColumn()))
					component
							.setForeground(ExplainDataSetContext.TARGET_COLOUR);

				// Otherwise, fade it.
				else
					component.setForeground(ExplainDataSetContext.FADED_COLOUR);
			}
		}
	}
}
