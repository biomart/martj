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

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.FileUtils;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobPlan;

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
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void beginJob(final String jobId) throws JobException {
		try {
			final JobPlan jobPlan = JobHandler.loadJobPlan(jobId);
			// Just save it again. We don't need to make any changes yet.
			JobHandler.saveJobPlan(jobPlan);
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job has finished receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void endJob(final String jobId) throws JobException {
		// We actually don't need to do anything at the end of the job.
	}

	/**
	 * Obtain a list of the jobs that MartRunner is currently managing.
	 * 
	 * @return a list of jobs.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static Collection listJobs() throws JobException {
		final File[] jobDirs = JobHandler.jobsDir.listFiles();
		final Collection jobIds = new ArrayList();
		for (int i = 0; i < jobDirs.length; i++)
			jobIds.add(jobDirs[i].getName());
		// TODO Introduce a JobOverview class that gets serialized
		// instead of using a list of filenames. This way we can
		// store status against each job.
		return jobIds;
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
			// TODO Only remove job if not currently in progress.
			// Recursively delete the job directory.
			FileUtils.delete(JobHandler.getJobDir(jobId));
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	private static File getJobDir(final String jobId) throws IOException {
		final File jobDir = new File(JobHandler.jobsDir, jobId);
		if (!jobDir.exists()) {
			Log.debug("Creating job directory for " + jobId);
			jobDir.mkdir();
			// Getting the SQL dir forces it to be created.
			JobHandler.getSQLDir(jobId);
			// Save a default plan.
			Log.debug("Creating default plan for " + jobId);
			JobHandler.saveJobPlan(new JobPlan(jobId));
		}
		return jobDir;
	}

	private static File getSQLDir(final String jobId) throws IOException {
		final File sqlDir = new File(JobHandler.getJobDir(jobId), "sql");
		if (!sqlDir.exists()) {
			Log.debug("Creating SQL directory for " + jobId);
			sqlDir.mkdir();
		}
		return sqlDir;
	}

	private static File getJobPlanFile(final String jobId) throws IOException {
		return new File(JobHandler.getJobDir(jobId), "plan");
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
