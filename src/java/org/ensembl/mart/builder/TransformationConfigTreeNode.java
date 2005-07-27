/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.d

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.builder;

import javax.swing.tree.DefaultMutableTreeNode;

import org.ensembl.mart.builder.lib.*;
 
/**
 * Class TransformationConfigTreeNode extends DefaultMutableTreeNode.
 *
 * <p>This class is written so that the tree node is aware of the datasetconfig etc objects
 * </p>
 *
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.TransformationConfig
 */

public class TransformationConfigTreeNode extends DefaultMutableTreeNode {

  /**
   * Each Node stores all child objects in a single Vector, but some TransformationConfigTree
   * userObjects store heterogenous groups of children in different lists in a particular 
   * order. This method calculates the index adjustment to apply to any node Vector index 
   * to get the TransformationConfigTree userObject index, which could be 0 for any TransformationConfigTree
   * object that only stores one type of child object.  In general, for a TransformationConfigTree
   * userObject which stores separate lists of objects a, b, and c in order, the relationship
   * between the node Vector index (V) and the individual object index within the
   * parent TransformationConfigTree userObject is:
   * (a) V[i] = a[i]
   * (b) V[i] = b[i - (a.length)]
   * (c) V[i] = c[i - (a.length + b.length)] 
   * @param parent - TransformationConfigTree userObject into which dropnode is to be dropped 
   * @param child - TransformationConfigTree userObject object for which an index inside parent is needed 
   * @return adjustment for any index of child inside parent.
   */
  	protected static int getHeterogenousOffset(Object parent, Object child) {
    	return 0;
  	}
  
	protected String name;

	public TransformationConfigTreeNode(String name) {
		this.name = name;
	}

	public TransformationConfigTreeNode(String name, Object obj) {
		this.name = name;
		this.setUserObject(obj);
	}

	public void setName(String newName) {
		name = newName;
	}

	public String toString() {
		return name;
	}

	public void setUserObject(Object obj) {
		super.setUserObject(obj);

		if (obj instanceof org.ensembl.mart.builder.lib.TransformationConfig) {
			setName("TransformationConfig: " + ((ConfigurationBase) obj).getElement().getAttributeValue("internalName"));
			TransformationConfig dsv = (TransformationConfig) obj;
			ConfigurationBase[] fpages = dsv.getChildObjects();
			for (int i = 0; i < fpages.length; i++) {
					Dataset fp = (Dataset) fpages[i];
					String fpName = fp.getElement().getAttributeValue("internalName");
					TransformationConfigTreeNode fpNode = new TransformationConfigTreeNode("Dataset:" + fpName);
					fpNode.setUserObject(fp);
					this.add(fpNode);
					ConfigurationBase[] groups = fp.getChildObjects();
					for (int j = 0; j < groups.length; j++) {
							Transformation fiGroup = (Transformation) groups[j];
							String grName = fiGroup.getElement().getAttributeValue("userTableName");
							TransformationConfigTreeNode grNode = new TransformationConfigTreeNode("Transformation:" + grName);
							grNode.setUserObject(fiGroup);
							ConfigurationBase[] tunits = fiGroup.getChildObjects();
							for (int k = 0; k < tunits.length; k++) {
									TransformationUnit tunit = (TransformationUnit) tunits[k];
									String tuName = tunit.getElement().getAttributeValue("internalName");
									TransformationConfigTreeNode tuNode = new TransformationConfigTreeNode("TransformationUnit:" + tuName);
									tuNode.setUserObject(tunit);							
							}
					}
			}
							
		} else if (obj instanceof org.ensembl.mart.builder.lib.Dataset) {
			setName("Dataset: " + ((ConfigurationBase) obj).getElement().getAttributeValue("internalName"));
			Dataset fp = (Dataset) obj;
			ConfigurationBase[] groups = fp.getChildObjects();
			for (int j = 0; j < groups.length; j++) {
					Transformation fiGroup = (Transformation) groups[j];
					String grName = fiGroup.getElement().getAttributeValue("userTableName");
					TransformationConfigTreeNode grNode = new TransformationConfigTreeNode("Transformation:" + grName);
					grNode.setUserObject(fiGroup);
					this.add(grNode);
					ConfigurationBase[] tunits = fiGroup.getChildObjects();
					for (int k = 0; k < tunits.length; k++) {
							TransformationUnit tunit = (TransformationUnit) tunits[k];
							String tuName = tunit.getElement().getAttributeValue("internalName");
							TransformationConfigTreeNode tuNode = new TransformationConfigTreeNode("TransformationUnit:" + tuName);
							tuNode.setUserObject(tunit);							
					}
			}

		} else if (obj instanceof org.ensembl.mart.builder.lib.Transformation) {
			setName("Transformation: " + ((ConfigurationBase) obj).getElement().getAttributeValue("userTableName"));
			Transformation fp = (Transformation) obj;
			ConfigurationBase[] groups = fp.getChildObjects();
			for (int j = 0; j < groups.length; j++) {
					TransformationUnit fiGroup = (TransformationUnit) groups[j];
					String grName = fiGroup.getElement().getAttributeValue("userTableName");
					TransformationConfigTreeNode grNode = new TransformationConfigTreeNode("TransformationUnit:" + grName);
					grNode.setUserObject(fiGroup);
					this.add(grNode);	
			}	

		} else if (obj instanceof org.ensembl.mart.builder.lib.TransformationUnit) {
			setName("TransformationUnit: " + ((ConfigurationBase) obj).getElement().getAttributeValue("internalName"));
		} 

	}
	
}


