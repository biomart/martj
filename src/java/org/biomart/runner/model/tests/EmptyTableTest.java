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

package org.biomart.runner.model.tests;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.runner.controller.MartRunnerProtocol;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.exceptions.TestException;
import org.biomart.runner.model.JobPlan;

/**
 * Tests all tables it can find for emptiness. Emptiness is defined as having no
 * rows, having no columns other than the key columns, or having all nulls in
 * every row for the non-key columns.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public class EmptyTableTest extends JobTest {
	private static final long serialVersionUID = 1;

	/**
	 * Set up a new test against the given job plan.
	 * 
	 * @param host
	 *            the host to run SQL against.
	 * @param port
	 *            the port to run SQL against.
	 * @param plan
	 *            the plan to test.
	 */
	public EmptyTableTest(final String host, final String port,
			final JobPlan plan) {
		super(host, port, plan);
	}

	public void runTest(final String host, final String port)
			throws SQLException {
		// Load tables and views from database, then loop over them.
		Log.debug("Starting empty table test. Loading tables.");
		final Collection tableEntries;
		try {
			tableEntries = MartRunnerProtocol.Client.listTables(host, port,
					this.getJobPlan().getJobId());
		} catch (final ProtocolException pe) {
			this.exception = new TestException(pe);
			return;
		}
		// Do the loop. Record tables alphabetically.
		final Set tables = new TreeSet();
		for (final Iterator i = tableEntries.iterator(); i.hasNext();) {
			final Map entry = (Map) i.next();
			tables.add((String) entry.get("TABLE_NAME"));
		}

		// Work out step size.
		final double stepSize = 100.0 / (double) tables.size();

		// For each table...
		boolean failed = false;
		for (final Iterator i = tables.iterator(); i.hasNext()
				&& !this.isTestStopped();) {
			final String dbTableName = (String) i.next();
			Log.debug("Testing " + dbTableName);
			// List all columns.
			final Set keyCols = new HashSet();
			final Set nonKeyCols = new HashSet();
			final Set nullableNonKeyCols = new HashSet();

			// Load tables and views from database, then loop over them.
			Log.debug("Loading columns.");
			final Collection columnEntries;
			try {
				columnEntries = MartRunnerProtocol.Client.listColumns(host,
						port, this.getJobPlan().getJobId(), dbTableName);
			} catch (final ProtocolException pe) {
				this.exception = new TestException(pe);
				return;
			}
			// Do the loop. Record tables alphabetically.
			for (final Iterator j = columnEntries.iterator(); j.hasNext();) {
				final Map entry = (Map) j.next();
				final String dbTblColName = (String) entry.get("COLUMN_NAME");
				final boolean nullable = ((Integer) entry.get("NULLABLE"))
						.intValue() != DatabaseMetaData.columnNoNulls;
				if (dbTblColName.endsWith(Resources.get("keySuffix")))
					keyCols.add(dbTblColName);
				else {
					nonKeyCols.add(dbTblColName);
					if (nullable)
						nullableNonKeyCols.add(dbTblColName);
				}
			}

			// Construct SQL to count all rows.
			Log.debug("Executing select count(1).");
			StringBuffer sql = new StringBuffer();
			sql.append("select count(1) from ");
			sql.append(this.getJobPlan().getTargetSchema());
			sql.append('.');
			sql.append(dbTableName);
			int countAll = 0;
			try {
				final Collection results = MartRunnerProtocol.Client.runSQL(
						host, port, this.getJobPlan().getJobId(), sql
								.toString());
				countAll = Integer.parseInt(((Map.Entry) ((Map) results
						.iterator().next()).entrySet().iterator().next())
						.getValue().toString());
			} catch (final ProtocolException pe) {
				this.exception = new TestException(pe);
				return;
			}

			// Process results.
			if (countAll > 0) {
				if (nonKeyCols.size() == 0) {
					// LOG - Table has no non-key columns.
					this.report.append(System.getProperty("line.separator"));
					this.report.append(Resources.get("emptyTableNoNonKeyCols",
							dbTableName));
					failed = true;
				} else if (nullableNonKeyCols.size() > 0) {
					Log.debug("Executing select count(not null).");
					// Construct SQL to count rows where all nullable-non-_key
					// cols are null.
					sql.setLength(0);
					sql.append("select count(1) from ");
					sql.append(this.getJobPlan().getTargetSchema());
					sql.append('.');
					sql.append(dbTableName);
					sql.append(" where not (");
					for (final Iterator j = nullableNonKeyCols.iterator(); j
							.hasNext()
							&& !this.isTestStopped();) {
						final String dbColName = (String) j.next();
						sql.append(dbColName);
						sql.append(" is null");
						if (j.hasNext())
							sql.append(" and ");
					}
					sql.append(')');

					int countNonNull = 0;
					try {
						final Collection results = MartRunnerProtocol.Client
								.runSQL(host, port, this.getJobPlan()
										.getJobId(), sql.toString());
						countNonNull = Integer
								.parseInt(((Map.Entry) ((Map) results
										.iterator().next()).entrySet()
										.iterator().next()).getValue()
										.toString());
					} catch (final ProtocolException pe) {
						this.exception = new TestException(pe);
						return;
					}

					// Process results.
					if (countNonNull == 0) {
						// LOG - Table contains no values in columns.
						this.report
								.append(System.getProperty("line.separator"));
						this.report.append(Resources.get(
								"emptyTableAllNonKeyColsNull", dbTableName));
						failed = true;
					}
				}
			} else {
				// LOG - Table contains no rows at all.
				this.report.append(System.getProperty("line.separator"));
				this.report.append(Resources.get("emptyTableHasNoRows",
						dbTableName));
				failed = true;
			}

			// Update progress.
			this.progress += stepSize;
		}

		if (failed) {
			this.report.append(System.getProperty("line.separator"));
			this.report.append(Resources.get("allTestsPassed"));
		}
		Log.debug("Empty table test done.");
	}
}
