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

import java.util.logging.Logger;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.PushAction;

/**
 * Base class for FilterWidgets.
 */
public abstract class FilterWidget extends InputPage {

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
  protected void assignPushOptions(PushAction[] optionPushes) {

    pushOptionHandlers = new PushOptionsHandler[optionPushes.length];

    for (int i = 0; i < optionPushes.length; i++) {
      pushOptionHandlers[i] =
        new PushOptionsHandler(optionPushes[i], filterGroupWidget);
      pushOptionHandlers[i].push();
    }
  }

  /**
   * @return true if otherfilter has the same fieldName
   */
  protected boolean equivalentFilter(Object possibleFilter) {
    return fieldName != null
      && !"".equals(fieldName)
      && possibleFilter != null
      && possibleFilter instanceof Filter
      && ((Filter) possibleFilter).getField()!=null
      && ((Filter) possibleFilter).getField().equals(fieldName);
  }

  protected abstract void setFilter(Filter filter);

  /**
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

    public InputPageAwareBasicFilter(Option option, InputPage inputPage) {
      super(
        option.getFieldFromContext(),
        option.getTableConstraintFromContext(),
        option.getQualifierFromContext(),
        option.getValueFromContext(),
        option.getHandlerFromContext());
      this.inputPage = inputPage;
    }

    public InputPage getInputPage() {
      return inputPage;
    }
  }

  /** 
   * Responds to the addition of a relevant filter to the query by 
   * updates the state of widget to reflect the filter.
   * @see org.ensembl.mart.lib.QueryChangeListener#filterAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void filterAdded(Query sourceQuery, int index, Filter filter) {
    if (equivalentFilter(filter))
      setFilter(filter);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#filterChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.Filter, org.ensembl.mart.lib.Filter)
   */
  public void filterChanged(
    Query sourceQuery,
    Filter oldFilter,
    Filter newFilter) {
  }

  /**
   * Responds to the removal of a relevant filters from the query by 
   * updates the state of widget to reflect the filter.
   * @see org.ensembl.mart.lib.QueryChangeListener#filterRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void filterRemoved(Query sourceQuery, int index, Filter filter) {
    if (equivalentFilter(filter))
      setFilter(null);
  }

}
