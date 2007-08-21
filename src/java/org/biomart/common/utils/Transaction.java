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

package org.biomart.common.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Schema;
import org.biomart.common.exceptions.TransactionException;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * This static class provides methods which signal the beginning and end of a
 * transaction. It has no data - it is merely a pair of events and an associated
 * event handler queue.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.7
 */
public class Transaction {

	private final static String LOCK = "__TRANSACTION__LOCK__";

	/**
	 * Listener are notified when a transaction starts and ends.
	 */
	public static interface TransactionListener {
		/**
		 * Reset the modified flags on this object, possibly before a new
		 * tranaction starts, or after a transaction which caused changes which
		 * are not considered important.
		 */
		public void transactionReset();

		/**
		 * A transaction has begun.
		 * 
		 * @param evt
		 *            the event describing the transaction.
		 */
		public void transactionStarted(final TransactionEvent evt);

		/**
		 * A transaction has ended.
		 * 
		 * @param evt
		 *            the event describing the transaction. This will be the
		 *            exact same object passed to
		 *            {@link #transactionStarted(org.biomart.common.utils.Transaction.TransactionEvent)}
		 *            if it refers to the same transaction.
		 * @throws TransactionException
		 *             if anything went wrong.
		 */
		public void transactionEnded(final TransactionEvent evt)
				throws TransactionException;

		/**
		 * Indicate that some aspect of this object has been directly affected
		 * by the current transaction.
		 * 
		 * @param modified
		 *            whether or not it has been affected.
		 */
		public void setDirectModified(final boolean modified);

		/**
		 * Indicate that some aspect of this object has been indirectly affected
		 * by the current transaction.
		 * 
		 * @param modified
		 *            whether or not it has been affected.
		 */
		public void setIndirectModified(final boolean modified);

		/**
		 * Has this object been directly affected by this transaction?
		 * 
		 * @return <tt>true</tt> if it has.
		 */
		public boolean isDirectModified();

		/**
		 * Has this object been indirectly affected by this transaction?
		 * 
		 * @return <tt>true</tt> if it has.
		 */
		public boolean isIndirectModified();
	}

	private static class WeakTransactionListener implements TransactionListener {
		private WeakReference listenerRef;

		private WeakTransactionListener(final TransactionListener listener) {
			this.listenerRef = new WeakReference(listener);
		}

		public void transactionReset() {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null)
				this.removeListener();
			else
				listener.transactionReset();
		}

		public void transactionStarted(final TransactionEvent evt) {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null)
				this.removeListener();
			else
				listener.transactionStarted(evt);
		}

		public void transactionEnded(final TransactionEvent evt)
				throws TransactionException {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null)
				this.removeListener();
			else
				listener.transactionEnded(evt);
		}

		public void setDirectModified(final boolean modified) {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null)
				this.removeListener();
			else
				listener.setDirectModified(modified);
		}

		public void setIndirectModified(final boolean modified) {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null)
				this.removeListener();
			else
				listener.setIndirectModified(modified);
		}

		public boolean isDirectModified() {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null) {
				this.removeListener();
				return false;
			} else
				return listener.isIndirectModified();
		}

		public boolean isIndirectModified() {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null) {
				this.removeListener();
				return false;
			} else
				return listener.isIndirectModified();
		}

		private void removeListener() {
			try {
				Transaction.removeTransactionListener(this);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private final static Set listeners = new HashSet();

	/**
	 * Adds a listener to the queue. Listeners are not stored in any particular
	 * order, so you cannot guarantee that one listener will be called before
	 * the next.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public static void addTransactionListener(final TransactionListener listener) {
		Transaction.listeners.add(new WeakTransactionListener(listener));
	}

	private static void removeTransactionListener(
			final TransactionListener listener) {
		Transaction.listeners.remove(listener);
	}

	private static int inProgress = 0;

	private static Transaction currentTransaction;

	/**
	 * Reset all transaction listeners ready for a new transaction.
	 */
	public static void reset() {
		synchronized (Transaction.LOCK) {
			if (Transaction.inProgress == 0)
				for (final Iterator i = Transaction.getOrderedListeners()
						.iterator(); i.hasNext();)
					((TransactionListener) i.next()).transactionReset();
		}
	}

	/**
	 * Flag that a transaction has started. If another transaction is already
	 * underway, this call is simply ignored and the existing transaction
	 * continues. If no other transactions are underway, a new one is created
	 * and
	 * {@link TransactionListener#transactionStarted(org.biomart.common.utils.Transaction.TransactionEvent)}
	 * is called.
	 */
	public static void start() {
		synchronized (Transaction.LOCK) {
			if (++Transaction.inProgress == 1) {
				Transaction.currentTransaction = new Transaction();
				final TransactionEvent event = new TransactionEvent(
						Transaction.currentTransaction);
				for (final Iterator i = Transaction.getOrderedListeners()
						.iterator(); i.hasNext();) {
					final TransactionListener listener = (TransactionListener) i
							.next();
					listener.transactionReset();
					listener.transactionStarted(event);
				}
			}
		}
	}

	/**
	 * Flag that a transaction has ended. If other transactions were started in
	 * the meantime, the last one to end will trigger a
	 * {@link TransactionListener#transactionEnded(org.biomart.common.utils.Transaction.TransactionEvent)}
	 * event. Spawns a thread to do this and returns instantly.
	 */
	public synchronized static void end() {
		synchronized (Transaction.LOCK) {
			if (Transaction.inProgress == 0)
				return;
			if (--Transaction.inProgress == 0) {
				final TransactionEvent event = new TransactionEvent(
						Transaction.currentTransaction);
				new LongProcess() {
					public void run() throws Exception {
						for (final Iterator i = Transaction
								.getOrderedListeners().iterator(); i.hasNext();)
							((TransactionListener) i.next())
									.transactionEnded(event);
						Transaction.currentTransaction = null;
					}
				}.start();
			}
		}
	}

	/**
	 * Flag that a transaction has ended. If other transactions were started in
	 * the meantime, the last one to end will trigger a
	 * {@link TransactionListener#transactionEnded(org.biomart.common.utils.Transaction.TransactionEvent)}
	 * event. Waits for all to complete before returning.
	 */
	public synchronized static void endAndWait() {
		synchronized (Transaction.LOCK) {
			if (Transaction.inProgress == 0)
				return;
			if (--Transaction.inProgress == 0
					&& Transaction.currentTransaction != null) {
				try {
					final TransactionEvent event = new TransactionEvent(
							Transaction.currentTransaction);
					for (final Iterator i = Transaction.getOrderedListeners()
							.iterator(); i.hasNext();)
						((TransactionListener) i.next())
								.transactionEnded(event);
				} catch (final TransactionException te) {
					StackTrace.showStackTrace(te);
				}
				Transaction.currentTransaction = null;
			}
		}
	}

	private static List getOrderedListeners() {
		// Schemas get notified first in order to ensure that
		// data is up-to-date.
		// Then, partition tables, so that datasets relying on them
		// are up-to-date.
		// Then, datasets.
		// Then, anything else that is interested.
		final List list = new ArrayList();
		final List schs = new ArrayList();
		final List pts = new ArrayList();
		final List ds = new ArrayList();
		final List normal = new ArrayList();
		for (final Iterator i = Transaction.listeners.iterator(); i.hasNext();) {
			final TransactionListener tl = (TransactionListener) i.next();
			if (tl instanceof DataSet) {
				if (((DataSet) tl).isPartitionTable())
					pts.add(tl);
				else
					ds.add(tl);
			} else if (tl instanceof Schema)
				schs.add(tl);
			else
				normal.add(tl);
		}
		list.addAll(schs);
		list.addAll(pts);
		list.addAll(ds);
		list.addAll(normal);
		return list;
	}

	/**
	 * Checks to see if there is currently a transaction in progress.
	 * 
	 * @return the current transaction, or <tt>null</tt> if there isn't one.
	 */
	public static Transaction getCurrentTransaction() {
		return Transaction.currentTransaction;
	}

	/**
	 * Describes a transaction start/end event. Currently this class has no
	 * useful properties and the event objects that use it can safely be
	 * ignored.
	 */
	public static class TransactionEvent extends EventObject {
		private static final long serialVersionUID = 1L;

		/**
		 * Create a new event fired from the specified source.
		 * 
		 * @param source
		 *            the source that fired the event. This will be a
		 *            Transaction object.
		 */
		public TransactionEvent(final Object source) {
			super(source);
		}
	}

	/**
	 * Create a new, empty transaction.
	 */
	public Transaction() {

	}
}
