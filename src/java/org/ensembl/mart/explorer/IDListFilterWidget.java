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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.ensembl.mart.guiutils.QuickFrame;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.util.LoggingUtil;

/**
 * An ID list filter offers the user with a mechanism for filtering by IDs. The user can specify
 * a list oif IDs using verious sources and specify the type of the IDs.
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
 */
public class IDListFilterWidget
  extends FilterWidget
  implements ActionListener {

  private JComboBox list = new JComboBox();

  private JTextArea idString = new JTextArea(10, 10);
  private JTextField file = new JTextField(20);
  private JTextField url = new JTextField(20);
  private JButton browseForFile = new JButton("Browse");

  private JRadioButton idStringRadioButton =
    new JRadioButton("IDs (type or paste)");
  private JRadioButton fileRadioButton =
    new JRadioButton("File containing IDs");
  private JRadioButton urlRadioButton = new JRadioButton("URL containing IDs");
  private JRadioButton noneButton = new JRadioButton("None");

  private Object lastRadioButton = null;

  /**
   * @param filterGroupWidget
   * @param query
   * @param filterDescription
   * @param tree
   */
  public IDListFilterWidget(
    FilterGroupWidget filterGroupWidget,
    Query query,
    FilterDescription filterDescription,
    QueryTreeView tree) {
    super(filterGroupWidget, query, filterDescription, tree);

    ButtonGroup bg = new ButtonGroup();
    bg.add(idStringRadioButton);
    bg.add(fileRadioButton);
    bg.add(urlRadioButton);
    bg.add(noneButton);
    noneButton.setSelected(true);

    idStringRadioButton.addActionListener(this);
    fileRadioButton.addActionListener(this);
    urlRadioButton.addActionListener(this);

    //  TODO add key and focus listener to idString: change -> remove filter + new filter
    
    //  TODO add key and focus listener to url: change -> remove filter + new filter

    //  TODO add key and focus listener to file: change -> remove filter + new filter

    Box b = Box.createVerticalBox();

    b.add(
      createRow(createLabel(), (JComponent) Box.createHorizontalGlue(), null));
    b.add(list);

    b.add(createRow(idStringRadioButton, idString, null));

    b.add(createRow(fileRadioButton, file, browseForFile));

    b.add(createRow(urlRadioButton, url, null));

    b.add(createRow(noneButton, (JComponent) Box.createHorizontalGlue(), null));

    setOptions(filterDescription.getOptions());

    add(b);
  }

  private JComponent createRow(JComponent a, JComponent b, JComponent c) {
    Box p = Box.createHorizontalBox();
    if (a != null)
      p.add(a);
    if (b != null)
      p.add(b);
    if (c != null)
      p.add(c);
    return p;
  }

  public void setOptions(Option[] options) {

    if (list == null)
      return;

    list.removeActionListener(this);
    list.removeAllItems();

    // add items
    for (int i = 0; i < options.length; i++) {
      Option o = options[i];
      if (o.isSelectable())
        list.addItem(new OptionToStringWrapper(this, o));
    }

    list.addActionListener(this);

    list.validate();

  }

  protected void setFilter(Filter filter) {
    // TODO Auto-generated method stub

  }

  public void actionPerformed(ActionEvent e) {
   
   Object c = e.getSource();

    if (c == lastRadioButton)
      return;

    else {
      System.out.println(c);

      if (c == idStringRadioButton) {
        
        lastRadioButton = c;
        removeFilter();
        // only allow if string set.
        if (idString.getText().length() != 0) {
          System.out.println(
            "TODO remove existing filter, if any and set new IDs one");
        } 

      } else if (c == fileRadioButton) {
        
        lastRadioButton = c;
        removeFilter();
        // only allow if string set.
        if (file.getText().length() != 0) {
          System.out.println(
            "TODO remove existing filter, if any and set new FILE one");
        } 

      } else if (c == urlRadioButton) {
        
        lastRadioButton = c;
        removeFilter();
        // only allow if string set.
        if (url.getText().length() != 0) {
          System.out.println(
            "TODO remove existing filter, if any and set new URL one");
        } 

      }
    }

  }


  /**
   * If filter exists is set it is removed from the query.
   *
   */
  private void removeFilter() {
    System.out.println("TODO Remove existing filter");
  
    if (filter!=null) query.removeFilter(filter);
    
  }

  /**
   * Unit test for this class.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    // enable logging messages
    LoggingUtil.setAllRootHandlerLevelsToFinest();
    Logger.getLogger(Query.class.getName()).setLevel(Level.FINEST);

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
        "id_list test",
        "someTableConstraint",
        "someKey",
        null,
        "someDescription");

    Option o = new Option("fred_id", "true");
    o.setParent(fd);
    o.setDisplayName("Fred");
    o.setField("fred_field");
    fd.addOption(o);
    Option o2 = new Option("barney_id", "true");
    o2.setParent(fd);
    o2.setDisplayName("Barney");
    o2.setField("barney");
    fd.addOption(o2);

    new QuickFrame(
      IDListFilterWidget.class.getName(),
      new IDListFilterWidget(null, q, fd, null));
  }

}
