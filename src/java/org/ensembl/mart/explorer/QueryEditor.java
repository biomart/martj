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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceException;
import org.ensembl.mart.lib.config.CompositeDSViewAdaptor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.shell.MartShellLib;
import org.ensembl.mart.util.AutoFlushOutputStream;
import org.ensembl.mart.util.FileUtil;
import org.ensembl.mart.util.LoggingUtil;
import org.ensembl.mart.util.MaximumBytesInputFilter;
import org.ensembl.mart.util.PollingInputStream;
import org.ensembl.util.ExtensionFileFilter;

// TODO delete tmp file when object deleted.
// TODO Support attribute order rearrangment via DnD
// TODO Support attribute / filter removal by DELETE key
// TODO selecting an attribute / filter should cause it to be shown in InputPanel

/**
 * Provides a panel in which a user can create and edit
 * a Query.
 * 
 * <p>
 * The Panel represents the queries made available in the 
 * MartConfiguration object it is initialised with.
 * </p>
 */
public class QueryEditor
  extends JPanel
  implements PropertyChangeListener, TreeSelectionListener {

  private DSViewAdaptor dsViewAdaptor;

  private JSplitPane topAndBottom;

  private JSplitPane top;

  private static final Logger logger =
    Logger.getLogger(QueryEditor.class.getName());

  /** default percentage of total width allocated to the tree constituent component. */
  private double TREE_WIDTH = 0.27d;

  /** default percentage of total height allocated to the tree constituent component. */
  private double TREE_HEIGHT = 0.7d;

  private Dimension MINIMUM_SIZE = new Dimension(50, 50);

  /** DatasetViews defining the "query space" this editor encompasses. */
  private DatasetView[] datasetViews;

  /** The query part of the model. */
  private Query query;

  private Engine engine = new Engine();

  private JFileChooser mqlFileChooser = new JFileChooser();

  private DefaultTreeModel treeModel;
  private DefaultMutableTreeNode rootNode;

  private JToolBar toolBar = new JToolBar();
  private JTree treeView;
  private JPanel inputPanel;
  private JEditorPane outputPanel = new JEditorPane();

  private DatasetWidget datasetPage;
  private String currentDatasetName;
  private OutputSettingsPage outputSettingsPage;

  private AttributePageSetWidget attributesPage;
  private FilterPageSetWidget filtersPage;

  private Option lastDatasetOption;

  /** Maps attributes to the tree node they are represented by. */
  private Map attributeToWidget;

  private Feedback feedback = new Feedback(this);

  private JFileChooser resultsFileChooser = new JFileChooser();

  /** File for temporarily storing results in while this instance exists. */
  private File tmpFile;

  private boolean resultsStale = true;

  private int maxPreviewBytes = 10000;

  /**
   * 
   * @throws IOException if fails to create temporary results file.
   */
  public QueryEditor() throws IOException {

    this.query = new Query();
    this.attributeToWidget = new HashMap();

    query.addPropertyChangeListener(this);

    initToolbar();
    initTree();
    initInputPanel();
    initOutputPanel();

    datasetPage = new DatasetWidget(query);
    addPage(datasetPage);

    layoutPanes();

    mqlFileChooser.addChoosableFileFilter(
      new ExtensionFileFilter("mql", "MQL Files"));

    tmpFile = File.createTempFile("mart" + System.currentTimeMillis(), ".tmp");
    tmpFile.deleteOnExit();
    
  }

  private void initToolbar() {

    toolBar.add(createButton("Execute", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doExecute();
      }
    }));

    toolBar.add(createButton("Save", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doSaveResults();
      }
    }));

    toolBar.add(createButton("Save As", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doSaveResultsAs();
      }
    }));

    toolBar.addSeparator();

    toolBar.add(createButton("Export MQL", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doExportMQL();
      }
    }));

    toolBar.setMaximumSize(toolBar.getSize());

  }

  /**
   * Save results to file, user must select file if no output file selected. 
   */
  private void doSaveResults() {
    runQuery(true, false);
  }

  /**
   * Save results to file, user must select output file.
   */
  private void doSaveResultsAs() {

    runQuery(true, true);
  }

  /**
   * convenience method.
   * @param label
   * @param listener
   * @return
   */
  private JButton createButton(String label, ActionListener listener) {
    JButton b = new JButton(label);
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
    top.setDividerLocation(treeWidth);
    topAndBottom.setDividerLocation(treeHeight);

    //    top.setDividerLocation( TREE_WIDTH );
    //    topAndBottom.setDividerLocation( 1 - TREE_WIDTH );

    // this doesn't actually set the size to the minumum but
    // cause it to resize correctly. 
    //inputPanel.setMinimumSize(MINIMUM_SIZE);
  }

  private void showInputPage(InputPage page) {
    ((CardLayout) (inputPanel.getLayout())).show(inputPanel, page.getName());
  }

  /**
   * Adds page to input panel and tree view.
   * @param page page to be added
   */
  private void addPage(InputPage page) {

    //  Add page to input panel.
    inputPanel.add(page.getName(), page);
    showInputPage(page);

    // Add page's node and show it
    treeModel.insertNodeInto(
      page.getNode(),
      rootNode,
      rootNode.getChildCount());
    TreePath path = new TreePath(rootNode).pathByAddingChild(page.getNode());
    treeView.makeVisible(path);

    treeView.setRootVisible(false);
  }

  /**
   * Sets the relative positions of the constituent components. Layout is:
   * <pre>
   * tree   |    input
   * -----------------
   *     output
   * </pre>
   */
  private void layoutPanes() {

    treeView.setMinimumSize(MINIMUM_SIZE);
    inputPanel.setMinimumSize(MINIMUM_SIZE);
    outputPanel.setMinimumSize(MINIMUM_SIZE);

    top =
      new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        new JScrollPane(treeView),
        inputPanel);
    top.setOneTouchExpandable(true);

    topAndBottom =
      new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        top,
        new JScrollPane(outputPanel));
    topAndBottom.setOneTouchExpandable(true);

    // don't use default FlowLayout manager because it won't resize components if
    // QueryEditor is resized.
    setLayout(new BorderLayout());

    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        resizeSplits();
      }

    });
    add(toolBar, BorderLayout.NORTH);
    add(topAndBottom, BorderLayout.CENTER);

  }

  /**
   * 
   */
  private void initOutputPanel() {
    outputPanel.setEditable(false);
  }

  /**
   * 
   */
  private void initInputPanel() {
    inputPanel = new JPanel();
    inputPanel.setLayout(new CardLayout());
    inputPanel.add("input", new JLabel("input panel"));
  }

  /**
   * 
   */
  private void initTree() {
    rootNode = new DefaultMutableTreeNode("Query");
    treeModel = new DefaultTreeModel(rootNode);
    treeView = new JTree(treeModel);
    treeView.addTreeSelectionListener(this);
  }

  /**
   * Opens the dataset option tree if it is available, otherwise does
   * nothing. Calling this methid saves the user having to open the 
   * option list manually.
   */
  public void showDatasetOptions() {
    if (datasetPage != null)
      datasetPage.showTree();
  }

  /**
   * Loads dataset views from files in classpath for test
   * purposes.
   * @return preloaded dataset views
   * @throws ConfigurationException
   */
  static DatasetView[] testDSViews() throws ConfigurationException {
    CompositeDSViewAdaptor adaptor = new CompositeDSViewAdaptor();

    String[] urls =
      new String[] {
        "data/XML/homo_sapiens__ensembl_genes.xml",
        "data/XML/homo_sapiens__snps.xml",
        "data/XML/homo_sapiens__vega_genes.xml" };
    for (int i = 0; i < urls.length; i++) {
      URL dvURL = QueryEditor.class.getClassLoader().getResource(urls[i]);
      adaptor.add(new URLDSViewAdaptor(dvURL, true));
      logger.info("Using DatasetView: " + dvURL);
    }

    DatasetView[] views = adaptor.getDatasetViews();

    DataSource ds = DatabaseUtil.createDataSource("mysql",
      //"127.0.0.1",
    //"3313",
  "ensembldb.ensembl.org", "3306", "ensembl_mart_17_1",
      //"ensro",
  "anonymous", null, 10, "com.mysql.jdbc.Driver");

    for (int i = 0, n = views.length; i < n; i++) {
      views[i].setDatasource(ds);
    }

    return views;
  }

  public static void main(String[] args) throws Exception {

    // enable logging messages
    LoggingUtil.setAllRootHandlerLevelsToFinest();
    logger.setLevel(Level.FINEST);

    DatasetView[] views = testDSViews();

    final QueryEditor editor = new QueryEditor();
    editor.setName("test_query");
    editor.setDatasetViews(views);

    JButton executeButton = new JButton("Execute");
    executeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          editor.doExecute();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    JFrame f = new JFrame("Query Editor (Test Frame)");
    Box p = Box.createVerticalBox();
    p.add(editor);
    p.add(executeButton);
    f.getContentPane().add(p);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setSize(950, 750);
    f.setVisible(true);

    // preselect a view
    editor.datasetPage.setDatasetViewByDisplayName(views[0].getDisplayName());

    // To be called by using program to open tree ready for user to choose
    // dataset. Saves a click.
    //editor.showDatasetOptions();

    // TODO support programmatically selecting homosapiens ensembl gene option
    //   Option o = config.getLayout().getOptionByInternalName("homo_sapiens").getOptionByInternalName("homo_sapiens_ensembl_genes");
    //    editor.datasetPage.setOption( o );

  }

  /**
   * Redraws the tree if there are any property changes in the Query.
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {

    String propertyName = evt.getPropertyName();
    Object newValue = evt.getNewValue();
    Object oldValue = evt.getOldValue();

    if (evt.getSource() == query) {

      if ("datasetInternalName".equals(propertyName)) {

        //        if ( newValue!=null )
        datasetChanged(datasetPage.getDatasetView());
        //        else
        //          reset();

      } else if ("dataSource".equals(propertyName)) {

        // no nothing because datasource is not displayed in 
        // tree

      } else if ("star_bases".equals(propertyName)) {

        // ignore this change because not represented in tree,
        // star_bases will be removed in future

      } else if ("primary_keys".equals(propertyName)) {

        // ignore this change because not represented in tree,
        // primary_keys will be removed in future

      } else if ("attribute".equals(propertyName)) {

        if (newValue != null && oldValue == null)
          insertNode(
            attributesPage.getNode(),
            ((InputPageAware) newValue).getInputPage().getNode());

        else if (newValue == null && oldValue != null)
          treeModel.removeNodeFromParent(
            ((InputPageAware) oldValue).getInputPage().getNode());

      } else if ("filter".equals(propertyName)) {

        if (newValue != null && oldValue == null) {
          insertNode(
            filtersPage.getNode(),
            ((InputPageAware) newValue).getInputPage().getNode());
        } else if (newValue == null && oldValue != null)
          treeModel.removeNodeFromParent(
            ((InputPageAware) oldValue).getInputPage().getNode());

      } else {
        logger.warning("Unrecognised propertyChange: " + propertyName);
      }

      Enumeration enum = rootNode.breadthFirstEnumeration();
      while (enum.hasMoreElements())
        treeModel.nodeChanged((TreeNode) enum.nextElement());

    }

    if (evt.getSource() == outputSettingsPage) {
      treeModel.nodeChanged(outputSettingsPage.getNode());
    }

    resultsStale = true;

  }

  /**
   * Inserts child node under parent in tree and select it.
   * @param parent
   * @param child
   */
  private void insertNode(MutableTreeNode parent, MutableTreeNode child) {

    treeModel.insertNodeInto(child, parent, parent.getChildCount());

    // make node selected in tree
    treeView.setSelectionPath(
      new TreePath(rootNode).pathByAddingChild(parent).pathByAddingChild(
        child));

  }

  /**
   * Update the model (query) and the view (tree and inputPanels).
   */
  void datasetChanged(DatasetView dataset) {

    treeModel.nodeChanged(datasetPage.getNode());

    // Basically we remove most things from the model and views before adding those things that should
    // be present back in.

    // Update the model
    query.removeAllAttributes();
    query.removeAllFilters();

    // enables soon to be obsolete listeners to be garbage collected once they are removed from the views.
    query.removeAllPropertyChangeListeners();

    //  but we still need to listen to changes keep tree up to date
    query.addPropertyChangeListener(this);

    // We need to remove all pages from the inputPanel so that we can add pages with the same
    // name again later. e.g. attributesPage.
    inputPanel.removeAll();

    // Remove all nodes from the tree then add the datasetPage. Removing
    // the other nodes one at a time using treeModel.removeNodeFromParent( XXX.getNode() ) 
    // failed to work on jdk1.4.1@linux. 
    rootNode.removeAllChildren();
    addPage(datasetPage);
    treeModel.reload();

    if (dataset != null) {

      addAttributePages(dataset);
      addFilterPages(dataset);
      addOutputPage();

      // select the attributes page
      treeView.setSelectionPath(
        new TreePath(rootNode).pathByAddingChild(attributesPage.getNode()));

    }
  }

  /**
   * Adds output page to tree and InputPanel.
   */
  private void addOutputPage() {
    outputSettingsPage = new OutputSettingsPage();
    outputSettingsPage.addPropertyChangeListener(this);
    addPage(outputSettingsPage);
  }

  /**
   * 
   */
  private void addFilterPages(DatasetView dataset) {
    filtersPage = new FilterPageSetWidget(query, dataset);
    addPage(filtersPage);
  }

  /**
   * Creates the attribute pages and various maps that are useful 
   * for relating nodes, pages and attributes.
   */
  private void addAttributePages(DatasetView dataset) {

    attributesPage = new AttributePageSetWidget(query, dataset);
    //    List list = attributesPage.getLeafWidgets();
    //    AttributeDescriptionWidget[] attributePages = (AttributeDescriptionWidget[]) list.toArray(new AttributeDescriptionWidget[list.size()]);
    //    for (int i = 0; i < attributePages.length; i++) {
    //      AttributeDescriptionWidget w = attributePages[i];
    //      Attribute a = w.getAttribute();
    //      attributeFieldNameToPage.put( a.getField(), w );
    //      attributeToWidget.put( a, w );
    //    }

    addPage(attributesPage);

  }

  /**
   * Show input page corresponding to selected tree node. 
   */
  public void valueChanged(TreeSelectionEvent e) {
    if (e.getNewLeadSelectionPath() != null
      && e.getNewLeadSelectionPath().getLastPathComponent() != null) {

      DefaultMutableTreeNode node =
        (DefaultMutableTreeNode) e
          .getNewLeadSelectionPath()
          .getLastPathComponent();

      if (node.getUserObject() instanceof InputPage) {

        InputPage page = (InputPage) node.getUserObject();
        showInputPage(page);

      }
    }
  }

  /**
   * @return
   */
  public DatasetView[] getDatasetViews() {
    return datasetViews;
  }

  /**
   * @param views
   */
  public void setDatasetViews(DatasetView[] views) {
    datasetViews = views;
    datasetPage.setDatasetViews(datasetViews);
  }

  /**
   * @return
   */
  public Query getQuery() {
    return query;
  }

  /**
   * Executes query and writes results to temporary file.
   *
   */
  public void doExecute() {
    runQuery(false, false);
  }

  /**
   * Executes query and stores the results in a temporary file. These results 
   * may be stored in a user specified results file.
   * 
   * Part of the temporary file is read and
   * displyed in the preview pane. Saving is implemented by copying the tmp
   * file to the file selected by the user.
   * 
   * Threads are used for concurrent writing and reading from the temporary file.
   * 
   * @param save whether results file should be saved
   * @param changeResultsFile if true the results file chooser is displayed
   */
  private void runQuery(final boolean save, final boolean changeResultsFile) {

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
      final InputStream is = new MaximumBytesInputFilter(pis, maxPreviewBytes);

      new Thread() {

        public void run() {
          execute();
          // copy tmp file to results file if necessary
          try {
            if (save)
              FileUtil.copyFile(tmpFile, resultsFileChooser.getSelectedFile());
          } catch (IOException e) {
            feedback.warn(e);
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

          loadPreviewPanel(is);
        }
      }
      .start();

    } catch (FileNotFoundException e) {
      feedback.warn(e);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * Reads all data from is and displays in preview panel.
   */
  private void loadPreviewPanel(InputStream is) {

    try {
      outputPanel.read(is, null);
      is.close();
    } catch (IOException e) {
      feedback.warn(e);
    }
  }

  /**
   * 
   */
  protected void execute() {

    if (query.getDatasetInternalName() == null) {
      feedback.warn("Dataset view must be set before executing query.");
      return;
    } else if (query.getDataSource() == null) {
      feedback.warn("Data base must be set before executing query.");
      return;
    } else if (query.getAttributes().length == 0) {
      feedback.warn("Attributes must be set before executing query.");
      return;
    } // TODO check output format set query.
    

    if (!resultsStale)
      return;

    try {

      // The preview pane loading system can't read bytes from tmpFile
      // until they are written to disk so we force the output memory buffer to
      // flush often.
      final OutputStream os =
        new AutoFlushOutputStream(
          new FileOutputStream(tmpFile),
          maxPreviewBytes);

      engine.execute(query, FormatSpec.TABSEPARATEDFORMAT, os);
      os.close();
      resultsStale = false;

    } catch (SequenceException e) {
      feedback.warn(e);
    } catch (FormatException e) {
      feedback.warn(e);
    } catch (InvalidQueryException e) {
      feedback.warn(e);
    } catch (SQLException e) {
      feedback.warn(e);
    } catch (IOException e) {
      feedback.warn(e);
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
   * or null if datasetView unset.
   */
  public String getMQL() throws InvalidQueryException {

    String mql = null;
    DatasetView datasetView = datasetPage.getDatasetView();
    if (datasetView == null)
      throw new InvalidQueryException("DatasetView must be selected before query can be converted to MQL.");

    MartShellLib msl = new MartShellLib(null);
    mql = msl.QueryToMQL(query, datasetView);

    return mql;
  }

  /**
   * Exports mql to a file chosen by user.
   */
  public void doExportMQL() {

    try {

      if (mqlFileChooser.getSelectedFile() == null) {
        mqlFileChooser.setCurrentDirectory(
          new File(System.getProperty("user.home")));
        mqlFileChooser.setSelectedFile(new File(query.getQueryName() + ".mql"));
      }

      String mql = getMQL();

      if (mqlFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        return;

      File f = mqlFileChooser.getSelectedFile().getAbsoluteFile();
      FileOutputStream os;
      os = new FileOutputStream(f);
      os.write(mql.getBytes());
      os.write('\n');
      os.close();
    } catch (InvalidQueryException e) {
      feedback.warn(e.getMessage());
    } catch (IOException e) {
      feedback.warn(e.getMessage());
    }
  }


}
