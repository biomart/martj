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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.ensembl.mart.guiutils.QuickFrame;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.Option;

/**
 * An ID list filter offers the user with a mechanism for filtering by IDs. The user can specify
 * a list oif IDs using verious sources and specify the type of the IDs.
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
 */
public class IDListFilterWidget extends FilterWidget  implements ActionListener{

  private JComboBox list = new JComboBox();
  private JTextArea idString = new JTextArea(10, 10);
  private JTextField file = new JTextField(20);
  private JTextField url = new JTextField(20);  
  private JButton clear = new JButton("Clear");
  private JButton browseForFile = new JButton("Browse");

  /**
   * @param filterGroupWidget
   * @param query
   * @param filterDescription
   * @param tree
   */
  public IDListFilterWidget(FilterGroupWidget filterGroupWidget, Query query, FilterDescription filterDescription, QueryTreeView tree) {
    super(filterGroupWidget, query, filterDescription, tree);
    
    System.out.println("TODO: handle "+filterDescription);
    
    Box b = Box.createVerticalBox();
    
    b.add(createLabel());
    b.add(list);
    b.add(new JLabel("IDs (type or paste)"));
    b.add(idString);
    b.add(new JLabel("File containing IDs"));
    b.add(file);
    b.add(browseForFile);
    b.add(new JLabel("URL containing IDs"));
    b.add(url);
    b.add(clear);
    
    add(b);
  }





  public void setOptions(Option[] options) {
    // TODO Auto-generated method stub
    
  }


  protected void setFilter(Filter filter) {
    // TODO Auto-generated method stub
    
  }



  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    
  }


  /**
   * Unit test for this class.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    Query q = new Query();
    FilterGroup fg = new FilterGroup();
    FilterGroupWidget fgw = new FilterGroupWidget(q, "fgw", fg, null);
    FilterDescription fd =
          new FilterDescription(
            "someInternalName",
            "someField",
            "boolean",
            "someQualifier",
            "someLegalQualifiers",
            "test boolean",
            "someTableConstraint",
            "someKey",
            null,
            "someDescription");
    new QuickFrame(IDListFilterWidget.class.getName(), new IDListFilterWidget(null, q, fd, null));
  }
}
