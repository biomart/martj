/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.builder.view.gui.diagrams.contexts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.DataSetComponent;
import org.biomart.common.resources.Resources;

/**
 * Provides the context menus and colour schemes to use when viewing the all
 * datasets tab.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class AllDataSetsContext implements DiagramContext {
	private MartTab martTab;

	/**
	 * Creates a new context which will pass any menu actions onto the given
	 * mart tab.
	 * 
	 * @param martTab
	 *            the mart tab which will receive any menu actions the user
	 *            selects.
	 */
	public AllDataSetsContext(final MartTab martTab) {
		this.martTab = martTab;
	}

	/**
	 * Obtain the mart tab to pass menu events onto.
	 * 
	 * @return the mart tab this context is attached to.
	 */
	protected MartTab getMartTab() {
		return this.martTab;
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {
		if (object instanceof DataSet) {

			// Set the background colour.
			if (((DataSet) object).isPartitionTable())
				component.setBackground(DataSetComponent.PARTITION_BACKGROUND);
			else if (((DataSet) object).isMasked())
				component.setBackground(DataSetComponent.MASKED_BACKGROUND);
			else if (((DataSet) object).isInvisible())
				component.setBackground(DataSetComponent.INVISIBLE_BACKGROUND);
			else
				component.setBackground(DataSetComponent.VISIBLE_BACKGROUND);

			// Update dotted line (partitioned).
			((DataSetComponent) component)
					.setRestricted(this.getMartTab().getMart()
							.getPartitionTableForDataSet((DataSet) object) != null);

			((DataSetComponent) component).setRenameable(true);
			((DataSetComponent) component).setSelectable(true);
		}
	}

	public void populateMultiContextMenu(final JPopupMenu contextMenu,
			final Collection selectedItems, final Class clazz) {
		// Menu for multiple dataset objects.
		if (DataSet.class.isAssignableFrom(clazz)) {
			// Visible.
			final JMenuItem visible = new JMenuItem(Resources
					.get("uninvisibleGroupDataSetTitle"));
			visible.setMnemonic(Resources
					.get("uninvisibleGroupDataSetMnemonic").charAt(0));
			visible.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					for (final Iterator i = selectedItems.iterator(); i
							.hasNext();) {
						final DataSet ds = (DataSet) i.next();
						AllDataSetsContext.this.getMartTab().getDataSetTabSet()
								.requestVisibleDataSet(ds);
					}
				}
			});
			contextMenu.add(visible);

			// Invisible.
			final JMenuItem invisible = new JMenuItem(Resources
					.get("invisibleGroupDataSetTitle"));
			invisible.setMnemonic(Resources
					.get("invisibleGroupDataSetMnemonic").charAt(0));
			invisible.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					for (final Iterator i = selectedItems.iterator(); i
							.hasNext();) {
						final DataSet ds = (DataSet) i.next();
						AllDataSetsContext.this.getMartTab().getDataSetTabSet()
								.requestInvisibleDataSet(ds);
					}
				}
			});
			contextMenu.add(invisible);
		}
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {
		// Nothing extra to do.
	}
}
