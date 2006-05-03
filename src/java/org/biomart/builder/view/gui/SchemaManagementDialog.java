/*
 * SchemaManagementDialog.java
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
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.AncestorListener;
import org.biomart.builder.controller.JDBCSchema;
import org.biomart.builder.controller.MartUtils;
import org.biomart.builder.model.Schema;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Construct a new table provider based on user input.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 3rd May 2006
 * @since 0.1
 */
public class SchemaManagementDialog extends JDialog {
    /**
     * Our parent schema.
     */
    private SchemaTabSet schemaTabSet;
    
    /**
     * The provider we created.
     */
    private Schema schema;
    
    /**
     * The dialog fields.
     */
    private JComboBox type;
    private JTextField name;
    private ConnectionPanel connectionPanel;
    private JButton test;
    private JButton cancel;
    private JButton execute;
    
    /**
     * Creates a new instance of SchemaManagementDialog.
     */
    private SchemaManagementDialog(final SchemaTabSet schemaTabSet, String title, String executeButtonText, final Schema template) {
        super(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder(),
                title,
                true);
        this.schemaTabSet = schemaTabSet;
        
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
        this.name = new JTextField(20);
        this.type = new JComboBox(new String[]{
            BuilderBundle.getString("jdbcSchema")
        });
        final JPanel connectionPanelHolder = new JPanel();
        this.type.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (type.getSelectedItem().equals(BuilderBundle.getString("jdbcSchema"))) {
                    if (!(connectionPanel instanceof JDBCConnectionPanel)) {
                        connectionPanelHolder.removeAll();
                        connectionPanel = new JDBCConnectionPanel(schemaTabSet, template);
                        connectionPanelHolder.add(connectionPanel);
                        connectionPanelHolder.validate();
                    }
                }
            }
        });
        // fake default selection
        this.type.setSelectedItem(BuilderBundle.getString("jdbcSchema"));
        
        // create buttons in dialog
        this.test = new JButton(BuilderBundle.getString("testButton"));
        this.cancel = new JButton(BuilderBundle.getString("cancelButton"));
        this.execute = new JButton(executeButtonText);
        
        // execute all the buttons and fields with their labels to the dialog
        JLabel label = new JLabel(BuilderBundle.getString("nameLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        JPanel field = new JPanel();
        field.add(this.name);
        gridBag.setConstraints(field, fieldConstraints);
        content.add(field);
        
        label = new JLabel(BuilderBundle.getString("typeLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        field = new JPanel();
        field.add(this.type);
        // create panel chooser here and add
        field.add(connectionPanelHolder);
        gridBag.setConstraints(field, fieldConstraints);
        content.add(field);
        
        label = new JLabel();
        gridBag.setConstraints(label, labelLastRowConstraints);
        content.add(label);
        field = new JPanel();
        field.add(this.test);
        field.add(this.cancel);
        field.add(this.execute);
        gridBag.setConstraints(field, fieldLastRowConstraints);
        content.add(field);
        
        // intercept the cancel button
        this.cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                schema = null;
                hide();
            }
        });
        
        // intercept the test button
        this.test.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Schema testSchema = createSchema();
                if (testSchema != null) {
                    boolean passedTest = false;
                    try {
                        passedTest = MartUtils.testSchema(testSchema);
                    } catch (Throwable t) {
                        passedTest = false;
                        schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder().showStackTrace(t);
                    }
                    // Tell the user what happened.
                    if (passedTest) {
                        JOptionPane.showMessageDialog(
                                content,
                                BuilderBundle.getString("schemaTestPassed"),
                                BuilderBundle.getString("testTitle"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(
                                content,
                                BuilderBundle.getString("schemaTestFailed"),
                                BuilderBundle.getString("testTitle"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        // intercept the execute button
        this.execute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                schema = createSchema();
                if (schema != null) hide();
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
    private void resetFields(Schema template) {
        if (template != null) {
            this.type.setSelectedItem(BuilderBundle.getString("jdbcSchema"));
            this.type.setEnabled(false); // Gray out as we can't change this property.
            this.name.setText(template.getName());
            this.name.setEnabled(false); // Gray out as we can't change this property.
        } else {
            this.type.setSelectedIndex(0);
            this.name.setText(null);
        }
    }
    
    /**
     * Validates the fields.
     */
    private boolean validateFields() {
        List messages = new ArrayList();
        
        if (this.isEmpty(this.name.getText())) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("name")));
        }
        
        if (this.type.getSelectedIndex()==-1) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("type")));
        }
        
        if (!messages.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    messages.toArray(new String[0]),
                    BuilderBundle.getString("validationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        
        return messages.isEmpty() && this.connectionPanel.validateFields();
    }
    
    /**
     * Creates a table provider from the fields given.
     */
    private Schema createSchema() {
        if (!this.validateFields()) return null;
        else {
            try {
                String type = (String)this.type.getSelectedItem();
                if (type.equals(BuilderBundle.getString("jdbcSchema"))) {
                    return ((JDBCConnectionPanel)this.connectionPanel).createSchema(this.name.getText());
                }
            } catch (Throwable t) {
                this.schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder().showStackTrace(t);
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
    public static Schema createSchema(SchemaTabSet schemaTabSet) {
        SchemaManagementDialog dialog = new SchemaManagementDialog(
                schemaTabSet,
                BuilderBundle.getString("newSchemaDialogTitle"),
                BuilderBundle.getString("addButton"),
                null);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
        return dialog.schema;
    }
    
    /**
     * Static method which allows the user to modify an existing table provider.
     */
    public static boolean modifySchema(SchemaTabSet schemaTabSet, Schema schema) {
        SchemaManagementDialog dialog = new SchemaManagementDialog(
                schemaTabSet,
                BuilderBundle.getString("modifySchemaDialogTitle"),
                BuilderBundle.getString("modifyButton"),
                schema);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
        if (dialog.schema != null && dialog.schema instanceof JDBCSchema) {
            return (((JDBCConnectionPanel)dialog.connectionPanel).modifySchema(schema) != null);
        } else {
            return false;
        }
    }
}
