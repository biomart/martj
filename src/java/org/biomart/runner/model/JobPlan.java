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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.biomart.common.resources.Settings;

/**
 * Handles planning and execution of jobs. The maximum number of threads allowed
 * is controlled by the 'maxthreads' property in the BioMart properties file.
 * See {@link Settings#getProperty(String)}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class JobPlan extends DefaultTreeModel implements Serializable {

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

	/**
	 * Create a new job plan.
	 * 
	 * @param jobId
	 *            the id of the job this plan is for.
	 */
	public JobPlan(final String jobId) {
		super(new DefaultMutableTreeNode(), true); // Dummy root.
		this.setRoot(new JobPlanSection(jobId, this, null));
		this.jobId = jobId;
		this.threadCount = 1;
	}

	/**
	 * Get the starting point for the plan.
	 * 
	 * @return the starting section.
	 */
	public JobPlanSection getStartingSection() {
		return (JobPlanSection) this.getRoot();
	}

	/**
	 * Add an action to the end of a job.
	 * 
	 * @param sectionPath
	 *            the section this applies to.
	 * @param actions
	 *            the actions to add.
	 */
	public void addActions(final String[] sectionPath, final Collection actions) {
		JobPlanSection section = this.getStartingSection();
		for (int i = 0; i < sectionPath.length; i++)
			section = section.getSubSection(sectionPath[i]);
		for (final Iterator i = actions.iterator(); i.hasNext();)
			section.addAction(new JobPlanAction((String) i.next(), section));
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
	 * Obtain the overall status.
	 * 
	 * @return the status.
	 */
	public JobStatus getStatus() {
		return this.getStartingSection().getStatus();
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

	public boolean equals(final Object other) {
		if (!(other instanceof JobPlan))
			return false;
		return this.jobId.equals(((JobPlan) other).getJobId());
	}

	/**
	 * Describes a section of a job, ie. a group of associated actions.
	 */
	public static class JobPlanSection implements Serializable, TreeNode {
		private static final long serialVersionUID = 1L;

		private final String label;

		private final Map subSections = Collections
				.synchronizedMap(new LinkedHashMap());

		private final List actions = Collections
				.synchronizedList(new ArrayList());

		private final JobPlanSection parent;

		private final JobPlan plan;

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
		 * Get the label for this section.
		 * 
		 * @return the label.
		 */
		public String getLabel() {
			return this.label;
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
		public Collection getAllSubSections() {
			return this.subSections.values();
		}

		/**
		 * Add an action.
		 * 
		 * @param action
		 *            the action to add.
		 */
		public void addAction(final JobPlanAction action) {
			this.actions.add(action);
		}

		/**
		 * Get all actions as {@link JobPlanAction} objects.
		 * 
		 * @return all actions.
		 */
		public Collection getAllActions() {
			return this.actions;
		}

		/**
		 * How many actions in total are in this section and all subsections?
		 * 
		 * @return the count.
		 */
		public int countActions() {
			int count = this.actions.size();
			for (final Iterator i = this.getAllSubSections().iterator(); i
					.hasNext();)
				count += ((JobPlanSection) i.next()).countActions();
			return count;
		}

		/**
		 * @return the ended
		 */
		public Date getEnded() {
			Date ended = null;
			for (final Iterator i = this.getAllSubSections().iterator(); i
					.hasNext();) {
				final JobPlanSection section = (JobPlanSection) i.next();
				final Date sectionEnded = section.getEnded();
				if (ended == null)
					ended = sectionEnded;
				else if (sectionEnded != null)
					ended = sectionEnded.after(ended) ? sectionEnded : ended;
			}
			for (final Iterator i = this.getAllActions().iterator(); i
					.hasNext();) {
				final JobPlanAction action = (JobPlanAction) i.next();
				final Date actionEnded = action.getEnded();
				if (ended == null)
					ended = actionEnded;
				else if (actionEnded != null)
					ended = actionEnded.after(ended) ? actionEnded : ended;
			}
			return ended;
		}

		/**
		 * @return the messages
		 */
		public String getMessages() {
			final StringBuffer messages = new StringBuffer();
			if (this.getAllSubSections().size() > 0)
				for (final Iterator i = this.getAllSubSections().iterator(); i
						.hasNext();) {
					final String message = ((JobPlanSection) i.next())
							.getMessages();
					if (message != null && message.trim().length() > 0)
						messages.append(message + '\n');
				}
			if (this.getAllActions().size() > 0)
				for (final Iterator i = this.getAllActions().iterator(); i
						.hasNext();) {
					final String message = ((JobPlanAction) i.next())
							.getMessages();
					if (message != null && message.trim().length() > 0)
						messages.append(message + '\n');
				}
			return messages.length() == 0 ? null : messages.toString();
		}

		/**
		 * @return the started
		 */
		public Date getStarted() {
			Date started = null;
			for (final Iterator i = this.getAllSubSections().iterator(); i
					.hasNext();) {
				final JobPlanSection section = (JobPlanSection) i.next();
				final Date sectionStarted = section.getStarted();
				if (started == null)
					started = sectionStarted;
				else if (sectionStarted != null)
					started = sectionStarted.before(started) ? sectionStarted
							: started;
			}
			for (final Iterator i = this.getAllActions().iterator(); i
					.hasNext();) {
				final JobPlanAction action = (JobPlanAction) i.next();
				final Date actionStarted = action.getStarted();
				if (started == null)
					started = actionStarted;
				else if (actionStarted != null)
					started = actionStarted.before(started) ? actionStarted
							: started;
			}
			return started;
		}

		/**
		 * @return the status
		 */
		public JobStatus getStatus() {
			// Status is important - we get the highest ranking one from
			// our children and return that one.
			JobStatus status = null;
			for (final Iterator i = this.getAllActions().iterator(); i
					.hasNext();) {
				final JobPlanAction action = (JobPlanAction) i.next();
				final JobStatus actionStatus = action.getStatus();
				if (status == null)
					status = actionStatus;
				else
					status = actionStatus.compareTo(status) < 0 ? actionStatus
							: status;
			}
			for (final Iterator i = this.getAllSubSections().iterator(); i
					.hasNext();) {
				final JobPlanSection section = (JobPlanSection) i.next();
				final JobStatus sectionStatus = section.getStatus();
				if (status == null)
					status = sectionStatus;
				else
					status = sectionStatus.compareTo(status) < 0 ? sectionStatus
							: status;
			}
			return status;
		}

		private Vector getChildren() {
			final Vector children = new Vector();
			children.addAll(this.actions);
			children.addAll(this.subSections.values());
			return children;
		}

		public Enumeration children() {
			return this.getChildren().elements();
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public TreeNode getChildAt(final int childIndex) {
			return (TreeNode) this.getChildren().get(childIndex);
		}

		public int getChildCount() {
			return this.subSections.size() + this.actions.size();
		}

		public int getIndex(final TreeNode node) {
			return this.getChildren().indexOf(node);
		}

		public TreeNode getParent() {
			return this.parent;
		}

		public boolean isLeaf() {
			return false;
		}

		public int hashCode() {
			return this.getLabel().hashCode();
		}

		public boolean equals(final Object other) {
			if (!(other instanceof JobPlanSection))
				return false;
			return this.getLabel() == ((JobPlanSection) other).getLabel();
		}
	}

	/**
	 * Represents an individual action.
	 */
	public static class JobPlanAction implements Serializable, TreeNode {
		private static final long serialVersionUID = 1L;

		private final String action;

		private JobStatus status;

		private Date started;

		private Date ended;

		private String messages;

		private final JobPlanSection parent;

		private static int NEXT_IDENTIFIER = 0;

		private final int sequence = JobPlanAction.NEXT_IDENTIFIER++;

		/**
		 * Create a new action.
		 * 
		 * @param action
		 *            the action to create.
		 * @param parent
		 *            the parent node.
		 */
		public JobPlanAction(final String action, final JobPlanSection parent) {
			this.action = action;
			this.status = JobStatus.NOT_QUEUED;
			this.parent = parent;
		}

		/**
		 * Obtain this action.
		 * 
		 * @return the action.
		 */
		public String getAction() {
			return this.action;
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
		 */
		public void setEnded(final Date ended) {
			this.ended = ended;
		}

		/**
		 * @return the messages
		 */
		public String getMessages() {
			return this.messages;
		}

		/**
		 * @param messages
		 *            the messages to set
		 */
		public void setMessages(final String messages) {
			this.messages = messages;
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
		 */
		public void setStarted(final Date started) {
			this.started = started;
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
		 */
		public void setStatus(final JobStatus status) {
			this.status = status;
		}

		public Enumeration children() {
			return null;
		}

		public boolean getAllowsChildren() {
			return false;
		}

		public TreeNode getChildAt(final int childIndex) {
			return null;
		}

		public int getChildCount() {
			return 0;
		}

		public int getIndex(final TreeNode node) {
			return 0;
		}

		public TreeNode getParent() {
			return this.getJobSection();
		}

		/**
		 * Find out what section this action belongs to.
		 * 
		 * @return the section.
		 */
		public JobPlanSection getJobSection() {
			return this.parent;
		}

		public boolean isLeaf() {
			return true;
		}

		/**
		 * Return a unique identifier.
		 * 
		 * @return the identifier.
		 */
		public int getUniqueIdentifier() {
			return this.sequence;
		}

		public int hashCode() {
			return this.sequence;
		}

		public boolean equals(final Object other) {
			if (!(other instanceof JobPlanAction))
				return false;
			return this.getUniqueIdentifier() == ((JobPlanAction) other)
					.getUniqueIdentifier();
		}
	}
}
