/*
 * Created on Aug 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.explorer;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.ensembl.mart.lib.Query;

/**
 * Base class for user input pages.
 */
public class InputPage extends JPanel {

  private Query query;
  private TreeNode node;

  public InputPage(String name, Query query) {
    setName(name);
    this.query = query;
    node = new DefaultMutableTreeNode(this);
  }

  public TreeNode getNode() {
    return node;
  }

}
