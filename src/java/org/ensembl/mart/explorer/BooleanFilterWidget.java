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

import org.ensembl.mart.lib.BooleanFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * A boolean filter widget has a description and three radio buttons;
 * "require", "ignore", "irrelevant". The state of these buttons is 
 * synchronised with that of the query.
 */
public class BooleanFilterWidget extends FilterWidget 
implements PropertyChangeListener , ActionListener{

  private JRadioButton require = new JRadioButton("require");
  private JRadioButton exclude = new JRadioButton("exclude");
  private JRadioButton irrelevant = new JRadioButton("irrelevant");
  
  private Object currentButton = null;
  
  private BooleanFilter filter;
  private String requireCondition;
  private String excludeCondition;
  

  /**
   * BooleanFilter that has contains an InputPage, this page is used by the QueryEditor
   * when it detects the filter has been added or removed from the query.
   */
  private class InputPageAwareNullableFilter extends BooleanFilter implements InputPageAware {
    private InputPage inputPage;

		public InputPageAwareNullableFilter(String field, String condition, InputPage inputPage) {
			super(field, condition);
			this.inputPage = inputPage;
		}

		public InputPageAwareNullableFilter(
			String field,
			String tableConstraint,
			String condition,
      InputPage inputPage) {
			super(field, tableConstraint, condition);
			this.inputPage = inputPage;
		}

    public InputPage getInputPage() {
      return inputPage;
    }
  }

  /**
   * @param query
   * @param filterDescription
   */
  public BooleanFilterWidget(FilterGroupWidget filterGroupWidget, Query query, FilterDescription filterDescription) {
    super(filterGroupWidget, query, filterDescription);
    
    
    if ( "boolean".equals( filterDescription.getType() ) ) {
      requireCondition = BooleanFilter.isNotNULL;
      excludeCondition = BooleanFilter.isNULL;
    } else {
      requireCondition = BooleanFilter.isNotNULL_NUM;
      excludeCondition = BooleanFilter.isNULL_NUM;
    }
    
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
		if (evt.getSource() == currentButton)
			return;

		currentButton = evt.getSource();

		BooleanFilter oldFilter = filter;

		if (currentButton == require) resetFilter( requireCondition );
    else if (currentButton == exclude) resetFilter( excludeCondition );
    else filter = null;

		updateQueryFilters(oldFilter, filter);
	}

	/**
	 * @param filter
	 */
	private void resetFilter(String condition) {

		this.filter =
			new InputPageAwareNullableFilter(
				filterDescription.getFieldName(),
				filterDescription.getTableConstraint(),
				condition,
        this );

		setNodeLabel(
			null,
			filterDescription.getFieldName() + filter.getRightHandClause());
	}



  /* (non-Javadoc)
   * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
   */
  public void setOptions(Option[] options) {
    // TODO Auto-generated method stub

  }

}
