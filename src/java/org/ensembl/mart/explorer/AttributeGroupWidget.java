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

import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JScrollPane;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;

/**
 * Widget representing an AttibuteGroup. 
 */
public class AttributeGroupWidget extends GroupWidget {

  private final static Logger logger =
    Logger.getLogger(AttributeGroupWidget.class.getName());

  private int lastWidth;

  private AttributeGroup group;

  /**
   * @param query
   * @param name
   */
  public AttributeGroupWidget(Query query, String name, AttributeGroup group) {

    super(name, query);

    this.group = group;

    Box panel = Box.createVerticalBox();
    leafWidgets = addCollections(panel, group.getAttributeCollections());
    panel.add(Box.createVerticalGlue());
    
    add(new JScrollPane(panel));
    
  }

  /**
   * @param collections
   */
  private List addCollections(
    Container container,
    AttributeCollection[] collections) {

    List widgets = new ArrayList();

    for (int i = 0; i < collections.length; i++) {

      AttributeCollection collection = collections[i];
      InputPage[] attributes = getAttributeWidgets(collection);
      widgets.addAll(Arrays.asList(attributes));
      GridPanel p =
        new GridPanel(attributes, 2, 25, collection.getDisplayName());
      container.add(p);

    }
    return widgets;
  }

  /**
   * Converts collection.UIAttributeDescriptions into InputPages.
   * @param collection
   * @return array of AttributeDescriptionWidgets, one for each 
   * UIAttributeDescription in the collection.
   */
  private InputPage[] getAttributeWidgets(AttributeCollection collection) {

    List attributeDescriptions = collection.getUIAttributeDescriptions();
    List pages = new ArrayList();

    for (Iterator iter = attributeDescriptions.iterator(); iter.hasNext();) {
      Object element = iter.next();

      if (element instanceof UIAttributeDescription) {

        UIAttributeDescription a = (UIAttributeDescription) element;
        AttributeDescriptionWidget w = new AttributeDescriptionWidget(query, a);
        pages.add(w);
      } else if ( element instanceof UIDSFilterDescription ){
        logger.warning("TODO Unsupported domain specific attribute description: " + element.getClass().getName() + element);
      }
      else {

        logger.severe("Unsupported attribute description: " +  element.getClass().getName() + element);
      }
    }

    return (InputPage[]) pages.toArray(new InputPage[pages.size()]);

  }

}
