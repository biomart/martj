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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Box;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatabaseSettingsDialog extends Box {

	private Preferences preferences = null;
	
	private LabelledComboBox host = null;
	private LabelledComboBox port = null;
	private LabelledComboBox database = null;
	private LabelledComboBox user = null;
	private LabelledComboBox password = null;
	
	
	public DatabaseSettingsDialog() {
		this( null );
	}

	public DatabaseSettingsDialog(Preferences preferences) {
		
		super( BoxLayout.Y_AXIS );
		
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
		
		password = new LabelledComboBox("Password");
		password.setPreferenceKey("password");
		add( password );
		
		if ( preferences!=null ) setPrefs(preferences);
		
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

		// persist state for next time program runs
		host.store(preferences, 10);
		port.store(preferences, 10);
		database.store(preferences, 10);
		user.store(preferences, 10);
		password.store(preferences, 10);

		try {

			// write preferences to persistent storage
			preferences.flush();

		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		return true;

	}

	public static void main(String[] args) {
		
		DatabaseSettingsDialog d = new DatabaseSettingsDialog( );
		d.setPrefs( Preferences.userNodeForPackage( d.getClass() ) );
		JFrame f = new JFrame();
		f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		f.pack();
		f.setVisible( true );
		d.showDialog( f );
		
	}

	

	public String getHost() {
		return host.getText();
	}

	public void setPrefs(Preferences prefs) {
		
		this.preferences = prefs;
		
		host.load( prefs );
		port.load( prefs );
		database.load( prefs );
		user.load( prefs );
		password.load( prefs );
	
	}

	public Preferences getPrefs() {
		return preferences;
	}
}
