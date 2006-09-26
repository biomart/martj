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

package org.biomart.builder.view.gui.diagrams;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.ColumnComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * Displays a transformation step, depending on what is passed to the
 * constructor. The results is always a diagram containing only those components
 * which are involved in the current transformation.
 * <p>
 * Note how sub-diagrams do not have contexts, in order to prevent user
 * interaction with them.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public abstract class ExplainTransformationDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	/**
	 * Creates an empty diagram.
	 * 
	 * @param martTab
	 *            the tabset to communicate with via menus.
	 */
	protected ExplainTransformationDiagram(MartTab martTab) {
		super(martTab);
	}

	protected void updateAppearance() {
		// Set the background.
		this.setBackground(ExplainTransformationDiagram.BACKGROUND_COLOUR);
	}

	/**
	 * Static reference to the background colour to use for components.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	/**
	 * This version of the class shows a bunch of columns.
	 */
	public static class Columns extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private Collection columns;

		/**
		 * Creates a diagram showing the given set of columns.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param columns
		 *            the columns to explain.
		 */
		public Columns(final MartTab martTab, final Collection columns) {
			super(martTab);

			// Remember the columns, and calculate the diagram.
			this.columns = columns;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			for (final Iterator i = this.columns.iterator(); i.hasNext();) {
				final ColumnComponent columnComponent = new ColumnComponent(
						(Column) i.next(), this);
				this.addDiagramComponent(columnComponent);
			}
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * This version of the class shows a single table.
	 */
	public static class SingleTable extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private Table table;

		private Collection includeCols;

		/**
		 * Creates a diagram showing the given table.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param table
		 *            the table to show.
		 * @param includeCols
		 *            the columns to show in the table.
		 */
		public SingleTable(final MartTab martTab, final Table table,
				final Collection includeCols) {
			super(martTab);

			// Remember the table, and calculate the diagram.
			this.table = table;
			this.includeCols = includeCols;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Replicate the table in an empty schema then add the columns
			// requested.
			final Schema tempSourceSchema = new GenericSchema(this.table
					.getSchema().getName());
			final Table tempSource = new GenericTable(this.table.getName(),
					tempSourceSchema);
			for (Iterator i = this.includeCols.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			this.addDiagramComponent(new TableComponent(this.table, this));
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * This version of the class shows a temp table on the left and a real table
	 * on the right.
	 */
	public static class TempReal extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private Collection tempTableKeyColumns;

		private String tempTableSchemaName;

		private Key key;

		private Relation relation;

		private Collection lIncludeCols;

		private Collection rIncludeCols;

		/**
		 * Creates a diagram showing the given pair of tables and a relation
		 * between them.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param tempTableKeyColumns
		 *            the columns to use as a key in the displayed temp table.
		 * @param tempTableSchemaName
		 *            the name to use for the fake schema the temp table lives
		 *            in.
		 * @param key
		 *            the key to explain the relation from.
		 * @param relation
		 *            the relation to explain.
		 * @param lIncludeCols
		 *            the columns to show in the LHS.
		 * @param rIncludeCols
		 *            the columns to show in the RHS.
		 */
		public TempReal(final MartTab martTab,
				final String tempTableSchemaName,
				final Collection tempTableKeyColumns, final Key key,
				final Relation relation, final Collection lIncludeCols,
				final Collection rIncludeCols) {
			super(martTab);

			// Remember the columns, and calculate the diagram.
			this.tempTableSchemaName = tempTableSchemaName;
			this.tempTableKeyColumns = tempTableKeyColumns;
			this.lIncludeCols = lIncludeCols;
			this.rIncludeCols = rIncludeCols;
			this.key = key;
			this.relation = relation;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Create a temp table called TEMP with the given columns
			// and given foreign key.
			final Schema tempSourceSchema = new GenericSchema(
					this.tempTableSchemaName);
			final Table tempSource = new GenericTable(Resources
					.get("dummyTempTableName"), tempSourceSchema);
			for (Iterator i = this.lIncludeCols.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			Key tempSourceKey;
			try {
				if (this.key instanceof ForeignKey) {
					tempSourceKey = new GenericForeignKey(new ArrayList(
							this.tempTableKeyColumns));
					tempSource.addForeignKey((ForeignKey) tempSourceKey);
				} else {
					tempSourceKey = new GenericPrimaryKey(new ArrayList(
							this.tempTableKeyColumns));
					tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
				}
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.relation.getOtherKey(this.key);
			final Table realTarget = realTargetKey.getTable();
			final Schema tempTargetSchema = new GenericSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new GenericTable(realTarget.getName(),
					tempTargetSchema);
			for (Iterator i = this.rIncludeCols.iterator(); i.hasNext();)
				tempTarget.addColumn((Column) i.next());
			Key tempTargetKey;
			try {
				if (realTargetKey instanceof ForeignKey) {
					tempTargetKey = new GenericForeignKey(realTargetKey
							.getColumns());
					tempTarget.addForeignKey((ForeignKey) tempTargetKey);
				} else {
					tempTargetKey = new GenericPrimaryKey(realTargetKey
							.getColumns());
					tempTarget.setPrimaryKey((PrimaryKey) tempTargetKey);
				}
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Create a copy of the relation but change to be between the
			// two temp keys.
			Relation tempRelation;
			try {
				tempRelation = new GenericRelation(tempSourceKey,
						tempTargetKey, this.relation.getCardinality());
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Add source and target tables.
			this.addDiagramComponent(new TableComponent(tempSource, this));
			this.addDiagramComponent(new TableComponent(tempTarget, this));
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.addDiagramComponent(relationComponent);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * This version of the class shows a dataset table on the left and a real
	 * table on the right.
	 */
	public static class DatasetReal extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private DataSetTable dsTable;

		private Collection dsTableKeyColumns;

		private Key key;

		private Relation relation;

		private Collection lIncludeCols;

		private Collection rIncludeCols;

		/**
		 * Creates a diagram showing the given pair of tables and a relation
		 * between them.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param dsTable
		 *            the dataset table to show on the left.
		 * @param dsTableKeyColumns
		 *            the columns to use as a key in the displayed dataset
		 *            table.
		 * @param key
		 *            the key to explain the relation from.
		 * @param relation
		 *            the relation to explain.
		 * @param lIncludeCols
		 *            the columns to show in the LHS.
		 * @param rIncludeCols
		 *            the columns to show in the RHS.
		 */
		public DatasetReal(final MartTab martTab, final DataSetTable dsTable,
				final Collection dsTableKeyColumns, final Key key,
				final Relation relation, final Collection lIncludeCols,
				final Collection rIncludeCols) {
			super(martTab);

			// Remember the columns, and calculate the diagram.
			this.dsTable = dsTable;
			this.dsTableKeyColumns = dsTableKeyColumns;
			this.key = key;
			this.relation = relation;
			this.lIncludeCols = lIncludeCols;
			this.rIncludeCols = rIncludeCols;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Create a fake dataset table with the given columns
			// and given foreign key.
			final Schema tempSourceSchema = new GenericSchema(this.dsTable
					.getSchema().getName());
			final Table tempSource = new GenericTable(this.dsTable.getName(),
					tempSourceSchema);
			for (Iterator i = this.lIncludeCols.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			Key tempSourceKey;
			try {
				if (this.key instanceof ForeignKey) {
					tempSourceKey = new GenericForeignKey(new ArrayList(
							this.dsTableKeyColumns));
					tempSource.addForeignKey((ForeignKey) tempSourceKey);
				} else {
					tempSourceKey = new GenericPrimaryKey(new ArrayList(
							this.dsTableKeyColumns));
					tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
				}
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.relation.getOtherKey(this.key);
			final Table realTarget = realTargetKey.getTable();
			final Schema tempTargetSchema = new GenericSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new GenericTable(realTarget.getName(),
					tempTargetSchema);
			for (Iterator i = this.rIncludeCols.iterator(); i.hasNext();)
				tempTarget.addColumn((Column) i.next());
			Key tempTargetKey;
			try {
				if (realTargetKey instanceof ForeignKey) {
					tempTargetKey = new GenericForeignKey(realTargetKey
							.getColumns());
					tempTarget.addForeignKey((ForeignKey) tempTargetKey);
				} else {
					tempTargetKey = new GenericPrimaryKey(realTargetKey
							.getColumns());
					tempTarget.setPrimaryKey((PrimaryKey) tempTargetKey);
				}
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Create a copy of the relation but change to be between the
			// two temp keys.
			Relation tempRelation;
			try {
				tempRelation = new GenericRelation(tempSourceKey,
						tempTargetKey, this.relation.getCardinality());
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Add source and target tables.
			this.addDiagramComponent(new TableComponent(tempSource, this));
			this.addDiagramComponent(new TableComponent(tempTarget, this));
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.addDiagramComponent(relationComponent);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * This version of the class shows two real tables.
	 */
	public static class RealReal extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private Key key;

		private Relation relation;

		private Collection lIncludeCols;

		private Collection rIncludeCols;

		/**
		 * Creates a diagram showing the given pair of tables and a relation
		 * between them.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param key
		 *            the key to explain the relation from.
		 * @param relation
		 *            the relation to explain.
		 * @param lIncludeCols
		 *            the columns to show in the LHS.
		 * @param rIncludeCols
		 *            the columns to show in the RHS.
		 */
		public RealReal(final MartTab martTab, final Key key,
				final Relation relation, final Collection lIncludeCols,
				final Collection rIncludeCols) {
			super(martTab);

			// Remember the columns, and calculate the diagram.
			this.key = key;
			this.relation = relation;
			this.lIncludeCols = lIncludeCols;
			this.rIncludeCols = rIncludeCols;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Create a fake dataset table with the given columns
			// and given foreign key.
			final Table realSource = this.key.getTable();
			final Schema tempSourceSchema = new GenericSchema(realSource
					.getSchema().getName());
			final Table tempSource = new GenericTable(realSource.getName(),
					tempSourceSchema);
			List tempKeyColumns = new ArrayList();
			for (Iterator i = this.lIncludeCols.iterator(); i.hasNext();) {
				final Column realColumn = (Column) i.next();
				tempSource.addColumn(realColumn);
				if (realColumn instanceof WrappedColumn
						&& this.key.getColumns()
								.contains(
										((WrappedColumn) realColumn)
												.getWrappedColumn()))
					tempKeyColumns.add(realColumn);
			}
			Key tempSourceKey;
			try {
				if (this.key instanceof ForeignKey) {
					tempSourceKey = new GenericForeignKey(tempKeyColumns);
					tempSource.addForeignKey((ForeignKey) tempSourceKey);
				} else {
					tempSourceKey = new GenericPrimaryKey(tempKeyColumns);
					tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
				}
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.relation.getOtherKey(this.key);
			final Table realTarget = realTargetKey.getTable();
			final Schema tempTargetSchema = new GenericSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new GenericTable(realTarget.getName(),
					tempTargetSchema);
			for (Iterator i = this.rIncludeCols.iterator(); i.hasNext();)
				tempTarget.addColumn((Column) i.next());
			Key tempTargetKey;
			try {
				if (realTargetKey instanceof ForeignKey) {
					tempTargetKey = new GenericForeignKey(realTargetKey
							.getColumns());
					tempTarget.addForeignKey((ForeignKey) tempTargetKey);
				} else {
					tempTargetKey = new GenericPrimaryKey(realTargetKey
							.getColumns());
					tempTarget.setPrimaryKey((PrimaryKey) tempTargetKey);
				}
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Create a copy of the relation but change to be between the
			// two temp keys.
			Relation tempRelation;
			try {
				tempRelation = new GenericRelation(tempSourceKey,
						tempTargetKey, this.relation.getCardinality());
			} catch (AssociationException e) {
				// Really should never happen.
				throw new MartBuilderInternalError(e);
			}

			// Add source and target tables.
			this.addDiagramComponent(new TableComponent(tempSource, this));
			this.addDiagramComponent(new TableComponent(tempTarget, this));
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.addDiagramComponent(relationComponent);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}
}
