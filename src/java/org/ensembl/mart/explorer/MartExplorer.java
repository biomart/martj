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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import  java.util.prefs.*;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;

/**
 * MartExplorer Graphical User Interface provides a graphical interface
 * to Mart databases.
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MartExplorer extends JFrame {
	
	private static final int WIDTH = 700;
	private static final int HEIGHT = 700;
  private static final String CONFIG_FILE_KEY = "CONFIG_FILE_KEY";
	
	private JFileChooser configFileChooser = null;
	private Preferences prefs = null;
 
    
	private QueryManager queryManager = new QueryManager();
	//private Engine engine = new Engine();
	  
    
	public MartExplorer() {
		super("Mart Explorer");
        
    prefs = Preferences.userNodeForPackage( this.getClass() );
        
    initConfigFileChooser();    
		setJMenuBar(createMenuBar());

		setSize(WIDTH, HEIGHT);

		

	}


	/**
	 * Initialises _configFileChooser_. Sets the last loaded 
   * config file if available and makes the 
   * chooser only show XML files.
	 */
	private void initConfigFileChooser() {
    configFileChooser = new JFileChooser();
		FileFilter xmlFilter = new FileFilter() {
			public boolean accept(File f) {
				return f != null
					&& (f.isDirectory()
						|| f.getName().toLowerCase().endsWith(".xml"));
			}
			public String getDescription() {
				return "XML Files";
			}
		};

		String lastChosenFile = prefs.get(CONFIG_FILE_KEY, null);

		if (lastChosenFile != null) {
			configFileChooser.setSelectedFile(new File(lastChosenFile));
		}
		configFileChooser.addChoosableFileFilter(xmlFilter);

	}

	public void warn(String message) {
		JOptionPane.showMessageDialog(
			this,
			message,
			"Warning",
			JOptionPane.WARNING_MESSAGE);
	}

	public void warn(String message, Exception e) {
		warn(message + ":" + e.getMessage());
		e.printStackTrace();
	}

	public void connectToDatabase() {
		// TODO
	}

	/**
	 * Presents the user with a file chooser dialog with which she can 
	 * choose a configuration file. 
	 */
	public void loadConfigFromFile() {
		// user chooses file
		
		int action = configFileChooser.showOpenDialog(this);

		// convert file contents into string
		if (action == JFileChooser.APPROVE_OPTION) {
			File f = configFileChooser.getSelectedFile().getAbsoluteFile();
			prefs.put( CONFIG_FILE_KEY, f.toString() ) ;
			try {
				MartConfiguration config = new MartConfigurationFactory().getInstance( f.toURL() );
			} catch (MalformedURLException e) {
				warn("Couldn't find file. " + e);
			} catch (ConfigurationException e) {
				warn( "Problem loading configuration file. ", e );
			}
							
		}
	}



	/**
	 * Exits the programs.
	 */
	public void exit() {
		System.exit(0);	
	}

	/**
		 * 
		 */
		public void newQuery() {
			// TODO Auto-generated method stub
		
		}
		

	public void executeQuery() {
				// TODO Auto-generated method stub
		
			}
			
	public void save() {
				// TODO Auto-generated method stub
		
			}

	public void saveAs() {
				// TODO Auto-generated method stub
		
			}
			
	public void about() {
				// TODO Auto-generated method stub
		
			}

	/**
	 * @return
	 */
	private JMenuBar createMenuBar() {
		
		
		JMenu file = new JMenu("File");
		
		JMenuItem connectDB = new JMenuItem("Connect to database");
		file.add( connectDB );
		connectDB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				connectToDatabase();		
			}

		});
		
		JMenuItem loadConfig = new JMenuItem("Load config from file");
		file.add( loadConfig );
		loadConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				loadConfigFromFile();		
			}
		});
		
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				exit();		
			}

		});
		file.add( exit );
		
		JMenu query = new JMenu("Query");
		JMenuItem newQuery = new JMenuItem("New");
		newQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				newQuery();		
			}

		});
		query.add( newQuery );
		JMenuItem execute = new JMenuItem("Execute");
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				executeQuery();		
			}

		});
		query.add( execute );
		JMenuItem save = new JMenuItem("Save Results");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				save();		
			}

		});
		query.add( save );
		JMenuItem saveAs = new JMenuItem("Save Results as");
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				saveAs();		
			}

		});
		query.add( saveAs );		
		
		JMenu help = new JMenu("Help");
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				about();		
			}

		});
		help.add( about );
		
		JMenuBar all = new JMenuBar();
		all.add( file );
		all.add( query );
		all.add( help );
		return all;
	}

	


	


	public static void main(String[] args) {
		new MartExplorer().setVisible( true );
	}
}
