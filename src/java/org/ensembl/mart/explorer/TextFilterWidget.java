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

// TODO bring this page to front if node selected

package org.ensembl.mart.explorer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * Widget with a label and text entry area which adds/removes
 * a corresponding <code>Filter</code> object from the query. Entering text folled by
 * <code>return</code> causes a filter to be added or changed. Clearing the text
 * and pressing <code>return</code> removes the filter.
 */
public class TextFilterWidget
  extends FilterWidget
  implements ActionListener, PropertyChangeListener {

  private JTextField textField;
  private BasicFilter filter;


	/**
	   * BooleanFilter that has contains a tree node.
	   */
	private class InputPageAwareBasicFilter
		extends BasicFilter
		implements InputPageAware {
		private InputPage inputPage;

		public InputPageAwareBasicFilter(
			String field,
			String condition,
			String value,
			InputPage inputPage) {

			super(field, condition, value);
			this.inputPage = inputPage;

		}

		public InputPageAwareBasicFilter(
			String field,
			String tableConstraint,
			String condition,
			String value,
			InputPage inputPage) {

			super(field, tableConstraint, condition, value);
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
  public TextFilterWidget(Query query, FilterDescription filterDescription) {

    super(query, filterDescription);
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    textField = new JTextField(5);
    textField.addActionListener(this); // listen for user entered changes
    query.addPropertyChangeListener(this); // listen for changes to query
    add(new JLabel(filterDescription.getDisplayName()));
    add(textField);
    add(Box.createHorizontalGlue());
  }

  /**
   * Update query when text filter change. Adds filter to query
   * when text entered first time, changes filter if text changed, 
   * removes filter if text cleared.
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void actionPerformed(ActionEvent e) {

    String value = textField.getText();

    // remove filter
    if (value == null || "".equals(value)) {
      if (filter != null) {
        query.removeFilter(filter);
        filter = null;
        setField(null);
        setNodeLabel(null, null);
      }
      return;
    }

    // Add / change filter
    BasicFilter old = filter;
    setFilter(
      new InputPageAwareBasicFilter(
        filterDescription.getFieldName(),
        filterDescription.getTableConstraint(),
        filterDescription.getQualifier(),
        value,
        this ));

    updateQueryFilters( old, filter );

  }

  /**
   * @param filter
   */
  private void addFilterToQuery(BasicFilter filter) {
    // TODO Auto-generated method stub
    
  }

  /**
   * Update text field when relevant Filter is added or
   * removed from query. Filter is relevant if 
   * <code>filter.getFieldName().equals(  )</code>
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {

    if (evt.getSource() == query && "filter".equals(evt.getPropertyName())) {

      Object newValue = evt.getNewValue();
      Object oldValue = evt.getOldValue();

      // a relevant filter has been added to query 
      if (newValue != null
        && newValue instanceof BasicFilter
        && oldValue == null) {

        BasicFilter f = (BasicFilter) newValue;
        if ( relevantFilter(f) ) setFilter(f);

      }

      // a relevant filter has been removed from the query
      if (newValue == null
        && oldValue != null
        && oldValue instanceof BasicFilter) {

        BasicFilter f = (BasicFilter) oldValue;
        if ( relevantFilter(f) ) setFilter( null );

      }

    }
  }



  private boolean relevantFilter(BasicFilter f) {
    return f.getField().equals( filterDescription.getFieldName() );
  }

  private void setFilter(BasicFilter filter) {

    this.filter = filter;

    String rhs = null;
    if (filter != null)
      rhs =
        filterDescription.getDisplayName()
          + filterDescription.getQualifier()
          + filter.getValue();
    setNodeLabel(null, rhs);
    
    // Must do this BEFORE adding the filter
    // to the query because the QueryEditor, which reponds
    // to Query property changes, uses the field as a key 
    // to look up THIS page in order to get it's node for
    // displaying in the tree.
    setField(filter);
  }
  
  /**
   * Does nothing.
   * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
   */
  public void setOptions(Option[] options) {
  }

}
