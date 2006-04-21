/*
 * TableProviderView.java
 *
 * Created on 11 April 2006, 16:00
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of a {@link TableProvider} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 21st April 2006
 * @since 0.1
 */
public class TableProviderView extends View {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.WHITE;
    
    /**
     * Internal reference to the provider we are viewing.
     */
    private final TableProvider tableProvider;
    
    /**
     * Creates a new instance of TableProviderView over a given provider.
     *
     * @param tableProvider the given table provider.
     */
    public TableProviderView(WindowTabSet windowTabSet, TableProvider tableProvider) {
        super(windowTabSet);
        this.setBackground(TableProviderView.BACKGROUND_COLOUR);
        this.tableProvider = tableProvider;
    }
    
    /**
     * Returns our table provider.
     */
    public TableProvider getTableProvider() {
        return this.tableProvider;
    }
    
    /**
     * Syncs this table provider individually against the database.
     */
    public void synchroniseTableProvider() {
        try {
            this.tableProvider.synchronise();
            this.windowTabSet.synchronise();
        } catch (Throwable t) {
            this.windowTabSet.getSchemaManager().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Test this table provider individually against the database.
     */
    public void testTableProvider() {
        boolean passedTest = false;
        try {
            passedTest = tableProvider.test();
        } catch (Throwable t) {
            passedTest = false;
            this.windowTabSet.getSchemaManager().getMartBuilder().showStackTrace(t);
        }
        // Tell the user what happened.
        if (passedTest) {
            JOptionPane.showMessageDialog(
                    this,
                    BuilderBundle.getString("tblProvTestPassed"),
                    BuilderBundle.getString("testTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    BuilderBundle.getString("tblProvTestFailed"),
                    BuilderBundle.getString("testTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        // The following are applicable to all table provider views.
        // The following are not applicable to DataSets (we can tell by the listener type).
        if (!(this.getAdaptor() instanceof DataSetAdaptor)) {
            contextMenu.addSeparator();
            final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseTblProvTitle", this.tableProvider.getName()));
            sync.setMnemonic(BuilderBundle.getString("synchroniseTblProvMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    synchroniseTableProvider();
                }
            });
            contextMenu.add(sync);
            final JMenuItem test = new JMenuItem(BuilderBundle.getString("testTblProvTitle", this.tableProvider.getName()));
            test.setMnemonic(BuilderBundle.getString("testTblProvMnemonic").charAt(0));
            test.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    testTableProvider();
                }
            });
            contextMenu.add(test);
            final JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeTblProvTitle", this.tableProvider.getName()));
            remove.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    windowTabSet.getTableProviderTabSet().confirmRemoveTableProvider(tableProvider);
                }
            });
            contextMenu.add(remove);
        }
        // Return.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * Resyncs the table providers with the contents of the set.
     */
    public void synchronise() {
        // TODO: Construct/update our set of Component.Table and Component.Relation objects.
    }
}
