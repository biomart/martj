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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.DSAttributeGroup;

/**
 * Widget representing an AttributePage.
 */
public class AttributePageWidget extends PageWidget {

  private final Logger logger = Logger.getLogger( AttributePageWidget.class.getName() );

	private AttributePage page;
	/**
	 * @param name
	 * @param query
	 */
	public AttributePageWidget(Query query, String name, AttributePage page) {

		super(name, query);

		this.page = page;
    

    
    
		List attributeGroups = page.getAttributeGroups();
    for (Iterator iter = attributeGroups.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if ( element instanceof AttributeGroup ) {
        AttributeGroup group = (AttributeGroup)element;
        String groupName = group.getDisplayName();
    
        AttributeGroupWidget w = new AttributeGroupWidget( query, groupName, group );
        tabbedPane.add( groupName, w);  
        leafWidgets.addAll( w.getLeafWidgets() );
			}
      else if ( element instanceof DSAttributeGroup ) {
        // TODO handle DSAttributeGroup
        logger.warning( "TODO: handle DSAttributeGroup: " + element.getClass().getName() );
        // create page
        // add pag as tab
      }
      else {
        throw new RuntimeException( "Unrecognised type in attribute group list: " 
                                    + element);
      }
      
		}
  }

}
