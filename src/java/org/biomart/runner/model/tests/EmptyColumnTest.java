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

import java.io.IOException;
import java.net.Socket;
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
import org.biomart.runner.exceptions.TestException;
import org.biomart.runner.model.JobPlan;

/**
 * Tests all columns it can find for emptiness. Emptiness is defined as having
 * at least one row, being nullable, and having all rows equal to null.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class EmptyColumnTest extends JobTest {
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
	public EmptyColumnTest(final String host, final String port,
			final JobPlan plan) {
		super(host, port, plan);
	}

	public void runTest(final String host, final String port)
			throws SQLException {
		// Load tables and views from database, then loop over them.
		Log.debug("Starting empty column test. Loading tables.");
		boolean failed = false;
		Socket clientSocket = null;
		try {
			clientSocket = MartRunnerProtocol.Client.createClientSocket(host,
					port);

			final Collection tableEntries;
			try {
				tableEntries = MartRunnerProtocol.Client.listTables(
						clientSocket, null, this.getJobPlan().getJobId());
				clientSocket.close();
			} catch (final Throwable pe) {
				this.exception = new TestException(pe);
				return;
			}
			// Do the loop. Record tables alphabetically.
			final Set tables = new TreeSet();
			for (final Iterator i = tableEntries.iterator(); i.hasNext();) {
				final Map entry = (Map) i.next();
				tables.add(entry.get("TABLE_NAME"));
			}

			// Work out step size.
			final double stepSize = 100.0 / tables.size();

			// For each table...
			for (final Iterator i = tables.iterator(); i.hasNext()
					&& !this.isTestStopped();) {
				final String dbTableName = (String) i.next();
				Log.debug("Testing " + dbTableName);
				// List all columns.
				final Set nullableNonKeyCols = new HashSet();

				// Load tables and views from database, then loop over them.
				Log.debug("Loading columns.");
				final Collection columnEntries;
				try {
					columnEntries = MartRunnerProtocol.Client.listColumns(
							clientSocket, null, this.getJobPlan().getJobId(),
							dbTableName);
				} catch (final Throwable pe) {
					this.exception = new TestException(pe);
					return;
				}
				// Do the loop. Record tables alphabetically.
				for (final Iterator j = columnEntries.iterator(); j.hasNext();) {
					final Map entry = (Map) j.next();
					final String dbTblColName = (String) entry
							.get("COLUMN_NAME");
					final boolean nullable = ((Integer) entry.get("NULLABLE"))
							.intValue() != DatabaseMetaData.columnNoNulls;
					if (!dbTblColName.endsWith(Resources.get("keySuffix"))
							&& nullable)
						nullableNonKeyCols.add(dbTblColName);
				}

				// Skip if has no nullable columns.
				if (nullableNonKeyCols.size() > 0) {

					// Construct SQL to count all rows.
					Log.debug("Executing select count(1).");
					final StringBuffer sql = new StringBuffer();
					sql.append("select count(1) from ");
					sql.append(this.getJobPlan().getTargetSchema());
					sql.append('.');
					sql.append(dbTableName);
					int countAll = 0;
					try {
						final Collection results = MartRunnerProtocol.Client
								.runSQL(clientSocket, this.getJobPlan()
										.getJobId(), sql.toString());
						countAll = Integer.parseInt(((Map.Entry) ((Map) results
								.iterator().next()).entrySet().iterator()
								.next()).getValue().toString());
					} catch (final Throwable pe) {
						this.exception = new TestException(pe);
						return;
					}

					// Skip if table has no rows.
					if (countAll > 0) {
						final double subStepSize = stepSize
								/ nullableNonKeyCols.size();
						for (final Iterator j = nullableNonKeyCols.iterator(); j
								.hasNext()
								&& !this.isTestStopped();) {
							final String colName = (String) j.next();
							Log.debug("Executing select count(not null) on "
									+ colName);
							// Use 'is null' and compare to total-row-count
							// - this uses indexes if available, as opposed
							// to 'is not null', and is therefore more
							// efficient.
							sql.setLength(0);
							sql.append("select count(1) from ");
							sql.append(this.getJobPlan().getTargetSchema());
							sql.append('.');
							sql.append(dbTableName);
							sql.append(" where ");
							sql.append(colName);
							sql.append(" is null");

							int countNull = 0;
							try {
								final Collection results = MartRunnerProtocol.Client
										.runSQL(clientSocket, this.getJobPlan()
												.getJobId(), sql.toString());
								countNull = Integer
										.parseInt(((Map.Entry) ((Map) results
												.iterator().next()).entrySet()
												.iterator().next()).getValue()
												.toString());
							} catch (final Throwable pe) {
								this.exception = new TestException(pe);
								return;
							}

							// Process results.
							if (countNull == countAll) {
								// LOG - Table contains no values in column.
								this.report.append(System
										.getProperty("line.separator"));
								this.report.append(Resources.get(
										"emptyColumnColIsNull", new String[] {
												dbTableName, colName }));
								failed = true;
							}

							// Update progress.
							this.progress += subStepSize;
						}
					} else
						// Update progress.
						this.progress += stepSize;
				} else
					// Update progress.
					this.progress += stepSize;
			}
		} catch (final IOException e) {
			this.exception = new TestException(e);
		} finally {
			if (clientSocket != null)
				try {
					clientSocket.close();
				} catch (final IOException t) {
					// Don't care.
				}
		}

		if (!failed) {
			this.report.append(System.getProperty("line.separator"));
			this.report.append(Resources.get("allTestsPassed"));
		}
		Log.debug("Empty column test done.");
	}
}
