/* Generated by Together */

package org.ensembl.mart.explorer.gui;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import org.ensembl.mart.explorer.*;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.ButtonGroup;


/** Input panel where user selects export format, compression and file name. */
public class ExportPanel extends JPanel implements org.ensembl.mart.explorer.gui.QueryInputPage {

    /** Creates new form ExportPanel */
    public ExportPanel(QueryPanel queryPanel) {
        this();
        this.queryPanel = queryPanel;
    }

    public ExportPanel() {
      initGUI();
    }

    /** This method is called from within the constructor to initialize the form. */
    private void initGUI() {
        exportFormatPanel.setBorder(null);
        exportFormatPanel.add(formatLabel);
        exportFormatPanel.add(tsvFormatButton);
        tsvFormatButton.setText("jRadioButton1");
        tsvFormatButton.setText("Text, tab separated");
        tsvFormatButton.setSelected(true);
        formatLabel.setText("Format");
        exportToFileButton.setText("Export to File");
        noCompressionButton.setText("jRadioButton1");
        noCompressionButton.setText("None");
        noCompressionButton.setSelected(true);
        compressionPanel.setBorder(null);
        compressionPanel.add(compressionLabel);
        compressionPanel.add(noCompressionButton);
        fileNameLabel.setText("File name");
        fileNameLabel.setToolTipText("Name of file results written to");
        fileNamePanel.setBorder(null);
        fileNamePanel.add(fileNameLabel);
        fileNamePanel.add(fileName);
        fileName.setEditable(true);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setPreferredSize(new java.awt.Dimension(600, 114));
        add(exportFormatPanel);
        add(windowExportPanel);
        add(fileExportPanel);
        exportToWindowButton.setText("jRadioButton1");
        exportToWindowButton.setText("Export to window");
        windowExportPanel.setLayout(new javax.swing.BoxLayout(windowExportPanel, javax.swing.BoxLayout.X_AXIS));
        windowExportPanel.add(exportToWindowButton);
        windowExportPanel.add(windowName);
        windowName.setEditable(true);
        fileExportPanel.setLayout(new javax.swing.BoxLayout(fileExportPanel, javax.swing.BoxLayout.X_AXIS));
        fileExportPanel.add(jPanel1);
        fileExportPanel.add(fileDetailsPanel);
        fileDetailsPanel.setLayout(new javax.swing.BoxLayout(fileDetailsPanel, javax.swing.BoxLayout.Y_AXIS));
        fileDetailsPanel.add(fileNamePanel);
        fileDetailsPanel.add(compressionPanel);
        exportToWindowButton.setSelected(true);
        exportButtonGroup.add(exportToWindowButton);
        exportButtonGroup.add(exportToFileButton);
        compressionLabel.setText("Compression");
        jPanel1.setAlignmentX(0.1f);
        jPanel1.setAlignmentY(0.1f);
        jPanel1.add(exportToFileButton);
    }

    public void updateQuery(Query query)  throws InvalidQueryException  {
        Formatter r = null;
        if (tsvFormatButton.isSelected()) r = new SeparatedValueFormatter ("\t");
        if (exportToWindowButton.isSelected()) {
          // Get the ResultWindow from the main GUI. This is so the main GUI can manage the windows.
          String name = Tool.selected(windowName);
          if ( name==null ) name = "";
          ResultWindow rw
            = queryPanel.getMartExplorerGUI().createResultWindow( name, r);
            query.setResultTarget( rw );
        }
        else if (exportToFileButton.isSelected()) {
            query.setResultTarget(new ResultFile(Tool.selected(fileName), r));
        }
    }

    public void updatePage(Query query) {
        ResultTarget rt = query.getResultTarget();
        if (rt != null) {
            // set GUI to reflect selected result file
            if (rt instanceof ResultFile) {
                Tool.prepend(rt.getName(), fileName);
                exportToFileButton.setSelected(true);
            }
            // set GUI to reflect selected result window
            else if (rt instanceof ResultWindow) {
                Tool.prepend(rt.getName(), windowName);
                exportToWindowButton.setSelected(true);
            }
            // Set GUI to reflect which formatters included
            Formatter r = rt.getFormatter();
            if (r != null) {
                if (r instanceof SeparatedValueFormatter) tsvFormatButton.setSelected(true);
            }
        }
    }

    public QueryPanel getQueryPanel(){
            return queryPanel;
        }

    public void setQueryPanel(QueryPanel queryPanel){
            this.queryPanel = queryPanel;
        }

    /**
     * Removes all selected values. 
     */
    public void clear(){
			tsvFormatButton.setSelected( true );
      noCompressionButton.setSelected( true );
      exportToWindowButton.setSelected( true );
			Tool.clear( windowName );
      Tool.clear( fileName );
    }

    private JPanel fileNamePanel = new JPanel();
    private JLabel fileNameLabel = new JLabel();
    private JComboBox fileName = new JComboBox();
    private JPanel compressionPanel = new JPanel();
    private JRadioButton noCompressionButton = new JRadioButton();
    private JPanel exportFormatPanel = new JPanel();
    private JRadioButton tsvFormatButton = new JRadioButton();
    private JPanel windowExportPanel = new JPanel();
    private JRadioButton exportToWindowButton = new JRadioButton();
    private JComboBox windowName = new JComboBox();
    private JPanel fileExportPanel = new JPanel();
    private JRadioButton exportToFileButton = new JRadioButton();
    private JPanel fileDetailsPanel = new JPanel();
    private ButtonGroup exportButtonGroup = new ButtonGroup();
    private JLabel compressionLabel = new JLabel();
    private JLabel formatLabel = new JLabel();
    private JPanel jPanel1 = new JPanel();
    private QueryPanel queryPanel;
}
