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

package org.biomart.builder.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueList;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueRange;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * Represents one task in the grand scheme of constructing a mart.
 * Implementations of this abstract class will provide specific methods for
 * working with the various different stages of mart construction.
 * <p>
 * In all actions, if any schema parameter is null, it means to use the dataset
 * schema instead, as specified by the datasetSchemaName parameter.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public abstract class MartConstructorAction {

	private String datasetTableName;

	private String datasetSchemaName;

	/**
	 * Sets up a node.
	 * 
	 * @param datasetSchemaName
	 *            the name of the schema within which the dataset will be
	 *            constructed. Wherever other schemas in actions are specified
	 *            as null, this schema will be used in place.
	 * @param datasetTableName
	 *            the name of the table this action is associated with.
	 */
	public MartConstructorAction(String datasetSchemaName,
			String datasetTableName) {
		this.datasetSchemaName = datasetSchemaName;
		this.datasetTableName = datasetTableName;
		Log.debug("Constructor action created: " + this.getClass().getName());
	}

	/**
	 * Returns the dataset table name for this action.
	 * 
	 * @return the dataset table name.
	 */
	public String getDataSetTableName() {
		return this.datasetTableName;
	}

	/**
	 * Returns the dataset schema name for this action.
	 * 
	 * @return the dataset schema name.
	 */
	public String getDataSetSchemaName() {
		return this.datasetSchemaName;
	}

	/**
	 * Override this method to produce a message describing what this node of
	 * the graph will do.
	 * 
	 * @return a description of what this node will do.
	 */
	public abstract String getStatusMessage();

	/**
	 * Copy optimiser table actions.
	 */
	public static class CopyOptimiserVia extends CopyOptimiserDirect {
		private static final long serialVersionUID = 1L;

		private List fromKeyColumns;

		private String viaTableName;

		/**
		 * Creates a new CopyOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public CopyOptimiserVia(String datasetSchemaName,
				String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		/**
		 * @return the fromKeyColumns
		 */
		public List getFromKeyColumns() {
			return this.fromKeyColumns;
		}

		/**
		 * @param fromKeyColumns
		 *            the fromKeyColumns to set
		 */
		public void setFromKeyColumns(List fromKeyColumns) {
			this.fromKeyColumns = fromKeyColumns;
		}

		/**
		 * @return the viaTableName
		 */
		public String getViaTableName() {
			return this.viaTableName;
		}

		/**
		 * @param viaTableName
		 *            the viaTableName to set
		 */
		public void setViaTableName(String viaTableName) {
			this.viaTableName = viaTableName;
		}
	}

	/**
	 * Copy optimiser table actions.
	 */
	public static class CopyOptimiserDirect extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String fromOptTableName;

		private String toOptTableName;

		private String fromOptColumnName;

		private String toOptColumnName;

		private List toKeyColumns;

		private boolean countNotBool;

		/**
		 * Creates a new CopyOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public CopyOptimiserDirect(String datasetSchemaName,
				String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCopyOpt", this.getFromOptColumnName());
		}

		/**
		 * @return the fromOptTableName
		 */
		public String getFromOptTableName() {
			return this.fromOptTableName;
		}

		/**
		 * @param fromOptTableName
		 *            the fromOptTableName to set
		 */
		public void setFromOptTableName(String fromOptTableName) {
			this.fromOptTableName = fromOptTableName;
		}

		/**
		 * @return the optColumnName
		 */
		public String getFromOptColumnName() {
			return this.fromOptColumnName;
		}

		/**
		 * @param optColumnName
		 *            the optColumnName to set
		 */
		public void setFromOptColumnName(String optColumnName) {
			this.fromOptColumnName = optColumnName;
		}

		/**
		 * @return the toOptTableName
		 */
		public String getToOptTableName() {
			return this.toOptTableName;
		}

		/**
		 * @param toOptTableName
		 *            the toOptTableName to set
		 */
		public void setToOptTableName(String toOptTableName) {
			this.toOptTableName = toOptTableName;
		}

		/**
		 * @return the toKeyColumns
		 */
		public List getToKeyColumns() {
			return this.toKeyColumns;
		}

		/**
		 * @param toKeyColumns
		 *            the toKeyColumns to set
		 */
		public void setToKeyColumns(List toKeyColumns) {
			this.toKeyColumns = toKeyColumns;
		}

		/**
		 * @return the countNotBool
		 */
		public boolean isCountNotBool() {
			return this.countNotBool;
		}

		/**
		 * @param countNotBool
		 *            the countNotBool to set
		 */
		public void setCountNotBool(boolean countNotBool) {
			this.countNotBool = countNotBool;
		}

		/**
		 * @return the toOptColumnName
		 */
		public String getToOptColumnName() {
			return this.toOptColumnName;
		}

		/**
		 * @param toOptColumnName
		 *            the toOptColumnName to set
		 */
		public void setToOptColumnName(String toOptColumnName) {
			this.toOptColumnName = toOptColumnName;
		}
	}

	/**
	 * Update optimiser table actions.
	 */
	public static class UpdateOptimiser extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection keyColumns;

		private String optTableName;

		private Collection nonNullColumns;

		private String optColumnName;

		private String sourceTableName;

		private boolean countNotBool;

		private boolean nullNotZero;
		
		private ExpressionColumnDefinition expression;
		
		private DataSetTable expressionDSTable;

		/**
		 * Creates a new UpdateOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public UpdateOptimiser(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcUpdateOpt", new String[] {
					this.getOptTableName(), this.getOptColumnName() });
		}

		/**
		 * @return the countNotBool
		 */
		public boolean isCountNotBool() {
			return this.countNotBool;
		}

		/**
		 * @param countNotBool
		 *            the countNotBool to set
		 */
		public void setCountNotBool(boolean countNotBool) {
			this.countNotBool = countNotBool;
		}

		/**
		 * @return the nullNotZero
		 */
		public boolean isNullNotZero() {
			return this.nullNotZero;
		}

		/**
		 * @param nullNotZero
		 *            the nullNotZero to set
		 */
		public void setNullNotZero(boolean nullNotZero) {
			this.nullNotZero = nullNotZero;
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return this.keyColumns;
		}

		/**
		 * @param keyColumns
		 *            the keyColumns to set
		 */
		public void setKeyColumns(Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the nonNullColumns
		 */
		public Collection getNonNullColumns() {
			return this.nonNullColumns;
		}

		/**
		 * @param nonNullColumns
		 *            the nonNullColumns to set
		 */
		public void setNonNullColumns(Collection nonNullColumns) {
			this.nonNullColumns = nonNullColumns;
		}

		/**
		 * @return the optColumnName
		 */
		public String getOptColumnName() {
			return this.optColumnName;
		}

		/**
		 * @param optColumnName
		 *            the optColumnName to set
		 */
		public void setOptColumnName(String optColumnName) {
			this.optColumnName = optColumnName;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return this.optTableName;
		}

		/**
		 * @param optTableName
		 *            the optTableName to set
		 */
		public void setOptTableName(String optTableName) {
			this.optTableName = optTableName;
		}

		/**
		 * @return the sourceTableName
		 */
		public String getSourceTableName() {
			return this.sourceTableName;
		}

		/**
		 * @param sourceTableName
		 *            the sourceTableName to set
		 */
		public void setSourceTableName(String sourceTableName) {
			this.sourceTableName = sourceTableName;
		}

		/**
		 * @return the expression
		 */
		public ExpressionColumnDefinition getExpression() {
			return expression;
		}

		/**
		 * @param expression the expression to set
		 */
		public void setExpression(ExpressionColumnDefinition expression) {
			this.expression = expression;
		}

		/**
		 * @return the expressionDSTable
		 */
		public DataSetTable getExpressionDSTable() {
			return expressionDSTable;
		}

		/**
		 * @param expressionDSTable the expressionDSTable to set
		 */
		public void setExpressionDSTable(DataSetTable expressionDSTable) {
			this.expressionDSTable = expressionDSTable;
		}
	}

	/**
	 * Create optimiser table actions.
	 */
	public static class CreateOptimiser extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection keyColumns;

		private String optTableName;

		/**
		 * Creates a new CreateOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public CreateOptimiser(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCreateOpt", this.getOptTableName());
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return this.keyColumns;
		}

		/**
		 * @param keyColumns
		 *            the keyColumns to set
		 */
		public void setKeyColumns(Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return this.optTableName;
		}

		/**
		 * @param optTableName
		 *            the optTableName to set
		 */
		public void setOptTableName(String optTableName) {
			this.optTableName = optTableName;
		}
	}

	/**
	 * LeftJoin actions.
	 */
	public static class LeftJoin extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String leftTable;

		private String rightSchema;

		private String rightTable;

		private List leftJoinColumns;

		private List rightJoinColumns;

		private List leftSelectColumns;

		private List rightSelectColumns;

		private String resultTable;

		/**
		 * Creates a new LeftJoin action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public LeftJoin(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge", new String[] { this.getLeftTable(),
					this.getRightTable(), this.getResultTable() });
		}

		/**
		 * @return the leftJoinColumns
		 */
		public List getLeftJoinColumns() {
			return this.leftJoinColumns;
		}

		/**
		 * @param leftJoinColumns
		 *            the leftJoinColumns to set
		 */
		public void setLeftJoinColumns(List leftJoinColumns) {
			this.leftJoinColumns = leftJoinColumns;
		}

		/**
		 * @return the leftSelectColumns
		 */
		public List getLeftSelectColumns() {
			return this.leftSelectColumns;
		}

		/**
		 * @param leftSelectColumns
		 *            the leftSelectColumns to set
		 */
		public void setLeftSelectColumns(List leftSelectColumns) {
			this.leftSelectColumns = leftSelectColumns;
		}

		/**
		 * @return the leftTable
		 */
		public String getLeftTable() {
			return this.leftTable;
		}

		/**
		 * @param leftTable
		 *            the leftTable to set
		 */
		public void setLeftTable(String leftTable) {
			this.leftTable = leftTable;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the rightJoinColumns
		 */
		public List getRightJoinColumns() {
			return this.rightJoinColumns;
		}

		/**
		 * @param rightJoinColumns
		 *            the rightJoinColumns to set
		 */
		public void setRightJoinColumns(List rightJoinColumns) {
			this.rightJoinColumns = rightJoinColumns;
		}

		/**
		 * @return the rightSchema
		 */
		public String getRightSchema() {
			return this.rightSchema;
		}

		/**
		 * @param rightSchema
		 *            the rightSchema to set
		 */
		public void setRightSchema(String rightSchema) {
			this.rightSchema = rightSchema;
		}

		/**
		 * @return the rightSelectColumns
		 */
		public List getRightSelectColumns() {
			return this.rightSelectColumns;
		}

		/**
		 * @param rightSelectColumns
		 *            the rightSelectColumns to set
		 */
		public void setRightSelectColumns(List rightSelectColumns) {
			this.rightSelectColumns = rightSelectColumns;
		}

		/**
		 * @return the rightTable
		 */
		public String getRightTable() {
			return this.rightTable;
		}

		/**
		 * @param rightTable
		 *            the rightTable to set
		 */
		public void setRightTable(String rightTable) {
			this.rightTable = rightTable;
		}
	}

	/**
	 * Join actions.
	 */
	public static class Join extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String leftTable;

		private String rightSchema;

		private String rightTable;

		private List leftJoinColumns;

		private List rightJoinColumns;

		private Map selectColumns;

		private String resultTable;

		private RestrictedTableDefinition tableRestriction;

		private RestrictedRelationDefinition relationRestriction;

		private boolean relationRestrictionLeftIsFirst;

		private String partitionColumn;

		private ValueRange partitionRangeDef;

		private ValueList partitionListDef;

		private String partitionValue;

		private TransformationUnit relationRestrictionPreviousUnit;

		/**
		 * Creates a new LeftJoin action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Join(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge", new String[] { this.getLeftTable(),
					this.getRightTable(), this.getResultTable() });
		}

		/**
		 * @return the leftJoinColumns
		 */
		public List getLeftJoinColumns() {
			return this.leftJoinColumns;
		}

		/**
		 * @param leftJoinColumns
		 *            the leftJoinColumns to set
		 */
		public void setLeftJoinColumns(List leftJoinColumns) {
			this.leftJoinColumns = leftJoinColumns;
		}

		/**
		 * @return the leftTable
		 */
		public String getLeftTable() {
			return this.leftTable;
		}

		/**
		 * @param leftTable
		 *            the leftTable to set
		 */
		public void setLeftTable(String leftTable) {
			this.leftTable = leftTable;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the rightJoinColumns
		 */
		public List getRightJoinColumns() {
			return this.rightJoinColumns;
		}

		/**
		 * @param rightJoinColumns
		 *            the rightJoinColumns to set
		 */
		public void setRightJoinColumns(List rightJoinColumns) {
			this.rightJoinColumns = rightJoinColumns;
		}

		/**
		 * @return the rightSchema
		 */
		public String getRightSchema() {
			return this.rightSchema;
		}

		/**
		 * @param rightSchema
		 *            the rightSchema to set
		 */
		public void setRightSchema(String rightSchema) {
			this.rightSchema = rightSchema;
		}

		/**
		 * @return the rightTable
		 */
		public String getRightTable() {
			return this.rightTable;
		}

		/**
		 * @param rightTable
		 *            the rightTable to set
		 */
		public void setRightTable(String rightTable) {
			this.rightTable = rightTable;
		}

		/**
		 * @return the selectColumns
		 */
		public Map getSelectColumns() {
			return this.selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(Map selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the tableRestriction
		 */
		public RestrictedTableDefinition getTableRestriction() {
			return this.tableRestriction;
		}

		/**
		 * @param tableRestriction
		 *            the tableRestriction to set
		 */
		public void setTableRestriction(
				RestrictedTableDefinition tableRestriction) {
			this.tableRestriction = tableRestriction;
		}

		/**
		 * @return the partitionColumn
		 */
		public String getPartitionColumn() {
			return this.partitionColumn;
		}

		/**
		 * @param partitionColumn
		 *            the partitionColumn to set
		 */
		public void setPartitionColumn(String partitionColumn) {
			this.partitionColumn = partitionColumn;
		}

		/**
		 * @return the relationRestriction
		 */
		public RestrictedRelationDefinition getRelationRestriction() {
			return this.relationRestriction;
		}

		/**
		 * @param relationRestriction
		 *            the relationRestriction to set
		 */
		public void setRelationRestriction(
				RestrictedRelationDefinition relationRestriction) {
			this.relationRestriction = relationRestriction;
		}

		/**
		 * @return the relationRestrictionLeftIsFirst
		 */
		public boolean isRelationRestrictionLeftIsFirst() {
			return this.relationRestrictionLeftIsFirst;
		}

		/**
		 * @param relationRestrictionLeftIsFirst
		 *            the relationRestrictionLeftIsFirst to set
		 */
		public void setRelationRestrictionLeftIsFirst(
				boolean relationRestrictionLeftIsFirst) {
			this.relationRestrictionLeftIsFirst = relationRestrictionLeftIsFirst;
		}

		/**
		 * @return the partitionRangeDef
		 */
		public ValueRange getPartitionRangeDef() {
			return this.partitionRangeDef;
		}

		/**
		 * @param partitionRangeDef
		 *            the partitionRangeDef to set
		 */
		public void setPartitionRangeDef(ValueRange partitionRangeDef) {
			this.partitionRangeDef = partitionRangeDef;
		}

		/**
		 * @return the relationRestrictionPreviousUnit
		 */
		public TransformationUnit getRelationRestrictionPreviousUnit() {
			return this.relationRestrictionPreviousUnit;
		}

		/**
		 * @param relationRestrictionPreviousUnit
		 *            the relationRestrictionPreviousUnit to set
		 */
		public void setRelationRestrictionPreviousUnit(
				TransformationUnit relationRestrictionPreviousUnit) {
			this.relationRestrictionPreviousUnit = relationRestrictionPreviousUnit;
		}

		/**
		 * @return the partitionValue
		 */
		public String getPartitionValue() {
			return this.partitionValue;
		}

		/**
		 * @param partitionValue
		 *            the partitionValue to set
		 */
		public void setPartitionValue(String partitionValue) {
			this.partitionValue = partitionValue;
		}

		/**
		 * @return the partitionListDef
		 */
		public ValueList getPartitionListDef() {
			return this.partitionListDef;
		}

		/**
		 * @param partitionListDef
		 *            the partitionListDef to set
		 */
		public void setPartitionListDef(ValueList partitionListDef) {
			this.partitionListDef = partitionListDef;
		}
	}

	/**
	 * ConcatJoin actions.
	 */
	public static class ConcatJoin extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String leftTable;

		private String rightSchema;

		private String rightTable;

		private List leftJoinColumns;

		private List rightJoinColumns;

		private String resultTable;

		private RestrictedTableDefinition restrictedTableDefinition;

		private RestrictedRelationDefinition restrictedRelationDefinition;

		private boolean relationRestrictionLeftIsFirst;

		private String concatColumnName;

		private ConcatRelationDefinition concatColumnDefinition;

		private RecursionType recursionType;

		private List recursionFromColumns;

		private List recursionToColumns;

		private String recursionTable;

		private List recursionSecondFromColumns;

		private List recursionSecondToColumns;

		private TransformationUnit relationRestrictionPreviousUnit;

		/**
		 * Creates a new LeftJoin action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public ConcatJoin(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcConcat", this.getConcatColumnName());
		}

		/**
		 * @return the leftJoinColumns
		 */
		public List getLeftJoinColumns() {
			return this.leftJoinColumns;
		}

		/**
		 * @param leftJoinColumns
		 *            the leftJoinColumns to set
		 */
		public void setLeftJoinColumns(List leftJoinColumns) {
			this.leftJoinColumns = leftJoinColumns;
		}

		/**
		 * @return the leftTable
		 */
		public String getLeftTable() {
			return this.leftTable;
		}

		/**
		 * @param leftTable
		 *            the leftTable to set
		 */
		public void setLeftTable(String leftTable) {
			this.leftTable = leftTable;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the rightJoinColumns
		 */
		public List getRightJoinColumns() {
			return this.rightJoinColumns;
		}

		/**
		 * @param rightJoinColumns
		 *            the rightJoinColumns to set
		 */
		public void setRightJoinColumns(List rightJoinColumns) {
			this.rightJoinColumns = rightJoinColumns;
		}

		/**
		 * @return the rightSchema
		 */
		public String getRightSchema() {
			return this.rightSchema;
		}

		/**
		 * @param rightSchema
		 *            the rightSchema to set
		 */
		public void setRightSchema(String rightSchema) {
			this.rightSchema = rightSchema;
		}

		/**
		 * @return the rightTable
		 */
		public String getRightTable() {
			return this.rightTable;
		}

		/**
		 * @param rightTable
		 *            the rightTable to set
		 */
		public void setRightTable(String rightTable) {
			this.rightTable = rightTable;
		}

		/**
		 * @return the tableRestriction
		 */
		public RestrictedTableDefinition getTableRestriction() {
			return this.restrictedTableDefinition;
		}

		/**
		 * @param restrictedTableDefinition
		 *            the tableRestriction to set
		 */
		public void setTableRestriction(
				RestrictedTableDefinition restrictedTableDefinition) {
			this.restrictedTableDefinition = restrictedTableDefinition;
		}

		/**
		 * @return the relationRestriction
		 */
		public RestrictedRelationDefinition getRelationRestriction() {
			return this.restrictedRelationDefinition;
		}

		/**
		 * @param restrictedRelationDefinition
		 *            the relationRestriction to set
		 */
		public void setRelationRestriction(
				RestrictedRelationDefinition restrictedRelationDefinition) {
			this.restrictedRelationDefinition = restrictedRelationDefinition;
		}

		/**
		 * @return the relationRestrictionLeftIsFirst
		 */
		public boolean isRelationRestrictionLeftIsFirst() {
			return this.relationRestrictionLeftIsFirst;
		}

		/**
		 * @param relationRestrictionLeftIsFirst
		 *            the relationRestrictionLeftIsFirst to set
		 */
		public void setRelationRestrictionLeftIsFirst(
				boolean relationRestrictionLeftIsFirst) {
			this.relationRestrictionLeftIsFirst = relationRestrictionLeftIsFirst;
		}

		/**
		 * @return the concatColumnDefinition
		 */
		public ConcatRelationDefinition getConcatColumnDefinition() {
			return this.concatColumnDefinition;
		}

		/**
		 * @param concatColumnDefinition
		 *            the concatColumnDefinition to set
		 */
		public void setConcatColumnDefinition(
				ConcatRelationDefinition concatColumnDefinition) {
			this.concatColumnDefinition = concatColumnDefinition;
		}

		/**
		 * @return the concatColumnName
		 */
		public String getConcatColumnName() {
			return this.concatColumnName;
		}

		/**
		 * @param concatColumnName
		 *            the concatColumnName to set
		 */
		public void setConcatColumnName(String concatColumnName) {
			this.concatColumnName = concatColumnName;
		}

		/**
		 * @return the recursionFromColumns
		 */
		public List getRecursionFromColumns() {
			return this.recursionFromColumns;
		}

		/**
		 * @param recursionFromColumns
		 *            the recursionFromColumns to set
		 */
		public void setRecursionFromColumns(List recursionFromColumns) {
			this.recursionFromColumns = recursionFromColumns;
		}

		/**
		 * @return the recursionSecondFromColumns
		 */
		public List getRecursionSecondFromColumns() {
			return this.recursionSecondFromColumns;
		}

		/**
		 * @param recursionSecondFromColumns
		 *            the recursionSecondFromColumns to set
		 */
		public void setRecursionSecondFromColumns(
				List recursionSecondFromColumns) {
			this.recursionSecondFromColumns = recursionSecondFromColumns;
		}

		/**
		 * @return the recursionSecondToColumns
		 */
		public List getRecursionSecondToColumns() {
			return this.recursionSecondToColumns;
		}

		/**
		 * @param recursionSecondToColumns
		 *            the recursionSecondToColumns to set
		 */
		public void setRecursionSecondToColumns(List recursionSecondToColumns) {
			this.recursionSecondToColumns = recursionSecondToColumns;
		}

		/**
		 * @return the recursionTable
		 */
		public String getRecursionTable() {
			return this.recursionTable;
		}

		/**
		 * @param recursionTable
		 *            the recursionTable to set
		 */
		public void setRecursionTable(String recursionTable) {
			this.recursionTable = recursionTable;
		}

		/**
		 * @return the recursionToColumns
		 */
		public List getRecursionToColumns() {
			return this.recursionToColumns;
		}

		/**
		 * @param recursionToColumns
		 *            the recursionToColumns to set
		 */
		public void setRecursionToColumns(List recursionToColumns) {
			this.recursionToColumns = recursionToColumns;
		}

		/**
		 * @return the recursionType
		 */
		public RecursionType getRecursionType() {
			return this.recursionType;
		}

		/**
		 * @param recursionType
		 *            the recursionType to set
		 */
		public void setRecursionType(RecursionType recursionType) {
			this.recursionType = recursionType;
		}

		/**
		 * @return the relationRestrictionPreviousUnit
		 */
		public TransformationUnit getRelationRestrictionPreviousUnit() {
			return this.relationRestrictionPreviousUnit;
		}

		/**
		 * @param relationRestrictionPreviousUnit
		 *            the relationRestrictionPreviousUnit to set
		 */
		public void setRelationRestrictionPreviousUnit(
				TransformationUnit relationRestrictionPreviousUnit) {
			this.relationRestrictionPreviousUnit = relationRestrictionPreviousUnit;
		}
	}

	/**
	 * AddExpression actions.
	 */
	public static class AddExpression extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String table;

		private Collection selectColumns;

		private Map expressionColumns;

		private Collection groupByColumns;

		private String resultTable;

		/**
		 * Creates a new Select action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public AddExpression(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcExpressionAdd", this.getExpressionColumns()
					.keySet().toString());
		}

		/**
		 * @return the expressionColumns
		 */
		public Map getExpressionColumns() {
			return this.expressionColumns;
		}

		/**
		 * @param expressionColumns
		 *            the expressionColumns to set
		 */
		public void setExpressionColumns(Map expressionColumns) {
			this.expressionColumns = expressionColumns;
		}

		/**
		 * @return the groupByColumns
		 */
		public Collection getGroupByColumns() {
			return this.groupByColumns;
		}

		/**
		 * @param groupByColumns
		 *            the groupByColumns to set
		 */
		public void setGroupByColumns(Collection groupByColumns) {
			this.groupByColumns = groupByColumns;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the selectColumns
		 */
		public Collection getSelectColumns() {
			return this.selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(Collection selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Select distinct * actions.
	 */
	public static class Distinct extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schema;

		private String table;

		private String resultTable;
		
		/**
		 * Creates a new Distinct action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Distinct(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDistinct", new String[] {
					this.getResultTable(), this.getTable() });
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the schema
		 */
		public String getSchema() {
			return this.schema;
		}

		/**
		 * @param schema
		 *            the schema to set
		 */
		public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Select actions.
	 */
	public static class Select extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schema;

		private String table;

		private Map selectColumns;

		private String resultTable;

		private RestrictedTableDefinition tableRestriction;

		private String partitionColumn;

		private ValueRange partitionRangeDef;

		private ValueList partitionListDef;

		private String partitionValue;

		/**
		 * Creates a new Select action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Select(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCreate", new String[] {
					this.getResultTable(), this.getTable() });
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the schema
		 */
		public String getSchema() {
			return this.schema;
		}

		/**
		 * @param schema
		 *            the schema to set
		 */
		public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
		 * @return the selectColumns
		 */
		public Map getSelectColumns() {
			return this.selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(Map selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}

		/**
		 * @return the tableRestriction
		 */
		public RestrictedTableDefinition getTableRestriction() {
			return this.tableRestriction;
		}

		/**
		 * @param tableRestriction
		 *            the tableRestriction to set
		 */
		public void setTableRestriction(
				RestrictedTableDefinition tableRestriction) {
			this.tableRestriction = tableRestriction;
		}

		/**
		 * @return the partitionColumn
		 */
		public String getPartitionColumn() {
			return this.partitionColumn;
		}

		/**
		 * @param partitionColumn
		 *            the partitionColumn to set
		 */
		public void setPartitionColumn(String partitionColumn) {
			this.partitionColumn = partitionColumn;
		}

		/**
		 * @return the partitionRangeDef
		 */
		public ValueRange getPartitionRangeDef() {
			return this.partitionRangeDef;
		}

		/**
		 * @param partitionRangeDef
		 *            the partitionRangeDef to set
		 */
		public void setPartitionRangeDef(ValueRange partitionRangeDef) {
			this.partitionRangeDef = partitionRangeDef;
		}

		/**
		 * @return the partitionValue
		 */
		public String getPartitionValue() {
			return this.partitionValue;
		}

		/**
		 * @param partitionValue
		 *            the partitionValue to set
		 */
		public void setPartitionValue(String partitionValue) {
			this.partitionValue = partitionValue;
		}

		/**
		 * @return the partitionListDef
		 */
		public ValueList getPartitionListDef() {
			return this.partitionListDef;
		}

		/**
		 * @param partitionListDef
		 *            the partitionListDef to set
		 */
		public void setPartitionListDef(ValueList partitionListDef) {
			this.partitionListDef = partitionListDef;
		}
	}

	/**
	 * Drop column actions.
	 */
	public static class DropColumns extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection columns;

		private String table;

		/**
		 * Creates a new DropColumns action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public DropColumns(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDropCols", this.getColumns().toString());
		}

		/**
		 * @return the columns
		 */
		public Collection getColumns() {
			return this.columns;
		}

		/**
		 * @param columns
		 *            the columns to set
		 */
		public void setColumns(Collection columns) {
			this.columns = columns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Drop actions.
	 */
	public static class Drop extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String table;

		/**
		 * Creates a new Drop action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Drop(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDrop", this.getTable());
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Index actions.
	 */
	public static class Index extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String table;

		private List columns;

		/**
		 * Creates a new Index action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Index(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcIndex", new String[] { this.getTable(),
					this.getColumns().toString() });
		}

		/**
		 * @return the columns
		 */
		public List getColumns() {
			return this.columns;
		}

		/**
		 * @param columns
		 *            the columns to set
		 */
		public void setColumns(List columns) {
			this.columns = columns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Rename actions.
	 */
	public static class Rename extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String from;

		private String to;

		/**
		 * Creates a new Rename action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Rename(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcRename", new String[] { this.getFrom(),
					this.getTo() });
		}

		/**
		 * @return the from
		 */
		public String getFrom() {
			return this.from;
		}

		/**
		 * @param from
		 *            the from to set
		 */
		public void setFrom(String from) {
			this.from = from;
		}

		/**
		 * @return the to
		 */
		public String getTo() {
			return this.to;
		}

		/**
		 * @param to
		 *            the to to set
		 */
		public void setTo(String to) {
			this.to = to;
		}

	}
}
