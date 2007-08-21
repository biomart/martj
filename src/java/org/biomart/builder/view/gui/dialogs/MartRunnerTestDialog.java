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

import javax.swing.JDialog;

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

	/**
	 * Opens a test dialog to allow tests against a particular job plan.
	 * 
	 * @param jobPlan
	 *            the job to run tests against.
	 */
	public static void showTests(final JobPlan jobPlan) {
		// Open the dialog.
		new MartRunnerTestDialog(jobPlan).setVisible(true);
	}

	private MartRunnerTestDialog(final JobPlan jobPlan) {
		super();
		this.setTitle(Resources.get("testJobDialogTitle"));
		this.setModal(false); // User can move about freely.
		// TODO
		// LHS panel lists valid job tests for this plan.
		// Selecting test in LHS updates panel on RHS.
		// RHS panel shows valid settings for selected test.
	}
}
