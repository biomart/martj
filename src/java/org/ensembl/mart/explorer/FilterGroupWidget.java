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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JScrollPane;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FilterGroupWidget extends PageWidget {

  private Logger logger = Logger.getLogger(FilterGroupWidget.class.getName());

  private FilterGroup group;

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
        new GridPanel(attributes, 2, 25, collection.getDisplayName());
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

      if (element instanceof UIFilterDescription) {

        UIFilterDescription a = (UIFilterDescription) element;
        FilterPageSetWidget.TYPES.add( a.getType() );
        FilterDescriptionWidget w = new FilterDescriptionWidget(query, a);
        pages.add(w);
      } 
      else if (element instanceof UIDSFilterDescription) {

        logger.warning("TODO Unsupported domain specific filter description: " + element.getClass().getName() + element);
      }
      else {
        logger.severe(  "Unrecognised filter: " +  element.getClass().getName() + element); 
      }
      
    }

    return (InputPage[]) pages.toArray(new InputPage[pages.size()]);
	}

}
