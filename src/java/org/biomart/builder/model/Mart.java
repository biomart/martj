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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.PartitionTable.PartitionTableApplication;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.BeanMap;
import org.biomart.common.utils.Transaction;
import org.biomart.common.utils.Transaction.TransactionEvent;
import org.biomart.common.utils.Transaction.TransactionListener;

/**
 * The mart contains the set of all schemas that are providing data to this
 * mart. It also has zero or more datasets based around these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class Mart implements TransactionListener {
	private static final long serialVersionUID = 1L;

	/**
	 * Subclasses use this field to fire events of their own.
	 */
	protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	private final BeanMap datasets;

	private final BeanMap schemas;

	private String outputSchema = null;

	private String outputHost = null;

	private String outputPort = null;

	private String overrideHost = null;

	private String overridePort = null;

	private boolean directModified = false;

	/**
	 * Constant referring to table and column name conversion.
	 */
	public static final int USE_MIXED_CASE = 0;

	/**
	 * Constant referring to table and column name conversion.
	 */
	public static final int USE_UPPER_CASE = 1;

	/**
	 * Constant referring to table and column name conversion.
	 */
	public static final int USE_LOWER_CASE = 2;

	private int nameCase = Mart.USE_MIXED_CASE;

	// For use in hash code and equals to prevent dups in prop change.
	private static int ID_SERIES = 0;

	private final int uniqueID = Mart.ID_SERIES++;

	private Collection schemaCache;

	private Collection datasetCache;

	/**
	 * Construct a new, empty, mart.
	 */
	public Mart() {
		Log.debug("Creating new mart");
		this.datasets = new BeanMap(new TreeMap());
		this.schemas = new BeanMap(new TreeMap());

		Transaction.addTransactionListener(this);

		// All changes to us make us modified.
		final PropertyChangeListener listener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				Mart.this.setDirectModified(true);
			}
		};
		this.pcs.addPropertyChangeListener("case", listener);
		this.pcs.addPropertyChangeListener("outputHost", listener);
		this.pcs.addPropertyChangeListener("outputPort", listener);
		this.pcs.addPropertyChangeListener("outputSchema", listener);
		this.pcs.addPropertyChangeListener("overrideHost", listener);

		// Listeners on schema and dataset additions to spot
		// and handle renames.
		this.schemaCache = new HashSet();
		this.schemas.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				final Collection newSchs = new HashSet(Mart.this.getSchemas()
						.values());
				if (!newSchs.equals(Mart.this.schemaCache)) {
					Mart.this.setDirectModified(true);
					// Identify dropped ones.
					final Collection dropped = new HashSet(
							Mart.this.schemaCache);
					dropped.removeAll(newSchs);
					// Identify new ones.
					newSchs.removeAll(Mart.this.schemaCache);
					// Drop dropped ones.
					for (final Iterator i = dropped.iterator(); i.hasNext();)
						Mart.this.schemaCache.remove(i.next());
					// Add added ones.
					for (final Iterator i = newSchs.iterator(); i.hasNext();) {
						final Schema sch = (Schema) i.next();
						Mart.this.schemaCache.add(sch);
						sch.addPropertyChangeListener("name",
								new PropertyChangeListener() {
									public void propertyChange(
											final PropertyChangeEvent pe) {
										Mart.this.setDirectModified(true);
										Mart.this.schemas.remove(pe
												.getOldValue());
										Mart.this.schemas.put(pe.getNewValue(),
												sch);
									}
								});
						sch.addPropertyChangeListener("directModified",
								new PropertyChangeListener() {
									public void propertyChange(
											final PropertyChangeEvent evt) {
										Mart.this.setDirectModified(true);
									}
								});
					}
				}
			}
		});
		this.datasetCache = new HashSet();
		this.datasets.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				final Collection newDss = new HashSet(Mart.this.getDataSets()
						.values());
				if (!newDss.equals(Mart.this.datasetCache)) {
					Mart.this.setDirectModified(true);
					// Identify dropped ones.
					final Collection dropped = new HashSet(
							Mart.this.datasetCache);
					dropped.removeAll(newDss);
					// Identify new ones.
					newDss.removeAll(Mart.this.datasetCache);
					// Drop dropped ones.
					for (final Iterator i = dropped.iterator(); i.hasNext();)
						Mart.this.datasetCache.remove(i.next());
					// Add added ones.
					for (final Iterator i = newDss.iterator(); i.hasNext();) {
						final DataSet ds = (DataSet) i.next();
						Mart.this.datasetCache.add(ds);
						ds.addPropertyChangeListener("name",
								new PropertyChangeListener() {
									public void propertyChange(
											final PropertyChangeEvent pe) {
										Mart.this.setDirectModified(true);
										Mart.this.datasets.remove(pe
												.getOldValue());
										Mart.this.datasets.put(
												pe.getNewValue(), ds);
									}
								});
						ds.addPropertyChangeListener("directModified",
								new PropertyChangeListener() {
									public void propertyChange(
											final PropertyChangeEvent evt) {
										Mart.this.setDirectModified(true);
									}
								});
					}
				}
			}
		});
	}

	/**
	 * Obtain the unique series number for this mart.
	 * 
	 * @return the unique Id.
	 */
	public int getUniqueId() {
		return this.uniqueID;
	}

	public int hashCode() {
		return 0; // All marts go in one big bucket!
	}

	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		else if (obj == null)
			return false;
		else if (obj instanceof Mart)
			return this.uniqueID == ((Mart) obj).uniqueID;
		else
			return false;
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

	public void transactionReset() {
		this.directModified = false;
	}

	public void transactionStarted(final TransactionEvent evt) {
		// Ignore, for now.
	}

	public void transactionEnded(final TransactionEvent evt) {
		// Don't really care for now.
	}

	/**
	 * Adds a property change listener.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
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
	 * What case to use for table and column names? Mixed is default.
	 * 
	 * @return one of {@link #USE_LOWER_CASE}, {@link #USE_UPPER_CASE}, or
	 *         {@link #USE_MIXED_CASE}.
	 */
	public int getCase() {
		return this.nameCase;
	}

	/**
	 * What case to use for table and column names? Mixed is default.
	 * 
	 * @param nameCase
	 *            one of {@link #USE_LOWER_CASE}, {@link #USE_UPPER_CASE}, or
	 *            {@link #USE_MIXED_CASE}.
	 */
	public void setCase(final int nameCase) {
		Log.debug("Changing case for " + this + " to " + nameCase);
		final int oldValue = this.nameCase;
		if (this.nameCase == nameCase)
			return;
		// Make the change.
		this.nameCase = nameCase;
		this.pcs.firePropertyChange("nameCase", oldValue, nameCase);
	}

	/**
	 * Optional, sets the default target schema this mart will output dataset
	 * DDL to later.
	 * 
	 * @param outputSchema
	 *            the target schema.
	 */
	public void setOutputSchema(final String outputSchema) {
		Log.debug("Changing outputSchema for " + this + " to " + outputSchema);
		final String oldValue = this.outputSchema;
		if (this.outputSchema == outputSchema || this.outputSchema != null
				&& this.outputSchema.equals(outputSchema))
			return;
		// Make the change.
		this.outputSchema = outputSchema;
		this.pcs.firePropertyChange("outputSchema", oldValue, outputSchema);
	}

	/**
	 * Optional, gets the default target schema this mart will output dataset
	 * DDL to later.
	 * 
	 * @return the target schema.
	 */
	public String getOutputSchema() {
		return this.outputSchema;
	}

	/**
	 * Optional, sets the default target host this mart will output dataset DDL
	 * to later.
	 * 
	 * @param outputHost
	 *            the target host.
	 */
	public void setOutputHost(final String outputHost) {
		Log.debug("Changing outputHost for " + this + " to " + outputHost);
		final String oldValue = this.outputHost;
		if (this.outputHost == outputHost || this.outputHost != null
				&& this.outputHost.equals(outputHost))
			return;
		// Make the change.
		this.outputHost = outputHost;
		this.pcs.firePropertyChange("outputHost", oldValue, outputHost);
	}

	/**
	 * Optional, gets the default target host this mart will output dataset DDL
	 * to later.
	 * 
	 * @return the target host.
	 */
	public String getOutputHost() {
		return this.outputHost;
	}

	/**
	 * Optional, sets the default target port this mart will output dataset DDL
	 * to later.
	 * 
	 * @param outputPort
	 *            the target port.
	 */
	public void setOutputPort(final String outputPort) {
		Log.debug("Changing outputPort for " + this + " to " + outputPort);
		final String oldValue = this.outputPort;
		if (this.outputPort == outputPort || this.outputPort != null
				&& this.outputPort.equals(outputPort))
			return;
		// Make the change.
		this.outputPort = outputPort;
		this.pcs.firePropertyChange("outputPort", oldValue, outputPort);
	}

	/**
	 * Optional, gets the default target port this mart will output dataset DDL
	 * to later.
	 * 
	 * @return the target port.
	 */
	public String getOutputPort() {
		return this.outputPort;
	}

	/**
	 * Optional, sets the default target JDBC host this mart will output dataset
	 * DDL to later.
	 * 
	 * @param overrideHost
	 *            the target host.
	 */
	public void setOverrideHost(final String overrideHost) {
		Log.debug("Changing overrideHost for " + this + " to " + overrideHost);
		final String oldValue = this.overrideHost;
		if (this.overrideHost == overrideHost || this.overrideHost != null
				&& this.overrideHost.equals(overrideHost))
			return;
		// Make the change.
		this.overrideHost = overrideHost;
		this.pcs.firePropertyChange("overrideHost", oldValue, overrideHost);
	}

	/**
	 * Optional, gets the default target JDBC host this mart will output dataset
	 * DDL to later.
	 * 
	 * @return the target host.
	 */
	public String getOverrideHost() {
		return this.overrideHost;
	}

	/**
	 * Optional, sets the default target JDBC port this mart will output dataset
	 * DDL to later.
	 * 
	 * @param overridePort
	 *            the target port.
	 */
	public void setOverridePort(final String overridePort) {
		Log.debug("Changing overridePort for " + this + " to " + overridePort);
		final String oldValue = this.overridePort;
		if (this.overridePort == overridePort || this.overridePort != null
				&& this.overridePort.equals(overridePort))
			return;
		// Make the change.
		this.overridePort = overridePort;
		this.pcs.firePropertyChange("overridePort", oldValue, overridePort);
	}

	/**
	 * Optional, gets the default target JDBC port this mart will output dataset
	 * DDL to later.
	 * 
	 * @return the target port.
	 */
	public String getOverridePort() {
		return this.overridePort;
	}

	/**
	 * Returns the set of dataset objects which this mart includes. The set may
	 * be empty but it is never <tt>null</tt>.
	 * 
	 * @return a set of dataset objects. Keys are names, values are datasets.
	 */
	public BeanMap getDataSets() {
		return this.datasets;
	}

	/**
	 * Returns the set of partition column names which this mart includes. The
	 * set may be empty but it is never <tt>null</tt>.
	 * 
	 * @return a set of partition column names (as strings).
	 */
	public Collection getPartitionColumnNames() {
		final List colNames = new ArrayList();
		for (final Iterator i = this.getPartitionTableNames().iterator(); i
				.hasNext();) {
			final PartitionTable pt = ((DataSet) this.getDataSets().get(
					i.next())).asPartitionTable();
			for (final Iterator j = pt.getSelectedColumnNames().iterator(); j
					.hasNext();) {
				final String col = (String) j.next();
				if (!col.equals(PartitionTable.DIV_COLUMN))
					colNames.add(pt.getName() + "." + col);
			}
		}
		Collections.sort(colNames);
		return Collections.unmodifiableCollection(colNames);
	}

	/**
	 * Returns the set of partition table names which this mart includes. The
	 * set may be empty but it is never <tt>null</tt>.
	 * 
	 * @return a set of partition table names (as strings).
	 */
	public Collection getPartitionTableNames() {
		final List tblNames = new ArrayList();
		for (final Iterator i = this.getDataSets().values().iterator(); i
				.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			if (ds.isPartitionTable())
				tblNames.add(ds.getName());
		}
		Collections.sort(tblNames);
		return Collections.unmodifiableCollection(tblNames);
	}

	/**
	 * Returns the set of schema objects which this mart includes. The set may
	 * be empty but it is never <tt>null</tt>.
	 * 
	 * @return a set of schema objects. Keys are names, values are actual
	 *         schemas.
	 */
	public BeanMap getSchemas() {
		return this.schemas;
	}

	/**
	 * Given a set of tables, produce the minimal set of datasets which include
	 * all the specified tables. Tables can be included in the same dataset if
	 * they are linked by 1:M relations (1:M, 1:M in a chain), or if the table
	 * is the last in the chain and is linked to the previous table by a pair of
	 * 1:M and M:1 relations via a third table, simulating a M:M relation.
	 * <p>
	 * If the chains of tables fork, then one dataset is generated for each
	 * branch of the fork.
	 * <p>
	 * Every suggested dataset is synchronised before being returned.
	 * <p>
	 * Datasets will be named after their central tables. If a dataset with that
	 * name already exists, a '_' and sequence number will be appended to make
	 * the new dataset name unique.
	 * <p>
	 * See also
	 * {@link #continueSubclassing(Collection, Collection, DataSet, Table)}.
	 * 
	 * @param includeTables
	 *            the tables that must appear in the final set of datasets.
	 * @return the collection of datasets generated.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws DataModelException
	 *             if synchronisation fails.
	 */
	public Collection suggestDataSets(final Collection includeTables)
			throws SQLException, DataModelException {
		Log.debug("Suggesting datasets for " + includeTables);
		// The root tables are all those which do not have a M:1 relation
		// to another one of the initial set of tables. This means that
		// extra datasets will be created for each table at the end of
		// 1:M:1 relation, so that any further tables past it will still
		// be included.
		Log.debug("Finding root tables");
		final Collection rootTables = new HashSet(includeTables);
		for (final Iterator i = includeTables.iterator(); i.hasNext();) {
			final Table candidate = (Table) i.next();
			for (final Iterator j = candidate.getRelations().iterator(); j
					.hasNext();) {
				final Relation rel = (Relation) j.next();
				if (rel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
					continue;
				if (!rel.isOneToMany())
					continue;
				if (!rel.getManyKey().getTable().equals(candidate))
					continue;
				if (includeTables.contains(rel.getOneKey().getTable()))
					rootTables.remove(candidate);
			}
		}
		// We construct one dataset per root table.
		final Set suggestedDataSets = new TreeSet();
		for (final Iterator i = rootTables.iterator(); i.hasNext();) {
			final Table rootTable = (Table) i.next();
			Log.debug("Constructing dataset for root table " + rootTable);
			final DataSet dataset;
			try {
				dataset = new DataSet(this, rootTable, rootTable.getName());
			} catch (final ValidationException e) {
				// Skip this one.
				continue;
			}
			this.getDataSets().put(dataset.getName(), dataset);
			// Process it.
			final Collection tablesIncluded = new HashSet();
			tablesIncluded.add(rootTable);
			Log.debug("Attempting to find subclass datasets");
			suggestedDataSets.addAll(this.continueSubclassing(includeTables,
					tablesIncluded, dataset, rootTable));
		}

		// Synchronise them all.
		Log.debug("Synchronising constructed datasets");
		for (final Iterator i = suggestedDataSets.iterator(); i.hasNext();)
			((DataSet) i.next()).synchronise();

		// Do any of the resulting datasets contain all the tables
		// exactly with subclass relations between each?
		// If so, just use that one dataset and forget the rest.
		Log.debug("Finding perfect candidate");
		DataSet perfectDS = null;
		for (final Iterator i = suggestedDataSets.iterator(); i.hasNext()
				&& perfectDS == null;) {
			final DataSet candidate = (DataSet) i.next();

			// A candidate is a perfect match if the set of tables
			// covered by the subclass relations is the same as the
			// original set of tables requested.
			final Collection scTables = new HashSet();
			for (final Iterator j = candidate.getRelations().iterator(); j
					.hasNext();) {
				final Relation r = (Relation) j.next();
				if (!r.isSubclassRelation(candidate))
					continue;
				final Table t1 = r.getFirstKey().getTable();
				final Table t2 = r.getSecondKey().getTable();
				// Expand this to include all tables included by M:1/1:1/M:M
				// relations from the actual subclassed tables. This will bring
				// in all those available via 1:M:1 relations.
				if (scTables.add(t1))
					for (final Iterator k = t1.getRelations().iterator(); k
							.hasNext();) {
						final Relation r1 = (Relation) k.next();
						if (r1.isOneToMany()
								&& r1.getOneKey().getTable().equals(t1))
							continue;
						scTables.add(r1.getOtherKey(r1.getKeyForTable(t1))
								.getTable());
					}
				if (scTables.add(t2))
					for (final Iterator k = t2.getRelations().iterator(); k
							.hasNext();) {
						final Relation r2 = (Relation) k.next();
						if (r2.isOneToMany()
								&& r2.getOneKey().getTable().equals(t2))
							continue;
						scTables.add(r2.getOtherKey(r2.getKeyForTable(t2))
								.getTable());
					}
			}
			// Finally perform the check to see if we have them all.
			if (scTables.containsAll(includeTables))
				perfectDS = candidate;
		}
		if (perfectDS != null) {
			Log.debug("Perfect candidate found - dropping others");
			// Drop the others.
			for (final Iterator i = suggestedDataSets.iterator(); i.hasNext();) {
				final DataSet candidate = (DataSet) i.next();
				if (!candidate.equals(perfectDS)) {
					this.getDataSets().remove(candidate.getName());
					i.remove();
				}
			}
			// Rename it to lose any extension it may have gained.
			perfectDS.setName(perfectDS.getCentralTable().getName());
		} else
			Log.debug("No perfect candidate found - retaining all");

		// Return the final set of suggested datasets.
		return suggestedDataSets;
	}

	/**
	 * This internal method takes a bunch of tables that the user would like to
	 * see as subclass or main tables in a single dataset, and attempts to find
	 * a subclass path between them. For each subclass path it can build, it
	 * produces one dataset based on that path. Each path contains as many
	 * tables as possible. The paths do not overlap. If there is a choice, the
	 * one chosen is arbitrary.
	 * 
	 * @param includeTables
	 *            the tables we want to include as main or subclass tables.
	 * @param tablesIncluded
	 *            the tables we have managed to include in a path so far.
	 * @param dataset
	 *            the dataset we started out from which contains just the main
	 *            table on its own with no subclassing.
	 * @param table
	 *            the real table we are looking at to see if there is a subclass
	 *            path between any of the include tables and any of the existing
	 *            subclassed or main tables via this real table.
	 * @return the datasets we have created - one per subclass path, or if there
	 *         were none, then a singleton collection containing the dataset
	 *         originally passed in.
	 */
	private Collection continueSubclassing(final Collection includeTables,
			final Collection tablesIncluded, final DataSet dataset,
			final Table table) {
		// Check table has a primary key.
		final Key pk = table.getPrimaryKey();

		// Make a unique set to hold all the resulting datasets. It
		// is initially empty.
		final Collection suggestedDataSets = new HashSet();
		// Make a set to contain relations to subclass.
		final Collection subclassedRelations = new HashSet();
		// Make a map to hold tables included for each relation.
		final Map relationTablesIncluded = new HashMap();
		// Make a list to hold all tables included at this level.
		final Collection localTablesIncluded = new HashSet(tablesIncluded);

		// Find all 1:M relations starting from the given table that point
		// to another interesting table.
		if (pk != null)
			for (final Iterator i = pk.getRelations().iterator(); i.hasNext();) {
				final Relation r = (Relation) i.next();
				if (!r.isOneToMany())
					continue;
				else if (r.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT))
					continue;

				// For each relation, if it points to another included
				// table via 1:M we should subclass the relation.
				final Table target = r.getManyKey().getTable();
				if (includeTables.contains(target)
						&& !localTablesIncluded.contains(target)) {
					subclassedRelations.add(r);
					final Collection newRelationTablesIncluded = new HashSet(
							tablesIncluded);
					relationTablesIncluded.put(r, newRelationTablesIncluded);
					newRelationTablesIncluded.add(target);
					localTablesIncluded.add(target);
				}
			}

		// Find all 1:M:1 relations starting from the given table that point
		// to another interesting table.
		if (pk != null)
			for (final Iterator i = pk.getRelations().iterator(); i.hasNext();) {
				final Relation firstRel = (Relation) i.next();
				if (!firstRel.isOneToMany())
					continue;
				else if (firstRel.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT))
					continue;

				final Table intermediate = firstRel.getManyKey().getTable();
				for (final Iterator j = intermediate.getForeignKeys()
						.iterator(); j.hasNext();) {
					final Key fk = (Key) j.next();
					if (fk.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
						continue;
					for (final Iterator k = fk.getRelations().iterator(); k
							.hasNext();) {
						final Relation secondRel = (Relation) k.next();
						if (secondRel.equals(firstRel))
							continue;
						else if (!secondRel.isOneToMany())
							continue;
						else if (secondRel.getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT))
							continue;
						// For each relation, if it points to another included
						// table via M:1 we should subclass the relation.
						final Table target = secondRel.getOneKey().getTable();
						if (includeTables.contains(target)
								&& !localTablesIncluded.contains(target)) {
							subclassedRelations.add(firstRel);
							final Collection newRelationTablesIncluded = new HashSet(
									tablesIncluded);
							relationTablesIncluded.put(firstRel,
									newRelationTablesIncluded);
							newRelationTablesIncluded.add(target);
							localTablesIncluded.add(target);
						}
					}
				}
			}

		// No subclassing? Return a singleton.
		if (subclassedRelations.isEmpty())
			return Collections.singleton(dataset);

		// Iterate through the relations we found and recurse.
		// If not the last one, we copy the original dataset and
		// work on the copy, otherwise we work on the original.
		for (final Iterator i = subclassedRelations.iterator(); i.hasNext();) {
			final Relation r = (Relation) i.next();
			DataSet suggestedDataSet = dataset;
			try {
				if (i.hasNext()) {
					suggestedDataSet = new DataSet(this, dataset
							.getCentralTable(), dataset.getName());
					// Copy subclassed relations from existing dataset.
					for (final Iterator j = dataset.getIncludedRelations()
							.iterator(); j.hasNext();)
						((Relation) j.next()).setSubclassRelation(
								suggestedDataSet, true);
				}
				r.setSubclassRelation(suggestedDataSet, true);
			} catch (final ValidationException e) {
				// Not valid? OK, ignore this one.
				continue;
			}
			suggestedDataSets.addAll(this.continueSubclassing(includeTables,
					(Collection) relationTablesIncluded.get(r),
					suggestedDataSet, r.getManyKey().getTable()));
		}

		// Return the resulting datasets.
		return suggestedDataSets;
	}

	/**
	 * Given a dataset and a set of columns from one table upon which a table of
	 * that dataset is based, find all other tables which have similar columns,
	 * and create a new dataset for each one.
	 * <p>
	 * This method will not create datasets around tables which have already
	 * been used as the underlying table in any dataset table in the existing
	 * dataset. Neither will it create a dataset around the table from which the
	 * original columns came.
	 * <p>
	 * There may be no datasets resulting from this, if the columns do not
	 * appear elsewhere.
	 * <p>
	 * Datasets are synchronised before being returned.
	 * <p>
	 * Datasets will be named after their central tables. If a dataset with that
	 * name already exists, a '_' and sequence number will be appended to make
	 * the new dataset name unique.
	 * 
	 * @param dataset
	 *            the dataset the columns were selected from.
	 * @param columns
	 *            the columns to search across.
	 * @return the resulting set of datasets.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws DataModelException
	 *             if synchronisation fails.
	 */
	public Collection suggestInvisibleDataSets(final DataSet dataset,
			final Collection columns) throws SQLException, DataModelException {
		Log.debug("Suggesting invisible datasets for " + dataset + " columns "
				+ columns);
		final Collection invisibleDataSets = new HashSet();
		final Table sourceTable = ((Column) columns.iterator().next())
				.getTable();
		// Find all tables which mention the columns specified.
		Log.debug("Finding candidate tables");
		final Collection candidates = new HashSet();
		for (final Iterator i = this.schemas.values().iterator(); i.hasNext();)
			for (final Iterator j = ((Schema) i.next()).getTables().values()
					.iterator(); j.hasNext();) {
				final Table table = (Table) j.next();
				int matchingColCount = 0;
				for (final Iterator k = columns.iterator(); k.hasNext();) {
					final Column col = (Column) k.next();
					if (table.getColumns().containsKey(col.getName())
							|| table
									.getColumns()
									.containsKey(
											col.getName()
													+ Resources
															.get("foreignKeySuffix")))
						matchingColCount++;
				}
				if (matchingColCount == columns.size())
					candidates.add(table);
			}
		// Remove from the found tables all those which are already
		// used, and the one from which the original columns came.
		Log.debug("Removing candidates that are already used in this dataset");
		candidates.remove(sourceTable);
		for (final Iterator i = dataset.getTables().values().iterator(); i
				.hasNext();)
			candidates.remove(((DataSetTable) i.next()).getFocusTable());
		// Generate the dataset for each.
		Log.debug("Creating datasets for remaining candidates");
		for (final Iterator i = candidates.iterator(); i.hasNext();) {
			final Table table = (Table) i.next();
			final DataSet inv;
			try {
				inv = new DataSet(this, table, table.getName());
			} catch (final ValidationException e) {
				// Skip this one.
				continue;
			}
			this.getDataSets().put(inv.getName(), inv);
			invisibleDataSets.add(inv);
		}
		// Synchronise them all and make them all invisible.
		Log.debug("Synchronising suggested datasets");
		for (final Iterator i = invisibleDataSets.iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			ds.setInvisible(true);
			ds.synchronise();
		}
		// Return the results.
		return invisibleDataSets;
	}

	/**
	 * If the dataset has had a partition applied to it, return it.
	 * 
	 * @param ds
	 *            the dataset to check.
	 * @return the partition application.
	 */
	public PartitionTableApplication getPartitionTableApplicationForDataSet(
			final DataSet ds) {
		for (final Iterator i = this.getPartitionTableNames().iterator(); i
				.hasNext();) {
			final PartitionTable pt = ((DataSet) this.getDataSets().get(
					i.next())).asPartitionTable();
			if (pt.getApplication(ds, PartitionTable.NO_DIMENSION) != null)
				return pt.getApplication(ds, PartitionTable.NO_DIMENSION);
		}
		return null;
	}

	/**
	 * If the dimension has had a partition applied to it, return it.
	 * 
	 * @param dm
	 *            the dimension to check.
	 * @return the partition application.
	 */
	public PartitionTableApplication getPartitionTableApplicationForDimension(
			final DataSetTable dm) {
		for (final Iterator i = this.getPartitionTableNames().iterator(); i
				.hasNext();) {
			final PartitionTable pt = ((DataSet) this.getDataSets().get(
					i.next())).asPartitionTable();
			if (pt.getApplication(dm.getDataSet(), dm.getName()) != null)
				return pt.getApplication(dm.getDataSet(), dm.getName());
		}
		return null;
	}
}
