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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.util.LoggingUtil;

/**
 * MartExplorer is a graphical application that enables a 
 * user to construct queries and execute them against Mart databases.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartExplorer extends JFrame {

	/*
	  jdbc driver [mysql default, add, remove]
	  database
	  dataset (view)
	  a+f+os
	 */

	// TODO about dialog

	// TODO list datasources
	// TODO delete datasource

	// TODO load query
	// TODO save query

	// TODO manage datatabases: list, remove

	// TODO chained queries
	// TODO user resolve datasetView name space clashes
	// TODO support multiple, user added, jdbc drivers [initDatabaseSettings()]

	// TODO support user renaming queries 

	// TODO clone query
	// TODO load registry file

	private MartSettings martManager = new MartSettings();
  private DatasetViewSettings datasetViewSettings = new DatasetViewSettings();

	private Logger logger = Logger.getLogger(MartExplorer.class.getName());

	private final static String TITLE =
		" MartExplorer(Developement version- incomplete and unstable)";

	private static final Dimension PREFERRED_SIZE = new Dimension(1024, 768);

	/** Currently available datasets. */
	private List datasetViews = new ArrayList();

	private DSViewAdaptor dsvAdaptor;

	/** Currently available databases. */
	private List databaseDSViewAdaptors = new ArrayList();

	/** Currently visible QueryEditors **/
	private List queryEditors = new ArrayList();

	private JTabbedPane queryEditorTabbedPane = new JTabbedPane();

	private DatabaseSettingsDialog databaseDialog;

	/** Persistent preferences object used to hold user history. */
	private Preferences prefs;

	public DatasetView[] getDatasetViews() throws ConfigurationException {
		return dsvAdaptor.getDatasetViews();
	}

	private Feedback feedback = new Feedback(this);

	public static void main(String[] args) throws ConfigurationException {

		if (!LoggingUtil.isLoggingConfigFileSet())
			Logger.getLogger("org.ensembl.mart").setLevel(Level.WARNING);
		MartExplorer me = new MartExplorer();
		me.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		me.setVisible(true);

		// test mode preloads datasets and sets up a query ready to use.
		if (true) {

//			me.addDataSource(
//				DatabaseUtil.createDataSource(
//					"mysql",
//					"127.0.0.1",
//					"3313",
//					"ensembl_mart_17_1",
//					"ensro",
//					null,
//					10,
//					"com.mysql.jdbc.Driver"));
      me.setDsvAdaptor(QueryEditor.testDSViewAdaptor());
      me.doNewQuery();
      
		} 
  }

	public MartExplorer() {

		super(TITLE);

		setJMenuBar(createMenuBar());
		getContentPane().add(queryEditorTabbedPane);
		setSize(PREFERRED_SIZE);

	}

	public void doAbout() {
		// TODO Auto-generated method stub

	}

	/**
	 * Adds component to application as a tabbed pane. The tab's
	 * name id component.getName().
	 */
	private void addQueryEditor(JComponent component) {
		queryEditorTabbedPane.add(component.getName(), component);
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

		JMenuItem importRegistry = new JMenuItem("Import registry file");
		importRegistry.setEnabled(false);
		file.add(importRegistry);

		JMenuItem exportRegistry = new JMenuItem("Export registry file");
		exportRegistry.setEnabled(false);
		file.add(exportRegistry);
		file.addSeparator();
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doExit();
			}

		});
		file.add(exit);

		JMenu settings = new JMenu("Settings");

		JMenuItem datasources = new JMenuItem("Datasources");
		settings.add(datasources);
		datasources.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doDatasourceSettings();
			}
		});

		JMenuItem datasetviews = new JMenuItem("DatasetViews");
    settings.add( datasetviews );
		datasetviews.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doDatasetViewSettings();
			}
		});

		JMenu query = new JMenu("Query");
		JMenuItem newQuery = new JMenuItem("New Query");
		newQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doNewQuery();
			}
		});
		query.add(newQuery);

		JMenuItem newVirtualQuery = new JMenuItem("New Virtual Query");
		newVirtualQuery.setEnabled(false);
		newVirtualQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doNewVirtualQuery();
			}
		});
		query.add(newVirtualQuery);

		JMenuItem removeQuery = new JMenuItem("Remove");
		removeQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doRemoveQuery();
			}
		});
		query.add(removeQuery);

		JMenuItem load = new JMenuItem("Load");
		load.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doLoadQueryFromMQL();
			}

		});
		query.add(load);

		query.addSeparator();
		JMenuItem importQuery = new JMenuItem("Import MQL");
		importQuery.setEnabled(false);
		query.add(importQuery);

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
		all.add(settings);
		all.add(query);
		all.add(help);
		return all;
	}

	/**
   * 
   */
  protected void doDatasetViewSettings() {
    datasetViewSettings.showDialog(this);
    
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
	protected void doDatasourceSettings() {

		martManager.showDialog(this);
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

	/**
		 * 
		 */
	public void doLoadQueryFromMQL() {
		QueryEditor qe = null;
		try {
			qe = new QueryEditor(dsvAdaptor, martManager, datasetViewSettings);
			addQueryEditor(qe);
			qe.doLoadQuery();
		} catch (IOException e) {
			feedback.warn(e);
			if (qe != null)
				queryEditorTabbedPane.remove(qe);
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

			if (dsvAdaptor==null || dsvAdaptor.getDatasetViews().length == 0) {
				feedback.warn(
					"No dataset views available. You need load one or more "
						+ "datasets before you can create a query.");

			} else {

				QueryEditor qe;
				qe = new QueryEditor(dsvAdaptor, martManager, datasetViewSettings);
				qe.setName(nextQueryBuilderTabLabel());
				addQueryEditor(qe);

			}
		} catch (ConfigurationException e) {
			feedback.warn(e);
		} catch (IOException e) {
			feedback.warn(e);
		}

	}

	/**
	 * TODO Creates a new query from the results of other queries.
	 */
	public void doNewVirtualQuery() {

	}

	/**
	 * @return
	 */
	public DSViewAdaptor getDsvAdaptor() {
		return dsvAdaptor;
	}

	/**
	 * @param adaptor
	 */
	public void setDsvAdaptor(DSViewAdaptor adaptor) {
		dsvAdaptor = adaptor;
	}

}
