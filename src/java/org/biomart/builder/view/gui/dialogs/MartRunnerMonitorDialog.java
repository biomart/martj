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
import java.awt.Dimension;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.runner.exceptions.ProtocolException;
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
	 */
	public static void monitor(final String host, final String port) {
		// Open the dialog.
		new MartRunnerMonitorDialog(host, port).show();
	}

	private final DefaultListModel jobListModel;

	private final JScrollPane jobDescScroller;

	private MartRunnerMonitorDialog(final String host, final String port) {
		// Create the blank dialog, and give it an appropriate title.
		super(Resources.get("monitorDialogTitle", new String[] { host, port }));

		// Set up a timer for us.
		final Timer timer = new Timer();

		// Populate the job list and set up a timer to keep refreshing it.
		this.jobListModel = new DefaultListModel();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				try {
					jobListModel.removeAllElements();
					for (final Iterator i = Client.listJobs(host, port)
							.iterator(); i.hasNext();)
						jobListModel.addElement(i.next());
				} catch (final ProtocolException e) {
					StackTrace.showStackTrace(e);
				}
			}
		}, 0, 60 * 1000); // Updates once per minute.

		// Make the LHS list of jobs.
		final JList jobList = new JList(this.jobListModel);
		jobList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JPanel jobListPanel = new JPanel(new BorderLayout());
		jobListPanel.add(jobList, BorderLayout.CENTER);
		final JScrollPane jobListScroller = new JScrollPane(jobListPanel);		
		jobListScroller.setPreferredSize(new Dimension(150, 500));

		// Make the RHS scrollpane containing job descriptions.
		final JPanel emptyPanel = new JPanel();
		this.jobDescScroller = new JScrollPane(emptyPanel);	
		this.jobDescScroller.setPreferredSize(new Dimension(450, 500));

		// Add a listener to the list to update the pane on the right.
		jobList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent e) {
				final Object selection = jobList.getSelectedValue();
				if (selection != null) {
					// Update the panel on the right with the new job.
					final String jobId = (String) selection;
					MartRunnerMonitorDialog.this.jobDescScroller
							.setViewportView(new JobDescPanel(jobId, host, port));

					// Pack the window.
					MartRunnerMonitorDialog.this.pack();
				}
			}
		});

		// TODO Add context menu to each job in the job list.
		
		// Make the content pane.
		final JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, false, jobListScroller,
				this.jobDescScroller);
		splitPane.setOneTouchExpandable(true);
		
		// Set up our content pane.
		this.setContentPane(splitPane);

		// Pack the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	private class JobDescPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		/**
		 * Create a new job description panel. In the top half goes two panes -
		 * an email settings pane, and the job tree view. In the bottom half
		 * goes an explanation panel.
		 * 
		 * @param jobId
		 *            the job id.
		 * @param host
		 *            the host to talk to MartRunner at.
		 * @param port
		 *            the port to talk to MartRunner at.
		 */
		public JobDescPanel(final String jobId, final String host,
				final String port) {
			super(new BorderLayout());

			// TODO Get job details from server and set up timer to keep
			// on refreshing those details.
			this.add(new JLabel("Job: " + jobId), BorderLayout.CENTER);
		}
	}
}
