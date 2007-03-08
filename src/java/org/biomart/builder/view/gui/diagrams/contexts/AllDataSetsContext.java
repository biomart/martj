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

import javax.swing.ImageIcon;
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
 * @since 0.1
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
			if (((DataSet) object).getInvisible())
				component.setBackground(DataSetComponent.INVISIBLE_BACKGROUND);
			else
				component.setBackground(DataSetComponent.VISIBLE_BACKGROUND);

			((DataSetComponent) component).setRenameable(true);
			((DataSetComponent) component).setSelectable(true);
		}
	}

	public void populateMultiContextMenu(final JPopupMenu contextMenu,
			final Collection selectedItems, final Class clazz) {
		// Menu for multiple dataset objects.
		if (DataSet.class.isAssignableFrom(clazz)) {
			// Visible/invisible
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

		// DataSet objects have different menus to the background.
		if (object instanceof DataSet) {

			// Add a separator if the menu is not already empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// What schema is this?
			final DataSet dataset = (DataSet) object;

			// Add an option to rename this dataset.
			final JMenuItem rename = new JMenuItem(Resources
					.get("renameDataSetTitle"));
			rename
					.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(
							0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					AllDataSetsContext.this.martTab.getDataSetTabSet()
							.requestRenameDataSet(dataset);
				}
			});
			contextMenu.add(rename);

			// Add an option to replicate this dataset.
			final JMenuItem replicate = new JMenuItem(Resources
					.get("replicateDataSetTitle"));
			replicate.setMnemonic(Resources.get("replicateDataSetMnemonic")
					.charAt(0));
			replicate.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					AllDataSetsContext.this.martTab.getDataSetTabSet()
							.requestReplicateDataSet(dataset);
				}
			});
			contextMenu.add(replicate);

			// Option to remove the dataset from the mart.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeDataSetTitle"), new ImageIcon(Resources
					.getResourceAsURL("cut.gif")));
			remove
					.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(
							0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					AllDataSetsContext.this.martTab.getDataSetTabSet()
							.requestRemoveDataSet(dataset);
				}
			});
			contextMenu.add(remove);
		}
	}
}
