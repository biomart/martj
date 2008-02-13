package org.biomart.configurator.view;

import java.awt.Dimension;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SplitPanel {
	
	private JButton    m_tempButton_1 = new JButton("LHS");
    private JButton    m_tempButton_2 = new JButton("RHS");
	
	private JSplitPane splitPane =  new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_tempButton_1, m_tempButton_2);
	
	public SplitPanel() {
		// TODO Auto-generated constructor stub
		splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);
        //Provide a preferred size for the split pane.
        splitPane.setPreferredSize(new Dimension(400, 200));
	}

	public JSplitPane getSplitPanel() {
		return this.splitPane;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
