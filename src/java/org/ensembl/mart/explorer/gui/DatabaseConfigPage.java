/* Generated by Together */

package org.ensembl.mart.explorer.gui;

import javax.swing.*;
import org.ensembl.mart.explorer.*;

/** Provides input options for specifying which database to connect to. */
public class DatabaseConfigPage extends JPanel implements org.ensembl.mart.explorer.gui.QueryInputPage {
    /** Creates new form DatabaseConfigPane */
    public DatabaseConfigPage() {
        initGUI();
    }

    /** This method is called from within the constructor to initialize the form. */
    private void initGUI() {
        port.setEditable(true);
        port.setToolTipText("Database port");
        passwordLabel.setText("Password");
        passwordLabel.setToolTipText("Host Name");
        passwordPanel.setLayout(new javax.swing.BoxLayout(passwordPanel, javax.swing.BoxLayout.X_AXIS));
        passwordPanel.add(passwordLabel);
        passwordPanel.add(password);
        userLabel.setText("User");
        userLabel.setToolTipText("User Name");
        userPanel.setLayout(new javax.swing.BoxLayout(userPanel, javax.swing.BoxLayout.X_AXIS));
        userPanel.add(userLabel);
        userPanel.add(user);
        portLabel.setText("Port");
        portLabel.setToolTipText("Port Number");
        portPanel.setLayout(new javax.swing.BoxLayout(portPanel, javax.swing.BoxLayout.X_AXIS));
        portPanel.add(portLabel);
        portPanel.add(port);
        hostLabel.setText("Host");
        hostLabel.setToolTipText("Host Name");
        hostPanel.setLayout(new javax.swing.BoxLayout(hostPanel, javax.swing.BoxLayout.X_AXIS));
        hostPanel.add(hostLabel);
        hostPanel.add(host);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        add(hostPanel);
        add(portPanel);
        add(userPanel);
        add(passwordPanel);
        add(databasePanel);
        host.setEditable(true);
        host.setToolTipText("Database host computer");
        user.setEditable(true);
        user.setToolTipText("Database user name");
        databaseLabel.setText("Database");
        databaseLabel.setToolTipText("Species to retrieve data about");
        databasePanel.setLayout(new javax.swing.BoxLayout(databasePanel, javax.swing.BoxLayout.X_AXIS));
        databasePanel.add(databaseLabel);
        databasePanel.add(database);
        database.setToolTipText("Database");
        database.setEditable(true);
        password.setText("");
        password.setPreferredSize(new java.awt.Dimension(4, 21));
        password.setMaximumSize(new java.awt.Dimension(2147483647, 21));
        password.setMinimumSize(new java.awt.Dimension(4, 21));
        password.setSize(new java.awt.Dimension(431, 21));
    }

    public void updateQuery(Query query)  throws InvalidQueryException {
        query.setHost( Tool.selected ( host ) );
        query.setPort( Tool.selected ( port ) );
        query.setUser( Tool.selected ( user ) );
        query.setPassword( new String(password.getPassword()) );
        query.setDatabase( Tool.selected ( database ) );
    }

    public void updatePage(Query query){
			Tool.prepend( query.getHost(), host );
			Tool.prepend( query.getPort(), port );
      Tool.prepend( query.getDatabase(), database );
      Tool.prepend( query.getUser(), user );
    }

    /**
     * Removes all selected values. 
     */
    public void clear(){
			Tool.clear( host );
			Tool.clear( port );
			Tool.clear( user );
			password.setText( "" );
			Tool.clear( database );

    }

    private JPanel hostPanel = new JPanel();
    private JLabel hostLabel = new JLabel();
    private JPanel portPanel = new JPanel();
    private JLabel portLabel = new JLabel();
    private JPanel userPanel = new JPanel();
    private JLabel userLabel = new JLabel();
    private JPanel passwordPanel = new JPanel();
    private JLabel passwordLabel = new JLabel();
    private JComboBox host = new JComboBox();
    private JComboBox port = new JComboBox();
    private JComboBox user = new JComboBox();
    private JPanel databasePanel = new JPanel();
    private JLabel databaseLabel = new JLabel();
    private JComboBox database = new JComboBox();
    private JPasswordField password = new JPasswordField();
}
