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

package org.biomart.builder.controller;

/**
 * Tools for running SQL.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class MartRunner {

	/**
	 * Request a new job ID. Don't define the job, just request an ID for one
	 * that could be defined in future.
	 * 
	 * @return a unique job ID.
	 */
	public static int requestNewJob() {
		// TODO Generate a new job ID.
		return 0;
	}

	/**
	 * Flag that a job is about to start receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 */
	public static void beginJob(final int jobId) {
		// TODO Begin a job definition.
	}

	/**
	 * Flag that a job has finished receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 */
	public static void endJob(final int jobId) {
		// TODO End a job definition.
	}

	// Tools are static and cannot be instantiated.
	private MartRunner() {
	}
}
