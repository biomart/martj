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

import javax.swing.JLabel;

import org.biomart.common.resources.Resources;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.tests.EmptyColumnTest;
import org.biomart.runner.model.tests.JobTest;

/**
 * This test panel contains the options needed to run a {@link EmptyColumnTest},
 * which checks for empty or all-null columns.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public class EmptyColumnTestPanel extends JobTestPanel {
	private static final long serialVersionUID = 1;

	private final JobTest test;

	/**
	 * Construct a new test panel.
	 * 
	 * @param host
	 *            the host to run SQL against.
	 * @param port
	 *            the port to run SQL against.
	 * @param plan
	 *            the plan to test.
	 */
	public EmptyColumnTestPanel(final String host, final String port,
			final JobPlan plan) {
		super();
		this.test = new EmptyColumnTest(host, port, plan);
	}

	protected void addFields() {
		// Add 'we need no fields'.
		this.add(new JLabel(Resources.get("testNeedsNoFields")),
				this.fieldConstraints);
	}

	protected String[] doValidateOptions() {
		// We have no options.
		return new String[0];
	}

	public String getDisplayName() {
		return Resources.get("emptyColumnTestName");
	}

	protected JobTest getJobTest() {
		return this.test;
	}

	protected void setJobTestOptions() {
		// Nothing needs doing.
	}

}
