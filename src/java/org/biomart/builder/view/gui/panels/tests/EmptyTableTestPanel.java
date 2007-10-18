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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.biomart.common.resources.Resources;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.tests.EmptyTableTest;
import org.biomart.runner.model.tests.JobTest;

/**
 * This test panel contains the options needed to run a {@link EmptyTableTest},
 * which checks for empty or all-null tables.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public class EmptyTableTestPanel extends JobTestPanel {
	private static final long serialVersionUID = 1;

	private final EmptyTableTest test;

	private ButtonGroup emptyTableGroup;

	private ButtonGroup noNonKeyGroup;

	private ButtonGroup nullableGroup;

	private static final Map BUTTON_MAP = new HashMap();

	static {
		EmptyTableTestPanel.BUTTON_MAP.put(Resources.get("emptyTableDrop"),
				new Integer(EmptyTableTest.DROP));
		EmptyTableTestPanel.BUTTON_MAP.put(Resources.get("emptyTableSkip"),
				new Integer(EmptyTableTest.SKIP));
		EmptyTableTestPanel.BUTTON_MAP.put(Resources.get("emptyTableReport"),
				new Integer(EmptyTableTest.REPORT));
	}

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
	public EmptyTableTestPanel(final String host, final String port,
			final JobPlan plan) {
		super();
		this.test = new EmptyTableTest(host, port, plan);
	}

	protected void addFields() {
		// Empty table test.
		this.add(new JLabel(Resources.get("emptyTableNoRowsTest")),
				this.labelConstraints);
		JPanel field = new JPanel();
		this.emptyTableGroup = new ButtonGroup();
		// report
		JRadioButton radio = new JRadioButton(
				Resources.get("emptyTableReport"), true);
		this.emptyTableGroup.add(radio);
		field.add(radio);
		// drop
		radio = new JRadioButton(Resources.get("emptyTableDrop"));
		this.emptyTableGroup.add(radio);
		field.add(radio);
		// skip
		radio = new JRadioButton(Resources.get("emptyTableSkip"));
		this.emptyTableGroup.add(radio);
		field.add(radio);
		this.add(field, this.fieldConstraints);

		// Non-key cols test.
		this.add(new JLabel(Resources.get("emptyTableNoNonKeyColsTest")),
				this.labelConstraints);
		field = new JPanel();
		this.noNonKeyGroup = new ButtonGroup();
		// report
		radio = new JRadioButton(Resources.get("emptyTableReport"), true);
		this.noNonKeyGroup.add(radio);
		field.add(radio);
		// drop
		radio = new JRadioButton(Resources.get("emptyTableDrop"));
		this.noNonKeyGroup.add(radio);
		field.add(radio);
		// skip
		radio = new JRadioButton(Resources.get("emptyTableSkip"));
		this.noNonKeyGroup.add(radio);
		field.add(radio);
		this.add(field, this.fieldConstraints);

		// Nullable cols test.
		this.add(new JLabel(Resources.get("emptyTableNullableColsTest")),
				this.labelConstraints);
		field = new JPanel();
		this.nullableGroup = new ButtonGroup();
		// report
		radio = new JRadioButton(Resources.get("emptyTableReport"), true);
		this.nullableGroup.add(radio);
		field.add(radio);
		// drop
		radio = new JRadioButton(Resources.get("emptyTableDrop"));
		this.nullableGroup.add(radio);
		field.add(radio);
		// skip
		radio = new JRadioButton(Resources.get("emptyTableSkip"));
		this.nullableGroup.add(radio);
		field.add(radio);
		this.add(field, this.fieldConstraints);
	}

	protected String[] doValidateOptions() {
		// We have no options that need to be validated.
		return new String[0];
	}

	public String getDisplayName() {
		return Resources.get("emptyTableTestName");
	}

	protected JobTest getJobTest() {
		return this.test;
	}

	protected void setJobTestOptions() {
		this.test.setNoRowsTest(this.getSelection(this.emptyTableGroup));
		this.test.setAllNullableColsEmptyTest(this
				.getSelection(this.nullableGroup));
		this.test.setNoNonKeyColsTest(this.getSelection(this.noNonKeyGroup));
	}

	// This method returns the selected radio button in a button group
	private int getSelection(final ButtonGroup group) {
		for (final Enumeration e = group.getElements(); e.hasMoreElements();) {
			final JRadioButton b = (JRadioButton) e.nextElement();
			if (b.getModel() == group.getSelection())
				return ((Integer) EmptyTableTestPanel.BUTTON_MAP.get(b
						.getText())).intValue();
		}
		return EmptyTableTest.SKIP;
	}
}
