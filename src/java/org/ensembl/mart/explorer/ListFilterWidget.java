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
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.OptionPush;

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

	private Filter filter;

	private Map filterValueToItem;

	private OptionPusher[] optionPushers;

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

		String field = filterDescription.getFieldName();
		if (field == null || "".equals(field))
			throw new RuntimeException(
				"field invalid: >"
					+ field
					+ "<\nfilterDescritoion = "
					+ filterDescription);

		setOptions(filterDescription.getOptions());

	}

	/**
	 * Responds to the removal or addition of relevant filters from the query. Updates the state of
	 * this widget by changing the currently selected item in the list.
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getSource() == query) {

			if (evt.getPropertyName().equals("filter")) {

				Object oldValue = evt.getOldValue();
				Filter f;
				if (oldValue != null
					&& oldValue instanceof Filter
					&& (f = (Filter) oldValue).getField().equals(fieldName)) {
					removeFilter(f);
				}

				Object newValue = evt.getNewValue();
				if (newValue != null
					&& newValue instanceof Filter
					&& (f = (Filter) newValue).getField().equals(fieldName)) {
					setFilter(f);
				}
			}
		}

	}

	/**
	 * Sets this,filter=filter and selects the appropriate item in the list.
	 * @param filter
	 */
	private void setFilter(Filter filter) {

		this.filter = filter;
		setSelectedItem((OptionWrapper) filterValueToItem.get(filter.getValue()));

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
	 * Remove the filter; sets this.filter to null and resets the 
	 * selected item to "No Filter".
	 * @param filter
	 */
	private void removeFilter(Filter filter) {

		if (filter != this.filter)
			throw new RuntimeException("Widget is out of sync with query, filters should be the same");

		this.filter = null;

		setSelectedItem(emptySelection);

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

			removePushOptions( optionPushers );
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

			pushOptions(option.getOptionPushes());
		}

	}

	/**
	 * Removes all options from the push targets.
	 */
	private void removePushOptions(OptionPusher[] optionPushers) {

		int n = (optionPushers == null) ? 0 : optionPushers.length;
		for (int i = 0; i < n; i++)
			optionPushers[i].remove();

	}

	/**
	 * @param pushs
	 */
	private void pushOptions(OptionPush[] optionPushes) {

		optionPushers = new OptionPusher[optionPushes.length];

		for (int i = 0; i < optionPushes.length; i++) {
			optionPushers[i] = new OptionPusher(optionPushes[i], filterGroupWidget);
			optionPushers[i].push();
		}
	}

	private void removeFilter() {
	
  	if (filter != null) {

			query.removePropertyChangeListener(this);
			query.removeFilter(filter);
			query.addPropertyChangeListener(this);
			filter = null;

		}

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
    if (options != null && options.length>0 ) {

      valueToItem = new HashMap();

      for (int i = 0; i < options.length; i++) {

        Option option = options[i];

        String value = option.getValue();
        if (value == null || "".equals(value))
          throw new RuntimeException(
            "Option.value invalid: >" + value + "<\noption = " + option);

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

		removePushOptions( optionPushers );

		removeFilter();

    filterValueToItem = resetList( list, options );
		
	}


}
