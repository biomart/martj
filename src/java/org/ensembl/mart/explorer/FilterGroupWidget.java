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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JScrollPane;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.MapFilterDescription;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FilterGroupWidget extends PageWidget {

  private Logger logger = Logger.getLogger(FilterGroupWidget.class.getName());

  private FilterGroup group;
  
  private Map internalNameToLeafWidget = new HashMap();

	/**
	 * @param name
	 * @param query
	 */
	public FilterGroupWidget(Query query, String name, FilterGroup group) {
		super(query, name);
		
    this.group = group;

    Box panel = Box.createVerticalBox();
    leafWidgets = addCollections(panel, group.getFilterCollections());
    panel.add(Box.createVerticalGlue());
    
    add(new JScrollPane(panel));
    
	}

	/**
	 * @param panel
	 * @param collections
	 * @return
	 */
	private List addCollections(Box panel, FilterCollection[] collections) {
    List widgets = new ArrayList();

    for (int i = 0; i < collections.length; i++) {

      FilterCollection collection = collections[i];
      InputPage[] attributes = getFilterWidgets(collection);
      widgets.addAll(Arrays.asList(attributes));
      GridPanel p =
        new GridPanel(attributes, 1, 400, 35, collection.getDisplayName());
      panel.add(p);

    }
    return widgets;
	}

	/**
	 * @param collection
	 * @return
	 */
	private InputPage[] getFilterWidgets(FilterCollection collection) {
    List filterDescriptions = collection.getUIFilterDescriptions();
    List pages = new ArrayList();

    for (Iterator iter = filterDescriptions.iterator(); iter.hasNext();) {
      Object element = iter.next();

      if (element instanceof FilterDescription || element instanceof MapFilterDescription) {

        FilterDescription a = (FilterDescription) element;
        FilterPageSetWidget.TYPES.add( a.getType() );
        
        //FilterWidget w = new FilterWidget(query, a);
        FilterWidget w = createFilterWidget(query, a);
        if ( w!=null ) 
          pages.add(w);
      } 
      else {
        logger.severe(  "Unrecognised filter: " +  element.getClass().getName() + element); 
      }
      
    }

    return (InputPage[]) pages.toArray(new InputPage[pages.size()]);
	}


  private FilterWidget createFilterWidget(
    Query query,
    FilterDescription filterDescription) {
      
    String type = filterDescription.getType();
    FilterWidget w = null;

    if ("text_entry".equals(type)) {
    
      w = new TextFilterWidget( this, query, filterDescription );
    
    } else if ("list".equals(type)) {
      
      w = new ListFilterWidget( this, query, filterDescription );
      
    } else if ("range".equals(type)) {
    
    } else if ("boolean".equals(type) || "boolean_num".equals(type) ) {
    
      w = new BooleanFilterWidget( this, query, filterDescription );
    
    } else if ("text_entry_basic_filter".equals(type) || "drop_down_basic_filter".equals(type) ) {
    
      w = new ListFilterWidget( this, query, filterDescription );
    
    }
    
    
    if ( w!=null ) {
      
      internalNameToLeafWidget.put( filterDescription.getInternalName(), w);
       
    }else {
    
      logger.warning("Unsupported filter: " 
      + filterDescription.getClass().getName()
      + ", " + filterDescription );
    
    }
    
    return w;   
  }

	/**
	 * @param string
	 * @return
	 */
	public FilterWidget getFilterWidget( String internalName ) {
    return (FilterWidget)internalNameToLeafWidget.get( internalName );
	}

}
