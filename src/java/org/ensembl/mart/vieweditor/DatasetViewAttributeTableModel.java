package org.ensembl.mart.vieweditor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class DatasetViewAttributeTableModel extends AbstractTableModel{
    protected String[] columnNames = {"Attribute", "Value"};
    protected static final int COLUMN_COUNT = 2;
    protected Object [][] data = new Object [] [] {{"",""}};

    public DatasetViewAttributeTableModel(){

    }

    public int getRowCount(){
        return data.length;
    }

    public int getColumnCount(){
        return  COLUMN_COUNT;
    }

    public boolean  isCellEditable(int row, int col) {
        if (col == 1)
            return true;
        else
            return false;
    }

    public Object getValueAt(int row, int column){
        return data[row][column];
    }

    public void setValueAt(Object obj,int row, int column){

        if (column == 1){
           if (row == 0)
               data[0][1] = (String)obj;
            else if (row == 1)
               data[1][1] = (String)obj;
        }
    }

    public Class getColumnClass(int column){
        return (data[0][column]).getClass();
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    public void setData(Object [] []data){
        this.data = data;
    }
}
