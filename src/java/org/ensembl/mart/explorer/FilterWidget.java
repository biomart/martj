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

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * Base class for FilterWidgets.
 */
public abstract class FilterWidget extends InputPage
implements PropertyChangeListener {

  protected UIFilterDescription filterDescription;

	/**
	 * @param query
	 * @param name
	 */
	public FilterWidget(Query query, UIFilterDescription filterDescription) {
		
    super(query, filterDescription.getDisplayName() );
    this.filterDescription = filterDescription;
    
    
	}
  

  /**
   * @return
   */
  public UIFilterDescription getFilterDescription() {
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

}
