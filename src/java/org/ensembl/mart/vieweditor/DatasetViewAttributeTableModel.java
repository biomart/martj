package org.ensembl.mart.vieweditor;

import org.ensembl.mart.lib.config.BaseConfigurationObject;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.AttributeDescription;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import java.util.Vector;
import java.util.Enumeration;

public class DatasetViewAttributeTableModel implements TableModel {

    protected String[] columnNames = {"Attribute", "Value"};
    protected Vector tableModelListenerList;
    protected static final int COLUMN_COUNT = 2;
    protected BaseConfigurationObject obj;
    protected String objClass;
    protected String[] firstColumnData;
    DatasetViewTreeNode node;

    public DatasetViewAttributeTableModel(DatasetViewTreeNode node, String[] firstColumnData, String objClass) {
        this.node = node;
        this.obj = (BaseConfigurationObject)node.getUserObject();
        this.firstColumnData = firstColumnData;
        this.objClass = objClass;
        tableModelListenerList = new Vector();
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
            switch (rowIndex) {
                case 0:
                    return obj.getDescription();
                case 1:
                    return obj.getDisplayName();
                case 2:
                    return obj.getInternalName();
                case 3:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                        //return ((FilterDescription)obj).getIsSelectable();
                            return "";
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            return ((AttributeDescription) obj).getField();
                    }
                case 4:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            return ((FilterDescription) obj).getLegalQualifiers();
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            return ((AttributeDescription) obj).getTableConstraint();
                    }
                case 5:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            return ((FilterDescription) obj).getQualifier();
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            return new Long(((AttributeDescription) obj).getMaxLength());
                    }
                case 6:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            return ((FilterDescription) obj).getTableConstraint();
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            return ((AttributeDescription) obj).getSource();
                    }
                case 7:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            return ((FilterDescription) obj).getType();
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            return ((AttributeDescription) obj).getHomePageURL();
                    }
                case 8:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            return "";
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            return ((AttributeDescription) obj).getLinkoutURL();
                    }

            }
    }
         return "";
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
         if (columnIndex == 1) {
            switch (rowIndex) {
                case 0:
                    obj.setDescription((String)aValue);
                case 1:
                    obj.setDisplayName((String)aValue);
                case 2:
                    obj.setInternalName((String)aValue);
              /*  case 3:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            ((FilterDescription)obj).setIsSelectable((String)aValue);
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            ((AttributeDescription) obj).setField((String)aValue);
                    }
                case 4:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            ((FilterDescription) obj).setLegalQualifiers((String)aValue);
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            ((AttributeDescription) obj).setTableConstraint((String)aValue);
                    }
                case 5:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            ((FilterDescription) obj).setQualifier((String)aValue);
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            new Long(((AttributeDescription) obj).setMaxLength((String)aValue));
                    }
                case 6:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            ((FilterDescription) obj).setTableConstraint((String)aValue);
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            ((AttributeDescription) obj).setSource((String)aValue);
                    }
                case 7:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            ((FilterDescription) obj).setType((String)aValue);
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            ((AttributeDescription) obj).setHomePageURL((String)aValue);
                    }
                case 8:
                    {
                        if (objClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
                            return "";
                        else if (objClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
                            ((AttributeDescription) obj).setLinkoutURL((String)aValue);
                    }   */

            }
    }

        TableModelEvent tme = new TableModelEvent(this, rowIndex);
        fireEvent(tme);
        node.setUserObject(obj);
    }

    public void setObject(BaseConfigurationObject obj) {
        this.obj = obj;
    }

    public DatasetViewTreeNode getNode(){
        return node;
    }
    private void fireEvent( TableModelEvent tme )
      {
         for( Enumeration e = tableModelListenerList.elements(); e.hasMoreElements(); )
         {
            TableModelListener tml = ( TableModelListener) e.nextElement();
            tml.tableChanged( tme );
         }
      }
}
