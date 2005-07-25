/*
 * Created on Jul 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import org.jdom.Element;

/**
 * @author arek
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TransformationConfig extends ConfigurationBase {

	public TransformationConfig (Element element){
		
		super(element);
		this.element=element;
		
	}
	
	
}
