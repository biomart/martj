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

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Filter;

/**
 * Used to create the label on the TreeNode it added to and store
 * optional attribute and filter objects. 
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class TreeNodeData {

  public static final TreeNodeData DATASOURCE =
    new TreeNodeData("Datasource", ":", null);
  
  public static final TreeNodeData DATASET =
    new TreeNodeData("Dataset", ":", null);
  
  public static final TreeNodeData ATTRIBUTES =
    new TreeNodeData("Attributes", null, null);
  
  public static final TreeNodeData FILTERS =
    new TreeNodeData("Filters", null, null);
  
  public static final TreeNodeData FORMAT =
    new TreeNodeData("Format", null, null);

  private String label;
  private String separator;
  private String rightText;
  private Attribute attribute;
  private Filter filter;

  private TreeNodeData(
    String label,
    String separator,
    String rightText,
    Attribute attribute,
    Filter filter) {

    this.label = label;
    this.separator = separator;
    this.rightText = rightText;
    this.attribute = attribute;
    this.filter = filter;
  }

  public TreeNodeData(
    String label,
    String separator,
    String rightText,
    Attribute attribute) {

    this(label, separator, rightText, attribute, null);
  }

  public TreeNodeData(
    String label,
    String separator,
    String rightText,
    Filter filter) {

    this(label, separator, rightText, null, filter);
  }

  public TreeNodeData(String label, String separator, String rightText) {
    this(label, separator, rightText, null, null);
  }

  /**
   * Generates a small
   * piece of html that is used to create the "labels" for tree nodes.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<html>");
    buf.append("<b>");
    if (label != null)
      buf.append(label);
    if (separator != null)
      buf.append(separator);

    buf.append("</b> ");
    if (rightText != null)
      buf.append(rightText);
    buf.append("</html>");
    return buf.toString();
  }

  /**
   * @return attribute if set, otherwise null
   */
  public Attribute getAttribute() {
    return attribute;
  }

  /**
   * @return filter if set, otherwise null
   */
  public Filter getFilter() {
    return filter;
  }

  /**
   * @return
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return
   */
  public String getRightText() {
    return rightText;
  }

  /**
   * @return
   */
  public String getSeparator() {
    return separator;
  }

  /**
   * @param string
   */
  public void setRightText(String string) {
    rightText = string;
  }

}
