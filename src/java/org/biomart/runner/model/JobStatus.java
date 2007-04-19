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
import java.util.HashMap;
import java.util.Map;

/**
 * Handles job/section/action statuses.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class JobStatus implements Serializable, Comparable {

	private static final long serialVersionUID = 1L;

	private final String status;

	private final int rank;

	private static final Map singletons = new HashMap();

	/**
	 * Unknown things.
	 */
	public static final JobStatus UNKNOWN = JobStatus
			.getJobStatus("UNKNOWN", 0);

	/**
	 * Incomplete things.
	 */
	public static final JobStatus INCOMPLETE = JobStatus.getJobStatus(
			"INCOMPLETE", 1);

	/**
	 * New things.
	 */
	public static final JobStatus NEW = JobStatus.getJobStatus("NEW", 2);

	/**
	 * Running things.
	 */
	public static final JobStatus RUNNING = JobStatus
			.getJobStatus("RUNNING", 3);

	/**
	 * Stopped things.
	 */
	public static final JobStatus STOPPED = JobStatus
			.getJobStatus("STOPPED", 4);

	/**
	 * Failed things.
	 */
	public static final JobStatus FAILED = JobStatus.getJobStatus("FAILED", 5);

	/**
	 * Queued things.
	 */
	public static final JobStatus QUEUED = JobStatus.getJobStatus("QUEUED", 6);

	/**
	 * Completed things.
	 */
	public static final JobStatus COMPLETED = JobStatus.getJobStatus(
			"COMPLETED", 7);

	private JobStatus(final String status, final int rank) {
		this.status = status;
		this.rank = rank;
	}

	private static JobStatus getJobStatus(final String status, final int rank) {
		if (!JobStatus.singletons.containsKey(status))
			JobStatus.singletons.put(status, new JobStatus(status, rank));
		return (JobStatus) JobStatus.singletons.get(status);
	}

	public int compareTo(final Object other) {
		final JobStatus otherStatus = (JobStatus) other;
		return this.rank - otherStatus.rank;
	}

	public String toString() {
		return this.status;
	}

	public int hashCode() {
		return this.status.hashCode();
	}

	public boolean equals(final Object other) {
		if (!(other instanceof JobStatus))
			return false;
		return ((JobStatus) other).status.equals(this.status);
	}
}
