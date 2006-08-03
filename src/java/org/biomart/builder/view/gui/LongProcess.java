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

package org.biomart.builder.view.gui;

import java.awt.Container;
import java.awt.Cursor;

import javax.swing.SwingUtilities;

import org.biomart.builder.exceptions.MartBuilderInternalError;

/**
 * <p>
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
 * @version 0.1.5, 20th June 2006
 * @since 0.1
 */
public abstract class LongProcess {

	private static Container container;

	private static int longProcessCount = 0;

	private static final Object lockObject = "My MartBuilder Hourglass Lock";

	/**
	 * Sets the container which the hourglass will appear over.
	 * 
	 * @param newContainer
	 *            the container over which the mouse will transform into an
	 *            hourglass.
	 */
	public static void setContainer(final Container newContainer) {
		LongProcess.container = newContainer;
	}

	/**
	 * Runs the given task in the background, in a Swing-thread-safe
	 * environment. Whilst the task is running, the hourglass is shown over the
	 * container set using {@link LongProcess#setContainer(Container)}.
	 * 
	 * @param process
	 *            the process to run.
	 */
	public synchronized static void run(final Runnable process) {
		new Thread(new Runnable() {
			public void run() {
				try {
					// Update the number of processes currently running.
					synchronized (LongProcess.lockObject) {
						LongProcess.longProcessCount++;
					}

					// If this is the first process to start, open the
					// hourglass.
					if (LongProcess.longProcessCount == 1)
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								final Cursor normalCursor = new Cursor(
										Cursor.WAIT_CURSOR);
								LongProcess.container.setCursor(normalCursor);
							}
						});

					// Let the process run.
					try {
						process.run();
					} catch (final Exception e) {
						throw new MartBuilderInternalError(e);
					}
				} catch (final Error e) {
					throw e;
				} finally {
					// Decrease the number of processes currently running.
					synchronized (LongProcess.lockObject) {
						LongProcess.longProcessCount--;
					}

					// If that was the last one, stop the hourglass.
					if (LongProcess.longProcessCount == 0)
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								final Cursor normalCursor = new Cursor(
										Cursor.DEFAULT_CURSOR);
								LongProcess.container.setCursor(normalCursor);
							}
						});
				}
			}
		}).start();
	}
}
