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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.runner.exceptions.TestException;
import org.biomart.runner.model.tests.JobTest;

/**
 * Test panels represent all the different options that can be used to run a
 * particular test. They completely contain each kind of test and run those
 * tests, with the abstract parent class providing monitor functions. Static
 * methods allow for the registration and discovery of various panel types.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public abstract class JobTestPanel extends JPanel {

	private static final Map panels = new TreeMap();

	private boolean started = false;

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
		try {
			return (JobTestPanel) ((Class) JobTestPanel.panels.get(name))
					.newInstance();
		} catch (final Exception e) {
			StackTrace.showStackTrace(e);
			return null;
		}
	}

	/**
	 * Add a new panel to the list of available ones.
	 * 
	 * @param name
	 *            the name of this new panel.
	 * @param panelClass
	 *            the class of the panel object.
	 */
	protected static void addPanel(final String name, final Class panelClass) {
		JobTestPanel.panels.put(name, panelClass);
	}

	/**
	 * Creates a new panel.
	 * 
	 */
	public JobTestPanel() {
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

		// TODO Add start/stop/report button and listeners.
		// TODO Disable stop, enable start, disable report.
	}

	/**
	 * Create and add input fields to the panel using global grid bag
	 * constraints.
	 */
	protected void addFields() {
		// TODO
	}

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
	 * Start the test this panel represents.
	 */
	public void startTest() {
		if (!this.started && this.validateOptions()) {
			this.started = true;
			try {
				this.getJobTest().startTest();
			} catch (final TestException e) {
				StackTrace.showStackTrace(e);
				this.started = false;
				return;
			}
			// TODO Disable start, enable stop, disable report.
			// TODO Run the progress bar.
			// TODO When ends, update started, disable stop, enable start,
			// enable report if ended successfully.
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
