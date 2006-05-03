/*
 * SchemaComponent.java
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
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.resources.BuilderBundle;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 2nd May 2006
 * @since 0.1
 */
public class SchemaComponent extends BoxShapedComponent {
    /**
     * A map of keys to key components.
     */
    private Map keyToKeyComponent = new HashMap();
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public SchemaComponent(Schema schema, Diagram diagram) {
        super(schema, diagram);
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        // Create the border and set up the colors and fonts.
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.setForeground(Color.BLACK);
        this.setBackground(Color.PINK);
        // Add the label.
        JLabel label = new JLabel(schema.getName());
        label.setFont(Font.decode("Serif-BOLD-10"));
        this.add(label);
        // Is it a group?
        if (schema instanceof SchemaGroup) {
            StringBuffer sb = new StringBuffer();
            sb.append(BuilderBundle.getString("schemaGroupContains"));
            for (Iterator i = ((SchemaGroup)schema).getSchemas().keySet().iterator(); i.hasNext(); ) {
                String schemaName = (String)i.next();
                sb.append(schemaName);
                if (i.hasNext()) sb.append(", ");
            }
            label = new JLabel(sb.toString());
            label.setFont(Font.decode("Serif-BOLDITALIC-10"));
            this.add(label);
        }
        // Now the keys.
        for (Iterator i = schema.getExternalKeys().iterator(); i.hasNext(); ) {
            Key key = (Key)i.next();
            KeyComponent keyComponent = new KeyComponent(key, diagram, this);
            this.keyToKeyComponent.put(key, keyComponent);
            this.add(keyComponent);
        }
    }
    
    /**
     * Gets our tableProvider.
     */
    private Schema getSchema() {
        return (Schema)this.getObject();
    }
    
    /**
     * Gets our tableProvider.
     */
    private SchemaGroup getSchemaGroup() {
        return (SchemaGroup)this.getObject();
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
        return this.getSchema().getExternalRelations().size();
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getContextMenu() {
        if (this.getObject() instanceof SchemaGroup) return this.getGroupContextMenu();
        else return this.getSingleContextMenu(this.getSchema());
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getSingleContextMenu(final Schema schema) {
        JPopupMenu contextMenu = super.getContextMenu();
        // Extend it for this table here.
        contextMenu.addSeparator();
        
        JMenuItem showTables = new JMenuItem(BuilderBundle.getString("showTablesTitle"));
        showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic").charAt(0));
        showTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = getDiagram().getDataSetTabSet().getSchemaTabSet().indexOfTab(schema.getName());
                getDiagram().getDataSetTabSet().getSchemaTabSet().setSelectedIndex(index);
            }
        });
        contextMenu.add(showTables);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().renameSchema(schema);
            }
        });
        contextMenu.add(rename);
        
        JMenuItem modify = new JMenuItem(BuilderBundle.getString("modifySchemaTitle"));
        modify.setMnemonic(BuilderBundle.getString("modifySchemaMnemonic").charAt(0));
        modify.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().requestModifySchema(schema);
            }
        });
        contextMenu.add(modify);
        
        JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().synchroniseSchema(schema);
            }
        });
        contextMenu.add(sync);
        
        JMenuItem test = new JMenuItem(BuilderBundle.getString("testSchemaTitle"));
        test.setMnemonic(BuilderBundle.getString("testSchemaMnemonic").charAt(0));
        test.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().testSchema(schema);
            }
        });
        contextMenu.add(test);
        
        JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeSchemaTitle"));
        remove.setMnemonic(BuilderBundle.getString("removeSchemaMnemonic").charAt(0));
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().confirmRemoveSchema(schema);
            }
        });
        contextMenu.add(remove);
        
        JMenuItem addToGroup = new JMenuItem(BuilderBundle.getString("addToGroupTitle"));
        addToGroup.setMnemonic(BuilderBundle.getString("addToGroupMnemonic").charAt(0));
        addToGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().requestAddSchemaToSchemaGroup(schema);
            }
        });
        contextMenu.add(addToGroup);
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getGroupContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        // Extend it for this table here.
        contextMenu.addSeparator();
        
        JMenuItem showTables = new JMenuItem(BuilderBundle.getString("showTablesTitle"));
        showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic").charAt(0));
        showTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = getDiagram().getDataSetTabSet().getSchemaTabSet().indexOfTab(getSchemaGroup().getName());
                getDiagram().getDataSetTabSet().getSchemaTabSet().setSelectedIndex(index);
            }
        });
        contextMenu.add(showTables);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().renameSchema(getSchemaGroup());
            }
        });
        contextMenu.add(rename);
        
        JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().synchroniseSchema(getSchemaGroup());
            }
        });
        contextMenu.add(sync);
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
    
    /**
     * Set up the colours etc. for this component. Flags have already been set.
     */
    protected void setComponentColours() {
        // if getDiagram().getDiagramModifier() instanceof ...
    }
}
