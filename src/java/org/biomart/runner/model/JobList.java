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

package org.biomart.runner.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles list of jobs currently known.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class JobList implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map jobList;

	/**
	 * Create a new job list.
	 */
	public JobList() {
		this.jobList = new LinkedHashMap();
	}

	/**
	 * Add a job.
	 * 
	 * @param job
	 *            the job to add.
	 */
	public void addJob(final JobSummary job) {
		this.jobList.put(job.getJobId(), job);
	}

	/**
	 * Remove a job.
	 * 
	 * @param jobId
	 *            the job ID to remove.
	 */
	public void removeJob(final String jobId) {
		this.jobList.remove(jobId);
	}

	/**
	 * Get the Job summary for the given id.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @return the job summary.
	 */
	public JobSummary getJobSummary(final String jobId) {
		return (JobSummary) this.jobList.get(jobId);
	}

	/**
	 * Retrieve all jobs we currently know about.
	 * 
	 * @return the set of all jobs.
	 */
	public Collection getAllJobs() {
		return this.jobList.values();
	}

	/**
	 * A summary of a job's progress.
	 */
	public static class JobSummary implements Serializable {
		private static final long serialVersionUID = 1L;

		private static final int MAX_THREAD_COUNT = 5;

		private final String jobId;

		private String JDBCDriverClassName;

		private String JDBCURL;

		private String JDBCUsername;

		private String JDBCPassword;

		private int threadCount;

		private String contactEmailAddress;

		private JobStatus status;

		/**
		 * Create a new summary for the given job.
		 * 
		 * @param jobId
		 *            the job to summarise.
		 */
		public JobSummary(final String jobId) {
			this.jobId = jobId;
			this.status = JobStatus.INCOMPLETE;
			this.threadCount = 1;
			this.contactEmailAddress = null;
		}

		/**
		 * What job do we summarise?
		 * 
		 * @return the job ID.
		 */
		public String getJobId() {
			return this.jobId;
		}

		/**
		 * Tell us that we have received all the actions we're going to get.
		 */
		public void setAllActionsReceived() {
			this.status = JobStatus.NEW;
		}

		/**
		 * Get this job's status.
		 * 
		 * @return the status.
		 */
		public JobStatus getStatus() {
			return this.status;
		}

		/**
		 * @return the JDBCDriverClassName
		 */
		public String getJDBCDriverClassName() {
			return this.JDBCDriverClassName;
		}

		/**
		 * @param driverClassName
		 *            the JDBCDriverClassName to set
		 */
		public void setJDBCDriverClassName(String driverClassName) {
			this.JDBCDriverClassName = driverClassName;
		}

		/**
		 * @return the JDBCPassword
		 */
		public String getJDBCPassword() {
			return this.JDBCPassword;
		}

		/**
		 * @param password
		 *            the JDBCPassword to set
		 */
		public void setJDBCPassword(String password) {
			if (password != null && "".equals(password.trim()))
				password = null;
			this.JDBCPassword = password;
		}

		/**
		 * @return the JDBCURL
		 */
		public String getJDBCURL() {
			return this.JDBCURL;
		}

		/**
		 * @param jdbcurl
		 *            the JDBCURL to set
		 */
		public void setJDBCURL(String jdbcurl) {
			this.JDBCURL = jdbcurl;
		}

		/**
		 * @return the JDBCUsername
		 */
		public String getJDBCUsername() {
			return this.JDBCUsername;
		}

		/**
		 * @param username
		 *            the JDBCUsername to set
		 */
		public void setJDBCUsername(String username) {
			this.JDBCUsername = username;
		}

		/**
		 * @return the threadCount
		 */
		public int getThreadCount() {
			return threadCount;
		}

		/**
		 * @param threadCount
		 *            the threadCount to set
		 */
		public void setThreadCount(int threadCount) {
			this.threadCount = threadCount;
		}

		/**
		 * @return the threadCount
		 */
		public int getMaxThreadCount() {
			return JobSummary.MAX_THREAD_COUNT;
		}

		/**
		 * @return the contactEmailAddress
		 */
		public String getContactEmailAddress() {
			return contactEmailAddress;
		}

		/**
		 * @param contactEmailAddress
		 *            the contactEmailAddress to set
		 */
		public void setContactEmailAddress(String contactEmailAddress) {
			if (contactEmailAddress != null
					&& "".equals(contactEmailAddress.trim()))
				contactEmailAddress = null;
			this.contactEmailAddress = contactEmailAddress;
		}
	}
}
