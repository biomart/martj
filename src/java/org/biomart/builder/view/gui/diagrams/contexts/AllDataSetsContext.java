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

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * Provides the context menus and colour schemes to use when viewing the all
 * datasets tab.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 20th July 2006
 * @since 0.1
 */
public class AllDataSetsContext implements DiagramContext {
	private MartTab martTab;

	/**
	 * Creates a new context which will pass any menu actions onto the given
	 * dataset tabset.
	 * 
	 * @param martTab
	 *            the mart tab which will receive any menu actions the user
	 *            selects.
	 */
	public AllDataSetsContext(MartTab martTab) {
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

	public void populateContextMenu(JPopupMenu contextMenu, Object object) {

		// The background area of the diagram has some simple menu items
		// that refer to all schemas.
		if (object == null) {

			// Nothing, yet.

		}

		// DataSet objects have different menus to the background.
		else if (object instanceof DataSet) {

			// Add a separator if the menu is not already empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// What schema is this?
			final DataSet dataset = (DataSet) object;

			// Option to remove the dataset from the mart.
			JMenuItem remove = new JMenuItem(Resources
					.get("removeDataSetTitle"));
			remove
					.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(
							0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getDataSetTabSet().requestRemoveDataSet(dataset);
				}
			});
			contextMenu.add(remove);

			// Add an option to rename this dataset.
			JMenuItem rename = new JMenuItem(Resources
					.get("renameDataSetTitle"));
			rename
					.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(
							0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getDataSetTabSet().requestRenameDataSet(dataset);
				}
			});
			contextMenu.add(rename);

			// Add an option to replicate this dataset.
			JMenuItem replicate = new JMenuItem(Resources
					.get("replicateDataSetTitle"));
			replicate.setMnemonic(Resources.get("replicateDataSetMnemonic")
					.charAt(0));
			replicate.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					martTab.getDataSetTabSet().requestReplicateDataSet(dataset);
				}
			});
			contextMenu.add(replicate);

			// Option to create the DDL for the dataset.
			JMenuItem saveDDL = new JMenuItem(Resources.get("saveDDLTitle"));
			saveDDL.setMnemonic(Resources.get("saveDDLMnemonic").charAt(0));
			saveDDL.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestCreateDDL(dataset);
				}
			});
			contextMenu.add(saveDDL);
		}
	}

	public void customiseAppearance(JComponent component, Object object) {
		// Nothing needs doing here.
	}
}
