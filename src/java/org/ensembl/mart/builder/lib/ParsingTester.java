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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;



public class ParsingTester {

	public static void main(String[] args) throws JDOMException, IOException {

	
		SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build("/Users/arek/fly.xml");

        Dataset ds = new Dataset();
        
       
        
        Element root =doc.getRootElement();
        
        System.out.println(root.getChild("Dataset").getAttributeValue("internalName"));
        
        //System.out.println(root.getAttribute("TransformationUnit").getName());
        
        while (root.getChildren().iterator().hasNext()) {
        	
        	//String name = root
        	//Element child = root.getChild();
        	
        	
        }
        
        System.out.println("to string "+root.getChild("Dataset").toString());
        
        ElementFilter filter= new ElementFilter();
        
        
        //System.out.println(" filter "+ root.getChild("Dataset").getContent(filter));
        
        Iterator iter = root.getChild("Dataset").getContent(filter).iterator();
        
        for (int i=0;i<root.getChild("Dataset").getContent(filter).size();i++)System.out.println(iter.next());
        
        System.out.println(root.getAttributeValue("Dataset"));
        
        
        //root.
        
	
	}
}
