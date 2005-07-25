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

package org.ensembl.mart.builder;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.ensembl.mart.builder.lib.*;

/**
 * Class DatasetConfigAttributeTableModel implementing TableModel.
 *
 * <p>This class is written for the attributes table to implement autoscroll
 * </p>
 *
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.DatasetConfig
 */

public class TransformationConfigAttributeTableModel implements TableModel {

	protected String[] columnNames = { "Attribute", "Value" };
	protected Vector tableModelListenerList;
	protected static final int COLUMN_COUNT = 2;
	protected ConfigurationBase obj;
	protected String objClass;
	protected String[] firstColumnData;
	protected TransformationConfigTreeNode node;
	protected TransformationConfigTreeNode parent;
	protected int[] requiredFields;

	public TransformationConfigAttributeTableModel(TransformationConfigTreeNode node, String[] firstColumnData, String objClass) {
		this.node = node;
		this.obj = (ConfigurationBase) node.getUserObject();
		this.firstColumnData = firstColumnData;
		this.objClass = objClass;
		tableModelListenerList = new Vector();
		parent = (TransformationConfigTreeNode) node.getParent();
		
		requiredFields = obj.getRequiredFields();
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
	
	public int[] getRequiredFields() {
		return requiredFields;
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
			return obj.getElement().getAttributeValue(firstColumnData[rowIndex]);
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
		while (tableModelListenerList.remove((Object) l));
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {		
		//Sets the value in the cell at columnIndex and rowIndex to aValue.
		Object child = node.getUserObject();	
		if (columnIndex == 1) {
			//child may be a TransformationConfig, in which case dont try to remove/add the child to a null parent
			if (!(child instanceof org.ensembl.mart.builder.lib.TransformationConfig)) {
				Object parent = ((TransformationConfigTreeNode) node.getParent()).getUserObject();
				int index = node.getParent().getIndex(node) - TransformationConfigTreeNode.getHeterogenousOffset(parent, child);
				ConfigurationBase parentConf = (ConfigurationBase) parent;
				parentConf.removeChildObject(((ConfigurationBase) child).getElement().getAttributeValue("internalName"));
			}
			obj.getElement().setAttribute(firstColumnData[rowIndex], (String) aValue);
			TransformationConfigTreeNode newNode = new TransformationConfigTreeNode(obj.getElement().getAttributeValue("internalName"), obj);

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
	

	public void setObject(ConfigurationBase obj) {
		this.obj = obj;
	}

	public TransformationConfigTreeNode getParentNode() {
		return parent;
	}

	private void fireEvent(TableModelEvent tme) {
		for (Enumeration e = tableModelListenerList.elements(); e.hasMoreElements();) {
			TableModelListener tml = (TableModelListener) e.nextElement();
			tml.tableChanged(tme);
		}
	}
}
