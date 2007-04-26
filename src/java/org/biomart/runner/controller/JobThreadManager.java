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

package org.biomart.runner.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.SendMail;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.model.JobPlan.JobPlanAction;
import org.biomart.runner.model.JobPlan.JobPlanSection;

/**
 * Takes a job and runs it and manages the associated threads.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class JobThreadManager extends Thread {

	private static final String SYNC_KEY = "__SYNC__KEY__";

	private final String jobId;

	private final JobThreadManagerListener listener;

	private final List jobThreadPool = Collections
			.synchronizedList(new ArrayList());

	private boolean jobStopped = false;

	/**
	 * Create a new manager for the given job ID.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param listener
	 *            a callback listener.
	 */
	public JobThreadManager(final String jobId,
			final JobThreadManagerListener listener) {
		super();
		this.jobId = jobId;
		this.listener = listener;
	}

	/**
	 * Starts us.
	 */
	public void startThreadManager() {
		this.start();
	}

	/**
	 * Stops us.
	 */
	public void stopThreadManager() {
		// Stop all threads once they have finished their current action.
		this.jobStopped = true;
	}

	public void run() {
		// Get the summary and the plan.
		try {
			final JobPlan plan = JobHandler.getJobPlan(this.jobId);
			final String contactEmail = plan.getContactEmailAddress();

			// Send emails.
			if (contactEmail != null && !"".equals(contactEmail.trim()))
				try {
					SendMail
							.sendSMTPMail(new String[] { contactEmail },
									Resources.get("jobStartingSubject", ""
											+ this.jobId), "");
				} catch (final MessagingException e) {
					// We don't really care.
					Log.error(e);
				}

			// Timer updates thread pool with correct number of threads,
			// if that number has changed. If reduced, it stops the ones
			// that are in excess. If increased, it starts new ones.
			// Run timer immediately to create initial population.
			final Timer timer = new Timer();
			final TimerTask task = new TimerTask() {
				public void run() {
					JobThreadManager.this.resizeJobThreadPool(plan,
							JobThreadManager.this.jobStopped ? 0 : plan
									.getThreadCount());
				}
			};
			timer.schedule(task, 0, 5 * 1000); // Updates every 5 seconds.

			// Monitor pool and sleep until it is empty.
			do
				try {
					Thread.sleep(5 * 1000); // Checks every 5 seconds.
				} catch (final InterruptedException e) {
					// Don't care.
				}
			while (!this.jobThreadPool.isEmpty());

			// Stop monitoring the pool.
			timer.cancel();

			// Send emails.
			if (contactEmail != null && !"".equals(contactEmail.trim())) {
				final String subject;
				if (plan.getRoot().getStatus().equals(JobStatus.COMPLETED))
					subject = Resources.get("jobEndedOKSubject", ""
							+ this.jobId);
				else
					subject = Resources.get("jobEndedNOKSubject", ""
							+ this.jobId);
				try {
					SendMail.sendSMTPMail(new String[] { contactEmail },
							subject, "");
				} catch (final MessagingException e) {
					// We don't really care.
					Log.error(e);
				}
			}

			// Do a callback.
			this.listener.jobStopped(this.jobId);
		} catch (final JobException e) {
			// It hates us.
			Log.fatal(e);
		}
	}

	private synchronized void resizeJobThreadPool(final JobPlan plan,
			final int requiredSize) {
		int actualSize = this.jobThreadPool.size();
		if (requiredSize < actualSize)
			// Reduce pool by removing oldest thread.
			while (actualSize-- > requiredSize)
				this.jobThreadPool.remove(0);
		else if (requiredSize > actualSize)
			// Increase pool.
			while (actualSize++ < requiredSize) {
				// Add thread to pool and start it running.
				final Thread thread = new JobThread(this, plan);
				thread.start();
				this.jobThreadPool.add(thread);
			}
	}

	private static class JobThread extends Thread {

		private final JobThreadManager manager;

		private final JobPlan plan;

		private static int SEQUENCE_NUMBER = 0;

		private final int sequence = JobThread.SEQUENCE_NUMBER++;

		private boolean actionFailed = false;

		private Connection connection;

		private JobPlanSection currentSection = null;

		private JobThread(final JobThreadManager manager, final JobPlan plan) {
			super();
			this.manager = manager;
			this.plan = plan;
		}

		public void run() {
			Log.info(Resources.get("jobThreadStarting", "" + this.sequence));
			// Each thread grabs sections from the queue until none are left.
			while (this.continueRunning()
					&& (this.currentSection = this.getNextSection()) != null) {
				// Process section.
				Map actions;
				try {
					actions = JobHandler.getActions(this.plan.getJobId(),
							this.currentSection.getIdentifier());
				} catch (final JobException e) {
					// Break out early and complain.
					Log.error(e);
					break;
				}
				for (final Iterator i = actions.values().iterator(); i
						.hasNext()
						&& this.continueRunning();) {
					final JobPlanAction action = (JobPlanAction) i.next();
					// Only process queued/stopped
					// actions.
					if (!(action.getStatus().equals(JobStatus.QUEUED) || action
							.getStatus().equals(JobStatus.STOPPED)))
						continue;
					// Process the action.
					else
						this.processAction(action);
				}
				this.currentSection = null;
			}
			// Quit thread by removing ourselves.
			this.manager.jobThreadPool.remove(this);
			Log.info(Resources.get("jobThreadEnding", "" + this.sequence));
			this.closeConnection();
		}

		private String getCurrentSectionIdentifier() {
			return this.currentSection == null ? null : this.currentSection
					.getIdentifier();
		}

		private boolean continueRunning() {
			return !this.actionFailed && !this.manager.jobStopped
					&& this.manager.jobThreadPool.contains(this);
		}

		public boolean equals(final Object o) {
			if (!(o instanceof JobThread))
				return false;
			else
				return this.sequence == ((JobThread) o).sequence;
		}

		private void processAction(final JobPlanAction action) {
			try {
				// Update action status to running.
				JobHandler.setStatus(this.plan.getJobId(), action
						.getIdentifier(), JobStatus.RUNNING, null);
				// Execute action.
				String failureMessage = null;
				try {
					final Connection conn = this.getConnection();
					final Statement stmt = conn.createStatement();
					stmt.execute(action.toString());
					final SQLWarning warning = conn.getWarnings();
					if (warning != null)
						throw warning;
					stmt.close();
				} catch (final Throwable t) {
					final StringWriter messageWriter = new StringWriter();
					final PrintWriter pw = new PrintWriter(messageWriter);
					t.printStackTrace(pw);
					pw.flush();
					failureMessage = messageWriter.getBuffer().toString();
				}
				// Update status to failed or completed, and store
				// exception messages if failed.
				if (failureMessage != null) {
					JobHandler.setStatus(this.plan.getJobId(), action
							.getIdentifier(), JobStatus.FAILED, failureMessage);
					this.actionFailed = true;
				} else
					JobHandler.setStatus(this.plan.getJobId(), action
							.getIdentifier(), JobStatus.COMPLETED, null);
			} catch (final JobException e) {
				// We don't really care but print it just in case.
				Log.warn(e);
			}
		}

		private Connection getConnection() throws Exception {
			// If we are already connected, test to see if we are
			// still connected. If not, reset our connection.
			if (this.connection != null && this.connection.isClosed())
				try {
					Log.debug("Closing dead JDBC connection");
					this.connection.close();
				} catch (final SQLException e) {
					// We don't care. Ignore it.
				} finally {
					this.connection = null;
				}

			// If we are not connected, we should attempt to (re)connect now.
			if (this.connection == null) {
				Log.debug("Establishing JDBC connection");
				// Start out by loading the driver.
				final Class loadedDriverClass = Class.forName(this.plan
						.getJDBCDriverClassName());

				// Check it really is an instance of Driver.
				if (!Driver.class.isAssignableFrom(loadedDriverClass))
					throw new ClassCastException(Resources
							.get("driverClassNotJDBCDriver"));

				// Connect!
				final Properties properties = new Properties();
				properties.setProperty("user", this.plan.getJDBCUsername());
				if (this.plan.getJDBCPassword() != null)
					properties.setProperty("password", this.plan
							.getJDBCPassword());
				this.connection = DriverManager.getConnection(this.plan
						.getJDBCURL(), properties);
			}
			return this.connection;
		}

		private void closeConnection() {
			if (this.connection != null)
				try {
					Log.debug("Closing JDBC connection");
					this.connection.close();
				} catch (final SQLException e) {
					// We really don't care.
				}
		}

		private synchronized JobPlanSection getNextSection() {
			synchronized (JobThreadManager.SYNC_KEY) {
				final List sections = new ArrayList();
				sections.add(this.plan.getRoot());
				for (int i = 0; i < sections.size(); i++) {
					final JobPlanSection section = (JobPlanSection) sections
							.get(i);
					// Check actions. If none failed and none running,
					// and at least one queued or stopped, then select it.
					boolean hasUsableActions = false;
					boolean hasUnusableSiblings = false;
					// Only do check if has actions at all and is not
					// running or failed.
					if (section.getActionCount() > 0
							&& (section.getStatus().equals(JobStatus.STOPPED) || section
									.getStatus().equals(JobStatus.QUEUED))) {
						hasUsableActions = true;
						// Check that no sibling sections have actions that are
						// running or failed.
						final JobPlanSection parent = (JobPlanSection) section
								.getParent();
						if (parent != null)
							hasUnusableSiblings = parent.getStatus().equals(
									JobStatus.RUNNING)
									|| parent.getStatus().equals(
											JobStatus.FAILED);
					}
					// Double-check siblings
					for (final Iterator j = this.manager.jobThreadPool
							.iterator(); !hasUnusableSiblings && j.hasNext();) {
						final JobThread thread = (JobThread) j.next();
						final String threadId = thread
								.getCurrentSectionIdentifier();
						hasUnusableSiblings = threadId != null
								&& threadId.equals(section.getIdentifier());
					}
					// If all three checks satisfied, we can use this section.
					if (hasUsableActions && !hasUnusableSiblings)
						return section;
					// Otherwise, add subsections to list and keep looking.
					else
						sections.addAll(section.getSubSections());
				}
				// Return null if there are no more sections to process.
				return null;
			}
		}
	}

	/**
	 * A set of callback methods that the manager thread uses to notify
	 * interested parties of interesting things.
	 */
	public interface JobThreadManagerListener {
		/**
		 * This method is called when all threads have finished.
		 * 
		 * @param jobId
		 *            the jobId that has stopped.
		 */
		public void jobStopped(final String jobId);
	}
}
