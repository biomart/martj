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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.biomart.builder.view.gui.dialogs.MartRunnerMonitorDialog.JobPlanTreeModel.WrapperTreeNode;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;
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

	private static final Map STATUS_COLOR_MAP = new HashMap();

	static {
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.NOT_QUEUED,
				Color.BLACK);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.INCOMPLETE,
				Color.BLACK);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.QUEUED,
				Color.MAGENTA);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.FAILED,
				Color.RED);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.RUNNING,
				Color.YELLOW);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.STOPPED,
				Color.ORANGE);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.COMPLETED,
				Color.GREEN);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.UNKNOWN,
				Color.LIGHT_GRAY);
	}

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

		// Make the RHS scrollpane containing job descriptions.
		final JobPlanPanel jobPlanPanel = new JobPlanPanel(host, port);

		// Make the LHS list of jobs.
		final JobSummaryListModel jobSummaryListModel = new JobSummaryListModel(
				host, port);
		final JList jobList = new JList(jobSummaryListModel);
		jobList.setCellRenderer(new JobSummaryListCellRenderer());
		jobList.setBackground(Color.WHITE);
		jobList.setOpaque(true);
		jobList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JButton refreshJobList = new JButton(Resources
				.get("refreshButton"));
		final JPanel jobListPanel = new JPanel(new BorderLayout());
		jobListPanel.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
		jobListPanel.add(new JLabel(Resources.get("jobListTitle")),
				BorderLayout.PAGE_START);
		jobListPanel.add(new JScrollPane(jobList), BorderLayout.CENTER);
		jobListPanel.add(refreshJobList, BorderLayout.PAGE_END);
		// Updates when refresh button is hit.
		refreshJobList.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				new LongProcess() {
					private boolean firstRun = true;

					public void run() {
						try {
							jobSummaryListModel.updateList();
						} catch (final ProtocolException e) {
							StackTrace.showStackTrace(e);
						} finally {
							// Attempt to select the first item on first run.
							if (this.firstRun && defaultJob)
								jobList.setSelectedValue(jobSummaryListModel
										.lastElement(), true);
							this.firstRun = false;
						}
					}
				}.start();
			}
		});
		// Update now.
		refreshJobList.doClick();

		// Add a listener to the list to update the pane on the right.
		jobList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent e) {
				final Object selection = jobList.getSelectedValue();
				if (!e.getValueIsAdjusting()) {
					// Update the panel on the right with the new job.
					jobPlanPanel
							.setJobSummary(selection instanceof JobSummary ? (JobSummary) selection
									: null);
					// Pack the window.
					MartRunnerMonitorDialog.this.pack();
				}
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
						final JobSummary summary = (JobSummary) jobSummaryListModel
								.getElementAt(index);
						final JPopupMenu menu = new JPopupMenu();

						// Remove job.
						final JMenuItem remove = new JMenuItem(Resources
								.get("removeJobTitle"));
						remove.setMnemonic(Resources.get("removeJobMnemonic")
								.charAt(0));
						remove.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent evt) {
								// Confirm.
								if (JOptionPane.showConfirmDialog(jobList,
										Resources.get("removeJobConfirm"),
										Resources.get("questionTitle"),
										JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
									new LongProcess() {
										public void run() throws Exception {
											// Remove the job.
											Client.removeJob(host, port,
													summary.getJobId());
											// Update the list.
											refreshJobList.doClick();
										}
									}.start();
							}
						});
						menu.add(remove);

						// Show the menu.
						menu.show(jobList, e.getX(), e.getY());
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
			if (value instanceof JobSummary) {
				final JobSummary summary = (JobSummary) value;
				final String summaryText = summary.getJobId();
				final JobStatus status = summary.getStatus();
				if (status.equals(JobStatus.INCOMPLETE)) {
					label.setText(summaryText + " ["
							+ Resources.get("jobStatusIncomplete") + "]");
					font = MartRunnerMonitorDialog.ITALIC_FONT;
				} else {
					label.setText(summaryText);
					if (status.equals(JobStatus.FAILED)
							|| status.equals(JobStatus.STOPPED))
						font = MartRunnerMonitorDialog.BOLD_FONT;
					else if (status.equals(JobStatus.RUNNING))
						font = MartRunnerMonitorDialog.BOLD_ITALIC_FONT;
				}
				// White/Cyan stripes.
				bgColor = index % 2 == 0 ? Color.WHITE
						: MartRunnerMonitorDialog.PALE_BLUE;
				// Color-code text.
				fgColor = (Color) MartRunnerMonitorDialog.STATUS_COLOR_MAP
						.get(status);
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
				Object value, final boolean sel, final boolean expanded,
				final boolean leaf, final int row, final boolean hasFocus) {
			final JLabel label = new JLabel();
			label.setOpaque(true);
			Color fgColor = Color.BLACK;
			Color bgColor = Color.WHITE;
			Font font = MartRunnerMonitorDialog.PLAIN_FONT;
			// Sections are given text labels.
			if (value instanceof WrapperTreeNode)
				value = ((WrapperTreeNode) value).getRootJobPlanSection();
			if (value instanceof JobPlanSection) {
				final JobPlanSection section = (JobPlanSection) value;
				final String sectionText = section.getLabel() + " ("
						+ section.countActions() + ")";
				final JobStatus status = section.getStatus();
				if (status.equals(JobStatus.INCOMPLETE)) {
					label.setText(sectionText + " ["
							+ Resources.get("jobStatusIncomplete") + "]");
					font = MartRunnerMonitorDialog.ITALIC_FONT;
				} else {
					label.setText(sectionText);
					if (status.equals(JobStatus.FAILED)
							|| status.equals(JobStatus.STOPPED))
						font = MartRunnerMonitorDialog.BOLD_FONT;
					else if (status.equals(JobStatus.RUNNING))
						font = MartRunnerMonitorDialog.BOLD_ITALIC_FONT;
				}
				// White/Cyan stripes.
				bgColor = row % 2 == 0 ? Color.WHITE
						: MartRunnerMonitorDialog.PALE_BLUE;
				// Color-code text.
				fgColor = (Color) MartRunnerMonitorDialog.STATUS_COLOR_MAP
						.get(status);
			}
			// Actions are given text labels.
			else if (value instanceof JobPlanAction) {
				final JobPlanAction action = (JobPlanAction) value;
				final String actionText = action.getAction();
				final JobStatus status = action.getStatus();
				if (status.equals(JobStatus.INCOMPLETE)) {
					label.setText(actionText + " ["
							+ Resources.get("jobStatusIncomplete") + "]");
					font = MartRunnerMonitorDialog.ITALIC_FONT;
				} else {
					label.setText(actionText);
					if (status.equals(JobStatus.FAILED)
							|| status.equals(JobStatus.STOPPED))
						font = MartRunnerMonitorDialog.BOLD_FONT;
					else if (status.equals(JobStatus.RUNNING))
						font = MartRunnerMonitorDialog.BOLD_ITALIC_FONT;
				}
				// White/Cyan stripes.
				bgColor = row % 2 == 0 ? Color.WHITE
						: MartRunnerMonitorDialog.PALE_GREEN;
				// Color-code text.
				fgColor = (Color) MartRunnerMonitorDialog.STATUS_COLOR_MAP
						.get(status);
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
				this.addElement((JobSummary) i.next());
		}
	}

	// A panel for showing the job plans in.
	private static class JobPlanPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String host;

		private final String port;

		private JScrollPane treeScroller;

		private JTree tree;

		private JobPlanTreeModel treeModel;

		private String jobId;

		private final JTextField jobIdField;

		private final JSpinner threadSpinner;

		private final SpinnerNumberModel threadSpinnerModel;

		private final JTextField jdbcUrl;

		private final JTextField jdbcUser;

		private final JTextField contactEmail;

		private final JButton updateEmailButton;

		private final JFormattedTextField started;

		private final JFormattedTextField finished;

		private final JTextField elapsed;

		private final JTextField status;

		private final JTextArea messages;

		private final JPanel footerPanel;

		private final JButton startJob;

		private final JButton stopJob;

		private final JButton refreshJobTree;

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

			// Create constraints for labels that are not in the last row.
			final GridBagConstraints labelConstraints = new GridBagConstraints();
			labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
			labelConstraints.fill = GridBagConstraints.HORIZONTAL;
			labelConstraints.anchor = GridBagConstraints.LINE_END;
			labelConstraints.insets = new Insets(0, 2, 0, 0);
			// Create constraints for fields that are not in the last row.
			final GridBagConstraints fieldConstraints = new GridBagConstraints();
			fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
			fieldConstraints.fill = GridBagConstraints.NONE;
			fieldConstraints.anchor = GridBagConstraints.LINE_START;
			fieldConstraints.insets = new Insets(0, 1, 0, 2);
			// Create constraints for labels that are in the last row.
			final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
					.clone();
			labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
			// Create constraints for fields that are in the last row.
			final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
					.clone();
			fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

			// Create a panel to hold the header details.
			final JPanel headerPanel = new JPanel(new GridBagLayout());

			// Create the user-interactive bits of the panel.
			this.threadSpinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
			this.threadSpinner = new JSpinner(this.threadSpinnerModel);
			// Spinner listener updates summary thread count instantly.
			this.threadSpinnerModel.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (JobPlanPanel.this.jobId != null)
						try {
							Client
									.setThreadCount(
											JobPlanPanel.this.host,
											JobPlanPanel.this.port,
											JobPlanPanel.this.jobId,
											((Integer) JobPlanPanel.this.threadSpinnerModel
													.getValue()).intValue());
						} catch (final ProtocolException pe) {
							StackTrace.showStackTrace(pe);
						}
				}
			});

			// Populate the header panel.
			JLabel label = new JLabel(Resources.get("jobIdLabel"));
			headerPanel.add(label, labelConstraints);
			JPanel field = new JPanel();
			this.jobIdField = new JTextField(12);
			this.jobIdField.setEnabled(false);
			field.add(this.jobIdField);
			field.add(new JLabel(Resources.get("threadCountLabel")));
			field.add(threadSpinner);
			headerPanel.add(field, fieldConstraints);

			label = new JLabel(Resources.get("jdbcURLLabel"));
			headerPanel.add(label, labelConstraints);
			field = new JPanel();
			this.jdbcUrl = new JTextField(30);
			this.jdbcUrl.setEnabled(false);
			field.add(this.jdbcUrl);
			field.add(new JLabel(Resources.get("usernameLabel")));
			this.jdbcUser = new JTextField(12);
			this.jdbcUser.setEnabled(false);
			field.add(this.jdbcUser);
			headerPanel.add(field, fieldConstraints);

			label = new JLabel(Resources.get("contactEmailLabel"));
			headerPanel.add(label, labelConstraints);
			field = new JPanel();
			this.contactEmail = new JTextField(30);
			field.add(this.contactEmail);
			this.updateEmailButton = new JButton(Resources.get("updateButton"));
			// Listener on button to instantly update email address.
			this.updateEmailButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					if (JobPlanPanel.this.jobId != null)
						try {
							Client.setEmailAddress(JobPlanPanel.this.host,
									JobPlanPanel.this.port,
									JobPlanPanel.this.jobId,
									JobPlanPanel.this.contactEmail.getText()
											.trim());
						} catch (final ProtocolException pe) {
							StackTrace.showStackTrace(pe);
						}
				}
			});
			field.add(this.updateEmailButton);
			headerPanel.add(field, fieldConstraints);

			headerPanel.add(new JLabel(), labelLastRowConstraints);
			field = new JPanel();
			this.startJob = new JButton(Resources.get("startJobButton"));
			this.stopJob = new JButton(Resources.get("stopJobButton"));
			// Button listeners to start+stop jobs.
			this.startJob.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					if (JobPlanPanel.this.jobId != null)
						try {
							Client.startJob(JobPlanPanel.this.host,
									JobPlanPanel.this.port,
									JobPlanPanel.this.jobId);
							JobPlanPanel.this.startJob.setEnabled(false);
							JobPlanPanel.this.stopJob.setEnabled(true);
						} catch (final ProtocolException pe) {
							StackTrace.showStackTrace(pe);
						}
				}
			});
			this.startJob.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					if (JobPlanPanel.this.jobId != null)
						try {
							Client.stopJob(JobPlanPanel.this.host,
									JobPlanPanel.this.port,
									JobPlanPanel.this.jobId);
							JobPlanPanel.this.startJob.setEnabled(true);
							JobPlanPanel.this.stopJob.setEnabled(false);
						} catch (final ProtocolException pe) {
							StackTrace.showStackTrace(pe);
						}
				}
			});
			this.refreshJobTree = new JButton(Resources.get("refreshButton"));
			// Button listener to update tree details.
			this.refreshJobTree.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					final TreePath path = JobPlanPanel.this.tree
							.getSelectionPath();
					// FIXME Not repainting properly!
					JobPlanPanel.this.treeModel.update();
					JobPlanPanel.this.tree.repaint();
					// Reselect the node to update the summary box.
					if (path != null)
						JobPlanPanel.this.tree.setSelectionPath(path);
				}
			});
			field.add(this.startJob);
			field.add(this.stopJob);
			field.add(this.refreshJobTree);
			headerPanel.add(field, fieldLastRowConstraints);

			// Create a panel to hold the footer details.
			this.footerPanel = new JPanel(new GridBagLayout());

			// Populate the footer panel.
			label = new JLabel(Resources.get("statusLabel"));
			this.footerPanel.add(label, labelConstraints);
			field = new JPanel();
			this.status = new JTextField(12);
			this.status.setEnabled(false);
			field.add(this.status);
			field.add(new JLabel(Resources.get("elapsedLabel")));
			this.elapsed = new JTextField(12);
			this.elapsed.setEnabled(false);
			field.add(this.elapsed);
			field.add(new JLabel(Resources.get("startedLabel")));
			this.started = new JFormattedTextField(new SimpleDateFormat());
			this.started.setColumns(12);
			this.started.setEnabled(false);
			field.add(this.started);
			field.add(new JLabel(Resources.get("finishedLabel")));
			this.finished = new JFormattedTextField(new SimpleDateFormat());
			this.finished.setColumns(12);
			this.finished.setEnabled(false);
			field.add(this.finished);
			this.footerPanel.add(field, fieldConstraints);

			label = new JLabel(Resources.get("messagesLabel"));
			this.footerPanel.add(label, labelConstraints);
			field = new JPanel();
			this.messages = new JTextArea(5, 100);
			this.messages.setEnabled(false);
			field.add(new JScrollPane(this.messages));
			this.footerPanel.add(field, fieldConstraints);

			// Update the layout.
			headerPanel.validate();
			headerPanel.setMinimumSize(headerPanel.getPreferredSize());
			this.footerPanel.validate();
			this.footerPanel
					.setMinimumSize(this.footerPanel.getPreferredSize());
			this.add(this.footerPanel, BorderLayout.PAGE_END);
			this.add(headerPanel, BorderLayout.PAGE_START);
			this.add(new JLabel(), BorderLayout.CENTER); // Placeholder

			// Set the default values.
			this.setNoJob();
		}

		private void setNoJob() {
			if (this.treeScroller != null)
				this.remove(this.treeScroller);
			this.treeScroller = null;
			this.add(new JLabel(), BorderLayout.CENTER); // Placeholder
			this.jobId = null;
			this.jobIdField.setText(Resources.get("noJobSelected"));
			this.threadSpinnerModel.setValue(new Integer(1));
			this.threadSpinner.setEnabled(false);
			this.jdbcUrl.setText(null);
			this.jdbcUser.setText(null);
			this.contactEmail.setText(null);
			this.contactEmail.setEnabled(false);
			this.updateEmailButton.setEnabled(false);
			this.footerPanel.setVisible(false);
			this.startJob.setEnabled(false);
			this.stopJob.setEnabled(false);
			this.refreshJobTree.setEnabled(false);
		}

		private void setJobSummary(final JobSummary jobSummary) {
			if (jobSummary == null)
				this.setNoJob();
			else {
				if (this.treeScroller != null)
					this.remove(this.treeScroller);

				// Get job ID.
				this.jobId = jobSummary.getJobId();

				// Update viewable fields.
				this.jobIdField.setText(jobId);
				this.threadSpinner.setEnabled(true);
				this.contactEmail.setEnabled(true);
				this.updateEmailButton.setEnabled(true);
				this.footerPanel.setVisible(true);
				this.refreshJobTree.setEnabled(true);

				// Create a JTree to hold job details.
				this.treeModel = new JobPlanTreeModel(this.host, this.port,
						this.jobId, this);
				this.tree = new JTree(this.treeModel);
				this.tree.setOpaque(true);
				this.tree.setBackground(Color.WHITE);
				this.tree.setEditable(false); // Make it read-only.
				this.tree.setRootVisible(true); // Always show the root node.
				this.tree.setShowsRootHandles(true); // Allow root expansion.
				this.tree.setCellRenderer(new JobPlanTreeCellRenderer());

				// Add context menu to the job plan tree.
				this.tree.addMouseListener(new MouseListener() {
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
								// Work out what was clicked on or
								// multiply selected.
								Object selectedNode = treePath
										.getLastPathComponent();
								if (selectedNode instanceof WrapperTreeNode)
									selectedNode = ((WrapperTreeNode) selectedNode)
											.getRootJobPlanSection();
								final TreePath[] selectedPaths;
								if (tree.getSelectionCount() == 0
										&& (selectedNode instanceof JobPlanSection || selectedNode instanceof JobPlanAction))
									selectedPaths = new TreePath[] { treePath };
								else
									selectedPaths = tree.getSelectionPaths();

								// Show menu.
								final JPopupMenu contextMenu = this
										.getContextMenu(Arrays
												.asList(selectedPaths));
								if (contextMenu != null
										&& contextMenu.getComponentCount() > 0) {
									contextMenu.show(JobPlanPanel.this.tree, e
											.getX(), e.getY());
									e.consume();
								}
							}
						}
					}

					private JPopupMenu getContextMenu(
							final Collection selectedPaths) {
						// Convert paths to identifiers.
						final Set identifiers = new HashSet();
						final List selectedNodes = new ArrayList();
						for (final Iterator i = selectedPaths.iterator(); i.hasNext(); ) 
							selectedNodes.add(((TreePath)i.next()).getLastPathComponent());
						for (int i = 0; i < selectedNodes.size(); i++) {
							final Object node = selectedNodes.get(i);
							if (node instanceof JobPlanAction) 
								identifiers.add(new Integer(((JobPlanAction)node).getUniqueIdentifier()));
							else if (node instanceof JobPlanSection) {
								selectedNodes.addAll(((JobPlanSection)node).getAllSubSections());
								selectedNodes.addAll(((JobPlanSection)node).getAllActions());
							}
						}

						// Did we produce anything?
						if (identifiers.size() < 1)
							return null;

						// Build menu.
						final JPopupMenu contextMenu = new JPopupMenu();

						// Queue row.
						final JMenuItem queue = new JMenuItem(Resources
								.get("queueSelectionTitle"));
						queue.setMnemonic(Resources.get(
								"queueSelectionMnemonic").charAt(0));
						queue.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent evt) {
								// Confirm.
								new LongProcess() {
									public void run() throws Exception {
										// Queue the job.
										Client.queue(host, port, jobId,
												identifiers);
										// Update the list.
										JobPlanPanel.this.refreshJobTree
												.doClick();
									}
								}.start();
							}
						});
						contextMenu.add(queue);

						// Unqueue row.
						final JMenuItem unqueue = new JMenuItem(Resources
								.get("unqueueSelectionTitle"));
						unqueue.setMnemonic(Resources.get(
								"unqueueSelectionMnemonic").charAt(0));
						unqueue.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent evt) {
								// Confirm.
								new LongProcess() {
									public void run() throws Exception {
										// Unqueue the job.
										Client.unqueue(host, port, jobId,
												identifiers);
										// Update the list.
										JobPlanPanel.this.refreshJobTree
												.doClick();
									}
								}.start();
							}
						});
						contextMenu.add(unqueue);

						return contextMenu;
					}
				});

				// Listener on tree to update footer panel fields.
				this.tree.addTreeSelectionListener(new TreeSelectionListener() {

					public void valueChanged(TreeSelectionEvent e) {
						// Default values.
						Date started = null;
						Date ended = null;
						JobStatus status = JobStatus.UNKNOWN;
						String messages = null;
						long elapsed = 0;

						// Check a path was actually selected.
						final TreePath path = e.getPath();
						if (path != null) {
							Object selectedNode = e.getPath()
									.getLastPathComponent();

							// Get info.
							if (selectedNode instanceof WrapperTreeNode)
								selectedNode = ((WrapperTreeNode) selectedNode)
										.getRootJobPlanSection();
							if (selectedNode instanceof JobPlanSection) {
								final JobPlanSection section = (JobPlanSection) selectedNode;
								status = section.getStatus();
								started = section.getStarted();
								ended = section.getEnded();
								messages = section.getMessages();
							} else if (selectedNode instanceof JobPlanAction) {
								final JobPlanAction action = (JobPlanAction) selectedNode;
								status = action.getStatus();
								started = action.getStarted();
								ended = action.getEnded();
								messages = action.getMessages();
							}

							// Elapsed time calculation.
							if (started != null)
								if (ended != null)
									elapsed = ended.getTime()
											- started.getTime();
								else
									elapsed = new Date().getTime()
											- started.getTime();
						}

						// Elapsed time to string.
						long seconds = elapsed % 60;
						elapsed /= 60;
						long minutes = elapsed % 60;
						elapsed /= 60;
						long hours = elapsed % 24;
						elapsed /= 24;
						long days = elapsed;

						// Update dialog.
						JobPlanPanel.this.started.setValue(started);
						JobPlanPanel.this.finished.setValue(ended);
						JobPlanPanel.this.elapsed.setText(Resources.get(
								"timeElapsedPattern",
								new String[] { "" + days, "" + hours,
										"" + minutes, "" + seconds }));
						JobPlanPanel.this.status.setText(status.toString());
						JobPlanPanel.this.messages.setText(messages);

						// Redraw.
						JobPlanPanel.this.validate();
					}
				});

				this.treeScroller = new JScrollPane(this.tree);
				this.add(this.treeScroller, BorderLayout.CENTER);
			}

			// Redraw.
			this.validate();
		}
	}

	/**
	 * Represents a job plan as a tree model.
	 */
	public static class JobPlanTreeModel extends DefaultTreeModel {
		private static final long serialVersionUID = 1L;

		private static final TreeNode LOADING_TREE = new DefaultMutableTreeNode(
				Resources.get("loadingTree"));

		private JobPlan jobPlan;

		private final String host;

		private final String port;

		private final String jobId;

		private final JobPlanPanel planPanel;

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
		 * @param planPanel
		 *            the panel we are appearing in.
		 */
		public JobPlanTreeModel(final String host, final String port,
				final String jobId, final JobPlanPanel planPanel) {
			super(JobPlanTreeModel.LOADING_TREE, true);
			this.host = host;
			this.port = port;
			this.jobId = jobId;
			this.planPanel = planPanel;

			// Update the views showing the tree.
			this.reload();

			new LongProcess() {
				public void run() throws Exception {
					// Get job details.
					JobPlanTreeModel.this.loadModel();
					// Add elements of the job to tree.
					// Use a wrapper to allow updates to model without having to
					// recalculate tree.
					JobPlanTreeModel.this.setRoot(new WrapperTreeNode(
							JobPlanTreeModel.this));
					// Update the views showing the tree.
					JobPlanTreeModel.this.reload();
				}
			}.start();
		}

		private void loadModel() throws Exception {
			// Get job details.
			this.jobPlan = Client.getJobPlan(this.host, this.port, this.jobId);
			// Update GUI bits from the updated plan.
			this.planPanel.threadSpinnerModel.setValue(new Integer(this.jobPlan
					.getThreadCount()));
			this.planPanel.threadSpinnerModel.setMaximum(new Integer(
					this.jobPlan.getMaxThreadCount()));
			this.planPanel.jdbcUrl.setText(this.jobPlan.getJDBCURL());
			this.planPanel.jdbcUser.setText(this.jobPlan.getJDBCUsername());
			this.planPanel.contactEmail.setText(this.jobPlan
					.getContactEmailAddress());
			this.planPanel.startJob.setEnabled(!this.jobPlan.getStatus()
					.equals(JobStatus.RUNNING));
			this.planPanel.stopJob.setEnabled(this.jobPlan.getStatus().equals(
					JobStatus.RUNNING));
		}

		/**
		 * Update the model, but do not rebuild it. Will require a repaint of
		 * the tree to show any effect.
		 */
		public void update() {
			new LongProcess() {
				public void run() throws Exception {
					// Get job details.
					JobPlanTreeModel.this.loadModel();
				}
			}.start();
		}

		/**
		 * A virtual tree node which wraps a changing root node supplied by a
		 * changing tree model and makes it look like nothing has changed.
		 */
		public static class WrapperTreeNode implements TreeNode {

			private final JobPlanTreeModel model;

			/**
			 * A virtual node which wraps a changing model.
			 * 
			 * @param model
			 *            the model.
			 */
			public WrapperTreeNode(final JobPlanTreeModel model) {
				this.model = model;
			}

			public Enumeration children() {
				return this.getRootJobPlanSection().children();
			}

			public boolean getAllowsChildren() {
				return this.getRootJobPlanSection().getAllowsChildren();
			}

			public TreeNode getChildAt(int childIndex) {
				return this.getRootJobPlanSection().getChildAt(childIndex);
			}

			public int getChildCount() {
				return this.getRootJobPlanSection().getChildCount();
			}

			public int getIndex(TreeNode node) {
				return this.getRootJobPlanSection().getIndex(node);
			}

			public TreeNode getParent() {
				return this.getRootJobPlanSection().getParent();
			}

			public boolean isLeaf() {
				return this.getRootJobPlanSection().isLeaf();
			}

			/**
			 * Get the root node that this is wrapping.
			 * 
			 * @return the root node.
			 */
			public JobPlanSection getRootJobPlanSection() {
				return ((JobPlanSection) this.model.jobPlan.getRoot());
			}
		}
	}
}
