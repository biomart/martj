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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * TODO change update mechanism to be based on setting the datasetName / 
 * internalName.
 * 
 * Widget representing dataset views available to user as a tree. 
 * 
 * The tree is constructed from all of the datasetNames 
 * (datasetView.internalName). 
 * "__" is used as the separator for menu names and leaf items in the tree.
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
public class DatasetWidget
  extends InputPage
  implements PropertyChangeListener {

  private Logger logger = Logger.getLogger(DatasetWidget.class.getName());

  private Matcher separatorMatcher = Pattern.compile("__").matcher("");

  // --- state

  private DatasetView selectedDatasetView = null;
  private String lastSelectedDisplayName = null;

  private DatasetView[] availableDatasetViews = null;
  private Set availableDatasetNames = new HashSet();
  private Set availableDisplayNames = new HashSet();
  private Map displayNameToDatasetView = new HashMap();
  private Map displayNameToShortName = new HashMap();
  private Map datasetNameToDatasetView = new HashMap();

  // --- GUI components
  private JMenuBar treeMenu = new JMenuBar();
  private JMenu treeTopMenu = new JMenu();
  private JLabel label = new JLabel("Dataset");
  private JTextField currentSelectedText = new JTextField(30);
  private JButton button = new JButton("change");
  private JMenuItem clearDatasetMenuItem = new JMenuItem("Clear Dataset");

  /**
   * This widget is part of a system based on the MVC design pattern. 
   * From this perspective the widget is a View and a Controller
   * and the query is the Model.
   * @param query underlying model for this widget.
   */
  public DatasetWidget(Query query) {

    super(query, "Dataset");

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
    box.add(label);
    box.add(Box.createHorizontalStrut(5));
    box.add(button);
    box.add(Box.createHorizontalStrut(5));
    box.add(currentSelectedText);

    setLayout(new BorderLayout());
    add(box, BorderLayout.NORTH);

  }

  public void showTree() {
    treeTopMenu.doClick();
  }

  /**
   * Selects datasetView with specified displayName. 
   * @param displayName display name of the datasetView to be selected. 
   * Current selection is cleared if null. If no datasetView with that
   * name is available nothing happens.
   */
  public void setDatasetViewByDisplayName(String displayName) {

    if (same(displayName, lastSelectedDisplayName))
      return;

    if (displayName != null) {

      DatasetView dsv = (DatasetView) displayNameToDatasetView.get(displayName);

      // ignore unkown displayName
      if (dsv == null)
        return;

      this.selectedDatasetView = dsv;

      String label = removeSeparators(displayName);
      currentSelectedText.setText(label);
      setNodeLabel("Dataset", label);

    } else {

      currentSelectedText.setText("");
      selectedDatasetView = null;
      setNodeLabel("Dataset", "");
      this.selectedDatasetView = null;

    }

    String datasetViewInternalName =
      (selectedDatasetView != null)
        ? selectedDatasetView.getInternalName()
        : null;

    updateQueryDatasetName(datasetViewInternalName);

    lastSelectedDisplayName = displayName;
  }

  /**
   * @param displayName
   * @return
   */
  private String removeSeparators(String name) {

    return separatorMatcher.reset(name).replaceAll(" ");

  }

  /**
   * If query has attributes or filters then set a "confirm" dialog box
   * is displayed before the dataset is changed. 
   * @param displayName display name of the datasetView.
   */
  private void doUserSelectDatasetView(String displayName) {

    if (same(displayName, lastSelectedDisplayName))
      return;

    //  Confirm user really wants to change dataset
    if (query.getAttributes().length > 0 || query.getFilters().length > 0) {

      int o =
        JOptionPane.showConfirmDialog(
          this,
          new JLabel("Changing the dataset will cause the query settings to be cleared. Continue?"),
          "Change Attributes",
          JOptionPane.YES_NO_OPTION);

      // undo if user changes mind
      if (o != JOptionPane.OK_OPTION) {

        currentSelectedText.setText(removeSeparators(displayName));
        return;
      }
    }

    setDatasetViewByDisplayName(displayName);
  }

  /**
   * @param value1 String to compare with value2, can be null.
   * @param value2 String to comare with value1, can be null.
   * @return true if value1 and value2 are equal, otherwise false.
   */
  private boolean same(String value1, String value2) {
    return value1 == value2 || (value1 != null && value1.equals(value2));

  }

  /**
     * Sets query.datasetName to datasetName.
     */
  private void updateQueryDatasetName(String datasetName) {

    query.removePropertyChangeListener(this);
    query.setDatasetInternalName(datasetName);

    if (datasetName == null) {
      query.setStarBases(null);
      query.setPrimaryKeys(null);
      query.setDataSource(null);

    } else {

      DatasetView view =
        (DatasetView) datasetNameToDatasetView.get(datasetName);
      query.setStarBases(view.getStarBases());
      query.setPrimaryKeys(view.getPrimaryKeys());
      query.setDataSource(view.getDatasource());
    }
    query.addPropertyChangeListener(this);
  }

  /**
     * Responds to a change in dataset on the query. Updates the state of
     * this widget by changing the currently selected item in the list.
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
  public void propertyChange(PropertyChangeEvent evt) {

    if (evt.getSource() == query && evt.getPropertyName().equals("datasetInternalName")) {

      String datasetName = (String) evt.getNewValue();

      if (datasetName == null) {
        setDatasetViewByDisplayName(null);
      } else if (
        selectedDatasetView != null
          && datasetName.equals(selectedDatasetView.getInternalName())) {
        // do nothing because datasetName is unchanged       
      }

      // update selection if unique
      else {

        Object o = datasetNameToDatasetView.get(datasetName);
        if (o == null) {
          JOptionPane.showMessageDialog(
            this,
            "No DatasetView available for the datasetName "
              + "on the current query: ",
            "Dataset View problem",
            JOptionPane.WARNING_MESSAGE);

        } else if (o instanceof List) {

          // dataSetName maps to more than one datasetView, user must choose
          // which one to use
          List l = (List) o;
          Object[] possibleValues = new Object[l.size()];
          for (int i = 0; i < l.size(); ++i) {
            DatasetView dsv = (DatasetView) l.get(i);
            possibleValues[i] = dsv.getDisplayName();
          }
          Object displayName =
            JOptionPane.showInputDialog(
              this,
              "Choose DatasetView",
              "Several datasetViews are availble for the dataset"
                + "set on the query, which do you want to use?",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              possibleValues,
              possibleValues[0]);

          currentSelectedText.setText(
            (String) displayNameToShortName.get(displayName));
        }

      }

    }

  }

  /**
   * @return
   */
  public DatasetView[] getDatasetViews() {
    return availableDatasetViews;
  }

  /**
   * Resets the widget clearing the previous selected DatasetView.
   * @param datasetViews
   */
  public void setDatasetViews(DatasetView[] datasetViews) {
    this.availableDatasetViews = datasetViews;

    unpack(datasetViews);

    lastSelectedDisplayName = null;
    selectedDatasetView = null;

    updateQueryDatasetName(null);

    currentSelectedText.setText("");

    updateMenu(datasetViews);

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

    availableDisplayNames.clear();
    availableDatasetNames.clear();
    displayNameToShortName.clear();

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

    datasetNameToDatasetView.clear();
    displayNameToDatasetView.clear();

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

  /**
   * Adds menu items and submenus to menu based on contents of _options_. A submenu is added 
   * when an option contains
   * sub options. If an option has no sub options it is added as a leaf node. This method calls
   * itself recursively to build up the menu tree.
   * @param menu menu to add options to
   * @param options options to be added, method does nothing if null.
   * @param prefix prepended to option.getDisplayName() to create internal name for menu item 
   * created for each option.
   */
  //  private void addOptions(JMenu menu, Option[] options, String prefix) {
  //
  //    for (int i = 0; options != null && i < options.length; i++) {
  //
  //      final Option option = options[i];
  //
  //      String displayName = option.getDisplayName();
  //      String qualifiedName = prefix + " " + displayName;
  //
  //      if (option.getOptions().length == 0) {
  //
  //        // add menu item
  //        JMenuItem item = new JMenuItem(displayName);
  //        item.setName(qualifiedName);
  //        menu.add(item);
  //        item.addActionListener(new ActionListener() {
  //          public void actionPerformed(ActionEvent event) {
  //            setOption(option);
  //          }
  //        });
  //
  //        valueToOption.put(option.getValue(), option);
  //
  //      } else {
  //
  //        // Add sub menu
  //        JMenu subMenu = new JMenu(displayName);
  //        menu.add(subMenu);
  //        addOptions(subMenu, option.getOptions(), qualifiedName);
  //
  //      }
  //    }
  //
  //  }

  /**
   * @return
   */
  public DatasetView getDatasetView() {
    return selectedDatasetView;
  }

	/**
	 * 
	 */
	public void setDatasetViewByInternalName(String internalName) {
		// TODO Auto-generated method stub
		DatasetView dsv = (DatasetView)datasetNameToDatasetView.get( internalName);
		setDatasetViewByInternalName( dsv.getDisplayName() );
	}

}
