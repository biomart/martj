/* Generated by Together */

package org.ensembl.mart.explorer.gui;

import java.awt.*;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.ensembl.mart.explorer.Query;
import org.ensembl.mart.explorer.*;
import javax.swing.JScrollPane;

/** Panel in which user enters a query. */
public class QueryPanel extends JPanel {
    /** Creates new form QueryPanel */
    public QueryPanel( MartExplorerGUI martExplorerGUI) {
        this();
        this.martExplorerGUI = martExplorerGUI;
    }

    public QueryPanel() {
      initGUI();
      exportTab.setQueryPanel( this );
    }

    /** This method is called from within the constructor to initialize the form. */
    private void initGUI() {
        setPreferredSize(new Dimension(300, 200));
        setLayout(new java.awt.BorderLayout());
        add(queryTabs, java.awt.BorderLayout.CENTER);
        queryTabs.add(databaseTab, "Database");
        queryTabs.add(new JScrollPane(filterTab), "Filters");
        queryTabs.add(attributeTab, "Attributes");
        queryTabs.add(exportTab, "Export");
    }

		public void clear() {
			for (int i=0; i<inputPages.length; i++) {
				inputPages[i].clear();
      }
    }

    /** Loads a predefined query into the query panel so that the query parameters are displayed in the input pages. */
    public void updatePage( Query query) {
			for (int i=0; i<inputPages.length; i++) {
				inputPages[i].updatePage( query );
      }
    }

    /** Returns the query defined by the user. */
    public void updateQuery( Query query) throws InvalidQueryException {
			for (int i=0; i<inputPages.length; i++) {
				inputPages[i].updateQuery( query );
      }

    }

    public MartExplorerGUI getMartExplorerGUI(){
            return martExplorerGUI;
        }

    public void setMartExplorerGUI(MartExplorerGUI martExplorerGUI){
            this.martExplorerGUI = martExplorerGUI;
        }

    private JTabbedPane queryTabs = new JTabbedPane();
    private DatabaseConfigPage databaseTab = new DatabaseConfigPage();
    private ExportPanel exportTab = new ExportPanel();
    private FilterPanel filterTab = new FilterPanel ();
    private AttributePanel attributeTab = new AttributePanel ();
    private QueryInputPage[] inputPages = new QueryInputPage[] {
			databaseTab
      ,exportTab
      ,filterTab,attributeTab };
    private MartExplorerGUI martExplorerGUI;
}
