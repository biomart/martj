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


import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

/**
 * Class MartViewEditor extends JFrame..
 *
 * <p>This class contains the main function, it draws the external frame, toolsbar, menus.
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetView
 */

public class MartViewEditor extends JFrame {

    private JDesktopPane desktop;
    static private final String newline = "\n";
    private JFileChooser fc;
    static final private String NEW = "New";
    static final private String OPEN = "Open";
    static final private String SAVE = "Save";
    static final private String COPY = "Copy";
    static final private String CUT = "Cut";
    static final private String PASTE = "Paste";
    static final private String DELETE = "Delete";
    static final private String UNDO = "Undo";
    static final private String REDO = "Redo";
    static final private String HELP = "Copy";
    private File file = null;

    public MartViewEditor() {
        super("Mart View Editor");
        JFrame.setDefaultLookAndFeelDecorated(true);
        fc = new JFileChooser();

        //Create the toolbar.
        JToolBar toolBar = new JToolBar("Still draggable");
        addButtons(toolBar);

        //Make the big window be indented 50 pixels from each edge
        //of the screen.
        int inset = 100;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                screenSize.width - inset * 2,
                screenSize.height - inset * 2);

        //Set up the GUI.
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(toolBar, BorderLayout.NORTH);

        desktop = new JDesktopPane();
        this.getContentPane().add(desktop, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());

        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    protected void addButtons(JToolBar toolBar) {
        JButton button = null;

        //first button
        button = makeNavigationButton("new", NEW,
                "Create a new dataset view",
                "new");
        toolBar.add(button);

        //second button
        button = makeNavigationButton("open", OPEN,
                "Open a dataset view",
                "open");
        toolBar.add(button);

        //third button
        button = makeNavigationButton("save", SAVE,
                "Save dataset view",
                "save");
        toolBar.add(button);

        button = makeNavigationButton("copy", COPY,
                "Copy a tree node",
                "copy");
        toolBar.add(button);

        button = makeNavigationButton("cut", CUT,
                "Cut a tree node","cut");
        toolBar.add(button);

        button = makeNavigationButton("paste", PASTE,
                "Paste tree node",
                "paste");
        toolBar.add(button);

        button = makeNavigationButton("undo", UNDO,
                "Undo",
                "undo");
        toolBar.add(button);

        button = makeNavigationButton("redo", REDO,
                "Redo",
                "redo");
        toolBar.add(button);

    }

    protected JButton makeNavigationButton(String imageName,
                                           String actionCommand,
                                           String toolTipText,
                                           String altText) {
        //Look for the image.
        String imgLocation = imageName
                + ".gif";
        URL imageURL = DatasetViewTree.class.getResource(imgLocation);

        //Create and initialize the button.
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(new MenuActionListener());

        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            System.err.println("Resource not found: "
                    + imgLocation);
        }

        return button;
    }

    protected JMenuBar createMenuBar() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.getAccessibleContext().setAccessibleDescription(
                "the file related menu");
        menuBar.add(menu);

        //a group of JMenuItems
        ImageIcon icon = createImageIcon("new.gif");
        menuItem = new JMenuItem("New Dataset View", icon);
        MartViewEditor.MenuActionListener menuActionListener = new MartViewEditor.MenuActionListener();
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        // menuItem.setAccelerator(KeyStroke.getKeyStroke(
        // KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Creates a new file");
        menu.add(menuItem);

        icon = createImageIcon("open.gif");
        menuItem = new JMenuItem("Open Dataset View", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_O);
        menu.add(menuItem);

        icon = createImageIcon("save.gif");
        menuItem = new JMenuItem("Save", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_S);
        menu.add(menuItem);

        menuItem = new JMenuItem("Save as");
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_A);
        menu.add(menuItem);

        //a group of radio button menu items
        menu.addSeparator();
        menuItem = new JMenuItem("Print XML");
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_P);
        menu.add(menuItem);

        //a group of check box menu items
        menu.addSeparator();
        menuItem = new JMenuItem("Exit");
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_X);
        menu.add(menuItem);

        //Build edit menu in the menu bar.
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menu.getAccessibleContext().setAccessibleDescription(
                "this is the edit menu");
        menuBar.add(menu);
        icon = createImageIcon("undo.gif");
        menuItem = new JMenuItem("Undo", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_U); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "undo");
        menu.add(menuItem);
        icon = createImageIcon("redo.gif");
        menuItem = new JMenuItem("Redo", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "redo");
        menu.add(menuItem);
        menu.addSeparator();
        icon = createImageIcon("cut.gif");
        menuItem = new JMenuItem("Cut", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "cuts to clipboard");
        menu.add(menuItem);
        icon = createImageIcon("copy.gif");
        menuItem = new JMenuItem("Copy", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "copies to clipboard");
        menu.add(menuItem);
        icon = createImageIcon("paste.gif");
        menuItem = new JMenuItem("Paste", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "pastes from clipboard");
        menu.add(menuItem);

        menu.addSeparator();
        icon = createImageIcon("remove.gif");
        menuItem = new JMenuItem("Delete", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "deletes");
        menu.add(menuItem);
        icon = createImageIcon("add.gif");
        menuItem = new JMenuItem("Insert", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "inserts");
        menu.add(menuItem);

        //Build view menu in the menu bar.
        menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);
        menu.getAccessibleContext().setAccessibleDescription(
                "this is the view menu");
        menuBar.add(menu);

        //Build help menu in the menu bar.
        icon = createImageIcon("help.gif");
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menu.getAccessibleContext().setAccessibleDescription(
                "this is the help menu");
        menuBar.add(menu);
        menuItem = new JMenuItem("Docomentation", icon);
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_M); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "documentation");
        menu.add(menuItem);
        menuItem = new JMenuItem("About...");
        menuItem.addActionListener(menuActionListener);
        menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
        menuItem.getAccessibleContext().setAccessibleDescription(
                "inserts");
        menu.add(menuItem);

        return menuBar;
    }


    //Create a new internal frame.
    protected void createFrame(File file) {

            DatasetViewTreeWidget frame = new DatasetViewTreeWidget(file,this);
            frame.setVisible(true);
            desktop.add(frame);
            try {
                frame.setSelected(true);
            } catch (java.beans.PropertyVetoException e) {
            }

    }

    //Quit the application.
    protected void quit() {
        System.exit(0);
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        MartViewEditor frame = new MartViewEditor();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ImageIcon icon = createImageIcon("MartView_cube.gif");
        frame.setIconImage(icon.getImage());
        //Display the window.
        frame.setVisible(true);
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = DatasetViewTreeWidget.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(
                            UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception e) {
                }
                createAndShowGUI();
            }
        });
    }

// Inner class that handles Menu Action Events
    protected class MenuActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Cut"))
                cut();
            else if (e.getActionCommand().equals("Copy"))
                copy();
            else if (e.getActionCommand().equals("Paste"))
                paste();
            else if (e.getActionCommand().equals("Insert"))
                insert();
            else if (e.getActionCommand().equals("Delete"))
                delete();
            else if (e.getActionCommand().startsWith("New"))
                newDatasetView();
            else if (e.getActionCommand().startsWith("Open"))
                openDatasetView();
            else if (e.getActionCommand().equals("Exit"))
                exit();
            else if (e.getActionCommand().equals("Save"))
                save();
            else if (e.getActionCommand().equals("Save as"))
                save_as();
            else if (e.getActionCommand().startsWith("Undo"))
                undo();
            else if (e.getActionCommand().startsWith("Redo"))
                redo();
        }
    }

    public void cut() {
         ((DatasetViewTreeWidget)desktop.getSelectedFrame()).cut();
    }

    public void copy() {
         ((DatasetViewTreeWidget)desktop.getSelectedFrame()).copy();
    }

    public void paste() {
         ((DatasetViewTreeWidget)desktop.getSelectedFrame()).paste();
    }

    public void insert() {
          //((DatasetViewTreeWidget)desktop.getSelectedFrame()).insert();
    }

    public void delete() {
          ((DatasetViewTreeWidget)desktop.getSelectedFrame()).delete();
    }

    public void newDatasetView() {
        createFrame(null);
    }

    public void setFileChooserPath(File file){
        this.file = file;
    }

    public File getFileChooserPath(){
        return file;
    }
    public void openDatasetView() {

        XMLFileFilter filter = new XMLFileFilter();
        fc.addChoosableFileFilter(filter);
        int returnVal = fc.showOpenDialog(this.getContentPane());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            createFrame(file);
            //This is where a real application would open the file.
            System.out.println("Opening: " + file.getName() + "." + newline);
        } else {
            System.out.println("Open command cancelled by user." + newline);
        }

    }

    public void save() {
        ((DatasetViewTreeWidget)desktop.getSelectedFrame()).save();
    }

    public void save_as() {
       ((DatasetViewTreeWidget)desktop.getSelectedFrame()).save_as();
    }

    public void exit() {
        System.exit(0);
    }

    public void undo() {

    }

    public void redo() {

    }
}


