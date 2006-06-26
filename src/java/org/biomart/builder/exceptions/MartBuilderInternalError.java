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

package org.biomart.builder.exceptions;

/**
 * This is a serious internal error.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 19th May 2006
 * @since 0.1
 */
public class MartBuilderInternalError extends Error {
	private static final long serialVersionUID = 1;

	/**
	 * Creates a new instance of <tt>MartBuilderInternalError</tt> without
	 * detail message.
	 */
	public MartBuilderInternalError() {
		super();
	}

	/**
	 * Constructs an instance of <tt>MartBuilderInternalError</tt> with the
	 * specified detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public MartBuilderInternalError(String msg) {
		super(msg);
	}

	/**
	 * Constructs an instance of <tt>MartBuilderInternalError</tt> with the
	 * specified detail message and cause.
	 * 
	 * @param msg
	 *            the detail message.
	 * @param t
	 *            the underlying cause.
	 */
	public MartBuilderInternalError(String msg, Throwable t) {
		super(msg, t);
	}

	/**
	 * Constructs an instance of <tt>MartBuilderInternalError</tt> with the
	 * specified cause.
	 * 
	 * @param t
	 *            the underlying cause.
	 */
	public MartBuilderInternalError(Throwable t) {
		super(t);
	}
}