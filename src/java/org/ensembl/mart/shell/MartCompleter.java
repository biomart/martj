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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package org.ensembl.mart.shell;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartCompleter implements ReadlineCompleter {

	/* (non-Javadoc)
	 * @see org.gnu.readline.ReadlineCompleter#completer(java.lang.String, int)
	 */
	private Iterator possibleValues; // iterator for subsequent calls.
	private SortedSet currentSet = new TreeSet();

	private SortedSet commandSet = new TreeSet(); // will hold basic shell commands
	private Map setMapper = new HashMap();  // will hold special sets, with String keys
	
	private SortedSet selectSet = new TreeSet(); // will hold attribute names
	private SortedSet sequenceSet = new TreeSet(); // will hold sequences
	private SortedSet fromSet = new TreeSet(); // will hold datasets
	private SortedSet whereSet = new TreeSet(); // will hold filters 
	private SortedSet describeSet = new TreeSet(); // will hold pages
	private SortedSet helpSet = new TreeSet(); // will hold help keys available

	private SortedSet backupSet = new TreeSet();

	//sequence possibilities
	private final String COMMANDS = "commands";
	private final String DESCRIBE = "describe";
	private final String HELP = "help";
	private final String SELECT = "select";
	private final String SEQUENCE = "sequence";
	private final String FROM = "from";
	private final String WHERE = "where";

	public MartCompleter(MartConfiguration martconf) {
		setMapper.put(COMMANDS, commandSet);
		setMapper.put(DESCRIBE, describeSet);
		setMapper.put(HELP, helpSet);
		setMapper.put(SELECT, selectSet);
		setMapper.put(SEQUENCE, sequenceSet);
		setMapper.put(FROM, fromSet);
		setMapper.put(WHERE, whereSet);
		
		Dataset[] dsets = martconf.getDatasets();
		for (int i = 0, n = dsets.length; i < n; i++) {
			Dataset dataset = dsets[i];
			fromSet.add(dataset.getInternalName());
			describeSet.add(dataset.getInternalName());

			getPages(dataset);
		}

		SetDefaultMode();
	}

	public String completer(String text, int state) {
		if (state == 0) {
			// first call to completer(): initialize our choices-iterator

			if (Readline.getLineBuffer().indexOf(DESCRIBE) >= 0)
				SetDescribeMode();
			if (Readline.getLineBuffer().indexOf(HELP) >= 0)
			  SetHelpMode();
			  	
      int selectInd = Readline.getLineBuffer().lastIndexOf(SELECT);
      int seqInd = Readline.getLineBuffer().lastIndexOf(SEQUENCE);
      int fromInd = Readline.getLineBuffer().lastIndexOf(FROM);
      int whereInd = Readline.getLineBuffer().lastIndexOf(WHERE);

			if ( ( selectInd > seqInd ) && ( selectInd > fromInd ) && (selectInd > whereInd ) )
				SetSelectMode();
			if ( ( seqInd > selectInd ) && ( seqInd > fromInd ) && ( seqInd > whereInd ) )
				SetSequenceMode();
			if ( (fromInd > selectInd ) && ( fromInd > seqInd ) && ( fromInd > whereInd ) )
				SetFromMode();
			if ( ( whereInd > selectInd ) && ( whereInd > seqInd ) && ( whereInd > fromInd ) )
				SetWhereMode();
				
			possibleValues = currentSet.tailSet(text).iterator();
		}

		if (possibleValues.hasNext()) {
			String nextKey = (String) possibleValues.next();
			if (nextKey.startsWith(text))
				return nextKey;
		}

		return null; // we reached the last choice.
	}

	private void getPages(Dataset dset) {
		FilterPage[] fps = dset.getFilterPages();
		for (int i = 0, n = fps.length; i < n; i++) {
			FilterPage page = fps[i];
			String intName = page.getInternalName();
			if (!describeSet.contains(intName))
				describeSet.add(intName);
			getNames(page);
		}

		AttributePage[] aps = dset.getAttributePages();
		for (int i = 0, n = aps.length; i < n; i++) {
			AttributePage page = aps[i];
			String intName = page.getInternalName();
			if (!describeSet.contains(intName))
				describeSet.add(intName);
			getNames(page);
		}
	}

	private void getNames(Object page) {
		if (page instanceof FilterPage) {
			FilterPage fpage = (FilterPage) page;

			//get FilterSetDescriptions
			FilterSetDescription[] fsets = fpage.getAllFilterSetDescriptions();
			for (int i = 0, n = fsets.length; i < n; i++) {
				FilterSetDescription description = fsets[i];
				String intName = description.getInternalName();
				if (!whereSet.contains(intName))
					whereSet.add(intName);
				if (!describeSet.contains(intName))
					describeSet.add(intName);
			}

			//get FilterGroups for describeMode
			List fgroups = fpage.getFilterGroups();
			for (int i = 0, n = fgroups.size(); i < n; i++) {
				Object group = fgroups.get(i);
				if (group instanceof FilterGroup) {
					String intName = ((FilterGroup) group).getInternalName();
					if (!describeSet.contains(intName))
						describeSet.add(intName);
				} // do not worry about DSFilterGroups for now, nothing to describe
			}

			//get FilterDescriptions
			List fdesc = fpage.getAllUIFilterDescriptions();
			for (int i = 0, n = fdesc.size(); i < n; i++) {
				Object element = fdesc.get(i);

				String intName = null;
				if (element instanceof UIFilterDescription)
					intName = ((UIFilterDescription) element).getInternalName();
				else
					intName = ((UIDSFilterDescription) element).getInternalName();

				if (!whereSet.contains(intName))
					whereSet.add(intName);
			}
		} else {
			AttributePage apage = (AttributePage) page;
			List as = apage.getAllUIAttributeDescriptions();
			for (int i = 0, n = as.size(); i < n; i++) {
				UIAttributeDescription element = (UIAttributeDescription) as.get(i);
				String intName = element.getInternalName();

				if (!selectSet.contains(intName))
					selectSet.add(intName);
			}
		}
	}

	/*
	 * for internal use only, doesnt make sense to make public
	 */
	private void SetDescribeMode() {
		currentSet = new TreeSet();
		currentSet.addAll( (SortedSet) setMapper.get(DESCRIBE) );
	}

  private void SetHelpMode() {
  	currentSet = new TreeSet();
  	currentSet.addAll( (SortedSet) setMapper.get(HELP) );
  }
  
	/**
	 * Sets the MartCompleter into the default mode, with only commands available in the choices set
	 *
	 */
	public void SetDefaultMode() {
		currentSet = new TreeSet();
		currentSet.addAll( (SortedSet) setMapper.get(COMMANDS) );
	}

	/**
	 * Sets the MartCompleter into SelectMode, with commands, and attributes in the choices set
	 *
	 */
	public void SetSelectMode() {
		currentSet = new TreeSet();
		currentSet.addAll( (SortedSet) setMapper.get(SELECT) );
	}

	/**
	 * Sets the MartCompleter into SequenceModem with only sequences avaialable in the choices set
	 *
	 */
	public void SetSequenceMode() {
		currentSet = new TreeSet();
		currentSet.addAll( (SortedSet) setMapper.get(SEQUENCE) );
	}

	/**
	 * Sets the MartCompleter into the From mode, with commands and datasets available in the choices set 
	 *
	 */
	public void SetFromMode() {
		currentSet = new TreeSet();
		currentSet.addAll( (SortedSet) setMapper.get(FROM) );
	}

	/**
	 * Sets the MartCompleter into the Where mode, with commands, filters, and filtersets avaiable in the choices set
	 *
	 */
	public void SetWhereMode() {
		currentSet = new TreeSet();
		currentSet.addAll( (SortedSet) setMapper.get(WHERE) );
	}
	
	public void AddAvailableCommandsTo(String key, Collection commands) {
		if (! setMapper.containsKey(key))
		  logger.warn("Key " + key + " is not a member of the command completion system\n");
		else {
			SortedSet set = (SortedSet) setMapper.get(key);
			set.addAll(commands);
			setMapper.put(key, set);
		}		  
	}
	
	private Logger logger = Logger.getLogger(MartCompleter.class.getName());
}