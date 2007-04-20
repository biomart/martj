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

import javax.mail.MessagingException;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.SendMail;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.model.JobList.JobSummary;

/**
 * Takes a job and runs it and manages the associated threads.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class JobThreadManager extends Thread {

	private final String jobId;

	private final JobThreadManagerListener listener;

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
		// TODO Empty the queue and stop all threads.
	}

	public void run() {
		// Get the summary and the plan.
		try {
			final JobSummary summary = JobHandler.getJobList().getJobSummary(
					this.jobId);
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

			// TODO Group together tasks and create a queue with waitpoints.
			// TODO Create a thread pool with N initial threads.
			// TODO Each thread grabs tasks from the queue.
			// Queue = first section with queued/stopped actions by tree
			// walk and do not go past sections with running actions when
			// searching (this differs from running sections so careful!).
			// If the section contains any failed actions, it is not run at all.
			// TODO Timer updates thread pool with correct number of threads.
			// TODO Keep going until queue is empty and threads are all done.

			// TODO When last thread is finished...

			// Send emails.
			if (contactEmail != null && !"".equals(contactEmail.trim())) {
				final String subject;
				if (summary.getStatus().equals(JobStatus.COMPLETED))
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
