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
import java.util.regex.Matcher;

import javax.swing.Box;
import javax.swing.JScrollPane;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatasetConfig;

/**
 * Widget representing an AttibuteGroup.
 */
public class AttributeGroupWidget extends GroupWidget {

	private final static Logger logger = Logger
			.getLogger(AttributeGroupWidget.class.getName());

	private int lastWidth;

	private AttributeGroup group;

	private AttributePage page;

	/**
	 * @param query
	 * @param name
	 */
	public AttributeGroupWidget(Query query, String name, AttributeGroup group,
			AttributePage page, QueryTreeView tree, DatasetConfig dsv,
			AdaptorManager manager) {

		super(name, query, tree);

		this.group = group;
		this.page = page;

		Box panel = Box.createVerticalBox();
		leafWidgets = addCollections(panel, group.getAttributeCollections(),
				dsv, manager);
		panel.add(Box.createVerticalGlue());

		add(new JScrollPane(panel));

	}

	/**
	 * @param collections
	 */
	private List addCollections(Container container,
			AttributeCollection[] collections, DatasetConfig dsv,
			AdaptorManager manager) {

		List widgets = new ArrayList();
		
		
		
		for (int i = 0; i < collections.length; i++) {

			if (tree.skipConfigurationObject(collections[i]))
				continue;

			if (group.getInternalName().equals("sequence")) {
				if (collections[i].getInternalName().matches(
						"\\w*seq_scope\\w*")) {
					SequenceGroupWidget w = new SequenceGroupWidget(
							collections[i].getDisplayName(), collections[i]
									.getInternalName(), query, tree, dsv,
							manager);
					widgets.add(w);
					container.add(w);
				} else
					continue;
			} else {
				AttributeCollection collection = collections[i];
				InputPage[] attributes = getAttributeWidgets(collection,
						manager, dsv);
				widgets.addAll(Arrays.asList(attributes));
				GridPanel p = new GridPanel(attributes, 2, 200, 35, collection
						.getDisplayName());
				container.add(p);
			}
		}
		return widgets;
	}

	/**
	 * Converts collection.UIAttributeDescriptions into InputPages.
	 * 
	 * @param collection
	 * @return array of AttributeDescriptionWidgets, one for each
	 *         AttributeDescription in the collection.
	 */
private InputPage[] getAttributeWidgets(AttributeCollection collection, AdaptorManager manager, DatasetConfig dsv) 
{

    List attributeDescriptions = collection.getAttributeDescriptions();
    List pages = new ArrayList();

    for (Iterator iter = attributeDescriptions.iterator(); iter.hasNext();) 
    {
      Object element = iter.next();
      
      if (element instanceof AttributeDescription) 
      {

        AttributeDescription a = (AttributeDescription) element;
        if (tree.skipConfigurationObject(a)) continue;
        
        if (a.getInternalName().indexOf('.') > 0) 
        {
        	String[] info = a.getInternalName().split("\\.");
            String dname = info[0];
            String aname = info[1];
            String main_dataset = dsv.getDataset(); // returns data set name hsapiens_gene_ensembl
            
        	if (dname.compareTo(main_dataset) == 0) /// check if its  a self pointing place holder
        	{        		     		
        		AttributePage  attpage_PH = dsv.getPageForAttribute(aname);
        		AttributeCollection collection_PH = attpage_PH.getCollectionForAttributeDescription(aname);  		        		

        		List attributeDescriptions_PH = collection_PH.getAttributeDescriptions();
        	    
        		List pages_PH = new ArrayList();

        	    for (Iterator iter_PH = attributeDescriptions_PH.iterator(); iter_PH.hasNext();) 
        	    {
        	      Object element_PH = iter_PH.next();
        	      
        	      if (element_PH instanceof AttributeDescription) 
        	      {

        	        AttributeDescription a_PH = (AttributeDescription) element_PH;
        	        if (tree.skipConfigurationObject(a_PH)) continue;
        	       
        	        if(a_PH.getInternalName().compareTo(aname) == 0) // means same
        	        {
        	            a.setInternalName(a_PH.getInternalName());
	        	        a.setDisplayName(a_PH.getDisplayName());
	        	        a.setField(a_PH.getField());
	        	        a.setTableConstraint(a_PH.getTableConstraint());	        	 
	        	        a.setKey(a_PH.getKey());
	        	        break;
        	        }
        	      }
        	    }
        	}                 	
        	else 
        	{
        		a.setDisplayName(manager.getPointerAttribute(a.getInternalName()).getDisplayName());
        		a.setField(a.getInternalName());
        		a.setTableConstraint(a.getInternalName());
        	}
        }
        
        AttributeDescriptionWidget w = new AttributeDescriptionWidget(query, a, tree);
        pages.add(w);
      }
      else {

        logger.severe("Unsupported attribute description: " +  element.getClass().getName() + element);
      }
    }

    return (InputPage[]) pages.toArray(new InputPage[pages.size()]);

  }
}
