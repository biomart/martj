/*
 * Created on Aug 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.explorer;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.ensembl.mart.lib.Query;

/**
 * Base class for user input pages.
 */
public class InputPage extends JPanel {

  private Query query;
  private MutableTreeNode node;
  private String defaultNodeLabel;

  public InputPage(String name, Query query) {
    setName(name);
    this.query = query;
    node = new DefaultMutableTreeNode(this);
    defaultNodeLabel = "<html><b>"+name+"</b></html>";
  }

  public MutableTreeNode getNode() {
    return node;
  }

  public String toString() {
    return defaultNodeLabel;
  }

}
