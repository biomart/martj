package org.ensembl.mart.vieweditor;

import org.ensembl.mart.lib.config.DatasetView;

import javax.swing.*;
import java.awt.dnd.Autoscroll;
import java.awt.*;


public class DatasetViewAttributesTable extends JTable implements Autoscroll{

     public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
     protected Insets scrollInsets = defaultScrollInsets;
     protected DatasetViewTreeModel treemodel = null;
     protected JInternalFrame frame;
     protected DatasetView dsView = null;

     public DatasetViewAttributesTable(DatasetView dsView, JInternalFrame frame) {
        super(new DatasetViewAttributeTableModel());
        //super(null);
        this.dsView = dsView;
        this.frame = frame;

     }

     // Autoscrolling support
    public void setScrollInsets(Insets insets) {
        this.scrollInsets = insets;
    }

    public Insets getScrollInsets() {
        return scrollInsets;
    }

    // Implementation of Autoscroll interface
    public Insets getAutoscrollInsets() {
        Rectangle r = getVisibleRect();
        Dimension size = getSize();
        Insets i = new Insets(r.y + scrollInsets.top, r.x + scrollInsets.left,
                size.height - r.y - r.height + scrollInsets.bottom,
                size.width - r.x - r.width + scrollInsets.right);
        return i;
    }

    public void autoscroll(Point location) {
        JScrollPane scroller =
                (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        if (scroller != null) {
            JScrollBar hBar = scroller.getHorizontalScrollBar();
            JScrollBar vBar = scroller.getVerticalScrollBar();
            Rectangle r = getVisibleRect();
            if (location.x <= r.x + scrollInsets.left) {
                // Need to scroll left
                hBar.setValue(hBar.getValue() - hBar.getUnitIncrement(-1));
            }
            if (location.y <= r.y + scrollInsets.top) {
                // Need to scroll up
                vBar.setValue(vBar.getValue() - vBar.getUnitIncrement(-1));
            }
            if (location.x >= r.x + r.width - scrollInsets.right) {
                // Need to scroll right
                hBar.setValue(hBar.getValue() + hBar.getUnitIncrement(1));
            }
            if (location.y >= r.y + r.height - scrollInsets.bottom) {
                // Need to scroll down
                vBar.setValue(vBar.getValue() + vBar.getUnitIncrement(1));
            }
        }

    }
}
