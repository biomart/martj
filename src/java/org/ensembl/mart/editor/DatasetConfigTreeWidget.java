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

package org.ensembl.mart.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.BaseConfigurationObject;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DatasetConfigIterator;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.SimpleDSConfigAdaptor;
import org.ensembl.mart.lib.config.URLDSConfigAdaptor;



/**
 * DatasetConfigTreeWidget extends internal frame.
 *
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetConfig
 */
public class DatasetConfigTreeWidget extends JInternalFrame {

    private DatasetConfig datasetConfig = null;
    private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;
    private JDesktopPane desktop;
    private GridBagConstraints constraints;
    private DatasetConfigTree tree;
    private File file = null;
    private MartEditor editor;

	
    public DatasetConfigTreeWidget(File file, MartEditor editor, DatasetConfig dsv, String user, String dataset, String internalName, String database) {

        super("Dataset Tree " + (++openFrameCount),
                true, //resizable
                true, //closable
                true, //maximizable
                true);//iconifiable
        this.editor = editor;
        try {
		  DatasetConfig config = null;	
          if (dsv == null){	
	   //  this.setFrameIcon(createImageIcon(MartEditor.IMAGE_DIR+"MartConfig_cube.gif"));
            
            if (file == null) {
            	if (user == null){
            	  if (database == null){	
                    config = new DatasetConfig("new", "new", "new");
                    config.setDSConfigAdaptor(new SimpleDSConfigAdaptor(config)); //prevents lazyLoading
                    config.addFilterPage(new FilterPage("new"));
                    config.addAttributePage(new AttributePage("new"));
            	  }
            	  
            	  else{
            	  	config = MartEditor.getDatabaseDatasetConfigUtils().getNaiveDatasetConfigFor(database,dataset);
            	  }
            	}
            	else{//Importing config
//              ignore cache, do not validate, include hidden members
					DSConfigAdaptor adaptor = new DatabaseDSConfigAdaptor(MartEditor.getDetailedDataSource(),user, true, false, true);
					DatasetConfigIterator configs = adaptor.getDatasetConfigs();
					while (configs.hasNext()){
            DatasetConfig lconfig = (DatasetConfig) configs.next();
					  if (lconfig.getDataset().equals(dataset) && lconfig.getInternalName().equals(internalName)){
					    config = lconfig;
					    break;
					  }
					}
					
            	}
            } else {
                URL url = file.toURL();
//            ignore cache, do not validate, include hidden members
                DSConfigAdaptor adaptor = new URLDSConfigAdaptor(url,true, false, true);

                // only config one in the file so get that one
                config = (DatasetConfig) adaptor.getDatasetConfigs().next();
            }
          }
          else{
          	config = new DatasetConfig(dsv, true, false);
          }
            this.setTitle(config.getInternalName());

            JFrame.setDefaultLookAndFeelDecorated(true);

            DatasetConfigAttributesTable attrTable = new DatasetConfigAttributesTable(
                    config, this);
            tree = new DatasetConfigTree(config,
                    this, attrTable);
                    
			//DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
			//renderer.setTextNonSelectionColor(Color.red);
			//tree.setCellRenderer(renderer);        
			tree.setCellRenderer(new MyRenderer());        
            // for update         
            setDatasetConfig(config);
            
            JScrollPane treeScrollPane = new JScrollPane(tree);
            JScrollPane tableScrollPane = new JScrollPane(attrTable);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    treeScrollPane, tableScrollPane);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(150);

            //Provide minimum sizes for the two components in the split pane.
            Dimension minimumSize = new Dimension(100, 50);
            treeScrollPane.setMinimumSize(minimumSize);
            tableScrollPane.setMinimumSize(minimumSize);

            this.getContentPane().add(splitPane);

            //...Then set the window size or call pack...
            setSize(500, 400);

            //Set the window's location.
            setLocation(xOffset * openFrameCount, yOffset * openFrameCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test purposes only. Creates a frame with a JTree containing
     * a presepecified DatasetConfig.dtd compatible configuration file.
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {


    }

    /**
     * @return
     */
    public DatasetConfig getDatasetConfig() {
        return datasetConfig;
    }
    
	public MartEditor getEditor() {
		return editor;
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

    /**
     * @param config
     */
    public void setDatasetConfig(DatasetConfig config) {
        clearDatasetConfig();
        datasetConfig = config;
        loadDatasetConfig();
    }

    /**
     * Loads the datasetConfig by creating a tree to represent it
     * and displaying it.
     */
    private void loadDatasetConfig() {


    }

    /**
     * Removes current dataset config if one is loaded, otherwise does nothing.
     */
    private void clearDatasetConfig() {

    }

    public void save(){
        tree.save();
    }

    public void save_as(){
           tree.save_as();
       }

	public void export() throws ConfigurationException{
	    tree.export();
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

	public void makeHidden(){
		tree.makeHidden();
	}


    public void insert(){
       // tree.insert();
    }

    public void delete(){
        tree.delete();
    }

    public void setFileChooserPath(File file){
        this.file = file;
        editor.setFileChooserPath(file);
    }

    public File getFileChooserPath(){
        return editor.getFileChooserPath();
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = DatasetConfigTreeWidget.class.getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
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

		if (isHidden(value)){
			setTextNonSelectionColor(Color.lightGray);
			setTextSelectionColor(Color.lightGray);
		} else{
		    setTextNonSelectionColor(Color.black);
			setTextSelectionColor(Color.black);
		}
		super.getTreeCellRendererComponent(
						tree, value, sel,
						expanded, leaf, row,
						hasFocus);			
		
		return this;
	}

	protected boolean isHidden(Object value) {
		DatasetConfigTreeNode node =
				(DatasetConfigTreeNode)value;
		BaseConfigurationObject nodeObject = (BaseConfigurationObject) node.getUserObject();		
		if (nodeObject.getAttribute("hidden") != null && nodeObject.getAttribute("hidden").equals("true")){
			return true;
		}

		return false;
	}
}
