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

import javax.swing.JCheckBox;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.UIAttributeDescription;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AttributeDescriptionWidget
	extends InputPage
	implements PropertyChangeListener {

	private UIAttributeDescription attributeDescription;
	private Query query;
	private Attribute attribute;
	private JCheckBox button;

	/**
	 * @param query
	 * @param name
   */
	public AttributeDescriptionWidget(
		final Query query,
		UIAttributeDescription attributeDescription) {

		super(query, attributeDescription.getDisplayName());

		this.attributeDescription = attributeDescription;
		this.query = query;

		attribute =
			new FieldAttribute(
				attributeDescription.getFieldName(),
				attributeDescription.getTableConstraint());
    setField( attribute );
    
		button = new JCheckBox(attributeDescription.getDisplayName());
		button.addActionListener(new ActionListener() {
			
      public void actionPerformed(ActionEvent event) {
        
        // TODO add / remove from tree (and inputPanel / use same name as attribute panel?)
        if ( button.isSelected() ) query.addAttribute(attribute);
        else query.removeAttribute(attribute);

			}
		});

		query.addPropertyChangeListener(this);

		add(button);
	}

  public Attribute getAttribute() {
    return attribute;
  }

	/** 
	 * Listens to changes in query. If an attribute corresponding to this widget is added then
	 * this the state of this widget is set to selected. If such an attribute is removed then
	 * the state is set to deselected.
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {

    // respond to property changes if propertyName=="attribute" 
    // and changed value is of type FieldAttribute and it matches
    // and the changed value's field matches attribute.field

		if (evt.getSource() == query) {

			if (evt.getPropertyName().equals("attribute")) {

				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();

				// attribute removed
				if (oldValue == null
					&& newValue != null
					&& newValue instanceof FieldAttribute) {

					FieldAttribute tmp = (FieldAttribute) newValue;
					if (tmp.getField().equals(attribute.getField())) {
						button.setSelected( true );

					}
				}

				// attribute removed
				else if (
					oldValue != null
						&& newValue == null
						&& oldValue instanceof FieldAttribute) {

					FieldAttribute tmp = (FieldAttribute) oldValue;
					if (tmp.getField().equals(attribute.getField())) {
						button.setSelected( false );

					}
				}

			}
		}

	}

	
	
}
