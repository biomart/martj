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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.UnrolledColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.PartitionTable.PartitionColumn;
import org.biomart.builder.model.PartitionTable.PartitionRow;
import org.biomart.builder.model.PartitionTable.PartitionTableApplication;
import org.biomart.builder.model.PartitionTable.PartitionTableApplication.PartitionAppliedRow;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.CompoundRelationDefinition;
import org.biomart.builder.model.Relation.RestrictedRelationDefinition;
import org.biomart.builder.model.Table.RestrictedTableDefinition;
import org.biomart.builder.model.TransformationUnit.Expression;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.builder.model.TransformationUnit.SkipTable;
import org.biomart.builder.model.TransformationUnit.UnrollTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.exceptions.TransactionException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.BeanMap;
import org.biomart.common.utils.Transaction;
import org.biomart.common.utils.Transaction.TransactionEvent;
import org.biomart.common.utils.Transaction.TransactionListener;
import org.biomart.common.utils.Transaction.WeakPropertyChangeListener;

/**
 * A {@link DataSet} instance serves two purposes. First, it contains lists of
 * settings that are specific to this dataset and affect the way in which tables
 * and relations in the schemas it draws data from behave. Secondly, it is a
 * {@link Schema} itself, containing definitions of all the tables in the
 * dataset it represents and how they relate to each other.
 * <p>
 * The settings that customise the way in which schemas it uses behave include
 * masking of unwanted relations and columns, and flagging of relations as
 * concat-only or subclassed. These settings are specific to this dataset and do
 * not affect other datasets.
 * <p>
 * The central table of the dataset is a reference to a real table, from which
 * the main table of the dataset will be derived and all other transformations
 * in the dataset to produce dimensions and subclasses will begin.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class DataSet extends Schema {
	private static final long serialVersionUID = 1L;

	private final PropertyChangeListener rebuildListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			final Object src = evt.getSource();
			Object val = evt.getNewValue();
			if (val == null)
				val = evt.getOldValue();
			if (src instanceof Relation || src instanceof Table) {
				if (val instanceof DataSet)
					DataSet.this.needsFullSync = val == DataSet.this;
				else if (val instanceof String)
					DataSet.this.needsFullSync = DataSet.this.getTables()
							.containsKey(val);
				else
					DataSet.this.needsFullSync = true;
			} else
				DataSet.this.needsFullSync = true;
			DataSet.this.setDirectModified(true);
		}
	};

	private final PropertyChangeListener existenceListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent evt) {
			// Are we deaded?
			DataSet.this.deadCheck = true;
		}
	};

	private final Table centralTable;

	private final Collection includedRelations;

	private final Collection includedTables;

	private final Collection includedSchemas;

	private boolean invisible;

	private PartitionTable partitionTable;

	private DataSetOptimiserType optimiser;

	private boolean indexOptimiser;

	private PartitionTableApplication partitionTableApplication = null;

	private boolean deadCheck = false;

	/**
	 * Use this key for dataset-wide operations.
	 */
	public static final String DATASET = "__DATASET_WIDE__";

	private final Map mods = new HashMap();

	/**
	 * The constructor creates a dataset around one central table and gives the
	 * dataset a name. It adds itself to the specified mart automatically.
	 * <p>
	 * If the name already exists, an underscore and a sequence number will be
	 * appended until the name is unique, as per the constructor in
	 * {@link Schema}, which it inherits from.
	 * 
	 * @param mart
	 *            the mart this dataset will belong to.
	 * @param centralTable
	 *            the table to use as the central table for this dataset.
	 * @param name
	 *            the name to give this dataset.
	 * @throws ValidationException
	 *             if the central table does not have a primary key.
	 */
	public DataSet(final Mart mart, final Table centralTable, final String name)
			throws ValidationException {
		// Super first, to set the name.
		super(mart, name, name, name);

		// Remember the settings and make some defaults.
		this.invisible = false;
		this.centralTable = centralTable;
		this.optimiser = DataSetOptimiserType.NONE;
		this.includedRelations = new LinkedHashSet();
		this.includedTables = new LinkedHashSet();
		this.includedSchemas = new LinkedHashSet();
		this.partitionTable = null;

		// Always need syncing at end of creating transaction.
		this.needsFullSync = true;

		// All changes to us make us modified.
		final PropertyChangeListener listener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				DataSet.this.setDirectModified(true);
			}
		};
		this.pcs.addPropertyChangeListener("partitionTable", listener);
		this.pcs.addPropertyChangeListener("datasetOptimiser", listener);
		this.pcs.addPropertyChangeListener("indexOptimiser", listener);
		this.pcs.addPropertyChangeListener("invisible", listener);
		this.pcs.addPropertyChangeListener("partitionTableApplication",
				listener);

		// Recalculate completely if parent mart changes case.
		this.getMart().addPropertyChangeListener("case", this.rebuildListener);
	}

	protected void tableDropped(final Table table) {
		final DataSetTable dsTable = (DataSetTable)table;
		// Remove all mods.
		for (final Iterator j = this.getMart().getSchemas().values().iterator(); j
				.hasNext();) {
			final Schema sch = (Schema) j.next();
			for (final Iterator k = sch.getTables().values().iterator(); k
					.hasNext();)
				((Table) k.next()).dropMods(dsTable.getDataSet(), dsTable
						.getName());
			for (final Iterator k = sch.getRelations().iterator(); k.hasNext();)
				((Relation) k.next()).dropMods(dsTable.getDataSet(),
						dsTable.getName());
		}
		// Remove all partition table applications from this dimension.
		final PartitionTableApplication pta = dsTable.getPartitionTableApplication();
		if (pta!=null)
			pta.getPartitionTable().removeFrom(dsTable.getDataSet(), dsTable.getName());
	}

	public void transactionEnded(final TransactionEvent evt)
			throws TransactionException {
		try {
			if (this.deadCheck
					&& !this.getMart().getSchemas().containsKey(
							this.centralTable.getSchema().getName())
					|| !this.centralTable.getSchema().getTables().containsKey(
							this.centralTable.getName()))
				this.getMart().getDataSets().remove(this.getName());
			else
				super.transactionEnded(evt);
		} finally {
			this.deadCheck = false;
		}
	}

	/**
	 * Sets a new name for this dataset. It checks with the mart first, and
	 * renames it if is not unique.
	 * 
	 * @param name
	 *            the new name for the schema.
	 */
	public void setName(String name) {
		Log.debug("Renaming dataset " + this + " to " + name);
		final String oldValue = this.name;
		if (this.name == name || this.name != null && this.name.equals(name))
			return;
		// Make new name unique.
		final String baseName = name;
		for (int i = 1; this.getMart().getDataSets().containsKey(name); name = baseName
				+ "_" + i++)
			;
		this.name = name;
		this.pcs.firePropertyChange("name", oldValue, name);
	}

	/**
	 * This contains the 'real' version of all dataset modifications. It is
	 * accessed directly by MartBuilderXML, DataSetTable and DataSetColumn and
	 * is not for use anywhere else at all under any circumstances. This is used
	 * for applying dataset-wide modifications.
	 * 
	 * @param property
	 *            the property to look up - e.g. maskedDimension, etc.
	 * @return the set of tables that the property currently applies to. This
	 *         set can be added to or removed from accordingly. The keys of the
	 *         map are names, the values are optional subsidiary objects.
	 */
	public Map getMods(final String property) {
		return this.getMods(DataSet.DATASET, property);
	}

	/**
	 * This contains the 'real' version of all dataset modifications. It is
	 * accessed directly by MartBuilderXML, DataSetTable and DataSetColumn and
	 * is not for use anywhere else at all under any circumstances.
	 * 
	 * @param table
	 *            the table to apply the property to.
	 * @param property
	 *            the property to look up - e.g. maskedDimension, etc.
	 * @return the set of tables that the property currently applies to. This
	 *         set can be added to or removed from accordingly. The keys of the
	 *         map are names, the values are optional subsidiary objects.
	 */
	public Map getMods(final String table, final String property) {
		if (!this.mods.containsKey(table))
			this.mods.put(table, new HashMap());
		if (!((Map) this.mods.get(table)).containsKey(property))
			((Map) this.mods.get(table)).put(property, new HashMap());
		return (Map) ((Map) this.mods.get(table)).get(property);
	}

	/**
	 * Is this a partitioned dataset?
	 * 
	 * @return the definition if it is.
	 */
	public PartitionTableApplication getPartitionTableApplication() {
		return this.partitionTableApplication;
	}

	/**
	 * Partition this dataset.
	 * 
	 * @param partitionTableApplication
	 *            the definition. <tt>null</tt> to un-partition.
	 */
	public void setPartitionTableApplication(
			final PartitionTableApplication partitionTableApplication) {
		final PartitionTableApplication oldValue = this
				.getPartitionTableApplication();
		if (partitionTableApplication == oldValue
				|| partitionTableApplication != null
				&& partitionTableApplication.equals(oldValue))
			return;
		this.partitionTableApplication = partitionTableApplication;
		this.pcs.firePropertyChange("partitionTableApplication", oldValue,
				partitionTableApplication);
	}

	/**
	 * Is this dataset a partition table?
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isPartitionTable() {
		return this.partitionTable != null;
	}

	/**
	 * Convert/Revert partition table status.
	 * 
	 * @param partitionTable
	 *            <tt>true</tt> if this dataset is to be a partition table.
	 * @throws PartitionException
	 *             if this dataset cannot be converted. You should use
	 *             {@link #isConvertableToPartitionTable()} to check first
	 *             before calling this if you want to avoid the exception.
	 */
	public void setPartitionTable(final boolean partitionTable)
			throws PartitionException {
		Log.debug("Setting partition table flag to " + partitionTable + " in "
				+ this);
		final boolean oldValue = this.partitionTable != null;
		if (partitionTable == oldValue)
			return;
		if (partitionTable) {
			this.partitionTable = new PartitionTable() {

				// Keep map in alphabetical order.
				private final Collection allCols = new TreeSet();

				public String getName() {
					return DataSet.this.getName();
				}

				public Collection getAvailableColumnNames() {
					if (this.allCols.isEmpty())
						for (final Iterator i = DataSet.this.getMainTable()
								.getColumns().values().iterator(); i.hasNext();) {
							final DataSetColumn col = (DataSetColumn) i.next();
							this.allCols.add(col.getName());
						}
					return this.allCols;
				}

				protected List getRows(final String schemaPartition)
						throws PartitionException {
					if (this.getSelectedColumnNames().isEmpty())
						throw new PartitionException(Resources
								.get("initialColumnsNotSpecified"));
					return DataSet.this.getRowsBySimpleSQL(this,
							schemaPartition);
				}
			};
			// Listen to partition table and pass on modification events.
			this.partitionTable.addPropertyChangeListener(this.listener);
		} else {
			// Remove all applications first.
			for (final Iterator j = this.partitionTable.getAllApplications()
					.entrySet().iterator(); j.hasNext();) {
				final Map.Entry entry = (Map.Entry) j.next();
				final DataSet target = (DataSet) entry.getKey();
				for (final Iterator l = ((Map) entry.getValue()).entrySet()
						.iterator(); l.hasNext();) {
					final Map.Entry entry3 = (Map.Entry) l.next();
					final PartitionTableApplication pta = (PartitionTableApplication) ((WeakReference) entry3
							.getValue()).get();
					if (pta == null)
						continue;
					// Drop the application.
					final String dim = (String) entry3.getKey();
					if (target.getTables().containsKey(dim))
						((DataSetTable) target.getTables().get(dim))
								.setPartitionTableApplication(null);
					else
						target.setPartitionTableApplication(null);
				}
			}
			// Drop the table.
			this.partitionTable = null;
		}
		this.pcs.firePropertyChange("partitionTable", oldValue, partitionTable);
	}

	private List getRowsBySimpleSQL(final PartitionTable pt,
			final String schemaPartition) throws PartitionException {
		Log.debug("Loading rows by simple SQL");

		// Obtain schema.
		final Schema schema = DataSet.this.getMainTable().getFocusTable()
				.getSchema();
		if (!(schema instanceof JDBCSchema))
			return Collections.EMPTY_LIST;
		final JDBCSchema jdbc = (JDBCSchema) schema;

		// Connect.
		Connection conn = null;
		final List rows = new ArrayList();
		try {
			final String usablePartition = (schemaPartition != null && jdbc
					.getPartitions().containsKey(schemaPartition)) ? schemaPartition
					: jdbc.getDataLinkSchema();
			conn = jdbc.getConnection(schemaPartition);
			// Construct SQL statement.
			Log.debug("Building SQL");
			final StringBuffer sql = new StringBuffer();

			// This is generic SQL and should not need any dialects.

			final List trueSelectedCols = new ArrayList();
			for (final Iterator i = pt.getSelectedColumnNames().iterator(); i
					.hasNext();) {
				final String col = (String) i.next();
				if (!col.equals(PartitionTable.DIV_COLUMN))
					trueSelectedCols.add(col);
			}

			// Make a map of columns in statement to
			// named columns in results. Use allCols to
			// map modified names back to real names in
			// order to track down dataset column objects.
			// Update position map with column modified names.
			// Keys are column names, values are integers.
			int nextCol = 1; // ResultSet is 1-indexed.
			final Map positionMap = new HashMap();
			final StringBuffer sqlSel = new StringBuffer();
			sqlSel.append("select distinct ");
			final StringBuffer sqlFrom = new StringBuffer();
			sqlFrom.append(" from ");
			final StringBuffer sqlWhere = new StringBuffer();
			sqlWhere.append(" where ");
			char currSuffix = 'a' - 1;
			final Map prevSuffixes = new HashMap();
			for (final Iterator i = DataSet.this.getMainTable()
					.getTransformationUnits().iterator(); i.hasNext()
					&& positionMap.size() <= trueSelectedCols.size();) {
				final TransformationUnit tu = (TransformationUnit) i.next();
				if (tu instanceof SelectFromTable) {
					// JoinTable extends SelectFromTable.
					// Skip SkipTables and UnrollTables.
					if (tu instanceof SkipTable || tu instanceof UnrollTable)
						continue;
					// Add the unit to the from clause.
					final Table selTab = ((SelectFromTable) tu).getTable();
					final String selSch = selTab.getSchema().equals(schema) ? usablePartition
							: selTab.getSchema().getDataLinkSchema();
					Key prevKey = null;
					if (tu instanceof JoinTable) {
						prevKey = ((JoinTable) tu).getSchemaSourceKey();
						sqlFrom.append(',');
					}
					sqlFrom.append(selSch);
					sqlFrom.append('.');
					sqlFrom.append(selTab.getName());
					sqlFrom.append(" as ");
					sqlFrom.append(++currSuffix);
					prevSuffixes.put(tu, new Character(currSuffix));
					if (tu instanceof JoinTable) {
						final JoinTable jtu = (JoinTable) tu;
						final char lhs;
						final char rhs;
						final TransformationUnit prevTu = jtu.getPreviousUnit();
						if (prevKey.equals(jtu.getSchemaRelation()
								.getFirstKey())) {
							lhs = ((Character) prevSuffixes.get(prevTu))
									.charValue();
							rhs = currSuffix;
						} else {
							rhs = ((Character) prevSuffixes.get(prevTu))
									.charValue();
							lhs = currSuffix;
						}
						// Append join info to where clause.
						if (!sqlWhere.toString().equals(" where "))
							sqlWhere.append(" and ");
						for (int k = 0; k < prevKey.getColumns().length; k++) {
							if (k > 0)
								sqlWhere.append(" and ");
							sqlWhere.append(prevKey.equals(jtu
									.getSchemaRelation().getFirstKey()) ? lhs
									: rhs);
							sqlWhere.append('.');
							sqlWhere.append(((Column) prevKey.getColumns()[k])
									.getName());
							sqlWhere.append('=');
							sqlWhere.append(prevKey.equals(jtu
									.getSchemaRelation().getFirstKey()) ? rhs
									: lhs);
							sqlWhere.append('.');
							sqlWhere.append(((Column) jtu.getSchemaRelation()
									.getOtherKey(jtu.getSchemaSourceKey())
									.getColumns()[k]).getName());
						}
						// Add any rel restrictions to where clause.
						final RestrictedRelationDefinition rr = jtu
								.getSchemaRelation().getRestrictRelation(this,
										this.getMainTable().getName(),
										jtu.getSchemaRelationIteration());
						if (rr != null) {
							sqlWhere.append(" and ");
							sqlWhere.append(rr.getSubstitutedExpression(""
									+ lhs, "" + rhs, false, false, jtu));
						}
					}
					// Add any table restrictions to where clause.
					final RestrictedTableDefinition rt = selTab
							.getRestrictTable(this, this.getMainTable()
									.getName());
					if (rt != null) {
						if (!sqlWhere.toString().equals(" where "))
							sqlWhere.append(" and ");
						sqlWhere.append(rt.getSubstitutedExpression(""
								+ currSuffix));
					}
					// If any unit columns match selected columns,
					// add them to the select statement and their
					// position to the index map.
					for (final Iterator j = tu.getNewColumnNameMap().entrySet()
							.iterator(); j.hasNext();) {
						final Map.Entry entry = (Map.Entry) j.next();
						final DataSetColumn dsCol = (DataSetColumn) entry
								.getValue();
						if (trueSelectedCols.contains(dsCol.getName())) {
							final String col = (String) entry.getKey();
							if (nextCol > 1)
								sqlSel.append(',');
							sqlSel.append(currSuffix);
							sqlSel.append('.');
							sqlSel.append(col);
							positionMap.put(new Integer(nextCol++), dsCol
									.getName());
						}
					}
				} else
					throw new PartitionException(Resources
							.get("cannotDoBasicSQL"));
			}

			// Build SQL.
			sql.append(sqlSel);
			sql.append(sqlFrom);
			if (!sqlWhere.toString().equals(" where "))
				sql.append(sqlWhere);

			// Run it.
			Log.debug("About to run SQL: " + sql.toString());
			final PreparedStatement stmt = conn
					.prepareStatement(sql.toString());
			stmt.execute();
			final ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				// Read rows.
				final Map rowCols = new HashMap();
				// Populate rowCols with column names as
				// keys and values as objects.
				for (final Iterator i = positionMap.entrySet().iterator(); i
						.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					rowCols.put(entry.getValue(), rs.getObject(((Integer) entry
							.getKey()).intValue()));
				}
				// Build the row from rowCols.
				final PartitionRow row = new PartitionRow(pt) {
					public String getValue(String columnName)
							throws PartitionException {
						return "" + rowCols.get(columnName);
					}
				};
				rows.add(row);
			}
		} catch (final SQLException e) {
			throw new PartitionException(e);
		} finally {
			if (conn != null)
				try {
					conn.close();
				} catch (final SQLException e2) {
					// Don't care.
				}
		}

		return rows;
	}

	/**
	 * Check to see if this dataset is convertable to partition table status.
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isConvertableToPartitionTable() {
		// The answer is yes, but only if our main table has no
		// group-by expression columns and nothing uses partitions
		// and there are no compound relations.
		if (this.partitionTableApplication != null)
			return false;
		for (final Iterator i = this.getTables().values().iterator(); i
				.hasNext();) {
			final DataSetTable dsTable = (DataSetTable) i.next();
			if (dsTable.getPartitionTableApplication() != null)
				return false;
			if (!dsTable.getType().equals(DataSetTableType.MAIN))
				if (dsTable.getFocusRelation().getCompoundRelation(this,
						dsTable.getName()) != null)
					return false;
			for (final Iterator j = dsTable.getIncludedRelations().iterator(); j
					.hasNext();)
				if (((Relation) j.next()).getCompoundRelation(this, dsTable
						.getName()) != null)
					return false;
		}
		for (final Iterator i = this.getMainTable().getColumns().values()
				.iterator(); i.hasNext();)
			if (i.next() instanceof ExpressionColumn)
				return false;
		// If we get here, we're good.
		return true;
	}

	/**
	 * Get the partition table representation of this dataset.
	 * 
	 * @return a {@link PartitionTable} object providing partition table data
	 *         based on this dataset's main table.
	 */
	public PartitionTable asPartitionTable() {
		return this.partitionTable;
	}

	/**
	 * Obtain all tables used by this dataset, in the order in which they were
	 * included.
	 * 
	 * @return all tables.
	 */
	public Collection getIncludedTables() {
		return this.includedTables;
	}

	/**
	 * Obtain all relations used by this dataset, in the order in which they
	 * were included.
	 * 
	 * @return all relations.
	 */
	public Collection getIncludedRelations() {
		return this.includedRelations;
	}

	/**
	 * This internal method builds a dataset table based around a real table. It
	 * works out what dimensions and subclasses are required then recurses to
	 * create those too.
	 * 
	 * @param type
	 *            the type of table to build.
	 * @param parentDSTable
	 *            the table which this dataset table creates a foreign key to.
	 *            If this is to be a subclass table, it will inherit all columns
	 *            from this parent table.
	 * @param realTable
	 *            the real table in a schema from where the transformation to
	 *            create this dataset table will begin.
	 * @param sourceRelation
	 *            the real relation in a schema which was followed in order to
	 *            discover that this dataset table should be created. For
	 *            instance, it could be the 1:M relation between the realTable
	 *            parameter of this call, and the realTable parameter of the
	 *            main table call to this method.
	 * @throws PartitionException
	 *             if partitioning is in use and went wrong.
	 */
	private void generateDataSetTable(final DataSetTableType type,
			final DataSetTable parentDSTable, final Table realTable,
			final List sourceDSCols, final Relation sourceRelation,
			final Map subclassCount, final int relationIteration,
			final Collection unusedTables) throws PartitionException {
		Log.debug("Creating dataset table for " + realTable
				+ " with parent relation " + sourceRelation + " as a " + type);
		// Create the empty dataset table. Use a unique prefix
		// to prevent naming clashes.
		String prefix = "";
		if (parentDSTable != null) {
			final String parts[] = parentDSTable.getName().split(
					Resources.get("tablenameSep"));
			prefix = parts[parts.length - 1] + Resources.get("tablenameSep");
		}
		String fullName = prefix + realTable.getName();
		if (relationIteration > 0)
			fullName = fullName + Resources.get("tablenameSubSep")
					+ relationIteration;
		// Loop over all tables with similar names to check for reuse.
		DataSetTable dsTable = null;
		for (final Iterator i = this.getTables().entrySet().iterator(); i
				.hasNext()
				&& dsTable == null;) {
			final Map.Entry entry = (Map.Entry) i.next();
			final String testName = (String) entry.getKey();
			final DataSetTable testTable = (DataSetTable) entry.getValue();
			// If find table starting with same letters, check to see
			// if can reuse, and update fullName to match it.
			if (testName.equals(fullName) || testName.startsWith(fullName))
				if (testTable.getFocusRelation() == null
						|| testTable.getFocusRelation().equals(sourceRelation)) {
					fullName = testName;
					dsTable = testTable;
					dsTable.setType(type); // Just to make sure.
					unusedTables.remove(dsTable);
					dsTable.getTransformationUnits().clear();
				} else
					dsTable = null;
		}
		// If still not found anything after all tables checked,
		// create new table.
		if (dsTable == null) {
			dsTable = new DataSetTable(fullName, this, type, realTable,
					sourceRelation);
			this.getTables().put(dsTable.getName(), dsTable);
			// Listen to this table to modify ourselves.
			// As it happens, nothing can happen to a dstable yet that
			// requires this.
		}
		dsTable.includedRelations.clear();
		dsTable.includedTables.clear();

		// Create the three relation-table pair queues we will work with. The
		// normal queue holds pairs of relations and tables. The other two hold
		// a list of relations only, the tables being the FK ends of each
		// relation. The normal queue has a third object associated with each
		// entry, which specifies whether to treat the 1:M relations from
		// the merged table as dimensions or not.
		final List normalQ = new ArrayList();
		final List subclassQ = new ArrayList();
		final List dimensionQ = new ArrayList();

		// Set up a list to hold columns for this table's primary key.
		final List dsTablePKCols = new ArrayList();

		// Make a list of existing columns and foreign keys.
		final Collection unusedCols = new HashSet(dsTable.getColumns().values());
		final Collection unusedFKs = new HashSet(dsTable.getForeignKeys());

		// Make a map for unique column base names.
		final Map uniqueBases = new HashMap();

		// If the parent dataset table is not null, add columns from it
		// as appropriate. Dimension tables get just the PK, and an
		// FK linking them back. Subclass tables get all columns, plus
		// the PK with FK link, plus all the relations we followed to
		// get these columns.
		TransformationUnit parentTU = null;
		if (parentDSTable != null) {
			parentTU = new SelectFromTable(parentDSTable);
			dsTable.addTransformationUnit(parentTU);

			// Make a list to hold the child table's FK cols.
			final List dsTableFKCols = new ArrayList();

			// Get the primary key of the parent DS table.
			final PrimaryKey parentDSTablePK = parentDSTable.getPrimaryKey();

			// Loop over each column in the parent table. If this is
			// a subclass table, add it. If it is a dimension table,
			// only add it if it is in the PK or is in the first underlying
			// key. In either case, if it is in the PK, add it both to the
			// child PK and the child FK.
			for (final Iterator i = parentDSTable.getColumns().values()
					.iterator(); i.hasNext();) {
				final DataSetColumn parentDSCol = (DataSetColumn) i.next();
				// If this is not a subclass table, we need to filter columns.
				if (!type.equals(DataSetTableType.MAIN_SUBCLASS)) {
					// Skip columns that are not in the primary key.
					final boolean inPK = Arrays.asList(
							parentDSTablePK.getColumns()).contains(parentDSCol);
					final boolean inSourceKey = sourceDSCols
							.contains(parentDSCol);
					if (!inPK && !inSourceKey)
						continue;
				}
				// If column is masked, don't inherit it.
				if (parentDSCol.isColumnMasked()) {
					// Remove it straight away if we had a copy of it before.
					if (dsTable.getColumns().containsKey(
							parentDSCol.getModifiedName())) {
						final DataSetColumn dsCol = (DataSetColumn) dsTable
								.getColumns()
								.get(parentDSCol.getModifiedName());
						if (dsCol instanceof InheritedColumn) {
							dsTable.getColumns().remove(
									parentDSCol.getModifiedName());
							unusedCols.remove(dsCol);
						}
					}
					continue;
				}
				// Only unfiltered columns reach this point. Create a copy of
				// the column.
				final InheritedColumn dsCol;
				if (!dsTable.getColumns().containsKey(
						parentDSCol.getModifiedName())) {
					dsCol = new InheritedColumn(dsTable, parentDSCol);
					dsTable.getColumns().put(dsCol.getName(), dsCol);
				} else {
					final DataSetColumn candDSCol = (DataSetColumn) dsTable
							.getColumns().get(parentDSCol.getModifiedName());
					if (candDSCol instanceof InheritedColumn)
						dsCol = (InheritedColumn) candDSCol;
					else {
						dsCol = new InheritedColumn(dsTable, parentDSCol);
						// Listen to this column to modify ourselves.
						if (!dsTable.getType().equals(
								DataSetTableType.DIMENSION)) {
							dsCol.addPropertyChangeListener("columnMasked",
									new WeakPropertyChangeListener(dsCol,
											"columnMasked",
											this.rebuildListener));
							dsCol.addPropertyChangeListener("columnRename",
									new WeakPropertyChangeListener(dsCol,
											"columnRename",
											this.rebuildListener));
						}
						dsTable.getColumns().put(dsCol.getName(), dsCol);
					}
				}
				unusedCols.remove(dsCol);
				parentTU.getNewColumnNameMap()
						.put(parentDSCol.getName(), dsCol);
				uniqueBases.put(parentDSCol.getModifiedName(), new Integer(0));
				// Add the column to the child's FK, but only if it was in
				// the parent PK.
				if (Arrays.asList(parentDSTablePK.getColumns()).contains(
						parentDSCol))
					dsTableFKCols.add(dsCol);
			}

			try {
				// Create the child FK.
				final ForeignKey dsTableFK = new ForeignKey(
						(Column[]) dsTableFKCols.toArray(new Column[0]));
				// Create only if not already exists.
				if (!dsTable.getForeignKeys().contains(dsTableFK)) {
					dsTable.getForeignKeys().add(dsTableFK);
					// Link the child FK to the parent PK.
					final Relation rel = new Relation(parentDSTablePK,
							dsTableFK, Cardinality.MANY);
					parentDSTablePK.getRelations().add(rel);
					dsTableFK.getRelations().add(rel);
				} else
					unusedFKs.remove(dsTableFK);
			} catch (final Throwable t) {
				throw new BioMartError(t);
			}

			// Do a user-friendly rename.
			if (dsTable.getTableRename() == null)
				dsTable.setTableRename(realTable.getName());

			// Copy all parent FKs and add to child, but WITHOUT
			// relations. Subclasses only!
			if (type.equals(DataSetTableType.MAIN_SUBCLASS))
				for (final Iterator i = parentDSTable.getForeignKeys()
						.iterator(); i.hasNext();) {
					final ForeignKey parentFK = (ForeignKey) i.next();
					final List childFKCols = new ArrayList();
					for (int j = 0; j < parentFK.getColumns().length; j++)
						childFKCols.add(parentTU.getNewColumnNameMap().get(
								((DataSetColumn) parentFK.getColumns()[j])
										.getName()));
					try {
						// Create the child FK.
						final ForeignKey dsTableFK = new ForeignKey(
								(Column[]) childFKCols.toArray(new Column[0]));
						// Create only if not already exists.
						if (!dsTable.getForeignKeys().contains(dsTableFK))
							dsTable.getForeignKeys().add(dsTableFK);
						else
							unusedFKs.remove(dsTableFK);
					} catch (final Throwable t) {
						throw new BioMartError(t);
					}
				}
		}

		// How many times are allowed to iterate over each relation?
		final Map relationCount = new HashMap();
		for (final Iterator i = this.getMart().getSchemas().values().iterator(); i
				.hasNext();) {
			final Schema schema = (Schema) i.next();
			final Set relations = new HashSet();
			for (final Iterator j = schema.getTables().values().iterator(); j
					.hasNext();) {
				final Table tbl = (Table) j.next();
				if (tbl.getPrimaryKey() != null)
					relations.addAll(tbl.getPrimaryKey().getRelations());
				for (final Iterator k = tbl.getForeignKeys().iterator(); k
						.hasNext();)
					relations.addAll(((ForeignKey) k.next()).getRelations());
			}
			for (final Iterator j = relations.iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				// Partition compounding is dealt with separately
				// and does not need to be included here.
				final CompoundRelationDefinition def = rel.getCompoundRelation(
						this, dsTable.getName());
				int compounded = def == null ? 1 : def.getN();
				// If loopback, increment count by one.
				if (rel.getLoopbackRelation(this, dsTable.getName()) != null)
					compounded++;
				relationCount.put(rel, new Integer(compounded));
			}
		}

		// Process the table. This operation will populate the initial
		// values in the normal, subclass and dimension queues. We only
		// want dimensions constructed if we are not already constructing
		// a dimension ourselves.
		this.processTable(parentTU, dsTable, dsTablePKCols, realTable, normalQ,
				subclassQ, dimensionQ, sourceDSCols, sourceRelation,
				relationCount, subclassCount, !type
						.equals(DataSetTableType.DIMENSION),
				Collections.EMPTY_LIST, Collections.EMPTY_LIST,
				relationIteration, 0, unusedCols, uniqueBases);

		// Process the normal queue. This merges tables into the dataset
		// table using the relation specified in each pair in the queue.
		// The third value is the dataset parent table columns to link from.
		// The fourth value of each entry in the queue determines whether or
		// not to continue making dimensions off each table in the queue.
		// The fifth value is the counter of how many times this relation has
		// been seen before.
		// The sixth value is a map of relation counts used to reach this point.
		for (int i = 0; i < normalQ.size(); i++) {
			final Object[] tuple = (Object[]) normalQ.get(i);
			final Relation mergeSourceRelation = (Relation) tuple[0];
			final List newSourceDSCols = (List) tuple[1];
			final Table mergeTable = (Table) tuple[2];
			final TransformationUnit previousUnit = (TransformationUnit) tuple[3];
			final boolean makeDimensions = ((Boolean) tuple[4]).booleanValue();
			final int iteration = ((Integer) tuple[5]).intValue();
			final List nameCols = (List) tuple[6];
			final List nameColSuffixes = (List) tuple[7];
			final Map newRelationCounts = (Map) tuple[8];
			this.processTable(previousUnit, dsTable, dsTablePKCols, mergeTable,
					normalQ, subclassQ, dimensionQ, newSourceDSCols,
					mergeSourceRelation, newRelationCounts, subclassCount,
					makeDimensions, nameCols, nameColSuffixes, iteration,
					i + 1, unusedCols, uniqueBases);
		}

		// Create the primary key on this table, but only if it has one.
		// Don't bother for dimensions.
		if (!dsTablePKCols.isEmpty()
				&& !dsTable.getType().equals(DataSetTableType.DIMENSION))
			// Create the key.
			dsTable.setPrimaryKey(new PrimaryKey((Column[]) dsTablePKCols
					.toArray(new Column[0])));
		else
			dsTable.setPrimaryKey(null);

		// Fish out any UnrollTable units and move to end of queue.
		final List units = dsTable.getTransformationUnits();
		for (int i = 1; i < units.size() - 1; i++) { // Skip very first+last.
			final TransformationUnit tu = (TransformationUnit) units.get(i);
			if (tu instanceof UnrollTable) {
				final TransformationUnit ptu = (TransformationUnit) units
						.get(i - 1);
				final TransformationUnit ntu = (TransformationUnit) units
						.get(i + 1);
				ntu.setPreviousUnit(ptu);
				final TransformationUnit ltu = (TransformationUnit) units
						.get(units.size() - 1);
				tu.setPreviousUnit(ltu);
				units.remove(i);
				units.add(tu);
				break;
			}
		}

		// First time round only - do we have an initial set of
		// expression column defs to create on this table?
		// If so, create them.
		final Map initialExpCols = this.getMods(dsTable.getName(),
				"initialExpressions");
		if (initialExpCols != null && !initialExpCols.isEmpty()) {
			for (final Iterator i = initialExpCols.values().iterator(); i
					.hasNext();) {
				final ExpressionColumnDefinition expcolDef = (ExpressionColumnDefinition) i
						.next();
				final ExpressionColumn expCol = new ExpressionColumn(expcolDef
						.getColKey(), dsTable, expcolDef);
				// Listen to this column to modify ourselves.
				if (!dsTable.getType().equals(DataSetTableType.DIMENSION))
					expCol.addPropertyChangeListener("columnRename",
							new WeakPropertyChangeListener(expCol,
									"columnRename", this.rebuildListener));
				dsTable.getColumns().put(expCol.getName(), expCol);
			}
			// Wipe it out so only happens first time.
			initialExpCols.clear();
		}
		// Insert Expression Column Transformation Unit
		// containing all expression columns defined on this table.
		final List exprCols = new ArrayList();
		for (final Iterator i = dsTable.getColumns().values().iterator(); i
				.hasNext();) {
			final DataSetColumn cand = (DataSetColumn) i.next();
			if (cand instanceof ExpressionColumn)
				exprCols.add(cand);
		}
		if (!exprCols.isEmpty()) {
			final Collection aliases = new HashSet();
			final Expression tu = new Expression((TransformationUnit) dsTable
					.getTransformationUnits().get(
							dsTable.getTransformationUnits().size() - 1),
					dsTable);
			dsTable.addTransformationUnit(tu);
			for (final Iterator i = exprCols.iterator(); i.hasNext();) {
				final ExpressionColumn expCol = (ExpressionColumn) i.next();
				// Save up the aliases to make dependents later.
				aliases.addAll(expCol.getDefinition().getAliases().keySet());
				unusedCols.remove(expCol);
				tu.getNewColumnNameMap().put(expCol.getName(), expCol);
				// Skip unique bases stuff here as no more cols get added after
				// this point.
			}
			// Mark all aliased columns as dependents
			for (final Iterator j = aliases.iterator(); j.hasNext();) {
				final DataSetColumn dsCol = (DataSetColumn) dsTable
						.getColumns().get((String) j.next());
				// We do a null check in case the expression refers to a
				// non-existent column.
				if (dsCol != null)
					dsCol.setExpressionDependency(true);
			}
		}

		// Drop unused columns and foreign keys.
		for (final Iterator i = unusedFKs.iterator(); i.hasNext();) {
			final ForeignKey fk = (ForeignKey) i.next();
			for (final Iterator j = fk.getRelations().iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				rel.getFirstKey().getRelations().remove(rel);
				rel.getSecondKey().getRelations().remove(rel);
			}
			dsTable.getForeignKeys().remove(fk);
		}
		for (final Iterator i = unusedCols.iterator(); i.hasNext();) {
			final Column deadCol = (Column) i.next();
			dsTable.getColumns().remove(deadCol.getName());
			// mods is Map{tablename -> Map{propertyName -> Map{...}} }
			for (final Iterator j = ((Map) this.mods.get(deadCol.getTable()
					.getName())).entrySet().iterator(); j.hasNext();) {
				final Map.Entry entry = (Map.Entry) j.next();
				// entry is propertyName -> Map{columnName, Object}}
				((Map) entry.getValue()).remove(deadCol.getName());
			}
		}

		// Only dataset tables with primary keys can have subclasses
		// or dimensions.
		if (dsTable.getPrimaryKey() != null) {
			// Process the subclass relations of this table.
			for (int i = 0; i < subclassQ.size(); i++) {
				final Object[] triple = (Object[]) subclassQ.get(i);
				final List newSourceDSCols = (List) triple[0];
				final Relation subclassRelation = (Relation) triple[1];
				final int iteration = ((Integer) triple[2]).intValue();
				this.generateDataSetTable(DataSetTableType.MAIN_SUBCLASS,
						dsTable, subclassRelation.getManyKey().getTable(),
						newSourceDSCols, subclassRelation, subclassCount,
						iteration, unusedTables);
			}

			// Process the dimension relations of this table. For 1:M it's easy.
			// For M:M, we have to work out which end is connected to the real
			// table, then process the table at the other end of the relation.
			for (int i = 0; i < dimensionQ.size(); i++) {
				final Object[] triple = (Object[]) dimensionQ.get(i);
				final List newSourceDSCols = (List) triple[0];
				final Relation dimensionRelation = (Relation) triple[1];
				final int iteration = ((Integer) triple[2]).intValue();
				if (dimensionRelation.isOneToMany())
					this.generateDataSetTable(DataSetTableType.DIMENSION,
							dsTable, dimensionRelation.getManyKey().getTable(),
							newSourceDSCols, dimensionRelation, subclassCount,
							iteration, unusedTables);
				else
					this.generateDataSetTable(DataSetTableType.DIMENSION,
							dsTable, dimensionRelation.getFirstKey().getTable()
									.equals(realTable) ? dimensionRelation
									.getSecondKey().getTable()
									: dimensionRelation.getFirstKey()
											.getTable(), newSourceDSCols,
							dimensionRelation, subclassCount, iteration,
							unusedTables);
			}
		}
	}

	/**
	 * This method takes a real table and merges it into a dataset table. It
	 * does this by creating {@link WrappedColumn} instances for each new column
	 * it finds in the table.
	 * <p>
	 * If a source relation was specified, columns in the key in the table that
	 * is part of that source relation are ignored, else they'll get duplicated.
	 * 
	 * @param dsTable
	 *            the dataset table we are constructing and should merge the
	 *            columns into.
	 * @param dsTablePKCols
	 *            the primary key columns of that table. If we find we need to
	 *            add to these, we should add to this list directly.
	 * @param mergeTable
	 *            the real table we are about to merge columns from.
	 * @param normalQ
	 *            the queue to add further real tables into that we find need
	 *            merging into this same dataset table.
	 * @param subclassQ
	 *            the queue to add starting points for subclass tables that we
	 *            find.
	 * @param dimensionQ
	 *            the queue to add starting points for dimension tables we find.
	 * @param sourceRelation
	 *            the real relation we followed to reach this table.
	 * @param relationCount
	 *            how many times we have left to follow each relation, so that
	 *            we don't follow them too often.
	 * @param subclassCount
	 *            how many times we have followed a particular subclass
	 *            relation.
	 * @param makeDimensions
	 *            <tt>true</tt> if we should add potential dimension tables to
	 *            the dimension queue, <tt>false</tt> if we should just ignore
	 *            them. This is useful for preventing dimensions from gaining
	 *            dimensions of their own.
	 * @param nameCols
	 *            the list of partition columns to prefix the new dataset
	 *            columns with.
	 * @param queuePos
	 *            this position in the queue to insert the next steps at.
	 * @throws PartitionException
	 *             if partitioning is in use and went wrong.
	 */
	private void processTable(final TransformationUnit previousUnit,
			final DataSetTable dsTable, final List dsTablePKCols,
			final Table mergeTable, final List normalQ, final List subclassQ,
			final List dimensionQ, final List sourceDataSetCols,
			final Relation sourceRelation, final Map relationCount,
			final Map subclassCount, final boolean makeDimensions,
			final List nameCols, final List nameColSuffixes,
			final int relationIteration, int queuePos,
			final Collection unusedCols, final Map uniqueBases)
			throws PartitionException {
		Log.debug("Processing table " + mergeTable);

		// Remember the schema.
		this.includedSchemas.add(mergeTable.getSchema());

		// Don't ignore any keys by default.
		final Set ignoreCols = new HashSet();

		final TransformationUnit tu;

		// Did we get here via somewhere else?
		if (sourceRelation != null) {
			// Work out what key to ignore by working out at which end
			// of the relation we are.
			final Key ignoreKey = sourceRelation.getKeyForTable(mergeTable);
			ignoreCols.addAll(Arrays.asList(ignoreKey.getColumns()));
			final Key mergeKey = sourceRelation.getOtherKey(ignoreKey);

			// Add the relation and key to the list that the table depends on.
			// This list is what defines the path required to construct
			// the DDL for this table.
			tu = new JoinTable(previousUnit, mergeTable, sourceDataSetCols,
					mergeKey, sourceRelation, relationIteration);

			// Remember we've been here.
			this.includedRelations.add(sourceRelation);
			dsTable.includedRelations.add(sourceRelation);
		} else
			tu = new SelectFromTable(mergeTable);
		this.includedTables.add(mergeTable);
		dsTable.includedTables.add(mergeTable);

		dsTable.addTransformationUnit(tu);

		// Work out the merge table's PK.
		final PrimaryKey mergeTablePK = mergeTable.getPrimaryKey();

		// We must merge only the first PK we come across, if this is
		// a main table, or the first PK we come across after the
		// inherited PK, if this is a subclass. Dimensions dont get
		// merged at all.
		boolean includeMergeTablePK = mergeTablePK != null
				&& !mergeTablePK.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT)
				&& !dsTable.getType().equals(DataSetTableType.DIMENSION);
		if (includeMergeTablePK && sourceRelation != null)
			// Only add further PK columns if the relation did NOT
			// involve our PK and was NOT 1:1.
			includeMergeTablePK = dsTablePKCols.isEmpty()
					&& !sourceRelation.isOneToOne()
					&& !sourceRelation.getFirstKey().equals(mergeTablePK)
					&& !sourceRelation.getSecondKey().equals(mergeTablePK);

		// Make a list of all columns involved in keys on the merge table.
		final Set colsUsedInKeys = new HashSet();
		for (final Iterator i = mergeTable.getKeys().iterator(); i.hasNext();)
			colsUsedInKeys.addAll(Arrays.asList(((Key) i.next()).getColumns()));

		// Add all columns from merge table to dataset table, except those in
		// the ignore key.
		for (final Iterator i = mergeTable.getColumns().values().iterator(); i
				.hasNext();) {
			final Column c = (Column) i.next();

			// Ignore those in the key used to get here.
			if (ignoreCols.contains(c))
				continue;

			// Create a wrapped column for this column.
			String colName = c.getName();
			// Add partitioning prefixes.
			for (int k = 0; k < nameCols.size(); k++) {
				final String pcolName = (String) nameCols.get(k);
				final String suffix = nameColSuffixes.size() <= k ? "" : "#"
						+ (String) nameColSuffixes.get(k);
				colName = pcolName + suffix + Resources.get("columnnameSep")
						+ colName;
			}
			// If appears in uniqueBases, add table suffix and retry.
			if (uniqueBases.containsKey(colName)) {
				colName = dsTable.getColumns().containsKey(colName) ? colName
						.endsWith(Resources.get("keySuffix")) ? colName
						.substring(0, colName.indexOf(Resources
								.get("keySuffix")))
						+ "_"
						+ c.getTable().getName()
						+ Resources.get("keySuffix") : colName + "_"
						+ c.getTable().getName() : colName;
				// If still appears in uniqueBases, add unique number.
				if (uniqueBases.containsKey(colName)) {
					final int numberSuffix = ((Integer) uniqueBases
							.get(colName)).intValue() + 1;
					uniqueBases.put(colName, new Integer(numberSuffix));
					// Add number suffix and update uniqueBases.
					colName = colName.endsWith(Resources.get("keySuffix")) ? colName
							.substring(0, colName.indexOf(Resources
									.get("keySuffix")))
							+ "_" + numberSuffix + Resources.get("keySuffix")
							: colName + "_" + numberSuffix;
				}
			} else
				uniqueBases.put(colName, new Integer(0));
			// Rename all PK columns to have the '_key' suffix.
			if (includeMergeTablePK
					&& Arrays.asList(mergeTablePK.getColumns()).contains(c)
					&& !colName.endsWith(Resources.get("keySuffix")))
				colName = colName + Resources.get("keySuffix");
			final WrappedColumn wc;
			if (dsTable.getColumns().containsKey(colName)) {
				final DataSetColumn candDSCol = (DataSetColumn) dsTable
						.getColumns().get(colName);
				if (candDSCol instanceof WrappedColumn)
					wc = (WrappedColumn) dsTable.getColumns().get(colName);
				else {
					wc = new WrappedColumn(c, colName, dsTable);
					dsTable.getColumns().put(wc.getName(), wc);
					// Listen to this column to modify ourselves.
					if (!dsTable.getType().equals(DataSetTableType.DIMENSION)) {
						wc.addPropertyChangeListener("columnMasked",
								new WeakPropertyChangeListener(wc,
										"columnMasked", this.rebuildListener));
						wc.addPropertyChangeListener("columnRename",
								new WeakPropertyChangeListener(wc,
										"columnRename", this.rebuildListener));
					}
				}
			} else {
				wc = new WrappedColumn(c, colName, dsTable);
				dsTable.getColumns().put(wc.getName(), wc);
				// Listen to this column to modify ourselves.
				if (!dsTable.getType().equals(DataSetTableType.DIMENSION)) {
					wc.addPropertyChangeListener("columnMasked",
							new WeakPropertyChangeListener(wc, "columnMasked",
									this.rebuildListener));
					wc.addPropertyChangeListener("columnRename",
							new WeakPropertyChangeListener(wc, "columnRename",
									this.rebuildListener));
				}
			}
			unusedCols.remove(wc);
			tu.getNewColumnNameMap().put(c.getName(), wc);
			wc.setPartitionCols(nameCols);

			// If the column is in any key on this table then it is a
			// dependency for possible future linking, which must be
			// flagged.
			wc.setKeyDependency(colsUsedInKeys.contains(c));

			// If the column was in the merge table's PK, and we are
			// expecting to add the PK to the generated table's PK, then
			// add it to the generated table's PK.
			if (includeMergeTablePK
					&& Arrays.asList(mergeTablePK.getColumns()).contains(c))
				dsTablePKCols.add(wc);
		}

		// Update the three queues with relations that lead away from this
		// table.
		for (final Iterator i = new TreeSet(mergeTable.getRelations())
				.iterator(); i.hasNext();) {
			final Relation r = (Relation) i.next();

			// Allow to go back up sourceRelation if it is a loopback
			// 1:M relation and we have just merged the 1 end.
			final boolean isLoopback = r.getLoopbackRelation(this, dsTable
					.getName()) != null
					&& r.getOneKey().equals(r.getKeyForTable(mergeTable));

			// Don't go back up relation just followed unless we are doing
			// loopback.
			if (r.equals(sourceRelation) && !isLoopback)
				continue;

			// Don't excessively repeat relations.
			if (((Integer) relationCount.get(r)).intValue() <= 0)
				continue;

			// Don't follow incorrect relations, or relations
			// between incorrect keys.
			if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
					|| r.getFirstKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT)
					|| r.getSecondKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
				continue;

			// Don't follow relations to ignored tables.
			if (r.getOtherKey(r.getKeyForTable(mergeTable)).getTable()
					.isMasked())
				continue;

			// Don't follow relations to masked schemas.
			if (r.getOtherKey(r.getKeyForTable(mergeTable)).getTable()
					.getSchema().isMasked())
				continue;

			// Don't follow masked relations.
			// NB. This is last so that only masked relations show
			// up, not those skipped for other reasons.
			if (r.isMaskRelation(this, dsTable.getName())) {
				// Make a fake SKIP table unit to show what
				// might still be possible for the user.
				final Key skipKey = r.getKeyForTable(mergeTable);
				final List newSourceDSCols = new ArrayList();
				for (int j = 0; j < skipKey.getColumns().length; j++) {
					final DataSetColumn col = tu.getDataSetColumnFor(skipKey
							.getColumns()[j]);
					newSourceDSCols.add(col);
				}
				final SkipTable stu = new SkipTable(tu, skipKey.getTable(),
						newSourceDSCols, skipKey, r, ((Integer) relationCount
								.get(r)).intValue());
				dsTable.addTransformationUnit(stu);
				continue;
			}

			// Decrement the relation counter.
			relationCount.put(r, new Integer(((Integer) relationCount.get(r))
					.intValue() - 1));

			// Set up a holder to indicate whether or not to follow
			// the relation.
			boolean followRelation = false;
			boolean forceFollowRelation = false;

			// Are we at the 1 end of a 1:M?
			// If so, we may need to make a dimension, a subclass, or
			// a concat column.
			if (r.isOneToMany() && r.getOneKey().getTable().equals(mergeTable)) {

				// Subclass subclassed relations, if we are currently
				// not building a dimension table.
				if (r.isSubclassRelation(this)
						&& !dsTable.getType()
								.equals(DataSetTableType.DIMENSION)) {
					final List newSourceDSCols = new ArrayList();
					for (int j = 0; j < r.getOneKey().getColumns().length; j++)
						newSourceDSCols.add(tu.getDataSetColumnFor(r
								.getOneKey().getColumns()[j]));
					// Deal with recursive subclasses.
					final int nextSC = subclassCount.containsKey(r) ? ((Integer) subclassCount
							.get(r)).intValue() + 1
							: 0;
					subclassCount.put(r, new Integer(nextSC));
					// Only do this if the subclassCount is less than
					// the maximum allowed.
					final CompoundRelationDefinition def = r
							.getCompoundRelation(this, dsTable.getName());
					final int childCompounded = def == null ? 1 : def.getN();
					if (nextSC < childCompounded)
						subclassQ.add(new Object[] { newSourceDSCols, r,
								new Integer(nextSC) });
				}

				// Dimensionize dimension relations, which are all other 1:M
				// relations, if we are not constructing a dimension
				// table, and are currently intending to construct
				// dimensions.
				else if (makeDimensions
						&& !dsTable.getType()
								.equals(DataSetTableType.DIMENSION)) {
					final List newSourceDSCols = new ArrayList();
					for (int j = 0; j < r.getOneKey().getColumns().length; j++) {
						final DataSetColumn newCol = tu.getDataSetColumnFor(r
								.getOneKey().getColumns()[j]);
						newSourceDSCols.add(newCol);
					}
					int childCompounded = 1;
					final CompoundRelationDefinition def = r
							.getCompoundRelation(this, dsTable.getName());
					if (def != null && def.isParallel())
						childCompounded = def.getN();
					// Follow the relation.
					for (int k = 0; k < childCompounded; k++)
						dimensionQ.add(new Object[] { newSourceDSCols, r,
								new Integer(k) });
					if (r.isMergeRelation(this)
							|| r.getUnrolledRelation(this) != null)
						forceFollowRelation = true;
				}

				// Forcibly follow forced or loopback relations.
				else if (r.getLoopbackRelation(this, dsTable.getName()) != null
						|| r.isForceRelation(this, dsTable.getName()))
					forceFollowRelation = true;
			}

			// Follow all others. Don't follow relations that are
			// already in the subclass or dimension queues.
			else
				followRelation = true;

			// If we follow a 1:1, and we are currently
			// including dimensions, include them from the 1:1 as well.
			// Otherwise, stop including dimensions on subsequent tables.
			if (followRelation || forceFollowRelation) {

				// Don't follow unrolled relations.
				final Column nameCol = r.getUnrolledRelation(this);
				if (nameCol != null) {
					// From M end, skip.
					if (mergeTable.equals(r.getManyKey().getTable()))
						continue;
					// From 1 end, do unroll.
					// Make an UNROLL unit that involves the relation and
					// includes the name column, the source and target keys,
					// and the unrolled ID and name UnrolledColumns.
					final List newSourceDSCols = new ArrayList();
					// Add UnrolledColumns for relation.
					String colName = "";
					// Add partitioning prefixes.
					for (int k = 0; k < nameCols.size(); k++) {
						final String pcolName = (String) nameCols.get(k);
						final String suffix = nameColSuffixes.size() <= k ? ""
								: "#" + (String) nameColSuffixes.get(k);
						colName = pcolName + suffix
								+ Resources.get("columnnameSep") + colName;
					}
					// Reuse columns.
					final String unrolledID = colName
							+ Resources.get("unrolledIDColName");
					final String unrolledName = colName
							+ Resources.get("unrolledNameColName");
					UnrolledColumn unrolledIDCol;
					if (dsTable.getColumns().containsKey(unrolledID)) {
						final DataSetColumn candidate = (DataSetColumn) dsTable
								.getColumns().get(unrolledID);
						if (candidate instanceof UnrolledColumn)
							unrolledIDCol = (UnrolledColumn) candidate;
						else {
							unrolledIDCol = new UnrolledColumn(unrolledID,
									dsTable);
							dsTable.getColumns().put(unrolledID, unrolledIDCol);
						}
					} else {
						unrolledIDCol = new UnrolledColumn(unrolledID, dsTable);
						dsTable.getColumns().put(unrolledID, unrolledIDCol);
					}
					UnrolledColumn unrolledNameCol;
					if (dsTable.getColumns().containsKey(unrolledName)) {
						final DataSetColumn candidate = (DataSetColumn) dsTable
								.getColumns().get(unrolledName);
						if (candidate instanceof UnrolledColumn)
							unrolledNameCol = (UnrolledColumn) candidate;
						else {
							unrolledNameCol = new UnrolledColumn(unrolledName,
									dsTable);
							dsTable.getColumns().put(unrolledName,
									unrolledNameCol);
						}
					} else {
						unrolledNameCol = new UnrolledColumn(unrolledName,
								dsTable);
						dsTable.getColumns().put(unrolledName, unrolledNameCol);
					}
					unusedCols.remove(unrolledIDCol);
					unusedCols.remove(unrolledNameCol);
					newSourceDSCols.add(unrolledIDCol);
					newSourceDSCols.add(unrolledNameCol);
					// Create UnrollTable transformation unit.
					dsTable.addTransformationUnit(new UnrollTable(tu, r,
							newSourceDSCols, nameCol, unrolledIDCol,
							unrolledNameCol));
					this.includedRelations.add(r);
					dsTable.includedRelations.add(r);
					// Skip onto next relation.
					continue;
				}

				final List nextNameCols = new ArrayList(nameCols);
				final Map nextNameColSuffixes = new HashMap();
				nextNameColSuffixes.put("" + 0, new ArrayList(nameColSuffixes));

				final Key sourceKey = r.getKeyForTable(mergeTable);
				final Key targetKey = r.getOtherKey(sourceKey);
				final List newSourceDSCols = new ArrayList();
				for (int j = 0; j < sourceKey.getColumns().length; j++)
					newSourceDSCols.add(tu.getDataSetColumnFor(sourceKey
							.getColumns()[j]));
				// Repeat queueing of relation N times if compounded.
				int childCompounded = 1;
				// Don't compound if loopback and we just processed the M end.
				final boolean skipCompound = r.getLoopbackRelation(this,
						dsTable.getName()) != null
						&& r.getManyKey().equals(r.getKeyForTable(mergeTable));
				if (!skipCompound) {
					final CompoundRelationDefinition def = r
							.getCompoundRelation(this, dsTable.getName());
					if (def != null && def.isParallel())
						childCompounded = def.getN();
					else {
						// Work out partition compounding. Table
						// applies within a dimension, where dataset does
						// not apply, but outside a dimension only dataset
						// applies.
						PartitionTableApplication usefulPart = dsTable
								.getPartitionTableApplication();
						if (usefulPart == null
								&& dsTable.equals(this.getMainTable()))
							usefulPart = this.partitionTableApplication;
						if (usefulPart == null)
							childCompounded = 1;
						else // When partitioning second level, only fork at
						// top.
						if (usefulPart.getPartitionAppliedRows().size() > 1
								&& previousUnit == null) {
							usefulPart.syncCounts();
							// Get the row information for the relation.
							final PartitionAppliedRow prow = (PartitionAppliedRow) usefulPart
									.getPartitionAppliedRows().get(1);
							childCompounded = prow.getCompound();
							nextNameCols.add(prow.getNamePartitionCol());
						} else
							childCompounded = 1;
					}
				}
				for (int k = 1; k <= childCompounded; k++) {
					final List nextList = new ArrayList(nameColSuffixes);
					nextList.add("" + k);
					nextNameColSuffixes.put("" + k, nextList);
				}
				// Insert first one at next position in queue
				// after current position. This creates multiple
				// top-down paths, rather than sideways-spanning trees of
				// actions. (If this queue were a graph, doing it this way
				// makes it depth-first as opposed to breadth-first).
				// The queue position is incremented so that they remain
				// in order - else they'd end up reversed.
				for (int k = 0; k < childCompounded; k++)
					normalQ.add(queuePos++, new Object[] {
							r,
							newSourceDSCols,
							targetKey.getTable(),
							tu,
							Boolean.valueOf(makeDimensions && r.isOneToOne()
									|| forceFollowRelation), new Integer(k),
							nextNameCols, nextNameColSuffixes.get("" + k),
							new HashMap(relationCount) });
			}
		}
	}

	/**
	 * Returns the central table of this dataset.
	 * 
	 * @return the central table of this dataset.
	 */
	public Table getCentralTable() {
		return this.centralTable;
	}

	/**
	 * Follows subclassed relations to find where transformation should really
	 * start for this dataset.
	 * 
	 * @return the real central table.
	 */
	public Table getRealCentralTable() {
		Log.debug("Finding actual central table");
		// Identify main table.
		final Table realCentralTable = this.getCentralTable();
		Table centralTable = realCentralTable;
		// If central table has subclass relations and is at the M key
		// end, then follow them to the real central table.
		boolean found;
		do {
			found = false;
			for (final Iterator i = centralTable.getForeignKeys().iterator(); i
					.hasNext()
					&& !found;)
				for (final Iterator j = ((ForeignKey) i.next()).getRelations()
						.iterator(); j.hasNext() && !found;) {
					final Relation rel = (Relation) j.next();
					if (rel.isSubclassRelation(this)) {
						centralTable = rel.getOneKey().getTable();
						found = true;
					}
				}
		} while (found && centralTable != realCentralTable);
		Log.debug("Actual central table is " + centralTable);
		return centralTable;
	}

	/**
	 * Returns the central table of this dataset.
	 * 
	 * @return the central table of this dataset.
	 */
	public DataSetTable getMainTable() {
		for (final Iterator i = this.getTables().values().iterator(); i
				.hasNext();) {
			final DataSetTable dst = (DataSetTable) i.next();
			if (dst.getType().equals(DataSetTableType.MAIN))
				return dst;
		}
		// Should never happen.
		throw new BioMartError();
	}

	/**
	 * Returns the post-creation optimiser type this dataset will use.
	 * 
	 * @return the optimiser type that will be used.
	 */
	public DataSetOptimiserType getDataSetOptimiserType() {
		return this.optimiser;
	}

	/**
	 * Sees if the optimiser will index its columns.
	 * 
	 * @return <tt>true</tt> if it will.
	 */
	public boolean isIndexOptimiser() {
		return this.indexOptimiser;
	}

	/**
	 * Test to see if this dataset is invisible.
	 * 
	 * @return <tt>true</tt> if it is invisible, <tt>false</tt> otherwise.
	 */
	public boolean isInvisible() {
		return this.invisible;
	}

	/**
	 * Sets the post-creation optimiser type this dataset will use.
	 * 
	 * @param optimiser
	 *            the optimiser type to use.
	 */
	public void setDataSetOptimiserType(final DataSetOptimiserType optimiser) {
		Log.debug("Setting optimiser to " + optimiser + " in " + this);
		final DataSetOptimiserType oldValue = this.optimiser;
		if (oldValue.equals(optimiser))
			return;
		this.optimiser = optimiser;
		this.pcs.firePropertyChange("optimiserType", oldValue, optimiser);
	}

	/**
	 * Sets the optimiser index type.
	 * 
	 * @param index
	 *            the optimiser index if <tt>true</tt>.
	 */
	public void setIndexOptimiser(final boolean index) {
		Log.debug("Setting optimiser index to " + index + " in " + this);
		final boolean oldValue = this.indexOptimiser;
		if (oldValue == index)
			return;
		this.indexOptimiser = index;
		this.pcs.firePropertyChange("indexOptimiser", oldValue, index);
	}

	/**
	 * Sets the inivisibility of this dataset.
	 * 
	 * @param invisible
	 *            <tt>true</tt> if it is invisible, <tt>false</tt>
	 *            otherwise.
	 */
	public void setInvisible(final boolean invisible) {
		Log.debug("Setting invisible flag to " + invisible + " in " + this);
		final boolean oldValue = this.invisible;
		if (oldValue == invisible)
			return;
		this.invisible = invisible;
		this.pcs.firePropertyChange("invisible", oldValue, invisible);
	}

	/**
	 * Synchronise this dataset with the schema that is providing its tables.
	 * Synchronisation means checking the columns and relations and removing any
	 * that have disappeared. The dataset is then regenerated. After
	 * regeneration, any customisations to the dataset such as partitioning are
	 * reapplied to columns which match the original names of the columns from
	 * before regeneration.
	 * 
	 * @throws SQLException
	 *             never thrown - this is inherited from {@link Schema} but does
	 *             not apply here because we are not doing any database
	 *             communications.
	 * @throws DataModelException
	 *             never thrown - this is inherited from {@link Schema} but does
	 *             not apply here because we are not attempting any new logic
	 *             with the schema.
	 */
	public void synchronise() throws SQLException, DataModelException {
		Log.debug("Regenerating dataset " + this.getName());
		super.synchronise();

		// Empty out used rels and schs.
		this.includedRelations.clear();
		this.includedSchemas.clear();
		this.includedTables.clear();

		// Make a list of all table names.
		final Collection unusedTables = new HashSet(this.getTables().values());
		try {
			// Generate the main table. It will recursively generate all the
			// others.
			this.generateDataSetTable(DataSetTableType.MAIN, null, this
					.getRealCentralTable(), Collections.EMPTY_LIST, null,
					new HashMap(), 0, unusedTables);
		} catch (final PartitionException pe) {
			throw new DataModelException(pe);
		}

		// Drop any rels from tables still in list, then drop tables too.
		for (final Iterator i = unusedTables.iterator(); i.hasNext();) {
			final Table deadTbl = (Table) i.next();
			for (final Iterator j = deadTbl.getKeys().iterator(); j.hasNext();) {
				final Key key = (Key) j.next();
				for (final Iterator r = key.getRelations().iterator(); r
						.hasNext();) {
					final Relation rel = (Relation) r.next();
					rel.getFirstKey().getRelations().remove(rel);
					rel.getSecondKey().getRelations().remove(rel);
				}
			}
			deadTbl.setPrimaryKey(null);
			deadTbl.getForeignKeys().clear();
			this.getTables().remove(deadTbl.getName());
			this.mods.remove(deadTbl.getName());
		}

		// Add us as a listener to mart's schemas to remove ourselves
		// if our central table's parent schema is removed.
		this.getMart().getSchemas().addPropertyChangeListener(
				new WeakPropertyChangeListener(this.getMart().getSchemas(),
						this.existenceListener));

		// Add us as a listener to the schema's tables so that
		// if the central table is removed, so are we.
		this.centralTable.getSchema().getTables().addPropertyChangeListener(
				new WeakPropertyChangeListener(this.centralTable.getSchema()
						.getTables(), this.existenceListener));

		// Add us as a listener to all included rels and schs, replacing
		// ourselves if we are already listening to them.
		for (final Iterator i = this.includedSchemas.iterator(); i.hasNext();) {
			final Schema sch = (Schema) i.next();
			sch.addPropertyChangeListener("masked",
					new WeakPropertyChangeListener(sch, "masked",
							this.rebuildListener));
		}
		// Gather up the tables we have used and those linked to them.
		final Set listeningTables = new HashSet();
		for (final Iterator i = this.includedRelations.iterator(); i.hasNext();) {
			final Relation rel = (Relation) i.next();
			// Don't bother listening to tables at end of incorrect
			// relations - but those that are excluded because of
			// non-relation-related reasons can be listened to.
			if (!rel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) {
				listeningTables.add(rel.getFirstKey().getTable());
				listeningTables.add(rel.getSecondKey().getTable());
			}
		}
		// Listen to the tables and their children.
		final Set listeningRels = new HashSet();
		for (final Iterator i = listeningTables.iterator(); i.hasNext();) {
			final Table tbl = (Table) i.next();
			listeningRels.addAll(tbl.getRelations());
			// Listen only to useful things.
			tbl.addPropertyChangeListener("masked",
					new WeakPropertyChangeListener(tbl, "masked",
							this.rebuildListener));
			tbl.addPropertyChangeListener("restrictTable",
					new WeakPropertyChangeListener(tbl, "restrictTable",
							this.rebuildListener));
			tbl.getColumns().addPropertyChangeListener(
					new WeakPropertyChangeListener(tbl.getColumns(),
							this.rebuildListener));
			tbl.getRelations().addPropertyChangeListener(
					new WeakPropertyChangeListener(tbl.getRelations(),
							this.rebuildListener));
		}
		// Listen to useful bits of the relation.
		for (final Iterator i = listeningRels.iterator(); i.hasNext();) {
			final Relation rel = (Relation) i.next();
			rel.addPropertyChangeListener("cardinality",
					new WeakPropertyChangeListener(rel, "cardinality",
							this.rebuildListener));
			rel.addPropertyChangeListener("status",
					new WeakPropertyChangeListener(rel, "status",
							this.rebuildListener));
			rel.addPropertyChangeListener("compoundRelation",
					new WeakPropertyChangeListener(rel, "compoundRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("unrolledRelation",
					new WeakPropertyChangeListener(rel, "unrolledRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("forceRelation",
					new WeakPropertyChangeListener(rel, "forceRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("loopbackRelation",
					new WeakPropertyChangeListener(rel, "loopbackRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("maskRelation",
					new WeakPropertyChangeListener(rel, "maskRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("mergeRelation",
					new WeakPropertyChangeListener(rel, "mergeRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("restrictRelation",
					new WeakPropertyChangeListener(rel, "restrictRelation",
							this.rebuildListener));
			rel.addPropertyChangeListener("subclassRelation",
					new WeakPropertyChangeListener(rel, "subclassRelation",
							this.rebuildListener));
		}
	}

	public boolean isMasked() {
		// Is the table or schema ignored?
		if (this.centralTable.isMasked()
				|| this.centralTable.getSchema().isMasked())
			return true;
		else
			return super.isMasked();
	}

	/**
	 * Find out what schemas are used in this dataset, in the order they were
	 * used.
	 * 
	 * @return the set of schemas used.
	 */
	public Collection getIncludedSchemas() {
		return this.includedSchemas;
	}

	/**
	 * A column on a dataset table has to be one of the types of dataset column
	 * available from this class.
	 * 
	 * DataSetColumns don't change, and so they don't provide any change
	 * listeners of any kind. They are always (re)created from scratch.
	 */
	public static class DataSetColumn extends Column {
		private static final long serialVersionUID = 1L;

		private boolean keyDependency;

		private boolean expressionDependency;

		private String partitionedName = null;

		private final List partitionCols = new ArrayList();

		/**
		 * This constructor gives the column a name.
		 * 
		 * @param name
		 *            the name to give this column.
		 * @param dsTable
		 *            the parent dataset table.
		 */
		public DataSetColumn(final String name, final DataSetTable dsTable) {
			// Call the super constructor using the alias generator to
			// ensure we have a unique name.
			super(dsTable, name);

			Log.debug("Creating dataset column " + name + " of type "
					+ this.getClass().getName());

			// Set up default mask/partition values.
			this.keyDependency = false;
			this.expressionDependency = false;

			// Listen to own settings.
			final PropertyChangeListener listener = new PropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent e) {
					DataSetColumn.this.setDirectModified(true);
				}
			};
			this.pcs.addPropertyChangeListener("columnMasked", listener);
			this.pcs.addPropertyChangeListener("columnRename", listener);
			this.pcs.addPropertyChangeListener("columnIndexed", listener);
		}

		/**
		 * Obtain the dataset this column belongs to.
		 * 
		 * @return the dataset it belongs to.
		 */
		public DataSet getDataSet() {
			return (DataSet) this.getTable().getSchema();
		}

		/**
		 * Obtain the dataset table this column belongs to.
		 * 
		 * @return the dataset table it belongs to.
		 */
		public DataSetTable getDataSetTable() {
			return (DataSetTable) this.getTable();
		}

		/**
		 * Get the named set of properties for this column.
		 * 
		 * @param property
		 *            the property to look up.
		 * @return the set of column names the property applies to.
		 */
		protected Map getMods(final String property) {
			return this.getDataSet().getMods(this.getDataSetTable().getName(),
					property);
		}

		/**
		 * Update the partition column list on this column to use the names.
		 * 
		 * @param partCols
		 *            the new list of partition column names that apply.
		 */
		public void setPartitionCols(final List partCols) {
			this.partitionCols.clear();
			this.partitionCols.addAll(partCols);
		}

		/**
		 * Fixes the name of this column to what it will be after partitioning
		 * has been applied using the current rows.
		 * 
		 * @param pta
		 *            the current partition table application.
		 * @throws PartitionException
		 *             if it could not get the values.
		 */
		public void fixPartitionedName(final PartitionTableApplication pta)
				throws PartitionException {
			final StringBuffer buf = new StringBuffer();
			for (final Iterator i = this.partitionCols.iterator(); i.hasNext();) {
				final String pcolName = (String) i.next();
				final PartitionColumn pcol = (PartitionColumn) pta
						.getPartitionTable().getColumns().get(pcolName);
				buf.append(pcol.getValueForRow(pcol.getPartitionTable()
						.currentRow()));
				buf.append(Resources.get("columnnameSep"));
			}
			String rest = this.getModifiedName();
			if (rest.indexOf("__") >= 0)
				rest = rest.substring(rest.lastIndexOf("__") + 2);
			buf.append(rest);
			this.partitionedName = buf.toString();
			// UC/LC/Mixed?
			switch (this.getDataSet().getMart().getCase()) {
			case Mart.USE_LOWER_CASE:
				this.partitionedName = this.partitionedName.toLowerCase();
				break;
			case Mart.USE_UPPER_CASE:
				this.partitionedName = this.partitionedName.toUpperCase();
				break;
			default:
				// Leave as-is.
				break;
			}
		}

		/**
		 * Get the name of this column after partitioning has been applied. Must
		 * call {@link #fixPartitionedName(PartitionTableApplication)} first
		 * else it will delegate to {@link #getModifiedName()}.
		 * 
		 * @return the partitioned name.
		 */
		public String getPartitionedName() {
			if (this.partitionedName == null)
				return this.getModifiedName();
			else
				return this.partitionedName;
		}

		/**
		 * Test to see if this column is required during intermediate
		 * construction phases.
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isRequiredInterim() {
			return this.keyDependency || this.expressionDependency
					|| !this.isColumnMasked();
		}

		/**
		 * Test to see if this column is required in the final completed dataset
		 * table.
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isRequiredFinal() {
			// Masked columns are not final.
			if (this.isColumnMasked())
				return false;
			// If appears in aliases on any group-by expression column
			// then is not required final.
			for (final Iterator i = this.getTable().getColumns().values()
					.iterator(); i.hasNext();) {
				final DataSetColumn dsCol = (DataSetColumn) i.next();
				if (dsCol instanceof ExpressionColumn) {
					final ExpressionColumnDefinition entry = ((ExpressionColumn) dsCol)
							.getDefinition();
					if (entry.isGroupBy()
							&& entry.getAliases().containsKey(this.getName()))
						return false;
				}
			}
			// By default if we reach here, we are final.
			return true;
		}

		/**
		 * Return this modified name including any renames etc.
		 * 
		 * @return the modified name.
		 */
		public String getModifiedName() {
			String name = this.getColumnRename();
			if (name == null)
				name = this.getName();
			// UC/LC/Mixed?
			switch (this.getDataSet().getMart().getCase()) {
			case Mart.USE_LOWER_CASE:
				return name.toLowerCase();
			case Mart.USE_UPPER_CASE:
				return name.toUpperCase();
			default:
				return name;
			}
		}

		/**
		 * Changes the dependency flag on this column.
		 * 
		 * @param dependency
		 *            the new dependency flag. <tt>true</tt> indicates that
		 *            this column is required for the fundamental structure of
		 *            the dataset table to exist. The column will get selected
		 *            regardless of it's masking flag. However, if it is masked,
		 *            it will be removed again after the dependency is
		 *            satisified.
		 */
		public void setKeyDependency(final boolean dependency) {
			this.keyDependency = dependency;
		}

		/**
		 * Changes the dependency flag on this column.
		 * 
		 * @param dependency
		 *            the new dependency flag. <tt>true</tt> indicates that
		 *            this column is required for the fundamental structure of
		 *            the dataset table to exist. The column will get selected
		 *            regardless of it's masking flag. However, if it is masked,
		 *            it will be removed again after the dependency is
		 *            satisified.
		 */
		public void setExpressionDependency(final boolean dependency) {
			this.expressionDependency = dependency;
		}

		/**
		 * Is this column required as a dependency?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isKeyDependency() {
			return this.keyDependency;
		}

		/**
		 * Is this column required as a dependency?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isExpressionDependency() {
			return this.expressionDependency;
		}

		private boolean isKeyCol() {
			// Are we in our table's PK or FK?
			final Set cols = new HashSet();
			for (final Iterator i = this.getDataSetTable().getKeys().iterator(); i
					.hasNext();)
				cols.addAll(Arrays.asList(((Key) i.next()).getColumns()));
			return cols.contains(this);
		}

		/**
		 * Is this a masked column?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isColumnMasked() {
			return this.getMods("columnMasked").containsKey(this.getName());
		}

		/**
		 * Mask this column.
		 * 
		 * @param columnMasked
		 *            <tt>true</tt> to mask.
		 * @throws ValidationException
		 *             if masking is not possible.
		 */
		public void setColumnMasked(final boolean columnMasked)
				throws ValidationException {
			final boolean oldValue = this.isColumnMasked();
			if (columnMasked == oldValue)
				return;
			if (columnMasked && this.isKeyCol())
				throw new ValidationException(Resources
						.get("cannotMaskNecessaryColumn"));
			if (columnMasked)
				this.getMods("columnMasked").put(this.getName(), null);
			else
				this.getMods("columnMasked").remove(this.getName());
			this.pcs.firePropertyChange("columnMasked", oldValue, columnMasked);
		}

		/**
		 * Is this an indexed column?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isColumnIndexed() {
			return this.getMods("columnIndexed").containsKey(this.getName());
		}

		/**
		 * Index this column.
		 * 
		 * @param columnIndexed
		 *            <tt>true</tt> to index.
		 */
		public void setColumnIndexed(final boolean columnIndexed) {
			final boolean oldValue = this.isColumnIndexed();
			if (columnIndexed == oldValue)
				return;
			if (columnIndexed)
				this.getMods("columnIndexed").put(this.getName(), null);
			else
				this.getMods("columnIndexed").remove(this.getName());
			this.pcs.firePropertyChange("columnIndexed", oldValue,
					columnIndexed);
		}

		/**
		 * Is this a renamed column?
		 * 
		 * @return <tt>null</tt> if it is not, otherwise return the new name.
		 */
		public String getColumnRename() {
			return (String) this.getMods("columnRename").get(this.getName());
		}

		/**
		 * Rename this column.
		 * 
		 * @param columnRename
		 *            the new name, or <tt>null</tt> to undo it.
		 * @throws ValidationException
		 *             if it could not be done.
		 */
		public void setColumnRename(String columnRename)
				throws ValidationException {
			String oldValue = this.getColumnRename();
			if (columnRename == oldValue || oldValue != null
					&& oldValue.equals(columnRename))
				return;
			if (oldValue == null)
				oldValue = this.getName();
			// Make the name unique.
			if (columnRename != null) {
				final Set entries = new HashSet();
				// Get renames of siblings.
				for (final Iterator i = this.getTable().getColumns().values()
						.iterator(); i.hasNext();)
					entries.add(((DataSetColumn) i.next()).getModifiedName());
				entries.remove(oldValue);
				// First we need to find out the base name, ie. the bit
				// we append numbers to make it unique, but before any
				// key suffix. If we appended numbers after the key
				// suffix then it would confuse MartEditor.
				String keySuffix = Resources.get("keySuffix");
				String baseName = columnRename;
				if (columnRename.endsWith(keySuffix))
					baseName = columnRename.substring(0, columnRename
							.indexOf(keySuffix));
				else if (!this.isKeyCol())
					keySuffix = "";
				columnRename = baseName + keySuffix;
				// Now, if the old name has a partition prefix, and the
				// new one doesn't, reinstate or replace it.
				if (this.getName().indexOf("__") >= 0) {
					if (columnRename.indexOf("__") >= 0)
						columnRename = columnRename.substring(columnRename
								.lastIndexOf("__") + 2);
					columnRename = this.getName().substring(0,
							this.getName().lastIndexOf("__") + 2)
							+ columnRename;
				}
				// Now simply check to see if the name is used, and
				// then add an incrementing number to it until it is unique.
				int suffix = 1;
				while (entries.contains(columnRename))
					columnRename = baseName + "_" + suffix++ + keySuffix;
			}
			// Check and change it.
			if (columnRename != null)
				this.getMods("columnRename").put(this.getName(), columnRename);
			else
				this.getMods("columnRename").remove(this.getName());
			this.pcs.firePropertyChange("columnRename", oldValue, columnRename);
		}

		/**
		 * A column on a dataset table that is an expression bringing together
		 * values from other columns. Those columns should be marked with a
		 * dependency flag to indicate that they are still needed even if
		 * otherwise masked. If this is the case, they can be dropped after the
		 * dependent expression column has been added.
		 * <p>
		 * Note that all expression columns should be added in a single step.
		 */
		public static class ExpressionColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

			private final ExpressionColumnDefinition definition;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param definition
			 *            the definition of this column's expression.
			 */
			public ExpressionColumn(final String name,
					final DataSetTable dsTable,
					final ExpressionColumnDefinition definition) {
				// The super constructor will make the alias for us.
				super(name, dsTable);
				this.definition = definition;

				definition.addPropertyChangeListener("directModified",
						new PropertyChangeListener() {
							public void propertyChange(
									final PropertyChangeEvent e) {
								ExpressionColumn.this.setDirectModified(true);
							}
						});
				this.visibleModified = false;
			}

			/**
			 * Obtain the expression behind this column.
			 * 
			 * @return the expression.
			 */
			public ExpressionColumnDefinition getDefinition() {
				return this.definition;
			}

			public void setColumnMasked(final boolean columnMasked)
					throws ValidationException {
				if (columnMasked == this.isColumnMasked())
					return;
				if (columnMasked)
					throw new ValidationException(Resources
							.get("cannotMaskExpressionColumn"));
				super.setColumnMasked(columnMasked);
			}
		}

		/**
		 * A column on a dataset table that is an unrolled column.
		 */
		public static class UnrolledColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

			/**
			 * This constructor gives the column a name.
			 * 
			 * @param name
			 *            the name to give the unrolled column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 */
			public UnrolledColumn(final String name, final DataSetTable dsTable) {
				// The super constructor will make the alias for us.
				super(name, dsTable);
				this.visibleModified = false;
			}

			public void setColumnMasked(final boolean columnMasked)
					throws ValidationException {
				if (columnMasked == this.isColumnMasked())
					return;
				if (columnMasked)
					throw new ValidationException(Resources
							.get("cannotMaskUnrolledColumn"));
				super.setColumnMasked(columnMasked);
			}
		}

		/**
		 * A column on a dataset table that is inherited from a parent dataset
		 * table.
		 */
		public static class InheritedColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

			private DataSetColumn dsColumn;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here. The name is inherited from the column too.
			 * 
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param dsColumn
			 *            the column to inherit.
			 */
			public InheritedColumn(final DataSetTable dsTable,
					final DataSetColumn dsColumn) {
				// The super constructor will make the alias for us.
				super(dsColumn.getModifiedName(), dsTable);
				// Remember the inherited column.
				this.dsColumn = dsColumn;
				this.visibleModified = false;
			}

			/**
			 * Returns the column that has been inherited by this column.
			 * 
			 * @return the inherited column.
			 */
			public DataSetColumn getInheritedColumn() {
				return this.dsColumn;
			}

			public String getModifiedName() {
				return this.getName();
			}

			public void setColumnMasked(final boolean columnMasked)
					throws ValidationException {
				if (columnMasked == this.isColumnMasked())
					return;
				if (columnMasked)
					throw new ValidationException(Resources
							.get("cannotMaskInheritedColumn"));
				super.setColumnMasked(columnMasked);
			}

			public void setColumnRename(final String columnRename)
					throws ValidationException {
				final String oldValue = this.getColumnRename();
				if (columnRename == oldValue || oldValue != null
						&& oldValue.equals(columnRename))
					return;
				if (columnRename != null)
					throw new ValidationException(Resources
							.get("cannotRenameInheritedColumn"));
				super.setColumnRename(columnRename);
			}
		}

		/**
		 * A column on a dataset table that wraps an existing column but is
		 * otherwise identical to a normal column. It assigns itself an alias if
		 * the original name is already used in the dataset table.
		 */
		public static class WrappedColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

			private final Column column;

			/**
			 * This constructor wraps an existing column. It also assigns an
			 * alias to the wrapped column if another one with the same name
			 * already exists on this table.
			 * 
			 * @param column
			 *            the column to wrap.
			 * @param colName
			 *            the name to give the wrapped column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 */
			public WrappedColumn(final Column column, final String colName,
					final DataSetTable dsTable) {
				// Call the parent which will use the alias generator for us.
				super(colName, dsTable);

				// Remember the wrapped column.
				this.column = column;
			}

			/**
			 * Returns the wrapped column.
			 * 
			 * @return the wrapped {@link Column}.
			 */
			public Column getWrappedColumn() {
				return this.column;
			}
		}
	}

	/**
	 * This class defines the various different ways of optimising a dataset
	 * after it has been constructed, eg. adding boolean columns.
	 */
	public static class DataSetOptimiserType implements Comparable {
		private static final long serialVersionUID = 1L;

		/**
		 * Use this constant to refer to no optimisation.
		 */
		public static final DataSetOptimiserType NONE = new DataSetOptimiserType(
				"NONE", false, false, false);

		/**
		 * Parent tables will inherit copies of count columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_INHERIT = new DataSetOptimiserType(
				"COLUMN_INHERIT", false, false, false);

		/**
		 * Parent tables will inherit copies of count tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_INHERIT = new DataSetOptimiserType(
				"TABLE_INHERIT", false, true, false);

		/**
		 * Parent tables will inherit copies of bool columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL_INHERIT = new DataSetOptimiserType(
				"COLUMN_BOOL_INHERIT", true, false, false);

		/**
		 * Parent tables will inherit copies of bool tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_BOOL_INHERIT = new DataSetOptimiserType(
				"TABLE_BOOL_INHERIT", true, true, false);

		/**
		 * Parent tables will inherit copies of bool columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL_NULL_INHERIT = new DataSetOptimiserType(
				"COLUMN_BOOL_NULL_INHERIT", true, false, true);

		/**
		 * Parent tables will inherit copies of bool tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_BOOL_NULL_INHERIT = new DataSetOptimiserType(
				"TABLE_BOOL_NULL_INHERIT", true, true, true);

		private final String name;

		private final boolean bool;

		private final boolean table;

		private final boolean useNull;

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this optimiser type object will display when printed.
		 * 
		 * @param name
		 *            the name of the optimiser type.
		 * @param bool
		 *            <tt>true</tt> if bool values (0,1) should be used
		 *            instead of counts.
		 * @param table
		 *            <tt>true</tt> if columns should live in their own
		 *            tables.
		 * @param useNull
		 *            if this is a bool column, use null/1 instead of 0/1.
		 */
		private DataSetOptimiserType(final String name, final boolean bool,
				final boolean table, final boolean useNull) {
			this.name = name;
			this.bool = bool;
			this.table = table;
			this.useNull = useNull;
		}

		public int compareTo(final Object o) throws ClassCastException {
			final DataSetOptimiserType c = (DataSetOptimiserType) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}

		/**
		 * Displays the name of this optimiser type object.
		 * 
		 * @return the name of this optimiser type object.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return <tt>true</tt> if columns counts should be replaced by 0/1
		 * boolean-style values.
		 * 
		 * @return <tt>true</tt> if columns counts should be replaced by 0/1
		 *         boolean-style values.
		 */
		public boolean isBool() {
			return this.bool;
		}

		/**
		 * Return <tt>true</tt> if columns 0/1 values should be replaced by
		 * null/1 equivalents.
		 * 
		 * @return <tt>true</tt> if columns 0/1 values should be replaced by
		 *         null/1 equivalents.
		 */
		public boolean isUseNull() {
			return this.useNull;
		}

		/**
		 * Return <tt>true</tt> if columns should live in their own table.
		 * 
		 * @return <tt>true</tt> if columns should live in their own table.
		 */
		public boolean isTable() {
			return this.table;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The method simply returns the name of the optimiser type.
		 */
		public String toString() {
			return this.getName();
		}

		/**
		 * Return the types of optimiser column available.
		 * 
		 * @return the types available. Keys are internal names for the types,
		 *         values are the actual type instances.
		 */
		public static Map getTypes() {
			final Map optimiserTypes = new LinkedHashMap();
			optimiserTypes.put("None", DataSetOptimiserType.NONE);
			optimiserTypes.put("ColumnInherit",
					DataSetOptimiserType.COLUMN_INHERIT);
			optimiserTypes.put("ColumnBoolInherit",
					DataSetOptimiserType.COLUMN_BOOL_INHERIT);
			optimiserTypes.put("ColumnBoolNullInherit",
					DataSetOptimiserType.COLUMN_BOOL_NULL_INHERIT);
			optimiserTypes.put("TableInherit",
					DataSetOptimiserType.TABLE_INHERIT);
			optimiserTypes.put("TableBoolInherit",
					DataSetOptimiserType.TABLE_BOOL_INHERIT);
			optimiserTypes.put("TableBoolNullInherit",
					DataSetOptimiserType.TABLE_BOOL_NULL_INHERIT);
			return optimiserTypes;
		}
	}

	/**
	 * This special table represents the merge of one or more other tables by
	 * following a series of relations rooted in a similar series of keys. As
	 * such it has no real columns of its own, so every column is from another
	 * table and is given an alias. The tables don't last, as they are
	 * (re)created from scratch every time, and so they don't need to implement
	 * any kind of change control.
	 */
	public static class DataSetTable extends Table {
		private static final long serialVersionUID = 1L;

		private final List transformationUnits;

		private DataSetTableType type;

		private final Table focusTable;

		private final Relation focusRelation;

		private final Collection includedRelations;

		private final Collection includedTables;

		/**
		 * The constructor calls the parent table constructor. It uses a dataset
		 * as a parent schema for itself. You must also supply a type that
		 * describes this as a main table, dimension table, etc.
		 * 
		 * @param name
		 *            the table name.
		 * @param ds
		 *            the dataset to hold this table in.
		 * @param type
		 *            the type that best describes this table.
		 * @param focusTable
		 *            the schema table this dataset table starts from.
		 * @param focusRelation
		 *            the schema relation used to reach the focus table. Can be
		 *            <tt>null</tt>.
		 */
		public DataSetTable(final String name, final DataSet ds,
				final DataSetTableType type, final Table focusTable,
				final Relation focusRelation) {
			// Super constructor first, using an alias to prevent duplicates.
			super(ds, name);

			Log.debug("Creating dataset table " + name);

			// Remember the other settings.
			this.type = type;
			this.focusTable = focusTable;
			this.focusRelation = focusRelation;
			this.transformationUnits = new ArrayList();
			this.includedRelations = new LinkedHashSet();
			this.includedTables = new LinkedHashSet();

			// Listen to own settings.
			final PropertyChangeListener listener = new PropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent e) {
					DataSetTable.this.setDirectModified(true);
				}
			};
			this.pcs.addPropertyChangeListener("type", listener);
			this.pcs.addPropertyChangeListener("tableRename", listener);
			this.pcs.addPropertyChangeListener("dimensionMasked", listener);
			this.pcs.addPropertyChangeListener("distinctTable", listener);
			this.pcs.addPropertyChangeListener("partitionTableApplication",
					listener);
		}

		/**
		 * Accept changes associated with columns from the target table. If the
		 * target table is null, all changes are accepted. All affected columns
		 * have their visible modified flag reset.
		 * 
		 * @param targetTable
		 *            the target table.
		 */
		public void acceptChanges(final Table targetTable) {
			this.acceptRejectChanges(targetTable, false);
		}

		/**
		 * Reject changes associated with columns from the target table. If the
		 * target table is null, all changes are rejected. Rejection means that
		 * if the relation was modified, the relation is masked. Otherwise, the
		 * affected columns are masked instead. All affected columns have their
		 * visible modified flag reset.
		 * 
		 * @param targetTable
		 *            the target table.
		 */
		public void rejectChanges(final Table targetTable) {
			this.acceptRejectChanges(targetTable, true);
		}

		private void acceptRejectChanges(final Table targetTable,
				final boolean reject) {
			// Locate the TU that provides the target table.
			for (final Iterator i = this.getTransformationUnits().iterator(); i
					.hasNext();) {
				final TransformationUnit tu = (TransformationUnit) i.next();
				if (tu instanceof SelectFromTable
						&& (targetTable == null || ((SelectFromTable) tu)
								.getTable().equals(targetTable))) {
					final SelectFromTable st = (SelectFromTable) tu;
					// Are we rejecting?
					if (reject && st instanceof JoinTable) {
						final JoinTable jt = (JoinTable) st;
						// Is the TU relation modified?
						if (jt.getSchemaRelation().isVisibleModified()) {
							jt.getSchemaRelation().setMaskRelation(
									this.getDataSet(), this.getName(), true);
							// No more needs to be done.
							continue;
						}
					}
					// Find all new columns from the TU.
					for (final Iterator j = st.getNewColumnNameMap().values()
							.iterator(); j.hasNext();) {
						final DataSetColumn dsCol = (DataSetColumn) j.next();
						// Is it new?
						if (!dsCol.isVisibleModified())
							continue;
						// Are we rejecting?
						if (reject)
							// Mask it.
							try {
								dsCol.setColumnMasked(true);
							} catch (final ValidationException ve) {
								// Ignore - if we can't mask it, it's because
								// it's
								// important.
							}
						// Reset visible modified on all of them.
						dsCol.transactionResetVisibleModified();
					}
				}
			}
		}

		/**
		 * Do any of the current visibly modified columns on this dataset table
		 * come from the specified source table?
		 * 
		 * @param table
		 *            the table to check.
		 * @return true if they do.
		 */
		public boolean hasVisibleModifiedFrom(final Table table) {
			for (final Iterator i = this.getColumns().values().iterator(); i
					.hasNext();) {
				final DataSetColumn dsCol = (DataSetColumn) i.next();
				if (!dsCol.isVisibleModified())
					continue;
				if (dsCol instanceof WrappedColumn
						&& ((WrappedColumn) dsCol).getWrappedColumn()
								.getTable().equals(table))
					return true;
			}
			return false;
		}

		/**
		 * Obtain the dataset this table belongs to.
		 * 
		 * @return the dataset it belongs to.
		 */
		public DataSet getDataSet() {
			return (DataSet) this.getSchema();
		}

		/**
		 * Get the named set of properties for this column.
		 * 
		 * @param property
		 *            the property to look up.
		 * @return the set of column names the property applies to.
		 */
		protected Map getMods(final String property) {
			return this.getDataSet().getMods(this.getName(), property);
		}

		/**
		 * Obtain all tables used by this dataset table in the order they were
		 * used.
		 * 
		 * @return all tables.
		 */
		public Collection getIncludedTables() {
			return this.includedTables;
		}

		/**
		 * Obtain all relations used by this dataset table in the order they
		 * were used.
		 * 
		 * @return all relations.
		 */
		public Collection getIncludedRelations() {
			return this.includedRelations;
		}

		/**
		 * Return this modified name including any renames etc.
		 * 
		 * @return the modified name.
		 */
		public String getModifiedName() {
			String name = this.getTableRename();
			if (name == null)
				name = this.getName();
			// UC/LC/Mixed?
			switch (((DataSet) this.getSchema()).getMart().getCase()) {
			case Mart.USE_LOWER_CASE:
				return name.toLowerCase();
			case Mart.USE_UPPER_CASE:
				return name.toUpperCase();
			default:
				return name;
			}
		}

		/**
		 * Obtain the focus relation for this dataset table. The focus relation
		 * is the one which the transformation uses to reach the focus table.
		 * 
		 * @return the focus relation.
		 */
		public Relation getFocusRelation() {
			return this.focusRelation;
		}

		/**
		 * Obtain the focus table for this dataset table. The focus table is the
		 * one which the transformation starts from.
		 * 
		 * @return the focus table.
		 */
		public Table getFocusTable() {
			return this.focusTable;
		}

		/**
		 * Adds a transformation unit to the end of the chain.
		 * 
		 * @param tu
		 *            the unit to add.
		 */
		void addTransformationUnit(final TransformationUnit tu) {
			this.transformationUnits.add(tu);
		}

		/**
		 * Gets the ordered list of transformation units.
		 * 
		 * @return the list of units.
		 */
		public List getTransformationUnits() {
			return this.transformationUnits;
		}

		/**
		 * Returns the type of this table specified at construction time.
		 * 
		 * @return the type of this table.
		 */
		public DataSetTableType getType() {
			return this.type;
		}

		/**
		 * Changes the type of this table specified at construction time.
		 * 
		 * @param type
		 *            the type of this table. Use with care.
		 */
		public void setType(final DataSetTableType type) {
			final DataSetTableType oldValue = this.getType();
			if (type == oldValue)
				return;
			this.type = type;
			this.pcs.firePropertyChange("type", oldValue, type);
		}

		/**
		 * Return the parent table, if any, or <tt>null</tt> if none.
		 * 
		 * @return the parent table.
		 */
		public DataSetTable getParent() {
			for (final Iterator i = this.getForeignKeys().iterator(); i
					.hasNext();) {
				final ForeignKey fk = (ForeignKey) i.next();
				if (fk.getRelations().size() > 0)
					return (DataSetTable) ((Relation) fk.getRelations()
							.iterator().next()).getOneKey().getTable();
			}
			return null;
		}

		/**
		 * Is this a masked table?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isDimensionMasked() {
			return this.getMods("dimensionMasked").containsKey(this.getName());
		}

		/**
		 * Mask this table.
		 * 
		 * @param dimensionMasked
		 *            <tt>true</tt> to mask.
		 * @throws ValidationException
		 *             if masking is not possible.
		 */
		public void setDimensionMasked(final boolean dimensionMasked)
				throws ValidationException {
			final boolean oldValue = this.isDimensionMasked();
			if (dimensionMasked == oldValue)
				return;
			if (dimensionMasked
					&& !this.getType().equals(DataSetTableType.DIMENSION))
				throw new ValidationException(Resources
						.get("cannotMaskNonDimension"));
			if (dimensionMasked)
				this.getMods("dimensionMasked").put(this.getName(), null);
			else
				this.getMods("dimensionMasked").remove(this.getName());
			this.pcs.firePropertyChange("dimensionMasked", oldValue,
					dimensionMasked);
		}

		/**
		 * Is this a distinct table?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isDistinctTable() {
			return this.getMods("distinctTable").containsKey(this.getName());
		}

		/**
		 * Distinct this table.
		 * 
		 * @param distinctTable
		 *            <tt>true</tt> to make distinct.
		 */
		public void setDistinctTable(final boolean distinctTable) {
			final boolean oldValue = this.isDistinctTable();
			if (distinctTable == oldValue)
				return;
			if (distinctTable)
				this.getMods("distinctTable").put(this.getName(), null);
			else
				this.getMods("distinctTable").remove(this.getName());
			this.pcs.firePropertyChange("distinctTable", oldValue,
					distinctTable);
		}

		/**
		 * Is this a partitioned table?
		 * 
		 * @return the definition if it is.
		 */
		public PartitionTableApplication getPartitionTableApplication() {
			return (PartitionTableApplication) this.getMods(
					"partitionTableApplication").get(this.getName());
		}

		/**
		 * Partition this table.
		 * 
		 * @param partitionTableApplication
		 *            the definition. <tt>null</tt> to un-partition.
		 */
		public void setPartitionTableApplication(
				final PartitionTableApplication partitionTableApplication) {
			final PartitionTableApplication oldValue = this
					.getPartitionTableApplication();
			if (partitionTableApplication == oldValue
					|| partitionTableApplication != null
					&& partitionTableApplication.equals(oldValue))
				return;
			if (partitionTableApplication != null)
				this.getMods("partitionTableApplication").put(this.getName(),
						partitionTableApplication);
			else
				this.getMods("partitionTableApplication")
						.remove(this.getName());
			this.pcs.firePropertyChange("partitionTableApplication", oldValue,
					partitionTableApplication);
		}

		/**
		 * Is this a renamed table?
		 * 
		 * @return <tt>null</tt> if it is not, otherwise return the new name.
		 */
		public String getTableRename() {
			return (String) this.getMods("tableRename").get(this.getName());
		}

		/**
		 * Rename this table.
		 * 
		 * @param tableRename
		 *            the new name, or <tt>null</tt> to undo it.
		 */
		public void setTableRename(String tableRename) {
			String oldValue = this.getTableRename();
			if (tableRename == oldValue || oldValue != null
					&& oldValue.equals(tableRename))
				return;
			if (oldValue == null)
				oldValue = this.getName();
			// Make the name unique if it has a parent.
			if (tableRename != null && this.getParent() != null) {
				final String baseName = tableRename;
				final Set entries = new HashSet();
				// Get renames of siblings.
				for (final Iterator i = this.getParent().getPrimaryKey()
						.getRelations().iterator(); i.hasNext();)
					entries.add(((DataSetTable) ((Relation) i.next())
							.getManyKey().getTable()).getModifiedName());
				entries.remove(oldValue);
				// Iterate over renamedTables entries.
				// If find an entry with same name, find ds table it refers to.
				// If entry ds table parent = table parent then increment and
				// restart search.
				int suffix = 1;
				while (entries.contains(tableRename))
					tableRename = baseName + "_" + suffix++;
			}
			// Check and change it.
			if (tableRename != null)
				this.getMods("tableRename").put(this.getName(), tableRename);
			else
				this.getMods("tableRename").remove(this.getName());
			this.pcs.firePropertyChange("tableRename", oldValue, tableRename);
		}

		/**
		 * What should the next expression column be called?
		 * 
		 * @return the name for it.
		 */
		public String getNextExpressionColumn() {
			final String prefix = Resources.get("expressionColumnPrefix");
			int suffix = 1;
			String name;
			do
				name = prefix + suffix++;
			while (this.getColumns().containsKey(name));
			return name;
		}
	}

	/**
	 * This class defines the various different types of DataSetTable there are.
	 */
	public static class DataSetTableType implements Comparable {
		private static final long serialVersionUID = 1L;

		/**
		 * Use this constant to refer to a dimension table.
		 */
		public static final DataSetTableType DIMENSION = new DataSetTableType(
				"DIMENSION");

		/**
		 * Use this constant to refer to a main table.
		 */
		public static final DataSetTableType MAIN = new DataSetTableType("MAIN");

		/**
		 * Use this constant to refer to a subclass of a main table.
		 */
		public static final DataSetTableType MAIN_SUBCLASS = new DataSetTableType(
				"MAIN_SUBCLASS");

		private final String name;

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this dataset table type object will display when printed.
		 * 
		 * @param name
		 *            the name of the dataset table type.
		 */
		private DataSetTableType(final String name) {
			this.name = name;
		}

		public int compareTo(final Object o) throws ClassCastException {
			final DataSetTableType c = (DataSetTableType) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}

		/**
		 * Displays the name of this dataset table type object.
		 * 
		 * @return the name of this dataset table type object.
		 */
		public String getName() {
			return this.name;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This will return the output of {@link #getName()}.
		 */
		public String toString() {
			return this.getName();
		}
	}

	/**
	 * Defines an expression column for a table.
	 */
	public static class ExpressionColumnDefinition implements
			TransactionListener {
		private static final long serialVersionUID = 1L;

		private BeanMap aliases;

		private String expr;

		private boolean groupBy;

		private String colKey;

		private boolean directModified = false;

		private final PropertyChangeSupport pcs = new PropertyChangeSupport(
				this);

		/**
		 * This constructor makes a new expression definition based on the given
		 * expression and a set of column aliases.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 * @param groupBy
		 *            if this expression requires a group-by statement to be
		 *            used on all columns not included in the expression.
		 * @param colKey
		 *            the name of the expression column that will be created.
		 */
		public ExpressionColumnDefinition(final String expr, final Map aliases,
				final boolean groupBy, final String colKey) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("expColMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("expColMissingAliases"));

			// Remember the settings.
			this.aliases = new BeanMap(new TreeMap(aliases));
			this.expr = expr;
			this.groupBy = groupBy;
			this.colKey = colKey;

			Transaction.addTransactionListener(this);

			final PropertyChangeListener listener = new PropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent e) {
					ExpressionColumnDefinition.this.setDirectModified(true);
				}
			};
			this.pcs.addPropertyChangeListener(listener);
			this.aliases.addPropertyChangeListener(listener);
		}

		public boolean isDirectModified() {
			return this.directModified;
		}

		public void setDirectModified(final boolean modified) {
			if (modified == this.directModified)
				return;
			final boolean oldValue = this.directModified;
			this.directModified = modified;
			this.pcs.firePropertyChange("directModified", oldValue, modified);
		}

		public boolean isVisibleModified() {
			return false;
		}

		public void setVisibleModified(final boolean modified) {
			// Ignore, for now.
		}

		public void transactionResetVisibleModified() {
			// Ignore, for now.
		}

		public void transactionResetDirectModified() {
			this.directModified = false;
		}

		public void transactionStarted(final TransactionEvent evt) {
			// Don't really care for now.
		}

		public void transactionEnded(final TransactionEvent evt) {
			// Ignore for now.
		}

		/**
		 * Adds a property change listener.
		 * 
		 * @param listener
		 *            the listener to add.
		 */
		public void addPropertyChangeListener(
				final PropertyChangeListener listener) {
			this.pcs.addPropertyChangeListener(listener);
		}

		/**
		 * Adds a property change listener.
		 * 
		 * @param property
		 *            the property to listen to.
		 * @param listener
		 *            the listener to add.
		 */
		public void addPropertyChangeListener(final String property,
				final PropertyChangeListener listener) {
			this.pcs.addPropertyChangeListener(property, listener);
		}

		/**
		 * Removes a property change listener.
		 * 
		 * @param listener
		 *            the listener to remove.
		 */
		public void removePropertyChangeListener(
				final PropertyChangeListener listener) {
			this.pcs.removePropertyChangeListener(listener);
		}

		/**
		 * Removes a property change listener.
		 * 
		 * @param property
		 *            the property to listen to.
		 * @param listener
		 *            the listener to remove.
		 */
		public void removePropertyChangeListener(final String property,
				final PropertyChangeListener listener) {
			this.pcs.removePropertyChangeListener(property, listener);
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link String} instances, and
		 *         values are aliases used in the expression.
		 */
		public BeanMap getAliases() {
			return this.aliases;
		}

		/**
		 * Returns the expression, <i>without</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @return the unsubstituted expression.
		 */
		public String getExpression() {
			return this.expr;
		}

		/**
		 * Get the name of the expression column.
		 * 
		 * @return the name.
		 */
		public String getColKey() {
			return this.colKey;
		}

		/**
		 * Does this expression require a group-by on all columns other than
		 * those included in the expression?
		 * 
		 * @return <tt>true</tt> if it does.
		 */
		public boolean isGroupBy() {
			return this.groupBy;
		}

		/**
		 * Set the group by flag on this expression.
		 * 
		 * @param groupBy
		 *            the new flag.
		 */
		public void setGroupBy(final boolean groupBy) {
			this.groupBy = groupBy;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @param dsTable
		 *            the table to use to look up column names from.
		 * @param prefix
		 *            the prefix to use for each column. If <tt>null</tt>, no
		 *            prefix is used.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final DataSetTable dsTable,
				final String prefix) {
			Log.debug("Calculating expression column expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final String col = (String) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				final DataSetColumn dsCol = (DataSetColumn) dsTable
						.getColumns().get(col);
				sub = sub.replaceAll(alias, prefix != null ? prefix + "."
						+ dsCol.getModifiedName() : dsCol.getModifiedName());
			}
			Log.debug("Expression is: " + sub);
			return sub;
		}

		/**
		 * The actual expression. The values from the alias maps will be used to
		 * refer to various columns. This value is RDBMS-specific.
		 * 
		 * @param expr
		 *            the actual expression to use.
		 */
		public void setExpression(final String expr) {
			this.expr = expr;
		}
	}
}
