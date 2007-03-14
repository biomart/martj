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

package org.biomart.common.view.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.JLabel;

/**
 * This simple tries to find the default system browser then uses it to open a
 * URL.
 * <p>
 * Based on code from <a
 * href="http://www.centerkey.com/java/browser/">BareBonesBrowser</a>.
 * <p>
 * Hyperlink button code based on <a
 * href="http://www.demo2s.com/Code/Java/Swing-Components/LinkButton.htm"> this
 * tutorial</a>.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class OpenBrowser {
	public static void openURL(String url) {
		String osName = System.getProperty("os.name").toLowerCase();
		try {
			if (osName.startsWith("mac")) {
				Class fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL",
						new Class[] { String.class });
				openURL.invoke(null, new Object[] { url });
			} else if (osName.startsWith("windows"))
				Runtime.getRuntime().exec(
						"rundll32 url.dll,FileProtocolHandler " + url);
			else {
				// assume Unix or Linux
				String[] browsers = { "firefox", "opera", "konqueror",
						"mozilla", "netscape" };
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++)
					if (Runtime.getRuntime().exec(
							new String[] { "which", browsers[count] })
							.waitFor() == 0)
						browser = browsers[count];
				if (browser != null)
					Runtime.getRuntime().exec(new String[] { browser, url });
			}
		} catch (Exception e) {
			// We don't really care if it fails. Tough luck.
		}
	}

	public static class OpenBrowserLabel extends JLabel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OpenBrowserLabel(final String url) {
			this(url, url);
		}

		public OpenBrowserLabel(final String text, final String url) {
			super(text);
			this.setToolTipText(url);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			this.setForeground(Color.BLUE);
			this.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					OpenBrowser.openURL(url);
				}
			});
		}
	}

}
