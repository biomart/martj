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

package org.ensembl.mart.explorer;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;

/**
 * Lays out components in a grid.
 */
public class GridPanel extends Box {

  private JComponent[] components;
  private int nColumns;
  private int rowHeight;
  private int lastWidth;

  public GridPanel(
    JComponent[] components,
    int nColumns,
    int rowHeight,
    String title) {
    super(BoxLayout.Y_AXIS);

    this.components = components;
    this.nColumns = nColumns;
    this.rowHeight = rowHeight;

    setBorder(new TitledBorder(title));
    
    // We manually control the size of the components
    // in this container.
    addComponentListener(new ComponentListener() {

      public void componentResized(ComponentEvent e) {
        resizeComponents( getParent().getSize().width );
      }

      public void componentMoved(ComponentEvent e) {
      }
      public void componentShown(ComponentEvent e) {
      }
      public void componentHidden(ComponentEvent e) {
      }

    });

    addComponents(components);
  }

  private void addComponents(JComponent[] components) {

    Box row = null;

    for (int i = 0; i < components.length; i++) {

      if (row == null)
        row = Box.createHorizontalBox();
      add(row);

      JComponent c = components[i];
      row.add(c);

      if ((i + 1) % nColumns == 0)
        row = null;
    }
    
    if ( components.length % nColumns != 0 ) row.add(Box.createHorizontalGlue()); 
  }

  /**
   * Resizes leafWidgets to fill width of parent
   * container.
   */
  private void resizeComponents( int width ) {

    if (width != lastWidth) {

      int noScrollWidth = width / nColumns - 10;
      int height = rowHeight;
      Dimension size = new Dimension(noScrollWidth, height);

      for (int i = 0, n = components.length; i < n; i++) {

        components[i].setPreferredSize(size);
        components[i].setMinimumSize(size);
        components[i].setMaximumSize(size);
        components[i].invalidate();
      }

      lastWidth = width;
    }
  }

}
