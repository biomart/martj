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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryChangeListener;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Widget representing the currently selected dataset view
 * and enabling the user to select another.  
 * 
 * The tree is constructed from all of the datasetNames 
 * displayNames. They are arranged as a tree of options.
 * Where "__" appears in the displayName it is used as a
 * branch point in a tree menu.
 * For example an array of datasetViews with these datasetNames:
 * <pre>
 * AAA
 * BBB
 * CCCCC__DDDDD
 * CCCCC__DDDDD__EEEE
 * CCCCC__DDDDD__FFFF
 * 
 * </pre>
 * Would be rendered as this tree:
 * <pre>
 * -AAA
 * -BBB
 * -CCCCC
 *       -DDDDD-EEEE
 *             -FFFF
 * </pre>
 *             
 * 
 * When a user selects a datasetView the dataset on the underlying query 
 * is updated. 
 * 
 * When a program sets the datasetView query.dataset is updated and so is 
 * the selected item in the tree.
 * 
 * When query.dataset is updated this widget throws a runtime exception 
 * if it's new possible state is ambiguous.
 *  
 * A single dataset could be represented by multiple datasetViews, in this 
 * case the widget cannot choose which view to select.  
 */
public class DatasetWidget extends InputPage implements QueryChangeListener {

  private DatasetView datasetView;

  private Logger logger = Logger.getLogger(DatasetWidget.class.getName());
  
  private Feedback feedback = new Feedback(this);

  private DSViewAdaptor datasetViewAdaptor;

  // Used to find and remove separators in datasetView.displayNames
  private Matcher separatorMatcher = Pattern.compile("__").matcher("");

  // --- state

  private Map datasetNameToDatasetView = new HashMap();

  // --- GUI components
  private JMenuBar treeMenu = new JMenuBar();
  private JMenu treeTopMenu = new JMenu();
  private JTextField currentSelectedText = new JTextField(30);
  private JButton button = new JButton("change");
  private JMenuItem clearDatasetMenuItem = new JMenuItem("Clear Dataset");

  /**
   * This widget is part of a system based on the MVC design pattern. 
   * From this perspective the widget is a View and a Controller
   * and the query is the Model.
   * @param query underlying model for this widget.
   */
  public DatasetWidget(Query query, DSViewAdaptor datasetViewAdaptor) {

    super(query, "Dataset");

    this.datasetViewAdaptor = datasetViewAdaptor;
    query.addQueryChangeListener(this);

    clearDatasetMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        doUserSelectDatasetView(null);
      }
    });

    currentSelectedText.setEditable(false);
    currentSelectedText.setMaximumSize(new Dimension(400, 27));

    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        showTree();
      }
    });

    // make the menu appear beneath the row of components 
    // containing the label, textField and button when displayed.
    treeMenu.setMaximumSize(new Dimension(0, 100));
    treeMenu.add(treeTopMenu);

    Box box = Box.createHorizontalBox();
    box.add(treeMenu);
    box.add(new JLabel("Dataset"));
    box.add(Box.createHorizontalStrut(5));
    box.add(button);
    box.add(Box.createHorizontalStrut(5));
    box.add(currentSelectedText);

    setLayout(new BorderLayout());
    add(box, BorderLayout.NORTH);

  }

  public void showTree() {
    try {
      if (datasetViewAdaptor != null) {

        updateMenu(datasetViewAdaptor.getDatasetViews());
        treeTopMenu.doClick();
      }
    } catch (ConfigurationException e) {
      feedback.warn(e);
    }

  }


  /**
   * If query has attributes or filters then set a "confirm" dialog box
   * is displayed before the dataset is changed. 
   * @param displayName display name of the datasetView.
   */
  private void doUserSelectDatasetView(String displayName) {

    //  Confirm user really wants to change dataset
    if (query.getAttributes().length > 0 || query.getFilters().length > 0) {

      int o =
        JOptionPane.showConfirmDialog(
          this,
          new JLabel("Changing the dataset will cause the query settings to be cleared. Continue?"),
          "Delete current Change Attributes",
          JOptionPane.YES_NO_OPTION);

      // undo if user changes mind
      if (o != JOptionPane.OK_OPTION)
        return;

    }

    // TODO add DatasetView.supports(Query) and use to decide whether to
    // clear query or leave unchanged.

    // Currently changing the dataset view requires us to reset the query 
    // because existing attributes and filters (or there combination) 
    // *might* not be valid for the new dataset view
    query.clear();

    if (displayName != null) {

      try {
        DatasetView dsv =
          datasetViewAdaptor.getDatasetViewByDisplayName(displayName);

        // initialise the query with settings from the datasetview.
        query.setDatasetInternalName(dsv.getDataset());
        query.setDataSource(dsv.getDatasource());
        query.setPrimaryKeys(dsv.getPrimaryKeys());
        query.setStarBases(dsv.getStarBases());

      } catch (ConfigurationException e) {
        feedback.warn(e);
      }
    }

  }


  /**
   * Unpacks the datasetViews into several sets and maps that enable
   * easy lookup of information.
   * 
   * displayName -> shortName
   * datasetName -> datasetView | List-of-datasetViews
   * displayName -> datasetView | List-of-datasetViews
   * 
   * @param datasetViews dataset views, should be sorted by displayNames.
   */
  private void unpack(DatasetView[] datasetViews) {

    Set availableDisplayNames = new HashSet();
    Set availableDatasetNames = new HashSet();
    Map displayNameToDatasetView = new HashMap();
    Map displayNameToShortName = new HashMap();

    if (datasetViews == null)
      return;

    Set clashingDisplayNames = new HashSet();
    Set clashingDatasetNames = new HashSet();

    for (int i = 0; i < datasetViews.length; i++) {

      DatasetView view = datasetViews[i];

      String displayName = view.getDisplayName();
      if (availableDisplayNames.contains(displayName))
        clashingDisplayNames.add(view);
      else
        availableDisplayNames.add(displayName);

      String datasetName = view.getInternalName();
      if (availableDatasetNames.contains(datasetName))
        clashingDatasetNames.add(view);
      else
        availableDatasetNames.add(datasetName);

      String[] elements = displayName.split("__");
      String shortName = elements[elements.length - 1];
      displayNameToShortName.put(displayName, shortName);
    }

    for (int i = 0; i < datasetViews.length; i++) {

      DatasetView view = datasetViews[i];

      String displayName = view.getDisplayName();
      if (clashingDisplayNames.contains(view)) {
        List list = (List) displayNameToDatasetView.get(displayName);
        if (list == null) {
          list = new LinkedList();
          displayNameToDatasetView.put(displayName, list);
        }
        list.add(view);
      } else {
        displayNameToDatasetView.put(displayName, view);
      }

      String datasetName = view.getInternalName();
      if (clashingDatasetNames.contains(view)) {
        List list = (List) datasetNameToDatasetView.get(datasetName);
        if (list == null) {
          list = new LinkedList();
          datasetNameToDatasetView.put(datasetName, list);
        }
        list.add(view);
      } else {
        datasetNameToDatasetView.put(datasetName, view);
      }

    }

  }

  /**
   * Update the menu to reflect the datasetViews. 
   * Menu item names and position in the menu tree are
   * derived from the displayNames of the datasetViews. 
   * @param datasetViews
   */
  private void updateMenu(DatasetView[] datasetViews) {

    treeTopMenu.removeAll();

    if (datasetViews == null || datasetViews.length == 0)
      return;

    //  we need the dsvs sorted so we can construct the menu tree
    // by parsing the array once
    Arrays.sort(datasetViews, new Comparator() {
      public int compare(Object o1, Object o2) {
        DatasetView d1 = (DatasetView) o1;
        DatasetView d2 = (DatasetView) o2;
        return d1.getDisplayName().compareTo(d2.getDisplayName());
      }
    });

    if (query.getDatasetInternalName() != null)
      treeTopMenu.add(clearDatasetMenuItem);

    String[][] tree = new String[][] {
    };
    Map menus = new HashMap();

    for (int i = 0; i < datasetViews.length; i++) {
      final DatasetView view = datasetViews[i];

      final String datasetName = view.getDisplayName();

      String[] elements = datasetName.split("__");

      for (int j = 0; j < elements.length; j++) {

        String substring = elements[j];

        JMenu parent = treeTopMenu;
        if (j > 0)
          parent = (JMenu) menus.get(elements[j - 1]);

        if (j + 1 == elements.length) {

          // user selectable leaf node
          JMenuItem item = new JMenuItem(substring);
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
              doUserSelectDatasetView(view.getDisplayName());
            }
          });
          parent.add(item);

        } else {

          // intermediate menu node
          JMenu menu = (JMenu) menus.get(elements[j]);
          if (menu == null) {
            menu = new JMenu(substring);
            menus.put(substring, menu);
            parent.add(menu);
          }

        }

      }
    }

  }


  public void queryNameChanged(
    Query sourceQuery,
    String oldName,
    String newName) {
  }

  /**
   * Responds to a change in dataset on the query. Updates the state of
   * this widget by changing the currently selected item in the list.
   */
  public void queryDatasetInternalNameChanged(
    Query sourceQuery,
    String oldDatasetInternalName,
    String newDatasetInternalName) {

    if (newDatasetInternalName == null) {

      currentSelectedText.setText("");

    } else {

      try {
        DatasetView[] dsvs =
          datasetViewAdaptor.getDatasetViewByDataset(newDatasetInternalName);
        // TODO Handle >1 dsv for dataset
        datasetView = dsvs[0];
        String dn = datasetView.getDisplayName(); 
        dn = separatorMatcher.reset( dn ).replaceAll(" ");
        currentSelectedText.setText( dn );
      } catch (ConfigurationException e) {
        feedback.warn(e);
        // show the raw dataset name by default
        currentSelectedText.setText(newDatasetInternalName);
      }

    }
  }

  public void queryDatasourceChanged(
    Query sourceQuery,
    DataSource oldDatasource,
    DataSource newDatasource) {
  }

  public void queryAttributeAdded(
    Query sourceQuery,
    int index,
    Attribute attribute) {
  }

  public void queryAttributeRemoved(
    Query sourceQuery,
    int index,
    Attribute attribute) {
  }

  public void queryFilterAdded(Query sourceQuery, int index, Filter filter) {
  }

  public void queryFilterRemoved(Query sourceQuery, int index, Filter filter) {
  }

  public void queryFilterChanged(
    Query sourceQuery,
    Filter oldFilter,
    Filter newFilter) {
  }

  public void querySequenceDescriptionChanged(
    Query sourceQuery,
    SequenceDescription oldSequenceDescription,
    SequenceDescription newSequenceDescription) {
  }

  public void queryLimitChanged(Query query, int oldLimit, int newLimit) {
  }

  public void queryStarBasesChanged(
    Query sourceQuery,
    String[] oldStarBases,
    String[] newStarBases) {
  }

  public void queryPrimaryKeysChanged(
    Query sourceQuery,
    String[] oldPrimaryKeys,
    String[] newPrimaryKeys) {
  }

  /**
   * Runs a test; an instance of this class is shown in a Frame.
   */
  public static void main(String[] args) throws Exception {
    JFrame f = new JFrame("Dataset Chooser (Test Frame)");
    Box p = Box.createVerticalBox();
    p.add(new DatasetWidget(new Query(), QueryEditor.testDSViewAdaptor()));
    f.getContentPane().add(p);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setSize(950, 750);
    f.setVisible(true);
  }
  /**
   * @return
   */
  public DatasetView getDatasetView() {
    return datasetView;
  }

}
