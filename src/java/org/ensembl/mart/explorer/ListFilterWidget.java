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
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.BaseNamedConfigurationObject;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.Option;

/**
 * Represents a list of user options. Some options cause the
 * options available in other widgets to be modified.
 */
public class ListFilterWidget extends FilterWidget implements ActionListener {

  private Filter filter;

  private Map filterValueToItem;

  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

  private JComboBox list;
  private Object lastSelectedItem;
  /**
   * @param query model to bv synchronised
   * @param filterDescription parameters for this widget
   */
  public ListFilterWidget(
    FilterGroupWidget filterGroupWidget,
    Query query,
    FilterDescription filterDescription) {
    super(filterGroupWidget, query, filterDescription);

    list = new JComboBox();
    list.addActionListener(this);

    configureList(list, filterDescription);

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(new JLabel(filterDescription.getDisplayName()));
    add(Box.createHorizontalStrut(5));
    add(list);
  }

  /**
   * Configures the options based on the filterDescription.
   * @param list
   * @param filterDescription
   */
  private void configureList(
    JComboBox list,
    FilterDescription filterDescription) {

    String field = filterDescription.getField();

    if (BaseNamedConfigurationObject.isInvalid(field) && !filterDescription.hasOptions())
      throw new RuntimeException(
        "Invalid filterDescription: " + filterDescription);

    setOptions(filterDescription.getOptions());

  }

  /**
   * Synchronises the state of this Filter with the specified filter.
   * Sets the appropriate selercted item and assigns / unassigns any PushOption.
   * @param filter filter to assign, or null if filter is to be removed.
   */
  protected void setFilter(Filter filter) {

    this.filter = filter;

    if (filter == null) {

      setSelectedItem(emptySelection);
      unassignPushOptions();
    
    } else {
     
      OptionWrapper ow =
        (OptionWrapper) filterValueToItem.get(filter.getValue());
      setSelectedItem(ow);
      assignPushOptions(ow.option.getPushActions());

    }
  }

  /**
   * @param emptySelection
   */
  private void setSelectedItem(OptionWrapper wrapper) {
    list.removeActionListener(this);
    list.setSelectedItem(wrapper);
    list.addActionListener(this);
  }

  /**
   * Handles user selecting an item from the list.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {

    Object selectedItem = list.getSelectedItem();

    if (selectedItem == lastSelectedItem)
      return;

    unassignPushOptions();

    Filter oldFilter = filter;

    if (selectedItem == emptySelection) {

      filter = null;

    } else if (selectedItem != emptySelection) {

      String value = null;
      Option option = ((OptionWrapper) selectedItem).option;
      if (option != null) {
        String tmp = option.getValue();
        if (tmp != null && !"".equals(tmp)) {
          value = tmp;
        }

        if (value != null) {

          filter =
            new InputPageAwareBasicFilter(
              filterDescription.getField(),
              option.getTableConstraint(),
              "=",
              value,
              this);
        }

        setNodeLabel(
          null,
          filterDescription.getField() + " = " + option.getValue());

        assignPushOptions(option.getPushActions());
      }
    }

    lastSelectedItem = selectedItem;
    updateQueryFilters(oldFilter, filter);
  }

  /**
   * Removes items from list and adds the empty selection to the empty selection.
   * @param list list to reset
   * @param options array of options to add to list. If null the list is reset to
   * contain just the emptySelection entry.
   * @return map from option.value to the item in the list it corresponds to. 
   */
  private Map resetList(JComboBox list, Option[] options) {

    Map valueToItem = new HashMap();

    //  must stop listening otherwise propertyChange() is called fore every change we make
    // to the list.
    list.removeActionListener(this);

    list.removeAllItems();

    // make first option be empty
    list.addItem(emptySelection);

    // Add any options to list
    if (options != null && options.length > 0) {

      valueToItem = new HashMap();

      for (int i = 0; i < options.length; i++) {

        Option option = options[i];

        String value = option.getValue();
        String field = option.getField();
        if (BaseNamedConfigurationObject.isInvalid(value) && BaseNamedConfigurationObject.isInvalid(field))
          throw new RuntimeException("Invalid option = " + option);

        // add each option, via a surrogate, to the list. 
        OptionWrapper ow = new OptionWrapper(option);
        valueToItem.put(value, ow);
        list.addItem(ow);

      }

    }

    list.addActionListener(this);

    return valueToItem;
  }

  /**
   * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
   */
  public void setOptions(Option[] options) {

    unassignPushOptions();

    removeFilterFromQuery( filter );

    filter = null;

    filterValueToItem = resetList(list, options);

  }

}
