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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.lib.config.DSFilterGroup;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSet;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.MapFilterDescription;
import org.ensembl.mart.lib.config.FilterDescription;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;

/**
 * <p>ReadlineCompleter implimenting object allowing Mart - specific
 * behavior.  It provides command-completion choices that are based
 * on the position within an MQL command (see MartShellLib for a description
 * of the Mart Query Language) where a user requests completion.
 * It provides public methods to allow its completion mode to
 * be changed, and allow these modes to be backed with specific lists of
 * choices for completion. The system provides the following modes:</p>
 * <ul>
 * <li><p>Command Mode: keyword: "command". Only available MartShell commands, and other names added by the client for this mode, are made available.</p>
 * <li><p>Describe Mode: keyword: "describe". This is a special mode, and requires a Map be added, which further categorizes the describe system commands based on successive keys.</p>
 * <li><p>Help Mode: keyword: "help". Only names added by the client for this mode are made available.</p>
 * <li><p>Get Mode: keyword: "get".  Only attribute_names, and other names added by the client for this mode are made available.</p>
 * <li><p>Sequence Mode: keyword: "sequence".  Only names added by the client for this mode are made available.</p>
 * <li><p>Dataset Mode: keyword: "datasets".  Only dataset names, and other names added by the client for this mode, are made available.</p>
 * <li><p>Where Mode: keyword: "where".  Only filter_names, filter_set_names, and other names added by the client for this mode, are made available.</p>
 * </ul>
 * <br>
 * <p>In some cases it will switch its own mode, but
 * the client code should manage its mode, as well, otherwise it may get stuck
 * in a particular mode when its keywords are processed in a line previous to the current
 * Readline line buffer.</p>
 * <p> Users wishing to add new Domain Specific attributes (analogous to sequences in the present system) should add keywords and modes to the system to
 *  support them.</p>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see MartShellLib
 * @see org.ensembl.mart.lib.config.MartConfiguration
 */
public class MartCompleter implements ReadlineCompleter {

	/* (non-Javadoc)
	 * @see org.gnu.readline.ReadlineCompleter#completer(java.lang.String, int)
	 */
	private Iterator possibleValues; // iterator for subsequent calls.
	private SortedSet currentSet = new TreeSet();
	private MartConfiguration martconf = null;

	private SortedSet commandSet = new TreeSet(); // will hold basic shell commands
	private Map setMapper = new HashMap(); // will hold special sets, with String keys

	private SortedSet getSet = new TreeSet(); // will hold user supplied values to add to attribte names during completion
	private SortedSet sequenceSet = new TreeSet(); // will hold sequences
	private SortedSet datasetSet = new TreeSet(); // will hold datasets
	private SortedSet whereSet = new TreeSet(); // will hold filters 
	private SortedSet helpSet = new TreeSet(); // will hold help keys available

	private final List NODATASETWARNING = Collections.unmodifiableList(Arrays.asList(new String[] { "No dataset set", "!" }));

	//describe Mode variables
	private Pattern describeStart = Pattern.compile("^describe\\s\\w*$");
	private Pattern describePageStart = Pattern.compile("^describe\\s(\\w+)\\s\\w*$");
	private Pattern describePage = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s\\w*$");
	private Pattern describeGroupStart = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s\\w*$");
	private Pattern describeGroup = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s\\w*$");
	private Pattern describeCollectionStart = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s\\w*$");
	private Pattern describeCollection = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s\\w*$");
	private Pattern describeDescriptionStart = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s\\w*$");
	private Pattern describeDescription = Pattern.compile("^describe\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)\\s\\w*$");

	//Query Mode Patterns
	private Pattern qModeStartPattern = Pattern.compile("^using\\s$");
	public Pattern qModePattern = Pattern.compile("^using\\s(\\w+).*");

	private final String COMMANDS = "commands";
	private final String DESCRIBE = "describe";
	private final String HELP = "help";
	private final String USE = "use";
	private final String DATASETS = "datasets";

	private Dataset envDataset = null;
	private Dataset lastLocalDataset = null;
	private List LocalDatasetStack = new ArrayList();

	//listlevel characters
	public int listLevel = 0;
	private String lastLine = null;

	private Logger logger = Logger.getLogger(MartCompleter.class.getName());

	/**
	 * Creates a MartCompleter Object.  The MartCompleter processes the MartConfiguration
	 * object, and stores important internal_names into the completion sets that are applicable to the given MartConfiguration object.
	 * @param martconf - a MartConfiguration Object
	 */
	public MartCompleter(MartConfiguration martconf) {
		setMapper.put(COMMANDS, commandSet);
		setMapper.put(HELP, helpSet);
		setMapper.put(MartShellLib.GETQSTART, getSet);
		setMapper.put(MartShellLib.QSEQUENCE, sequenceSet);
		setMapper.put(DATASETS, datasetSet);
		setMapper.put(MartShellLib.QWHERE, whereSet);

		this.martconf = martconf;

		Dataset[] dsets = martconf.getDatasets();
		for (int i = 0, n = dsets.length; i < n; i++) {
			Dataset dataset = dsets[i];
			datasetSet.add(dataset.getInternalName());
		}

		SetCommandMode();
	}

	/**
	 * Implimentation of the ReadlineCompleter completer method.  Switches its state based on the presence and position of keywords in the command.
	 * <ul>
	 *   <li><p> If the word "describe" occurs, Describe Mode is chosen.</p>
	 *   <li><p> If the word "help" occurs, Help Mode is chosen.</p> 
	 *   <li><p> If the word "select" is present, and occurs after all other keywords present, then Select Mode is chosen.</p>
	 *   <li><p> If the word "sequence" is present, and occurs after all other keywords present, then Sequence Mode is chosen.</p>
	 *   <li><p> If the word "from" is present, and occurs after all other keywords present, then From Mode is chosen.</p>
	 *   <li><p> If the word "where" is present, and occurs after all other keywords present, then Where Mode is chosen.</p>
	 * </ul>
	 */
	public String completer(String text, int state) {
		if (state == 0) {
			// first call to completer(): initialize our choices-iterator
			String currentCommand = Readline.getLineBuffer();
			SetModeForLine(Readline.getLineBuffer());
			possibleValues = currentSet.tailSet(text).iterator();
		}

		if (possibleValues.hasNext()) {
			String nextKey = (String) possibleValues.next();
			if (nextKey.startsWith(text))
				return nextKey;
		}

		return null; // we reached the last choice.
	}

	public void SetModeForLine(String currentCommand) {
		if (lastLine == null || !(lastLine.equals(currentCommand))) {
			if (currentCommand.indexOf(DESCRIBE) >= 0)
				SetDescribeMode();
			if (currentCommand.indexOf(HELP) >= 0)
				SetHelpMode();
			if (currentCommand.indexOf(USE) >= 0)
				SetDatasetMode();

			int usingInd = currentCommand.lastIndexOf(MartShellLib.USINGQSTART);
			int len = MartShellLib.USINGQSTART.length();

			if (usingInd >= 0) {
				//try pushing any local datasets available
				int index = currentCommand.indexOf(MartShellLib.USINGQSTART);

				while (index >= 0) {
					String testString = currentCommand.substring(index);

					Matcher qModeMatcher = qModePattern.matcher(testString);
					if (qModeMatcher.matches()) {
						String datasetName = qModeMatcher.group(1);
						pushLocalDataset(datasetName);
					}

					index = currentCommand.substring(index + len).indexOf(MartShellLib.USINGQSTART);
				}
			}

			if (lastLine == null) {
				checkListChars(currentCommand);
			} else {
				if (!currentCommand.equals(lastLine)) {
          if (currentCommand.startsWith(lastLine)) {
						String newBits = currentCommand.substring(lastLine.length());
						checkListChars(newBits);
					} else {
            checkListChars(currentCommand);
					}
				}
			}

			// determine which mode to be in during a query
			int getInd = currentCommand.lastIndexOf(MartShellLib.GETQSTART);
			int seqInd = currentCommand.lastIndexOf(MartShellLib.QSEQUENCE);
			int whereInd = currentCommand.lastIndexOf(MartShellLib.QWHERE);

			if ((usingInd > seqInd) && (usingInd > getInd) && (usingInd > whereInd))
				SetDatasetMode();
			if ((seqInd > usingInd) && (seqInd > getInd) && (seqInd > whereInd))
				SetSequenceMode();
			if ((getInd > usingInd) && (getInd > seqInd) && (getInd > whereInd))
				SetGetMode();
			if ((whereInd > usingInd) && (whereInd > seqInd) && (whereInd > getInd))
				SetWhereMode();

			lastLine = currentCommand;
		}
	}

	private void checkListChars(String currentCommand) {
		//test for listLevel characters, and popLocalDataset if needed
		if (currentCommand.indexOf(MartShellLib.LISTSTARTCHR) >= 0) {
			for (int i = currentCommand.indexOf(MartShellLib.LISTSTARTCHR), n = currentCommand.length(); i < n; i++) {
				if (currentCommand.charAt(i) == MartShellLib.LISTSTARTCHR)
					listLevel++;
			}
		}

		if (currentCommand.indexOf(MartShellLib.LISTENDCHR) >= 0) {
			for (int i = currentCommand.indexOf(MartShellLib.LISTENDCHR), n = currentCommand.length(); i < n; i++) {
				if (currentCommand.charAt(i) == MartShellLib.LISTENDCHR)
					listLevel--;

				while (LocalDatasetStack.size() > listLevel)
					popLocalDataset();
			}
      SetWhereMode();
		}
	}

	/**
	 * Sets the Environment Dataset for the session.  This dataset remains in effect
	 * for the duration of the MartCompleter objects existence, and can only be over ridden
	 * by a subsequent call to setEnvDataset, or a local dataset in the command
	 * 
	 * @param datasetName - String Name of the dataset
	 */
	public void setEnvDataset(String datasetName) {
		envDataset = martconf.getDatasetByName(datasetName); // might return null, but that is ok
	}

	/**
	 * Pushes a localDataset onto the stack.
	 * @param datasetName - String name of the requested dataset
	 */
	public void pushLocalDataset(String datasetName) {
		if (lastLocalDataset != null) {
			if (!lastLocalDataset.getInternalName().equals(datasetName)) {
				if (martconf.containsDataset(datasetName)) {
					LocalDatasetStack.add(lastLocalDataset);
					lastLocalDataset = martconf.getDatasetByName(datasetName);
				}
			}
		} else {
			if (martconf.containsDataset(datasetName)) {
				lastLocalDataset = martconf.getDatasetByName(datasetName);
			}
		}
	}

	/**
	 * Removes the first localDataset from the stack.
	 */
	public void popLocalDataset() {
		if (LocalDatasetStack.size() > 0) {
			lastLocalDataset = (Dataset) LocalDatasetStack.remove(0);
		} else {
			lastLocalDataset = null;
		}
	}

	/**
	 * Sets the MartCompleter into Describe Mode, sensitive to where in the describe command the user has requested completion.
	 *
	 */
	public void SetDescribeMode() {
		if (setMapper.containsKey(DESCRIBE)) {
			String currentCommand = Readline.getLineBuffer();
			Map keyMap = (Map) setMapper.get(DESCRIBE);

			//find out where in the command the user has requested completion
			Matcher StartMatcher = describeStart.matcher(currentCommand);
			Matcher PageStartMatcher = describePageStart.matcher(currentCommand);
			Matcher PageMatcher = describePage.matcher(currentCommand);
			Matcher GroupStartMatcher = describeGroupStart.matcher(currentCommand);
			Matcher GroupMatcher = describeGroup.matcher(currentCommand);
			Matcher CollectionStartMatcher = describeCollectionStart.matcher(currentCommand);
			Matcher CollectionMatcher = describeCollection.matcher(currentCommand);
			Matcher DescriptionStartMatcher = describeDescriptionStart.matcher(currentCommand);
			Matcher Descriptionmatcher = describeDescription.matcher(currentCommand);

			if (StartMatcher.matches()) {
				// same as fromMode, note, describeMode is set to true at end
				SetDatasetMode();
			} else if (PageStartMatcher.matches()) {
				// wants the potential page keys
				currentSet = new TreeSet();
				currentSet.addAll(keyMap.keySet());
			} else if (PageMatcher.matches()) {
				//wants the potential values for the given page key
				String dataset = PageMatcher.group(1);
				String pageKey = PageMatcher.group(2);

				if (martconf.containsDataset(dataset)) {
					currentSet = new TreeSet();

					if (pageKey.equals("FilterPage")) {
						FilterPage[] fpages = martconf.getDatasetByName(dataset).getFilterPages();
						for (int i = 0, n = fpages.length; i < n; i++) {
							FilterPage page = fpages[i];
							currentSet.add(page.getInternalName());
						}
					} else if (pageKey.equals("Filter")) {
						List fdesc = martconf.getDatasetByName(dataset).getAllUIFilterDescriptions();
						for (int i = 0, n = fdesc.size(); i < n; i++) {
							Object desc = fdesc.get(i);

							if (desc instanceof FilterDescription)
								currentSet.add(((FilterDescription) desc).getInternalName());
							else
								currentSet.add(((MapFilterDescription) desc).getInternalName());
						}
					} else if (pageKey.equals("AttributePage")) {

						AttributePage[] apages = martconf.getDatasetByName(dataset).getAttributePages();
						for (int i = 0, n = apages.length; i < n; i++) {
							AttributePage page = apages[i];
							currentSet.add(page.getInternalName());
						}
					} else {
						// must be Attribute
						List adesc = martconf.getDatasetByName(dataset).getAllUIAttributeDescriptions();
						for (int i = 0, n = adesc.size(); i < n; i++) {
							Object desc = adesc.get(i);

							if (desc instanceof AttributeDescription) {
								currentSet.add(((AttributeDescription) desc).getInternalName());
							} // else if we add UIDSAttributeDescriptions, add code here
						}
					}
				}

			} else if (GroupStartMatcher.matches()) {
				//wants the potential group keys
				String pageKey = GroupStartMatcher.group(2);

				if (keyMap.containsKey(pageKey)) {
					currentSet = new TreeSet();

					Map groupKeyMap = (Map) keyMap.get(pageKey);
					currentSet.addAll(groupKeyMap.keySet());
				}

			} else if (GroupMatcher.matches()) {
				// wants the potential values for given group key
				String datasetName = GroupMatcher.group(1);
				String pageKey = GroupMatcher.group(2);
				String pageName = GroupMatcher.group(3);
				String groupKey = GroupMatcher.group(4);

				if (pageKey.equals("FilterPage")) {
					if (groupKey.equals("FilterGroup")) {

						if (martconf.containsDataset(datasetName)) {
							if (martconf.getDatasetByName(datasetName).containsFilterPage(pageName)) {
								currentSet = new TreeSet();

								List groups = martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).getFilterGroups();
								for (int i = 0, n = groups.size(); i < n; i++) {
									Object group = groups.get(i);

									if (group instanceof FilterGroup)
										currentSet.add(((FilterGroup) group).getInternalName());
									else
										currentSet.add(((DSFilterGroup) group).getInternalName());
								}
							}

						}

					} else {
						// must be Filter
						if (martconf.containsDataset(datasetName)) {
							if (martconf.getDatasetByName(datasetName).containsFilterPage(pageName)) {
								currentSet = new TreeSet();

								List descs = martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).getAllUIFilterDescriptions();
								for (int i = 0, n = descs.size(); i < n; i++) {
									Object desc = descs.get(i);
									if (desc instanceof FilterDescription)
										currentSet.add(((FilterDescription) desc).getInternalName());
									else
										currentSet.add(((MapFilterDescription) desc).getInternalName());
								}
							}
						}
					}

				} else {
					// must be AttributePage
					if (groupKey.equals("AttributeGroup")) {
						if (martconf.containsDataset(datasetName)) {
							if (martconf.getDatasetByName(datasetName).containsAttributePage(pageName)) {
								currentSet = new TreeSet();

								List groups = martconf.getDatasetByName(datasetName).getAttributePageByName(pageName).getAttributeGroups();
								for (int i = 0, n = groups.size(); i < n; i++) {
									Object group = groups.get(i);

									if (group instanceof AttributeGroup)
										currentSet.add(((AttributeGroup) group).getInternalName());
									else
										currentSet.add(((DSAttributeGroup) group).getInternalName());
								}
							}
						}
					}
				}
			} else if (CollectionStartMatcher.matches()) { //wants the potential collection keys
				String pageKey = CollectionStartMatcher.group(2);

				if (keyMap.containsKey(pageKey)) {
					Map groupKeyMap = (Map) keyMap.get(pageKey);
					String groupKey = CollectionStartMatcher.group(4);

					if (groupKeyMap.containsKey(groupKey)) {
						currentSet = new TreeSet();
						Map collectionKeyMap = (Map) groupKeyMap.get(groupKey);
						currentSet.addAll(collectionKeyMap.keySet());
					}
				}

			} else if (CollectionMatcher.matches()) { //wants the values for the given collection key
				String datasetName = CollectionMatcher.group(1);
				String pageKey = CollectionMatcher.group(2);
				String pageName = CollectionMatcher.group(3);
				String groupName = CollectionMatcher.group(5);
				String collectionKey = CollectionMatcher.group(6);

				if (martconf.containsDataset(datasetName)) {
					if (pageKey.equals("FilterPage")) {

						if (martconf.getDatasetByName(datasetName).containsFilterPage(pageName)) {
							if (martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).containsFilterGroup(groupName)) {
								Object group = martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).getFilterGroupByName(groupName);

								if (group instanceof FilterGroup) {

									if (collectionKey.equals("FilterSet")) {

										if (((FilterGroup) group).hasFilterSets()) {
											currentSet = new TreeSet();

											FilterSet[] fsets = ((FilterGroup) group).getFilterSets();
											for (int i = 0, n = fsets.length; i < n; i++) {
												FilterSet set = fsets[i];
												currentSet.add(set.getInternalName());
											}

										}

									} else if (collectionKey.equals("FilterCollection")) {
										currentSet = new TreeSet();

										FilterCollection[] cols = ((FilterGroup) group).getFilterCollections();
										for (int i = 0, n = cols.length; i < n; i++) {
											FilterCollection collection = cols[i];
											currentSet.add(collection.getInternalName());
										}
									} else { // must be filter
										currentSet = new TreeSet();

										List descs = ((FilterGroup) group).getAllUIFilterDescriptions();
										for (int i = 0, n = descs.size(); i < n; i++) {
											Object desc = (Object) descs.get(i);
											if (desc instanceof FilterDescription)
												currentSet.add(((FilterDescription) desc).getInternalName());
											else
												currentSet.add(((MapFilterDescription) desc).getInternalName());
										}

									}
								}
							}
						}

					} else { // must be AttributePage

						if (martconf.getDatasetByName(datasetName).containsAttributePage(pageName)) {
							if (martconf.getDatasetByName(datasetName).getAttributePageByName(pageName).containsAttributeGroup(groupName)) {
								Object group = martconf.getDatasetByName(datasetName).getAttributePageByName(pageName).getAttributeGroupByName(groupName);

								if (group instanceof AttributeGroup) {

									if (collectionKey.equals("AttributeCollection")) {
										currentSet = new TreeSet();

										AttributeCollection[] colls = ((AttributeGroup) group).getAttributeCollections();
										for (int i = 0, n = colls.length; i < n; i++) {
											AttributeCollection collection = colls[i];
											currentSet.add(collection.getInternalName());
										}
									} else { // must be attribute
										currentSet = new TreeSet();

										List descs = ((AttributeGroup) group).getAllUIAttributeDescriptions();
										for (int i = 0, n = descs.size(); i < n; i++) {
											Object desc = descs.get(i);

											if (desc instanceof AttributeDescription) {
												currentSet.add(((AttributeDescription) desc).getInternalName());
											} // else, if we add UIDSAttributeDescriptions, add code here

										}
									}

								}
							}
						}

					}
				}

			} else if (DescriptionStartMatcher.matches()) { //wants the potential description keys
				String pageKey = DescriptionStartMatcher.group(2);

				if (keyMap.containsKey(pageKey)) {
					Map groupKeyMap = (Map) keyMap.get(pageKey);
					String groupKey = DescriptionStartMatcher.group(4);

					if (groupKeyMap.containsKey(groupKey)) {
						Map collectionKeyMap = (Map) groupKeyMap.get(groupKey);
						String collectionKey = DescriptionStartMatcher.group(6);

						if (collectionKeyMap.containsKey(collectionKey)) {
							currentSet = new TreeSet();
							Map descriptionKeyMap = (Map) collectionKeyMap.get(collectionKey);
							currentSet.addAll(descriptionKeyMap.keySet());
						}
					}
				}

			} else if (Descriptionmatcher.matches()) {
				//wants the values for the given description key
				String datasetName = Descriptionmatcher.group(1);
				String pageKey = Descriptionmatcher.group(2);
				String pageName = Descriptionmatcher.group(3);
				String groupName = Descriptionmatcher.group(5);
				String collectionKey = Descriptionmatcher.group(6);
				String collectionName = Descriptionmatcher.group(7);

				if (martconf.containsDataset(datasetName)) {
					if (pageKey.equals("FilterPage")) {
						if (martconf.getDatasetByName(datasetName).containsFilterPage(pageName)) {
							if ((martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).containsFilterGroup(groupName))
								&& (martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).getFilterGroupByName(groupName) instanceof FilterGroup)) {
								FilterGroup group = (FilterGroup) martconf.getDatasetByName(datasetName).getFilterPageByName(pageName).getFilterGroupByName(groupName);

								if (collectionKey.equals("FilterSet")) {
									if (group.containsFilterSet(collectionName)) {
										currentSet = new TreeSet();

										FilterSetDescription[] descs = group.getFilterSetByName(collectionName).getFilterSetDescriptions();
										for (int i = 0, n = descs.length; i < n; i++) {
											FilterSetDescription description = descs[i];
											currentSet.add(description.getInternalName());
										}
									}
								} else {
									if (group.containsFilterCollection(collectionName)) {
										currentSet = new TreeSet();

										List descs = group.getFilterCollectionByName(collectionName).getUIFilterDescriptions();
										for (int i = 0, n = descs.size(); i < n; i++) {
											Object desc = descs.get(i);
											if (desc instanceof FilterDescription)
												currentSet.add(((FilterDescription) desc).getInternalName());
											else
												currentSet.add(((MapFilterDescription) desc).getInternalName());
										}
									}
								}
							}
						}
					} else {
						//must be AttributePage
						if (martconf.containsDataset(datasetName)) {
							if (martconf.getDatasetByName(datasetName).containsAttributePage(pageName)) {
								if ((martconf.getDatasetByName(datasetName).getAttributePageByName(pageName).containsAttributeGroup(groupName))
									&& (martconf.getDatasetByName(datasetName).getAttributePageByName(pageName).getAttributeGroupByName(groupName) instanceof AttributeGroup)) {
									AttributeGroup group =
										(AttributeGroup) martconf.getDatasetByName(datasetName).getAttributePageByName(pageName).getAttributeGroupByName(groupName);

									if (group.containsAttributeCollection(collectionName)) {
										currentSet = new TreeSet();

										List descs = group.getAttributeCollectionByName(collectionName).getUIAttributeDescriptions();
										for (int i = 0, n = descs.size(); i < n; i++) {
											Object desc = descs.get(i);
											if (desc instanceof AttributeDescription)
												currentSet.add(((AttributeDescription) desc).getInternalName());
											// else, if we add UIDSAttributeDescriptions, put them here

										}
									}
								}
							}
						}
					}
				}

			} // else ?
		}
	}

	/**
	 * Sets the MartCompleter into Help Mode.
	 *
	 */
	public void SetHelpMode() {
		currentSet = new TreeSet();
		currentSet.addAll((SortedSet) setMapper.get(HELP));
	}

	/**
	 * Sets the MartCompleter into COMMAND mode
	 *
	 */
	public void SetCommandMode() {
		currentSet = new TreeSet();
		currentSet.addAll((SortedSet) setMapper.get(COMMANDS));
		lastLocalDataset = null;
	}

	/**
	 * Sets the MartCompleter into Get Mode
	 *
	 */
	public void SetGetMode() {
		if (lastLocalDataset != null) {
			currentSet = new TreeSet();
			currentSet.addAll((SortedSet) setMapper.get(MartShellLib.GETQSTART)); // add any user defined values

			List attributes = lastLocalDataset.getAllUIAttributeDescriptions();
			for (int i = 0, n = attributes.size(); i < n; i++) {
				Object attribute = attributes.get(i);
				if (attribute instanceof AttributeDescription)
					currentSet.add(((AttributeDescription) attribute).getInternalName());
				//else, if we impliment UIDSAttributeDescription
			}
		} else if (envDataset != null) {
			currentSet = new TreeSet();
			currentSet.addAll((SortedSet) setMapper.get(MartShellLib.GETQSTART)); // add any user defined values

			List attributes = envDataset.getAllUIAttributeDescriptions();
			for (int i = 0, n = attributes.size(); i < n; i++) {
				Object attribute = attributes.get(i);
				if (attribute instanceof AttributeDescription)
					currentSet.add(((AttributeDescription) attribute).getInternalName());
				//else, if we impliment UIDSAttributeDescription
			}

		} else {
			currentSet = new TreeSet();
			currentSet.addAll(NODATASETWARNING);
		}
	}

	/**
	 * Sets the MartCompleter into Sequence Mode
	 *
	 */
	public void SetSequenceMode() {
		currentSet = new TreeSet();
		currentSet.addAll((SortedSet) setMapper.get(MartShellLib.QSEQUENCE));
	}

	/**
	 * Sets the MartCompleter into the Dataset Mode 
		*
		*/
	public void SetDatasetMode() {
		currentSet = new TreeSet();
		currentSet.addAll((SortedSet) setMapper.get(DATASETS));
	}

	/**
	 * Sets the MartCompleter into the Where Mode
	 *
	 */
	public void SetWhereMode() {

		if (lastLocalDataset != null) {
			currentSet = new TreeSet();
			currentSet.addAll((SortedSet) setMapper.get(MartShellLib.QWHERE)); // user defined names

			List filters = lastLocalDataset.getAllUIFilterDescriptions();
			for (int i = 0, n = filters.size(); i < n; i++) {
				Object filter = filters.get(i);

				if (filter instanceof MapFilterDescription)
					currentSet.add(((MapFilterDescription) filter).getInternalName());
				else
					currentSet.add(((FilterDescription) filter).getInternalName());
			}
		} else if (envDataset != null) {
			currentSet = new TreeSet();
			currentSet.addAll((SortedSet) setMapper.get(MartShellLib.QWHERE)); // user defined names

			List filters = envDataset.getAllUIFilterDescriptions();
			for (int i = 0, n = filters.size(); i < n; i++) {
				Object filter = filters.get(i);

				if (filter instanceof MapFilterDescription)
					currentSet.add(((MapFilterDescription) filter).getInternalName());
				else
					currentSet.add(((FilterDescription) filter).getInternalName());
			}
		} else
			currentSet.add(NODATASETWARNING);
	}

	/**
	 * Adds a Collection of String names to a specific list specified by a keyword.
	 * If the keyword is not available, a warning is sent to the logging system, and
	 * the Collection is ignored.
	 * @param key - one of the available keywords for a specific Mode.
	 * @param commands - Collection of names to make available in that Mode.
	 */
	public void AddAvailableCommandsTo(String key, Object commands) {
		if (setMapper.containsKey(key)) {
			SortedSet set = (SortedSet) setMapper.get(key);
			set.addAll((Collection) commands);
			setMapper.put(key, set);
		} else if (key.equals(DESCRIBE)) {
			setMapper.put(key, commands);
		} else {
			logger.warn("Key " + key + " is not a member of the command completion system\n");
		}
	}
}