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

/**
 * This simple class wraps a thread, and displays an hourglass for as long as
 * that thread is running. It is synchronised so that if multiple threads are
 * running, only the first one will start the hourglass, and only the last one
 * to end will stop it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 7th June 2006
 * @since 0.1
 */
public abstract class LongProcess {

	private static Container container;
	
	private static int longProcessCount = 0;

	private static final Object lockObject = "My MartBuilder Hourglass Lock";
	
	/**
	 * Sets the container which the hourglass will appear over.
	 * @param newContainer the container over which the mouse will transform
	 * into an hourglass.
	 */
	public static void setContainer(Container newContainer) {
		container = newContainer;
	}

	public synchronized static void run(final Runnable process) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					// Update the number of processes currently running.
					synchronized (lockObject) {
						LongProcess.longProcessCount++;
					}

					// If this is the first process to start, open the
					// hourglass.
					if (LongProcess.longProcessCount == 1) {
						Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
						container.setCursor(hourglassCursor);
					}

					// Let the process run.
					process.run();
				} catch (Error e) {
					throw e;
				} finally {
					// Decrease the number of processes currently running.
					synchronized (lockObject) {
						LongProcess.longProcessCount--;
					}

					// If that was the last one, stop the hourglass.
					if (LongProcess.longProcessCount == 0) {
						Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
						container.setCursor(normalCursor);
					}
				}
			}
		});

		// Start the thread that does all this.
		t.start();
	}
}
