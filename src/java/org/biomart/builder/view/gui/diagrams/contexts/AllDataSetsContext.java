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

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
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
		// Nothing to do here.
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

		// The background area of the diagram has some simple menu items
		// that refer to all datasets.
		if (object == null) {

			// Add a separator if the menu is not already empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Gray out if there are no datasets.
			boolean grayOut = this.martTab.getDataSetTabSet().getTabCount() <= 1;

			// Option to remove all datasets from the mart.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeAllDataSetsTitle"), new ImageIcon(Resources
					.getResourceAsURL("cut.gif")));
			remove.setMnemonic(Resources.get("removeAllDataSetsMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					AllDataSetsContext.this.martTab.getDataSetTabSet()
							.requestRemoveAllDataSets();
				}
			});
			if (grayOut)
				remove.setEnabled(false);
			contextMenu.add(remove);

		}

		// DataSet objects have different menus to the background.
		else if (object instanceof DataSet) {

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

			contextMenu.addSeparator();

			// Add an option to make this dataset invisible.
			final JCheckBoxMenuItem invisible = new JCheckBoxMenuItem(Resources
					.get("invisibleDataSetTitle"));
			invisible.setMnemonic(Resources.get("invisibleDataSetMnemonic")
					.charAt(0));
			invisible.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (invisible.isSelected())
						AllDataSetsContext.this.martTab.getDataSetTabSet()
								.requestInvisibleDataSet(dataset);
					else
						AllDataSetsContext.this.martTab.getDataSetTabSet()
								.requestVisibleDataSet(dataset);
				}
			});
			if (dataset.getInvisible())
				invisible.setSelected(true);
			contextMenu.add(invisible);

			contextMenu.addSeparator();

			// Option to explain how the dataset was constructed.
			final JMenuItem explain = new JMenuItem(Resources
					.get("explainDataSetTitle"), new ImageIcon(Resources
					.getResourceAsURL("help.gif")));
			explain.setMnemonic(Resources.get("explainDataSetMnemonic").charAt(
					0));
			explain.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					AllDataSetsContext.this.getMartTab().getDataSetTabSet()
							.requestExplainDataSet(dataset);
				}
			});
			contextMenu.add(explain);

			contextMenu.addSeparator();

			// Option to create the DDL for the dataset.
			final JMenuItem saveDDL = new JMenuItem(Resources
					.get("saveDDLTitle"), new ImageIcon(Resources
					.getResourceAsURL("saveText.gif")));
			saveDDL.setMnemonic(Resources.get("saveDDLMnemonic").charAt(0));
			saveDDL.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					AllDataSetsContext.this.getMartTab().getDataSetTabSet()
							.requestCreateDDL(dataset);
				}
			});
			contextMenu.add(saveDDL);
		}
	}
}
