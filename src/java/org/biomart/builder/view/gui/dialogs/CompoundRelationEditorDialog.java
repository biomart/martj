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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.biomart.common.resources.Resources;

/**
 * A dialog which lists all the columns in a concat relation, and all the
 * columns in the table which are available to put in that relation. It can then
 * allow the user to move those columns around, thus e diting the relation. It
 * also allows the user to specify the separators to use during the
 * concatenation operation.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.1
 */
public class CompoundRelationEditorDialog extends JDialog {
	private static final long serialVersionUID = 1;

	/**
	 * Gets a compound arity.
	 * 
	 * @param compound
	 *            the initial arity to select.
	 * @return the arity the user defined.
	 */
	public static int getCompoundValue(
			final int startvalue) {
		final CompoundRelationEditorDialog dialog = new CompoundRelationEditorDialog(startvalue);
		dialog.setLocationRelativeTo(null);
		dialog.show();
		return dialog.getArity();
	}

	private SpinnerNumberModel arity;

	private CompoundRelationEditorDialog(final int startvalue) {
		// Create the base dialog.
		super();
		this.setTitle(Resources
				.get("compoundRelationDialogTitle"));
		this.setModal(true);

		// Create the layout manager for this panel.
		final GridBagLayout gridBag = new GridBagLayout();
		final JPanel content = new JPanel();
		content.setLayout(gridBag);
		this.setContentPane(content);

		// Create constraints for fields that are not in the last row.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are in the last row.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Set up the arity spinner field.
		this.arity = new SpinnerNumberModel(startvalue, 1, 10, 1); 
		final JSpinner spinner = new JSpinner(this.arity);
		
		// Set up the check box to turn it on and off.
		final JCheckBox checkbox = new JCheckBox();
		if (startvalue>1) checkbox.setSelected(true);

		// The close and execute buttons.
		final JButton close = new JButton(Resources.get("closeButton"));
		final JButton execute = new JButton(Resources.get("updateButton"));

		// Input fields.
		JPanel field = new JPanel();
		field.add(checkbox);
		field.add(new JLabel(Resources.get("compoundRelationNLabel")));
		field.add(spinner);
		field.add(new JLabel(Resources.get("compoundRelationSpinnerLabel")));
		gridBag.setConstraints(field, fieldConstraints);
		content.add(field);

		// Close/Execute buttons at the bottom.
		field = new JPanel();
		field.add(close);
		field.add(execute);
		gridBag.setConstraints(field, fieldLastRowConstraints);
		content.add(field);
		
		// Intercept the spinner.
    	spinner.addChangeListener(new ChangeListener() {
    	    public void stateChanged(ChangeEvent e) {
    	    	if (CompoundRelationEditorDialog.this.getArity()<=1)
    	    		checkbox.setSelected(false);
    	    	else
    	    		checkbox.setSelected(true);
    	    }
    	});
		// Intercept the checkbox.
		checkbox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (checkbox.isSelected() && CompoundRelationEditorDialog.this.getArity()==1) 
					arity.setValue(new Integer(2));
				else 
					arity.setValue(new Integer(1));
			}
		});

		// Intercept the close button, which closes the dialog
		// without taking any action.
		close.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				// Reset to default value.
				CompoundRelationEditorDialog.this.arity.setValue(new Integer(startvalue));
				CompoundRelationEditorDialog.this.hide();
			}
		});

		// Intercept the execute button, which validates the fields
		// then closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (CompoundRelationEditorDialog.this.validateFields()) 
					CompoundRelationEditorDialog.this.hide();
			}
		});

		// Make execute the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set size of window.
		this.pack();
	}

	private int getArity() {
		return this.arity.getNumber().intValue();
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
		final List messages = new ArrayList();

		// Must enter something in the arity box.
		if (this.arity.getNumber()==null)
			messages
					.add(Resources.get("fieldIsEmpty", Resources.get("arity")));
		
		// Any messages to display? Show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
	}
}
