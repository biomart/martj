/*
 * Created on Jan 22, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.ensembl.mart.explorer;

import java.awt.Component;

import javax.swing.JOptionPane;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Feedback {

	private Component src;

	public Feedback(Component src) {
		this.src = src;
	}

	
	/**
	 * Displays message in warning dialog box.
	 * @param message
	 */
	public void warn(String message) {
			JOptionPane.showMessageDialog(
				src,
				message,
				"Warning",
				JOptionPane.WARNING_MESSAGE);
		}

	/**
	 * Displays message plus exception.messge in warning dialog box 
	 * and prints stack trace to console.
	 * @param e exception
	 */
	public void warn(String message, Exception e) {
			warn(message + ":" + e.getMessage());
			e.printStackTrace();
		}

		/**
		 * Displays warning dialog and prints stack trace to console.
		 * @param e exception
		 */
		public void warn(Exception e) {
			warn(e.getMessage());
			e.printStackTrace();
		}
}