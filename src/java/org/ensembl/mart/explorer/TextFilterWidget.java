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

// TODO add clear button
// TODO remove node from tree if cleared
// TODO bring this page to front if node selected

package org.ensembl.mart.explorer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TextFilterWidget extends FilterDescriptionWidget 
implements ChangeListener, PropertyChangeListener  {

  private LabelledComboBox combo;
  private BasicFilter filter;

  /**
   * @param query
   * @param filterDescription
   */
  public TextFilterWidget(Query query, UIFilterDescription filterDescription) {
    
    super(query, filterDescription);
    
    
    combo = new LabelledComboBox( filterDescription.getDisplayName() );
    combo.addChangeListener( this ); // listen for user entered changes
    query.addPropertyChangeListener( this ); // listen for changes to query
    add( combo  );
  }

  
    /**
     * Update query when text filter change. Adds filter to query
     * when text entered first time, changes filter if text changed, 
     * removes filter if text cleared.
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
      
      String value = combo.getSelectedItem().toString();
      
      // remove filter
      if ( value==null || "".equals( value ) ) {
        if ( filter!=null ) {
          query.removeFilter( filter );
          filter = null;
          setField( null );
        }
        return;
      } 
      
      // Add / change filter
      BasicFilter old = filter;
      filter = new BasicFilter( filterDescription.getFieldName(),
                                filterDescription.getTableConstraint(),
                                filterDescription.getQualifier(), 
                                value);
      setNodeLabel( null,
        filterDescription.getDisplayName()
          + filterDescription.getQualifier()
          + value);
      // Must do this BEFORE adding the filter
      // to the query because the QueryEditor, which reponds
      // to Query property changes, uses the field as a key 
      // to look up THIS page in order to get it's node for
      // displaying in the tree.
      setField( filter );
       
      if ( old==null ) {
        query.addFilter( filter );
      } else {
        query.replaceFilter(old, filter);
      }
    }


    /**
     * Update combo when query changes.
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
      // TODO Auto-generated method stub
      
    }

}
