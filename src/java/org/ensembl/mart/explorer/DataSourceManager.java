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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;

/**
 * Widget for storing, selecting, adding and removing Marts.
 * Database addition dialog uses the preferences for this user in this package.
 */
public class DataSourceManager extends Box {

  private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog();
  private AdaptorManager datasetViewSettings = null;

  private final static Logger logger =
    Logger.getLogger(DataSourceManager.class.getName());

  private Vector marts = new Vector();
  private HashMap stringToMart = new HashMap();
  private LabelledComboBox combo;
  private String none = "None";
  private String selected = none;
  private Feedback feedback = new Feedback(this);

  /**
   * Constructor is deliberably onoly available to package memebers. 
   * @param datasetViewSettings
   */
  DataSourceManager(AdaptorManager datasetViewSettings) {

    super(BoxLayout.Y_AXIS);

    loadPrefs();

    this.datasetViewSettings = datasetViewSettings;

    databaseDialog.addDatabaseType("mysql");
    databaseDialog.addDriver("com.mysql.jdbc.Driver");
    databaseDialog.setPrefs(Preferences.userNodeForPackage(this.getClass()));

    combo = new LabelledComboBox("Datasource");
    combo.setEditable(false);

    JButton add = new JButton("Add");
    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doAdd();
      }
    });
    JButton delete = new JButton("Delete");
    delete.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doDelete();
      }
    });

    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(add);
    b.add(delete);

    add(combo);
    add(b);
  }



	/**
   * 
   */
  public void doDelete() {
    Object s = (String) combo.getSelectedItem();
    if (s != none)
      remove((DataSource) stringToMart.get(s));

  }

  /**
   * 
   */
  public void doAdd() {
    if (databaseDialog.showDialog(this)) {

      DetailedDataSource ds =
			  new DetailedDataSource(
			    databaseDialog.getDatabaseType(),
			    databaseDialog.getHost(),
			    databaseDialog.getPort(),
			    databaseDialog.getDatabase(),
			    databaseDialog.getUser(),
			    databaseDialog.getPassword(),
			    10,
			    databaseDialog.getDriver());
			
			
			try {
				DSViewAdaptor a = new DatabaseDSViewAdaptor(ds, databaseDialog.getUser());
				// TODO bind a and ds so that can recreate the link after persistence
				datasetViewSettings.add( a );
			} catch (ConfigurationException e1) {
			  feedback.warning("Couldn not load DatasetViews from \"" 
			    + ds
			    + "\". It might be possible to execute queries against this database.",e1, false);
			}
			
			add(ds);
			selected = ds.toString();
			initialiseCombo();
    }

  }

  /**
   * @return array of available marts
   */
  public DataSource[] getAll() {
    return (DataSource[]) marts.toArray(new DataSource[marts.size()]);
  }

  /**
   * Converts all marts into their toString() representations.
   * @return array of zero or more string where each one is the
   * result of a mart.toString(). Alphabetically sorted.
   */
  public String[] getAsStrings() {
    String[] s = new String[marts.size()];
    stringToMart.keySet().toArray(s);
    Arrays.sort(s);
    return s;
  }

  public DataSource get(int index) {
    return (DataSource) marts.get(index);
  }

  /**
   * @return number of marts
   */
  public int length() {
    return marts.size();
  }

  public DataSource get(String toStringRepresenation) {
    return (DataSource) stringToMart.get(toStringRepresenation);
  }

  /**
   * Adds a mart. If another mart with the same mart.toString() value 
   * has been added it is removed.
   * @param mart mart to add.
   */
  public void add(DataSource mart) {

    String key = mart.toString();
    Object clash = stringToMart.get(key);
    if (clash != null) {
      marts.remove(clash);
    }

    marts.add(mart);
    stringToMart.put(key, mart);

    Collections.sort(marts, new Comparator() {
      public int compare(Object o1, Object o2) {
        if (o1 == o2)
          return 0;
        else if (o1 == null)
          return -1;
        else if (o2 == null)
          return 1;
        else
          return o1.toString().compareTo(o2.toString());
      }
    });
    
    storePrefs();
  }

  /**
	 * TODO Store prefs in persistent storage.
	 */
	private void storePrefs() {
    logger.warning("Should store");   
		
	}
  
  
  /**
   * 
   * TODO Load prefs from persistent storage. 
   */
  private void loadPrefs() {
    logger.warning("Should load");   
  }

	public boolean remove(DataSource mart) {

    int index = marts.indexOf(mart);

    if (index == -1)
      return false;

    marts.remove(index);
    stringToMart.remove(mart.toString());

    // select the "next" item in the list
    if (mart.toString().equals(combo.getSelectedItem())) {
      if (index >= marts.size())
        selected = none;
      else
        selected = marts.get(index).toString();
    }

    initialiseCombo();

    storePrefs();

    return true;
  }

  /**
   * 
   */
  private void initialiseCombo() {

    logger.info("selected=" + selected);
    combo.removeAllItems();
    String[] keys = getAsStrings();
    combo.addItem(none);
    for (int i = 0; i < keys.length; i++)
      combo.addItem(keys[i]);
    combo.setSelectedItem(selected);

  }

  public boolean contains(DataSource mart) {
    return marts.contains(mart);
  }

  /**
   * Opens this MartManager as a dialog box. User can then
   * select, add and remove Marts.
   * @param parent
   * @return
   */
  public boolean showDialog(Component parent) {

    initialiseCombo();

    int option =
      JOptionPane.showOptionDialog(
        parent,
        this,
        "Datasource Chooser",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.DEFAULT_OPTION,
        null,
        null,
        null);

    if (option != JOptionPane.OK_OPTION) {
      return false;
    } else {
      selected = (String) combo.getSelectedItem();
      return true;
    }

  }

  /**
   * Runs test program.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Throwable {

    AdaptorManager dsvs = new AdaptorManager();
    DataSourceManager mm = dsvs.getMartSettings();

    // load some test values and check that the
    // manager works. 
    List l = QueryEditor.testDatasources();
    DataSource a = (DataSource) l.get(0);
    DataSource b = (DataSource) l.get(1);

    assert mm.length() == 0;
    mm.add(a);
    assert mm.length() == 1;
    mm.add(a);
    assert mm.getAll().length == 1;
    assert mm.get(0) == a;
    mm.add(b);
    assert mm.length() == 2;

    mm.showDialog(null);
    logger.info("selected=" + mm.getSelected());

    logger.info("finished");
    System.exit(0); // need to kill all threads
  }
  /**
   * @return selected datasource, null if none selected
   */
  public DataSource getSelected() {
    return (selected == none) ? null : (DataSource) stringToMart.get(selected);
  }

  public void setSelected(DataSource selectedDatasource) {
    if (selectedDatasource == null) {
      selected = none;
    } else {
      if (!marts.contains(selectedDatasource))
        add(selectedDatasource);
      selected = selectedDatasource.toString();
    }

  }


}
