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

import java.beans.PropertyChangeListener;

import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * Base class for FilterWidgets.
 */
public abstract class FilterWidget extends InputPage
implements PropertyChangeListener {

  protected String fieldName;

	protected FilterGroupWidget filterGroupWidget;

	protected FilterDescription filterDescription;

	/**
	 * @param query
	 * @param name
	 */
	public FilterWidget(FilterGroupWidget filterGroupWidget, Query query, FilterDescription filterDescription) {
		
    super(query, filterDescription.getDisplayName() );
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
  
    query.removePropertyChangeListener( this );
  
    if ( oldFilter!=null && newFilter!=null ) query.replaceFilter( oldFilter, newFilter);
    else if ( newFilter!=null ) query.addFilter( newFilter );
    else if ( oldFilter!=null ) query.removeFilter( oldFilter );
    
    query.addPropertyChangeListener( this );    
  
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

}
