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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.ensembl.mart.lib.IDListFilter;
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

  private Feedback feedback = new Feedback(this);

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
    noneButton.addActionListener(this);

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

  /**
   * Selects the button and item in list (if necessary) when filter changed.
   * This is a callback method called when a filter with the same fieldName 
   * as this widget is added or
   * removed to/from the query.
   */
  protected void setFilter(Filter filter) {

    if (filter == null) {

      noneButton.removeActionListener(this);
      noneButton.setSelected(true);
      noneButton.addActionListener(this);

    } else {

      IDListFilter f = (IDListFilter) filter;

      String[] ids = null;
      URL u = null;
      File fl = null;

      if ((ids = f.getIdentifiers()) != null && ids.length != 0) {

        idStringRadioButton.removeActionListener(this);
        idStringRadioButton.setSelected(true);
        idStringRadioButton.addActionListener(this);

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < ids.length; i++) 
          buf.append(ids[i]).append('\n');
        
        idString.setText(buf.toString());
        
      } else if ((u = f.getUrl()) != null) {

        urlRadioButton.removeActionListener(this);
        urlRadioButton.setSelected(true);
        urlRadioButton.addActionListener(this);

        url.setText(u.toExternalForm());

      } else if ((fl = f.getFile()) != null) {

        fileRadioButton.removeActionListener(this);
        fileRadioButton.setSelected(true);
        fileRadioButton.addActionListener(this);

        file.setText(fl.toString());

      }

    }

    this.filter = filter;

  }

  /**
   * Updates query in response to a user action. Removes old filter if necessary, adds new one if necessary
   * , or replaces old with new if necessary.
   */
  public void actionPerformed(ActionEvent e) {

    Filter newFilter = createFilter();

    query.removeQueryChangeListener(this);

    if (newFilter == null) {

      if (filter != null)
        query.removeFilter(filter);

    } else {

      if (filter != null)
        query.replaceFilter(filter, newFilter);

      else
        query.addFilter(newFilter);

    }

    filter = newFilter;

    query.addQueryChangeListener(this);
  }

  /**
   * Creates a filter based on the current state of the widget. If "none" is selected or
   * the field associated with the radio button is empty/invalid then no filter is returned.
   * @return filter if current state relates to one, otherwise null.
   */
  private Filter createFilter() {

    Option o = ((OptionToStringWrapper) list.getSelectedItem()).option;
    String f = o.getFieldFromContext();
    String tc = o.getTableConstraintFromContext();
    String k = o.getKeyFromContext();

    if (idStringRadioButton.isSelected() && idString.getText().length() != 0)
      return new IDListFilter(
        f,
        tc,
        k,
        idString.getText().split("(\\s+|\\s*,\\s*)"));

    else if (urlRadioButton.isSelected() && url.getText().length() != 0)
      try {
        return new IDListFilter(f, tc, k, new URL(url.getText()));
      } catch (MalformedURLException e) {
        feedback.warning("There is a problem with the URL: " + url.getText());

      } else if (fileRadioButton.isSelected() && file.getText().length() != 0)
      return new IDListFilter(f, tc, k, new File(file.getText()));

    return null;
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

  protected boolean equivalentFilter(Object otherFilter) {

    if (super.equivalentFilter(otherFilter))
      return true;

    if (otherFilter == null || !(otherFilter instanceof Filter))
      return false;

    if (indexOfListItemMatchingFilter((Filter) otherFilter) > -1)
      return true;

    return false;

  }

  /**
   * 
   * @param otherFilter
   * @return -1 if filter not relat
   */
  private int indexOfListItemMatchingFilter(Filter filter) {
    
    int index = -1;
    final int n = list.getItemCount();
    for (int i = 0; index==-1 && i < n; i++) {

      OptionToStringWrapper op = (OptionToStringWrapper) list.getItemAt(i);
      Option o = op.option;
      String f = filter.getField();
      String tc = filter.getTableConstraint();
      String k = filter.getKey();

      if (f != null
        && tc != null
        && k != null
        && !"".equals(f)
        && !"".equals(tc)
        && !"".equals(k)
        && f.equals(o.getFieldFromContext())
        && tc.equals(o.getTableConstraintFromContext())
        && k.equals(o.getKeyFromContext())) 
          index = i;
    }

    return index;
  }

}
