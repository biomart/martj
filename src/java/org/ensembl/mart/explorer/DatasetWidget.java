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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
 * TODO setDatasetViews() + setMenu()
 * TODO test
 * 
 * Widget representing dataset views available to user as a tree. 
 * 
 * The tree is constructed from all of the datasetView displayNamess. 
 * "__" is used as the "node element" separater in the displayName.
 * For example, imagine these datasetNames are provided, one per datasetView:
 * <pre>
 * AAA
 * BBB
 * CCCCC__DDDDD
 * CCCCC__DDDDD__EEEE
 * CCCCC__DDDDD__FFFF
 * 
 * </pre>
 * The Tree would be:
 * <pre>
 * -AAA
 * -BBB
 * -CCCCC
 *       -DDDDD-EEEE
 *             -FFFF
 * </pre>
 *             
 * 
 * When a user selects a dataset view the dataset on the underlying query 
 * is updated. 
 * 
 * When a program sets the datasetView query.dataset is updated and so is 
 * the widget.
 * 
 * When query.dataset is updated this widget throws a runtime exception 
 * because it's new possible state might be ambiguous. 
 * A single dataset could be represented by multiple datasetViews, in this 
 * case the widget cannot choose which view to select.  
 */
public class DatasetWidget
  extends InputPage
  implements PropertyChangeListener {

  private Logger logger = Logger.getLogger(DatasetWidget.class.getName());

  // --- state

  private DatasetView selectedDatasetView = null;
  private String lastSelectedDisplayName = null;

  private DatasetView[] availableDatasetViews = null;
  private Set availableDisplayNames = new HashSet();
  private Map displayNameToDatasetView = new HashMap();
  private Map displayNameToShortName = new HashMap();
  private Map datasetNameToDatasetView = new HashMap();

  // --- GUI components
  private JMenuBar treeMenu = new JMenuBar();
  private JMenu treeTopMenu = null;
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
  public void setSelectionDatasetViewDisplayName(String displayName) {

    if (displayName == lastSelectedDisplayName
      || (displayName != null && displayName.equals(lastSelectedDisplayName)))
      return;

    if (displayName != null) {

      DatasetView dsv = (DatasetView) displayNameToDatasetView.get(displayName);

      // ignore unkown displayName
      if (dsv == null)
        return;

      this.selectedDatasetView = dsv;

      String text = (String) displayNameToShortName.get(displayName);
      currentSelectedText.setText(text);
    } else {
      currentSelectedText.setText("");
      selectedDatasetView = null;
    }
    String datasetViewInternalName =
      (selectedDatasetView != null)
        ? selectedDatasetView.getInternalName()
        : null;

    updateQueryDatasetName(datasetViewInternalName);
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
          "Change Attributes",
          JOptionPane.YES_NO_OPTION);

      // undo if user changes mind
      if (o != JOptionPane.OK_OPTION) {

        String text =
          (String) displayNameToShortName.get(lastSelectedDisplayName);
        currentSelectedText.setText(text);
        return;
      }
    }

    setSelectionDatasetViewDisplayName(displayName);
  }

  /**
     * Sets query.datasetName to datasetName.
     */
  private void updateQueryDatasetName(String datasetName) {

    query.removePropertyChangeListener(this);
    query.setDatasetName(datasetName);
    query.addPropertyChangeListener(this);
  }

  /**
     * Responds to a change in dataset on the query. Updates the state of
     * this widget by changing the currently selected item in the list.
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
  public void propertyChange(PropertyChangeEvent evt) {

    if (evt.getSource() == query && evt.getPropertyName().equals("dataset")) {

      String datasetName = (String) evt.getNewValue();

      if (datasetName == null) {
        setSelectionDatasetViewDisplayName(null);
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
          List l = (List)o;
          Object[] possibleValues = new Object[ l.size()];
          for (int i=0; i<l.size(); ++i) {
            DatasetView dsv = (DatasetView) l.get(i);
            possibleValues[ i ] = dsv.getDisplayName();
          }
          Object displayName =
            JOptionPane.showInputDialog(
              this,
              "Choose DatasetView",
              "Several datasetViews are availble for the dataset" +              "set on the query, which do you want to use?",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              possibleValues,
              possibleValues[0]);

          
          currentSelectedText.setText( (String)displayNameToShortName.get( displayName) );
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

    datasetNameToDatasetView.clear();
    displayNameToDatasetView.clear();
    displayNameToShortName.clear();
    
    lastSelectedDisplayName = null;
    selectedDatasetView = null;
    
    updateQueryDatasetName(null);

    updateMenu(datasetViews);

  }

  /**
   * @param datasetViews
   */
  private void updateMenu(DatasetView[] datasetViews) {
    // TODO Auto-generated method stub
    //  for each datasetView
    //    create a qualified name
    //    map qualified name to dataset

    // sort by displayName

    // use last component of displayName as menu item's text

    // construct JMenu - see addOptions(...)

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

}
