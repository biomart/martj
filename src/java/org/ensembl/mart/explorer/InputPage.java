
package org.ensembl.mart.explorer;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.ensembl.mart.lib.Field;
import org.ensembl.mart.lib.Query;

/**
 * Base class for user input pages. Uses a BorderLayout by default.
 */
public class InputPage extends JPanel {

  private Object userObject;

  protected Query query;
  private Field field;
  
  private String nodeLabel;  
  private MutableTreeNode node;
  private String defaultNodeLabel;

  protected List leafWidgets;  

  public InputPage(Query query, String name) {
    setName(name);
    this.query = query;
    setNodeLabel(name, null );
    node = new DefaultMutableTreeNode(this);
    leafWidgets = new ArrayList();
     
    //  use border layout so that a single item added to InputPage fills all available space in panel
    setLayout(new BorderLayout());   
  }

  public MutableTreeNode getNode() {
    return node;
  }

  /**
   * Derived classes should change _label_ to
   * whatever html string they want to appear in the tree view.
   * @return label.
   */
  public String toString() {
    return nodeLabel;
  }

  public void setNodeLabel(String title, String description) {
    StringBuffer buf = new StringBuffer();
    buf.append("<html>");
    if ( title!=null ) {
      buf.append("<b>").append(title);
      if ( description!=null ) buf.append(":");
      buf.append("</b> ");
    } 
    
    if ( description!=null ) buf.append(description);
    buf.append( "</html>" );
    nodeLabel =  buf.toString();
  }

	/**
	 * @return
	 */
	public String getNodeLabel() {
		return nodeLabel;
	}

  /**
   * User object is an object that the user has attached to 
   * this page.
   * @return user object if set, otherwise null.
   */
  public Object getUserObject() {
    return userObject;
  }

  /**
   * User object is an object that the user has attached to 
   * this page.
   * @return user object if set, otherwise null.
   */
  public void setUserObject( Object userObject ) {
    this.userObject = userObject;
  }



  public List getLeafWidgets() {
    return leafWidgets;
  }

  /**
   * @return field created by this InputPage, null if
   * none set.
   */
  public Field getField() {
    return field;
  }
  
  /**
   * @param field
   */
  protected void setField(Field field) {
    this.field = field;
  }

}
