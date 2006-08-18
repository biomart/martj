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

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.ColumnComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * This context adapts dataset org.biomart.builder.view.gui.diagrams to display
 * different colours, and provides the context menu for interacting with dataset
 * org.biomart.builder.view.gui.diagrams.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.32, 18th August 2006
 * @since 0.1
 */
public class DataSetContext extends WindowContext {
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
	public DataSetContext(final MartTab martTab, final DataSet dataset) {
		super(martTab, dataset);
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

		// Did the user click on a dataset object?
		if (object instanceof DataSet) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Add an option to make this dataset invisible.
			final JCheckBoxMenuItem invisible = new JCheckBoxMenuItem(Resources
					.get("invisibleDataSetTitle"));
			invisible.setMnemonic(Resources.get("invisibleDataSetMnemonic")
					.charAt(0));
			invisible.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (invisible.isSelected())
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestInvisibleDataSet(
										DataSetContext.this.getDataSet());
					else
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestVisibleDataSet(
										DataSetContext.this.getDataSet());
				}
			});
			if (this.getDataSet().getInvisible())
				invisible.setSelected(true);
			contextMenu.add(invisible);

			contextMenu.addSeparator();

			// Option to rename the dataset.
			final JMenuItem rename = new JMenuItem(Resources
					.get("renameDataSetTitle"));
			rename
					.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(
							0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestRenameDataSet(
									DataSetContext.this.getDataSet());
				}
			});
			contextMenu.add(rename);

			// Option to remove the dataset from the mart.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeDataSetTitle"), new ImageIcon(Resources
					.getResourceAsURL("org/biomart/builder/resources/cut.gif")));
			remove
					.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(
							0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestRemoveDataSet(
									DataSetContext.this.getDataSet());
				}
			});
			contextMenu.add(remove);

			contextMenu.addSeparator();

			// Make a group for the different optimiser types.
			final ButtonGroup optGroup = new ButtonGroup();

			// The no-optimiser option turns post-construction optimisation off.
			final JRadioButtonMenuItem optNone = new JRadioButtonMenuItem(
					Resources.get("optimiserNoneTitle"));
			optNone.setMnemonic(Resources.get("optimiserNoneMnemonic")
					.charAt(0));
			optNone.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestChangeOptimiserType(
									DataSetContext.this.getDataSet(),
									DataSetOptimiserType.NONE);
				}
			});
			optGroup.add(optNone);
			contextMenu.add(optNone);
			if (this.getDataSet().getDataSetOptimiserType().equals(
					DataSetOptimiserType.NONE))
				optNone.setSelected(true);

			// The column option turns on has-column optimisation.
			final JRadioButtonMenuItem optCol = new JRadioButtonMenuItem(
					Resources.get("optimiserColumnTitle"));
			optCol.setMnemonic(Resources.get("optimiserColumnMnemonic").charAt(
					0));
			optCol.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestChangeOptimiserType(
									DataSetContext.this.getDataSet(),
									DataSetOptimiserType.COLUMN);
				}
			});
			optGroup.add(optCol);
			contextMenu.add(optCol);
			if (this.getDataSet().getDataSetOptimiserType().equals(
					DataSetOptimiserType.COLUMN))
				optCol.setSelected(true);

			// The table option turns on table-of-has-column optimisation.
			final JRadioButtonMenuItem optTbl = new JRadioButtonMenuItem(
					Resources.get("optimiserTableTitle"));
			optTbl.setMnemonic(Resources.get("optimiserTableMnemonic")
					.charAt(0));
			optTbl.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestChangeOptimiserType(
									DataSetContext.this.getDataSet(),
									DataSetOptimiserType.TABLE);
				}
			});
			optGroup.add(optTbl);
			contextMenu.add(optTbl);
			if (this.getDataSet().getDataSetOptimiserType().equals(
					DataSetOptimiserType.TABLE))
				optTbl.setSelected(true);

			contextMenu.addSeparator();

			// Option to create the DDL for the dataset.
			final JMenuItem saveDDL = new JMenuItem(
					Resources.get("saveDDLTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/saveText.gif")));
			saveDDL.setMnemonic(Resources.get("saveDDLMnemonic").charAt(0));
			saveDDL.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestCreateDDL(DataSetContext.this.getDataSet());
				}
			});
			contextMenu.add(saveDDL);
		}

		// Did the user click on a dataset table?
		else if (object instanceof DataSetTable) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Work out which table we are dealing with, and what type it is.
			final DataSetTable table = (DataSetTable) object;
			final DataSetTableType tableType = table.getType();

			// Option to explain how the table was constructed.
			final JMenuItem explain = new JMenuItem(
					Resources.get("explainTableTitle"),
					new ImageIcon(
							Resources
									.getResourceAsURL("org/biomart/builder/resources/help.gif")));
			explain
					.setMnemonic(Resources.get("explainTableMnemonic")
							.charAt(0));
			explain.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestExplainTable(table);
				}
			});
			contextMenu.add(explain);

			contextMenu.addSeparator();

			// Rename the table.
			final JMenuItem rename = new JMenuItem(Resources
					.get("renameTableTitle"));
			rename.setMnemonic(Resources.get("renameTableMnemonic").charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestRenameDataSetTable(table);
				}
			});
			contextMenu.add(rename);

			// Add an expression column.
			final JMenuItem expression = new JMenuItem(Resources
					.get("addExpressionColumnTitle"));
			expression.setMnemonic(Resources.get("addExpressionColumnMnemonic")
					.charAt(0));
			expression.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestAddExpressionColumn(table);
				}
			});
			contextMenu.add(expression);

			contextMenu.addSeparator();

			// Dimension tables have their own options.
			if (tableType.equals(DataSetTableType.DIMENSION)) {

				// The dimension can be merged by using this option. This
				// simply changes the relation cardinality to 1:1. This
				// affects ALL datasets, not just this one!
				final JMenuItem mergeDM = new JMenuItem(
						Resources.get("mergeDimensionTitle"),
						new ImageIcon(
								Resources
										.getResourceAsURL("org/biomart/builder/resources/collapseAll.gif")));
				mergeDM.setMnemonic(Resources.get("mergeDimensionMnemonic")
						.charAt(0));
				mergeDM.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getSchemaTabSet()
								.requestChangeRelationCardinality(
										table.getSourceRelation(),
										Cardinality.ONE);
					}
				});
				contextMenu.add(mergeDM);

				// The dimension can be removed by using this option. This
				// simply masks the relation that caused the dimension to exist.
				final JMenuItem removeDM = new JMenuItem(
						Resources.get("removeDimensionTitle"),
						new ImageIcon(
								Resources
										.getResourceAsURL("org/biomart/builder/resources/cut.gif")));
				removeDM.setMnemonic(Resources.get("removeDimensionMnemonic")
						.charAt(0));
				removeDM.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestMaskRelation(
										DataSetContext.this.getDataSet(),
										table.getSourceRelation());
					}
				});
				contextMenu.add(removeDM);
			}

			// Subclass tables have their own options too.
			else if (tableType.equals(DataSetTableType.MAIN_SUBCLASS)) {

				// The subclass table can be removed by using this option. This
				// simply masks the relation that caused the subclass to exist.
				final JMenuItem unsubclass = new JMenuItem(
						Resources.get("removeSubclassTitle"),
						new ImageIcon(
								Resources
										.getResourceAsURL("org/biomart/builder/resources/collapseAll.gif")));
				unsubclass.setMnemonic(Resources.get("removeSubclassMnemonic")
						.charAt(0));
				unsubclass.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestUnsubclassRelation(
										DataSetContext.this.getDataSet(),
										table.getSourceRelation());
					}
				});
				contextMenu.add(unsubclass);
			}

			// Main tables have their own stuff as well.
			else if (tableType.equals(DataSetTableType.MAIN)) {
				// Suggest invisible datasets.
				final JMenuItem invisible = new JMenuItem(Resources
						.get("suggestInvisibleDatasetsTitle"));
				invisible.setMnemonic(Resources.get(
						"suggestInvisibleDatasetsMnemonic").charAt(0));
				invisible.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestSuggestInvisibleDatasets(
										DataSetContext.this.getDataSet(), table);
					}
				});
				contextMenu.add(invisible);
			}
		}

		// Keys have menus too.
		else if (object instanceof Key) {
			// Keys behave as though the table had been clicked on.
			final Table table = ((Key) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}

		// Column menu goes here.
		else if (object instanceof DataSetColumn) {
			// Columns first show the stuff that would have showed
			// had the table been clicked on.
			final Table table = ((DataSetColumn) object).getTable();
			this.populateContextMenu(contextMenu, table);

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
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestRenameDataSetColumn(column);
				}
			});
			contextMenu.add(rename);

			contextMenu.addSeparator();

			// Mask the column.
			final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(Resources
					.get("maskColumnTitle"));
			mask.setMnemonic(Resources.get("maskColumnMnemonic").charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (mask.isSelected())
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestMaskColumn(
										DataSetContext.this.getDataSet(),
										column);
					else
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestUnmaskColumn(
										DataSetContext.this.getDataSet(),
										column);
				}
			});
			contextMenu.add(mask);
			mask.setSelected(column.getMasked());

			contextMenu.addSeparator();

			// If it is partitioned, make a submenu to change the partition
			// type.
			if (column.getPartitionType() != null) {

				// The option to change the partition type.
				final JMenuItem changepartition = new JMenuItem(
						Resources.get("changePartitionColumnTitle"),
						new ImageIcon(
								Resources
										.getResourceAsURL("org/biomart/builder/resources/expandAll.gif")));
				changepartition.setMnemonic(Resources.get(
						"changePartitionColumnMnemonic").charAt(0));
				changepartition.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestPartitionByColumn(
										DataSetContext.this.getDataSet(),
										column);
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
										.getResourceAsURL("org/biomart/builder/resources/expandAll.gif")));
				partition.setMnemonic(Resources.get("partitionColumnMnemonic")
						.charAt(0));
				partition.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestPartitionByColumn(
										DataSetContext.this.getDataSet(),
										column);
					}
				});
				contextMenu.add(partition);
			}

			// The option to turn off partitioning.
			final JMenuItem unpartition = new JMenuItem(Resources
					.get("unpartitionColumnTitle"));
			unpartition.setMnemonic(Resources.get("unpartitionColumnMnemonic")
					.charAt(0));
			unpartition.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestUnpartitionByColumn(
									DataSetContext.this.getDataSet(), column);
				}
			});
			contextMenu.add(unpartition);
			if (column.getPartitionType() == null)
				unpartition.setEnabled(false);

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
						DataSetContext.this.getMartTab().getDataSetTabSet()
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
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestRemoveExpressionColumn(
										(ExpressionColumn) column);
					}
				});
				contextMenu.add(remove);

			}
		}
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {

		// Is it a relation?
		if (object instanceof Relation) {

			// Which relation is it?
			final Relation relation = (Relation) object;

			// What tables does it link?
			final DataSetTable target = (DataSetTable) relation.getManyKey()
					.getTable();

			// Highlight SUBCLASS relations.
			if (target.getType().equals(DataSetTableType.MAIN_SUBCLASS))
				component.setForeground(RelationComponent.SUBCLASS_COLOUR);

			// All the rest are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);

			// Do the stroke.
			final RelationComponent relcomp = (RelationComponent) component;
			relcomp.setStroke(RelationComponent.ONE_MANY);
		}

		// Is it a table?
		else if (object instanceof DataSetTable) {

			// Which table is it?
			final DataSetTableType tableType = ((DataSetTable) object)
					.getType();

			// Highlight SUBCLASS tables.
			if (tableType.equals(DataSetTableType.MAIN_SUBCLASS))
				component.setForeground(TableComponent.SUBCLASS_COLOUR);

			// Highlight DIMENSION tables.
			else if (tableType.equals(DataSetTableType.DIMENSION))
				component.setForeground(TableComponent.DIMENSION_COLOUR);

			// All others are normal.
			else
				component.setForeground(TableComponent.NORMAL_COLOUR);
		}

		// Columns.
		else if (object instanceof DataSetColumn) {

			// Which column is it?
			final DataSetColumn column = (DataSetColumn) object;

			// Magenta EXPRESSION columns.
			if (column instanceof InheritedColumn)
				component.setForeground(ColumnComponent.INHERITED_COLOUR);

			// Fade out all MASKED columns.
			else if (column.getMasked())
				component.setForeground(ColumnComponent.FADED_COLOUR);

			// Blue PARTITIONED columns.
			else if (column.getPartitionType() != null)
				component.setForeground(ColumnComponent.PARTITIONED_COLOUR);

			// Magenta EXPRESSION columns.
			else if (column instanceof ExpressionColumn)
				component.setForeground(ColumnComponent.EXPRESSION_COLOUR);

			// All others are normal.
			else
				component.setForeground(ColumnComponent.NORMAL_COLOUR);
		}
	}
}
