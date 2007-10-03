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

import java.io.Serializable;
import java.sql.SQLException;

import org.biomart.runner.exceptions.TestException;
import org.biomart.runner.model.JobPlan;

/**
 * Represents a process that tests a job.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public abstract class JobTest implements Serializable {

	private static final long serialVersionUID = 1L;

	private final JobPlan plan;

	private boolean finished = false;

	private boolean stopped = false;

	private final String host;

	private final String port;

	/**
	 * Update this during {@link #runTest(String, String)} up to a maximum of
	 * 100 when the test is complete.
	 */
	protected int progress = 0;

	/**
	 * Append to this buffer during {@link #runTest(String, String)} to generate
	 * the textual report.
	 */
	protected final StringBuffer report = new StringBuffer();

	/**
	 * Set this if anything goes wrong during {@link #runTest(String, String)}.
	 */
	protected TestException exception = null;

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
	public JobTest(final String host, final String port, final JobPlan plan) {
		this.plan = plan;
		this.host = host;
		this.port = port;
	}

	/**
	 * Run the test. This should kick the test off in the background.
	 * 
	 * @throws TestException
	 *             if it cannot be started.
	 */
	public void startTest() throws TestException {
		// Reset report ready flag.
		this.progress = 0;
		this.report.setLength(0);
		this.finished = false;
		this.exception = null;
		this.stopped = false;
		// Delegate and start a runnable.
		new Runnable() {
			public void run() {
				try {
					JobTest.this.runTest(JobTest.this.host, JobTest.this.port);
				} catch (final SQLException se) {
					// Log it.
					exception = new TestException(se);
				}
				// When runnable complete, close connection
				// and update report ready flag.
				JobTest.this.finished = true;
			}
		}.run();
	}

	/**
	 * Provides the method which runs the test. This will append to the
	 * {@link #report} buffer and update {@link #progress}, and set the
	 * {@link #exception} if any needs to be set. It should check to see if the
	 * user has stopped it by periodically calling {@link #isTestStopped()}.
	 * 
	 * @param host
	 *            the host to send SQL to.
	 * @param port
	 *            the port to send SQL to.
	 * @throws SQLException
	 *             if it goes seriously wrong. Anything less minor should be
	 *             stored in {@link #exception}.
	 */
	protected abstract void runTest(final String host, final String port)
			throws SQLException;

	/**
	 * Get the job plan we are testing.
	 * 
	 * @return the plan.
	 */
	protected JobPlan getJobPlan() {
		return this.plan;
	}

	/**
	 * Stops the test, even if it is not yet ready.
	 */
	public void stopTest() {
		this.stopped = true;
	}

	/**
	 * The {@link #runTest(String, String)} method uses this to find out if it needs
	 * to stop early.
	 * 
	 * @return <tt>true</tt> if it has been stopped.
	 */
	protected boolean isTestStopped() {
		return this.stopped;
	}

	/**
	 * Obtain progress, from 0-100.
	 * 
	 * @return the percent complete, expressed as an integer.
	 */
	public int getProgress() {
		return this.progress;
	}

	/**
	 * Is the test report ready for inspection, or has the test failed?
	 * 
	 * @return <tt>true</tt> if it is or has.
	 */
	public boolean isFinished() {
		return this.finished;
	}

	/**
	 * Obtain the report from running the test.
	 * 
	 * @return the report.
	 */
	public String getReport() {
		return this.report.toString();
	}

	/**
	 * If the test failed, find out why.
	 * 
	 * @return the exception that caused it to fail. <tt>null</tt> if it did
	 *         not fail.
	 */
	public TestException getException() {
		return this.exception;
	}
}
