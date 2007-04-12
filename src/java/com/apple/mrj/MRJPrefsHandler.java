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
package com.apple.mrj;

/**
 * This class provides some useful fake stuff for launching any
 * BioMart Java GUI appliaction on a Mac with nice Mac GUI stuff.
 * On a real Mac it gets overridden by the system version of the
 * same class. On non-Mac machines it is ignored.
 * <p>
 * Mac-specific stuff from <a
 * href="http://www.kfu.com/~nsayer/Java/reflection.html">http://www.kfu.com/~nsayer/Java/reflection.html</a>.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public interface MRJPrefsHandler {
	/**
	 * Handle requests.
	 */
	public void HandlePrefs();
}
