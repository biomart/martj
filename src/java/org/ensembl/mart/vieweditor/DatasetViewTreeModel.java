package org.ensembl.mart.vieweditor;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Created by IntelliJ IDEA.
 * User: katerina
 * Date: 18-Nov-2003
 * Time: 18:35:08
 * To change this template use Options | File Templates.
 */
public class DatasetViewTreeModel extends DefaultTreeModel {

// This class represents the data model for this tree
      DatasetViewTreeNode node;
      public DatasetViewTreeModel(DatasetViewTreeNode node) {
          super(node);
          this.node = node;
      }

      public void valueForPathChanged(TreePath path,
                                   Object newValue) {
        DatasetViewTreeNode node1 =
            (DatasetViewTreeNode)path.getLastPathComponent();
        String value = (String)newValue;

          System.out.println("in model "+value);
          node1.setName(value);

        nodeChanged(node1);
      }



}
