/*
 * TableProviderDiagramComponent.java
 *
 * Created on 19 April 2006, 15:36
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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 25th April 2006
 * @since 0.1
 */
public class TableProviderDiagramComponent extends BoxShapedDiagramComponent {
    /**
     * A map of keys to key components.
     */
    private Map keyToKeyComponent = new HashMap();
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public TableProviderDiagramComponent(TableProvider tableProvider, Diagram parentDisplay) {
        super(tableProvider, parentDisplay);
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        // Create the border and set up the colors and fonts.
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.setForeground(Color.BLACK);
        this.setBackground(Color.PINK);
        // Add the label.
        JLabel label = new JLabel(tableProvider.getName());
        label.setFont(Font.decode("Serif-BOLD-10"));
        this.add(label);
        // Now the keys.
        for (Iterator i = tableProvider.getExternalKeys().iterator(); i.hasNext(); ) {
            Key key = (Key)i.next();
            KeyDiagramComponent keyComponent = new KeyDiagramComponent(key, parentDisplay, this);
            this.keyToKeyComponent.put(key, keyComponent);
            this.add(keyComponent);
        }
    }
    
    /**
     * Gets our tableProvider.
     */
    private TableProvider getTableProvider() {
        return (TableProvider)this.getObject();
    }
    
    /**
     * Gets a key component.
     */
    public Map getKeyComponents() {
        return this.keyToKeyComponent;
    }
    
    /**
     * Count the relations attached to our inner object.
     */
    public int countExternalRelations() {
        return this.getTableProvider().getExternalRelations().size();
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        // Extend it for this table here.
        contextMenu.addSeparator();
        
        JMenuItem showTables = new JMenuItem(BuilderBundle.getString("showTablesTitle"));
        showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic").charAt(0));
        showTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = getDiagram().getWindowTabSet().getTableProviderTabSet().indexOfTab(getTableProvider().getName());
                getDiagram().getWindowTabSet().getTableProviderTabSet().setSelectedIndex(index);
            }
        });
        contextMenu.add(showTables);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameTblProvTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameTblProvMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getWindowTabSet().getTableProviderTabSet().renameTableProvider(getTableProvider());
            }
        });
        contextMenu.add(rename);
        
        JMenuItem modify = new JMenuItem(BuilderBundle.getString("modifyTblProvTitle"));
        modify.setMnemonic(BuilderBundle.getString("modifyTblProvMnemonic").charAt(0));
        modify.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getWindowTabSet().getTableProviderTabSet().requestModifyTableProvider(getTableProvider());
            }
        });
        contextMenu.add(modify);
        
        JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseTblProvTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseTblProvMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getWindowTabSet().getTableProviderTabSet().synchroniseTableProvider(getTableProvider());
            }
        });
        contextMenu.add(sync);
        
        JMenuItem test = new JMenuItem(BuilderBundle.getString("testTblProvTitle"));
        test.setMnemonic(BuilderBundle.getString("testTblProvMnemonic").charAt(0));
        test.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getWindowTabSet().getTableProviderTabSet().testTableProvider(getTableProvider());
            }
        });
        contextMenu.add(test);
        
        JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeTblProvTitle"));
        remove.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getWindowTabSet().getTableProviderTabSet().confirmRemoveTableProvider(getTableProvider());
            }
        });
        contextMenu.add(remove);
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
    
    /**
     * Set up the colours etc. for this component. Flags have already been set.
     */
    protected void setComponentColours() {
        // boolean xyFlagSet = this.getDiagram().getFlag(ComponentView.XYFLAG);
    }
}
