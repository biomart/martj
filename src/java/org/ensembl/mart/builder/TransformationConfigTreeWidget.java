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
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
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

//import org.ensembl.mart.lib.config.AttributePage;

import org.ensembl.mart.builder.config.*;

/**
 * TransformationConfigTreeWidget extends internal frame.
 *
 *
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.DatasetConfig
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

	
    public TransformationConfigTreeWidget(File file, MartBuilder builder, TransformationConfig dsv, String user, String dataset, String internalName, String schema){

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
	   //  this.setFrameIcon(createImageIcon(MartEditor.IMAGE_DIR+"MartConfig_cube.gif"));
            
            if (file == null) {
            	if (user == null){
            	  if (schema == null){	
                    config = new TransformationConfig("new");
                    //config.setDSConfigAdaptor(new SimpleDSConfigAdaptor(config)); //prevents lazyLoading
                    config.addDataset(new Dataset("new","mainTable"));
            	  }
            	  
            	  //else{  NAIVE CREATION	
            	  //	config = MartEditor.getDatabaseTransformationConfigUtils().getNaiveTransformationConfigFor(schema,dataset);
            	  //}
            	}
            	else{//Importing config
//              ignore cache, do not loadFully, include hidden members
					//DSConfigAdaptor adaptor = new DatabaseDSConfigAdaptor(MartBuilder.getDetailedDataSource(),user, true, false, true);
					//TransformationConfigIterator configs = adaptor.getTransformationConfigs();
					//while (configs.hasNext()){
            //TransformationConfig lconfig = (TransformationConfig) configs.next();
					  //if (lconfig.getDataset().equals(dataset) && lconfig.getInternalName().equals(internalName)){
					    //config = lconfig;
					    //break;
					  //}
					//}
					
            	}
            } else {// OPEN FROM FILE
                URL url = file.toURL();
//            ignore cache, include hidden members
                
                // old code
                /*
                DSConfigAdaptor adaptor = new URLDSConfigAdaptor(url,true, true);

                // only config one in the file so get that one
                config = (TransformationConfig) adaptor.getTransformationConfigs().next();
				*/
				
				// new code without using adaptor
				TransformationConfigXMLUtils xmlUtils = new TransformationConfigXMLUtils(true);
				config = xmlUtils.getTransformationConfigForXMLStream(InputSourceUtil.getStreamForURL(url));
				xmlUtils.loadTransformationConfigWithDocument( config, xmlUtils.getDocumentForXMLStream( InputSourceUtil.getStreamForURL( url ) ) );
				
				
                
            }
          }
          else{
          	//config = new TransformationConfig(dsv, true, false);
			config = new TransformationConfig(dsv);
        	     
          }
            //this.setTitle(config.getInternalName());
             
            //config.getDataset();
            //config.getAdaptor().getDataSource();
          
			//this.setTitle(schema + "." + config.getDataset());
            JFrame.setDefaultLookAndFeelDecorated(true);

            TransformationConfigAttributesTable attrTable = new TransformationConfigAttributesTable(
                    config, this);
            tree = new TransformationConfigTree(config,
                    this, attrTable);
                    
			//DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
			//renderer.setTextNonSelectionColor(Color.red);
			//tree.setCellRenderer(renderer);        
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

    /**
     * Test purposes only. Creates a frame with a JTree containing
     * a presepecified TransformationConfig.dtd compatible configuration file.
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {


    }

    /**
     * @return
     */
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

    /**
     * @param config
     */
    public void setTransformationConfig(TransformationConfig config) {
        clearTransformationConfig();
        transformationConfig = config;
        loadTransformationConfig();
    }

    /**
     * Loads the transformationConfig by creating a tree to represent it
     * and displaying it.
     */
    private void loadTransformationConfig() {


    }

    /**
     * Removes current dataset config if one is loaded, otherwise does nothing.
     */
    private void clearTransformationConfig() {

    }

    public void save(){
        tree.save();
    }

    public void save_as(){
           tree.save_as();
       }

	//public void export() throws ConfigurationException{
	//    tree.export();
	//}

    public void cut(){
        tree.cut();
    }

    public void copy(){
        tree.copy();
    }

    public void paste(){
        tree.paste();
    }


    public void insert(){
       // tree.insert();
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


    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = TransformationConfigTreeWidget.class.getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
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
		//try{
		//	TransformationConfigTreeWidget dw = (TransformationConfigTreeWidget) e.getInternalFrame();
		//	if (!MartEditor.getDatabaseDatasetConfigUtils().isDatasetConfigChanged(null,dw.getDatasetConfig())){// current and db out of synch
		//		int returnType = JOptionPane.showConfirmDialog(null,"Close?","Changes not exported",JOptionPane.OK_CANCEL_OPTION);
		//		if (returnType == 0){
		//			e.getInternalFrame().dispose();
		//		}
		//	}
		//	else{
		//		e.getInternalFrame().dispose();
		//	}
		//}
		//catch(Exception exc){
			// connection changed already
		//	e.getInternalFrame().dispose();
		//}
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
		TransformationConfigTreeNode node =
				(TransformationConfigTreeNode)value;
		BaseConfigurationObject nodeObject = (BaseConfigurationObject) node.getUserObject();		
		if (nodeObject.getAttribute("hidden") != null && nodeObject.getAttribute("hidden").equals("true")){
			return true;
		}

		return false;
	}
}
