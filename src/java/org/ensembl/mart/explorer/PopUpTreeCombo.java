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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultTreeModel;

import org.ensembl.mart.lib.config.DatasetView;

/**
 * Abstract generic tree option widget. It presents the user with set of options
 * organised as a tree.
 * 
 * <p>Developers who wish to extend this
 * component should implement the update() method. This is called after the
 * change button is pressed but before the menu is displayed. This enables the available
 * options in the menu to be updated before it is displayed. Typical usage: 
 * <code>
 * <ol>
 * <li>clear();
 * <li>construct tree from the rootNode down.
 * </ol>
 * </code>
 * </p>
 */
public abstract class PopUpTreeCombo extends JPanel {

  private String lastLabel;
  private Object lastObject;
  private JMenuItem oldItem;
  // --- state
  protected LabelledTreeNode rootNode = new LabelledTreeNode(null, null);
  protected DefaultTreeModel model = new DefaultTreeModel(rootNode);

  private List listeners = new ArrayList();

  // --- UI
  private JButton button = new JButton("change");
  private JTextField selectedTextField = new JTextField(30);
  private JMenuBar treeMenu = new JMenuBar();
  private JMenu firstTier = new JMenu();
  private Feedback feedback = new Feedback(this);

  public PopUpTreeCombo(String label) {
    super();
    createUI(label);

  }

  public void addActionListener(ActionListener listener) {
    listeners.add(listener);
  }

  public void removeActionListener(ActionListener listener) {
    listeners.remove(listener);
  }

  /** 
   * To be overidden be implementing classes who wish to changed the model
   * before the tree is displayed.
   */
  public abstract void update();

  private void createUI(String label) {

    selectedTextField.setEditable(false);
    selectedTextField.setMaximumSize(new Dimension(400, 27));

    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        showTree();
      }
    });

    // make the menu appear beneath the row of components 
    // containing the label, textField and button when displayed.
    treeMenu.setMaximumSize(new Dimension(0, 100));
    treeMenu.add(firstTier);

    add(new JLabel(label));
    add(treeMenu);
    add(button);
    add(selectedTextField);

  }

  public void showTree() {
    if (model != null) {

      update();
      updateMenu();
      firstTier.doClick();
    }

  }

  /**
   * @param displayName display name of the selected datasetView.
   */
  private void doSelect(String label, Object o) {

    if (label == lastLabel && o == lastObject)
      return;

    lastLabel = label;
    lastObject = o;

    selectedTextField.setText(label);

    ActionEvent event = new ActionEvent(this, 0, null);
    for (Iterator iter = listeners.iterator(); iter.hasNext();) {
      ActionListener l = (ActionListener) iter.next();
      l.actionPerformed(event);
    }

  }
  private JMenuItem noneMenuItem = new JMenuItem("None");
  private Map datasetNameToDatasetView = new HashMap();
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
   * Update the menu to reflect the model. 
   */
  private void updateMenu() {

    firstTier.removeAll();

    int n = rootNode.getChildCount();
    for (int i = 0; i < n; i++)
      addToMenu(firstTier, (LabelledTreeNode) rootNode.getChildAt(i));

  }

  /**
   * @param rootNode
   */
  private void addToMenu(JMenu menu, final LabelledTreeNode node) {
    if (node.getChildCount() == 0) {

      JMenuItem item = new JMenuItem(node.getLabel());
      menu.add(item);
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          doSelect(node.getLabel(), node.getUserObject());
        }
      });

    } else {

      JMenu m = new JMenu(node.getLabel());
      menu.add(m);

      int n = node.getChildCount();
      for (int i = 0; i < n; i++)
        addToMenu(m, (LabelledTreeNode) node.getChildAt(i));

    }

  }

  public static void main(String[] args) {

    final PopUpTreeCombo pu = new PopUpTreeCombo("Test") {
      public void update() {
        // Create a sample tree for rendering
        rootNode.removeAllChildren();
        rootNode.add(new LabelledTreeNode("a", "a"));
        rootNode.add(new LabelledTreeNode("b", "b"));
        LabelledTreeNode c = new LabelledTreeNode("c", "c");
        c.add(new LabelledTreeNode("c1", "c1"));
        rootNode.add(c);

        rootNode.add(new LabelledTreeNode("d", "d"));
      }

    };

    // test the listener support
    pu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("Selection changed to : " + pu.getSelectedLabel());
      }
    });

    Box p = Box.createVerticalBox();
    p.add(pu);

    JFrame f = new JFrame(PopUpTreeCombo.class.getName() + " (Test Frame)");
    f.getContentPane().add(p);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //f.setSize(250, 100);
    f.pack();
    f.setVisible(true);

  }

  /**
   * @return selected label, null if none selected.
   */
  public String getSelectedLabel() {
    return lastLabel;
  }

  /**
   * @return selected user object, null if none set.
   */
  public Object getSelectedUserObject() {
    return lastObject;
  }
}