/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OptionPush specifies a set of options that should be pushed onto
 * a filter. These options replace any options currently available on
 * that filter. It contains the name of the filter, available via getRef(),
 * and the options that are to be pushed, available via getOptions().
 */
public class OptionPush extends BaseConfigurationObject {

  private String ref;
  private List options = new ArrayList();
  private int hashcode = -1;

  /**
   * @param internalName
   * @param displayName
   * @param description
   * @throws ConfigurationException
   */
  public OptionPush(
    String internalName,
    String displayName,
    String description,
    String ref)
    throws ConfigurationException {

    super(internalName, displayName, description);

    if (ref == null || ref.equals(""))
      throw new ConfigurationException("Configuration Object must contain a ref\n");

    this.ref = ref;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {

    if (hashcode == -1) {

      hashcode = super.hashCode();

      hashcode = (31 * hashcode) + ref.hashCode();

      for (Iterator iter = options.iterator(); iter.hasNext();) {
        hashcode = (31 * hashcode) + iter.next().hashCode();
      }

    }
    return hashcode;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return o instanceof OptionPush && hashCode() == o.hashCode();
  }

  /**
   * @return name of filter the options should be set on.
   */
  public String getRef() {
    return ref;
  }

  /**
   * @param option an option that should be set on the target filter.
   */
  public void addOption(Option option) {
    options.add(option);
  }

  /**
   * @return Option[] all options to be pushed.
   */
  public Option[] getOptions() {

    return (Option[]) options.toArray(new Option[options.size()]);
  }


  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[");
    buf.append(super.toString());
    buf.append(", ref=").append(ref);
    buf.append(", options=").append(options);
    buf.append("]");

    return buf.toString();
  }

}
