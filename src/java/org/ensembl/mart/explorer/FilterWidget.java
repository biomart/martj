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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.PushOptions;

/**
 * Base class for FilterWidgets.
 */
public abstract class FilterWidget
	extends InputPage
	implements PropertyChangeListener {

	private final static Logger logger =
		Logger.getLogger(FilterWidget.class.getName());

	protected String fieldName;

	protected FilterGroupWidget filterGroupWidget;

	protected FilterDescription filterDescription;

	/**
	 * @param query
	 * @param name
	 */
	public FilterWidget(
		FilterGroupWidget filterGroupWidget,
		Query query,
		FilterDescription filterDescription) {

		super(query, filterDescription.getDisplayName());
		this.filterDescription = filterDescription;
		this.filterGroupWidget = filterGroupWidget;
		this.fieldName = filterDescription.getField();

	}

	/**
	 * @return
	 */
	public FilterDescription getFilterDescription() {
		return filterDescription;
	}

	/**
	   * Updates the filter(s) on the query. Removes oldFilter if set and
	   * adds newFilter if set.
	   * @param oldFilter
	   * @param newFilter
	   */
	protected void updateQueryFilters(Filter oldFilter, Filter newFilter) {

		query.removePropertyChangeListener(this);

		if (oldFilter != null && newFilter != null)
			query.replaceFilter(oldFilter, newFilter);
		else if (newFilter != null)
			query.addFilter(newFilter);
		else if (oldFilter != null)
			query.removeFilter(oldFilter);

		query.addPropertyChangeListener(this);

	}

	public abstract void setOptions(Option[] options);

	protected OptionWrapper emptySelection = new OptionWrapper(null);

	/**
	 * Holds an Option and returns option.getDisplayName() from
	 * toString(). This class is used to add Options to the
	 * combo box.
	 */
	protected class OptionWrapper {
		protected Option option;

		protected OptionWrapper(Option option) {
			this.option = option;
		}

		public String toString() {
			return (option == null) ? "No Filter" : option.getDisplayName();
		}
	}

	/**
	 * validates strings, checking if not null and not empty.
	 * @param s
	 * @return true if string is not null and not empty
	 */
	public static final boolean isInvalid(String s) {
		return s == null && "".equals(s);
	}

	protected PushOptionsHandler[] pushOptionHandlers;

	/**
	   * Removes all options from the push targets.
	   */
	protected void unassignPushOptions() {

		int n = (pushOptionHandlers == null) ? 0 : pushOptionHandlers.length;
		for (int i = 0; i < n; i++)
			pushOptionHandlers[i].remove();

	}

	/**
	   * @param pushs
	   */
	protected void assignPushOptions(PushOptions[] optionPushes) {

		pushOptionHandlers = new PushOptionsHandler[optionPushes.length];

		for (int i = 0; i < optionPushes.length; i++) {
			pushOptionHandlers[i] =
				new PushOptionsHandler(optionPushes[i], filterGroupWidget);
			pushOptionHandlers[i].push();
			//System.out.println( "Pushing options" + optionPushes[i]);
		}
	}

	/**
	   * Responds to the removal or addition of relevant filters from the query. Updates the state of
	   * this widget by changing the currently selected item in the list.
	   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	   */
	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getSource() == query) {

			if (evt.getPropertyName().equals("filter")) {

				//				Object oldValue = evt.getOldValue();
				//				
				//				if (oldValue != null
				//					&& oldValue instanceof Filter
				//					&& (f = (Filter) oldValue).getField().equals(fieldName)) {
				//
				//					setFilter(null);
				//				}
				//
				//				Object newValue = evt.getNewValue();
				//				if (newValue != null
				//					&& newValue instanceof Filter
				//					&& (f = (Filter) newValue).getField().equals(fieldName)) {
				//
				//					System.out.println("filedName=>" + fieldName + "<" + f);
				//					setFilter(f);
				//				}

				
        Filter oldFilter = equivalentFilter( evt.getOldValue() );
				if (oldFilter != null ) setFilter(null);
				

				Filter newFilter = equivalentFilter( evt.getNewValue() );
				if (newFilter != null ) setFilter( newFilter );
				

			}
		}

	}

	protected final Filter equivalentFilter(Object possibleFilter) {
		Filter filter = null;
		if ( fieldName != null 
      && !"".equals(fieldName)
      && possibleFilter != null
			&& possibleFilter instanceof Filter
			&& (filter = (Filter) possibleFilter).getField().equals(fieldName)) {
			return filter;
		} else {
			return null;
		}
	}

	protected abstract void setFilter(Filter filter);
	protected void removeFilterFromQuery(Filter filter) {

		if (filter != null) {

			query.removePropertyChangeListener(this);
			query.removeFilter(filter);
			query.addPropertyChangeListener(this);
			filter = null;
		}
	} /**
				 * BasicFilter containing an InputPage, this page is used by the QueryEditor
				 * when it detects the filter has been added or removed from the query.
				 */
	static class InputPageAwareBasicFilter
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
}
