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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.ensembl.mart.lib.NullableFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * A boolean filter widget has a description and three radio buttons;
 * "require", "ignore", "irrelevant". If the state of these buttons is 
 * synchronised with that of the query.
 */
public class BooleanFilterWidget extends FilterWidget 
implements PropertyChangeListener , ActionListener{

  private JRadioButton require = new JRadioButton("require");
  private JRadioButton exclude = new JRadioButton("exclude");
  private JRadioButton irrelevant = new JRadioButton("irrelevant");
  
  private Object currentButton = null;
  
  private NullableFilter filter;
  private String type;
  

  /**
   * @param query
   * @param filterDescription
   */
  public BooleanFilterWidget(Query query, UIFilterDescription filterDescription) {
    super(query, filterDescription);
    
    this.type = filterDescription.getType().intern();
    irrelevant.setSelected( true );
    currentButton = irrelevant;
    
    ButtonGroup group = new ButtonGroup();
    group.add( require );
    group.add( exclude );
    group.add( irrelevant );
    
    require.addActionListener( this );
    exclude.addActionListener( this );
    irrelevant.addActionListener( this );
    
    Box panel = Box.createHorizontalBox();
    panel.add( new JLabel( filterDescription.getDisplayName() ) );
    panel.add( Box.createHorizontalGlue() );
    panel.add( require );
    panel.add( exclude );
    panel.add( irrelevant );
    
    add( panel );
  }

  /**
   * Respond to a change in the query if necessary.
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    // TODO Auto-generated method stub
    System.out.println( "change " + evt);
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent evt) {
    
    // user clicked currently selected button
    if ( evt.getSource()==currentButton ) return;
    
    currentButton = evt.getSource();
    
    System.out.println( "action " + evt.getSource());
    
    if ( currentButton==require ) addRequireFilter();
    else if ( currentButton==exclude ) addExcludeFilter();
    else removeFilter();
    
  }

  /**
   * 
   */
  private void removeFilter() {
    // TODO Auto-generated method stub
    
  }

  /**
   * 
   */
  private void addExcludeFilter() {
    String condition =
      (type == "boolean") ? NullableFilter.isNULL : NullableFilter.isNULL_NUM;
    
    // TODO refactor the first and last line into the propertyChange block.
    NullableFilter oldFilter = filter;
    setFilter(
      new NullableFilter(
        filterDescription.getFieldName(),
        filterDescription.getTableConstraint(),
        condition));
    updateQueryFilters( oldFilter, filter );
  }

  /**
   * @param filter
   */
  private void setFilter(NullableFilter filter) {
    // TODO Auto-generated method stub
    this.filter = filter;
    
  }

  /**
   * 
   */
  private void addRequireFilter() {
    // TODO Auto-generated method stub
    
  }

}
