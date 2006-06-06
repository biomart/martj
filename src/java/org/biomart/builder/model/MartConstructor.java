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

package org.biomart.builder.model;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This interface defines the behaviour expected from an object which can take a
 * dataset and actually construct a mart based on this information. Whether it
 * carries out the task or just writes some DDL to be run by the user later is
 * up to the implementor.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 5th June 2006
 * @since 0.1
 */
public interface MartConstructor extends DataLink, Comparable {
	/**
	 * This method takes a dataset and either generates a script for the user to
	 * run later to construct a mart, or does the work right now. The end result
	 * should be a completely finished and populated mart, or the script to make
	 * one. The work is done inside a thread, which is returned unstarted. The
	 * user should create a new {@link Thread} instance around this, and start
	 * it by calling {@link Thread#run()}. They can then monitor it using the
	 * methods provided by the {@link ConstructorRunnable} interface.
	 * 
	 * @param ds
	 *            the dataset to build the mart for.
	 * @return the thread that will build it.
	 */
	public ConstructorRunnable getConstructorRunnable(DataSet ds);

	/**
	 * Returns the name of this constructor.
	 * 
	 * @return the name of this constructor.
	 */
	public String getName();

	/**
	 * The base implementation simply does the bare minimum, ie. synchronises
	 * the dataset before starting work. It doesn't actually generate any tables
	 * or DDL. You should override the {@link #getConstructorRunnable(DataSet)}
	 * method to actually create a construction thread that does some useful
	 * work.
	 */
	public abstract class GenericMartConstructor implements MartConstructor {
		private final String name;

		/**
		 * The constructor creates a mart constructor with the given name.
		 * 
		 * @param name
		 *            the name for this new constructor.
		 */
		public GenericMartConstructor(String name) {
			// Remember the values.
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public abstract ConstructorRunnable getConstructorRunnable(DataSet ds);
		
		public boolean test() throws Exception {
			return true;
		}

		public boolean canCohabit(DataLink partner) {
			return false;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			MartConstructor c = (MartConstructor) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof MartConstructor))
				return false;
			MartConstructor c = (MartConstructor) o;
			return c.toString().equals(this.toString());
		}
	}
	
	/**
	 * This class refers to a placeholder mart constructor which does nothing
	 * except prevent null pointer exceptions.
	 */
	public static class DummyMartConstructor extends GenericMartConstructor {
		/**
		 * The constructor passes everything on up to the parent.
		 * @param name the name to give this mart constructor.
		 */
		public DummyMartConstructor(String name) {
			super(name);
		}
		
		/**
		 * {@inheritDoc}
		 * <p>
		 * This runnable will always fail immediately without attempting
		 * to do anything, throwing a constructor explanation saying
		 * that it does not implement any kind of SQL generation.
		 */
		public ConstructorRunnable getConstructorRunnable(DataSet ds) {
			return new ConstructorRunnable() {
				public void run() {
				}

				public String getStatusMessage() {
					return "";
				}

				public int getPercentComplete() {
					return 100;
				}

				public Exception getFailureException() {
					return new ConstructorException(BuilderBundle
							.getString("defaultMartConstNotImplemented"));
				}

				public void cancel() {
				}
			};
		}
	}


	/**
	 * This interface defines a class which does the actual construction work.
	 * It should keep its status up-to-date, as these will be displayed
	 * regularly to the user. You should probably provide a constructor which
	 * takes a dataset as a parameter.
	 */
	public interface ConstructorRunnable extends Runnable {

		/**
		 * This method should return a message describing what the thread is
		 * currently doing.
		 * 
		 * @return a message describing current activity.
		 */
		public String getStatusMessage();

		/**
		 * This method should return a value between 0 and 100 indicating how
		 * the thread is getting along in the general scheme of things. 0
		 * indicates just starting, 100 indicates complete or very nearly
		 * complete.
		 * 
		 * @return a percentage indicating how far the thread has got.
		 */
		public int getPercentComplete();

		/**
		 * If the thread failed, this method should return an exception
		 * describing the failure. If it succeeded, or is still in progress and
		 * hasn't failed yet, it should return <tt>null</tt>.
		 * 
		 * @return the exception that caused the thread to fail, if any, or
		 *         <tt>null</tt> otherwise.
		 */
		public Exception getFailureException();

		/**
		 * This method will be called if the user wants the thread to stop work
		 * straight away. It should set an exception for
		 * {@link #getFailureException()} to return saying that it was
		 * cancelled, so that the user knows it was so, and doesn't think it
		 * just finished successfully without any warnings.
		 */
		public void cancel();
	}
}
