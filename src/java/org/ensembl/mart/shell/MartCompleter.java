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

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.Dataset;
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
	private Iterator possibleValues;  // iterator for subsequent calls.
	private SortedSet commandSet = new TreeSet();
	private SortedSet backupSet = new TreeSet();
	
	//sequence possibilities
	private final String SEQUENCE = "sequence ";
	private final String FROM = "from";
	
	private boolean seqsLoaded = false;
  
  public MartCompleter(MartConfiguration martconf) {
    // add commands
    commandSet.addAll(new MartShell().availableCommands);
  	
  	// add sequences
  	commandSet.addAll(SequenceDescription.SEQS);
  	
  	Dataset[] dsets = martconf.getDatasets();
  	for (int i = 0, n = dsets.length; i < n; i++) {
			Dataset dataset = dsets[i];
			commandSet.add(dataset.getInternalName());
			
			getPages(dataset);
		}
  }
  
	public String completer(String text, int state) {
			if (state == 0) {
					 // first call to completer(): initialize our choices-iterator
					 possibleValues = commandSet.tailSet(text).iterator();
			}
			
			if (Readline.getLineBuffer().endsWith(SEQUENCE)) {
					if (! seqsLoaded) {
						Iterator tmp = SequenceDescription.SEQS.iterator();
						
						for (Iterator iter = commandSet.iterator(); iter.hasNext();)
							backupSet.add(iter.next());
						
						commandSet = new TreeSet();
							
						while (tmp.hasNext())
							commandSet.add(tmp.next());
						  
						possibleValues = commandSet.tailSet(text).iterator();
						seqsLoaded = true;
					}
			}
			
			if (seqsLoaded && Readline.getLineBuffer().endsWith(FROM)) {
				commandSet = new TreeSet();
				for (Iterator iter = backupSet.iterator(); iter.hasNext();)
          commandSet.add(iter.next());
				
				backupSet = new TreeSet();
				possibleValues = commandSet.tailSet(text).iterator();
				seqsLoaded = false;					
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
			if (! commandSet.contains( intName ) )
			  commandSet.add(intName);
			getNames(page);
		}
		
		AttributePage[] aps = dset.getAttributePages();
		for (int i = 0, n = aps.length; i < n; i++) {
			AttributePage page = aps[i];
			String intName = page.getInternalName();
   		if (! commandSet.contains( intName ) )
	  		commandSet.add(intName);
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
				if (! commandSet.contains(intName))
				  commandSet.add(intName);	
			}
  	  
  	  //get FilterDescriptions
  	  List fdesc = fpage.getAllUIFilterDescriptions();
  	  for (int i = 0, n = fdesc.size(); i < n; i++) {
				Object element = fdesc.get(i);
				
				String intName = null;
				if (element instanceof UIFilterDescription)
				  intName = ( (UIFilterDescription) element).getInternalName();
				else
				  intName = ( (UIDSFilterDescription) element).getInternalName();
				  
				if (! commandSet.contains(intName))
					commandSet.add(intName);
			}
  	}
  	else {
  		AttributePage apage = (AttributePage) page;
  		List as = apage.getAllUIAttributeDescriptions();
  		for (int i = 0, n = as.size(); i < n; i++) {
				UIAttributeDescription element = (UIAttributeDescription) as.get(i);
				String intName = element.getInternalName();
				
				if (! commandSet.contains(intName))
					commandSet.add(intName);				
			}
  	}
  }
}
