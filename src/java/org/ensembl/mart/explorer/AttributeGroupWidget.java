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
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.UIAttributeDescription;

/**
 * Widget representing an AttibuteGroup. 
 */
public class AttributeGroupWidget extends GroupWidget {

  private int lastWidth;
	private final static int NUM_COLUMNS = 2;
  private final static int ROW_HEIGHT = 25;

	private AttributeGroup group;
  

	/**
	 * @param query
	 * @param name
	 */
	public AttributeGroupWidget(Query query, String name, AttributeGroup group) {

		super(name, query);

		this.group = group;

    setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) ); 
    Box panel = Box.createVerticalBox();
    leafWidgets = addCollections(panel, group.getAttributeCollections());
    add( new JScrollPane( panel ) );
    add( Box.createVerticalGlue() );
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
			Box rows = Box.createVerticalBox();
			rows.setBorder(new TitledBorder(collection.getDisplayName()));
			container.add( rows );

			widgets.addAll( addAttributes( rows, collection.getUIAttributeDescriptions()) );
       
      rows.addComponentListener( new ComponentListener() {

				public void componentResized(ComponentEvent e) {
					resizeAttributes();
				}

				public void componentMoved(ComponentEvent e) {}
				public void componentShown(ComponentEvent e) {}
				public void componentHidden(ComponentEvent e) {}
        
      });
      }
      
      return widgets;
		}
    
    
	/**
	 * Resizes leafWidgets if the width of this component has changed.
	 */
	private void resizeAttributes() {

		final int width = getSize().width;

		if (width != lastWidth) {
			
      int noScrollWidth = width/NUM_COLUMNS - 10;
			int height = ROW_HEIGHT;
			Dimension size = new Dimension(noScrollWidth, height);

			for (int i = 0, n = leafWidgets.size(); i < n; i++) {
        InputPage w 
        = (InputPage)leafWidgets.get(i);
				w.setPreferredSize(size);
				w.setMinimumSize(size);
				w.setMaximumSize(size);
				w.invalidate();
			}

			lastWidth = width;
		}
	}

	

	/**
   * Adds attributes in rows where each each row is one "pair".
   * <pre>
   * att1 att2
   * att3 att4
   * att5 glue
   * </pre>
	* @ param collectionPanel
	* @ param attributes
	*/ 
	private List addAttributes(Box rows, List attributes) {

    List widgets = new ArrayList();

		for (Iterator iter = attributes.iterator(); iter.hasNext();) {

			Box row = Box.createHorizontalBox();
  
			for (int column = 0; column < NUM_COLUMNS; ++column ) {

				if (iter.hasNext()) {

					Object element = iter.next();
					
          if (element instanceof UIAttributeDescription) {
					
          	UIAttributeDescription a = (UIAttributeDescription) element;
            AttributeDescriptionWidget w = new AttributeDescriptionWidget(query, a); 
            widgets.add( w );
						row.add( w );
            
					
          } else {
					
          	throw new RuntimeException(
							"Unsupported attribute description: " + element);
					
          }
          
        } else {
				
        	// pad column 
					row.add(Box.createHorizontalGlue());
				
        }
			}

			rows.add(row);

		}
  
    return widgets;

	}

}
