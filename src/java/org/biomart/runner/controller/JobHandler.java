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
import java.util.Collection;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.FileUtils;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobList;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobList.JobSummary;

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

	static {
		if (!jobsDir.exists())
			jobsDir.mkdir();
	}
	
	// TODO When JobHandler updates the status of a section/action, update
	// both the section/action AND the JobSummary for that job by calling
	// getStatus() on the JobPlan for that job after doing the update.

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
			// Create a job list entry.
			final JobList jobList = JobHandler.loadJobList();
			final JobSummary jobSummary = new JobSummary(jobId);
			// Set the JDBC stuff.
			jobSummary.setJDBCDriverClassName(jdbcDriverClassName);
			jobSummary.setJDBCURL(jdbcURL);
			jobSummary.setJDBCUsername(jdbcUsername);
			jobSummary.setJDBCPassword(jdbcPassword);
			jobList.addJob(jobSummary);
			JobHandler.saveJobList(jobList);
			// Create and save a plan.
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
		try {
			final JobList jobList = JobHandler.loadJobList();
			jobList.getJobSummary(jobId).setAllActionsReceived();
			JobHandler.saveJobList(jobList);
		} catch (final IOException e) {
			throw new JobException(e);
		}
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
			final JobPlan jobPlan = JobHandler.loadJobPlan(jobId);
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
	public static JobList listJobs() throws JobException {
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
			// TODO Stop job first if currently running.
			// Remove the job list entry.
			final JobList jobList = JobHandler.loadJobList();
			jobList.removeJob(jobId);
			JobHandler.saveJobList(jobList);
			// Recursively delete the job directory.
			FileUtils.delete(JobHandler.getJobPlanFile(jobId));
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	private static File getJobListFile() throws IOException {
		return new File(JobHandler.jobsDir, "list");
	}

	private static File getJobPlanFile(final String jobId) throws IOException {
		return new File(JobHandler.jobsDir, jobId);
	}

	private static JobPlan loadJobPlan(final String jobId) throws IOException {
		synchronized (JobHandler.planDirLock) {
			Log.debug("Loading plan for " + jobId);
			final File jobPlanFile = JobHandler.getJobPlanFile(jobId);
			// Load existing job plan.
			FileInputStream fis = null;
			JobPlan jobPlan = null;
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
			Log.debug("Loading list");
			final File jobListFile = JobHandler.getJobListFile();
			// Doesn't exist? Return a default new list.
			if (!jobListFile.exists())
				return new JobList();
			// Load existing job plan.
			FileInputStream fis = null;
			JobList jobList = null;
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
