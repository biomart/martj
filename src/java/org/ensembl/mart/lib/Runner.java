package org.ensembl.mart.lib;

import java.io.IOException;
import java.net.URL;

import org.python.core.PyException;
import org.python.util.PythonInterpreter;

/**
 * @author craig
 *	
 * Runs the GUI from jar.
 */
public class Runner {
	
	public static void main(String[] args) throws PyException, IOException {

	
	  	// convert command line params to a form that can be passed 
	  	// to main() method in python script
		StringBuffer argList = new StringBuffer();
		argList.append("[ \"MartExplorer\"");
		for(int i=0; i<args.length; ++i) {
			argList.append(", \"").append( args[i] ).append("\"");
		}
		argList.append("]");
		
		// The GUI is implemenented as jython script. In order to run it
		// we load it into a python interpreter and then  run it.		
		URL url = ClassLoader.getSystemResource("src/jython/martexplorer/MartExplorerGUIApplication.py");
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile( url.openStream() );
		
		// start gui with command line parameter and set window to close
		// 1 means close program when window closed.
		interp.exec("main("+ argList +" ,1)");	
	}
}
