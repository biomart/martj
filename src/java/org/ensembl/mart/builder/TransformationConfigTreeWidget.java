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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.File;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.ensembl.mart.builder.lib.*;

/**
 * TransformationConfigTreeWidget extends internal frame.
 *
 *
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.TransformationConfig
 */

public class TransformationConfigTreeWidget extends JInternalFrame{

    private TransformationConfig transformationConfig = null;
    private static int openFrameCount = 0;
    private static final int xOffset = 10, yOffset = 10;
    private JDesktopPane desktop;
    private GridBagConstraints constraints;
    private TransformationConfigTree tree;
    private File file = null;
    private MartBuilder builder;

	
    public TransformationConfigTreeWidget(String file, MartBuilder builder, TransformationConfig dsv, String user, String dataset, String internalName, String schema){

        super("Dataset Tree " + (++openFrameCount),
                true, //resizable
                true, //closable
                true, //maximizable
                true);//iconifiable
        this.builder = builder;
        this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        this.addInternalFrameListener(new CloseListener());
        try {
		  	TransformationConfig config = null;	
          	if (dsv == null){	
            	if (file != null) { // OPEN FROM FILE				
                	ConfigurationAdaptor configAdaptor = new ConfigurationAdaptor();			
					config = configAdaptor.getTransformationConfig(file);
            	}
          	}
          	else{
				config = (TransformationConfig) dsv.copy();  
          	}
            JFrame.setDefaultLookAndFeelDecorated(true);

            TransformationConfigAttributesTable attrTable = new TransformationConfigAttributesTable(
                    config, this);
            tree = new TransformationConfigTree(config,
                    this, attrTable);
			tree.setCellRenderer(new MyRenderer());        
            // for update         
            setTransformationConfig(config);
            
            JScrollPane treeScrollPane = new JScrollPane(tree);
            JScrollPane tableScrollPane = new JScrollPane(attrTable);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    treeScrollPane, tableScrollPane);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(350);

            //Provide minimum sizes for the two components in the split pane.
            Dimension minimumSize = new Dimension(350, 450);
            treeScrollPane.setMinimumSize(minimumSize);
            tableScrollPane.setMinimumSize(minimumSize);

            this.getContentPane().add(splitPane);

            //...Then set the window size or call pack...
            setSize(800, 400);

            //Set the window's location.
            setLocation(xOffset * openFrameCount, yOffset * openFrameCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TransformationConfig getTransformationConfig() {
        return transformationConfig;
    }
    
	public MartBuilder getBuilder() {
		return builder;
	}

    public void addAttributesTable(JTable table) {
        add(this.getContentPane(), new JScrollPane(table), constraints, 1, 0, 1, 1);
    }

    public void add(Container cont, Component component, GridBagConstraints constraints, int x, int y, int w, int h) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        cont.add(component, constraints);
    }

    public void setTransformationConfig(TransformationConfig config) {
        //clearTransformationConfig();
        transformationConfig = config;
        //loadTransformationConfig();
    }

    public void save(){
        tree.save();
    }

    public void save_as(){
           tree.save_as();
       }

    public void cut(){
        tree.cut();
    }

    public void copy(){
        tree.copy();
    }

    public void paste(){
        tree.paste();
    }

    public void delete(){
        tree.delete();
    }

    public void setFileChooserPath(File file){
        this.file = file;
        builder.setFileChooserPath(file);
    }

    public File getFileChooserPath(){
        return builder.getFileChooserPath();
    }

}

class CloseListener implements InternalFrameListener {
	public void internalFrameClosed(InternalFrameEvent e) {
	}
	public void internalFrameOpened(InternalFrameEvent e) {
	}
	public void internalFrameIconified(InternalFrameEvent e) {
	}
	public void internalFrameDeiconified(InternalFrameEvent e) {
	}
	public void internalFrameActivated(InternalFrameEvent e) {
	}
	public void internalFrameDeactivated(InternalFrameEvent e) {
	}
	public void internalFrameClosing(InternalFrameEvent e){
		e.getInternalFrame().dispose();
	}
}

class MyRenderer extends DefaultTreeCellRenderer {

	public MyRenderer() {	
	}

	public Component getTreeCellRendererComponent(
						JTree tree,
						Object value,
						boolean sel,
						boolean expanded,
						boolean leaf,
						int row,
						boolean hasFocus) {

		setTextNonSelectionColor(Color.black);
		setTextSelectionColor(Color.black);
		super.getTreeCellRendererComponent(
						tree, value, sel,
						expanded, leaf, row,
						hasFocus);			
		
		return this;
	}

}
