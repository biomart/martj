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

import javax.naming.ConfigurationException;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.ensembl.mart.lib.BooleanFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.util.LoggingUtil;

/**
 * A boolean filter widget has a description and three radio buttons;
 * "require", "ignore", "irrelevant". The state of these buttons is 
 * synchronised with that of the query.
 */
public class BooleanFilterWidget
  extends FilterWidget
  implements ActionListener {

  private Box panel = Box.createHorizontalBox();

  private JRadioButton require = new JRadioButton("require");
  private JRadioButton exclude = new JRadioButton("exclude");
  private JRadioButton irrelevant = new JRadioButton("irrelevant");
  private JComboBox list = null;

  private BooleanFilter currentFilter;

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
    FilterDescription fd) {

    super(filterGroupWidget, query, fd);

    if ("boolean".equals(fd.getType())) {

      requireFilter =
        new BooleanFilter(
          fd.getField(),
          fd.getTableConstraint(),
          BooleanFilter.isNotNULL);

      excludeFilter =
        new BooleanFilter(
          fd.getField(),
          fd.getTableConstraint(),
          BooleanFilter.isNULL);

    } else {

      requireFilter =
        new BooleanFilter(
          fd.getField(),
          fd.getTableConstraint(),
          BooleanFilter.isNotNULL_NUM);

      excludeFilter =
        new BooleanFilter(
          fd.getField(),
          fd.getTableConstraint(),
          BooleanFilter.isNULL_NUM);

    }

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
   * Responds to user button actions. Adds and removes filters to/from
   * query.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent evt) {

    // user clicked currently selected button
    if (evt.getSource() == currentButton)
      return;

    currentButton = evt.getSource();

    if (currentFilter != null)
      query.removeFilter(currentFilter);

    if (currentButton == require)
      currentFilter = requireFilter;
    else if (currentButton == exclude)
      currentFilter = excludeFilter;
    else
      currentFilter = null;

    if ( currentFilter!=null )
      query.addFilter(currentFilter);
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
        list.addActionListener(this);
        panel.add(list, 1);
      }

      // TODO add options to list.

    }

  }

  /**
   * Selects the button based on the filter. This is a callback method called by
   * filterAdded(...) and filterRemoved(...) in FilterWidget base class. 
   */
  public void setFilter(Filter filter) {

    if (filter == null)
      irrelevant.setSelected(true);
    else if (filter.getValue().equals(requireFilter.getValue()))
      require.setSelected(true);
    else if (filter.getValue().equals(excludeFilter.getValue()))
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
    Logger.getLogger(Query.class.getName()).setLevel( Level.FINE );
    
    Query q = new Query();
    FilterGroup fg = new FilterGroup();
    FilterGroupWidget fgw = new FilterGroupWidget(q, "fgw", fg);
    FilterDescription fd =
      new FilterDescription(
        "someInternalName",
        "someField",
        "boolean",
        "someQualifier",
        "someLegalQualifiers",
        "someDisplayName",
        "someTableConstraint",
        null,
        "someDescription");
    BooleanFilterWidget bfw = new BooleanFilterWidget(fgw, q, fd);

    JFrame f = new JFrame("Boolean Filter - test");
    f.getContentPane().add(bfw);
    f.pack();
    f.setVisible(true);

  }

}
