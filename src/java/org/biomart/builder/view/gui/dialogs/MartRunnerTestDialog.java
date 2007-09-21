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
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.biomart.builder.view.gui.panels.tests.JobTestPanel;
import org.biomart.common.resources.Resources;
import org.biomart.runner.model.JobPlan;

/**
 * This dialog runs tests against a job.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class MartRunnerTestDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private static final TreeMap panels = new TreeMap();

	static {
		// TODO Populate panels with panel names -> panels.
	}

	/**
	 * Opens a test dialog to allow tests against a particular job plan.
	 * 
	 * @param jobPlan
	 *            the job to run tests against.
	 */
	public static void showTests(final JobPlan jobPlan) {
		// Open the dialog.
		final MartRunnerTestDialog dialog = new MartRunnerTestDialog(jobPlan);
		dialog.setVisible(true);
		dialog.dispose();
	}

	private MartRunnerTestDialog(final JobPlan jobPlan) {
		super();
		this.setTitle(Resources.get("testJobDialogTitle"));
		this.setModal(false); // User can move about freely.
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		final JList lhs = new JList(MartRunnerTestDialog.panels.keySet()
				.toArray());
		final JPanel rhs = new JPanel(new BorderLayout());
		final JPanel rhsHolder = new JPanel();
		final JPanel closeHolder = new JPanel();
		final JButton close = new JButton(Resources.get("closeButton"));

		closeHolder.add(close);
		rhs.add(closeHolder, BorderLayout.PAGE_END);
		rhs.add(rhsHolder, BorderLayout.CENTER);
		this.add(new JScrollPane(lhs), BorderLayout.LINE_START);
		this.add(rhs, BorderLayout.CENTER);

		// Listen and update panel.
		lhs.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				rhsHolder.removeAll();
				if (lhs.getSelectedValue() != null) {
					rhsHolder.add((JobTestPanel) MartRunnerTestDialog.panels
							.get((String) lhs.getSelectedValue()));
					MartRunnerTestDialog.this.pack();
				}
			}
		});

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}
}
