/*
 * SchemaManagerDialog.java
 *
 * Created on 25 April 2006, 16:09
 */

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

package org.biomart.builder.view.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Construct a new table provider based on user input.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 12th May 2006
 * @since 0.1
 */
public class PartitionColumnDialog extends JDialog {
    private DataSetTabSet datasetTabSet;
    
    /**
     * The partition type we created.
     */
    private PartitionedColumnType partitionType;
    
    /**
     * The dialog fields.
     */
    private JComboBox type;
    private JTextField singleValue;
    private JTextArea multiValue;
    private JButton cancel;
    private JButton execute;
    private JCheckBox nullable;
    
    /**
     * Creates a new instance of SchemaManagerDialog.
     */
    private PartitionColumnDialog(final DataSetTabSet datasetTabSet, String executeButtonText, final PartitionedColumnType template) {
        super(datasetTabSet.getMartTabSet().getMartBuilder(),
                BuilderBundle.getString("partitionColumnDialogTitle"),
                true);
        this.datasetTabSet = datasetTabSet;
        
        // create dialog panel
        GridBagLayout gridBag = new GridBagLayout();
        final JPanel content = new JPanel(gridBag);
        this.setContentPane(content);
        
        // create label constraints
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(0,2,0,0);
        // create field constraints
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.fill = GridBagConstraints.NONE;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.insets = new Insets(0,1,0,2);
        // create last row label constraints
        GridBagConstraints labelLastRowConstraints = (GridBagConstraints)labelConstraints.clone();
        labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
        // create last row field constraints
        GridBagConstraints fieldLastRowConstraints = (GridBagConstraints)fieldConstraints.clone();
        fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
        
        // create fields in dialog
        final JLabel valueLabel = new JLabel(BuilderBundle.getString("valuesLabel"));
        this.singleValue = new JTextField(30);
        this.multiValue = new JTextArea(5,30);
        this.type = new JComboBox(new String[]{
            BuilderBundle.getString("singlePartitionOption"),
            BuilderBundle.getString("collectionPartitionOption"),
            BuilderBundle.getString("uniquePartitionOption")
        });
        this.nullable = new JCheckBox();
        final JDialog us = this;
        this.type.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String)type.getSelectedItem();
                if (selectedItem.equals(BuilderBundle.getString("singlePartitionOption"))) {
                    valueLabel.setVisible(true);
                    singleValue.setVisible(true);
                    multiValue.setVisible(false);
                    nullable.setText(BuilderBundle.getString("useNullLabel"));
                    nullable.setVisible(true);
                } else if (selectedItem.equals(BuilderBundle.getString("collectionPartitionOption"))) {
                    valueLabel.setVisible(true);
                    singleValue.setVisible(false);
                    multiValue.setVisible(true);
                    nullable.setText(BuilderBundle.getString("includeNullLabel"));
                    nullable.setVisible(true);
                } else {
                    valueLabel.setVisible(false);
                    singleValue.setVisible(false);
                    multiValue.setVisible(false);
                    nullable.setVisible(false);
                }
                us.pack();
            }
        });
        this.nullable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (nullable.isSelected()) {
                    singleValue.setText(null);
                    singleValue.setEnabled(false);
                } else {
                    singleValue.setEnabled(true);
                }
            }
        });
        
        // create buttons in dialog
        this.cancel = new JButton(BuilderBundle.getString("cancelButton"));
        this.execute = new JButton(executeButtonText);
        
        // execute all the buttons and fields with their labels to the dialog
        JLabel label = new JLabel(BuilderBundle.getString("partitionTypeLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        JPanel field = new JPanel();
        field.add(this.type);
        gridBag.setConstraints(field, fieldConstraints);
        content.add(field);
        
        gridBag.setConstraints(valueLabel, labelConstraints);
        content.add(valueLabel);
        field = new JPanel();
        field.add(this.singleValue);
        field.add(this.multiValue);
        gridBag.setConstraints(field, fieldConstraints);
        content.add(field);
        
        label = new JLabel();
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        field = new JPanel();
        field.add(this.nullable);
        gridBag.setConstraints(field, fieldConstraints);
        content.add(field);
        
        label = new JLabel();
        gridBag.setConstraints(label, labelLastRowConstraints);
        content.add(label);
        field = new JPanel();
        field.add(this.cancel);
        field.add(this.execute);
        gridBag.setConstraints(field, fieldLastRowConstraints);
        content.add(field);
        
        // intercept the cancel button
        this.cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                partitionType = null;
                hide();
            }
        });
        
        // intercept the execute button
        this.execute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                partitionType = createPartitionType();
                if (partitionType != null) hide();
            }
        });
        // make it the default button.
        this.getRootPane().setDefaultButton(execute);
        
        // reset the fields
        this.resetFields(template);
        
        // set size of window
        this.pack();
    }
    
    /**
     * Resets the fields to their default values.
     */
    private void resetFields(PartitionedColumnType template) {
        // make default selection and values
        if (template instanceof SingleValue) {
            SingleValue sv = (SingleValue)template;
            this.type.setSelectedItem(BuilderBundle.getString("singlePartitionOption"));
            this.singleValue.setText(sv.getValue());
            if (sv.getIncludeNull()) this.nullable.doClick();
        } else if (template instanceof ValueCollection) {
            ValueCollection vc = (ValueCollection)template;
            if (vc.getIncludeNull()) this.nullable.doClick();
            this.type.setSelectedItem(BuilderBundle.getString("collectionPartitionOption"));
            StringBuffer sb = new StringBuffer();
            for (Iterator i = vc.getValues().iterator(); i.hasNext(); ) {
                sb.append((String)i.next());
                if (i.hasNext()) sb.append(System.getProperty("line.separator"));
            }
            this.multiValue.setText(sb.toString());
        } else {
            this.type.setSelectedItem(BuilderBundle.getString("uniquePartitionOption"));
        }
    }
    
    /**
     * Validates the fields.
     */
    private boolean validateFields() {
        List messages = new ArrayList();
        
        if (this.type.getSelectedIndex()==-1) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("type")));
        }
        
        String selectedItem = (String)this.type.getSelectedItem();
        
        if (selectedItem.equals(BuilderBundle.getString("singlePartitionOption"))) {
            if (this.isEmpty(this.singleValue.getText()) && !this.nullable.isSelected())
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("value")));
        } else if (selectedItem.equals(BuilderBundle.getString("collectionPartitionOption"))) {
            if (this.isEmpty(this.multiValue.getText()) && !this.nullable.isSelected())
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("value")));
        }
        
        if (!messages.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    messages.toArray(new String[0]),
                    BuilderBundle.getString("validationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        
        return messages.isEmpty();
    }
    
    /**
     * Creates a table provider from the fields given.
     */
    private PartitionedColumnType createPartitionType() {
        if (!this.validateFields()) return null;
        else {
            try {
                String type = (String)this.type.getSelectedItem();
                if (type.equals(BuilderBundle.getString("singlePartitionOption"))) {
                    return new SingleValue(this.singleValue.getText().trim(), this.nullable.isSelected());
                } else if (type.equals(BuilderBundle.getString("collectionPartitionOption"))) {
                    String[] values = this.multiValue.getText().trim().split(System.getProperty("line.separator"));
                    return new ValueCollection(Arrays.asList(values), this.nullable.isSelected());
                } else if (type.equals(BuilderBundle.getString("uniquePartitionOption"))) {
                    return new UniqueValues();
                }
            } catch (Throwable t) {
                this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
            }
            return null;
        }
    }
    
    /**
     * Tests a string for non-nullness.
     */
    private boolean isEmpty(String string) {
        return (string == null || string.trim().length() == 0);
    }
    
    /**
     * Static method which allows the user to create a new table provider.
     */
    public static PartitionedColumnType createPartitionedColumnType(DataSetTabSet datasetTabSet) {
        PartitionColumnDialog dialog = new PartitionColumnDialog(
                datasetTabSet,
                BuilderBundle.getString("createPartitionButton"),
                null);
        dialog.setLocationRelativeTo(datasetTabSet.getMartTabSet().getMartBuilder());
        dialog.show();
        return dialog.partitionType;
    }
    
    /**
     * Static method which allows the user to modify an existing table provider.
     */
    public static PartitionedColumnType updatePartitionedColumnType(DataSetTabSet datasetTabSet, PartitionedColumnType template) {
        PartitionColumnDialog dialog = new PartitionColumnDialog(
                datasetTabSet,
                BuilderBundle.getString("updatePartitionButton"),
                template);
        dialog.setLocationRelativeTo(datasetTabSet.getMartTabSet().getMartBuilder());
        dialog.show();
        return dialog.partitionType;
    }
}
