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
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.ColumnComponent;
import org.biomart.common.resources.Resources;

/**
 * This context is basically the same as {@link DataSetContext}, except
 * it only provides context menus and adaptations for {@link DataSetColumn}
 * instances.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class ExplainTransformationContext extends DataSetContext {
	/**
	 * Creates a new context that will adapt objects according to the settings
	 * in the specified dataset.
	 * 
	 * @param martTab
	 *            the mart tab this context appears in.
	 * @param dataset
	 *            the dataset this context will use for customising menus and
	 *            colours.
	 */
	public ExplainTransformationContext(final MartTab martTab,
			final DataSet dataset) {
		super(martTab, dataset);
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {

		// Columns.
		if (object instanceof DataSetColumn) {

			// Which column is it?
			final DataSetColumn column = (DataSetColumn) object;

			// Magenta EXPRESSION columns.
			if (column instanceof InheritedColumn)
				component.setBackground(ColumnComponent.INHERITED_COLOUR);
			// Fade out all MASKED columns.
			else if (((DataSet)column.getTable().getSchema()).getDataSetModifications().isMaskedColumn(column))
				component.setBackground(ColumnComponent.FADED_COLOUR);
		
			// Blue PARTITIONED columns.
			else if (((DataSet)column.getTable().getSchema()).getDataSetModifications().isPartitionedColumn(column))
				component.setBackground(ColumnComponent.PARTITIONED_COLOUR);

			// FIXME: Reinstate.
			/*

			// Magenta EXPRESSION columns.
			else if (column instanceof ExpressionColumn)
				component.setBackground(ColumnComponent.EXPRESSION_COLOUR);
			*/

			// All others are normal.
			else
				component.setBackground(ColumnComponent.NORMAL_COLOUR);
		}
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

		// Column menu goes here.
		if (object instanceof DataSetColumn) {
			// Add separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Work out which column has been clicked.
			final DataSetColumn column = (DataSetColumn) object;

			// Rename the column.
			final JMenuItem rename = new JMenuItem(Resources
					.get("renameColumnTitle"));
			rename.setMnemonic(Resources.get("renameColumnMnemonic").charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainTransformationContext.this.getMartTab()
							.getDataSetTabSet().requestRenameDataSetColumn(
									column);
				}
			});
			contextMenu.add(rename);

			contextMenu.addSeparator();

			// Mask the column.
			final boolean isMasked = ((DataSet)column.getTable().getSchema()).getDataSetModifications().isMaskedColumn(column);
			final boolean isPartitioned = ((DataSet)column.getTable().getSchema()).getDataSetModifications().isPartitionedColumn(column);
			final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(Resources
					.get("maskColumnTitle"));
			mask.setMnemonic(Resources.get("maskColumnMnemonic").charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (mask.isSelected())
						ExplainTransformationContext.this.getMartTab()
								.getDataSetTabSet().requestMaskColumn(
										ExplainTransformationContext.this
												.getDataSet(), column);
					else
						ExplainTransformationContext.this.getMartTab()
								.getDataSetTabSet().requestUnmaskColumn(
										ExplainTransformationContext.this
												.getDataSet(), column);
				}
			});
			contextMenu.add(mask);
			mask.setSelected(isMasked);
			if (isPartitioned)
				mask.setEnabled(false);

			contextMenu.addSeparator();

			// If it is partitioned, make a submenu to change the partition
			// type.
			if (isPartitioned) {

				// The option to change the partition type.
				final JMenuItem changepartition = new JMenuItem(
						Resources.get("changePartitionColumnTitle"),
						new ImageIcon(
								Resources
										.getResourceAsURL("expandAll.gif")));
				changepartition.setMnemonic(Resources.get(
						"changePartitionColumnMnemonic").charAt(0));
				changepartition.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainTransformationContext.this.getMartTab()
								.getDataSetTabSet().requestPartitionByColumn(
										ExplainTransformationContext.this
												.getDataSet(), column);
					}
				});
				contextMenu.add(changepartition);

			}

			// If it is not partitioned, allow the user to turn partitioning
			// on.
			else {

				// Option to enable partitioning.
				final JMenuItem partition = new JMenuItem(
						Resources.get("partitionColumnTitle"),
						new ImageIcon(
								Resources
										.getResourceAsURL("expandAll.gif")));
				partition.setMnemonic(Resources.get("partitionColumnMnemonic")
						.charAt(0));
				partition.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						ExplainTransformationContext.this.getMartTab()
								.getDataSetTabSet().requestPartitionByColumn(
										ExplainTransformationContext.this
												.getDataSet(), column);
					}
				});
				contextMenu.add(partition);
				if (isMasked)
					partition.setEnabled(false);
			}

			// The option to turn off partitioning.
			final JMenuItem unpartition = new JMenuItem(Resources
					.get("unpartitionColumnTitle"));
			unpartition.setMnemonic(Resources.get("unpartitionColumnMnemonic")
					.charAt(0));
			unpartition.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainTransformationContext.this.getMartTab()
							.getDataSetTabSet().requestUnpartitionByColumn(
									ExplainTransformationContext.this
											.getDataSet(), column);
				}
			});
			contextMenu.add(unpartition);
			if (!isPartitioned)
				unpartition.setEnabled(false);

			// FIXME: Reinstate.
			/*
			// Else, if it's an expression column...
			if (column instanceof ExpressionColumn) {

				contextMenu.addSeparator();

				// Option to modify column.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifyExpressionColumnTitle"));
				modify.setMnemonic(Resources.get(
						"modifyExpressionColumnMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainTransformationContext.this.getMartTab()
								.getDataSetTabSet()
								.requestModifyExpressionColumn(
										(ExpressionColumn) column);
					}
				});
				contextMenu.add(modify);

				// Option to remove column.
				final JMenuItem remove = new JMenuItem(Resources
						.get("removeExpressionColumnTitle"));
				remove.setMnemonic(Resources.get(
						"removeExpressionColumnMnemonic").charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainTransformationContext.this.getMartTab()
								.getDataSetTabSet()
								.requestRemoveExpressionColumn(
										(ExpressionColumn) column);
					}
				});
				contextMenu.add(remove);

			}
			*/
		}
	}
}
