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
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceException;
import org.ensembl.mart.lib.config.CompositeDSConfigAdaptor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSConfigAdaptor;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.URLDSConfigAdaptor;
import org.ensembl.mart.shell.MartShellLib;
import org.ensembl.mart.util.AutoFlushOutputStream;
import org.ensembl.mart.util.FileUtil;
import org.ensembl.mart.util.LoggingUtil;
import org.ensembl.mart.util.MaximumBytesInputFilter;
import org.ensembl.mart.util.PollingInputStream;
import org.ensembl.util.ExtensionFileFilter;

// TODO 1 Finish testing filter addition and removal
// TODO selecting an attribute / filter should cause it to be shown in InputPanel
// TODO support boolean_list filter type
// TODO support id list filters
// TODO add user defined size for preconfig buffer

/**
 * Widget for creating, loading, saving and editing Queries.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class QueryEditor extends JPanel {

  private int preconfigLimit    = 1000;
  private int maxPreconfigBytes = 100000;
  
  private AdaptorManager adaptorManager;

  private QueryEditorContext editorManager;

  private static final Logger logger =
    Logger.getLogger(QueryEditor.class.getName());

  /** default percentage of total width allocated to the tree constituent component. */
  private double TREE_WIDTH = 0.27d;

  /** default percentage of total height allocated to the tree constituent component. */
  private double TREE_HEIGHT = 0.7d;

  private Dimension MINIMUM_SIZE = new Dimension(50, 50);

  /** The query part of the model. */
  private Query query;

  private Engine engine = new Engine();

  private JFileChooser mqlFileChooser = new JFileChooser();

  private AdaptorManager datasetPage;
  private String currentDatasetName;
  private OutputSettingsPage outputSettingsPage;

  private AttributePageSetWidget attributesPage;
  private FilterPageSetWidget filtersPage;

  private Option lastDatasetOption;

  private Feedback feedback = new Feedback(this);

  private JFileChooser resultsFileChooser = new JFileChooser();

  private File currentDirectory;

  /** File for temporarily storing results in while this instance exists. */
  private File tmpFile;

  

  private JSplitPane leftAndRight;

  private JSplitPane middleAndBottom;

  private JEditorPane outputPanel;
	private InputPageContainer inputPanelContainer;

  /**
   * 
   * @throws IOException if fails to create temporary results file.
   */
  public QueryEditor(
    QueryEditorContext editorManager,
    AdaptorManager datasetConfigSettings)
    throws IOException {

    this.adaptorManager = datasetConfigSettings;
    this.editorManager = editorManager;
    this.query = new Query();

    QueryTreeView treeConfig = new QueryTreeView(query, datasetConfigSettings.getRootAdaptor());
    inputPanelContainer =
      new InputPageContainer(
        query,
        treeConfig,
        datasetConfigSettings);

    outputPanel = new JEditorPane();
    outputPanel.setEditable(false);

    addWidgets(
      new JScrollPane(treeConfig),
      new JScrollPane(inputPanelContainer),
      new JScrollPane(outputPanel));

    mqlFileChooser.addChoosableFileFilter(
      new ExtensionFileFilter("mql", "MQL Files"));

    tmpFile = File.createTempFile("mart" + System.currentTimeMillis(), ".tmp");
    tmpFile.deleteOnExit();

    // set default working directory
    setCurrentDirectory(new File(System.getProperty("user.home")));

  }

  /**
   * 
   */
  protected void doClose() {
    if ( editorManager!=null ) 
      editorManager.remove(this);
  }

  /**
	 * 
	 */
	private void doDatasetConfigChanged() {
		// TODO Auto-generated method stub
		
	}

	/**
   * 
   */
  public void doLoadQuery() {

    if (getMqlFileChooser().showOpenDialog(this)
      != JFileChooser.APPROVE_OPTION)
      return;

    logger.fine("Previous query: " + query);

    try {

      File f = getMqlFileChooser().getSelectedFile().getAbsoluteFile();
      logger.fine("Loading MQL from file: " + f);

      BufferedReader r = new BufferedReader(new FileReader(f));
      StringBuffer buf = new StringBuffer();
      for (String line = r.readLine(); line != null; line = r.readLine())
        buf.append(line);
      r.close();

      logger.fine("Loaded MQL: " + buf.toString());

      MartShellLib msl = new MartShellLib(adaptorManager.getRootAdaptor());
      setQuery(msl.MQLtoQuery(buf.toString()));
      logger.fine("Loaded Query:" + getQuery());

    } catch (InvalidQueryException e) {
      feedback.warning(e.getMessage());
    } catch (IOException e) {
      feedback.warning(e.getMessage());
    }

  }

  /**
   * Save results to file, user must select file if no output file selected. 
   */
  public void doSaveResults() {
    runQuery(true, false, 0);
  }

  /**
   * Save results to file, user must select output file.
   */
  public void doSaveResultsAs() {

    runQuery(true, true, 0);
  }

  /**
   * convenience method.
   * @param label
   * @param listener
   * @return
   */
  private JButton createButton(
    String label,
    ActionListener listener,
    boolean enabled) {
    JButton b = new JButton(label);
    b.setEnabled(enabled);
    b.addActionListener(listener);
    return b;
  }

  /**
   * Repositions the dividers after the component has been resized to maintain
   * the relative size of the panes.
   */
  private void resizeSplits() {

    // must set divider by explicit values rather than
    // proportions because the proportion approach fails
    // on winxp jre 1.4 when the component is FIRST added.
    // (It does work when the component is resized).
    Dimension size = getParent().getSize();
    int treeWidth = (int) (TREE_WIDTH * size.width);
    int treeHeight = (int) ((1 - TREE_WIDTH) * size.height);
    leftAndRight.setDividerLocation(treeWidth);
    middleAndBottom.setDividerLocation(treeHeight);

    // need to do this so the component is redrawn on win xp jre 1.4
    validate();

  }

  /**
   * Sets the relative positions of the constituent components with splitters
   * where needed. Layout is:
   * <pre>

   * -----------------
   * left   |    right  
   * -----------------
   *      bottom
   * </pre>
   */
  private void addWidgets(
    JComponent left,
    JComponent right,
    JComponent bottom) {

    left.setMinimumSize(MINIMUM_SIZE);
    right.setMinimumSize(MINIMUM_SIZE);
    bottom.setMinimumSize(MINIMUM_SIZE);

    leftAndRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
    leftAndRight.setOneTouchExpandable(true);

    middleAndBottom =
      new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftAndRight, bottom);
    middleAndBottom.setOneTouchExpandable(true);

    // don't use default FlowLayout manager because it won't resize components if
    // QueryEditor is resized.
    setLayout(new BorderLayout());

    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        resizeSplits();
      }

    });
    add(middleAndBottom, BorderLayout.CENTER);

  }


  /**
   * Loads dataset configs from files in classpath for test
   * purposes.
   * @return preloaded dataset configs
   * @throws ConfigurationException
   */
  static DSConfigAdaptor testDSConfigAdaptor(DSConfigAdaptor adaptor) throws ConfigurationException {

    //CompositeDSConfigAdaptor adaptor = new CompositeDSConfigAdaptor();

    String[] urls = new String[] { 
      //"data/XML/hsapiens_gene_est.xml"
      "data/XML/hsapiens_gene_ensembl.xml" 
      //,"data/XML/hsapiens_gene_vega.xml" 
    };
    for (int i = 0; i < urls.length; i++) {
      URL dvURL = QueryEditor.class.getClassLoader().getResource(urls[i]);
      //dont ignore cache, dont validate, dont include hidden members (these are only for MartEditor)
      ((CompositeDSConfigAdaptor)adaptor).add(new URLDSConfigAdaptor(dvURL,false, false, false));
    }

    return adaptor;
  }

  /**
   * @return list of datasources
   */
  static List testDatasources() throws ConfigurationException {
    Vector dss = new Vector();
    dss.add(
    new DetailedDataSource(
        "mysql",
        "ensembldb.ensembl.org",
        "3306",
        "ensembl_mart_17_1",
        "anonymous",
        null,
        10,
        "com.mysql.jdbc.Driver"));
    dss.add(
    new DetailedDataSource(
        "mysql",
        "ensembldb.ensembl.org",
        "3306",
        "ensembl_mart_18_1",
        "anonymous",
        null,
        10,
        "com.mysql.jdbc.Driver"));
    return dss;
  }

  public static void main(String[] args) throws Exception {

    // enable logging messages
    LoggingUtil.setAllRootHandlerLevelsToFinest();
    logger.setLevel(Level.FINEST);

    AdaptorManager dvs = testDatasetConfigSettings();
    final QueryEditor editor = new QueryEditor(null, dvs);
    editor.setName("test_query");

    JFrame f = new JFrame("Query Editor (Test Frame)");
    Box p = Box.createVerticalBox();
    p.add(editor);
    f.getContentPane().add(p);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setSize(950, 750);
    f.setVisible(true);

    // set 1st dsv to save having to do it while testing.
    editor.getQuery().setDatasetConfig((DatasetConfig) dvs.getRootAdaptor().getDatasetConfigs().next());
  }

  /**
   * @return
   */
  public Query getQuery() {
    return query;
  }


  /**
   * Executes query and writes results to preconfig panel, limits number
   * of result rows printed to preconfigLimit.
   *
   */
  public void doPreconfig() {
    runQuery(false, false, preconfigLimit);
  }


  /**
   * Executes query and writes results to temporary file.
   *
   */
  public void doExecute() {
    runQuery(false, false, 0);
  }

  /**
   * Executes query and stores the results in a temporary file. These results 
   * may be stored in a user specified results file.
   * 
   * Part of the temporary file is read and
   * displyed in the preconfig pane. Saving is implemented by copying the tmp
   * file to the file selected by the user.
   * 
   * Threads are used for concurrent writing and reading from the temporary file.
   * 
   * @param save whether results file should be saved
   * @param changeResultsFile if true the results file chooser is displayed
   * @param limit max number of rows in result set
   */
  private void runQuery(final boolean save, final boolean changeResultsFile, final int limit) {

    // Possible OPTIMISATION could write out to a dual outputStream, one outputStream goes
    // to a file, the other is a pipe into the application. This would prevent the
    // need to read from the file system and remove the need for PollingFileInputStream.

    //  user select result file if necessary
    if (save
      && resultsFileChooser.getSelectedFile() == null
      || changeResultsFile) {

      if (resultsFileChooser.getSelectedFile() == null)
        resultsFileChooser.setSelectedFile(new File(getName() + ".mart"));

      int option = resultsFileChooser.showSaveDialog(this);
      if (option != JFileChooser.APPROVE_OPTION)
        return;

    }

    // clear last results set before executing query
    outputPanel.setText("");

    try {

      // We use the PollingInputStream wrapper so that we keep trying to read from
      // file even reach end. Needed because we might read faster than we write to it.
      final PollingInputStream pis =
        new PollingInputStream(new FileInputStream(tmpFile));
      pis.setLive(true);

      // JEditorPane.read(inputStream,...) only displays the contents of the inputStream 
      // once the end of the stream is reached. Because of this, and to prevent the client
      // memory usage becoming extreme for large queries we limit the amount of data
      // to display by closing the inputStream (potentially) prematurely by the using the
      // MaximumBytesInputFilter() wrapper around the inputStream. 
      final InputStream is = new MaximumBytesInputFilter(pis, maxPreconfigBytes);

      new Thread() {

        public void run() {
          execute(limit);
          // copy tmp file to results file if necessary
          try {
            if (save)
              FileUtil.copyFile(tmpFile, resultsFileChooser.getSelectedFile());
          } catch (IOException e) {
            feedback.warning(e);
          }
          // tell the inputStream to stop reading once a -1 has been read
          // from the file because we aren't writing any more.
          pis.setLive(false);
        }
      }
      .start();

      // read results from file on another thread.
      new Thread() {

        public void run() {

          loadPreconfigPanel(is);
        }
      }
      .start();

    } catch (FileNotFoundException e) {
      feedback.warning(e);
    }

  }

  /**
   * Reads all data from is and displays in preconfig panel.
   */
  private void loadPreconfigPanel(InputStream is) {

    try {
      outputPanel.read(is, null);
      is.close();
    } catch (IOException e) {
      feedback.warning(e);
    }
  }

  /**
   * 
   */
  private void execute(final int limit) {

    if (query.getDataSource() == null) {
      feedback.warning("Data base must be set before executing query.");
      return;
    } else if (query.getAttributes().length == 0) {
      feedback.warning("Attributes must be set before executing query.");
      return;
    }

    try {

      // The preconfig pane loading system can't read bytes from tmpFile
      // until they are written to disk so we force the output memory buffer to
      // flush often.
      final OutputStream os =
        new AutoFlushOutputStream(
          new FileOutputStream(tmpFile),
          maxPreconfigBytes);

      
      int oldLimit = query.getLimit();
      if ( limit>0 ) query.setLimit( limit );

      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     
      engine.execute(query, inputPanelContainer.getOutputSettingsPage().getFormat(), os);
      os.close();

      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      
      if ( tmpFile.toURL().openConnection().getContentLength()<1 )
        feedback.warning("Empty result set.");
      
      if ( limit>0 ) query.setLimit( oldLimit );

    } catch (SequenceException e) {
      feedback.warning(e);
    } catch (FormatException e) {
      feedback.warning(e);
    } catch (InvalidQueryException e) {
      feedback.warning(e);
    } catch (SQLException e) {
      feedback.warning(e);
    } catch (IOException e) {
      feedback.warning(e);
    }

  }

  /**
   * Set the name for this widget and the query it contains.
   */
  public void setName(String name) {
    super.setName(name);
    query.setQueryName(name);
  }

  /*
   * @return the name of this widget, this is derived from it's query.name.
   */
  public String getName() {
    return query.getQueryName();
  }

  /**
   * @return mql representation of the current query,
   * or null if datasetConfig unset.
   */
  public String getQueryAsMQL() throws InvalidQueryException {

    String mql = null;
    DatasetConfig datasetConfig = query.getDatasetConfig();
    if (datasetConfig == null)
      throw new InvalidQueryException("DatasetConfig must be selected before query can be converted to MQL.");

    MartShellLib msl = new MartShellLib(null);
    mql = msl.QueryToMQL(query, datasetConfig);

    return mql;
  }

  /**
   * Exports mql to a file chosen by user.
   */
  public void doSaveQuery() {

    try {

      String mql = getQueryAsMQL();

      if (getMqlFileChooser().showSaveDialog(this)
        != JFileChooser.APPROVE_OPTION)
        return;

      File f = getMqlFileChooser().getSelectedFile().getAbsoluteFile();
      FileOutputStream os;
      os = new FileOutputStream(f);
      os.write(mql.getBytes());
      os.write('\n');
      os.close();

    } catch (InvalidQueryException e) {
      feedback.warning(e.getMessage());
    } catch (IOException e) {
      feedback.warning(e.getMessage());
    }
  }

  /**
   * Initialises "current dir" of chooser if no file currently set. 
   */
  private JFileChooser getMqlFileChooser() {

    // Do this here rather than (more simply) in constructor because 
    // that cause an excption to be thrown on linux. Possibly a bug in JVM/library. 

    if (mqlFileChooser.getSelectedFile() == null)
      mqlFileChooser.setCurrentDirectory(currentDirectory);

    return mqlFileChooser;
  }

  /**
   * Initialise this editor with the contents of the specified query.
   * @param query query settings to be used by editor.
   */
  public void setQuery(Query query) {
    this.query.initialise(query);
  }

  /**
   * @return 
   */
  public File getCurrentDirectory() {
    return currentDirectory;
  }

  /**
   * @param directory
   * @throws IllegalArgumentException if directory not exist or is not a real direcory
   */
  public void setCurrentDirectory(File directory) {
    if (!directory.exists())
      throw new IllegalArgumentException("Directory not exist: " + directory);
    if (!directory.isDirectory())
      throw new IllegalArgumentException(
        "File is not a directory: " + directory);
    currentDirectory = directory;
  }

  /**
   * 
   */
  public static AdaptorManager testDatasetConfigSettings() {
    AdaptorManager dvs = new AdaptorManager(false);
    dvs.setAdvancedOptionsEnabled(true);
    try {
      testDSConfigAdaptor(dvs.getRootAdaptor()) ;
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
    return dvs;

  }

	/**
   * When doPreconfig() is called a limit is set on the query with this value.
	 * @return max number of rows in preconfig results.
	 */
	public int getPreconfigLimit() {
		return preconfigLimit;
	}

	/**
	 * Set the maximum number of rows to be displayed during a doPreconfig() cal.
   * @return max number of rows to include in preconfig results pane.
	 */
	public void setPreconfigLimit(int i) {
		preconfigLimit = i;
	}

	/**
	 * 
	 */
	public void openDatasetConfigMenu() {
		inputPanelContainer.openDatasetConfigMenu();
	}

}
