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

import java.awt.Dimension;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class QueryEditor extends JPanel {

  private Dimension preferedSize;
  private MartConfiguration config;
  

  public QueryEditor(MartConfiguration config) {
    this.config = config;
    preferedSize = new Dimension( 100, 100 );
    setPreferredSize( preferedSize );
  }

	public static void main(String[] args) throws ConfigurationException {
    String confFile = "data/xmltest/test_file.xml";
    URL confURL = ClassLoader.getSystemResource(confFile);
    MartConfiguration config = new MartConfigurationFactory().getInstance(confURL);
    
   QueryEditor editor = new QueryEditor( config );
   JFrame f = new JFrame( "Query Editor" );
   f.getContentPane().add( editor );
   f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
   f.pack();
   f.setVisible( true );
	}
}
