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
import org.ensembl.mart.lib.config.AttributeDescription;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AttributeDescriptionWidget
  extends InputPage {

  private AttributeDescription attributeDescription;
  private Query query;
  private Attribute attribute;
  private JCheckBox button;

  /**
   * BooleanFilter containing an InputPage, this page is used by the QueryEditor
   * when it detects the filter has been added or removed from the query.
   */
  private class InputPageAwareAttribute
    extends FieldAttribute
    implements InputPageAware {

    private InputPage inputPage;

    public InputPageAwareAttribute(String field, InputPage inputPage) {
      super(field);
      this.inputPage = inputPage;
    }

    public InputPageAwareAttribute(
      String field,
      String tableConstraint,
      InputPage inputPage) {
      super(field, tableConstraint);
      this.inputPage = inputPage;
    }

    public InputPage getInputPage() {
      return inputPage;
    }
  }

  /**
   * @param query
   * @param name
   */
  public AttributeDescriptionWidget(
    final Query query,
    AttributeDescription attributeDescription) {

    super(query, attributeDescription.getDisplayName());

    this.attributeDescription = attributeDescription;
    this.query = query;

    attribute =
      new InputPageAwareAttribute(
        attributeDescription.getField(),
        attributeDescription.getTableConstraint(),
        this);
    setField(attribute);

    button = new JCheckBox(attributeDescription.getDisplayName());
    button.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent event) {

        if (button.isSelected())
          query.addAttribute(attribute);
        else
          query.removeAttribute(attribute);

      }
    });

    query.addQueryChangeListener(this);

    add(button);
  }

  public Attribute getAttribute() {
    return attribute;
  }


  /** 
   * If the attribute added corresponds to this widget then show it is
   * selected.
   * @see org.ensembl.mart.lib.QueryChangeListener#attributeAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void attributeAdded(
    Query sourceQuery,
    int index,
    Attribute attribute) {

    if (this.attribute.getField().equals(attribute.getField()))
      button.setSelected(true);

  }

  /**
   * If removed attribute corresponds to this widget then show 
   * it is not selected.
   * @see org.ensembl.mart.lib.QueryChangeListener#attributeRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void attributeRemoved(
    Query sourceQuery,
    int index,
    Attribute attribute) {

    if (this.attribute.getField().equals(attribute.getField()))
      button.setSelected(false);
  }

}
