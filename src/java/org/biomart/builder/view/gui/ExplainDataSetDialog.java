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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.resources.BuilderBundle;

/**
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 10th May 2006
 * @since 0.1
 */
public class ExplainDataSetDialog extends JDialog {
    
    /**
     * Creates a new instance of ExplainDataSetDialog
     */
    private ExplainDataSetDialog(SchemaTabSet schemaTabSet, DataSetTable dsTable, DataSetColumn dsColumn) {
        super(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder(),
                (dsColumn==null ? 
                    BuilderBundle.getString("explainTableDialogTitle", dsTable.getName()) :
                    BuilderBundle.getString("explainColumnDialogTitle", new String[]{dsTable.getName(), dsColumn.getName()})),
                true);
        
        // Useful things
        JButton close = new JButton(BuilderBundle.getString("closeButton"));
        
        // Make the content.
        JPanel content = new JPanel(new BorderLayout());
        this.setContentPane(content);
        
        // Compute the diagram.
        DataSetTabSet dsTabSet = schemaTabSet.getDataSetTabSet();
        ExplainDataSetDiagram diagram = new ExplainDataSetDiagram(dsTabSet, dsTable);
        ExplainDataSetContext context = new ExplainDataSetContext(dsTabSet, (DataSet)dsTable.getSchema());
        if (dsColumn!=null) context.setSelectedColumn(dsColumn);
        diagram.setDiagramContext(context);
        
        // Work out what size we want the diagram to be.
        Dimension size = diagram.getPreferredSize();
        size.width = Math.max(100, Math.min(size.width, 400));
        size.height = Math.max(100, Math.min(size.height, 400));
        content.setPreferredSize(size);
        
        // Add the diagram to the top part.
        content.add(new JScrollPane(diagram), BorderLayout.CENTER);

        // Add the close button to the bottom.
        content.add(close, BorderLayout.PAGE_END);

        // intercept the close button
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });
        // make it the default button.
        this.getRootPane().setDefaultButton(close);
        
        // set size of window
        this.pack();
    }

    /**
     * Static method which allows the user to create a new table provider.
     */
    public static void showTableExplanation(SchemaTabSet schemaTabSet, DataSetTable table) {
        ExplainDataSetDialog dialog = new ExplainDataSetDialog(schemaTabSet, table, null);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
    }
    
    /**
     * Static method which allows the user to create a new table provider.
     */
    public static void showColumnExplanation(SchemaTabSet schemaTabSet, DataSetColumn column) {
        ExplainDataSetDialog dialog = new ExplainDataSetDialog(schemaTabSet, (DataSetTable)column.getTable(), column);
        dialog.setLocationRelativeTo(schemaTabSet.getDataSetTabSet().getMartTabSet().getMartBuilder());
        dialog.show();
    }
}
