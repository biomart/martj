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
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

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

	private static final Font PLAIN_FONT = Font.decode("SansSerif-PLAIN-12");

	private static final Font ITALIC_FONT = Font.decode("SansSerif-ITALIC-12");

	private static final Font BOLD_FONT = Font.decode("SansSerif-BOLD-12");

	private static final Font BOLD_ITALIC_FONT = Font
			.decode("SansSerif-BOLDITALIC-12");

	private static final Color PALE_BLUE = Color.decode("0xEEEEFF");

	private static final Color PALE_GREEN = Color.decode("0xEEFFEE");

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
			public void windowClosed(final WindowEvent e) {
				timer.cancel();
			}

			public void windowClosing(final WindowEvent e) {
				timer.cancel();
			}
		});

		// Make the RHS scrollpane containing job descriptions.
		final JobPlanPanel jobPlanPanel = new JobPlanPanel(host, port);
		jobPlanPanel.setMinimumSize(new Dimension(450, 500));

		// Make the LHS list of jobs.
		final JobSummaryListModel jobSummaryListModel = new JobSummaryListModel(
				host, port);
		final JList jobList = new JList(jobSummaryListModel);
		jobList.setCellRenderer(new JobSummaryListCellRenderer());
		jobList.setBackground(Color.WHITE);
		jobList.setOpaque(true);
		jobList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JButton refreshJobList = new JButton(Resources
				.get("refreshJobList"));
		final JPanel jobListPanel = new JPanel(new BorderLayout());
		jobListPanel.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
		jobListPanel.add(new JLabel(Resources.get("jobListTitle")),
				BorderLayout.PAGE_START);
		jobListPanel.add(new JScrollPane(jobList), BorderLayout.CENTER);
		jobListPanel.add(refreshJobList, BorderLayout.PAGE_END);
		// Separate task so can attach to refresh button too.
		final TimerTask updateJobListTask = new TimerTask() {
			public void run() {
				new LongProcess() {
					private boolean firstRun = true;

					public void run() {
						Object selectedValue = null;
						try {
							selectedValue = jobList.getSelectedValue();
							jobSummaryListModel.updateList();
						} catch (final ProtocolException e) {
							StackTrace.showStackTrace(e);
						} finally {
							// Attempt to reselect the previous item.
							if (this.firstRun && defaultJob)
								jobList.setSelectedValue(jobSummaryListModel
										.lastElement(), true);
							else if (selectedValue != null)
								jobList.setSelectedValue(selectedValue, true);
							this.firstRun = false;
						}
					}
				}.start();
			}
		};
		// Updates once every 5 minutes.
		timer.scheduleAtFixedRate(updateJobListTask, 0, 5 * 60 * 1000);
		// Updates when refresh button is hit.
		refreshJobList.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				updateJobListTask.run();
			}
		});

		// Add a listener to the list to update the pane on the right.
		jobList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent e) {
				final Object selection = jobList.getSelectedValue();
				// Update the panel on the right with the new job.
				jobPlanPanel
						.setJobSummary(selection instanceof JobSummaryListModel.JobSummaryJobNode ? ((JobSummaryListModel.JobSummaryJobNode) selection)
								.getJobSummary()
								: null);
				// Pack the window.
				MartRunnerMonitorDialog.this.pack();
			}
		});

		// Add context menu to the job list.
		jobList.addMouseListener(new MouseListener() {
			public void mouseClicked(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mouseEntered(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mouseExited(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mousePressed(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mouseReleased(final MouseEvent e) {
				this.doMouse(e);
			}

			private void doMouse(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					final int index = jobList.locationToIndex(e.getPoint());
					if (index >= 0) {
						final JPopupMenu contextMenu = ((JobSummaryListModel.JobSummaryJobNode) jobSummaryListModel
								.getElementAt(index))
								.getContextMenu(MartRunnerMonitorDialog.this);
						if (contextMenu != null
								&& contextMenu.getComponentCount() > 0) {
							contextMenu.show(jobList, e
									.getX(), e.getY());
							e.consume();
						}
						e.consume();
					}
				}
			}

		});

		// Make the content pane.
		final JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, false, jobListPanel, jobPlanPanel);
		splitPane.setOneTouchExpandable(true);

		// Set up our content pane.
		this.setContentPane(splitPane);

		// Pack the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	// Renders cells nicely.
	private static class JobSummaryListCellRenderer implements ListCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(final JList list,
				final Object value, final int index, final boolean isSelected,
				final boolean cellHasFocus) {
			final JLabel label = new JLabel();
			label.setOpaque(true);
			Color fgColor = Color.BLACK;
			Color bgColor = Color.WHITE;
			Font font = MartRunnerMonitorDialog.PLAIN_FONT;
			// A Job List entry node?
			if (value instanceof JobSummaryListModel.JobSummaryJobNode) {
				final JobSummary summary = ((JobSummaryListModel.JobSummaryJobNode) value)
						.getJobSummary();
				if (!summary.isAllActionsReceived()) {
					label.setText(summary.getJobId() + " ["
							+ Resources.get("jobStatusIncomplete") + "]");
					font = MartRunnerMonitorDialog.ITALIC_FONT;
				} else
					label.setText(summary.getJobId());
				// White/Cyan stripes.
				bgColor = index % 2 == 0 ? Color.WHITE
						: MartRunnerMonitorDialog.PALE_BLUE;
				// TODO Color-code text.
			}
			// Others are plain text.
			else
				label.setText(value.toString());
			// Always white-on-color or color-on-white.
			label.setFont(font);
			label.setForeground(isSelected ? bgColor : fgColor);
			label.setBackground(isSelected ? fgColor : bgColor);
			// Others get no extra material.
			return label;
		}
	}

	// Renders cells nicely.
	private static class JobPlanTreeCellRenderer implements TreeCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getTreeCellRendererComponent(final JTree tree,
				final Object value, final boolean sel, final boolean expanded,
				final boolean leaf, final int row, final boolean hasFocus) {
			final JLabel label = new JLabel();
			label.setOpaque(true);
			Color fgColor = Color.BLACK;
			Color bgColor = Color.WHITE;
			Font font = MartRunnerMonitorDialog.PLAIN_FONT;
			// Sections are given text labels.
			if (value instanceof JobPlanTreeModel.JobPlanSectionNode) {
				final JobPlanSection section = ((JobPlanTreeModel.JobPlanSectionNode) value)
						.getSection();
				label.setText(section.getLabel() + " ("
						+ section.countActions() + ")");
				// White/Cyan stripes.
				bgColor = row % 2 == 0 ? Color.WHITE
						: MartRunnerMonitorDialog.PALE_BLUE;
				// TODO Color-code text.
			}
			// Actions are given text labels.
			else if (value instanceof JobPlanTreeModel.JobPlanActionNode) {
				label.setText(((JobPlanTreeModel.JobPlanActionNode) value)
						.getAction().getAction());
				// White/Gray stripes.
				bgColor = row % 2 == 0 ? Color.WHITE
						: MartRunnerMonitorDialog.PALE_GREEN;
				// TODO Color-code text.
			} else
				label.setText(value.toString());
			// Always white-on-color or color-on-white.
			label.setFont(font);
			label.setForeground(sel ? bgColor : fgColor);
			label.setBackground(sel ? fgColor : bgColor);
			// Everything else is default.
			return label;
		}
	}

	// A model for representing lists of jobs.
	private static class JobSummaryListModel extends DefaultListModel {
		private static final long serialVersionUID = 1L;

		private final String host;

		private final String port;

		private JobSummaryListModel(final String host, final String port) {
			super();
			this.host = host;
			this.port = port;
		}

		private void updateList() throws ProtocolException {
			// Communicate and update model.
			this.removeAllElements();
			for (final Iterator i = Client.listJobs(this.host, this.port)
					.getAllJobs().iterator(); i.hasNext();)
				this.addElement(new JobSummaryJobNode(this, (JobSummary) i
						.next()));
		}

		private String getHost() {
			return this.host;
		}

		private String getPort() {
			return this.port;
		}

		// A node in the job list.
		private static class JobSummaryJobNode {
			private final JobSummary summary;

			private final JobSummaryListModel list;

			private JobSummaryJobNode(final JobSummaryListModel list,
					final JobSummary summary) {
				this.summary = summary;
				this.list = list;
			}

			/**
			 * Obtain the summary this node represents.
			 * 
			 * @return the summary.
			 */
			public JobSummary getJobSummary() {
				return this.summary;
			}

			/**
			 * Obtain a popup menu to display on this node.
			 * 
			 * @param parent
			 *            the component this is displayed in.
			 * @return the menu.
			 */
			public JPopupMenu getContextMenu(final Component parent) {
				final JPopupMenu menu = new JPopupMenu();
				
				// Remove job.
				final JMenuItem remove = new JMenuItem(Resources
						.get("removeJobTitle"));
				remove
						.setMnemonic(Resources.get("removeJobMnemonic").charAt(
								0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						// Confirm.
						if (JOptionPane.showConfirmDialog(parent, Resources
								.get("removeJobConfirm"), Resources
								.get("questionTitle"),
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
							new LongProcess() {
								public void run() throws Exception {
									// Remove the job.
									Client
											.removeJob(
													JobSummaryJobNode.this.list
															.getHost(),
													JobSummaryJobNode.this.list
															.getPort(),
													JobSummaryJobNode.this
															.getJobSummary()
															.getJobId());
									// Update the list.
									JobSummaryJobNode.this.list.updateList();
								}
							}.start();
					}
				});
				menu.add(remove);
				
				// That's it.
				return menu;
			}
		}
	}

	// A panel for showing the job plans in.
	private static class JobPlanPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String host;

		private final String port;

		private static final int MIN_WIDTH = 450;

		private static final int MIN_HEIGHT = 500;

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
		public JobPlanPanel(final String host, final String port) {
			super(new BorderLayout(2, 2));
			this.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
			this.host = host;
			this.port = port;
			this.setNoJob();
		}

		public Dimension getMinimumSize() {
			return new Dimension(JobPlanPanel.MIN_WIDTH,
					JobPlanPanel.MIN_HEIGHT);
		}

		public Dimension getPreferredSize() {
			Dimension prefSize = super.getPreferredSize();
			if (prefSize.width < JobPlanPanel.MIN_WIDTH)
				prefSize.width = JobPlanPanel.MIN_WIDTH;
			if (prefSize.height < JobPlanPanel.MIN_HEIGHT)
				prefSize.height = JobPlanPanel.MIN_HEIGHT;
			return prefSize;
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
				tree.setCellRenderer(new JobPlanTreeCellRenderer());
				this.add(new JScrollPane(tree), BorderLayout.CENTER);

				// Add context menu to the job plan tree.
				tree.addMouseListener(new MouseListener() {
					public void mouseClicked(final MouseEvent e) {
						this.doMouse(e);
					}

					public void mouseEntered(final MouseEvent e) {
						this.doMouse(e);
					}

					public void mouseExited(final MouseEvent e) {
						this.doMouse(e);
					}

					public void mousePressed(final MouseEvent e) {
						this.doMouse(e);
					}

					public void mouseReleased(final MouseEvent e) {
						this.doMouse(e);
					}

					private void doMouse(final MouseEvent e) {
						if (e.isPopupTrigger()) {
							final TreePath treePath = tree.getPathForLocation(e
									.getX(), e.getY());
							if (treePath != null) {
								JPopupMenu contextMenu = null;
								final Object selectedNode = treePath
										.getLastPathComponent();
								if (selectedNode instanceof JobPlanTreeModel.JobPlanSectionNode)
									contextMenu = ((JobPlanTreeModel.JobPlanSectionNode) selectedNode)
											.getContextMenu(JobPlanPanel.this);
								else if (selectedNode instanceof JobPlanTreeModel.JobPlanActionNode)
									contextMenu = ((JobPlanTreeModel.JobPlanActionNode) selectedNode)
											.getContextMenu(JobPlanPanel.this);
								if (contextMenu != null
										&& contextMenu.getComponentCount() > 0) {
									contextMenu.show(tree, e.getX(), e.getY());
									e.consume();
								}
							}
						}
					}

				});
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
			}

			/**
			 * Obtain a popup menu to display on this node.
			 * 
			 * @param parent
			 *            the component this is displayed in.
			 * @return the menu.
			 */
			public JPopupMenu getContextMenu(final Component parent) {
				final JPopupMenu menu = new JPopupMenu();
				// TODO
				return menu;
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

			public TreeNode getChildAt(final int childIndex) {
				return (TreeNode) this.children.get(childIndex);
			}

			public int getChildCount() {
				return this.children.size();
			}

			public int getIndex(final TreeNode node) {
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
			}

			/**
			 * Obtain a popup menu to display on this node.
			 * 
			 * @param parent
			 *            the component this is displayed in.
			 * @return the menu.
			 */
			public JPopupMenu getContextMenu(final Component parent) {
				final JPopupMenu menu = new JPopupMenu();
				// TODO
				return menu;
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
				return this.parent;
			}

			public boolean isLeaf() {
				return true;
			}
		}
	}
}
