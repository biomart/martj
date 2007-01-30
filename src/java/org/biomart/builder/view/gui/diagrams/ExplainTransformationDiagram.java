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

import org.biomart.builder.model.TransformationUnit.LeftJoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
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
 *			$Author$
 * @since 0.1
 */
public abstract class ExplainTransformationDiagram extends Diagram {
	private static final long serialVersionUID = 1;
	
	private List tableComponents = new ArrayList();

	/**
	 * Creates an empty diagram, using the single-parameter constructor from
	 * {@link Diagram}.
	 * 
	 * @param martTab
	 *            the tabset to communicate with when (if) context menus are
	 *            selected.
	 */
	protected ExplainTransformationDiagram(MartTab martTab) {
		super(martTab);
	}

	protected void updateAppearance() {
		// Set the background.
		this.setBackground(ExplainTransformationDiagram.BACKGROUND_COLOUR);
	}
	
	/**
	 * Find out what table components we have.
	 * @return the list of components.
	 */
	public Collection getTableComponents() {
		return this.tableComponents;
	}

	/**
	 * The background colour to use for the diagram.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;
	
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
		 */
		public SingleTable(final MartTab martTab, final SelectFromTable stu) {
			super(martTab);

			// Remember the params, and calculate the diagram.
			this.stu = stu;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			this.removeAll();
			// Replicate the table in an empty schema then add the columns
			// requested.
			final Schema tempSourceSchema = new GenericSchema(this.stu.getTable()
					.getSchema().getName());
			final Table tempSource = new GenericTable(this.stu.getTable().getName(),
					tempSourceSchema);
			tempSourceSchema.addTable(tempSource);
			for (Iterator i = this.stu.getNewColumnNameMap().values().iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			final TableComponent tc = new TableComponent(tempSource, this);
			this.addDiagramComponent(tc);
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

		private final LeftJoinTable ltu;
		
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
		 */
		public TempReal(final MartTab martTab, final LeftJoinTable ltu,
				final List lIncludeCols) {
			super(martTab);

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
			final Schema tempSourceSchema = new GenericSchema(
					Resources
					.get("dummyTempSchemaName"));
			final Table tempSource = new GenericTable(Resources
					.get("dummyTempTableName"), tempSourceSchema);
			tempSourceSchema.addTable(tempSource);
			for (Iterator i = this.lIncludeCols.iterator(); i.hasNext();)
				tempSource.addColumn((Column) i.next());
			Key tempSourceKey;
			if (this.ltu.getSchemaSourceKey() instanceof ForeignKey) {
				tempSourceKey = new GenericForeignKey(
							this.ltu.getSourceDataSetColumns());
				try {
					tempSource.addForeignKey((ForeignKey) tempSourceKey);
				} catch (AssociationException e) {
					// Really should never happen.
					throw new BioMartError(e);
				}
			} else {
				tempSourceKey = new GenericPrimaryKey(
						this.ltu.getSourceDataSetColumns());
				tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.ltu.getSchemaRelation().getOtherKey(this.ltu.getSchemaSourceKey());
			final Table realTarget = realTargetKey.getTable();
			final Schema tempTargetSchema = new GenericSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new GenericTable(realTarget.getName(),
					tempTargetSchema);
			tempTargetSchema.addTable(tempTarget);
			for (Iterator i = this.ltu.getNewColumnNameMap().values().iterator(); i.hasNext();)
				tempTarget.addColumn((Column) i.next());
			Key tempTargetKey;
			if (realTargetKey instanceof ForeignKey) {
				tempTargetKey = new GenericForeignKey(realTargetKey
						.getColumns());
				try {
					tempTarget.addForeignKey((ForeignKey) tempTargetKey);
				} catch (AssociationException e) {
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
				tempRelation = new GenericRelation(tempSourceKey,
						tempTargetKey, this.ltu.getSchemaRelation().getCardinality());
				tempSourceKey.addRelation(tempRelation);
				tempTargetKey.addRelation(tempRelation);
			} catch (AssociationException e) {
				// Really should never happen.
				throw new BioMartError(e);
			}

			// Add source and target tables.
			final TableComponent tc1 = new TableComponent(tempSource, this);
			this.addDiagramComponent(tc1);
			this.getTableComponents().add(tc1);
			final TableComponent tc2 = new TableComponent(tempTarget, this);
			this.addDiagramComponent(tc2);
			this.getTableComponents().add(tc2);
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.addDiagramComponent(relationComponent);
			// Resize the diagram to fit.
			this.resizeDiagram();
		}
	}
}
