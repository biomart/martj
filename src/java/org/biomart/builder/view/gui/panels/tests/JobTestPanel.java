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

package org.biomart.builder.view.gui.panels.tests;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.common.view.gui.dialogs.ViewTextDialog;
import org.biomart.runner.exceptions.TestException;
import org.biomart.runner.model.tests.JobTest;

/**
 * Test panels represent all the different options that can be used to run a
 * particular test. They completely contain each kind of test and run those
 * tests, with the abstract parent class providing monitor functions. Static
 * methods allow for the registration and discovery of various panel types. This
 * should be done using a static initialiser block which calls 
 * {@link #addPanel(String, JobTestPanel)} with an instance of the panel.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public abstract class JobTestPanel extends JPanel {

	private static final Map panels = new TreeMap();

	private boolean started = false;
	
	private final JButton start;
	
	private final JButton stop;
	
	private final JButton report;
	
	private final JProgressBar progress;

	/**
	 * Use this in {@link #addFields()}.
	 */
	protected final GridBagConstraints labelConstraints;

	/**
	 * Use this in {@link #addFields()}.
	 */
	protected final GridBagConstraints fieldConstraints;

	/**
	 * Use this in {@link #addFields()}.
	 */
	protected final GridBagConstraints labelLastRowConstraints;

	/**
	 * Use this in {@link #addFields()}.
	 */
	protected final GridBagConstraints fieldLastRowConstraints;

	/**
	 * Get the available panel names.
	 * 
	 * @return the names.
	 */
	public static Collection getPanelNames() {
		return JobTestPanel.panels.keySet();
	}

	/**
	 * Obtain the named panel from the map of available ones.
	 * 
	 * @param name
	 *            the name to retrieve.
	 * @return an instance of the panel.
	 */
	public static JobTestPanel getPanel(final String name) {
		if (!JobTestPanel.panels.containsKey(name))
			return null;
		return (JobTestPanel) JobTestPanel.panels.get(name);
	}

	/**
	 * Add a new panel to the list of available ones.
	 * 
	 * @param name
	 *            the name of this new panel.
	 * @param panel
	 *            the panel object.
	 */
	protected static void addPanel(final String name, final JobTestPanel panel) {
		JobTestPanel.panels.put(name, panel);
	}
	
	/**
	 * Creates a new panel.
	 * 
	 */
	protected JobTestPanel() {
		super();

		// Create the layout manager for this panel.
		this.setLayout(new GridBagLayout());

		// Create constraints for labels that are not in the last row.
		this.labelConstraints = new GridBagConstraints();
		this.labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		this.labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		this.labelConstraints.anchor = GridBagConstraints.LINE_END;
		this.labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		this.fieldConstraints = new GridBagConstraints();
		this.fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		this.fieldConstraints.fill = GridBagConstraints.NONE;
		this.fieldConstraints.anchor = GridBagConstraints.LINE_START;
		this.fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		this.labelLastRowConstraints = (GridBagConstraints) this.labelConstraints
				.clone();
		this.labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		this.fieldLastRowConstraints = (GridBagConstraints) this.fieldConstraints
				.clone();
		this.fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Construct our input fields.
		this.addFields();

		// Create start/stop/report button.
		this.start = new JButton(Resources.get("startTestButton"));
		this.stop = new JButton(Resources.get("stopTestButton"));
		this.report = new JButton(Resources.get("reportTestButton"));
		// Listeners.
		this.start.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				JobTestPanel.this.startTest();
			}
		});
		this.stop.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				JobTestPanel.this.stopTest();
			}
		});
		this.report.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ViewTextDialog.displayText(Resources.get("testReportTitle"), JobTestPanel.this.getJobTest().getReport());
			}
		});
		
		// Create progress bar.
		this.progress = new JProgressBar();
		this.progress.setMinimum(0);
		this.progress.setMaximum(100);
		this.progress.setValue(0);
		this.progress.setIndeterminate(false);
		
		// Add buttons and progress bar to GUI.
		final JPanel buttonPanel = new JPanel();
		buttonPanel.add(this.start);
		buttonPanel.add(this.stop);
		buttonPanel.add(this.progress);
		this.add(this.progress, this.labelLastRowConstraints);
		this.add(buttonPanel, this.fieldLastRowConstraints);

		// Disable stop, enable start, disable report.
		this.start.setEnabled(true);
		this.stop.setEnabled(false);
		this.report.setEnabled(false);

		// TODO Set report button background status color to not-yet-run.
	}

	/**
	 * Create and add input fields to the panel using global grid bag
	 * constraints.
	 */
	protected abstract void addFields();

	private boolean validateOptions() {
		final String[] messages = this.doValidateOptions();
		// If there any messages to show the user, show them.
		if (messages.length == 0)
			return true;
		JOptionPane.showMessageDialog(null, messages, Resources
				.get("validationTitle"), JOptionPane.INFORMATION_MESSAGE);
		return false;
	}

	/**
	 * Return a list of validation failure messages, if there were any. If not,
	 * return an empty list. Never return null.
	 * 
	 * @return the list of validation errors.
	 */
	protected abstract String[] doValidateOptions();
	
	/**
	 * Called when the test is about to be run and needs the options
	 * copying from user input fields to the test before execution.
	 */
	protected abstract void setJobTestOptions();

	/**
	 * Start the test this panel represents.
	 */
	public void startTest() {
		if (!this.started && this.validateOptions()) {
			this.started = true;
			try {
				this.setJobTestOptions();
				this.getJobTest().startTest();
			} catch (final TestException e) {
				StackTrace.showStackTrace(e);
				this.started = false;
				return;
			}
			// Disable start, enable stop, disable report.
			this.start.setEnabled(false);
			this.stop.setEnabled(true);
			this.report.setEnabled(false);
			// Reset and start progress bar.
			this.progress.setValue(0);
			this.progress.setIndeterminate(true);
			// TODO Set report button background status to running.
			// TODO Add progress bar update timer.
			// TODO When ends, update started, disable stop, enable start,
			// enable report, set progress bar to 100%, set report
			// button background status to OK/failed.
		}
	}

	/**
	 * Stop the test this panel represents.
	 */
	public void stopTest() {
		this.getJobTest().stopTest();
	}

	/**
	 * Check to see if the test is still running.
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isTestRunning() {
		return this.started;
	}

	/**
	 * Obtain the test object this panel will run.
	 * 
	 * @return the test object.
	 */
	protected abstract JobTest getJobTest();
}
