/*
 * WindowContext.java
 *
 * Created on 19 April 2006, 09:43
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener behaviour by adding in DataSet-specific stuff.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.12, 16th May 2006
 * @since 0.1
 */
public class WindowContext extends SchemaContext {
    /**
     * Internal reference to our dataset.
     */
    private DataSet dataset;
    
    /**
     *
     * Creates a new instance of DataSetDiagramModifier over
     * a given dataset.
     *
     *
     * @param dataset the dataset we are attached to.
     */
    public WindowContext(DataSetTabSet datasetTabSet, DataSet dataset) {
        super(datasetTabSet);
        this.dataset = dataset;
    }
    
    /**
     * Retrieve our dataset.
     *
     * @return our dataset.
     */
    protected DataSet getDataSet() {
        return this.dataset;
    }
    
    /**
     * {@inheritDoc}
     */
    public void populateContextMenu(JPopupMenu contextMenu, Object object) {
        if (object==null || (object instanceof Schema)) {
            if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
            // Common stuff for background and Schema clicks.
            
            JMenuItem optimise = new JMenuItem(BuilderBundle.getString("optimiseDataSetTitle"));
            optimise.setMnemonic(BuilderBundle.getString("optimiseDataSetMnemonic").charAt(0));
            optimise.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestOptimiseDataSet(dataset);
                }
            });
            contextMenu.add(optimise);
        }
        
        else if (object instanceof Relation) {
            if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
            // Relation stuff
            final Relation relation = (Relation)object;
            
            boolean incorrect = relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT);
            boolean relationOneToOne = relation.getFKCardinality().equals(Cardinality.ONE);
            boolean relationMasked = this.dataset.getMaskedRelations().contains(relation);
            boolean relationConcated = this.dataset.getConcatOnlyRelations().contains(relation);
            boolean relationSubclassed = this.dataset.getSubclassedRelations().contains(relation);
            
            final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(BuilderBundle.getString("maskRelationTitle"));
            mask.setMnemonic(BuilderBundle.getString("maskRelationMnemonic").charAt(0));
            mask.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (mask.isSelected()) getDataSetTabSet().requestMaskRelation(dataset, relation);
                    else getDataSetTabSet().requestUnmaskRelation(dataset, relation);
                }
            });
            contextMenu.add(mask);
            if (incorrect) mask.setEnabled(false);
            if (relationMasked) mask.setSelected(true);
            
            final JCheckBoxMenuItem subclass = new JCheckBoxMenuItem(BuilderBundle.getString("subclassRelationTitle"));
            subclass.setMnemonic(BuilderBundle.getString("subclassRelationMnemonic").charAt(0));
            subclass.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (subclass.isSelected()) getDataSetTabSet().requestSubclassRelation(dataset, relation);
                    else getDataSetTabSet().requestUnsubclassRelation(dataset, relation);
                }
            });
            contextMenu.add(subclass);
            if (relationSubclassed) subclass.setSelected(true);
            if (incorrect || relationOneToOne || relationMasked || relationConcated) subclass.setEnabled(false);
            
            // Concat-only submenu, when parent selected unconcat, else show submenu
            // with change-relation option.
            JMenu concatSubmenu = new JMenu(BuilderBundle.getString("concatOnlyRelationTitle"));
            concatSubmenu.setMnemonic(BuilderBundle.getString("concatOnlyRelationMnemonic").charAt(0));
            ButtonGroup concatGroup = new ButtonGroup();
            
            JRadioButtonMenuItem none = new JRadioButtonMenuItem(BuilderBundle.getString("noneConcatTitle"));
            none.setMnemonic(BuilderBundle.getString("noneConcatMnemonic").charAt(0));
            none.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestUnconcatOnlyRelation(dataset, relation);
                }
            });
            concatGroup.add(none);
            concatSubmenu.add(none);
            if (this.dataset.getConcatRelationType(relation)==null) none.setSelected(true);
            
            JRadioButtonMenuItem comma = new JRadioButtonMenuItem(BuilderBundle.getString("commaConcatTitle"));
            comma.setMnemonic(BuilderBundle.getString("commaConcatMnemonic").charAt(0));
            comma.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestConcatOnlyRelation(dataset, relation, ConcatRelationType.COMMA);
                }
            });
            concatGroup.add(comma);
            concatSubmenu.add(comma);
            if (this.dataset.getConcatRelationType(relation)!=null && dataset.getConcatRelationType(relation).equals(ConcatRelationType.COMMA)) comma.setSelected(true);
            
            JRadioButtonMenuItem tab = new JRadioButtonMenuItem(BuilderBundle.getString("tabConcatTitle"));
            tab.setMnemonic(BuilderBundle.getString("tabConcatMnemonic").charAt(0));
            tab.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestConcatOnlyRelation(dataset, relation, ConcatRelationType.TAB);
                }
            });
            concatGroup.add(tab);
            concatSubmenu.add(tab);
            if (this.dataset.getConcatRelationType(relation)!=null && dataset.getConcatRelationType(relation).equals(ConcatRelationType.TAB)) tab.setSelected(true);
            
            JRadioButtonMenuItem space = new JRadioButtonMenuItem(BuilderBundle.getString("spaceConcatTitle"));
            space.setMnemonic(BuilderBundle.getString("spaceConcatMnemonic").charAt(0));
            space.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestConcatOnlyRelation(dataset, relation, ConcatRelationType.SPACE);
                }
            });
            concatGroup.add(space);
            concatSubmenu.add(space);
            if (this.dataset.getConcatRelationType(relation)!=null && dataset.getConcatRelationType(relation).equals(ConcatRelationType.SPACE)) space.setSelected(true);

            contextMenu.add(concatSubmenu);
            if (incorrect || relationOneToOne || relationMasked || relationSubclassed) concatSubmenu.setEnabled(false);
        }
        
        else if (object instanceof Key) {
            // Keys just show the parent table stuff.
            Table table = ((Key)object).getTable();
            this.populateContextMenu(contextMenu, table);
        }
        
        else if (object instanceof Column) {
            // Columns show the parent table stuff.
            Table table = ((Column)object).getTable();
            this.populateContextMenu(contextMenu, table);
        }
    }
    
    public void customiseAppearance(JComponent component, Object object) {
        
        if (object instanceof Relation) {

            Relation relation = (Relation)object;
            // Fade out all INFERRED_INCORRECT and MASKED relations.
            if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT) ||
                    this.dataset.getMaskedRelations().contains(relation)) {
                component.setForeground(RelationComponent.MASKED_COLOUR);
            }
            // Highlight CONCAT-ONLY relations.
            else if (this.dataset.getConcatOnlyRelations().contains(relation)) {
                component.setForeground(RelationComponent.CONCAT_COLOUR);
            }
            // Highlight SUBCLASS relations.
            else if (this.dataset.getSubclassedRelations().contains(relation)) {
                component.setForeground(RelationComponent.SUBCLASS_COLOUR);
            }
            // All others are normal.
            else {
                component.setForeground(RelationComponent.NORMAL_COLOUR);
            }
        }
        
        else if (object instanceof Key) {
            
            Key key = (Key)object;
            // Fade out all INFERRED_INCORRECT relations.
            if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) {
                component.setForeground(KeyComponent.MASKED_COLOUR);
            }
            // Highlight all HANDMADE relations.
            else if (key.getStatus().equals(ComponentStatus.HANDMADE)) {
                component.setForeground(KeyComponent.HANDMADE_COLOUR);
            }
            // All others are normal.
            else {
                component.setForeground(KeyComponent.NORMAL_COLOUR);
            }
            
            // Remove drag-and-drop.
            component.removeMouseListener(SchemaContext.dragAdapter);
        }
    }
}
