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

import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FilterDescriptionWidget extends InputPage{

  protected UIFilterDescription filterDescription;
  protected Filter filter;

	/**
	 * @param query
	 * @param name
	 */
	public FilterDescriptionWidget(Query query, UIFilterDescription filterDescription) {
		
    super(query, filterDescription.getDisplayName() );
    this.filterDescription = filterDescription;
    
		// TODO Auto-generated constructor stub
    
	}
  

  /**
   * @return
   */
  public UIFilterDescription getFilterDescription() {
    return filterDescription;
  }

  /**
   * @return
   */
  public Filter getFilter() {
    return filter;
  }

}
