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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.ensembl.mart.lib.BooleanFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.util.LoggingUtil;

/**
 * A boolean filter widget has a description and three radio buttons;
 * "require", "ignore", "irrelevant". The state of these buttons is 
 * synchronised with that of the query.
 */
public class BooleanFilterWidget
  extends FilterWidget
  implements ActionListener {

  private class OptionToStringProxy {
    private Option option;

    OptionToStringProxy(Option o) {
      this.option = o;
    }

    public String toString() {
      return option.getDisplayName();
    }
  }

  private Box panel = Box.createHorizontalBox();

  private JRadioButton require = new JRadioButton("require");
  private JRadioButton exclude = new JRadioButton("exclude");
  private JRadioButton irrelevant = new JRadioButton("irrelevant");
  private JComboBox list = null;

  private BooleanFilter requireFilter;
  private BooleanFilter excludeFilter;

  private Object currentButton = null;

  /**
   * @param query
   * @param filterDescription
   */
  public BooleanFilterWidget(
    FilterGroupWidget filterGroupWidget,
    Query query,
    FilterDescription fd,
    QueryTreeView tree) {

    super(filterGroupWidget, query, fd, tree);

    if ("boolean".equals(fd.getType()))
      initBoolean();

    else if ("boolean_num".equals(fd.getType()))
      initBooleanNum();
    else if ("boolean_num".equals(fd.getType()))
      initBooleanList();
    else
      new RuntimeException(
        "BooleanFilterWidget does not support filter description: "
          + fd
          + " becasue unrecognised type:"
          + fd.getType());

    irrelevant.setSelected(true);
    currentButton = irrelevant;

    ButtonGroup group = new ButtonGroup();
    group.add(require);
    group.add(exclude);
    group.add(irrelevant);

    require.addActionListener(this);
    exclude.addActionListener(this);
    irrelevant.addActionListener(this);

    panel.add(new JLabel(fd.getDisplayName()));
    if (list != null)
      panel.add(list);
    panel.add(Box.createHorizontalGlue());
    panel.add(require);
    panel.add(exclude);
    panel.add(irrelevant);
    add(panel);

    // adds list component if necessary  
    setOptions(fd.getOptions());

  }

  /**
   * 
   */
  private void initBooleanList() {
    // load options into list

    setOptions(filterDescription.getOptions());
  }

  /**
   * 
   */
  private void initBooleanNum() {

    requireFilter =
      new BooleanFilter(
        filterDescription.getField(),
        filterDescription.getTableConstraint(),
        filterDescription.getKey(),
        BooleanFilter.isNotNULL_NUM,
        filterDescription.getHandlerFromContext());

    excludeFilter =
      new BooleanFilter(
        filterDescription.getField(),
        filterDescription.getTableConstraint(),
        filterDescription.getKey(),
        BooleanFilter.isNULL_NUM,
        filterDescription.getHandlerFromContext());

  }

  private void initBoolean() {
    requireFilter =
      new BooleanFilter(
        filterDescription.getField(),
        filterDescription.getTableConstraint(),
        filterDescription.getKey(),
        BooleanFilter.isNotNULL,
        filterDescription.getHandlerFromContext());

    excludeFilter =
      new BooleanFilter(
        filterDescription.getField(),
        filterDescription.getTableConstraint(),
        filterDescription.getKey(),
        BooleanFilter.isNULL,
        filterDescription.getHandlerFromContext());

  }

  /**
   * Handles user selcting an item in list.
   * @param event
   */
  private void doSelectIListtem(ActionEvent event) {
    System.out.println("Handle user selcting item in list.");
    
    // TODO convert event to option.
    
    // TODO remove filter if set
    
    // TODO add filter if "required" selected.
  }

  /**
   * Responds to user button actions. Adds and removes filters to/from
   * query.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent evt) {

    // user clicked currently selected button
    if (evt.getSource() == currentButton)
      return;

    currentButton = evt.getSource();

    if (filter != null)
      query.removeFilter(filter);

    // TODO dynamically determine require and exclude filter for list
    // use getRequireFilter() and getExcludeFilter()
    if (currentButton == require)
      filter = requireFilter;
    else if (currentButton == exclude)
      filter = excludeFilter;
    else
      filter = null;

    if (filter != null)
      query.addFilter(filter);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
   */
  public void setOptions(Option[] options) {

    if (options == null) {
      // TODO remove list.
    } else if (options.length > 0) {

      if (list == null) {
        list = new JComboBox();
        list.setMaximumSize(new Dimension(100, 25));
        list.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent event) {
            doSelectIListtem(event);
          }

        });
        panel.add(list, 1);
      }

      for (int i = 0; i < options.length; i++) {
        Option o = options[i];
        if (o.isSelectable())
          list.addItem(new OptionToStringProxy(o));
      }
      panel.validate();
    }

  }

  /**
   * Selects the button based on the filter. This is a callback method called by
   * filterAdded(...) and filterRemoved(...) in FilterWidget base class. 
   */
  public void setFilter(Filter filter) {

    if (filter == null)
      irrelevant.setSelected(true);
    else if (filter.getCondition().equals(requireFilter.getCondition()))
      require.setSelected(true);
    else if (filter.getCondition().equals(excludeFilter.getCondition()))
      exclude.setSelected(true);

  }

  /**
   * Test program; a simple GUI using test data.
   * @param args
   * @throws org.ensembl.mart.lib.config.ConfigurationException
   */
  public static void main(String[] args)
    throws org.ensembl.mart.lib.config.ConfigurationException {

    // switch on logging for test purposes.
    LoggingUtil.setAllRootHandlerLevelsToFinest();
    Logger.getLogger(Query.class.getName()).setLevel(Level.FINE);

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
    BooleanFilterWidget bfw = new BooleanFilterWidget(fgw, q, fd, null);

    FilterDescription fd2 =
      new FilterDescription(
        "someInternalName",
        "someField",
        "boolean_num",
        "someQualifier",
        "someLegalQualifiers",
        "test boolean_num ",
        "someTableConstraint",
        "someKey",
        null,
        "someDescription");
    BooleanFilterWidget bfw2 = new BooleanFilterWidget(fgw, q, fd2, null);

    FilterDescription fd3 =
      new FilterDescription(
        "someInternalName",
        "someField",
        "boolean_list",
        "someQualifier",
        "someLegalQualifiers",
        "test boolean_list ",
        "someTableConstraint",
        "someKey",
        null,
        "someDescription");
    Option o = new Option("fred_id", "true");
    o.setDisplayName("Fred");
    fd3.addOption(o);
    Option o2 = new Option("barney_id", "true");
    o2.setDisplayName("Barney");
    fd3.addOption(o2);
    BooleanFilterWidget bfw3 = new BooleanFilterWidget(fgw, q, fd3, null);

    Box p = Box.createVerticalBox();
    p.add(bfw);
    p.add(bfw2);
    p.add(bfw3);

    JFrame f = new JFrame("Boolean Filter - test");
    f.getContentPane().add(p);
    f.setVisible(true);
    f.pack();

  }

}
