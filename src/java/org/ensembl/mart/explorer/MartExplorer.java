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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.util.LoggingUtil;
import org.ensembl.util.SystemUtil;

/**
 * MartExplorer is a graphical application that enables a 
 * user to construct queries and execute them against Mart databases.
 * 
 * <p>
 * A default registry containing several marts is loaded at
 * startup. The user can provide her own initialisation file
 * to replace the supplied one. The file should be 
 * called <b>.martj_adaptors.xml</b> and placed it in her 
 * home directory.
 * </p>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartExplorer extends JFrame implements QueryEditorContext {

  // TODO test save/load query

  // TODO chained queries
  // TODO user resolve datasetConfig name space clashes

  // TODO support user renaming queries 

  // TODO clone query

  // TODO Query | Rename

  private Logger logger = Logger.getLogger(MartExplorer.class.getName());

  /**
   * Name of the initial registry file to be loaded at startup.
   * This file should be placed in the user's home directory.
   * If the file does not exist then a default file is loaded
   * instead. 
   */
  public final static String REGISTRY_FILE_NAME = ".martj_adaptors.xml";

  /**
   * Default registry file loaded at startup if none
   * is found in the user's home directory.
   */
  private final static String DEFAULT_REGISTRY_URL = "data/defaultRegistry.xml";

  private static final String IMAGE_DIR = "data/image";

  private AdaptorManager adaptorManager = new AdaptorManager();

  private final static String TITLE = " Mart Explorer (Development version)";

  private static final Dimension PREFERRED_SIZE = new Dimension(1024, 768);

  /** Currently available datasets. */
  private List datasetConfigs = new ArrayList();

  /** Currently available databases. */
  private List databaseDSConfigAdaptors = new ArrayList();

  private JTabbedPane tabs = new JTabbedPane();

  /** Persistent preferences object used to hold user history. */
  private Preferences prefs = Preferences.userNodeForPackage(this.getClass());

  private Feedback feedback = new Feedback(this);

  private Help help = new Help();

  public static void main(String[] args) throws ConfigurationException {

    // enable logging messages
    //    LoggingUtil.setAllRootHandlerLevelsToFinest();
    //    Logger.getLogger(Query.class.getName()).setLevel(Level.FINE);

    if (!LoggingUtil.isLoggingConfigFileSet())
      Logger.getLogger("org.ensembl.mart").setLevel(Level.FINE);
    MartExplorer me = new MartExplorer();
    me.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    me.setVisible(true);

    me.loadDefaultAdaptors();

    if (me.getNumDatasetConfigsAvailable() > 0)
      me.doNewQuery();
  }

  public MartExplorer() {

    super(TITLE);

    createUI();

  }

  /**
   * 
   */
  private void loadDefaultAdaptors() {

    disableCursor();

    URL url = getClass().getClassLoader().getResource(DEFAULT_REGISTRY_URL);

    String path =
      System.getProperty("user.home") + File.separator + REGISTRY_FILE_NAME;
    File file = new File(path);
    if (file.exists())
      try {
        url = file.toURL();
      } catch (MalformedURLException e) {
        feedback.warning(e);
      }
    logger.fine("Loading default registry file: " + url);
    adaptorManager.importRegistry(url);

    enableCursor();
  }

  /**
   * 
   */
  private void createUI() {
    setJMenuBar(createMenuBar());
    getContentPane().add(createToolBar(), BorderLayout.NORTH);
    getContentPane().add(tabs, BorderLayout.CENTER);
    setSize(PREFERRED_SIZE);

    // add glass pane so we can "disable" cursor
    JPanel p = new JPanel();
    p.setOpaque(false);
    p.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        Toolkit.getDefaultToolkit().beep();
      }
    });
    setGlassPane(p);

  }

  /**
   * TODO Displays "about" dialog.
   *
   */
  public void doAbout() {
  }

  /**
   * Adds component to application as a tabbed pane. The tab's
   * name id component.getName().
   */
  private void addQueryEditor(JComponent component) {
    tabs.add(component.getName(), component);
  }

  /**
   * 
   * @return "Query_INDEX" where INDEX is the next highest
   * unique number used in the tab names.
   */
  private String nextQueryBuilderTabLabel() {

    int next = 1;

    int n = tabs.getTabCount();
    Pattern p = Pattern.compile("Query_(\\d+)");
    for (int i = 0; i < n; i++) {
      String title = tabs.getTitleAt(i);
      Matcher m = p.matcher(title);
      if (m.matches()) {
        int tmp = Integer.parseInt(m.group(1)) + 1;
        if (tmp > next)
          next = tmp;
      }
    }

    return "Query_" + next;
  }

  private Action newQueryAction =
    new AbstractAction("New Query", createImageIcon("new.gif")) {
    public void actionPerformed(ActionEvent event) {
      doNewQuery();
    }
  };

  private Action saveAction = new AbstractAction("Save", null) {
    public void actionPerformed(ActionEvent event) {
      doSave();
    }
  };

  private Action executeAction =
    new AbstractAction("Execute Query", createImageIcon("run.gif")) {
    public void actionPerformed(ActionEvent event) {
      doPreconfig();
    }
  };

  private Action saveResultsAction =
    new AbstractAction("Save Results", createImageIcon("save.gif")) {
    public void actionPerformed(ActionEvent event) {
      if (isQueryEditorSelected())
        getSelectedQueryEditor().doSaveResults();
    }
  };

  private JToolBar createToolBar() {
    JToolBar tb = new JToolBar();
    tb.add(new ExtendedButton(newQueryAction, "Create a New Query"));
    tb.add(new ExtendedButton(saveResultsAction, "Save Results to file"));
    tb.addSeparator();
    tb.add(new ExtendedButton(executeAction, "Execute Query"));
    return tb;
  }

  /**
   * @return
   */
  private JMenuBar createMenuBar() {

    JMenu query = new JMenu("File");

    JMenuItem newQuery = new JMenuItem(newQueryAction);
    query.add(newQuery).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));

    JMenuItem open = new JMenuItem("Open Query");
    open.setEnabled(false);
    open.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doLoadQueryFromMQL();
      }

    });
    query.add(open).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));

    JMenuItem saveQueryAsMQL = new JMenuItem("Save Query as MQL");
    saveQueryAsMQL.setEnabled(false);
    query.add(saveQueryAsMQL);

    JMenuItem saveQueryAsSQL = new JMenuItem("Save Query as SQL");
    saveQueryAsSQL.setEnabled(false);
    query.add(saveQueryAsSQL);

    JMenuItem close = new JMenuItem("Close Query");
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doRemoveQuery();
      }
    });
    query.add(close).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK));

    JMenuItem closeAll = new JMenuItem("Close All Queries");
    closeAll.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        while (tabs.getTabCount() > 0)
          doRemoveQuery();
      }
    });

    query.add(closeAll);

    JMenuItem execute = new JMenuItem(executeAction);
    query.add(execute).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK));

    query.addSeparator();

    JMenuItem saveResults = new JMenuItem(saveResultsAction);
    query.add(saveResults).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));

    JMenuItem saveResultsAs = new JMenuItem("Save Results As");
    saveResultsAs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        if (isQueryEditorSelected())
          getSelectedQueryEditor().doSaveResultsAs();
      }

    });

    query.add(saveResultsAs);

    query.addSeparator();

    JMenuItem exit_explorer = new JMenuItem("Quit");
    exit_explorer.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doExit();
      }

    });

    query.add(exit_explorer).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));

    JMenu settings = new JMenu("Settings");

    JMenuItem adaptors = new JMenuItem("Adaptors");
    settings.add(adaptors).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK));
    adaptors.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doDatasetConfigSettings();
      }
    });

    final JCheckBox advanced = new JCheckBox("Enable Advanced Options");
    advanced.setToolTipText(
      "Enables optional DatasetConfigs, ability to change dataset name and datasource.");
    advanced.setSelected(adaptorManager.isAdvancedOptionsEnabled());
    settings.add(advanced);
    advanced.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        adaptorManager.setAdvancedOptionsEnabled(advanced.isSelected());
      }
    });

    JMenuItem reset = new JMenuItem("Reset");
    reset.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          adaptorManager.reset();
          prefs.clear();
          loadDefaultAdaptors();
          advanced.setSelected(adaptorManager.isAdvancedOptionsEnabled());
          adaptorManager.clearCache();
        } catch (BackingStoreException e1) {
          feedback.warning(e1);
        }
      }
    });
    settings.add(reset);

    JMenu help = new JMenu("Help");
    JMenuItem about = new JMenuItem("About");
    about.setEnabled(false);
    about.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doAbout();
      }

    });
    help.add(about);

    JMenuItem docs = new JMenuItem("Documentation");
    docs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doDocumentation();
      }

    });
    help.add(docs);

    /**
    
    JMenu exit = new JMenu("Exit");
    JMenuItem exit_explorer = new JMenuItem("Exit Mart Explorer");
    exit_explorer.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doExit();
      }
    
    });
    
      exit.add(exit_explorer).setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
    **/

    JMenuBar all = new JMenuBar();
    all.add(query);
    all.add(settings);
    all.add(help);
    return all;
  }

  protected void doDocumentation() {
    help.showDialog(this);
  }

  protected void doSave() {
    if (isQueryEditorSelected())
      getSelectedQueryEditor().doSaveQuery();

  }

  /**
   * 
   */
  protected void doPreconfig() {
    if (isQueryEditorSelected())
      getSelectedQueryEditor().doPreconfig();
  }

  /**
   * 
   */
  protected void doDatasetConfigSettings() {
    adaptorManager.showDialog(this);

  }

  /**
   * Delete currently selected QueryBuilder from tabbed pane if one is 
   * selected.
   */
  protected void doRemoveQuery() {
    remove((QueryEditor) tabs.getSelectedComponent());
  }

  /**
     * 
     */
  public void doLoadQueryFromMQL() {
    QueryEditor qe = null;
    try {
      qe = new QueryEditor(this, adaptorManager);
      addQueryEditor(qe);
      qe.doLoadQuery();
    } catch (IOException e) {
      feedback.warning(e);
      if (qe != null)
        tabs.remove(qe);
    }
  }

  /**
   * Exits the programs.
   */
  public void doExit() {
    System.exit(0);
  }

  /**
     * 
     */
  public void doNewQuery() {

    try {

      if (getNumDatasetConfigsAvailable() == 0) {

        feedback.warning(
          "You need to add an "
            + "adaptor containing dataset configs before you can create a query.");

      } else {

        try {
          
          logger.finer("BEFORE New query: " + SystemUtil.memoryStatus());
          
          disableCursor();
          final QueryEditor qe = new QueryEditor(this, adaptorManager);
          qe.setName(nextQueryBuilderTabLabel());
          addQueryEditor(qe);
          tabs.setSelectedComponent(qe);
          qe.openDatasetConfigMenu();
        } finally {
          enableCursor();
        }
      }
    } catch (IOException e) {
      feedback.warning(e);
    }
  }

  /**
   * Disables the cursor whilst this runs.
   * @return number of dataset configs currently available.
   */
  public int getNumDatasetConfigsAvailable() {

    int n = 0;    
    try{
      disableCursor();
      n = adaptorManager.getRootAdaptor().getNumDatasetConfigs();
    } finally {
      enableCursor();
    }
    
    return n;
  }

  private void enableCursor() {
    setCursor(Cursor.getDefaultCursor());
    getGlassPane().setVisible(false);
  }

  private void disableCursor() {
    getGlassPane().setVisible(true);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  /**
   * @see org.ensembl.mart.explorer.QueryEditorManager#remove(org.ensembl.mart.explorer.QueryEditor)
   */
  public void remove(QueryEditor editor) {
    tabs.remove(editor);

  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.explorer.QueryEditorManager#getQueryEditors()
   */
  public QueryEditor[] getQueryEditors() {
    // TODO Auto-generated method stub
    return null;
  }

  private QueryEditor getSelectedQueryEditor() {
    return (QueryEditor) tabs.getSelectedComponent();
  }

  private boolean isQueryEditorSelected() {
    return getSelectedQueryEditor() != null;
  }

  /** Returns an ImageIcon, or null if the path was invalid. */
  private ImageIcon createImageIcon(String filename) {
    String path = IMAGE_DIR + "/" + filename;
    URL imgURL = getClass().getClassLoader().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

}
