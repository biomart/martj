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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryChangeListener;
import org.ensembl.mart.lib.SequenceDescription;
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

// TODO selecting an attribute / filter should cause it to be shown in InputPanel
// TODO support boolean_list filter type
// TODO user change q.dataSource, select from list [fire query at different databases]
// TODO support id list filters
// TODO add user defined size for preview buffer

/**
 * Widget for creating, loading, saving and editing Queries.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class QueryEditor extends JPanel {

	/**
	 * An instance of this class is used to determine if the tmp results file
	 * is up tdate with regard to the query. stale = true if the query changes
	 * after the listener is addded.
	 */
	private class ResultsStatus implements QueryChangeListener {

		private boolean stale = true;

		public ResultsStatus() {
		}

		public void queryNameChanged(
			Query sourceQuery,
			String oldName,
			String newName) {
		}

		public void datasetChanged(
			Query sourceQuery,
			String oldDatasetInternalName,
			String newDatasetInternalName) {
			stale = true;
		}

		public void datasourceChanged(
			Query sourceQuery,
			DataSource oldDatasource,
			DataSource newDatasource) {
			stale = true;
		}

		public void attributeAdded(
			Query sourceQuery,
			int index,
			Attribute attribute) {
			stale = true;
		}

		public void attributeRemoved(
			Query sourceQuery,
			int index,
			Attribute attribute) {
			stale = true;
		}

		public void filterAdded(Query sourceQuery, int index, Filter filter) {
			stale = true;
		}

		public void filterRemoved(
			Query sourceQuery,
			int index,
			Filter filter) {
			stale = true;
		}

		public void filterChanged(
			Query sourceQuery,
			Filter oldFilter,
			Filter newFilter) {
			stale = true;
		}

		public void sequenceDescriptionChanged(
			Query sourceQuery,
			SequenceDescription oldSequenceDescription,
			SequenceDescription newSequenceDescription) {
			stale = true;
		}

		public void limitChanged(Query query, int oldLimit, int newLimit) {
			stale = true;
		}

		public void starBasesChanged(
			Query sourceQuery,
			String[] oldStarBases,
			String[] newStarBases) {
			stale = true;
		}

		public void primaryKeysChanged(
			Query sourceQuery,
			String[] oldPrimaryKeys,
			String[] newPrimaryKeys) {
			stale = true;
		}

		public void setStale(boolean v) {
			this.stale = v;
		}

		public boolean isStale() {
			return stale;
		}

    /* (non-Javadoc)
     * @see org.ensembl.mart.lib.QueryChangeListener#queryDatasetViewChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.config.DatasetView, org.ensembl.mart.lib.config.DatasetView)
     */
    public void datasetViewChanged(Query query, DatasetView oldDatasetView, DatasetView newDatasetView) {
      // TODO Auto-generated method stub
      
    }
	}

	/** DatasetViewAdaptor defines the "query space" of available dataset views. */
	private DSViewAdaptor datasetViewAdaptor;

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

	private DatasetViewWidget datasetPage;
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

	private ResultsStatus resultsStatus = new ResultsStatus();

	private int maxPreviewBytes = 10000;

	private JSplitPane leftAndRight;

	private JSplitPane middleAndBottom;

	private JEditorPane outputPanel;

	/**
	 * 
	 * @throws IOException if fails to create temporary results file.
	 */
	public QueryEditor(DSViewAdaptor datasetViewAdaptor) throws IOException {

		this.datasetViewAdaptor = datasetViewAdaptor;
		this.query = new Query();
		this.query.addQueryChangeListener(resultsStatus);

		JComponent toolBar = createToolbar();
		QueryTreeView treeView = new QueryTreeView(query, datasetViewAdaptor);
    InputPageContainer inputPanelContainer = new InputPageContainer(query, datasetViewAdaptor);
    treeView.addTreeSelectionListener(inputPanelContainer);
		outputPanel = new JEditorPane();
		outputPanel.setEditable(false);

		addWidgets(
			new JScrollPane(toolBar),
			new JScrollPane(treeView),
			new JScrollPane(inputPanelContainer),
			new JScrollPane(outputPanel));

		mqlFileChooser.addChoosableFileFilter(
			new ExtensionFileFilter("mql", "MQL Files"));

		tmpFile = File.createTempFile("mart" + System.currentTimeMillis(), ".tmp");
		tmpFile.deleteOnExit();

		// set default working directory
		setCurrentDirectory(new File(System.getProperty("user.home")));

	}

	private JComponent createToolbar() {

		Box toolBar = Box.createHorizontalBox();
		toolBar.add(new JLabel("Query"));

		int gap = 5;
		toolBar.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));

		toolBar.add(createButton("Execute", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doExecute();
			}
		}, true));

		toolBar.add(createButton("Load", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doLoadQuery();
			}
		}, true));

		toolBar.add(createButton("Save", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSaveQuery();
			}
		}, true));

		toolBar.add(Box.createHorizontalStrut(20));

		toolBar.add(new JLabel("Results"));

		toolBar.add(createButton("Save", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSaveResults();
			}
		}, true));

		toolBar.add(createButton("Save As", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSaveResultsAs();
			}
		}, true));
		return toolBar;
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

			MartShellLib msl = new MartShellLib(datasetViewAdaptor);
			setQuery(msl.MQLtoQuery(buf.toString()));
			logger.fine("Loaded Query:" + getQuery());

		} catch (InvalidQueryException e) {
			feedback.warn(e.getMessage());
		} catch (IOException e) {
			feedback.warn(e.getMessage());
		}

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
	 *       top
	 * -----------------
	 * left   |    right  
	 * -----------------
	 *      bottom
	 * </pre>
	 */
	private void addWidgets(
		JComponent top,
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
		add(top, BorderLayout.NORTH);
		add(middleAndBottom, BorderLayout.CENTER);

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
	static DSViewAdaptor testDSViewAdaptor() throws ConfigurationException {

		CompositeDSViewAdaptor adaptor = new CompositeDSViewAdaptor();

		String[] urls = new String[] { "data/XML/homo_sapiens__ensembl_genes.xml"
			//,"data/XML/homo_sapiens__snps.xml"
			//,"data/XML/homo_sapiens__vega_genes.xml" 
		};
		for (int i = 0; i < urls.length; i++) {
			URL dvURL = QueryEditor.class.getClassLoader().getResource(urls[i]);
			adaptor.add(new URLDSViewAdaptor(dvURL, true));
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

		return adaptor;
	}

	public static void main(String[] args) throws Exception {

		// enable logging messages
		LoggingUtil.setAllRootHandlerLevelsToFinest();
		logger.setLevel(Level.FINEST);

		DatasetView[] views = null;

		final QueryEditor editor = new QueryEditor(testDSViewAdaptor());
		editor.setName("test_query");

		JFrame f = new JFrame("Query Editor (Test Frame)");
		Box p = Box.createVerticalBox();
		p.add(editor);
		f.getContentPane().add(p);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(950, 750);
		f.setVisible(true);

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
	private void execute() {

		if (query.getDataSource() == null) {
			feedback.warn("Data base must be set before executing query.");
			return;
		} else if (query.getAttributes().length == 0) {
			feedback.warn("Attributes must be set before executing query.");
			return;
		}

		// TODO check output format set query.

		if (!resultsStatus.isStale())
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
			resultsStatus.setStale(false);

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
	public String getQueryAsMQL() throws InvalidQueryException {

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
			feedback.warn(e.getMessage());
		} catch (IOException e) {
			feedback.warn(e.getMessage());
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
	 * @return
	 */
	public DSViewAdaptor getDatasetViewAdaptor() {
		return datasetViewAdaptor;
	}

}
