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
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.biomart.builder.view.gui.panels.tests.EmptyTableTestPanel;
import org.biomart.builder.view.gui.panels.tests.JobTestPanel;
import org.biomart.common.resources.Resources;
import org.biomart.runner.model.JobPlan;

/**
 * This dialog runs tests against a job.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public class MartRunnerTestDialog extends JFrame {
	private static final long serialVersionUID = 1;

	/**
	 * Opens a test dialog to allow tests against a particular job plan.
	 * 
	 * @param host
	 *            the host to run SQL against.
	 * @param port
	 *            the port to run SQL against.
	 * @param jobPlan
	 *            the job to run tests against.
	 */
	public static void showTests(final String host, final String port,
			final JobPlan jobPlan) {
		// Open the dialog.
		new MartRunnerTestDialog(host, port, jobPlan).setVisible(true);
	}

	private MartRunnerTestDialog(final String host, final String port,
			final JobPlan jobPlan) {
		super();
		this.setTitle(Resources.get("testJobDialogTitle"));
		this.getContentPane().setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Build the panels.
		final Map panels = new TreeMap();
		JobTestPanel panel = new EmptyTableTestPanel(host, port, jobPlan);
		panels.put(panel.getDisplayName(), panel);
		// TODO More panels.

		// Construct the dialog.
		final JList lhs = new JList(panels.keySet().toArray());
		final JPanel rhs = new JPanel(new BorderLayout());
		final JPanel rhsHolder = new JPanel();

		rhs.add(rhsHolder, BorderLayout.CENTER);
		this.getContentPane()
				.add(new JScrollPane(lhs), BorderLayout.LINE_START);
		this.getContentPane().add(rhs, BorderLayout.CENTER);

		// Listen and update panel.
		lhs.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				rhsHolder.removeAll();
				if (lhs.getSelectedValue() != null) {
					rhsHolder.add((JobTestPanel) panels.get((String) lhs
							.getSelectedValue()));
					MartRunnerTestDialog.this.pack();
				}
			}
		});

		// Select the first item in the list.
		lhs.setSelectedIndex(0);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}
}
