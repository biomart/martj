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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;

import javax.swing.SwingUtilities;

import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * This simple class wraps a thread, and displays an hourglass for as long as
 * that thread is running. It is synchronised so that if multiple threads are
 * running, only the first one will start the hourglass, and only the last one
 * to end will stop it.
 * <p>
 * The hourglass portion of the thread is run using
 * {@link SwingUtilities#invokeLater(java.lang.Runnable)}, so that it is
 * thread-safe for Swing and can safely work with the GUI. Any parts of the
 * process passed in that work with the GUI must also use this construct to
 * avoid concurrent modification problems.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public abstract class LongProcess {

	private static final Object lockObject = "My Hourglass Lock";

	private static int longProcessCount = 0;

	/**
	 * Runs the given task in the background, in a Swing-thread-safe
	 * environment. Whilst the task is running, the hourglass is shown over the
	 * currently active window, as specified by
	 * {@link DefaultKeyboardFocusManager#getActiveWindow()}.
	 * 
	 * @param process
	 *            the process to run.
	 */
	public synchronized static void run(final Runnable process) {
		// Which window needs it?
		final Component window = KeyboardFocusManager
				.getCurrentKeyboardFocusManager().getFocusedWindow();

		// Update the number of processes currently running.
		synchronized (LongProcess.lockObject) {
			// If this is the first process to start, open the
			// hourglass.
			if (++LongProcess.longProcessCount == 1)
				try {
					final Cursor normalCursor = new Cursor(Cursor.WAIT_CURSOR);
					window.setCursor(normalCursor);
				} catch (final Throwable t) {
					LongProcess.longProcessCount = 0;
					StackTrace.showStackTrace(t);
					return;
				}
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					// Let the process run.
					SwingUtilities.invokeAndWait(process);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				} finally {
					// Decrease the number of processes currently running.
					synchronized (LongProcess.lockObject) {
						// If that was the last one, stop the hourglass.
						if (--LongProcess.longProcessCount == 0)
							try {
								SwingUtilities.invokeAndWait(new Runnable() {
									public void run() {
										final Cursor normalCursor = new Cursor(
												Cursor.DEFAULT_CURSOR);
										window.setCursor(normalCursor);
									}
								});
							} catch (final Throwable t) {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										StackTrace.showStackTrace(t);
									}
								});
							}
					}
				}
			}
		}).start();
	}
}
