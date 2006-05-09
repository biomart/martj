/*
 * TableManagerDialog.java
 *
 * Created on 08 May 2006, 10:39
 */

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

package org.biomart.builder.view.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 9th May 2006
 * @since 0.1
 */
public class TableManagerDialog extends JDialog {
    
    private Table table;
    private JList columnsList;
    private DefaultListModel columnsListModel;
    private DiagramContext diagramContext;
    
    /** Creates a new instance of TableManagerDialog */
    private TableManagerDialog(SchemaTabSet schemaTabSet, Table table, DiagramContext diagramContext) {
        super(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder(),
                BuilderBundle.getString("tableManagerDialogTitle"),
                true);
        
        this.table = table;
        this.diagramContext = diagramContext;
        
        // Useful things
        JButton close = new JButton(BuilderBundle.getString("closeButton"));
        this.columnsListModel = new DefaultListModel();
        this.columnsList = new JList(this.columnsListModel);
        this.columnsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // create label constraints
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(2,2,2,2);
        // create field constraints
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.fill = GridBagConstraints.NONE;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.insets = new Insets(2,2,2,2);
        // create last row label constraints
        GridBagConstraints labelLastRowConstraints = (GridBagConstraints)labelConstraints.clone();
        labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
        // create last row field constraints
        GridBagConstraints fieldLastRowConstraints = (GridBagConstraints)fieldConstraints.clone();
        fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
        
        // Make the content.
        JPanel content = new JPanel(new BorderLayout());
        this.setContentPane(content);
        
        // Columns pane.
        GridBagLayout columnsGridBag = new GridBagLayout();
        JPanel colsPane = new JPanel(columnsGridBag);
        JLabel label = new JLabel(BuilderBundle.getString("schemaLabel"));
        columnsGridBag.setConstraints(label, labelConstraints);
        colsPane.add(label);
        JComponent field = new JTextField(this.table.getSchema().getName());
        field.setEnabled(false);
        columnsGridBag.setConstraints(field, fieldConstraints);
        colsPane.add(field);
        label = new JLabel(BuilderBundle.getString("tableLabel"));
        columnsGridBag.setConstraints(label, labelConstraints);
        colsPane.add(label);
        field = new JTextField(this.table.getName());
        field.setEnabled(false);
        columnsGridBag.setConstraints(field, fieldConstraints);
        colsPane.add(field);
        label = new JLabel(BuilderBundle.getString("columnsLabel"));
        columnsGridBag.setConstraints(label, labelConstraints);
        colsPane.add(label);
        field = new JScrollPane(this.columnsList);
        columnsGridBag.setConstraints(field, fieldConstraints);
        colsPane.add(field);
        label = new JLabel();
        columnsGridBag.setConstraints(label, labelLastRowConstraints);
        colsPane.add(label);
        field = close;
        columnsGridBag.setConstraints(field, fieldLastRowConstraints);
        colsPane.add(field);
        
        // Context pane.
        JComponent contextPane = this.diagramContext.getTableManagerContextPane(this);
        
        // Build the window.
        colsPane.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        content.add(colsPane, BorderLayout.LINE_START);
        contextPane.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        content.add(contextPane, BorderLayout.CENTER); // So that the context pane fills all available space.
        
        // intercept the close button
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                requestClose();
            }
        });
        // make it the default button.
        this.getRootPane().setDefaultButton(close);
        
        // populate columns list and context panel
        this.reloadTable();
        
        // set size of window
        this.pack();
    }
    
    public Table getTable() {
        return this.table;
    }
    
    public JList getColumnsList() {
        return this.columnsList;
    }
    
    public void requestClose() {
        this.hide();
    }
    
    public void reloadTable() {
        // Dataset table may have been recreated.
        if (this.table instanceof DataSetTable) this.table = this.table.getSchema().getTableByName(this.table.getName());
        // load the columns.
        this.columnsListModel.removeAllElements();
        List columns = new ArrayList();
        for (Iterator i = this.table.getColumns().iterator(); i.hasNext(); ) {
            Column col = (Column)i.next();
            columns.add(col.getName());
        }
        Collections.sort(columns);   
        for (Iterator i = columns.iterator(); i.hasNext(); ) this.columnsListModel.addElement(i.next());
    }
    
    /**
     * Static method which allows the user to create a new table provider.
     */
    public static void showTableManager(SchemaTabSet schemaTabSet, Table table, DiagramContext diagramContext) {
        TableManagerDialog dialog = new TableManagerDialog(
                schemaTabSet,
                table,
                diagramContext);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
    }
}
