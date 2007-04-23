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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles list of jobs currently known.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class JobList implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map jobList = Collections
			.synchronizedMap(new LinkedHashMap());

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
		if (!this.jobList.containsKey(jobId))
			this.jobList.put(jobId, new JobSummary(jobId));
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

		private final String jobId;

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
		 * @param status
		 *            the status to set
		 */
		public void setStatus(final JobStatus status) {
			this.status = status;
		}

		/**
		 * Get this job's status.
		 * 
		 * @return the status.
		 */
		public JobStatus getStatus() {
			return this.status;
		}

		public int hashCode() {
			return this.jobId.hashCode();
		}

		public boolean equals(final Object other) {
			if (!(other instanceof JobSummary))
				return false;
			return this.jobId.equals(((JobSummary) other).getJobId());
		}
	}
}