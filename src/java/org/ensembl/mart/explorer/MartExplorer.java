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
 * MartExplorer is a graphical application that enables a 
 * user to construct queries and execute them against Mart databases.
 */
public class MartExplorer extends JFrame {

  // TODO add database -> datasource
  // TODO remove datatabase
  // TODO set queryBuilder.dataSource -> q.dataSource
  // TODO exec query, results to file.
  // TODO display results file
  // TODO load registry file
  // TODO bind view and database

  private Logger logger = Logger.getLogger(MartExplorer.class.getName());

  private static final String CONFIG_FILE_KEY = "CONFIG_FILE_KEY";

  private static final Dimension PREFERRED_SIZE = new Dimension(1024, 768);

  /** Currently available datasets. */
  private List datasetViews = new ArrayList();

  /** Currently available databases. */
  private List databases = new ArrayList();

  /** Currently visible QueryEditors **/
  private List queryEditors = new ArrayList();

  private JFileChooser configFileChooser;

  private JTabbedPane queryEditorTabbedPane = new JTabbedPane();

  private DatabaseSettingsDialog databaseDialog;

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
    if (true) {

      DatasetView[] dsViews = QueryEditor.testDSViews();
      me.resolveAndAddDatasetVies(dsViews);
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
      String title = queryEditorTabbedPane.getTitleAt(i);
      Matcher m = p.matcher(title);
      if (m.matches()) {
        int tmp = Integer.parseInt(m.group(1)) + 1;
        if (tmp > next)
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

    JMenuItem loadRegistry = new JMenuItem("Load registry file");
    loadRegistry.setEnabled(false);
    file.add(loadRegistry);
    loadRegistry.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doLoadRegistry();
      }
    });

    JMenuItem exit = new JMenuItem("Exit");
    exit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doExit();
      }

    });
    file.add(exit);

    JMenu query = new JMenu("Query");
    JMenuItem newQuery = new JMenuItem("New");
    newQuery.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doNewQuery();
      }
    });
    query.add(newQuery);

    JMenuItem removeQuery = new JMenuItem("Remove");
    removeQuery.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doRemoveQuery();
      }
    });
    query.add(removeQuery);

    JMenuItem execute = new JMenuItem("Execute");
    execute.setEnabled(false);
    execute.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doExecuteQuery();
      }

    });
    query.add(execute);
    JMenuItem save = new JMenuItem("Save Results");
    save.setEnabled(false);
    save.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doSave();
      }

    });
    query.add(save);
    JMenuItem saveAs = new JMenuItem("Save Results as");
    saveAs.setEnabled(false);
    saveAs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doSaveAs();
      }

    });
    query.add(saveAs);

    JMenu settings = new JMenu("Settings");

    JMenu databases = new JMenu("Databases");
    settings.add(databases);

    JMenuItem addDatabase = new JMenuItem("Add");
    databases.add(addDatabase);
    addDatabase.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doAddDatabase();
      }
    });

    JMenuItem removeDatabase = new JMenuItem("Remove");
    removeDatabase.setEnabled(false);
    databases.add(removeDatabase);
    removeDatabase.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doRemoveDatabase();
      }
    });

    JMenu views = new JMenu("Views");
    settings.add(views);

    JMenuItem loadViewFromFile = new JMenuItem("Load from local file");
    views.add(loadViewFromFile);
    loadViewFromFile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doLoadDatasetsFromFile();
      }
    });

    JMenuItem bind = new JMenuItem("Bind to Databases");
    bind.setEnabled(false);
    views.add(bind);
    bind.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doBindViewsAndDatabases();
      }
    });

    JMenu help = new JMenu("Help");
    JMenuItem about = new JMenuItem("About");
    about.setEnabled(false);
    about.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doAbout();
      }

    });
    help.add(about);

    JMenuBar all = new JMenuBar();
    all.add(file);
    all.add(query);
    all.add(settings);
    all.add(help);
    return all;
  }

  /**
   * 
   */
  protected void doBindViewsAndDatabases() {
    // TODO Auto-generated method stub

  }

  /**
   * 
   */
  protected void doConfigureDatabases() {
    // TODO Auto-generated method stub

  }

  /**
   * 
   */
  protected void doRemoveDatabase() {
    // TODO Auto-generated method stub

  }

  /**
   * 
   */
  protected void doAddDatabase() {
    // TODO use this dialog to get settings.
    databaseDialog.showDialog(this);
    }

  /**
   * Delete currently selected QueryBuilder from tabbed pane if one is 
   * selected.
   */
  protected void doRemoveQuery() {

    int index = queryEditorTabbedPane.getSelectedIndex();
    if (index > -1) {
      queryEditorTabbedPane.remove(index);
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
    databaseDialog = new DatabaseSettingsDialog();

    // load supported database types into preferences. We do this rather than just 
    // loading them directly because if there are more than one wee don't want to 
    // override the last selected one.
    String key = databaseDialog.getDatabaseType().getPreferenceKey();
    String current = prefs.get(key, null);
    if (current == null || current.length() == 0) {
      prefs.put(key, "mysql");
    }
    databaseDialog.setPrefs(prefs);
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

  private void doLoadRegistry() {
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
