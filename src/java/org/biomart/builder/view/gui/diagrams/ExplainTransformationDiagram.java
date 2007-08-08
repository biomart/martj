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

import org.biomart.builder.model.TransformationUnit;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.builder.model.TransformationUnit.SkipTable;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.SchemaLayoutManager.SchemaLayoutConstraint;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.builder.view.gui.diagrams.contexts.ExplainContext;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.GenericForeignKey;
import org.biomart.common.model.Key.GenericPrimaryKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.model.Relation.GenericRelation;
import org.biomart.common.model.Schema.GenericSchema;
import org.biomart.common.model.Table.GenericTable;
import org.biomart.common.resources.Resources;

/**
 * Displays a transformation step, depending on what is passed to the
 * constructor. The results is always a diagram containing only those components
 * which are involved in the current transformation.
 * <p>
 * Note how diagrams do not have contexts, in order to prevent user interaction
 * with them.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public abstract class ExplainTransformationDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private final List tableComponents = new ArrayList();

	private final int step;

	private final ExplainContext explainContext;

	/**
	 * Creates an empty diagram, using the single-parameter constructor from
	 * {@link Diagram}.
	 * 
	 * @param martTab
	 *            the tabset to communicate with when (if) context menus are
	 *            selected.
	 * @param step
	 *            the step of the transformation this diagram represents.
	 * @param explainContext
	 *            the context used to provide the relation contexts, which are
	 *            the same as those that appear in the explain diagram in the
	 *            other tab to the transform view.
	 */
	protected ExplainTransformationDiagram(final MartTab martTab,
			final int step, final ExplainContext explainContext) {
		super(new SchemaLayoutManager(), martTab);
		this.step = step;
		this.explainContext = explainContext;
	}

	/**
	 * Get which step this diagram is representing.
	 * 
	 * @return the step of the transformation.
	 */
	protected int getStep() {
		return this.step;
	}

	/**
	 * Find out what table components we have.
	 * 
	 * @return the list of components.
	 */
	public Collection getTableComponents() {
		return this.tableComponents;
	}

	/**
	 * Get the explain context which appears in the other tab, which is to be
	 * used for providing contexts for relations in this diagram.
	 * 
	 * @return the context.
	 */
	public ExplainContext getExplainContext() {
		return this.explainContext;
	}

	/**
	 * This version of the class shows a single table.
	 */
	public static class SingleTable extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private final SelectFromTable stu;

		/**
		 * Creates a diagram showing the given table.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param stu
		 *            the transformation unit to show.
		 * @param step
		 *            the step of the transformation this diagram represents.
		 * @param explainContext
		 *            the context used to provide the relation contexts, which
		 *            are the same as those that appear in the explain diagram
		 *            in the other tab to the transform view.
		 */
		public SingleTable(final MartTab martTab, final SelectFromTable stu,
				final int step, final ExplainContext explainContext) {
			super(martTab, step, explainContext);

			// Remember the params, and calculate the diagram.
			this.stu = stu;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Replicate the table in an empty schema then add the columns
			// requested.
			final FakeSchema tempSourceSchema = new FakeSchema(this.stu
					.getTable().getSchema().getName());
			final Table tempSource = new RealisedTable(this.stu.getTable()
					.getName(), tempSourceSchema, this.stu.getTable(), this
					.getExplainContext());
			tempSourceSchema.addTable(tempSource);
			for (final Iterator i = this.stu.getNewColumnNameMap().values()
					.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			final TableComponent tc = new TableComponent(tempSource, this);
			this.add(tc, new SchemaLayoutConstraint(0), Diagram.TABLE_LAYER);
			this.getTableComponents().add(tc);
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

		private final JoinTable ltu;

		private final Collection lIncludeCols;

		/**
		 * Creates a diagram showing the given pair of tables and a relation
		 * between them.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param ltu
		 *            the transformation to explain.
		 * @param lIncludeCols
		 *            the columns to show in the temp table.
		 * @param step
		 *            the step of the transformation this diagram represents.
		 * @param explainContext
		 *            the context used to provide the relation contexts, which
		 *            are the same as those that appear in the explain diagram
		 *            in the other tab to the transform view.
		 */
		public TempReal(final MartTab martTab, final JoinTable ltu,
				final List lIncludeCols, final int step,
				final ExplainContext explainContext) {
			super(martTab, step, explainContext);

			// Remember the columns, and calculate the diagram.
			this.ltu = ltu;
			this.lIncludeCols = new ArrayList(lIncludeCols);
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Create a temp table called TEMP with the given columns
			// and given foreign key.
			final FakeSchema tempSourceSchema = new FakeSchema(Resources
					.get("dummyTempSchemaName"));
			final Table tempSource = new FakeTable(Resources
					.get("dummyTempTableName")
					+ " " + this.getStep(), tempSourceSchema);
			tempSourceSchema.addTable(tempSource);
			for (final Iterator i = this.lIncludeCols.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			Key tempSourceKey;
			if (this.ltu.getSchemaSourceKey() instanceof ForeignKey) {
				tempSourceKey = new GenericForeignKey(this.ltu
						.getSourceDataSetColumns());
				try {
					tempSource.addForeignKey((ForeignKey) tempSourceKey);
				} catch (final AssociationException e) {
					// Really should never happen.
					throw new BioMartError(e);
				}
			} else {
				tempSourceKey = new GenericPrimaryKey(this.ltu
						.getSourceDataSetColumns());
				tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.ltu.getSchemaRelation().getOtherKey(
					this.ltu.getSchemaSourceKey());
			final Table realTarget = realTargetKey.getTable();
			final FakeSchema tempTargetSchema = new FakeSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new RealisedTable(realTarget.getName(),
					tempTargetSchema, realTarget, this.getExplainContext());
			tempTargetSchema.addTable(tempTarget);
			for (final Iterator i = this.ltu.getNewColumnNameMap().values()
					.iterator(); i.hasNext();)
				tempTarget.addColumn((Column) i.next());
			Key tempTargetKey;
			if (realTargetKey instanceof ForeignKey) {
				tempTargetKey = new GenericForeignKey(realTargetKey
						.getColumns());
				try {
					tempTarget.addForeignKey((ForeignKey) tempTargetKey);
				} catch (final AssociationException e) {
					// Really should never happen.
					throw new BioMartError(e);
				}
			} else {
				tempTargetKey = new GenericPrimaryKey(realTargetKey
						.getColumns());
				tempTarget.setPrimaryKey((PrimaryKey) tempTargetKey);
			}

			// Create a copy of the relation but change to be between the
			// two fake keys.
			Relation tempRelation;
			try {
				tempRelation = new RealisedRelation(tempSourceKey,
						tempTargetKey, this.ltu.getSchemaRelation()
								.getCardinality(),
						this.ltu.getSchemaRelation(), this.ltu
								.getSchemaRelationIteration(), this
								.getExplainContext());
				// DON'T add to keys else it causes trouble with
				// the caching system!
			} catch (final AssociationException e) {
				// Really should never happen.
				throw new BioMartError(e);
			}

			// Add source and target tables.
			final TableComponent tc1 = new TableComponent(tempSource, this);
			this.add(tc1, new SchemaLayoutConstraint(1), Diagram.TABLE_LAYER);
			this.getTableComponents().add(tc1);
			final TableComponent tc2 = new TableComponent(tempTarget, this);
			this.add(tc2, new SchemaLayoutConstraint(1), Diagram.TABLE_LAYER);
			this.getTableComponents().add(tc2);
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.add(relationComponent, new SchemaLayoutConstraint(0),
					Diagram.RELATION_LAYER);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * This version of the class shows a temp table on the left and a real table
	 * on the right.
	 */
	public static class SkipTempReal extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private final SkipTable ltu;

		private final Collection lIncludeCols;

		/**
		 * The background color for this kind of diagram.
		 */
		public static final Color BG_COLOR = new Color(1.0f, 1.0f, 0.8f, 1.0f);

		/**
		 * Creates a diagram showing the given pair of tables and a relation
		 * between them. This is a faded-out 'fake' relation.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param ltu
		 *            the transformation to explain.
		 * @param lIncludeCols
		 *            the columns to show in the temp table.
		 * @param step
		 *            the step of the transformation this diagram represents.
		 * @param explainContext
		 *            the context used to provide the relation contexts, which
		 *            are the same as those that appear in the explain diagram
		 *            in the other tab to the transform view.
		 */
		public SkipTempReal(final MartTab martTab, final SkipTable ltu,
				final List lIncludeCols, final int step,
				final ExplainContext explainContext) {
			super(martTab, step, explainContext);

			this.setBackground(SkipTempReal.BG_COLOR);

			// Remember the columns, and calculate the diagram.
			this.ltu = ltu;
			this.lIncludeCols = new ArrayList(lIncludeCols);
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Create a temp table called TEMP with the given columns
			// and given foreign key.
			final FakeSchema tempSourceSchema = new FakeSchema(Resources
					.get("dummyTempSchemaName"));
			final Table tempSource = new FakeTable(Resources
					.get("dummyTempTableName")
					+ " " + this.getStep(), tempSourceSchema);
			tempSourceSchema.addTable(tempSource);
			for (final Iterator i = this.lIncludeCols.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			Key tempSourceKey;
			if (this.ltu.getSchemaSourceKey() instanceof ForeignKey) {
				tempSourceKey = new GenericForeignKey(this.ltu
						.getSourceDataSetColumns());
				try {
					tempSource.addForeignKey((ForeignKey) tempSourceKey);
				} catch (final AssociationException e) {
					// Really should never happen.
					throw new BioMartError(e);
				}
			} else {
				tempSourceKey = new GenericPrimaryKey(this.ltu
						.getSourceDataSetColumns());
				tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.ltu.getSchemaRelation().getOtherKey(
					this.ltu.getSchemaSourceKey());
			final Table realTarget = realTargetKey.getTable();
			final FakeSchema tempTargetSchema = new FakeSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new RealisedTable(realTarget.getName(),
					tempTargetSchema, realTarget, this.getExplainContext());
			tempTargetSchema.addTable(tempTarget);
			// Target table has no columns.
			Key tempTargetKey;
			if (realTargetKey instanceof ForeignKey) {
				tempTargetKey = new GenericForeignKey(realTargetKey
						.getColumns());
				try {
					tempTarget.addForeignKey((ForeignKey) tempTargetKey);
				} catch (final AssociationException e) {
					// Really should never happen.
					throw new BioMartError(e);
				}
			} else {
				tempTargetKey = new GenericPrimaryKey(realTargetKey
						.getColumns());
				tempTarget.setPrimaryKey((PrimaryKey) tempTargetKey);
			}

			// Create a copy of the relation but change to be between the
			// two fake keys.
			Relation tempRelation;
			try {
				tempRelation = new RealisedRelation(tempSourceKey,
						tempTargetKey, this.ltu.getSchemaRelation()
								.getCardinality(),
						this.ltu.getSchemaRelation(), this.ltu
								.getSchemaRelationIteration(), this
								.getExplainContext());
				// DON'T add to keys else it causes trouble with
				// the caching system!
			} catch (final AssociationException e) {
				// Really should never happen.
				throw new BioMartError(e);
			}

			// Add source and target tables.
			final TableComponent tc1 = new TableComponent(tempSource, this);
			this.add(tc1, new SchemaLayoutConstraint(1), Diagram.TABLE_LAYER);
			this.getTableComponents().add(tc1);
			final TableComponent tc2 = new TableComponent(tempTarget, this);
			this.add(tc2, new SchemaLayoutConstraint(1), Diagram.TABLE_LAYER);
			this.getTableComponents().add(tc2);
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.add(relationComponent, new SchemaLayoutConstraint(0),
					Diagram.RELATION_LAYER);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * This version of the class shows a bunch of additional columns added in
	 * the last transformation steps.
	 */
	public static class AdditionalColumns extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private final TransformationUnit etu;

		/**
		 * Creates a diagram showing the given table.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param etu
		 *            the transformation unit to show.
		 * @param step
		 *            the step of the transformation this diagram represents.
		 * @param explainContext
		 *            the context used to provide the relation contexts, which
		 *            are the same as those that appear in the explain diagram
		 *            in the other tab to the transform view.
		 */
		public AdditionalColumns(final MartTab martTab,
				final TransformationUnit etu, final int step,
				final ExplainContext explainContext) {
			super(martTab, step, explainContext);

			// Remember the params, and calculate the diagram.
			this.etu = etu;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Replicate the table in an empty schema then add the columns
			// requested.
			final FakeSchema tempSourceSchema = new FakeSchema(Resources
					.get("dummyTempSchemaName"));
			final Table tempSource = new FakeTable(Resources
					.get("dummyTempTableName")
					+ " " + this.getStep(), tempSourceSchema);
			tempSourceSchema.addTable(tempSource);
			for (final Iterator i = this.etu.getNewColumnNameMap().values()
					.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			final TableComponent tc = new TableComponent(tempSource, this);
			this.add(tc, new SchemaLayoutConstraint(0), Diagram.TABLE_LAYER);
			this.getTableComponents().add(tc);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}

	/**
	 * A realised relation is a generic relation with a specific iteration.
	 */
	public static class RealisedRelation extends GenericRelation {

		private final Relation relation;

		private final int relationIteration;

		private final ExplainContext explainContext;

		/**
		 * Use this constant to refer to a relation that covers all iterations,
		 * not just the realised one.
		 */
		public static final int NO_ITERATION = -1;

		/**
		 * Constructs a realised relation.
		 * 
		 * @param sourceKey
		 *            the realised source key.
		 * @param targetKey
		 *            the realised target key.
		 * @param cardinality
		 *            the realised cardinality.
		 * @param relation
		 *            the original relation.
		 * @param relationIteration
		 *            the original relation iteration.
		 * @param explainContext
		 *            the explain context for displaying this realised relation.
		 * @throws AssociationException
		 *             if the relation could not be established.
		 */
		public RealisedRelation(final Key sourceKey, final Key targetKey,
				final Relation.Cardinality cardinality,
				final Relation relation, final int relationIteration,
				final ExplainContext explainContext)
				throws AssociationException {
			super(sourceKey, targetKey, cardinality);
			this.relation = relation;
			this.relationIteration = relationIteration;
			this.explainContext = explainContext;
		}

		/**
		 * @return the explainContext
		 */
		public ExplainContext getExplainContext() {
			return this.explainContext;
		}

		/**
		 * @return the relation
		 */
		public Relation getRelation() {
			return this.relation;
		}

		/**
		 * @return the relationIteration
		 */
		public int getRelationIteration() {
			return this.relationIteration;
		}
	}

	/**
	 * Realised tables are copies of those found in real schemas.
	 */
	public static class RealisedTable extends GenericTable {

		private final Table table;

		private final ExplainContext explainContext;

		/**
		 * Creates a realised table.
		 * 
		 * @param name
		 *            the name to give the realised table.
		 * @param schema
		 *            the schema to put it in.
		 * @param table
		 *            the actual table we are referring to.
		 * @param explainContext
		 *            the context for displaying it.
		 */
		public RealisedTable(final String name, final FakeSchema schema,
				final Table table, final ExplainContext explainContext) {
			super(name, schema);
			this.table = table;
			this.explainContext = explainContext;
		}

		/**
		 * @return the explainContext
		 */
		public ExplainContext getExplainContext() {
			return this.explainContext;
		}

		/**
		 * @return the table
		 */
		public Table getTable() {
			return this.table;
		}
	}

	/**
	 * A fake schema does not really exist.
	 */
	public static class FakeSchema extends GenericSchema {
		/**
		 * Construct a fake schema with the given name.
		 * 
		 * @param name
		 *            the name.
		 */
		public FakeSchema(final String name) {
			super(name);
		}
	}

	/**
	 * A fake table does not really exist.
	 */
	public static class FakeTable extends GenericTable {
		/**
		 * Construct a fake table with the given name in the given schema.
		 * 
		 * @param name
		 *            the name.
		 * @param schema
		 *            the schema.
		 */
		public FakeTable(final String name, final FakeSchema schema) {
			super(name, schema);
		}
	}
}
