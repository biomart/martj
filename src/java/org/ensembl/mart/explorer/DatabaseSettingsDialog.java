/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.explorer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatabaseSettingsDialog extends Box {

	
	private Preferences preferences;
	
	private LabelledComboBox databaseType;
	private LabelledComboBox host;
	private LabelledComboBox port;
	private LabelledComboBox database;
	private LabelledComboBox user;
  
	private JPasswordField password;
  private String passwordPreferenceKey;
  private JCheckBox rememberPassword;
  private String rememberPasswordKey;
  
	public DatabaseSettingsDialog() {
		this(null);
	}

	public DatabaseSettingsDialog(Preferences preferences) {
		
		super( BoxLayout.Y_AXIS );
		
    databaseType = new LabelledComboBox("Database type");
    databaseType.setPreferenceKey("database_type");
    databaseType.setEditable( false );
    add( databaseType );
    	
		host = new LabelledComboBox("Host");
		host.setPreferenceKey("host");
		add( host );
		
		port = new LabelledComboBox("Port");
		port.setPreferenceKey( "port" );
		add( port );
		
		database = new LabelledComboBox("Database");
		database.setPreferenceKey("database");
		add( database );
		
		user = new LabelledComboBox("User");
		user.setPreferenceKey("user");
		add( user );	
		
		
    add( createPasswordPanel() );
    		
		if ( preferences!=null ) setPrefs(preferences);
		
	}

	/**
   * Constructs password panel.
	 * @return
	 */
	private Box createPasswordPanel() {

		password = new JPasswordField("Password");
		passwordPreferenceKey = "password";

		rememberPassword = new JCheckBox("Remember Password", false);
		rememberPasswordKey = "store_password";
		rememberPassword.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {

				if (rememberPassword.isSelected()) {

					int option =
						JOptionPane.showOptionDialog(
							rememberPassword,
							"The password will be stored unencrypted."
								+ "\nAnyone with access to your account"
								+ "\ncould read this password. "
								+ "\nRemember it?",
							"Remember Password?",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
					    null,
							null);
					rememberPassword.setSelected(option == JOptionPane.YES_OPTION);
				}

			}
		});

		Box box = Box.createHorizontalBox();
		box.add(new JLabel("Password"));
		box.add(password);
		box.add(rememberPassword);

		return box;
	}

	public boolean showDialog(Component parent) {

		int option =
			JOptionPane.showOptionDialog(
				parent,
				this,
				"Database Connection Settings",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				null,
				null);

		if (option != JOptionPane.OK_OPTION)
			return false;

    storePreferences( preferences );
		return true;

	}

	/**
	 * 
	 */
	private void storePreferences( Preferences preferences) {

		//  persist state for next time program runs
		databaseType.store(preferences, 10);
		host.store(preferences, 10);
		port.store(preferences, 10);
		database.store(preferences, 10);
		user.store(preferences, 10);
    
    preferences.putBoolean( rememberPasswordKey, rememberPassword.isSelected() );

		if (rememberPassword.isSelected())
			preferences.put(passwordPreferenceKey, getPassword());
		else
			preferences.remove(passwordPreferenceKey);

		try {

			// write preferences to persistent storage
			preferences.flush();

		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

	}


	private void loadPreferences(Preferences preferences) {

		databaseType.load(preferences);
		host.load(preferences);
		port.load(preferences);
		database.load(preferences);
		user.load(preferences);
    
    rememberPassword.setSelected( preferences.getBoolean( rememberPasswordKey, false) );
		password.setText(preferences.get(passwordPreferenceKey, ""));
	}


	public static void main(String[] args) {
		
		DatabaseSettingsDialog d = new DatabaseSettingsDialog( );
		d.setPrefs( Preferences.userNodeForPackage( d.getClass() ) );
		d.showDialog( null );
		System.exit(0);
	}

	

	public LabelledComboBox getHost() {
		return host;
	}

  public LabelledComboBox getDatabaseType() {
    return databaseType;
  }

	public void setPrefs(Preferences prefs) {
		
		this.preferences = prefs;
		
    loadPreferences( preferences );
	
	}

	public Preferences getPrefs() {
		return preferences;
	}


	
	/**
	 * @return database input box
	 */
	public LabelledComboBox getDatabase() {
		return database;
	}

	/**
	 * @return password input box
	 */
	public String getPassword() {
		return new String( password.getPassword() );
	}

	/**
	 * @return port input box
	 */
	public LabelledComboBox getPort() {
		return port;
	}

	/**
	 * @return User input box
	 */
	public LabelledComboBox getUser() {
		return user;
	}

	/**
	 * @param box
	 */
	public void setDatabase(LabelledComboBox box) {
		database = box;
	}


	/**
	 * @param box
	 */
	public void setPort(LabelledComboBox box) {
		port = box;
	}

	/**
	 * @param box
	 */
	public void setUser(LabelledComboBox box) {
		user = box;
	}

}
