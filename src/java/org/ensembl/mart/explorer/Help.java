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
import java.awt.Frame;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
/**
 * Displays the MartExplorer help file
 * <code>file data/martexplorer_help.html</code>.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Help extends Box {
	private JEditorPane pane = new JEditorPane();
	public Help() {
		super(BoxLayout.Y_AXIS);
		try {
			URL url = getClass().getClassLoader().getResource(
					"data/martexplorer_help.html");
			pane.setPage(url);
		} catch (MalformedURLException e) {
			pane.setText(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			pane.setText(e.getMessage());
			e.printStackTrace();
		}
		add(pane);
		pane.setEditable(false);
	}
	public void showDialog(Frame parent) {
		JDialog d = new JDialog(parent, "MartExplorer Documentation", true);
		d.getContentPane().add(new JScrollPane(this));
		d.setSize(400, 500);
		d.setVisible(true);
	}
	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		f.setSize(200, 200);
		new Help().showDialog(f);
	}
}
