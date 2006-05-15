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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JMenu;
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
 * @version 0.1.7, 12th May 2006
 * @since 0.1
 */
public class SchemaComponent extends BoxShapedComponent {    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public SchemaComponent(Schema schema, Diagram diagram) {
        super(schema, diagram);
        this.setLayout(new GridLayout(0,1));
        this.recalculateDiagramComponent();
    }
    
    public void recalculateDiagramComponent() {
        this.removeAll();
        Schema schema = this.getSchema();
        // Create the border and set up the colors and fonts.
        this.setBackground(Color.PINK);
        // Add the label.
        JLabel label = new JLabel(schema.getName());
        label.setFont(Font.decode("Serif-BOLD-10"));
        this.add(label);
        // Is it a group?
        if (schema instanceof SchemaGroup) {
            StringBuffer sb = new StringBuffer();
            sb.append(BuilderBundle.getString("schemaGroupContains"));
            for (Iterator i = ((SchemaGroup)schema).getSchemas().iterator(); i.hasNext(); ) {
                Schema s = (Schema)i.next();
                sb.append(s.getName());
                if (i.hasNext()) sb.append(", ");
            }
            label = new JLabel(sb.toString());
            label.setFont(Font.decode("Serif-BOLDITALIC-10"));
            this.add(label);
        }
        // Now the keys.
        for (Iterator i = schema.getExternalKeys().iterator(); i.hasNext(); ) {
            Key key = (Key)i.next();
            KeyComponent keyComponent = new KeyComponent(key, this.getDiagram(), this);
            this.addSubComponent(key, keyComponent);
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
        
        JMenuItem showTables = new JMenuItem(BuilderBundle.getString("showTablesTitle"));
        showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic").charAt(0));
        showTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = getDiagram().getDataSetTabSet().getSchemaTabSet().indexOfTab(schema.getName());
                getDiagram().getDataSetTabSet().getSchemaTabSet().setSelectedIndex(index);
            }
        });
        contextMenu.add(showTables);
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getGroupContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        
        JMenuItem showTables = new JMenuItem(BuilderBundle.getString("showTablesTitle"));
        showTables.setMnemonic(BuilderBundle.getString("showTablesMnemonic").charAt(0));
        showTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int index = getDiagram().getDataSetTabSet().getSchemaTabSet().indexOfTab(getSchemaGroup().getName());
                getDiagram().getDataSetTabSet().getSchemaTabSet().setSelectedIndex(index);
            }
        });
        contextMenu.add(showTables);
                
        JMenu groupMembers = new JMenu(BuilderBundle.getString("groupMembersTitle"));
        groupMembers.setMnemonic(BuilderBundle.getString("groupMembersMnemonic").charAt(0));
        contextMenu.add(groupMembers);
        
        for (Iterator i = this.getSchemaGroup().getSchemas().iterator(); i.hasNext(); ) {
            final Schema schema = (Schema)i.next();
            JMenu schemaMenu = new JMenu(schema.getName());
            
            JMenuItem renameM = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
            renameM.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
            renameM.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDiagram().getDataSetTabSet().getSchemaTabSet().requestRenameSchema(schema, true);
                }
            });
            schemaMenu.add(renameM);
            
            JMenuItem modifyM = new JMenuItem(BuilderBundle.getString("modifySchemaTitle"));
            modifyM.setMnemonic(BuilderBundle.getString("modifySchemaMnemonic").charAt(0));
            modifyM.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDiagram().getDataSetTabSet().getSchemaTabSet().requestModifySchema(schema);
                }
            });
            schemaMenu.add(modifyM);
            
            JMenuItem testM = new JMenuItem(BuilderBundle.getString("testSchemaTitle"));
            testM.setMnemonic(BuilderBundle.getString("testSchemaMnemonic").charAt(0));
            testM.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDiagram().getDataSetTabSet().getSchemaTabSet().requestTestSchema(schema);
                }
            });
            schemaMenu.add(testM);
                        
            JMenuItem unGroup = new JMenuItem(BuilderBundle.getString("ungroupMemberTitle"));
            unGroup.setMnemonic(BuilderBundle.getString("ungroupMemberMnemonic").charAt(0));
            unGroup.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDiagram().getDataSetTabSet().getSchemaTabSet().requestRemoveSchemaFromSchemaGroup(schema, getSchemaGroup());
                }
            });
            schemaMenu.add(unGroup);
            
            groupMembers.add(schemaMenu);
        }
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
}
