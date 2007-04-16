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

package org.biomart.builder.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobList.JobSummary;
import org.biomart.runner.model.JobPlan.JobPlanAction;
import org.biomart.runner.model.JobPlan.JobPlanSection;
import org.biomart.runner.model.MartRunnerProtocol.Client;

/**
 * This dialog monitors and interacts with SQL being run on a remote host.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class MartRunnerMonitorDialog extends JFrame {
	private static final long serialVersionUID = 1;

	/**
	 * Opens an explanation showing what a remote MartRunner host is up to.
	 * 
	 * @param host
	 *            the host to monitor.
	 * @param port
	 *            the port to connect to the host with.
	 * @param defaultJob
	 *            select the latest job, if <tt>true</tt>.
	 */
	public static void monitor(final String host, final String port,
			final boolean defaultJob) {
		// Open the dialog.
		new MartRunnerMonitorDialog(host, port, defaultJob).show();
	}

	private MartRunnerMonitorDialog(final String host, final String port,
			final boolean defaultJob) {
		// Create the blank dialog, and give it an appropriate title.
		super(Resources.get("monitorDialogTitle", new String[] { host, port }));

		// Set up a timer for us, and cancel it when the window closes.
		final Timer timer = new Timer();
		this.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				timer.cancel();
			}

			public void windowClosing(WindowEvent e) {
				timer.cancel();
			}
		});

		// Make the LHS list of jobs.
		final JobListModel jobListModel = new JobListModel(host, port);
		final JList jobList = new JList(jobListModel);
		jobList.setCellRenderer(new JobListCellRenderer());
		jobList.setBackground(Color.WHITE);
		jobList.setOpaque(true);
		jobList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JPanel jobListPanel = new JPanel(new BorderLayout());
		jobListPanel.add(new JLabel(Resources.get("jobListTitle")),
				BorderLayout.PAGE_START);
		jobListPanel.add(new JScrollPane(jobList), BorderLayout.CENTER);
		jobListPanel.setPreferredSize(new Dimension(150, 500));
		jobListPanel.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
		timer.scheduleAtFixedRate(new TimerTask() {
			private boolean firstRun = true;

			public void run() {
				Object selectedValue = null;
				try {
					selectedValue = jobList.getSelectedValue();
					jobListModel.updateList();
				} catch (final ProtocolException e) {
					StackTrace.showStackTrace(e);
				} finally {
					// Attempt to reselect the previous item.
					if (this.firstRun && defaultJob)
						jobList.setSelectedValue(jobListModel.lastElement(),
								true);
					else if (selectedValue != null)
						jobList.setSelectedValue(selectedValue, true);
					this.firstRun = false;
				}
			}
		}, 0, 5 * 60 * 1000); // Updates once every 5 minutes.

		// Make the RHS scrollpane containing job descriptions.
		final JobDescPanel jobDescPanel = new JobDescPanel(host, port);
		jobDescPanel.setPreferredSize(new Dimension(450, 500));

		// Make the content pane.
		final JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, false, jobListPanel, jobDescPanel);
		splitPane.setOneTouchExpandable(true);

		// Add a listener to the list to update the pane on the right.
		jobList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent e) {
				final Object selection = jobList.getSelectedValue();
				// Update the panel on the right with the new job.
				jobDescPanel.setJobSummary((JobSummary) selection);
				// Pack the window.
				MartRunnerMonitorDialog.this.pack();
			}
		});

		// TODO Add context menu to each job in the job list.

		// Set up our content pane.
		this.setContentPane(splitPane);

		// Pack the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	// Renders cells nicely.
	private static class JobListCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		private JobListCellRenderer() {
			super();
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			// A Job List entry node?
			if (value instanceof JobSummary) {
				value = ((JobSummary) value).getJobId();
			}
			// Default is to do what parent does.
			return super.getListCellRendererComponent(list, value, index,
					isSelected, cellHasFocus);
		}
	}

	// Renders cells nicely.
	private static class JobPlanCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;

		private JobPlanCellRenderer() {
			super();
			this.setBackgroundNonSelectionColor(Color.WHITE);
			this.setBackgroundSelectionColor(Color.YELLOW);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			// Sections are given text labels.
			if (value instanceof JobPlanTreeModel.JobPlanSectionNode)
				value = ((JobPlanTreeModel.JobPlanSectionNode) value)
						.getSection().getLabel();
			// Actions are given text labels.
			else if (value instanceof JobPlanTreeModel.JobPlanActionNode)
				value = ((JobPlanTreeModel.JobPlanActionNode) value)
						.getAction().getAction();
			// Everything else is default.
			return super.getTreeCellRendererComponent(tree, value, sel,
					expanded, leaf, row, hasFocus);
		}
	}

	// A model for representing lists of jobs.
	private static class JobListModel extends DefaultListModel {
		private static final long serialVersionUID = 1L;

		private final String host;

		private final String port;

		private JobListModel(final String host, final String port) {
			super();
			this.host = host;
			this.port = port;
		}

		private void updateList() throws ProtocolException {
			// Communicate and update model.
			this.removeAllElements();
			for (final Iterator i = Client.listJobs(host, port).getAllJobs()
					.iterator(); i.hasNext();)
				this.addElement((JobSummary) i.next());
		}
	}

	private static class JobDescPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String host;

		private final String port;

		/**
		 * Create a new job description panel. In the top half goes two panes -
		 * an email settings pane, and the job tree view. In the bottom half
		 * goes an explanation panel.
		 * 
		 * @param host
		 *            the host to talk to MartRunner at.
		 * @param port
		 *            the port to talk to MartRunner at.
		 */
		public JobDescPanel(final String host, final String port) {
			super(new BorderLayout(2, 2));
			this.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
			this.host = host;
			this.port = port;
			this.setNoJob();
		}

		private void setNoJob() {
			this.removeAll();
			this.add(new JLabel(Resources.get("noJobSelected")),
					BorderLayout.PAGE_START);
		}

		private void setJobSummary(final JobSummary jobSummary) {
			if (jobSummary == null)
				this.setNoJob();
			else {
				this.removeAll();
				final String jobId = jobSummary.getJobId();
				this.add(new JLabel(Resources.get("jobSelected", jobId)),
						BorderLayout.PAGE_START);
				// Create a JTree to hold job details.
				final JobPlanTreeModel treeModel = new JobPlanTreeModel(
						this.host, this.port, jobId);
				final JTree tree = new JTree(treeModel);
				tree.setOpaque(true);
				tree.setBackground(Color.WHITE);
				tree.setEditable(false); // Make it read-only.
				tree.setRootVisible(true); // Always show the root node.
				tree.setCellRenderer(new JobPlanCellRenderer());
				this.add(new JScrollPane(tree), BorderLayout.CENTER);
			}
		}
	}

	// Represents a job plan as a tree model.
	private static class JobPlanTreeModel extends DefaultTreeModel {
		private static final long serialVersionUID = 1L;

		private static final TreeNode LOADING_TREE = new DefaultMutableTreeNode(
				Resources.get("loadingTree"));

		private static final TreeNode BROKEN_TREE = new DefaultMutableTreeNode(
				Resources.get("brokenTree"));

		/**
		 * Creates a new tree model which auto-updates every five minutes
		 * against the given jobId and host/port combo.
		 * 
		 * @param host
		 *            the host.
		 * @param port
		 *            the port.
		 * @param jobId
		 *            the job ID.
		 */
		public JobPlanTreeModel(final String host, final String port,
				final String jobId) {
			super(JobPlanTreeModel.LOADING_TREE, true);

			// Set the loading message.
			this.setRoot(JobPlanTreeModel.LOADING_TREE);
			// Update the views showing the tree.
			this.reload();

			new LongProcess() {
				public void run() throws Exception {
					// Get job details.
					final JobPlan jobPlan = Client
							.getJobPlan(host, port, jobId);
					// Add elements of the job to tree.
					if (jobPlan != null)
						JobPlanTreeModel.this.setRoot(new JobPlanSectionNode(
								null, jobPlan.getStartingSection()));
					else
						JobPlanTreeModel.this
								.setRoot(JobPlanTreeModel.BROKEN_TREE);
					// Update the views showing the tree.
					JobPlanTreeModel.this.reload();
				}
			}.start();
		}

		// A tree node for a section.
		private static class JobPlanSectionNode implements TreeNode {
			private final JobPlanSectionNode parent;

			private final JobPlanSection section;

			private final Vector children;

			private JobPlanSectionNode(final JobPlanSectionNode parent,
					final JobPlanSection section) {
				this.parent = parent;
				this.section = section;
				// Build children - combination of sections and actions, actions
				// first.
				this.children = new Vector();
				for (final Iterator i = this.section.getAllActions().iterator(); i
						.hasNext();)
					this.children.add(new JobPlanActionNode(this,
							(JobPlanAction) i.next()));
				for (final Iterator i = this.section.getAllSubSections()
						.iterator(); i.hasNext();)
					this.children.add(new JobPlanSectionNode(this,
							(JobPlanSection) i.next()));

				// TODO Context menus!
			}

			/**
			 * Get the section this node represents.
			 * 
			 * @return the section.
			 */
			public JobPlanSection getSection() {
				return this.section;
			}

			public Enumeration children() {
				return this.children.elements();
			}

			public boolean getAllowsChildren() {
				return true;
			}

			public TreeNode getChildAt(int childIndex) {
				return (TreeNode) this.children.get(childIndex);
			}

			public int getChildCount() {
				return this.children.size();
			}

			public int getIndex(TreeNode node) {
				return this.children.indexOf(node);
			}

			public TreeNode getParent() {
				return this.parent;
			}

			public boolean isLeaf() {
				return false;
			}
		}

		// A tree node for an action.
		private static class JobPlanActionNode implements TreeNode {

			private final JobPlanSectionNode parent;

			private final JobPlanAction action;

			private JobPlanActionNode(final JobPlanSectionNode parent,
					final JobPlanAction action) {
				this.parent = parent;
				this.action = action;

				// TODO Context menus!
			}

			/**
			 * Get the action this node represents.
			 * 
			 * @return the action.
			 */
			public JobPlanAction getAction() {
				return this.action;
			}

			public Enumeration children() {
				return null;
			}

			public boolean getAllowsChildren() {
				return false;
			}

			public TreeNode getChildAt(int childIndex) {
				return null;
			}

			public int getChildCount() {
				return 0;
			}

			public int getIndex(TreeNode node) {
				return 0;
			}

			public TreeNode getParent() {
				return this.parent;
			}

			public boolean isLeaf() {
				return true;
			}
		}
	}
}
