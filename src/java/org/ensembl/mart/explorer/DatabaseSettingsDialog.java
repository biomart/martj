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

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatabaseSettingsDialog extends JPanel {

	private LabelledComboBox host = null;
	private Preferences prefs = null;
	
	public DatabaseSettingsDialog() {
		this( null );
	}

	public DatabaseSettingsDialog(Preferences preferences) {
		host = new LabelledComboBox("Host");
		add( host );
		
		if ( prefs!=null ) setPrefs(prefs);
		
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
				
		if ( option==JOptionPane.OK_OPTION ) {
			// persist state for next time program runs
			prefs.put("history.host", host.toPreferenceString(3) );
			System.out.println("Loading from prefs: " + prefs.get("history.host", "") );
			
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
			
			return true;
		} else {
			return false;
		}
	}

	public static void main(String[] args) {
		
		DatabaseSettingsDialog d = new DatabaseSettingsDialog( );
		d.setPrefs( Preferences.userNodeForPackage( d.getClass() ) );
		JFrame f = new JFrame();
		f.getContentPane().add( d );
		System.out.println( Boolean.toString( d.showDialog( f ) ));
		System.out.println( d.getHost() );
		System.exit(0);
	}

	

	public String getHost() {
		return host.getText();
	}

	public void setPrefs(Preferences prefs) {
		this.prefs = prefs;
		host.parsePreferenceString( prefs.get("history.host", "") );
	}

	public Preferences getPrefs() {
		return prefs;
	}
}
