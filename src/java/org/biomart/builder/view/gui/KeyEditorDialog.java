/*
 * ExplainDataSetDialog.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 17th May 2006
 * @since 0.1
 */
public class KeyEditorDialog extends JDialog {
    private DefaultListModel tableColumns;
    private DefaultListModel selectedColumns;
    
    /**
     * Creates a new instance of ExplainDataSetDialog
     */
    private KeyEditorDialog(SchemaTabSet schemaTabSet, Table table, String title, String action, List columns) {
        super(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder(),
                title,
                true);
        
        // Useful things
        this.tableColumns = new DefaultListModel();
        for (Iterator i = table.getColumns().iterator(); i.hasNext(); ) this.tableColumns.addElement(i.next());
        this.selectedColumns = new DefaultListModel();
        if (columns!=null) {
            for (Iterator i = columns.iterator(); i.hasNext(); ) {
                Object o = i.next();
                this.tableColumns.removeElement(o);
                this.selectedColumns.addElement(o);
            }
        }
        JButton close = new JButton(BuilderBundle.getString("closeButton"));
        JButton execute = new JButton(action);
        
        // The table list and buttons
        final JList tabColList = new JList(this.tableColumns);
        JButton insertButton = new JButton(BuilderBundle.getString("insertButton"));
        JButton removeButton = new JButton(BuilderBundle.getString("removeButton"));
        
        // The key list and buttons
        final JList keyColList = new JList(this.selectedColumns);
        JButton upButton = new JButton(BuilderBundle.getString("upButton"));
        JButton downButton = new JButton(BuilderBundle.getString("downButton"));
        
        // Make the content.
        Box content = Box.createHorizontalBox();
        this.setContentPane(content);
        
        // Left-hand side goes the table columns that are unused.
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel(BuilderBundle.getString("columnsAvailableLabel")), BorderLayout.PAGE_START);
        leftPanel.add(new JScrollPane(tabColList), BorderLayout.CENTER);
        leftPanel.setBorder(new EmptyBorder(2,2,2,2));
        Box leftButtonPanel = Box.createVerticalBox();
        leftButtonPanel.add(removeButton);
        leftButtonPanel.add(insertButton);
        leftButtonPanel.setBorder(new EmptyBorder(2,2,2,2));
        leftPanel.add(leftButtonPanel, BorderLayout.LINE_END);
        content.add(leftPanel);
        
        // Right-hand side goes the key columns.
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel(BuilderBundle.getString("keyColumnsLabel")), BorderLayout.PAGE_START);
        rightPanel.add(new JScrollPane(keyColList), BorderLayout.CENTER);
        rightPanel.setBorder(new EmptyBorder(2,2,2,2));
        Box rightButtonPanel = Box.createVerticalBox();
        rightButtonPanel.add(upButton);
        rightButtonPanel.add(downButton);
        rightButtonPanel.setBorder(new EmptyBorder(2,2,2,2));
        rightPanel.add(rightButtonPanel, BorderLayout.LINE_END);
        Box actionButtons = Box.createHorizontalBox();
        actionButtons.add(close);
        actionButtons.add(execute);
        actionButtons.setBorder(new EmptyBorder(2,2,2,2));
        rightPanel.add(actionButtons, BorderLayout.PAGE_END);
        content.add(rightPanel);
        
        // intercept the insert/remove buttons
        insertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = tabColList.getSelectedValue();
                if (selected!=null) {
                    selectedColumns.addElement(selected);
                    tableColumns.removeElement(selected);
                }
            }
        });
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = keyColList.getSelectedValue();
                if (selected!=null) {
                    tableColumns.addElement(selected);
                    selectedColumns.removeElement(selected);
                }
            }
        });
        
        // intercept the up/down buttons
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = keyColList.getSelectedValue();
                if (selected!=null) {
                    int currIndex = selectedColumns.indexOf(selected);
                    if (currIndex>0) {
                        Object swap = selectedColumns.get(currIndex-1);
                        selectedColumns.setElementAt(selected, currIndex-1);
                        selectedColumns.setElementAt(swap, currIndex);
                        keyColList.setSelectedIndex(currIndex-1);
                    }
                }
            }
        });
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = keyColList.getSelectedValue();
                if (selected!=null) {
                    int currIndex = selectedColumns.indexOf(selected);
                    if (currIndex<(selectedColumns.size()-1)) {
                        Object swap = selectedColumns.get(currIndex+1);
                        selectedColumns.setElementAt(selected, currIndex+1);
                        selectedColumns.setElementAt(swap, currIndex);
                        keyColList.setSelectedIndex(currIndex+1);
                    }
                }
            }
        });
        
        // intercept the close button
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });
        
        // intercept the execute button
        execute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (validateFields()) hide();
            }
        });
        // make it the default button.
        this.getRootPane().setDefaultButton(execute);
        
        // set size of window
        this.pack();
    }
    
    /**
     * Validates the fields.
     */
    private boolean validateFields() {
        List messages = new ArrayList();
        
        if (this.selectedColumns.isEmpty()) {
            messages.add(BuilderBundle.getString("keyColumnsEmpty"));
        }
        
        if (!messages.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    messages.toArray(new String[0]),
                    BuilderBundle.getString("validationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        
        return messages.isEmpty();
    }
    
    public static List createPrimaryKey(SchemaTabSet schemaTabSet, Table table) {
        KeyEditorDialog dialog = new KeyEditorDialog(schemaTabSet,
                table,
                BuilderBundle.getString("newPKDialogTitle"),
                BuilderBundle.getString("addButton"),
                null);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
        return Arrays.asList(dialog.selectedColumns.toArray());
    }
    
    public static List createForeignKey(SchemaTabSet schemaTabSet, Table table) {
        KeyEditorDialog dialog = new KeyEditorDialog(schemaTabSet,
                table,
                BuilderBundle.getString("newFKDialogTitle"),
                BuilderBundle.getString("addButton"),
                null);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
        return Arrays.asList(dialog.selectedColumns.toArray());
    }
    
    public static List editKey(SchemaTabSet schemaTabSet, Key key) {
        KeyEditorDialog dialog = new KeyEditorDialog(schemaTabSet,
                key.getTable(),
                BuilderBundle.getString("editKeyDialogTitle"),
                BuilderBundle.getString("modifyButton"),
                key.getColumns());
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
        return Arrays.asList(dialog.selectedColumns.toArray());
    }
}
