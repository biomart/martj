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
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import org.biomart.builder.controller.JDBCSchema;
import org.biomart.builder.controller.MartUtils;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Construct a new table provider based on user input.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 3rd May 2006
 * @since 0.1
 */
public class JDBCConnectionPanel extends ConnectionPanel implements ActionListener {
    /**
     * Constants.
     */
    private static Map DRIVER_MAP = new HashMap();
    static {
        DRIVER_MAP.put("com.mysql.jdbc.Driver", new String[]{"3306","jdbc:mysql://<HOST>:<PORT>/<DATABASE>"});
        DRIVER_MAP.put("oracle.jdbc.driver.OracleDriver",new String[]{"1531","jdbc:oracle:thin:@<HOST>:<PORT>:<DATABASE>"});
        DRIVER_MAP.put("org.postgresql.Driver",new String[]{"5432","jdbc:postgres://<HOST>:<PORT>/<DATABASE>"});
    }
    
    /**
     * The current JDBC template;
     */
    private String currentJDBCURLTemplate;
    
    /**
     * Our parent schema.
     */
    private SchemaTabSet schemaTabSet;
    
    /**
     * The dialog fields.
     */
    private JComboBox copysettings;
    private JCheckBox keyguessing;
    private JComboBox driverClass;
    private JTextField driverClassLocation;
    private JButton driverClassLocationButton;
    private JFileChooser jarFileChooser;
    private JTextField jdbcURL;
    private JTextField host;
    private JFormattedTextField port;
    private JTextField database;
    private JTextField username;
    private JPasswordField password;
    
    /**
     * Creates a new instance of SchemaManagementDialog.
     */
    public JDBCConnectionPanel(final SchemaTabSet schemaTabSet, Schema template) {
        super();
        this.schemaTabSet = schemaTabSet;
        
        // create dialog panel
        GridBagLayout gridBag = new GridBagLayout();
        this.setLayout(gridBag);
        
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
        this.keyguessing = new JCheckBox();
        this.driverClass = new JComboBox((String[])JDBCConnectionPanel.DRIVER_MAP.keySet().toArray(new String[0]));
        this.driverClass.setEditable(true);
        this.driverClass.addActionListener(this);
        this.driverClassLocation = new JTextField(30);
        this.driverClassLocationButton = new JButton(BuilderBundle.getString("browseButton"));
        this.jdbcURL = new JTextField(40);
        this.host = new JTextField(10);
        this.port = new JFormattedTextField(new DecimalFormat("0"));
        this.port.setColumns(4);
        this.database = new JTextField(10);
        this.username = new JTextField(10);
        this.password = new JPasswordField(10);
        this.copysettings = new JComboBox();
        for (Iterator i = this.schemaTabSet.getDataSetTabSet().getMart().getSchemas().iterator(); i.hasNext(); ) {
            Schema s = (Schema)i.next();
            if (!(s instanceof SchemaGroup)) this.copysettings.addItem(s);
        }
        this.copysettings.addActionListener(this);
        
        // create JDBC URL constructor
        DocumentListener jdbcURLConstructor = new JDBCURLConstructor(this.host, this.port, this.database, this.jdbcURL);
        this.host.getDocument().addDocumentListener(jdbcURLConstructor);
        this.port.getDocument().addDocumentListener(jdbcURLConstructor);
        this.database.getDocument().addDocumentListener(jdbcURLConstructor);
        
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
        
        // execute all the buttons and fields with their labels to the dialog
        JLabel label = new JLabel(BuilderBundle.getString("copySettingsLabel"));
        gridBag.setConstraints(label, labelConstraints);
        JPanel field = new JPanel();
        field.add(label);
        field.add(this.copysettings);
        gridBag.setConstraints(field, fieldConstraints);
        this.add(field);
        
        label = new JLabel(BuilderBundle.getString("keyguessingLabel"));
        gridBag.setConstraints(label, labelConstraints);
        this.add(label);
        field = new JPanel();
        field.add(this.keyguessing);
        gridBag.setConstraints(field, fieldConstraints);
        this.add(field);
        
        label = new JLabel(BuilderBundle.getString("driverClassLabel"));
        gridBag.setConstraints(label, labelConstraints);
        this.add(label);
        field = new JPanel();
        field.add(this.driverClass);
        gridBag.setConstraints(field, fieldConstraints);
        this.add(field);
        
        label = new JLabel(BuilderBundle.getString("driverClassLocationLabel"));
        gridBag.setConstraints(label, labelConstraints);
        this.add(label);
        field = new JPanel();
        field.add(this.driverClassLocation);
        field.add(this.driverClassLocationButton);
        gridBag.setConstraints(field, fieldConstraints);
        this.add(field);
        
        label = new JLabel(BuilderBundle.getString("hostLabel"));
        gridBag.setConstraints(label, labelConstraints);
        this.add(label);
        field = new JPanel();
        field.add(this.host);
        label = new JLabel(BuilderBundle.getString("portLabel"));
        field.add(label);
        field.add(this.port);
        label = new JLabel(BuilderBundle.getString("databaseLabel"));
        field.add(label);
        field.add(this.database);
        gridBag.setConstraints(field, fieldConstraints);
        this.add(field);
        
        label = new JLabel(BuilderBundle.getString("jdbcURLLabel"));
        gridBag.setConstraints(label, labelConstraints);
        this.add(label);
        field = new JPanel();
        field.add(this.jdbcURL);
        gridBag.setConstraints(field, fieldConstraints);
        this.add(field);
        
        label = new JLabel(BuilderBundle.getString("usernameLabel"));
        gridBag.setConstraints(label, labelLastRowConstraints);
        this.add(label);
        field = new JPanel();
        field.add(this.username);
        label = new JLabel(BuilderBundle.getString("passwordLabel"));
        field.add(label);
        field.add(this.password);
        gridBag.setConstraints(field, fieldLastRowConstraints);
        this.add(field);
        
        // attach the file chooser to the button
        final JPanel panel = this;
        this.driverClassLocationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (jarFileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                    File file = jarFileChooser.getSelectedFile();
                    if (file != null) {
                        driverClassLocation.setText(file.toString());
                    }
                }
            }
        });
        
        // reset the fields
        this.resetFields(template);
    }
    
    /**
     * Resets the fields to their default values.
     */
    private void resetFields(Schema template) {
        // All-comers.
        this.copysettings.setSelectedIndex(-1);
        // Specifics.
        if (template instanceof JDBCSchema) {
            this.copySettingsFrom(template);
        }
        // Everyone else.
        else {
            this.keyguessing.setSelected(false);
            this.driverClass.setSelectedIndex(-1);
            this.driverClassLocation.setText(null);
            this.jdbcURL.setText(null);
            this.host.setText(null);
            this.port.setText(null);
            this.database.setText(null);
            this.username.setText(null);
            this.password.setText(null);
        }
    }
    
    /**
     * Resets the fields to their default values.
     */
    private void copySettingsFrom(Schema template) {
        if (template instanceof JDBCSchema) {
            JDBCSchema jdbcSchema = (JDBCSchema)template;
            this.keyguessing.setSelected(jdbcSchema.isKeyGuessing());
            this.driverClass.setSelectedItem(jdbcSchema.getDriverClassName());
            this.driverClassLocation.setText(
                    jdbcSchema.getDriverClassLocation() == null
                    ? null
                    : jdbcSchema.getDriverClassLocation().toString());
            String jdbcURL = jdbcSchema.getJDBCURL();
            this.jdbcURL.setText(jdbcURL);
            this.username.setText(jdbcSchema.getUsername());
            this.password.setText(jdbcSchema.getPassword());
            // Parse the JDBC URL into host, port and database if driver known.
            String regexURL = new String(this.currentJDBCURLTemplate);
            regexURL = regexURL.replaceAll("<HOST>","(.*)");
            regexURL = regexURL.replaceAll("<PORT>","(.*)");
            regexURL = regexURL.replaceAll("<DATABASE>","(.*)");
            Pattern regex = Pattern.compile(regexURL);
            Matcher matcher = regex.matcher(jdbcURL);
            if (matcher.matches()) {
                this.host.setText(matcher.group(1));
                this.port.setText(matcher.group(2));
                this.database.setText(matcher.group(3));
            }
        }
    }
    
    /**
     * Validates the fields.
     */
    public boolean validateFields() {
        List messages = new ArrayList();
        
        if (this.isEmpty((String)this.driverClass.getSelectedItem())) {
            messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("driverClass")));
        } else {
            if (this.driverClassLocation.isEnabled() && this.isEmpty(this.driverClassLocation.getText()))
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("driverClassLocation")));
        }
        
        if (this.jdbcURL.isEnabled()) {
            if (this.isEmpty(this.jdbcURL.getText()))
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("jdbcURL")));
        } else {
            if (this.isEmpty(this.host.getText()))
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("host")));
            if (this.isEmpty(this.port.getText()))
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("port")));
            if (this.isEmpty(this.database.getText()))
                messages.add(BuilderBundle.getString("fieldIsEmpty", BuilderBundle.getString("database")));
        }
        
        if (this.isEmpty(this.username.getText())) {
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
    public Schema createSchema(String name) {
        if (!this.validateFields()) return null;
        else {
            try {
                boolean keyguessing = this.keyguessing.isSelected();
                String driverClassName = (String)this.driverClass.getSelectedItem();
                String driverClassLocation = this.driverClassLocation.getText();
                String url = this.jdbcURL.getText();
                String username = this.username.getText();
                String password = new String(this.password.getPassword());
                return MartUtils.createJDBCSchema(
                        driverClassLocation == null ? null : new File(driverClassLocation),
                        driverClassName,
                        url,
                        username,
                        password,
                        name,
                        keyguessing
                        );
            } catch (Throwable t) {
                this.schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder().showStackTrace(t);
            }
            return null;
        }
    }
    
    /**
     * Creates a table provider from the fields given.
     */
    public Schema modifySchema(Schema schema) {
        if (!this.validateFields()) return null;
        else {
            if (schema instanceof JDBCSchema) {
                try {
                    JDBCSchema jschema = (JDBCSchema)schema;
                    jschema.setKeyGuessing(this.keyguessing.isSelected());
                    jschema.setDriverClassName((String)this.driverClass.getSelectedItem());
                    String driverClassLocation = this.driverClassLocation.getText();
                    jschema.setDriverClassLocation(driverClassLocation == null ? null : new File(driverClassLocation));
                    jschema.setJDBCURL(this.jdbcURL.getText());
                    jschema.setUsername(this.username.getText());
                    jschema.setPassword(new String(this.password.getPassword()));
                } catch (Throwable t) {
                    this.schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
            return schema;
        }
    }
    
    /**
     * Tests a string for non-nullness.
     */
    private boolean isEmpty(String string) {
        return (string == null || string.trim().length() == 0);
    }
    
    public void actionPerformed(ActionEvent e) {
        // Copy settings?
        if (e.getSource() == this.copysettings) {
            Object obj = this.copysettings.getSelectedItem();
            if (obj!=null) this.copySettingsFrom((Schema)obj);
        }
        // Change driver class?
        else if (e.getSource() == this.driverClass) {
            String className = (String)this.driverClass.getSelectedItem();
            if (className!=null) {
                // Selected from list.
                if (JDBCConnectionPanel.DRIVER_MAP.containsKey(className)) {
                    String[] parts = (String[])JDBCConnectionPanel.DRIVER_MAP.get(className);
                    this.currentJDBCURLTemplate = parts[1];
                    this.port.setText(parts[0]);
                    this.jdbcURL.setEnabled(false);
                    this.host.setEnabled(true);
                    this.port.setEnabled(true);
                    this.database.setEnabled(true);
                    // Update fields.
                    if (this.jdbcURL.isEnabled()) {
                        // We're changing from custom to predefined.
                        this.jdbcURL.setText(null);
                        this.host.setText(null);
                        this.database.setText(null);
                    } else {
                        // We're changing from predefined to another predefined.
                        // No need to do anything.
                    }
                }
                // User-defined.
                else {
                    this.currentJDBCURLTemplate = null;
                    // Update fields.
                    this.jdbcURL.setEnabled(true);
                    this.host.setEnabled(false);
                    this.port.setEnabled(false);
                    this.database.setEnabled(false);
                    if (this.jdbcURL.isEnabled()) {
                        // We're changing from custom to another custom.
                        // No need to take any action.
                    } else {
                        // We're changing from predefined to custom.
                        this.jdbcURL.setText(null);
                        this.host.setText(null);
                        this.port.setText(null);
                        this.database.setText(null);
                    }
                }
                // Attempt to load the driver class.
                boolean classNotFound = true;
                try {
                    Class.forName(className);
                    classNotFound = false;
                } catch (Throwable t) {
                    classNotFound = true;
                }
                // If not found, check the class location.
                this.driverClassLocation.setEnabled(classNotFound);
            }
            // Nothing selected? Grey out the dependent bits.
            else {
                this.jdbcURL.setEnabled(false);
                this.host.setEnabled(false);
                this.port.setEnabled(false);
                this.database.setEnabled(false);
                this.driverClassLocation.setEnabled(false);
            }
        }
    }
    
    private class JDBCURLConstructor implements DocumentListener {
        private JTextField host;
        private JTextField port;
        private JTextField database;
        private JTextField jdbcURL;
        public JDBCURLConstructor(JTextField host, JTextField port, JTextField database, JTextField jdbcURL) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.jdbcURL = jdbcURL;
        }
        public void insertUpdate(DocumentEvent e) {
            this.updateJDBCURL();
        }
        public void removeUpdate(DocumentEvent e) {
            this.updateJDBCURL();
        }
        public void changedUpdate(DocumentEvent e) {
            this.updateJDBCURL();
        }
        private void updateJDBCURL() {
            if (currentJDBCURLTemplate == null) return;
            // Update the JDBC URL based on our current settings.
            String newURL = new String(currentJDBCURLTemplate);
            if (!isEmpty(this.host.getText())) newURL = newURL.replaceAll("<HOST>",this.host.getText());
            if (!isEmpty(this.port.getText())) newURL = newURL.replaceAll("<PORT>",this.port.getText());
            if (!isEmpty(this.database.getText())) newURL = newURL.replaceAll("<DATABASE>",this.database.getText());
            this.jdbcURL.setText(newURL);
        }
    }
}
