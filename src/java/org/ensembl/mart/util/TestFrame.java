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

package org.ensembl.mart.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * Simple facade for creating and showing a titled swing frame containing 
 * a user specified component and with proper closing behaviour.
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
 */
public class TestFrame extends JFrame {

  /**
   * Create frame with specified title and add userComponent then set visible and pack.
   * @param title frame title
   * @param userComponent component to add to frame.
   * @throws java.awt.HeadlessException
   */
  public TestFrame(String title, Component userComponent)
    throws HeadlessException {
    super(title);

    JButton closeButton = new JButton("Close " + title + " frame.");
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        System.exit(0);
      }
    });

    JPanel userComponentPanel = new JPanel();
    userComponentPanel.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Component on test"));
    //userComponentPanel.setBorder(new LineBorder(Color.BLACK));
    userComponentPanel.add(userComponent);
    
    Box b = Box.createHorizontalBox();
    b.add(closeButton);

    Box p = Box.createVerticalBox();
    p.add(userComponentPanel);
    p.add(Box.createVerticalStrut(20));
    p.add(b);
    p.setBorder(new EmptyBorder(20,20,20,20));
    
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().add(p);
    setVisible(true);
    pack();

    // TODO Auto-generated constructor stub
  }

}
