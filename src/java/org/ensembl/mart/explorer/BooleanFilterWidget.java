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

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.ensembl.mart.lib.BooleanFilter;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * A boolean filter widget has a description and three radio buttons;
 * "require", "ignore", "irrelevant". The state of these buttons is 
 * synchronised with that of the query.
 */
public class BooleanFilterWidget
	extends FilterWidget
	implements ActionListener {

	private Box panel = Box.createHorizontalBox();

	private JRadioButton require = new JRadioButton("require");
	private JRadioButton exclude = new JRadioButton("exclude");
	private JRadioButton irrelevant = new JRadioButton("irrelevant");
	private JComboBox list = null;

  private BooleanFilter filter;

	private Object currentButton = null;

	private String requireCondition;
	private String excludeCondition;

	/**
	 * BooleanFilter that has contains an InputPage, this page is used by the QueryEditor
	 * when it detects the filter has been added or removed from the query.
	 */
	private class InputPageAwareNullableFilter
		extends BooleanFilter
		implements InputPageAware {
		private InputPage inputPage;

		public InputPageAwareNullableFilter(
			String field,
			String condition,
			InputPage inputPage) {
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
	public BooleanFilterWidget(
		FilterGroupWidget filterGroupWidget,
		Query query,
		FilterDescription filterDescription) {
		super(filterGroupWidget, query, filterDescription);

		if ("boolean".equals(filterDescription.getType())) {
			requireCondition = BooleanFilter.isNotNULL;
			excludeCondition = BooleanFilter.isNULL;
		} else {
			requireCondition = BooleanFilter.isNotNULL_NUM;
			excludeCondition = BooleanFilter.isNULL_NUM;
		}

		irrelevant.setSelected(true);
		currentButton = irrelevant;

		ButtonGroup group = new ButtonGroup();
		group.add(require);
		group.add(exclude);
		group.add(irrelevant);

		require.addActionListener(this);
		exclude.addActionListener(this);
		irrelevant.addActionListener(this);

		panel.add(new JLabel(filterDescription.getDisplayName()));
		if (list != null)
			panel.add(list);
		panel.add(Box.createHorizontalGlue());
		panel.add(require);
		panel.add(exclude);
		panel.add(irrelevant);
		add(panel);

		// adds list component if necessary  
		setOptions(filterDescription.getOptions());

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

		if (currentButton == require)
			resetFilter(requireCondition);
		else if (currentButton == exclude)
			resetFilter(excludeCondition);
		else
			filter = null;

		updateQueryFilters(oldFilter, filter);
	}

	/**
	 * @param filter
	 */
	private void resetFilter(String condition) {

		this.filter =
			new InputPageAwareNullableFilter(
				filterDescription.getField(),
				filterDescription.getTableConstraint(),
				condition,
				this);

		setNodeLabel(
			null,
			filterDescription.getField() + filter.getRightHandClause());
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
	 */
	public void setOptions(Option[] options) {

		if (options == null) {
			// TODO remove list.
		} else if (options.length > 0) {

			if (list == null) {
				list = new JComboBox();
				list.addActionListener(this);
				panel.add(list, 1); 
			}

      // TODO add options to list.
      
		}

	}



  /**
   * Sets one of the radio button depending on the filter provided.
   * @see org.ensembl.mart.explorer.FilterWidget#setFilter(org.ensembl.mart.lib.Filter)
   */
  protected void setFilter(Filter filter) {
    this.filter = (BooleanFilter)filter;
    setNodeLabel(
      null,
      filterDescription.getField() + filter.getRightHandClause());
      
    if ( filter==null ) {
      irrelevant.setSelected( true );
    }
    else if ( filter.getValue().equals( requireCondition ) ) {
      require.setSelected( true );
    }
    else if ( filter.getValue().equals( excludeCondition ) ) {
      exclude.setSelected( true );
    } else {
      throw new RuntimeException( "Unsupported value for a boolean filter: " +        filter.getValue() );
    }
  }

}
