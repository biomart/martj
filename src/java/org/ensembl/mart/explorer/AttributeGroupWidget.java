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
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.UIAttributeDescription;

/**
 * Widget representing an AttibuteGroup. 
 */
public class AttributeGroupWidget extends InputPage {

	private AttributeGroup group;

	/**
	 * @param query
	 * @param name
	 */
	public AttributeGroupWidget(Query query, String name, AttributeGroup group) {

		super(name, query);

		this.group = group;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		addCollections(this, group.getAttributeCollections());

	}

	/**
	 * @param collections
	 */
	private void addCollections(
		Container container,
		AttributeCollection[] collections) {

		for (int i = 0; i < collections.length; i++) {

			AttributeCollection collection = collections[i];
			JPanel collectionPanel = new JPanel();
			collectionPanel.setBorder(new TitledBorder(collection.getDisplayName()));
			container.add(collectionPanel);

			addAttributes(collectionPanel, collection.getUIAttributeDescriptions());

		}
	}

	/**
	 * @param collectionPanel
	 * @param attributes
	 */
	private void addAttributes(Container collectionPanel, List attributes) {
		for (Iterator iter = attributes.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof UIAttributeDescription) {
				UIAttributeDescription attributeDescription = (UIAttributeDescription) element;
        collectionPanel.add( new AttributeDescriptionWidget( query, attributeDescription ) );
			} else {
				throw new RuntimeException(
					"Unsupported attribute description: " + element);
			}

		}

	}

}
