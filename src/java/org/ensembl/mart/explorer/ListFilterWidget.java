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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.OptionPush;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * Represents a list of user options. Some options cause the
 * options available in other widgets to be modified.
 */
public class ListFilterWidget extends FilterWidget implements ActionListener {

  /**
   * BasicFilter containing an InputPage, this page is used by the QueryEditor
   * when it detects the filter has been added or removed from the query.
   */
  private class InputPageAwareBasicFilter
    extends BasicFilter
    implements InputPageAware {

    private InputPage inputPage;

    public InputPageAwareBasicFilter(
      String field,
      String condition,
      String value,
      InputPage inputPage) {
      this(field, null, condition, value, inputPage);
    }

    public InputPageAwareBasicFilter(
      String field,
      String tableConstraint,
      String condition,
      String value,
      InputPage inputPage) {
      super(field, tableConstraint, condition, value);
      this.inputPage = inputPage;
    }

    public InputPage getInputPage() {
      return inputPage;
    }
  }

  private OptionPush[] activePushOptions;

  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

  private JComboBox list;
  private Filter filter;
  private Object lastSelectedItem;
  private OptionWrapper emptySelection = new OptionWrapper(null);

  /**
   * Holds an Option and returns option.getDisplayName() from
   * toString(). This class is used to add Options to the
   * combo box.
   */
  private class OptionWrapper {
    private Option option;

    private OptionWrapper(Option option) {
      this.option = option;
    }

    public String toString() {
      return (option == null) ? "No Filter" : option.getDisplayName();
    }
  }

  /**
   * @param query model to bv synchronised
   * @param filterDescription parameters for this widget
   */
  public ListFilterWidget(Query query, FilterDescription filterDescription) {
    super(query, filterDescription);

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

    String field = filterDescription.getFieldName();
    if (field == null || "".equals(field))
      throw new RuntimeException(
        "field invalid: >"
          + field
          + "<\nfilterDescritoion = "
          + filterDescription);

    // make first option be empty
    list.addItem(emptySelection);

    // add each option, via a surrogate, to the list.     
    Option[] options = filterDescription.getOptions();
    for (int i = 0; i < options.length; i++) {

      Option option = options[i];

      String value = option.getValue();
      if (value == null || "".equals(value))
        throw new RuntimeException(
          "Option.value invalid: >" + value + "<\noption = " + option);

      list.addItem(new OptionWrapper(option));
    }

  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    // TODO Auto-generated method stub

  }

  /**
   * Handles user selecting an item from the list.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    Object selectedItem = list.getSelectedItem();

    if (selectedItem == lastSelectedItem
      || (lastSelectedItem == null && selectedItem == emptySelection))
      return;

    if (lastSelectedItem != emptySelection) {
      query.removeFilter(filter);

      removePushOptions();
    }

    lastSelectedItem = selectedItem;

    if (selectedItem != emptySelection) {

      Option option = ((OptionWrapper) selectedItem).option;

      filter =
        new InputPageAwareBasicFilter(
          filterDescription.getFieldName(),
          option.getTableConstraint(),
          "=",
          option.getValue(),
          this);
      query.addFilter(filter);

      setNodeLabel(
        null,
        filterDescription.getFieldName() + " = " + option.getValue());

      setupPushOptions( option.getOptionPushes() );
    }

  }

  /**
   * 
   */
  private void removePushOptions() {
    // TODO removePushOptions()
    
  }

  /**
   * @param pushs
   */
  private void setupPushOptions(OptionPush[] pushOptions) {
    this.activePushOptions = pushOptions;
        for (int i = 0; i < pushOptions.length; i++) {
      OptionPush op = pushOptions[i];
      // TODO setupPushOptions(OptionPush[] pushs)
      //FilterWidget widget = getWidget( op.getRef() );
      //widget.setOptions( op.getOptions() );
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
   */
  public void setOptions(Option[] options) {
    // TODO Auto-generated method stub

  }

}
