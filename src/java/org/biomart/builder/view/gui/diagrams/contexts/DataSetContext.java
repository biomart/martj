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
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.components.BoxShapedComponent;
import org.biomart.builder.view.gui.diagrams.components.ColumnComponent;
import org.biomart.builder.view.gui.diagrams.components.KeyComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * This context adapts dataset diagrams to display different colours, and
 * provides the context menu for interacting with dataset diagrams.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class DataSetContext extends SchemaContext {
	private DataSet dataset;

	private boolean hideMasked = false;

	/**
	 * Creates a new context that will adapt database objects according to the
	 * settings in the specified dataset.
	 * 
	 * @param martTab
	 *            the mart tab this context appears in.
	 * @param dataset
	 *            the dataset this context will use for customising menus and
	 *            colours.
	 */
	public DataSetContext(final MartTab martTab, final DataSet dataset) {
		super(martTab);
		this.dataset = dataset;
	}

	private void changeHideMasked(final boolean masked, final Diagram diagram) {
		this.hideMasked = masked;
		diagram.repaintDiagram();
	}

	/**
	 * Obtain the dataset that this context is linked with.
	 * 
	 * @return our dataset.
	 */
	public DataSet getDataSet() {
		return this.dataset;
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

			// Is it compounded?
			((RelationComponent) component).setCompounded(this.dataset
					.getSchemaModifications().isCompoundRelation(null,
							target.getFocusRelation()));

			// Fade MASKED DIMENSION relations.
			if (this.getDataSet().getDataSetModifications().isMaskedTable(
					target)) {
				component.setVisible(!this.hideMasked);
				component.setForeground(RelationComponent.MASKED_COLOUR);
			}

			// Fade MERGED DIMENSION relations.
			else if (this.getDataSet().getSchemaModifications()
					.isMergedRelation(target.getFocusRelation()))
				component.setForeground(TableComponent.MASKED_COLOUR);

			// Highlight SUBCLASS relations.
			else if (target.getType().equals(DataSetTableType.MAIN_SUBCLASS))
				component.setForeground(RelationComponent.SUBCLASS_COLOUR);

			// All the rest are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);
		}

		// Is it a table?
		else if (object instanceof DataSetTable) {

			// Which table is it?
			final DataSetTableType tableType = ((DataSetTable) object)
					.getType();

			// Fade MASKED DIMENSION relations.
			if (this.getDataSet().getDataSetModifications().isMaskedTable(
					(DataSetTable) object)) {
				component.setVisible(!this.hideMasked);
				component.setForeground(TableComponent.MASKED_COLOUR);
			}

			// Fade MERGED DIMENSION tables.
			else if (this.getDataSet().getSchemaModifications()
					.isMergedRelation(
							((DataSetTable) object).getFocusRelation()))
				component.setForeground(TableComponent.MASKED_COLOUR);

			// Highlight SUBCLASS tables.
			else if (tableType.equals(DataSetTableType.MAIN_SUBCLASS))
				component.setForeground(TableComponent.SUBCLASS_COLOUR);

			// Highlight DIMENSION tables.
			else if (tableType.equals(DataSetTableType.DIMENSION)) {
				component
						.setForeground(this.getDataSet()
								.getDataSetModifications().isPartitionedTable(
										(DataSetTable) object) ? TableComponent.DIMENSION_PARTITIONED_COLOUR
								: TableComponent.DIMENSION_COLOUR);

				// Is it compounded?
				((TableComponent) component).setCompounded(this.dataset
						.getSchemaModifications().isCompoundRelation(null,
								((DataSetTable) object).getFocusRelation()));
			}

			// All others are normal.
			else
				component.setForeground(TableComponent.NORMAL_COLOUR);

			((TableComponent) component).setRenameable(true);
			((TableComponent) component).setSelectable(true);
		}

		// Columns.
		else if (object instanceof DataSetColumn) {

			// Which column is it?
			final DataSetColumn column = (DataSetColumn) object;

			// Fade out all MASKED columns.
			if (((DataSet) column.getTable().getSchema())
					.getDataSetModifications().isMaskedColumn(column))
				component.setBackground(ColumnComponent.FADED_COLOUR);
			// Blue PARTITIONED columns.
			else if (((DataSet) column.getTable().getSchema())
					.getDataSetModifications().isPartitionedColumn(column))
				component.setBackground(ColumnComponent.PARTITIONED_COLOUR);
			// Red INHERITED columns.
			else if (column instanceof InheritedColumn)
				component.setBackground(ColumnComponent.INHERITED_COLOUR);
			// Magenta EXPRESSION columns.
			else if (column instanceof ExpressionColumn)
				component.setBackground(ColumnComponent.EXPRESSION_COLOUR);
			// Magenta CONCAT columns.
			else if (column instanceof ConcatColumn)
				component.setBackground(ColumnComponent.CONCAT_COLOUR);
			// All others are normal.
			else
				component.setBackground(ColumnComponent.NORMAL_COLOUR);

			// Indexed?
			if (((DataSet) column.getTable().getSchema())
					.getDataSetModifications().isIndexedColumn(column))
				((BoxShapedComponent) component).setIndexed(true);
			else
				((BoxShapedComponent) component).setIndexed(false);

			// Change foreground of non-inherited columns.
			if (((DataSet) column.getTable().getSchema())
					.getDataSetModifications().isNonInheritedColumn(column))
				component.setForeground(ColumnComponent.NONINHERITED_FG_COLOUR);
			else
				component.setForeground(ColumnComponent.NORMAL_FG_COLOUR);

			((ColumnComponent) component).setRenameable(true);
			((ColumnComponent) component).setSelectable(true);
		}

		// Keys
		else if (object instanceof Key) {
			((BoxShapedComponent) component).setIndexed(true);

			// Remove drag-and-drop from the key as it does not apply in
			// the window context.
			((KeyComponent) component).setDraggable(false);
		}
	}

	public void populateMultiContextMenu(final JPopupMenu contextMenu,
			final Collection selectedItems, final Class clazz) {

		// Menu for multiple table selection.
		if (DataSetTable.class.isAssignableFrom(clazz)) {
			// If all are dimensions...
			boolean allDimensions = true;
			for (final Iterator i = selectedItems.iterator(); i.hasNext();)
				allDimensions &= ((DataSetTable) i.next()).getType().equals(
						DataSetTableType.DIMENSION);
			if (allDimensions) {
				// The dimension can be removed by using this option. This
				// simply masks the relation that caused the dimension to exist.
				final JMenuItem removeDM = new JMenuItem(Resources
						.get("maskGroupDimensionTitle"));
				removeDM.setMnemonic(Resources
						.get("maskGroupDimensionMnemonic").charAt(0));
				removeDM.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						for (final Iterator i = selectedItems.iterator(); i
								.hasNext();) {
							final DataSetTable table = (DataSetTable) i.next();
							final boolean isMasked = DataSetContext.this
									.getDataSet().getDataSetModifications()
									.isMaskedTable(table);
							final boolean isMerged = DataSetContext.this
									.getDataSet().getSchemaModifications()
									.isMergedRelation(table.getFocusRelation());
							final boolean isCompound = DataSetContext.this
									.getDataSet().getSchemaModifications()
									.isCompoundRelation(null,
											table.getFocusRelation());
							contextMenu.add(removeDM);
							if (!isMerged && !isCompound && !isMasked)
								DataSetContext.this.getMartTab()
										.getDataSetTabSet()
										.requestMaskDimension(
												DataSetContext.this
														.getDataSet(), table);
						}
					}
				});
				contextMenu.add(removeDM);

				final JMenuItem reinstateDM = new JMenuItem(Resources
						.get("unmaskGroupDimensionTitle"));
				reinstateDM.setMnemonic(Resources.get(
						"unmaskGroupDimensionMnemonic").charAt(0));
				reinstateDM.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						for (final Iterator i = selectedItems.iterator(); i
								.hasNext();) {
							final DataSetTable table = (DataSetTable) i.next();
							final boolean isMasked = DataSetContext.this
									.getDataSet().getDataSetModifications()
									.isMaskedTable(table);
							contextMenu.add(removeDM);
							if (isMasked)
								DataSetContext.this.getMartTab()
										.getDataSetTabSet()
										.requestUnmaskDimension(
												DataSetContext.this
														.getDataSet(), table);
						}
					}
				});
				contextMenu.add(reinstateDM);
			} else
				JOptionPane.showMessageDialog(this.getMartTab().getMartTabSet()
						.getMartBuilder(), Resources.get("multiTableDimOnly"),
						Resources.get("questionTitle"),
						JOptionPane.INFORMATION_MESSAGE);
		}

		// Menu for multiple column selection.
		else if (DataSetColumn.class.isAssignableFrom(clazz)) {

			// The dimension can be removed by using this option. This
			// simply masks the relation that caused the dimension to exist.
			final JMenuItem mask = new JMenuItem(Resources
					.get("maskGroupColumnTitle"));
			mask
					.setMnemonic(Resources.get("maskGroupColumnMnemonic")
							.charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					final Collection columns = new HashSet();
					for (final Iterator i = selectedItems.iterator(); i
							.hasNext();) {
						final DataSetColumn column = (DataSetColumn) i.next();
						final boolean isMasked = DataSetContext.this
								.getDataSet().getDataSetModifications()
								.isMaskedColumn(column);
						final boolean isPartitioned = DataSetContext.this
								.getDataSet().getDataSetModifications()
								.isPartitionedColumn(column);
						if (!isMasked && !isPartitioned
								&& !(column instanceof ConcatColumn)
								&& !(column instanceof ExpressionColumn))
							columns.add(column);
					}
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestMaskColumns(
									DataSetContext.this.getDataSet(), columns);
				}
			});
			contextMenu.add(mask);

			final JMenuItem unmask = new JMenuItem(Resources
					.get("unmaskGroupColumnTitle"));
			unmask.setMnemonic(Resources.get("unmaskGroupColumnMnemonic")
					.charAt(0));
			unmask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					final Collection columns = new HashSet();
					for (final Iterator i = selectedItems.iterator(); i
							.hasNext();) {
						final DataSetColumn column = (DataSetColumn) i.next();
						final boolean isMasked = DataSetContext.this
								.getDataSet().getDataSetModifications()
								.isMaskedColumn(column);
						if (isMasked)
							columns.add(column);
					}
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestUnmaskColumns(
									DataSetContext.this.getDataSet(), columns);
				}
			});
			contextMenu.add(unmask);

			contextMenu.addSeparator();

			// Index/Non-index
			final JMenuItem index = new JMenuItem(Resources
					.get("indexGroupColumnTitle"));
			index.setMnemonic(Resources.get("indexGroupColumnMnemonic").charAt(
					0));
			index.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					for (final Iterator i = selectedItems.iterator(); i
							.hasNext();) {
						final DataSetColumn column = (DataSetColumn) i.next();
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestIndexColumn(
										DataSetContext.this.getDataSet(),
										column);
					}
				}
			});
			contextMenu.add(index);

			final JMenuItem unindex = new JMenuItem(Resources
					.get("unindexGroupColumnTitle"));
			unindex.setMnemonic(Resources.get("unindexGroupColumnMnemonic")
					.charAt(0));
			unindex.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					for (final Iterator i = selectedItems.iterator(); i
							.hasNext();) {
						final DataSetColumn column = (DataSetColumn) i.next();
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestUnindexColumn(
										DataSetContext.this.getDataSet(),
										column);
					}
				}
			});
			contextMenu.add(unindex);

			// If all are non-dimensions...
			boolean allNonDimensions = true;
			for (final Iterator i = selectedItems.iterator(); i.hasNext();)
				allNonDimensions &= !((DataSetTable) ((DataSetColumn) i.next())
						.getTable()).getType().equals(
						DataSetTableType.DIMENSION);
			if (allNonDimensions) {

				contextMenu.addSeparator();

				// (Un)non-inherit all columns on this table.
				final JMenuItem non = new JMenuItem(Resources
						.get("nonInheritGroupTitle"));
				non.setMnemonic(Resources.get("nonInheritGroupMnemonic")
						.charAt(0));
				non.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						for (final Iterator i = selectedItems.iterator(); i
								.hasNext();) {
							final DataSetColumn column = (DataSetColumn) i
									.next();
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestNonInheritColumn(
											DataSetContext.this.getDataSet(),
											column);
						}
					}
				});
				contextMenu.add(non);
				final JMenuItem unnon = new JMenuItem(Resources
						.get("unNonInheritGroupTitle"));
				unnon.setMnemonic(Resources.get("unNonInheritGroupMnemonic")
						.charAt(0));
				unnon.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						for (final Iterator i = selectedItems.iterator(); i
								.hasNext();) {
							final DataSetColumn column = (DataSetColumn) i
									.next();
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestUnNonInheritColumn(
											DataSetContext.this.getDataSet(),
											column);
						}
					}
				});
				contextMenu.add(unnon);
			}
		}
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

		// Is it the diagram background?
		if (object instanceof Diagram) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Do the show/hide masked tables thing.
			final JCheckBoxMenuItem showHide = new JCheckBoxMenuItem(Resources
					.get("hideMaskedDimensionsTitle"));
			showHide.setMnemonic(Resources.get("hideMaskedDimensionsMnemonic")
					.charAt(0));
			showHide.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.changeHideMasked(showHide.isSelected(),
							(Diagram) object);
				}
			});
			contextMenu.add(showHide);
			showHide.setSelected(this.hideMasked);
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
			final JMenuItem explain = new JMenuItem(Resources
					.get("explainTableTitle"), new ImageIcon(Resources
					.getResourceAsURL("help.gif")));
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

			contextMenu.addSeparator();

			// Add an expression column.
			final JMenuItem expression = new JMenuItem(Resources
					.get("addExpressionColumnTitle"));
			expression.setMnemonic(Resources.get("addExpressionColumnMnemonic")
					.charAt(0));
			expression.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					DataSetContext.this.getMartTab().getDataSetTabSet()
							.requestExpressionColumn(table, null);
				}
			});
			contextMenu.add(expression);

			contextMenu.addSeparator();

			final boolean isMasked = this.getDataSet()
					.getDataSetModifications().isMaskedTable(table);
			final boolean isMerged = this.getDataSet().getSchemaModifications()
					.isMergedRelation(table.getFocusRelation());
			final boolean isCompound = this.getDataSet()
					.getSchemaModifications().isCompoundRelation(null,
							table.getFocusRelation());

			// Dimension tables have their own options.
			if (tableType.equals(DataSetTableType.DIMENSION)) {

				// The dimension can be merged by using this option. This
				// affects all dimensions based on this relation.
				final JCheckBoxMenuItem mergeDM = new JCheckBoxMenuItem(
						Resources.get("mergeDimensionTitle"));
				mergeDM.setMnemonic(Resources.get("mergeDimensionMnemonic")
						.charAt(0));
				mergeDM.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						if (mergeDM.isSelected())
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestMergeDimension(
											DataSetContext.this.getDataSet(),
											table);
						else
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestUnmergeDimension(
											DataSetContext.this.getDataSet(),
											table);
					}
				});
				contextMenu.add(mergeDM);
				mergeDM.setSelected(isMerged);
				if (isCompound)
					mergeDM.setEnabled(false);

				// The dimension can be removed by using this option. This
				// simply masks the relation that caused the dimension to exist.
				final JCheckBoxMenuItem removeDM = new JCheckBoxMenuItem(
						Resources.get("maskDimensionTitle"));
				removeDM.setMnemonic(Resources.get("maskDimensionMnemonic")
						.charAt(0));
				removeDM.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						if (removeDM.isSelected())
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestMaskDimension(
											DataSetContext.this.getDataSet(),
											table);
						else
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestUnmaskDimension(
											DataSetContext.this.getDataSet(),
											table);
					}
				});
				contextMenu.add(removeDM);
				if (isMerged || isCompound)
					removeDM.setEnabled(false);
				if (isMasked)
					removeDM.setSelected(true);

				// The dim table can be subclassed by using this option. This
				// simply subclasses the relation that caused the dim to exist.
				final JMenuItem subclass = new JMenuItem(Resources
						.get("dimToSubclassTitle"));
				subclass.setMnemonic(Resources.get("dimToSubclassMnemonic")
						.charAt(0));
				subclass.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestSubclassRelation(
										DataSetContext.this.getDataSet(),
										table.getFocusRelation());
					}
				});
				if (isMerged || isMasked || isCompound)
					subclass.setEnabled(false);
				contextMenu.add(subclass);

				// The compound option allows the user to compound a relation.
				final JCheckBoxMenuItem compound = new JCheckBoxMenuItem(
						Resources.get("replicateDimensionTitle"));
				compound.setMnemonic(Resources
						.get("replicateDimensionMnemonic").charAt(0));
				compound.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestReplicateDimension(
										DataSetContext.this.dataset, table);
						compound.setSelected(DataSetContext.this.dataset
								.getSchemaModifications().isCompoundRelation(
										null, table.getFocusRelation()));
					}
				});
				contextMenu.add(compound);
				if (isMasked || isMerged)
					compound.setEnabled(false);
				if (isCompound)
					compound.setSelected(true);
				contextMenu.addSeparator();

				// If it is partitioned, make a submenu to change the partition
				// type.
				final boolean isPartitioned = this.dataset
						.getDataSetModifications().isPartitionedTable(table);
				if (isPartitioned) {

					// The option to change the partition type.
					final JMenuItem changepartition = new JMenuItem(Resources
							.get("changePartitionTableTitle"), new ImageIcon(
							Resources.getResourceAsURL("expandAll.gif")));
					changepartition.setMnemonic(Resources.get(
							"changePartitionTableMnemonic").charAt(0));
					changepartition.addActionListener(new ActionListener() {
						public void actionPerformed(final ActionEvent evt) {
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestPartitionByColumn(
											DataSetContext.this.getDataSet(),
											table, null);
						}
					});
					contextMenu.add(changepartition);

				}

				// If it is not partitioned, allow the user to turn partitioning
				// on.
				else {

					// Option to enable partitioning.
					final JMenuItem partition = new JMenuItem(Resources
							.get("partitionTableTitle"), new ImageIcon(
							Resources.getResourceAsURL("expandAll.gif")));
					partition.setMnemonic(Resources.get(
							"partitionTableMnemonic").charAt(0));
					partition.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							DataSetContext.this.getMartTab().getDataSetTabSet()
									.requestPartitionByColumn(
											DataSetContext.this.getDataSet(),
											table, null);
						}
					});
					contextMenu.add(partition);
					if (isMasked)
						partition.setEnabled(false);
				}

				// The option to turn off partitioning.
				final JMenuItem unpartition = new JMenuItem(Resources
						.get("unpartitionTableTitle"));
				unpartition.setMnemonic(Resources.get(
						"unpartitionTableMnemonic").charAt(0));
				unpartition.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestUnpartitionByColumn(
										DataSetContext.this.getDataSet(), table);
					}
				});
				contextMenu.add(unpartition);
				if (!isPartitioned)
					unpartition.setEnabled(false);
			}
			// Special stuff for all non-dimension tables.
			else {
				// (Un)non-inherit all columns on this table.
				final JMenuItem non = new JMenuItem(Resources
						.get("nonInheritAllTitle"));
				non.setMnemonic(Resources.get("nonInheritAllMnemonic")
						.charAt(0));
				non.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestNonInheritAllColumns(
										DataSetContext.this.getDataSet(), table);
					}
				});
				contextMenu.add(non);
				final JMenuItem unnon = new JMenuItem(Resources
						.get("unNonInheritAllTitle"));
				unnon.setMnemonic(Resources.get("unNonInheritAllMnemonic")
						.charAt(0));
				unnon.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this
								.getMartTab()
								.getDataSetTabSet()
								.requestUnNonInheritAllColumns(
										DataSetContext.this.getDataSet(), table);
					}
				});
				contextMenu.add(unnon);
			}

			// Subclass tables have their own options too.
			if (tableType.equals(DataSetTableType.MAIN_SUBCLASS)) {

				contextMenu.addSeparator();

				// The subclass table can be removed by using this option. This
				// simply masks the relation that caused the subclass to exist.
				final JMenuItem unsubclass = new JMenuItem(Resources
						.get("removeSubclassTitle"));
				unsubclass.setMnemonic(Resources.get("removeSubclassMnemonic")
						.charAt(0));
				unsubclass.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestUnsubclassRelation(
										DataSetContext.this.getDataSet(),
										table.getFocusRelation());
					}
				});
				contextMenu.add(unsubclass);

				// The compound option allows the user to compound a relation.
				final JCheckBoxMenuItem compound = new JCheckBoxMenuItem(
						Resources.get("recurseSubclassTitle"));
				compound.setMnemonic(Resources.get("recurseSubclassMnemonic")
						.charAt(0));
				compound.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestRecurseSubclass(
										DataSetContext.this.dataset, table);
						compound.setSelected(DataSetContext.this.dataset
								.getSchemaModifications().isCompoundRelation(
										null, table.getFocusRelation()));
					}
				});
				contextMenu.add(compound);
				if (isMasked || isMerged)
					compound.setEnabled(false);
				if (isCompound)
					compound.setSelected(true);
				contextMenu.addSeparator();
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
			final boolean isPartitioned = ((DataSet) column.getTable()
					.getSchema()).getDataSetModifications()
					.isPartitionedColumn(column);
			final boolean isMasked = ((DataSet) column.getTable().getSchema())
					.getDataSetModifications().isMaskedColumn(column);
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
			mask.setSelected(isMasked);
			if (isPartitioned || column instanceof ConcatColumn
					|| column instanceof ExpressionColumn)
				mask.setEnabled(false);

			// Non-inherit inherited columns.
			final boolean isNonInherited = ((DataSet) column.getTable()
					.getSchema()).getDataSetModifications()
					.isNonInheritedColumn(column);
			final JCheckBoxMenuItem inherited = new JCheckBoxMenuItem(Resources
					.get("nonInheritColumnTitle"));
			inherited.setMnemonic(Resources.get("nonInheritColumnMnemonic")
					.charAt(0));
			inherited.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (inherited.isSelected())
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestNonInheritColumn(
										DataSetContext.this.getDataSet(),
										column);
					else
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestUnNonInheritColumn(
										DataSetContext.this.getDataSet(),
										column);
				}
			});
			contextMenu.add(inherited);
			inherited.setSelected(isNonInherited);
			inherited.setEnabled(!isMasked
					&& !((DataSetTable) column.getTable()).getType().equals(
							DataSetTableType.DIMENSION));

			// Index the column.
			final boolean isIndexed = ((DataSet) column.getTable().getSchema())
					.getDataSetModifications().isIndexedColumn(column);
			final JCheckBoxMenuItem index = new JCheckBoxMenuItem(Resources
					.get("indexColumnTitle"));
			index.setMnemonic(Resources.get("indexColumnMnemonic").charAt(0));
			index.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					if (index.isSelected())
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestIndexColumn(
										DataSetContext.this.getDataSet(),
										column);
					else
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestUnindexColumn(
										DataSetContext.this.getDataSet(),
										column);
				}
			});
			contextMenu.add(index);
			index.setSelected(isIndexed);

			contextMenu.addSeparator();

			// If it is partitioned, make a submenu to change the partition
			// type.
			if (isPartitioned) {

				// The option to change the partition type.
				final JMenuItem changepartition = new JMenuItem(Resources
						.get("changePartitionColumnTitle"), new ImageIcon(
						Resources.getResourceAsURL("expandAll.gif")));
				changepartition.setMnemonic(Resources.get(
						"changePartitionColumnMnemonic").charAt(0));
				changepartition.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestPartitionByColumn(
										DataSetContext.this.getDataSet(),
										(DataSetTable) column.getTable(),
										column);
					}
				});
				contextMenu.add(changepartition);

			}

			// If it is not partitioned, allow the user to turn partitioning
			// on.
			else {

				// Option to enable partitioning.
				final JMenuItem partition = new JMenuItem(Resources
						.get("partitionColumnTitle"), new ImageIcon(Resources
						.getResourceAsURL("expandAll.gif")));
				partition.setMnemonic(Resources.get("partitionColumnMnemonic")
						.charAt(0));
				partition.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						DataSetContext.this.getMartTab().getDataSetTabSet()
								.requestPartitionByColumn(
										DataSetContext.this.getDataSet(),
										(DataSetTable) column.getTable(),
										column);
					}
				});
				contextMenu.add(partition);
				if (isMasked
						|| !(column instanceof WrappedColumn)
						|| !((DataSetTable) column.getTable()).getType()
								.equals(DataSetTableType.DIMENSION))
					partition.setEnabled(false);
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
									DataSetContext.this.getDataSet(),
									(DataSetTable) column.getTable());
				}
			});
			contextMenu.add(unpartition);
			if (!isPartitioned)
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
								.requestExpressionColumn(
										(DataSetTable) column.getTable(),
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
										(DataSetTable) column.getTable(),
										(ExpressionColumn) column);
					}
				});
				contextMenu.add(remove);

			}
		}
	}
}
