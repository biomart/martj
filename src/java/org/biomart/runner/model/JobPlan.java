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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.runner.controller.JobHandler;
import org.biomart.runner.exceptions.JobException;

/**
 * Handles planning and execution of jobs. The maximum number of threads allowed
 * is controlled by the 'maxthreads' property in the BioMart properties file.
 * See {@link Settings#getProperty(String)}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class JobPlan implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String jobId;

	private static final int MAX_THREAD_COUNT = Integer.parseInt(Settings
			.getProperty("maxthreads") == null ? "5" : Settings
			.getProperty("maxthreads"));

	private String JDBCDriverClassName;

	private String JDBCURL;

	private String JDBCUsername;

	private String JDBCPassword;

	private int threadCount;

	private String contactEmailAddress;

	private final JobPlanSection root;

	private final Map sectionIds = new HashMap();

	/**
	 * Create a new job plan.
	 * 
	 * @param jobId
	 *            the id of the job this plan is for.
	 */
	public JobPlan(final String jobId) {
		this.root = new JobPlanSection(jobId, this, null);
		this.jobId = jobId;
		this.threadCount = 1;
	}

	/**
	 * Get the starting point for the plan.
	 * 
	 * @return the starting section.
	 */
	public JobPlanSection getRoot() {
		return this.root;
	}

	/**
	 * Obtain the section with the given ID.
	 * 
	 * @param sectionId
	 *            the ID.
	 * @return the section.
	 */
	public JobPlanSection getJobPlanSection(final String sectionId) {
		return (JobPlanSection) this.sectionIds.get(sectionId);
	}

	/**
	 * Set an action count.
	 * 
	 * @param sectionPath
	 *            the section this applies to.
	 * @param actionCount
	 *            the action count to set.
	 */
	public void setActionCount(final String[] sectionPath, final int actionCount) {
		JobPlanSection section = this.getRoot();
		for (int i = 0; i < sectionPath.length; i++)
			section = section.getSubSection(sectionPath[i]);
		section.setActionCount(actionCount);
	}

	/**
	 * Get the id of the job this plan is for.
	 * 
	 * @return the id of the job.
	 */
	public String getJobId() {
		return this.jobId;
	}

	/**
	 * @return the threadCount
	 */
	public int getThreadCount() {
		return this.threadCount;
	}

	/**
	 * @param threadCount
	 *            the threadCount to set
	 */
	public void setThreadCount(final int threadCount) {
		this.threadCount = threadCount;
	}

	/**
	 * @return the threadCount
	 */
	public int getMaxThreadCount() {
		return JobPlan.MAX_THREAD_COUNT;
	}

	/**
	 * @return the contactEmailAddress
	 */
	public String getContactEmailAddress() {
		return this.contactEmailAddress;
	}

	/**
	 * @param contactEmailAddress
	 *            the contactEmailAddress to set
	 */
	public void setContactEmailAddress(final String contactEmailAddress) {
		this.contactEmailAddress = contactEmailAddress;
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
	public void setJDBCDriverClassName(final String driverClassName) {
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
	public void setJDBCPassword(final String password) {
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
	public void setJDBCURL(final String jdbcurl) {
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
	public void setJDBCUsername(final String username) {
		this.JDBCUsername = username;
	}

	public int hashCode() {
		return this.jobId.hashCode();
	}

	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append(this.jobId);
		if (this.root.getStatus().equals(JobStatus.INCOMPLETE)) {
			buf.append(" [");
			buf.append(Resources.get("jobStatusIncomplete"));
			buf.append("]");
		}
		buf.append(" (");
		buf.append(this.root.getTotalActionCount());
		buf.append(")");
		return buf.toString();
	}

	public boolean equals(final Object other) {
		if (!(other instanceof JobPlan))
			return false;
		return this.jobId.equals(((JobPlan) other).getJobId());
	}

	/**
	 * Describes a section of a job, ie. a group of associated actions.
	 */
	public static class JobPlanSection implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String label;

		private final Map subSections = Collections
				.synchronizedMap(new LinkedHashMap());

		private int actionCount = 0;

		private final JobPlanSection parent;

		private final JobPlan plan;

		private JobStatus status;

		private Date started;

		private Date ended;

		private static int NEXT_IDENTIFIER = 0;

		private final int sequence = JobPlanSection.NEXT_IDENTIFIER++;

		/**
		 * Define a new section with the given label.
		 * 
		 * @param label
		 *            the label.
		 * @param parent
		 *            the parent node.
		 * @param plan
		 *            the plan this section is part of.
		 */
		public JobPlanSection(final String label, final JobPlan plan,
				final JobPlanSection parent) {
			this.label = label;
			this.parent = parent;
			this.plan = plan;
			this.status = JobStatus.INCOMPLETE;
			plan.sectionIds.put(this.getIdentifier(), this);
		}

		/**
		 * Obtain the job plan.
		 * 
		 * @return the job plan.
		 */
		public JobPlan getJobPlan() {
			return this.plan;
		}

		/**
		 * Obtain the parent node.
		 * 
		 * @return the parent node.
		 */
		public JobPlanSection getParent() {
			return this.parent;
		}

		/**
		 * Get a subsection. Creates it if it does not exist.
		 * 
		 * @param label
		 *            the label of the subsection.
		 * @return the subsection.
		 */
		public JobPlanSection getSubSection(final String label) {
			if (!this.subSections.containsKey(label))
				this.subSections.put(label, new JobPlanSection(label,
						this.plan, this));
			return (JobPlanSection) this.subSections.get(label);
		}

		/**
		 * Get all subsections as {@link JobPlanSection} objects.
		 * 
		 * @return all subsections.
		 */
		public Collection getSubSections() {
			return this.subSections.values();
		}

		/**
		 * Sets the action count.
		 * 
		 * @param actionCount
		 *            the action count to add.
		 */
		public void setActionCount(final int actionCount) {
			this.actionCount = actionCount;
		}

		/**
		 * How many actions are in this section alone?
		 * 
		 * @return the count.
		 */
		public int getActionCount() {
			return this.actionCount;
		}

		/**
		 * How many actions in total are in this section and all subsections?
		 * 
		 * @return the count.
		 */
		public int getTotalActionCount() {
			int count = this.getActionCount();
			for (final Iterator i = this.getSubSections().iterator(); i
					.hasNext();)
				count += ((JobPlanSection) i.next()).getTotalActionCount();
			return count;
		}

		/**
		 * @return the ended
		 */
		public Date getEnded() {
			return this.ended;
		}

		private void updateEnded(Date newEnded, final Collection allActions) {
			// If our date is not null and new date is not null
			// and new date is before our date, do nothing.
			if (this.ended != null) {
				if (newEnded != null) {
					if (newEnded.before(this.ended))
						return;
				}
				// If our date is not null and new date is null,
				// take latest date from children.
				else {
					for (final Iterator i = this.getSubSections().iterator(); i
							.hasNext();) {
						final Date childEnded = ((JobPlanSection) i.next())
								.getEnded();
						if (newEnded == null || newEnded.before(childEnded))
							newEnded = childEnded;
					}
					if (allActions != null)
						for (final Iterator i = allActions.iterator(); i
								.hasNext();) {
							final Date childEnded = ((JobPlanAction) i.next())
									.getEnded();
							if (newEnded == null || newEnded.before(childEnded))
								newEnded = childEnded;
						}
				}
			}
			// Otherwise if new date is also null, do nothing.
			else if (newEnded == null)
				return;
			// Update date as it has changed.
			this.ended = newEnded;
			if (this.parent != null)
				this.parent.updateEnded(newEnded, null);
		}

		/**
		 * @return the started
		 */
		public Date getStarted() {
			return this.started;
		}

		private void updateStarted(Date newStarted, final Collection allActions) {
			// If our date is not null and new date is not null
			// and new date is after our date, do nothing.
			if (this.started != null) {
				if (newStarted != null) {
					if (newStarted.after(this.started))
						return;
				}
				// If our date is not null and new date is null,
				// take earliest date from children.
				else {
					for (final Iterator i = this.getSubSections().iterator(); i
							.hasNext();) {
						final Date childStarted = ((JobPlanSection) i.next())
								.getStarted();
						if (newStarted == null
								|| newStarted.after(childStarted))
							newStarted = childStarted;
					}
					if (allActions != null)
						for (final Iterator i = allActions.iterator(); i
								.hasNext();) {
							final Date childStarted = ((JobPlanAction) i.next())
									.getStarted();
							if (newStarted == null
									|| newStarted.after(childStarted))
								newStarted = childStarted;
						}
				}
			}
			// Otherwise if new date is also null, do nothing.
			else if (newStarted == null)
				return;
			// Update date as it has changed.
			this.started = newStarted;
			if (this.parent != null)
				this.parent.updateStarted(newStarted, null);
		}

		/**
		 * @return the status
		 */
		public JobStatus getStatus() {
			return this.status;
		}

		private void updateStatus(JobStatus newStatus,
				final Collection allActions) {
			// New one less important? Check all and take most important.
			if (!newStatus.isMoreImportantThan(this.status)) {
				for (final Iterator i = this.getSubSections().iterator(); i
						.hasNext();) {
					final JobStatus childStatus = ((JobPlanSection) i.next())
							.getStatus();
					if (childStatus.isMoreImportantThan(newStatus))
						newStatus = childStatus;
				}
				if (allActions != null)
					for (final Iterator i = allActions.iterator(); i.hasNext();) {
						final JobStatus childStatus = ((JobPlanAction) i.next())
								.getStatus();
						if (childStatus.isMoreImportantThan(newStatus))
							newStatus = childStatus;
					}
			}
			// Same status? Keep it.
			if (newStatus.equals(this.status))
				return;
			// Change it now.
			this.status = newStatus;
			if (this.parent != null)
				this.parent.updateStatus(newStatus, null);
		}

		/**
		 * Return a unique identifier.
		 * 
		 * @return the identifier.
		 */
		public String getIdentifier() {
			return "" + this.sequence;
		}

		public int hashCode() {
			return this.sequence;
		}

		public String toString() {
			final StringBuffer buf = new StringBuffer();
			buf.append(this.label);
			if (this.getStatus().equals(JobStatus.INCOMPLETE)) {
				buf.append(" [");
				buf.append(Resources.get("jobStatusIncomplete"));
				buf.append("]");
			}
			buf.append(" (");
			buf.append(this.getTotalActionCount());
			buf.append(")");
			return buf.toString();
		}

		public boolean equals(final Object other) {
			if (!(other instanceof JobPlanSection))
				return false;
			return this.sequence == ((JobPlanSection) other).sequence;
		}
	}

	/**
	 * Represents an individual action.
	 */
	public static class JobPlanAction implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String action;

		private JobStatus status;

		private Date started;

		private Date ended;

		private String message;

		private final String parentIdentifier;

		private final String jobId;

		private static int NEXT_IDENTIFIER = 0;

		private final int sequence = JobPlanAction.NEXT_IDENTIFIER++;

		/**
		 * Create a new action.
		 * 
		 * @param jobId
		 *            the job.
		 * @param action
		 *            the action to create.
		 * @param parentIdentifier
		 *            the parent node ID.
		 */
		public JobPlanAction(final String jobId, final String action,
				final String parentIdentifier) {
			this.action = action;
			this.status = JobStatus.NOT_QUEUED;
			this.parentIdentifier = parentIdentifier;
			this.jobId = jobId;
		}

		/**
		 * @return the ended
		 */
		public Date getEnded() {
			return this.ended;
		}

		/**
		 * @param ended
		 *            the ended to set
		 * @param allActions
		 *            all actions in this section, in order to do sibling tests.
		 */
		public void setEnded(final Date ended, final Collection allActions) {
			this.ended = ended;
			try {
				JobHandler.getSection(this.jobId, this.parentIdentifier)
						.updateEnded(ended, allActions);
			} catch (final JobException e) {
				// Aaargh!
				Log.error(e);
			}
		}

		/**
		 * @return the messages
		 */
		public String getMessage() {
			return this.message;
		}

		/**
		 * @param message
		 *            the message to set
		 */
		public void setMessage(final String message) {
			this.message = message;
		}

		/**
		 * @return the started
		 */
		public Date getStarted() {
			return this.started;
		}

		/**
		 * @param started
		 *            the started to set
		 * @param allActions
		 *            all actions in this section, in order to do sibling tests.
		 */
		public void setStarted(final Date started, final Collection allActions) {
			this.started = started;
			try {
				JobHandler.getSection(this.jobId, this.parentIdentifier)
						.updateStarted(started, allActions);
			} catch (final JobException e) {
				// Aaargh!
				Log.error(e);
			}
		}

		/**
		 * @return the status
		 */
		public JobStatus getStatus() {
			return this.status;
		}

		/**
		 * @param status
		 *            the status to set
		 * @param allActions
		 *            all actions in this section, in order to do sibling tests.
		 */
		public void setStatus(final JobStatus status,
				final Collection allActions) {
			if (status.equals(this.status))
				return;
			this.status = status;
			try {
				JobHandler.getSection(this.jobId, this.parentIdentifier)
						.updateStatus(status, allActions);
			} catch (final JobException e) {
				// Aaargh!
				Log.error(e);
			}
		}

		/**
		 * Get the parent section ID.
		 * 
		 * @return the parent section ID.
		 */
		public String getParentIdentifier() {
			return this.parentIdentifier;
		}

		/**
		 * Return a unique identifier.
		 * 
		 * @return the identifier.
		 */
		public String getIdentifier() {
			return this.parentIdentifier + "#" + this.sequence;
		}

		public int hashCode() {
			return this.sequence;
		}

		public String toString() {
			final StringBuffer buf = new StringBuffer();
			buf.append(this.action);
			if (this.getStatus().equals(JobStatus.INCOMPLETE)) {
				buf.append(" [");
				buf.append(Resources.get("jobStatusIncomplete"));
				buf.append("]");
			}
			return buf.toString();
		}

		public boolean equals(final Object other) {
			if (!(other instanceof JobPlanAction))
				return false;
			return this.sequence == ((JobPlanAction) other).sequence;
		}
	}
}
