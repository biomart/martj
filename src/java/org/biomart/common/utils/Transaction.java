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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.builder.view.gui.diagrams.components.DiagramComponent;
import org.biomart.common.exceptions.TransactionException;
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

	// In seconds.
	private final static int LISTENER_FLUSH_INTERVAL = 5;

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
		public void transactionResetDirectModified();

		/**
		 * Reset the modified flags on this object, possibly before a new
		 * tranaction starts, or after a transaction which caused changes which
		 * are not considered important.
		 */
		public void transactionResetVisibleModified();

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
		 * by the current transaction and it needs visibly highlighting.
		 * 
		 * @param modified
		 *            whether or not it has been affected.
		 */
		public void setVisibleModified(final boolean modified);

		/**
		 * Has this object been directly affected by this transaction and needs
		 * visibly highlighting?
		 * 
		 * @return <tt>true</tt> if it has.
		 */
		public boolean isVisibleModified();

		/**
		 * Indicate that some aspect of this object has been directly affected
		 * by the current transaction.
		 * 
		 * @param modified
		 *            whether or not it has been affected.
		 */
		public void setDirectModified(final boolean modified);

		/**
		 * Has this object been directly affected by this transaction?
		 * 
		 * @return <tt>true</tt> if it has.
		 */
		public boolean isDirectModified();
	}

	/**
	 * A weak property change listener that dies when the wrapped listener goes
	 * out of scope. This is very important to use when the object being
	 * listened to could outlive the listening object (e.g. a GUI component will
	 * 99% of the time outlive the object it represents, so the GUI component
	 * should listen via weak listener reference).
	 */
	public static class WeakPropertyChangeListener implements
			PropertyChangeListener {
		private WeakReference listenerRef;

		private Object parent;

		private String property;

		/**
		 * Create a new weak property change listener.
		 * 
		 * @param parent
		 *            the parent property change support object.
		 * @param listener
		 *            the listener to wrap.
		 */
		public WeakPropertyChangeListener(final Object parent,
				final PropertyChangeListener listener) {
			this(parent, null, listener);
		}

		/**
		 * Create a new weak property change listener.
		 * 
		 * @param parent
		 *            the parent property change support object.
		 * @param property
		 *            the property name to listen to - null for all.
		 * @param listener
		 *            the listener to wrap.
		 */
		public WeakPropertyChangeListener(final Object parent,
				final String property, final PropertyChangeListener listener) {
			this.listenerRef = new WeakReference(listener);
			this.parent = parent;
			this.property = property;
		}

		/**
		 * Obtain the wrapped listener.
		 * 
		 * @return the listener, or null if it has gone away.
		 */
		public PropertyChangeListener getListener() {
			return (PropertyChangeListener) this.listenerRef.get();
		}

		public boolean equals(final Object obj) {
			if (obj instanceof WeakPropertyChangeListener) {
				final WeakPropertyChangeListener them = (WeakPropertyChangeListener) obj;
				return them.property == this.property
						&& them.parent == this.parent
						&& them.listenerRef.get() == this.listenerRef.get();
			} else
				return false;
		}

		public void propertyChange(final PropertyChangeEvent evt) {
			final PropertyChangeListener listener = this.getListener();
			if (listener == null) {
				if (this.parent != null)
					try {
						if (this.property == null) {
							final Method method = this.parent
									.getClass()
									.getMethod(
											"removePropertyChangeListener",
											new Class[] { PropertyChangeListener.class });
							method.invoke(this.parent, new Object[] { this });
						} else {
							final Method method = this.parent
									.getClass()
									.getMethod(
											"removePropertyChangeListener",
											new Class[] {
													String.class,
													PropertyChangeListener.class });
							method.invoke(this.parent, new Object[] {
									this.property, this });
						}
					} catch (final Exception e) {
						// Don't care.
					} finally {
						// Don't do it again.
						this.parent = null;
					}
			} else
				listener.propertyChange(evt);
		}
	}

	private static class WeakTransactionListener implements TransactionListener {
		private WeakReference listenerRef;

		private static int nextHashCode = 0;

		private int hashcode = WeakTransactionListener.nextHashCode++;

		private WeakTransactionListener(final TransactionListener listener) {
			this.listenerRef = new WeakReference(listener);
		}

		public int hashCode() {
			return this.hashcode;
		}

		public boolean equals(final Object obj) {
			return obj instanceof WeakTransactionListener
					&& ((WeakTransactionListener) obj).hashcode == this.hashcode;
		}

		/**
		 * Obtain the wrapped listener.
		 * 
		 * @return the listener, or null if it has gone away.
		 */
		public TransactionListener get() {
			final TransactionListener listener = (TransactionListener) this.listenerRef
					.get();
			if (listener == null)
				this.removeListener();
			return listener;
		}

		public void transactionResetDirectModified() {
			final TransactionListener listener = this.get();
			if (listener != null)
				listener.transactionResetDirectModified();
		}

		public void transactionResetVisibleModified() {
			final TransactionListener listener = this.get();
			if (listener != null)
				listener.transactionResetVisibleModified();
		}

		public void transactionStarted(final TransactionEvent evt) {
			final TransactionListener listener = this.get();
			if (listener != null)
				listener.transactionStarted(evt);
		}

		public void transactionEnded(final TransactionEvent evt)
				throws TransactionException {
			final TransactionListener listener = this.get();
			if (listener != null)
				listener.transactionEnded(evt);
		}

		public void setVisibleModified(final boolean modified) {
			final TransactionListener listener = this.get();
			if (listener != null)
				listener.setVisibleModified(modified);
		}

		public boolean isVisibleModified() {
			final TransactionListener listener = this.get();
			if (listener != null)
				return listener.isVisibleModified();
			return false;
		}

		public void setDirectModified(final boolean modified) {
			final TransactionListener listener = this.get();
			if (listener != null)
				listener.setDirectModified(modified);
		}

		public boolean isDirectModified() {
			final TransactionListener listener = this.get();
			if (listener != null)
				return listener.isDirectModified();
			return false;
		}

		private void removeListener() {
			try {
				Transaction.listeners.remove(this);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private final static Set listeners = Collections
			.synchronizedSet(new HashSet());

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

	private static int inProgress = 0;

	private static Transaction currentTransaction;

	/**
	 * Reset all transaction listeners ready for a new transaction.
	 */
	public static void resetVisibleModified() {
		for (final Iterator i = Transaction.getOrderedListeners().iterator(); i
				.hasNext();)
			((TransactionListener) i.next()).transactionResetVisibleModified();
	}

	/**
	 * Reset all transaction listeners ready for a new transaction.
	 */
	public static void resetDirectModified() {
		for (final Iterator i = Transaction.getOrderedListeners().iterator(); i
				.hasNext();)
			((TransactionListener) i.next()).transactionResetDirectModified();
	}

	/**
	 * Flag that a transaction has started. If another transaction is already
	 * underway, this call is simply ignored and the existing transaction
	 * continues. If no other transactions are underway, a new one is created
	 * and
	 * {@link TransactionListener#transactionStarted(org.biomart.common.utils.Transaction.TransactionEvent)}
	 * is called.
	 * 
	 * @param allowVisModChange
	 *            does this transaction change visible modifiers?
	 */
	public static void start(final boolean allowVisModChange) {
		synchronized (Transaction.LOCK) {
			if (++Transaction.inProgress == 1) {
				Transaction.currentTransaction = new Transaction(
						allowVisModChange);
				final TransactionEvent event = new TransactionEvent(
						Transaction.currentTransaction);
				for (final Iterator i = Transaction.getOrderedListeners()
						.iterator(); i.hasNext();) {
					final TransactionListener listener = (TransactionListener) i
							.next();
					listener.transactionResetDirectModified();
					listener.transactionStarted(event);
				}
			}
		}
	}

	/**
	 * Flag that a transaction has ended. If other transactions were started in
	 * the meantime, the last one to end will trigger a
	 * {@link TransactionListener#transactionEnded(org.biomart.common.utils.Transaction.TransactionEvent)}
	 * event.
	 */
	public synchronized static void end() {
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
		synchronized (Transaction.LOCK) {
			final List sch = new ArrayList();
			final List schComp = new ArrayList();
			final List schRel = new ArrayList();
			final List dsComp = new ArrayList();
			final List dsRel = new ArrayList();
			final List pts = new ArrayList();
			final List ds = new ArrayList();
			final List diag = new ArrayList();
			final List diagComp = new ArrayList();
			final List rest = new ArrayList();
			for (final Iterator i = new ArrayList(Transaction.listeners)
					.iterator(); i.hasNext();) {
				final TransactionListener tl = ((WeakTransactionListener) i
						.next()).get();
				if (tl == null)
					continue;
				else if (tl instanceof DataSet) {
					if (((DataSet) tl).isPartitionTable())
						pts.add(tl);
					else
						ds.add(tl);
				} else if (tl instanceof Schema)
					sch.add(tl);
				else if (tl instanceof DiagramComponent)
					diagComp.add(tl);
				else if (tl instanceof Diagram)
					diag.add(tl);
				else if (tl instanceof Relation) {
					if (((Relation) tl).getFirstKey().getTable().getSchema() instanceof DataSet)
						dsRel.add(tl);
					else
						schRel.add(tl);
				} else if (tl instanceof Key) {
					if (((Key) tl).getTable().getSchema() instanceof DataSet)
						dsComp.add(tl);
					else
						schComp.add(tl);
				} else if (tl instanceof Column) {
					if (tl instanceof DataSetColumn)
						dsComp.add(tl);
					else
						schComp.add(tl);
				} else if (tl instanceof Table)
					if (tl instanceof DataSetTable)
						dsComp.add(tl);
					else
						schComp.add(tl);
				else
					rest.add(tl);
			}
			final List list = new ArrayList();
			// schemas.
			list.addAll(sch);
			// non-relation schema components.
			list.addAll(schComp);
			// relations.
			list.addAll(schRel);
			// partition tables.
			list.addAll(pts);
			// dataset.
			list.addAll(ds);
			// non-relation dataset components.
			list.addAll(dsComp);
			// dataset relations.
			list.addAll(dsRel);
			// diagram components.
			list.addAll(diagComp);
			// diagrams.
			list.addAll(diag);
			// anything else that is interested.
			list.addAll(rest);
			return list;
		}
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

	private final boolean allowVisModChange;

	/**
	 * Create a new, empty transaction.
	 * 
	 * @param allowVisModChange
	 *            Does this transaction modify visible modification flags?
	 */
	public Transaction(final boolean allowVisModChange) {
		this.allowVisModChange = allowVisModChange;
	}

	/**
	 * Does this transaction modify visible modification flags?
	 * 
	 * @return <tt>true</tt> if it does.
	 */
	public boolean isAllowVisModChange() {
		return this.allowVisModChange;
	}

	// Timer thread to remove dead weak references.
	private static final Timer t = new Timer();
	static {
		Transaction.t.schedule(new TimerTask() {
			public void run() {
				System.gc();
				for (final Iterator i = new ArrayList(Transaction.listeners)
						.iterator(); i.hasNext();)
					((WeakTransactionListener) i.next()).get();
				System.err.println(Transaction.listeners.size());
			}
		}, Transaction.LISTENER_FLUSH_INTERVAL * 1000,
				Transaction.LISTENER_FLUSH_INTERVAL * 1000);
	}
}
