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

package org.ensembl.mart.vieweditor;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.BaseConfigurationObject;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;

/**
 * Class DatasetViewAttributeTableModel implementing TableModel.
 *
 * <p>This class is written for the attributes table to implement autoscroll
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetView
 */

public class DatasetViewAttributeTableModel implements TableModel {

    protected String[] columnNames = {"Attribute", "Value"};
    protected Vector tableModelListenerList;
    protected static final int COLUMN_COUNT = 2;
    protected BaseConfigurationObject obj;
    protected String objClass;
    protected String[] firstColumnData;
    protected DatasetViewTreeNode node;
    protected DatasetViewTreeNode parent;

    public DatasetViewAttributeTableModel(DatasetViewTreeNode node, String[] firstColumnData, String objClass) {
        this.node = node;
        this.obj = (BaseConfigurationObject) node.getUserObject();
        this.firstColumnData = firstColumnData;
        this.objClass = objClass;
        tableModelListenerList = new Vector();
        parent = (DatasetViewTreeNode) node.getParent();
    }

    public void addTableModelListener(TableModelListener l) {
        // Adds a listener to the list that is notified each time a change to the data model occurs.
        tableModelListenerList.add(l);
    }

    public Class getColumnClass(int columnIndex) {
        //Returns the most specific superclass for all the cell values in the column.
        try {
            return Class.forName("java.lang.String");
        } catch (Exception e) {
            return null;
        }
    }

    public int getColumnCount() {
        //Returns the number of columns in the model.
        return COLUMN_COUNT;
    }

    public String getColumnName(int columnIndex) {
        //Returns the name of the column at columnIndex.
        return columnNames[columnIndex];
    }

    public int getRowCount() {
        //Returns the number of rows in the model.
        return firstColumnData.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        //Returns the value for the cell at columnIndex and rowIndex.
        if (columnIndex == 0) {
            return firstColumnData[rowIndex];
        } else {
            return obj.getAttribute(firstColumnData[rowIndex]);
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        //Returns true if the cell at rowIndex and columnIndex is editable.
        if (columnIndex == 0)
            return false;
        return true;
    }

    public void removeTableModelListener(TableModelListener l) {
        //Removes a listener from the list that is notified each time a change to the data model occurs.
        while (tableModelListenerList.remove((Object) l)) ;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        //Sets the value in the cell at columnIndex and rowIndex to aValue.
        Object child = node.getUserObject();

        if (columnIndex == 1) {
            //child may be a DatasetView, in which case dont try to remove/add the child to a null parent
            if (child instanceof org.ensembl.mart.lib.config.DatasetView) {
                obj.setAttribute(firstColumnData[rowIndex], (String) aValue);
            } else {
                Object parent = ((DatasetViewTreeNode) node.getParent()).getUserObject();
                int index = node.getParent().getIndex(node);

                if (parent instanceof org.ensembl.mart.lib.config.DatasetView) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterPage) {
                        DatasetView view = (DatasetView) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        view.removeFilterPage((FilterPage) node.getUserObject());

                    } else if (child instanceof org.ensembl.mart.lib.config.AttributePage) {
                        DatasetView view = (DatasetView) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        view.removeAttributePage((AttributePage) node.getUserObject());

                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterGroup) {
                        FilterPage fp = (FilterPage) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        fp.removeFilterGroup((FilterGroup) node.getUserObject());
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterCollection) {
                        FilterGroup fg = (FilterGroup) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        fg.removeFilterCollection((FilterCollection) node.getUserObject());
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
                        FilterCollection fc = (FilterCollection) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        fc.removeFilterDescription((FilterDescription) node.getUserObject());
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
                    if (child instanceof org.ensembl.mart.lib.config.AttributeGroup) {
                        AttributePage ap = (AttributePage) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        ap.removeAttributeGroup((AttributeGroup) node.getUserObject());
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
                    if (child instanceof org.ensembl.mart.lib.config.AttributeCollection) {
                        AttributeGroup ag = (AttributeGroup) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        ag.removeAttributeCollection((AttributeCollection) node.getUserObject());
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
                    if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
                        AttributeCollection ac = (AttributeCollection) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        ac.removeAttributeDescription((AttributeDescription) node.getUserObject());
                    }
                }

                obj.setAttribute(firstColumnData[rowIndex], (String) aValue);

                if (parent instanceof org.ensembl.mart.lib.config.DatasetView) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterPage) {
                        DatasetView view = (DatasetView) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        view.insertFilterPage(index, (FilterPage) obj);

                    } else if (child instanceof org.ensembl.mart.lib.config.AttributePage) {
                        DatasetView view = (DatasetView) ((DatasetViewTreeNode) node.getParent()).getUserObject();

                        view.insertAttributePage(index - view.getFilterPages().length, (AttributePage) obj);

                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterGroup) {
                        FilterPage fp = (FilterPage) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        fp.insertFilterGroup(index, (FilterGroup) obj);
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterCollection) {
                        FilterGroup fg = (FilterGroup) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        fg.insertFilterCollection(index, (FilterCollection) obj);
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
                    if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
                        FilterCollection fc = (FilterCollection) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        fc.insertFilterDescription(index, (FilterDescription) obj);
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
                    if (child instanceof org.ensembl.mart.lib.config.AttributeGroup) {
                        AttributePage ap = (AttributePage) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        ap.insertAttributeGroup(index, (AttributeGroup) obj);
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
                    if (child instanceof org.ensembl.mart.lib.config.AttributeCollection) {
                        AttributeGroup ag = (AttributeGroup) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        ag.insertAttributeCollection(index, (AttributeCollection) obj);
                    }
                } else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
                    if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
                        AttributeCollection ac = (AttributeCollection) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                        ac.insertAttributeDescription(index, (AttributeDescription) obj);
                    }
                }
            }
            DatasetViewTreeNode newNode = new DatasetViewTreeNode(obj.getAttribute("internalName"), obj);

            if (parent != null) {
                int index = parent.getIndex(node);
                node.removeFromParent();
                parent.insert(newNode, index);
                node = newNode;
            }
            TableModelEvent tme = new TableModelEvent(this, rowIndex);

            fireEvent(tme);
        }
    }

    public void setObject(BaseConfigurationObject obj) {
        this.obj = obj;
    }

    public DatasetViewTreeNode getParentNode() {
        return parent;
    }

    private void fireEvent(TableModelEvent tme) {
        for (Enumeration e = tableModelListenerList.elements(); e.hasMoreElements();) {
            TableModelListener tml = (TableModelListener) e.nextElement();
            tml.tableChanged(tme);
        }
    }
}
