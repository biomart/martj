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

import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetViewUtils;

/**
 * DatasetViewTreeWidget extends internal frame.
 *
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetView
 */
public class DatasetViewTreeWidget extends JInternalFrame {

    private DatasetView datasetView = null;
    private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;
    private JDesktopPane desktop;
    private GridBagConstraints constraints;
    private DatasetViewTree tree;
    private File file = null;
    private MartViewEditor editor;

    public DatasetViewTreeWidget(File file, MartViewEditor editor, String user, String dataset, String database) {

        super("Dataset Tree " + (++openFrameCount),
                true, //resizable
                true, //closable
                true, //maximizable
                true);//iconifiable
        this.editor = editor;
        try {
	   //  this.setFrameIcon(createImageIcon(MartViewEditor.IMAGE_DIR+"MartView_cube.gif"));
            DatasetView view = new DatasetView();
            if (file == null) {
            	if (user == null){
            	  if (database == null){	
                    view = new DatasetView("new", "new", "new");
                    view.addFilterPage(new FilterPage("new"));
                    view.addAttributePage(new AttributePage("new"));
            	  }
            	  else{
            	  	view = DatabaseDatasetViewUtils.getNaiveDatasetViewFor(MartViewEditor.getDetailedDataSource(),database,dataset);
            	  }
            	}
            	else{
					DSViewAdaptor adaptor = new DatabaseDSViewAdaptor(MartViewEditor.getDetailedDataSource(),user);
					DatasetView views[] = adaptor.getDatasetViews();
					for (int k =0; k < views.length;k++){
					  if (views[k].getDataset().equals(dataset)){
					    view = views[k];
					    break;
					  }
					}
					
            	}
            } else {
                URL url = file.toURL();
                DSViewAdaptor adaptor = new URLDSViewAdaptor(url, true);

                // only view one in the file so get that one
                view = adaptor.getDatasetViews()[0];
            }
            this.setTitle(view.getInternalName());

            JFrame.setDefaultLookAndFeelDecorated(true);

            DatasetViewAttributesTable attrTable = new DatasetViewAttributesTable(
                    view, this);
            tree = new DatasetViewTree(view,
                    this, attrTable);
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
     * a presepecified DatasetView.dtd compatible configuration file.
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {


    }

    /**
     * @return
     */
    public DatasetView getDatasetView() {
        return datasetView;
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
     * @param view
     */
    public void setDatasetView(DatasetView view) {
        clearDatasetView();
        datasetView = view;
        loadDatasetView();
    }

    /**
     * Loads the datasetView by creating a tree to represent it
     * and displaying it.
     */
    private void loadDatasetView() {


    }

    /**
     * Removes current dataset view if one is loaded, otherwise does nothing.
     */
    private void clearDatasetView() {

    }

    public void save(){
        tree.save();
    }

    public void save_as(){
           tree.save_as();
       }

	public void export(){
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
        java.net.URL imgURL = DatasetViewTreeWidget.class.getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

}
