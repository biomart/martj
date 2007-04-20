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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.FileUtils;
import org.biomart.common.utils.SendMail;
import org.biomart.runner.controller.JobThreadManager.JobThreadManagerListener;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobList;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.model.JobList.JobSummary;
import org.biomart.runner.model.JobPlan.JobPlanAction;
import org.biomart.runner.model.JobPlan.JobPlanSection;

/**
 * Tools for running SQL.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class JobHandler {

	private static final Object planDirLock = "__PLANDIR__LOCK__";

	private static long nextJobSuffix = System.currentTimeMillis();

	private static final File jobsDir = new File(
			Settings.getStorageDirectory(), "jobs");

	private static JobList jobListCache = null;

	private static final Map jobPlanCache = new HashMap();

	private static final Map jobManagers = new HashMap();

	static {
		if (!JobHandler.jobsDir.exists())
			JobHandler.jobsDir.mkdir();
	}

	/**
	 * Request a new job ID. Don't define the job, just request an ID for one
	 * that could be defined in future.
	 * 
	 * @return a unique job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static String nextJobId() throws JobException {
		synchronized (JobHandler.planDirLock) {
			return "" + JobHandler.nextJobSuffix++;
		}
	}

	/**
	 * Locate any jobs that say they're running and change them to say they're
	 * STOPPED.
	 * 
	 * @return the number of jobs changed.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static int stopCrashedJobs() throws JobException {
		final List stoppedActions = new ArrayList();
		final List stoppedJobs = new ArrayList();
		// Update job summaries.
		final JobList jobList = JobHandler.getJobList();
		for (final Iterator i = jobList.getAllJobs().iterator(); i.hasNext();) {
			final JobSummary summary = (JobSummary) i.next();
			// Update actions.
			final JobPlan plan = JobHandler.getJobPlan(summary.getJobId());
			final List sections = new ArrayList();
			sections.add(plan.getStartingSection());
			for (int j = 0; j < sections.size(); j++) {
				final JobPlanSection section = (JobPlanSection) sections.get(j);
				sections.addAll(section.getAllSubSections());
				for (final Iterator l = section.getAllActions().iterator(); l
						.hasNext();) {
					final JobPlanAction action = (JobPlanAction) l.next();
					if (action.getStatus().equals(JobStatus.RUNNING)) {
						stoppedActions.add(action);
						stoppedJobs.add(summary);
					}
				}
			}
		}
		if (stoppedActions.size() > 0)
			JobHandler.setActionStatus(stoppedActions, JobStatus.STOPPED);
		// Send an email when find stopped jobs.
		for (final Iterator i = stoppedJobs.iterator(); i.hasNext();) {
			final JobSummary summary = (JobSummary) i.next();
			final String jobId = summary.getJobId();
			final JobPlan plan = JobHandler.getJobPlan(jobId);
			final String contactEmail = plan.getContactEmailAddress();
			if (contactEmail != null && !"".equals(contactEmail.trim()))
				try {
					SendMail.sendSMTPMail(new String[] { contactEmail },
							Resources.get("jobStoppedSubject", "" + jobId), "");
				} catch (final MessagingException e) {
					// We don't really care.
					Log.error(e);
				}
		}
		return stoppedJobs.size();
	}

	/**
	 * Flag that a job is about to start receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param jdbcDriverClassName
	 *            the JDBC driver classname for the server the job will run
	 *            against.
	 * @param jdbcURL
	 *            the JDBC URL of the server the job will run against.
	 * @param jdbcUsername
	 *            the JDBC username for the server the job will run against.
	 * @param jdbcPassword
	 *            the JDBC password for the server the job will run against.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void beginJob(final String jobId,
			final String jdbcDriverClassName, final String jdbcURL,
			final String jdbcUsername, final String jdbcPassword)
			throws JobException {
		try {
			// Create a job list entry and a job plan.
			final JobList jobList = JobHandler.getJobList();
			final JobSummary jobSummary = jobList.getJobSummary(jobId);
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Set the JDBC stuff.
			jobPlan.setJDBCDriverClassName(jdbcDriverClassName);
			jobPlan.setJDBCURL(jdbcURL);
			jobPlan.setJDBCUsername(jdbcUsername);
			jobPlan.setJDBCPassword(jdbcPassword);
			jobList.addJob(jobSummary);
			// Save all.
			JobHandler.saveJobList(jobList);
			JobHandler.saveJobPlan(new JobPlan(jobId));
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job is about to end receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void endJob(final String jobId) throws JobException {
		final List sections = new ArrayList();
		final List actions = new ArrayList();
		final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
		sections.add(jobPlan.getStartingSection());
		for (int i = 0; i < sections.size(); i++) {
			final JobPlanSection section = (JobPlanSection) sections.get(i);
			sections.addAll(section.getAllSubSections());
			actions.addAll(section.getAllActions());
		}
		// Queue the job.
		JobHandler.setActionStatus(actions, JobStatus.QUEUED);
	}

	/**
	 * Change the status of a job summary object.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param status
	 *            the new status.
	 * @throws JobException
	 *             if it can't.
	 */
	public static void setSummaryStatus(final String jobId,
			final JobStatus status) throws JobException {
		try {
			// Create a job list entry and a job plan.
			final JobList jobList = JobHandler.getJobList();
			final JobSummary jobSummary = jobList.getJobSummary(jobId);
			// Set the status.
			jobSummary.setStatus(status);
			// Save all.
			JobHandler.saveJobList(jobList);
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Change the status of a job plan action object.
	 * 
	 * @param actions
	 *            the job action(s).
	 * @param status
	 *            the new status.
	 * @throws JobException
	 *             if it can't.
	 */
	public static void setActionStatus(final Collection actions,
			final JobStatus status) throws JobException {
		try {
			final Set jobPlans = new HashSet();
			for (final Iterator i = actions.iterator(); i.hasNext();) {
				final JobPlanAction action = (JobPlanAction) i.next();
				// Create a job list entry and a job plan.
				final JobPlan jobPlan = action.getJobSection().getJobPlan();
				// Set the status.
				action.setStatus(status);
				// Remember the plans.
				jobPlans.add(jobPlan);
			}
			for (final Iterator i = jobPlans.iterator(); i.hasNext();) {
				final JobPlan jobPlan = (JobPlan) i.next();
				// Save all.
				JobHandler.saveJobPlan(jobPlan);
				// Update the summary.
				JobHandler.setSummaryStatus(jobPlan.getJobId(), jobPlan
						.getStatus());
			}
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Change the status of a job plan action object.
	 * 
	 * @param action
	 *            the job action.
	 * @param status
	 *            the new status.
	 * @throws JobException
	 *             if it can't.
	 */
	public static void setActionStatus(final JobPlanAction action,
			final JobStatus status) throws JobException {
		JobHandler.setActionStatus(Collections.singletonList(action), status);
	}

	/**
	 * Queue some set of sections+actions.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param identifiers
	 *            the selected node identifiers.
	 * @throws JobException
	 *             if it cannot do it.
	 */
	public static void queue(final String jobId, final Collection identifiers)
			throws JobException {
		JobHandler.setStatusForSelection(jobId, identifiers, JobStatus.QUEUED);
	}

	/**
	 * Unqueue some set of sections+actions.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param identifiers
	 *            the selected node identifiers.
	 * @throws JobException
	 *             if it cannot do it.
	 */
	public static void unqueue(final String jobId, final Collection identifiers)
			throws JobException {
		JobHandler.setStatusForSelection(jobId, identifiers,
				JobStatus.NOT_QUEUED);
	}

	private static void setStatusForSelection(final String jobId,
			final Collection identifiers, final JobStatus status)
			throws JobException {
		// Iterate over all actions in order and if in set of
		// identifiers, add to list to modify.
		final List sections = new ArrayList();
		final List actions = new ArrayList();
		final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
		sections.add(jobPlan.getStartingSection());
		for (int i = 0; i < sections.size(); i++) {
			final JobPlanSection section = (JobPlanSection) sections.get(i);
			sections.addAll(section.getAllSubSections());
			for (final Iterator k = section.getAllActions().iterator(); k
					.hasNext();) {
				final JobPlanAction action = (JobPlanAction) k.next();
				if (identifiers.contains(new Integer(action
						.getUniqueIdentifier())))
					actions.add(action);
			}
		}
		JobHandler.setActionStatus(actions, status);
	}

	/**
	 * Flag that an action is to be added to the end of a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param sectionPath
	 *            the section this applies to.
	 * @param actions
	 *            the actions to add.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void addActions(final String jobId,
			final String[] sectionPath, final Collection actions)
			throws JobException {
		try {
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Add the action to the job.
			jobPlan.addActions(sectionPath, actions);
			// Save it again.
			JobHandler.saveJobPlan(jobPlan);
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Gets the plan for a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @return the plan.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static JobPlan getJobPlan(final String jobId) throws JobException {
		try {
			return JobHandler.loadJobPlan(jobId);
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Obtain a list of the jobs that MartRunner is currently managing.
	 * 
	 * @return a list of jobs.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static JobList getJobList() throws JobException {
		try {
			return JobHandler.loadJobList();
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Makes MartRunner forget about a job.
	 * 
	 * @param jobId
	 *            the job to forget.
	 * @throws JobException
	 *             if it couldn't lose its memory.
	 */
	public static void removeJob(final String jobId) throws JobException {
		try {
			// Stop job first if currently running.
			JobHandler.stopJob(jobId);
			// Remove the job list entry.
			final JobList jobList = JobHandler.getJobList();
			jobList.removeJob(jobId);
			JobHandler.saveJobList(jobList);
			// Recursively delete the job directory.
			FileUtils.delete(JobHandler.getJobPlanFile(jobId));
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job email address has changed.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param email
	 *            the new email address to use as a contact address.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void setEmailAddress(final String jobId, final String email)
			throws JobException {
		try {
			// Create a job list entry.
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Set the email stuff.
			final String trimmedEmail = email.trim().length() == 0 ? null
					: email.trim();
			if (jobPlan.getContactEmailAddress() == null
					&& trimmedEmail != null
					|| jobPlan.getContactEmailAddress() != null
					&& !jobPlan.getContactEmailAddress().equals(trimmedEmail)) {
				jobPlan.setContactEmailAddress(trimmedEmail);
				JobHandler.saveJobPlan(jobPlan);
			}
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job thread count has changed.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param threadCount
	 *            the new thread count to use.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void setThreadCount(final String jobId, final int threadCount)
			throws JobException {
		try {
			// Create a job list entry.
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Set the thread count.
			if (threadCount != jobPlan.getThreadCount()) {
				jobPlan.setThreadCount(threadCount);
				JobHandler.saveJobPlan(jobPlan);
			}
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Starts a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void startJob(final String jobId) throws JobException {
		if (JobHandler.jobManagers.containsKey(jobId))
			return; // Ignore if already running.
		final JobThreadManager manager = new JobThreadManager(jobId,
				new JobThreadManagerListener() {
					public void jobStopped(final String jobId) {
						JobHandler.jobManagers.remove(jobId);
						Log.info(Resources.get("threadManagerStopped", jobId));
					}
				});
		JobHandler.jobManagers.put(jobId, manager);
		manager.startThreadManager();
		Log.info(Resources.get("startedThreadManager", jobId));
	}

	/**
	 * Stops a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void stopJob(final String jobId) throws JobException {
		if (!JobHandler.jobManagers.containsKey(jobId))
			return; // Ignore if already stopped.
		final JobThreadManager manager = (JobThreadManager) JobHandler.jobManagers
				.get(jobId);
		manager.stopThreadManager();
		Log.info(Resources.get("stoppedThreadManager", jobId));
	}

	private static File getJobListFile() throws IOException {
		return new File(JobHandler.jobsDir, "list");
	}

	private static File getJobPlanFile(final String jobId) throws IOException {
		return new File(JobHandler.jobsDir, jobId);
	}

	private static JobPlan loadJobPlan(final String jobId) throws IOException {
		synchronized (JobHandler.planDirLock) {
			if (JobHandler.jobPlanCache.containsKey(jobId))
				return (JobPlan) JobHandler.jobPlanCache.get(jobId);
			Log.debug("Loading plan for " + jobId);
			final File jobPlanFile = JobHandler.getJobPlanFile(jobId);
			// Load existing job plan.
			FileInputStream fis = null;
			JobPlan jobPlan = null;
			// Doesn't exist? Return a default new plan.
			if (!jobPlanFile.exists())
				jobPlan = new JobPlan(jobId);
			else
				try {
					fis = new FileInputStream(jobPlanFile);
					final ObjectInputStream ois = new ObjectInputStream(fis);
					jobPlan = (JobPlan) ois.readObject();
				} catch (final IOException e) {
					throw e;
				} catch (final Throwable t) {
					// This is horrible. Make up a default one.
					Log.error(t);
					jobPlan = new JobPlan(jobId);
				} finally {
					if (fis != null)
						fis.close();
				}
			JobHandler.jobPlanCache.put(jobId, jobPlan);
			return jobPlan;
		}
	}

	private static void saveJobPlan(final JobPlan jobPlan) throws IOException {
		synchronized (JobHandler.planDirLock) {
			final String jobId = jobPlan.getJobId();
			Log.debug("Saving plan for " + jobId);
			final File jobPlanFile = JobHandler.getJobPlanFile(jobId);
			// Save (overwrite) file with plan.
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(jobPlanFile);
				final ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(jobPlan);
				oos.flush();
			} finally {
				if (fos != null)
					fos.close();
			}
		}
	}

	private static JobList loadJobList() throws IOException {
		synchronized (JobHandler.planDirLock) {
			if (JobHandler.jobListCache != null)
				return JobHandler.jobListCache;
			Log.debug("Loading list");
			final File jobListFile = JobHandler.getJobListFile();
			// Load existing job plan.
			FileInputStream fis = null;
			JobList jobList = null;
			// Doesn't exist? Return a default new list.
			if (!jobListFile.exists())
				jobList = new JobList();
			else
				try {
					fis = new FileInputStream(jobListFile);
					final ObjectInputStream ois = new ObjectInputStream(fis);
					jobList = (JobList) ois.readObject();
				} catch (final IOException e) {
					throw e;
				} catch (final Throwable t) {
					// This is horrible. Make up a default one.
					Log.error(t);
					jobList = new JobList();
				} finally {
					if (fis != null)
						fis.close();
				}
			JobHandler.jobListCache = jobList;
			return jobList;
		}
	}

	private static void saveJobList(final JobList jobList) throws IOException {
		synchronized (JobHandler.planDirLock) {
			Log.debug("Saving list");
			final File jobListFile = JobHandler.getJobListFile();
			// Save (overwrite) file with plan.
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(jobListFile);
				final ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(jobList);
				oos.flush();
				fos.flush();
			} finally {
				if (fos != null)
					fos.close();
			}
		}
	}

	// Tools are static and cannot be instantiated.
	private JobHandler() {
	}
}
