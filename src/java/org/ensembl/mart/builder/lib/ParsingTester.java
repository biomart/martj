/*
 * Created on Jul 19, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

/**
 * @author arek
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */



public class ParsingTester {

	private static String inputConfigFile="/Users/arek/fly.xml";
	private static String outputConfigFile="/Users/arek/myxml.xml";

	
	
	public static void main(String[] args) {

		ConfigurationAdaptor cad = new ConfigurationAdaptor();
		
		
		// get config
		NewTransformationConfig nts = cad.getTransformationConfig(inputConfigFile);

		
		/**
		 * traversing the object tree, getting/setting attributes
		 */

        // removing objects
		nts.removeChildObject("fly");

		
		ConfigurationBase[] nbso = nts.getChildObjects();
		for (int i = 0; i < nbso.length; i++) {
			Dataset ds = (Dataset) nbso[i];

			
			// getting attributes
			System.out.println(ds.datasetKey + " internalName: "+ ds.element.getAttributeValue("internalName"));

			// copy object
			Dataset newDS = (Dataset) ds.copy();
			System.out.println(newDS.element.getAttributeValue("internalName"));
			
			ConfigurationBase[] trso = ds.getChildObjects();
			for (int j = 0; j < trso.length; j++) {
				Transformation ts = (Transformation) trso[j];

				
				// setting attributes
				ts.element.setAttribute("internalName", "mm");
				System.out.println(ts.column_operations + " internalName "+ ts.element.getAttributeValue("internalName"));

			}
		}

		
		
		// writing document
		cad.writeDocument(nts,outputConfigFile);

	}
}