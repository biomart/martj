/*
 * Created on Jul 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * 
 * @author <a href="mailto:arek@ebi.ac.uk">Arek Kasprzyk</a>
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class ConfigurationBase implements Cloneable {

	protected Element element;

	protected int[] requiredFields;

	ArrayList children = new ArrayList();

	public ConfigurationBase(Element element) {

		this.element = element;
	}

	public ConfigurationBase() {

		Element element = new Element("new");
		this.element = element;

	}

	public ConfigurationBase copy() {

		ConfigurationBase obj = null;
		try {
			obj = (ConfigurationBase) this.clone();
			Element newElement = (Element) this.element.clone();
			obj.element = newElement;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return obj;

	}

	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void setRequiredFields(int[] reqFields) {
		requiredFields = reqFields;
	}

	public int[] getRequiredFields() {
		if (requiredFields == null) {
			int[] newRequiredFields = { 0 };
			return newRequiredFields;
		}
		return requiredFields;
	}

	public void addChildObject(ConfigurationBase base) {
		this.children.add(base);
	}

	public ConfigurationBase[] getChildObjects() {
		ConfigurationBase[] b = new ConfigurationBase[children.size()];
		return (ConfigurationBase[]) children.toArray(b);
	}

	public void removeChildObject(String internalName) {

		int i = 0;

		for (i = 0; i < children.size(); i++) {

			ConfigurationBase cb = (ConfigurationBase) children.get(i);

			if (cb.element.getAttributeValue("internalName").equals(
					internalName)) {

				cb.element.detach();
				children.remove(i);
				continue;

			}

		}

	}

}