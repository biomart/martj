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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

/**
 * MartExplorer Graphical User Interface provides a graphical interface
 * to Mart databases.
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MartExplorer extends JFrame {

  private Logger logger = Logger.getLogger(MartExplorer.class.getName());

  private static final String CONFIG_FILE_KEY = "CONFIG_FILE_KEY";

  private static final Dimension PREFERRED_SIZE = new Dimension(1024, 768);

  /** Currently available datasets. */
  private List datasetViews = new ArrayList();

  /** Currently visible QueryEditors **/
  private List queryEditors = new ArrayList();

  private JFileChooser configFileChooser;

  private JTabbedPane queryEditorTabbedPane = new JTabbedPane();

  private DatabaseSettingsDialog databaseSettings;

  /** Persistent preferences object used to hold user history. */
  private Preferences prefs;

  public DatasetView[] getDatasetViews() {
    return (DatasetView[]) datasetViews.toArray(
      new DatasetView[datasetViews.size()]);
  }

  public static void main(String[] args) throws ConfigurationException {
    MartExplorer me = new MartExplorer();
    me.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    me.setVisible(true);
    
    // test mode preloads datasets and sets up a query ready to use.
    if ( true ) {
      
      DatasetView[] dsViews = QueryEditor.testDSViews();
      me.resolveAndAddDatasetVies( dsViews );
//      QueryEditor qe = new QueryEditor();
//      qe.setDatasetViews( dsViews );
//      qe.sel)
//      me.addQueryEditor( qe );
      me.doNewQuery();
    }
  }

  public MartExplorer() {

    super("Mart Explorer");

    prefs = Preferences.userNodeForPackage(this.getClass());

    initConfigFileChooser();
    initDatabaseSettings();
    setJMenuBar(createMenuBar());
    getContentPane().add(queryEditorTabbedPane);
    setSize(PREFERRED_SIZE);

  }

  public void doAbout() {
    // TODO Auto-generated method stub

  }

  public void doLoadDatasetsFromDatabase() {
    if (databaseSettings.showDialog(this)) {

      System.out.println("todo - use db settings");
    }
  }

  /**
   * Adds QueryEditor to application as a tabbed pane. The tab's
   * name id queryEditor.getName(). If no name is supplied a new name
   * is created and it is assigned to both the tab and queryEditor.name.
   * @param queryEditor
   */
  private void addQueryEditor(QueryEditor queryEditor) {
    String name = queryEditor.getName();
    if (name == null || "".equals(name)) {
      name = nextQueryBuilderTabLabel();
      queryEditor.setName(name);
    }
    queryEditorTabbedPane.add(name, queryEditor);
  }


  /**
   * 
   * @return "Query_INDEX" where INDEX is the next highest
   * unique number used in the tab names.
   */
  private String nextQueryBuilderTabLabel() {
    
    int next = 1;
    
    int n = queryEditorTabbedPane.getTabCount();
    Pattern p = Pattern.compile("Query_(\\d+)");
    for (int i = 0; i < n; i++) {
      String title = queryEditorTabbedPane.getTitleAt( i );
      Matcher  m = p.matcher( title );
      if ( m.matches() ) {
        int tmp = Integer.parseInt( m.group(1) ) + 1;
        if ( tmp > next ) 
          next = tmp;
      }       
    }
    
    return "Query_" + next;
  }

  /**
   * @return
   */
  private JMenuBar createMenuBar() {

    JMenu file = new JMenu("File");

    JMenuItem exit = new JMenuItem("Exit");
    exit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doExit();
      }

    });
    file.add(exit);

    JMenu dataset = new JMenu("Datasets");

    JMenuItem loadDB = new JMenuItem("Load from database");
    dataset.add(loadDB);
    loadDB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doLoadDatasetsFromDatabase();
      }

    });

    JMenuItem loadConfig = new JMenuItem("Load config from file");
    dataset.add(loadConfig);
    loadConfig.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doLoadDatasetsFromFile();
      }
    });

    JMenuItem loadRegistry = new JMenuItem("Load config from registry file");
    dataset.add(loadRegistry);
    loadRegistry.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doLoadDatasetsFromRegistry();
      }

    });

    JMenu query = new JMenu("Query");
    JMenuItem newQuery = new JMenuItem("New");
    newQuery.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doNewQuery();
      }
    });
    query.add(newQuery);

    JMenuItem deleteQuery = new JMenuItem("Delete");
    deleteQuery.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doDeleteQuery();
      }
    });
    query.add(deleteQuery);

    JMenuItem execute = new JMenuItem("Execute");
    execute.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doExecuteQuery();
      }

    });
    query.add(execute);
    JMenuItem save = new JMenuItem("Save Results");
    save.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doSave();
      }

    });
    query.add(save);
    JMenuItem saveAs = new JMenuItem("Save Results as");
    saveAs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doSaveAs();
      }

    });
    query.add(saveAs);

    JMenu help = new JMenu("Help");
    JMenuItem about = new JMenuItem("About");
    about.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doAbout();
      }

    });
    help.add(about);

    JMenuBar all = new JMenuBar();
    all.add(file);
    all.add(dataset);
    all.add(query);
    all.add(help);
    return all;
  }

  /**
   * Delete currently selected QueryBuilder from tabbed pane if one is 
   * selected.
   */
  protected void doDeleteQuery() {
    
    int index = queryEditorTabbedPane.getSelectedIndex();
    if ( index > -1 ) {
      queryEditorTabbedPane.remove( index );
    }
  }

  public void doExecuteQuery() {
    // TODO Auto-generated method stub

  }

  /**
   * Exits the programs.
   */
  public void doExit() {
    System.exit(0);
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
          && (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
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

  private void initDatabaseSettings() {
    databaseSettings = new DatabaseSettingsDialog();

    // load supported database types into preferences. We do this rather than just 
    // loading them directly because if there are more than one wee don't want to 
    // override the last selected one.
    String key = databaseSettings.getDatabaseType().getPreferenceKey();
    String current = prefs.get(key, null);
    if (current == null || current.length() == 0) {
      prefs.put(key, "mysql");
    }
    databaseSettings.setPrefs(prefs);
  }

  /**
   * Presents the user with a file chooser dialog with which she can 
   * choose a configuration file. 
   */
  public void doLoadDatasetsFromFile() {
    // user chooses file

    int action = configFileChooser.showOpenDialog(this);

    // convert file contents into string
    if (action == JFileChooser.APPROVE_OPTION) {
      File f = configFileChooser.getSelectedFile().getAbsoluteFile();
      prefs.put(CONFIG_FILE_KEY, f.toString());

      try {
        URLDSViewAdaptor adaptor = new URLDSViewAdaptor(f.toURL(), false);
        DatasetView[] newViews = adaptor.getDatasetViews();
        resolveAndAddDatasetVies(newViews);

      } catch (MalformedURLException e) {
        JOptionPane.showMessageDialog(
          this,
          "File " + f.toString() + " not found: " + e.getMessage());
      } catch (ConfigurationException e) {
        JOptionPane.showMessageDialog(
          this,
          "Problem loading the Failed to load file: "
            + f.toString()
            + ": "
            + e.getMessage());

      }

    }
  }

  /**
   * Adds new DatasetViews to the application but forces the user
   * to resolve any name.version clashes.  
   * @param newViews
   */
  private void resolveAndAddDatasetVies(DatasetView[] newViews) {

    DatasetView[] currentViews = getDatasetViews();
    // TODO find any clashes
    // TODO user to resolve clashes
    datasetViews.addAll(Arrays.asList(newViews));
  }

  private void doLoadDatasetsFromRegistry() {
    // TODO Auto-generated method stub
    logger.warning("TODO");
  }

  /**
  	 * 
  	 */
  public void doNewQuery() {

    if (datasetViews.size() == 0) {
      warn(
        "No datasets available. You need load one or more "
          + "datasets before you can create a query.");
    } else {
      QueryEditor qe = new QueryEditor();
      qe.setDatasetViews(getDatasetViews());
      addQueryEditor(qe);
    }

  }

  public void doSave() {
    // TODO Auto-generated method stub

  }

  public void doSaveAs() {
    // TODO Auto-generated method stub

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
}
