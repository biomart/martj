/*
 * NewTableProviderDialog.java
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import org.biomart.builder.controller.SchemaTools;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Construct a new table provider based on user input.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 25th April 2006
 * @since 0.1
 */
public class NewTableProviderDialog extends JDialog {
    /**
     * Our parent schema.
     */
    private TableProviderTabSet tableProviderTabSet;
    
    /**
     * The provider we created.
     */
    private TableProvider tableProvider;
    
    /**
     * The dialog fields.
     */
    private JComboBox type;
    private JTextField name;
    private JTextField driverClass;
    private JTextField driverClassLocation;
    private JButton driverClassLocationButton;
    private JFileChooser jarFileChooser;
    private JTextField jdbcURL;
    private JTextField username;
    private JPasswordField password;
    private JButton test;
    private JButton cancel;
    private JButton add;
    
    /**
     * Creates a new instance of NewTableProviderDialog.
     */
    public NewTableProviderDialog(final TableProviderTabSet tableProviderTabSet) {
        super(tableProviderTabSet.getWindowTabSet().getSchemaTabSet().getMartBuilder(),
                BuilderBundle.getString("newTblProvDialogTitle"),
                true);
        this.tableProviderTabSet = tableProviderTabSet;
        
        // create dialog panel
        GridBagLayout gridBag = new GridBagLayout();
        final JPanel content = new JPanel(gridBag);
        this.setContentPane(content);
        
        // create label constraints
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(2,2,2,2);
        // create field constraints
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.fill = GridBagConstraints.NONE;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.insets = new Insets(2,2,2,2);
        // create last row label constraints
        GridBagConstraints labelLastRowConstraints = (GridBagConstraints)labelConstraints.clone();
        labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
        // create last row field constraints
        GridBagConstraints fieldLastRowConstraints = (GridBagConstraints)fieldConstraints.clone();
        fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
        
        // create fields in dialog
        this.type = new JComboBox(new String[]{
            BuilderBundle.getString("jdbcDMDTableProvider"),
            BuilderBundle.getString("jdbcKeyGuessingTableProvider")
        });
        this.name = new JTextField(20);
        this.driverClass = new JTextField(50);
        this.driverClassLocation = new JTextField(30);
        this.driverClassLocationButton = new JButton(BuilderBundle.getString("browseButton"));
        this.jdbcURL = new JTextField(50);
        this.username = new JTextField(20);
        this.password = new JPasswordField(20);
        
        // create buttons in dialog
        this.test = new JButton(BuilderBundle.getString("testButton"));
        this.cancel = new JButton(BuilderBundle.getString("cancelButton"));
        this.add = new JButton(BuilderBundle.getString("addButton"));
        
        // create the file chooser
        this.jarFileChooser = new JFileChooser();
        this.jarFileChooser.setFileFilter(new FileFilter(){
            /**
             * {@inheritDoc}
             * <p>Accepts only files ending in ".jar".</p>
             */
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().endsWith(".jar"));
            }
            
            /**
             * {@inheritDoc}
             */
            public String getDescription() {
                return BuilderBundle.getString("JARFileFilterDescription");
            }
        });
        
        // add all the buttons and fields with their labels to the dialog
        JLabel label = new JLabel(BuilderBundle.getString("typeLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        gridBag.setConstraints(this.type, fieldConstraints);
        content.add(this.type);
        
        label = new JLabel(BuilderBundle.getString("nameLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        gridBag.setConstraints(this.name, fieldConstraints);
        content.add(this.name);
        
        label = new JLabel(BuilderBundle.getString("driverClassLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        gridBag.setConstraints(this.driverClass, fieldConstraints);
        content.add(this.driverClass);
        
        label = new JLabel(BuilderBundle.getString("driverClassLocationLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        JPanel driverLocationPanel = new JPanel();
        driverLocationPanel.add(this.driverClassLocation);
        driverLocationPanel.add(this.driverClassLocationButton);
        gridBag.setConstraints(driverLocationPanel, fieldConstraints);
        content.add(driverLocationPanel);
        
        label = new JLabel(BuilderBundle.getString("jdbcURLLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        gridBag.setConstraints(this.jdbcURL, fieldConstraints);
        content.add(this.jdbcURL);
        
        label = new JLabel(BuilderBundle.getString("usernameLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        gridBag.setConstraints(this.username, fieldConstraints);
        content.add(this.username);
        
        label = new JLabel(BuilderBundle.getString("passwordLabel"));
        gridBag.setConstraints(label, labelConstraints);
        content.add(label);
        gridBag.setConstraints(this.password, fieldConstraints);
        content.add(this.password);
        
        label = new JLabel();
        gridBag.setConstraints(label, labelLastRowConstraints);
        content.add(label);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(this.test);
        buttonPanel.add(this.cancel);
        buttonPanel.add(this.add);
        gridBag.setConstraints(buttonPanel, fieldLastRowConstraints);
        content.add(buttonPanel);
        
        // intercept the cancel button
        this.cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableProvider = null;
                hide();
            }
        });
        
        // intercept the test button
        this.test.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TableProvider tableProvider = createTableProvider();
                if (tableProvider != null) {
                    boolean passedTest = false;
                    try {
                        passedTest = SchemaTools.testTableProvider(tableProvider);
                    } catch (Throwable t) {
                        passedTest = false;
                        tableProviderTabSet.getWindowTabSet().getSchemaTabSet().getMartBuilder().showStackTrace(t);
                    }
                    // Tell the user what happened.
                    if (passedTest) {
                        JOptionPane.showMessageDialog(
                                content,
                                BuilderBundle.getString("tblProvTestPassed"),
                                BuilderBundle.getString("testTitle"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(
                                content,
                                BuilderBundle.getString("tblProvTestFailed"),
                                BuilderBundle.getString("testTitle"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        // intercept the add button
        this.add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableProvider = createTableProvider();
                if (tableProvider != null) hide();
            }
        });
        // make it the default button.
        this.getRootPane().setDefaultButton(add);
        
        // attach the file chooser to the button
        this.driverClassLocationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (jarFileChooser.showOpenDialog(content) == JFileChooser.APPROVE_OPTION) {
                    File file = jarFileChooser.getSelectedFile();
                    if (file != null) {
                        driverClassLocation.setText(file.toString());
                    }
                }
            }
        });
        
        // reset the fields
        this.resetFields();
        
        // set size of window
        this.pack();
    }
    
    /**
     * Resets the fields to their default values.
     */
    private void resetFields() {
        this.tableProvider = null;
        this.type.setSelectedIndex(0);
        this.name.setText(null);
        this.driverClass.setText(null);
        this.driverClassLocation.setText(null);
        this.jdbcURL.setText(null);
        this.username.setText(null);
        this.password.setText(null);
    }
    
    /**
     * Validates the fields.
     */
    private boolean validateFields() {
        List messages = new ArrayList();
        
        if (isEmpty(this.name.getText())) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("name")));
        }
        
        if (isEmpty(this.driverClass.getText())) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("driverClass")));
        } else {
            // Attempt to load the class.
            boolean classFound = false;
            try {
                Class.forName(this.driverClass.getText());
                classFound = true;
            } catch (Exception e) {
                classFound = false;
            }
            // If not found, check the class location.
            if (!classFound && isEmpty(this.driverClassLocation.getText())) {
                messages.add(BuilderBundle.getString("driverClassNotFound"));
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("driverClassLocation")));
            } else {
                
            }
        }
        
        if (isEmpty(this.jdbcURL.getText())) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("jdbcURL")));
        }
        
        if (isEmpty(this.username.getText())) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("username")));
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
    private TableProvider createTableProvider() {
        if (!this.validateFields()) return null;
        else {
            try {
                String type = (String)this.type.getSelectedItem();
                String driverClassName = this.driverClass.getText();
                String driverClassLocation = this.driverClassLocation.getText();
                String url = this.jdbcURL.getText();
                String username = this.username.getText();
                String password = new String(this.password.getPassword());
                String name = this.name.getText();
                if (type.equals(BuilderBundle.getString("jdbcDMDTableProvider")))
                    return SchemaTools.createJDBCTableProvider(
                            driverClassLocation == null ? null : new File(driverClassLocation),
                            driverClassName,
                            url,
                            username,
                            password,
                            name);
                else if (type.equals(BuilderBundle.getString("jdbcKeyGuessingTableProvider")))
                    return SchemaTools.createJDBCKeyGuessingTableProvider(
                            driverClassLocation == null ? null : new File(driverClassLocation),
                            driverClassName,
                            url,
                            username,
                            password,
                            name);
            } catch (Throwable t) {
                this.tableProviderTabSet.getWindowTabSet().getSchemaTabSet().getMartBuilder().showStackTrace(t);
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
     * Intercept the show() command and reset our fields before doing so.
     */
    public void show() {
        this.resetFields();
        super.show();
    }
    
    /**
     * Retrieve the provider we created.
     */
    public TableProvider getTableProvider() {
        return this.tableProvider;
    }
}
