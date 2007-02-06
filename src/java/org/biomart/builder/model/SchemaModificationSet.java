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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This interface defines a set of modifications to a schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class SchemaModificationSet {

	/**
	 * The name to use for dataset-wide modifications.
	 */
	public static final String DATASET = "__DATASET_WIDE__";

	private final DataSet ds;

	private final Collection subclassedRelations = new HashSet();

	private Collection mergedRelations = new HashSet();

	private final Map maskedRelations = new HashMap();

	private final Map forceIncludeRelations = new HashMap();

	private Map compoundRelations = new HashMap();

	private final Map restrictedTables = new HashMap();

	private final Map restrictedRelations = new HashMap();

	private final Map concatRelations = new HashMap();

	public SchemaModificationSet(final DataSet ds) {
		this.ds = ds;
	}

	public void setMaskedRelation(final Relation relation) {
		this.setMaskedRelation(SchemaModificationSet.DATASET, relation);
	}

	public void setMaskedRelation(final DataSetTable table,
			final Relation relation) {
		this.setMaskedRelation(table.getName(), relation);
	}

	private void setMaskedRelation(final String tableName,
			final Relation relation) {
		if (!this.maskedRelations.containsKey(tableName))
			this.maskedRelations.put(tableName, new HashSet());
		final Collection masks = (Collection) this.maskedRelations
				.get(tableName);
		masks.add(relation);
	}

	public void unsetMaskedRelation(final Relation relation) {
		this.unsetMaskedRelation(SchemaModificationSet.DATASET, relation);
	}

	public void unsetMaskedRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Collection globalMasks = (Collection) this.maskedRelations
				.get(SchemaModificationSet.DATASET);
		if (globalMasks != null && globalMasks.contains(relation))
			throw new ValidationException(Resources
					.get("relationMaskedGlobally"));
		this.unsetMaskedRelation(table.getName(), relation);
	}

	private void unsetMaskedRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isMaskedRelation(tableName, relation))
			return;
		if (this.maskedRelations.containsKey(tableName)) {
			final Collection masks = (Collection) this.maskedRelations
					.get(tableName);
			masks.remove(relation);
			if (masks.isEmpty())
				this.maskedRelations.remove(tableName);
		}
	}

	public boolean isMaskedRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isMaskedRelation(table == null ? SchemaModificationSet.DATASET
						: table.getName(), relation);
	}

	private boolean isMaskedRelation(final String tableName,
			final Relation relation) {
		final Collection globalMasks = (Collection) this.maskedRelations
				.get(SchemaModificationSet.DATASET);
		final Collection masks = (Collection) this.maskedRelations
				.get(tableName);
		return (masks != null && masks.contains(relation))
				|| (globalMasks != null && globalMasks.contains(relation));
	}

	public Map getMaskedRelations() {
		return this.maskedRelations;
	}

	public void setMergedRelation(final Relation rel) {
		this.mergedRelations.add(rel);
	}

	public void unsetMergedRelation(final Relation rel) {
		this.mergedRelations.remove(rel);
	}

	public boolean isMergedRelation(final Relation rel) {
		return this.mergedRelations.contains(rel);
	}

	public Collection getMergedRelations() {
		return this.mergedRelations;
	}

	public void setRestrictedTable(final Table table,
			final RestrictedTableDefinition restriction) {
		this.setRestrictedTable(SchemaModificationSet.DATASET, table,
				restriction);
	}

	public void setRestrictedTable(final DataSetTable dsTable,
			final Table table, final RestrictedTableDefinition restriction) {
		this.setRestrictedTable(dsTable.getName(), table, restriction);
	}

	private void setRestrictedTable(final String dsTableName,
			final Table table, final RestrictedTableDefinition restriction) {
		if (!this.restrictedTables.containsKey(dsTableName))
			this.restrictedTables.put(dsTableName, new HashMap());
		final Map restrictions = (Map) this.restrictedTables.get(dsTableName);
		restrictions.put(table, restriction);
	}

	public void unsetRestrictedTable(final Table table) {
		this.unsetRestrictedTable(SchemaModificationSet.DATASET, table);
	}

	public void unsetRestrictedTable(final DataSetTable dsTable,
			final Table table) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalRests = (Map) this.restrictedTables
				.get(SchemaModificationSet.DATASET);
		if (globalRests != null && globalRests.containsKey(table))
			throw new ValidationException(Resources
					.get("tableRestrictedGlobally"));
		this.unsetRestrictedTable(dsTable.getName(), table);
	}

	private void unsetRestrictedTable(final String dsTableName,
			final Table table) {
		// Skip already-unmasked relations.
		if (!this.isRestrictedTable(dsTableName, table))
			return;
		if (this.restrictedTables.containsKey(dsTableName)) {
			final Map rests = (Map) this.restrictedTables.get(dsTableName);
			rests.remove(table);
			if (rests.isEmpty())
				this.restrictedTables.remove(dsTableName);
		}
	}

	public boolean isRestrictedTable(final DataSetTable dsTable,
			final Table table) {
		return this.isRestrictedTable(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), table);
	}

	private boolean isRestrictedTable(final String dsTableName,
			final Table table) {
		final Map globalRests = (Map) this.restrictedTables
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedTables.get(dsTableName);
		return (rests != null && rests.containsKey(table))
				|| (globalRests != null && globalRests.containsKey(table));
	}

	public RestrictedTableDefinition getRestrictedTable(
			final DataSetTable dsTable, final Table table) {
		return this.getRestrictedTable(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), table);
	}

	private RestrictedTableDefinition getRestrictedTable(
			final String dsTableName, final Table table) {
		if (!this.isRestrictedTable(dsTableName, table))
			return null;
		final Map globalRests = (Map) this.restrictedTables
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedTables.get(dsTableName);
		return (rests != null && rests.containsKey(table)) ? (RestrictedTableDefinition) rests
				.get(table)
				: (RestrictedTableDefinition) globalRests.get(table);
	}

	public Map getRestrictedTables() {
		return this.restrictedTables;
	}

	public void setRestrictedRelation(final Relation relation, final int index,
			final RestrictedRelationDefinition restriction) {
		this.setRestrictedRelation(SchemaModificationSet.DATASET, relation,
				index, restriction);
	}

	public void setRestrictedRelation(final DataSetTable dsTable,
			final Relation relation, final int index,
			final RestrictedRelationDefinition restriction) {
		this.setRestrictedRelation(dsTable.getName(), relation, index,
				restriction);
	}

	private void setRestrictedRelation(final String dsTableName,
			final Relation relation, final int index,
			final RestrictedRelationDefinition restriction) {
		if (!this.restrictedRelations.containsKey(dsTableName))
			this.restrictedRelations.put(dsTableName, new HashMap());
		final Map restrictions = (Map) this.restrictedRelations
				.get(dsTableName);
		if (!restrictions.containsKey(relation))
			restrictions.put(relation, new HashMap());
		((Map) restrictions.get(relation)).put(new Integer(index), restriction);
	}

	public void unsetRestrictedRelation(final Relation relation, final int index) {
		this.unsetRestrictedRelation(SchemaModificationSet.DATASET, relation,
				index);
	}

	public void unsetRestrictedRelation(final DataSetTable dsTable,
			final Relation relation, final int index)
			throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		if (globalRests != null
				&& globalRests.containsKey(relation)
				&& ((Map) globalRests.get(relation)).containsKey(new Integer(
						index)))
			throw new ValidationException(Resources
					.get("relationRestrictedGlobally"));
		this.unsetRestrictedRelation(dsTable.getName(), relation, index);
	}

	private void unsetRestrictedRelation(final String dsTableName,
			final Relation relation, final int index) {
		if (!this.isRestrictedRelation(dsTableName, relation, index))
			return;
		if (this.restrictedRelations.containsKey(dsTableName)) {
			final Map rests = (Map) this.restrictedRelations.get(dsTableName);
			((Map) rests.get(relation)).remove(new Integer(index));
			if (((Map) rests.get(relation)).isEmpty())
				rests.remove(relation);
			if (rests.isEmpty())
				this.restrictedRelations.remove(dsTableName);
		}
	}

	public boolean isRestrictedRelation(final DataSetTable dsTable,
			final Relation relation) {
		return this.isRestrictedRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation);
	}

	private boolean isRestrictedRelation(final String dsTableName,
			final Relation relation) {
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedRelations.get(dsTableName);
		return (rests != null && rests.containsKey(relation))
				|| (globalRests != null && globalRests.containsKey(relation));
	}

	public boolean isRestrictedRelation(final DataSetTable dsTable,
			final Relation relation, final int index) {
		return this.isRestrictedRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private boolean isRestrictedRelation(final String dsTableName,
			final Relation relation, final int index) {
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedRelations.get(dsTableName);
		return (rests != null && rests.containsKey(relation) && ((Map) rests
				.get(relation)).containsKey(new Integer(index)))
				|| (globalRests != null && globalRests.containsKey(relation) && ((Map) globalRests
						.get(relation)).containsKey(new Integer(index)));
	}

	public RestrictedRelationDefinition getRestrictedRelation(
			final DataSetTable dsTable, final Relation relation, final int index) {
		return this.getRestrictedRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private RestrictedRelationDefinition getRestrictedRelation(
			final String dsTableName, final Relation relation, final int index) {
		if (!this.isRestrictedRelation(dsTableName, relation, index))
			return null;
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedRelations.get(dsTableName);
		return (rests != null && rests.containsKey(relation) && ((Map) rests
				.get(relation)).containsKey(new Integer(index))) ? (RestrictedRelationDefinition) ((Map) rests
				.get(relation)).get(new Integer(index))
				: (RestrictedRelationDefinition) ((Map) globalRests
						.get(relation)).get(new Integer(index));
	}

	public Map getRestrictedRelations() {
		return this.restrictedRelations;
	}

	public String nextConcatColumn(final DataSetTable table) {
		final String tableKey = table.getName();
		int i = 1;
		if (this.concatRelations.containsKey(tableKey))
			while (((Map) this.concatRelations.get(tableKey))
					.containsKey(Resources.get("concatColumnPrefix") + i)) {
				i++;
			}
		return Resources.get("concatColumnPrefix") + i;
	}

	public void setConcatRelation(final Relation relation, final int index,
			final ConcatRelationDefinition restriction)
			throws ValidationException {
		this.setConcatRelation(SchemaModificationSet.DATASET, relation, index,
				restriction);
	}

	public void setConcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index,
			final ConcatRelationDefinition restriction)
			throws ValidationException {
		this.setConcatRelation(dsTable.getName(), relation, index, restriction);
	}

	private void setConcatRelation(final String dsTableName,
			final Relation relation, final int index,
			final ConcatRelationDefinition restriction)
			throws ValidationException {
		if (!relation.isOneToMany())
			throw new ValidationException(Resources
					.get("cannotConcatNonOneMany"));
		if (!this.concatRelations.containsKey(dsTableName))
			this.concatRelations.put(dsTableName, new HashMap());
		final Map restrictions = (Map) this.concatRelations.get(dsTableName);
		if (!restrictions.containsKey(relation))
			restrictions.put(relation, new HashMap());
		((Map) restrictions.get(relation)).put(new Integer(index), restriction);
	}

	public void unsetConcatRelation(final Relation relation, final int index) {
		this
				.unsetConcatRelation(SchemaModificationSet.DATASET, relation,
						index);
	}

	public void unsetConcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index)
			throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		if (globalRests != null
				&& globalRests.containsKey(relation)
				&& ((Map) globalRests.get(relation)).containsKey(new Integer(
						index)))
			throw new ValidationException(Resources
					.get("relationConcatedGlobally"));
		this.unsetConcatRelation(dsTable.getName(), relation, index);
	}

	private void unsetConcatRelation(final String dsTableName,
			final Relation relation, final int index) {
		if (!this.isConcatRelation(dsTableName, relation, index))
			return;
		if (this.concatRelations.containsKey(dsTableName)) {
			final Map rests = (Map) this.concatRelations.get(dsTableName);
			((Map) rests.get(relation)).remove(new Integer(index));
			if (((Map) rests.get(relation)).isEmpty())
				rests.remove(relation);
			if (rests.isEmpty())
				this.concatRelations.remove(dsTableName);
		}
	}

	public boolean isConcatRelation(final DataSetTable dsTable,
			final Relation relation) {
		return this.isConcatRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation);
	}

	private boolean isConcatRelation(final String dsTableName,
			final Relation relation) {
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.concatRelations.get(dsTableName);
		return (rests != null && rests.containsKey(relation))
				|| (globalRests != null && globalRests.containsKey(relation));
	}

	public boolean isConcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index) {
		return this.isConcatRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private boolean isConcatRelation(final String dsTableName,
			final Relation relation, final int index) {
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.concatRelations.get(dsTableName);
		return (rests != null && rests.containsKey(relation) && ((Map) rests
				.get(relation)).containsKey(new Integer(index)))
				|| (globalRests != null && globalRests.containsKey(relation) && ((Map) globalRests
						.get(relation)).containsKey(new Integer(index)));
	}

	public ConcatRelationDefinition getConcatRelation(
			final DataSetTable dsTable, final Relation relation, final int index) {
		return this.getConcatRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private ConcatRelationDefinition getConcatRelation(
			final String dsTableName, final Relation relation, final int index) {
		if (!this.isConcatRelation(dsTableName, relation, index))
			return null;
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.concatRelations.get(dsTableName);
		return (rests != null && rests.containsKey(relation) && ((Map) rests
				.get(relation)).containsKey(new Integer(index))) ? (ConcatRelationDefinition) ((Map) rests
				.get(relation)).get(new Integer(index))
				: (ConcatRelationDefinition) ((Map) globalRests.get(relation))
						.get(new Integer(index));
	}

	public Map getConcatRelations() {
		return this.concatRelations;
	}

	public void setForceIncludeRelation(final Relation relation) {
		this.setForceIncludeRelation(SchemaModificationSet.DATASET, relation);
	}

	public void setForceIncludeRelation(final DataSetTable table,
			final Relation relation) {
		this.setForceIncludeRelation(table.getName(), relation);
	}

	private void setForceIncludeRelation(final String tableName,
			final Relation relation) {
		if (!this.forceIncludeRelations.containsKey(tableName))
			this.forceIncludeRelations.put(tableName, new HashSet());
		final Collection masks = (Collection) this.forceIncludeRelations
				.get(tableName);
		masks.add(relation);
	}

	public void unsetForceIncludeRelation(final Relation relation) {
		this.unsetForceIncludeRelation(SchemaModificationSet.DATASET, relation);
	}

	public void unsetForceIncludeRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Collection globalIncs = (Collection) this.forceIncludeRelations
				.get(SchemaModificationSet.DATASET);
		if (globalIncs != null && globalIncs.contains(relation))
			throw new ValidationException(Resources
					.get("relationForcedGlobally"));
		this.unsetForceIncludeRelation(table.getName(), relation);
	}

	private void unsetForceIncludeRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isForceIncludeRelation(tableName, relation))
			return;
		if (this.forceIncludeRelations.containsKey(tableName)) {
			final Collection incs = (Collection) this.forceIncludeRelations
					.get(tableName);
			incs.remove(relation);
			if (incs.isEmpty())
				this.forceIncludeRelations.remove(tableName);
		}
	}

	public boolean isForceIncludeRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isForceIncludeRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private boolean isForceIncludeRelation(final String tableName,
			final Relation relation) {
		final Collection globalIncs = (Collection) this.forceIncludeRelations
				.get(SchemaModificationSet.DATASET);
		final Collection incs = (Collection) this.forceIncludeRelations
				.get(tableName);
		return (incs != null && incs.contains(relation))
				|| (globalIncs != null && globalIncs.contains(relation));
	}

	public Map getForceIncludeRelations() {
		return this.forceIncludeRelations;
	}

	public void setSubclassedRelation(final Relation relation)
			throws ValidationException {
		Log.debug("Flagging subclassed relation " + relation + " in "
				+ this.ds.getName());

		// Skip if already subclassed.
		if (this.isSubclassedRelation(relation))
			return;

		// Check that the relation is a 1:M relation and isn't a loop-back.
		if (!relation.isOneToMany())
			throw new ValidationException(Resources.get("subclassNotOneMany"));
		if (relation.getFirstKey().getTable().equals(
				relation.getSecondKey().getTable()))
			throw new ValidationException(Resources
					.get("subclassNotBetweenTwoTables"));

		// Work out the child end of the relation - the M end. The parent is
		// the 1 end.
		final Table parentTable = relation.getOneKey().getTable();
		final Table childTable = relation.getManyKey().getTable();

		// If there are no existing subclass relations, then we only
		// need to test that either the parent or the child is the central
		// table.
		if (this.subclassedRelations.isEmpty()) {
			if (!(parentTable.equals(this.ds.getCentralTable()) || childTable
					.equals(this.ds.getCentralTable())))
				throw new ValidationException(Resources
						.get("subclassNotOnCentralTable"));
		}

		// We have existing subclass relations.
		else {
			// We need to test if the selected relation links to
			// a table which itself has subclass relations, or
			// is the central table, and has not got an
			// existing subclass relation in the direction we
			// are working in.

			// Check that child has no M:1s and parent has no 1:Ms already.
			// Check that parent has M:1, and child has 1:M.
			boolean parentHasM1 = false;
			boolean childHasM1 = false;
			boolean parentHas1M = false;
			boolean childHas1M = false;
			for (final Iterator i = this.subclassedRelations.iterator(); i
					.hasNext();) {
				final Relation rel = (Relation) i.next();
				if (rel.getOneKey().getTable().equals(parentTable))
					parentHas1M = true;
				else if (rel.getOneKey().getTable().equals(childTable))
					childHas1M = true;
				if (rel.getManyKey().getTable().equals(parentTable))
					parentHasM1 = true;
				else if (rel.getManyKey().getTable().equals(childTable))
					childHasM1 = true;
			}

			// If child has M:1 or parent has 1:M, we cannot do this.
			if (childHasM1 || parentHas1M)
				throw new ValidationException(Resources
						.get("mixedCardinalitySubclasses"));

			// If parent is not central or doesn't have M:1, and
			// child is not central or doesn't have 1:M, we cannot do this.
			final boolean parentIsCentral = parentTable.equals(this.ds
					.getCentralTable());
			final boolean childIsCentral = parentTable.equals(this.ds
					.getCentralTable());
			if (!(parentIsCentral || childIsCentral)
					&& !(parentHasM1 || childHas1M))
				throw new ValidationException(Resources
						.get("subclassNotOnCentralTable"));
		}

		// Mark the relation.
		this.subclassedRelations.add(relation);
	}

	public boolean isSubclassedRelation(final Relation r) {
		return this.subclassedRelations.contains(r);
	}

	public Collection getSubclassedRelations() {
		return this.subclassedRelations;
	}

	public void unsetSubclassedRelation(final Relation relation) {
		Log.debug("Unflagging subclass relation " + relation + " in "
				+ this.ds.getName());
		if (!this.isSubclassedRelation(relation))
			return;
		// Break the chain first.
		final Key key = relation.getManyKey();
		if (key != null) {
			final Table target = key.getTable();
			if (!target.equals(this.ds.getCentralTable()))
				if (target.getPrimaryKey() != null)
					for (final Iterator i = target.getPrimaryKey()
							.getRelations().iterator(); i.hasNext();) {
						final Relation rel = (Relation) i.next();
						if (rel.isOneToMany())
							this.unsetSubclassedRelation(rel);
					}
		}
		// Then remove the head of the chain.
		this.subclassedRelations.remove(relation);
	}

	public void setCompoundRelation(final Relation relation, final int n) {
		this.setCompoundRelation(SchemaModificationSet.DATASET, relation, n);
	}

	public void setCompoundRelation(final DataSetTable table,
			final Relation relation, final int n) {
		this.setCompoundRelation(table.getName(), relation, n);
	}

	private void setCompoundRelation(final String tableName,
			final Relation relation, final int n) {
		if (!this.compoundRelations.containsKey(tableName))
			this.compoundRelations.put(tableName, new HashMap());
		final Map masks = (Map) this.compoundRelations.get(tableName);
		masks.put(relation, new Integer(n));
	}

	public void unsetCompoundRelation(final Relation relation) {
		this.unsetCompoundRelation(SchemaModificationSet.DATASET, relation);
	}

	public void unsetCompoundRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalComps = (Map) this.compoundRelations
				.get(SchemaModificationSet.DATASET);
		if (globalComps != null && globalComps.containsKey(relation))
			throw new ValidationException(Resources
					.get("relationCompoundedGlobally"));
		this.unsetCompoundRelation(table.getName(), relation);
	}

	private void unsetCompoundRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isCompoundRelation(tableName, relation))
			return;
		if (this.compoundRelations.containsKey(tableName)) {
			final Map comps = (Map) this.compoundRelations.get(tableName);
			comps.remove(relation);
			if (comps.isEmpty())
				this.compoundRelations.remove(tableName);
		}
	}

	public boolean isCompoundRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isCompoundRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private boolean isCompoundRelation(final String tableName,
			final Relation relation) {
		final Map globalComps = (Map) this.compoundRelations
				.get(SchemaModificationSet.DATASET);
		final Map comps = (Map) this.compoundRelations.get(tableName);
		return (comps != null && comps.containsKey(relation))
				|| (globalComps != null && globalComps.containsKey(relation));
	}

	public int getCompoundRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.getCompoundRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private int getCompoundRelation(final String tableName,
			final Relation relation) {
		final Map globalComps = (Map) this.compoundRelations
				.get(SchemaModificationSet.DATASET);
		final Map comps = (Map) this.compoundRelations.get(tableName);
		return (comps != null && comps.containsKey(relation)) ? ((Integer) comps
				.get(relation)).intValue()
				: ((Integer) globalComps.get(relation)).intValue();
	}

	public Map getCompoundRelations() {
		return this.compoundRelations;
	}

	public void replicate(final SchemaModificationSet target) {
		target.subclassedRelations.clear();
		target.subclassedRelations.addAll(this.subclassedRelations);
		target.maskedRelations.clear();
		target.maskedRelations.putAll(this.maskedRelations);
		target.forceIncludeRelations.clear();
		target.forceIncludeRelations.putAll(this.forceIncludeRelations);
		target.mergedRelations.clear();
		target.mergedRelations.addAll(this.mergedRelations);
		target.compoundRelations.clear();
		target.compoundRelations.putAll(this.compoundRelations);
		target.restrictedTables.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.restrictedTables.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.restrictedTables.put(entry.getKey(), new HashMap());
			((Map) target.restrictedTables.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
		target.restrictedRelations.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.restrictedRelations.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.restrictedRelations.put(entry.getKey(), new HashMap());
			for (final Iterator j = ((Map) entry.getValue()).entrySet()
					.iterator(); j.hasNext();) {
				final Map.Entry entry2 = (Map.Entry) j.next();
				((Map) target.restrictedRelations.get(entry.getKey())).put(entry2.getKey(), new HashMap());
				((Map) ((Map) target.restrictedRelations.get(entry.getKey()))
						.get(entry2.getKey())).putAll((Map) entry2.getValue());
			}
			((Map) target.restrictedRelations.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
		target.concatRelations.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.concatRelations.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.concatRelations.put(entry.getKey(), new HashMap());
			for (final Iterator j = ((Map) entry.getValue()).entrySet()
					.iterator(); j.hasNext();) {
				final Map.Entry entry2 = (Map.Entry) j.next();
				((Map) target.concatRelations.get(entry.getKey())).put(entry2.getKey(), new HashMap());
				((Map) ((Map) target.concatRelations.get(entry.getKey()))
						.get(entry2.getKey())).putAll((Map) entry2.getValue());
			}
			((Map) target.concatRelations.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class RestrictedTableDefinition {

		private Map aliases;

		private String expr;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 */
		public RestrictedTableDefinition(final String expr, final Map aliases) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("tblRestrictMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("tblRestrictMissingAliases"));

			// Remember the settings.
			this.aliases = new TreeMap();
			this.aliases.putAll(aliases);
			this.expr = expr;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getAliases() {
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
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param tablePrefix
		 *            the prefix to use for the table in the expression.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final String tablePrefix) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, tablePrefix + "." + col.getName());
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

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class RestrictedRelationDefinition {

		private Map leftAliases;

		private Map rightAliases;

		private String expr;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 */
		public RestrictedRelationDefinition(final String expr,
				final Map leftAliases, final Map rightAliases) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingExpression"));
			if (leftAliases == null || leftAliases.isEmpty()
					|| rightAliases == null || rightAliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingAliases"));

			// Remember the settings.
			this.leftAliases = new TreeMap();
			this.leftAliases.putAll(leftAliases);
			this.rightAliases = new TreeMap();
			this.rightAliases.putAll(rightAliases);
			this.expr = expr;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getLeftAliases() {
			return this.leftAliases;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getRightAliases() {
			return this.rightAliases;
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
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param tablePrefix
		 *            the prefix to use for the table in the expression.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final String leftTablePrefix,
				final String rightTablePrefix) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.leftAliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, leftTablePrefix + "."
						+ col.getName());
			}
			for (final Iterator i = this.rightAliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, rightTablePrefix + "."
						+ col.getName());
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

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class ConcatRelationDefinition {

		private Map aliases;

		private String expr;

		private String rowSep;

		private String colKey;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 */
		public ConcatRelationDefinition(final String expr, final Map aliases,
				final String rowSep, final String colKey) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("concatRelMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("concatRelMissingAliases"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("concatRelMissingRowSep"));

			// Remember the settings.
			this.aliases = new TreeMap();
			this.aliases.putAll(aliases);
			this.expr = expr;
			this.rowSep = rowSep;
			this.colKey = colKey;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getAliases() {
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

		public String getColKey() {
			return this.colKey;
		}

		/**
		 * Returns the expression, <i>without</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @return the unsubstituted expression.
		 */
		public String getRowSep() {
			return this.rowSep;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param tablePrefix
		 *            the prefix to use for the table in the expression.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final String tablePrefix) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, tablePrefix + "." + col.getName());
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
