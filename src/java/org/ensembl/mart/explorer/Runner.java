package org.ensembl.mart.explorer;

/**
 * @author craig
 *	
 * Runs the GUI from jar.
 */
public class Runner {

	public static void main(String[] args) {
		org.python.util.jython.main(new String[] {"src/jython/martexplorer/MartExplorerGUIApplication.py"});
	}
}
