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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.CompositeDSViewAdaptor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterPage;
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
 * <li><p>DatasetView Mode: keyword: "datasets".  Only dataset names, and other names added by the client for this mode, are made available.</p>
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
  private CompositeDSViewAdaptor adaptorManager = null;

  private SortedSet commandSet = new TreeSet(); // will hold basic shell commands
  private SortedSet domainSpecificSet = new TreeSet(); // will hold sequences
  private SortedSet helpSet = new TreeSet(); // will hold help keys available

  private SortedSet addBaseSet = new TreeSet(); // will hold add completions
  private SortedSet listSet = new TreeSet(); // will hold list request keys
  private SortedSet procSet = new TreeSet(); // will hold stored procedure names for remove, describe, and execute
  private SortedSet environmentSet = new TreeSet(); // will hold environment command completions

  private SortedSet removeBaseSet = new TreeSet(); // will hold remove base completions
  private SortedSet updateBaseSet = new TreeSet(); // will hold update remove base completions
  private SortedSet setBaseSet = new TreeSet(); // will hold set base completions
  private SortedSet describeBaseSet = new TreeSet(); // will hold describe base completions
  private SortedSet executeBaseSet = new TreeSet(); // will hold execute base completions

  private SortedSet martSet = new TreeSet(); // will hold  String names for remove, and set 
  private SortedSet adaptorLocationSet = new TreeSet(); // will hold adaptor names for update and remove
  private SortedSet datasetViewSet = new TreeSet(); // will hold DatasetView names for use, set, remove, and describe 

  private final List NODATASETWARNING =
    Collections.unmodifiableList(Arrays.asList(new String[] { "No DatasetViews loaded", "!" }));
  private final List NOENVWARNING =
    Collections.unmodifiableList(Arrays.asList(new String[] { "Please set environmenal Mart and Dataset", "!" }));
  private final List ERRORMODE = Collections.unmodifiableList(Arrays.asList(new String[] { "ERROR ENCOUNTERED" }));

  private final String ADDC = "add";
  private final String REMOVEC = "remove";
  private final String LISTC = "list";
  private final String UPDATEC = "update";
  private final String SETC = "set";
  private final String UNSETC = "unset";
  private final String DESCRIBEC = "describe";
  private final String HELPC = "help";
  private final String USEC = "use";
  private final String ENVC = "environment";
  private final String EXECC = "execute";

  private DatasetView envDataset = null;
  private DatasetView currentDataset = null;
  private boolean usingLocalDataset = false;

  private DetailedDataSource envMart = null;

  private List currentApages = new ArrayList();
  private List currentFpages = new ArrayList();
  private String lastFilterName = null;
  private String lastAttributeName = null;

  private boolean attributeMode = false;
  private boolean whereMode = false;
  private boolean whereNamesMode = false;
  private boolean whereQualifiersMode = false;
  private boolean whereValuesMode = false;

  //listlevel characters
  public int listLevel = 0;
  private String lastLine = null;

  private Logger logger = Logger.getLogger(MartCompleter.class.getName());

  /**
   * Creates a MartCompleter Object.  The MartCompleter processes the MartConfiguration
   * object, and stores important internal_names into the completion sets that are applicable to the given MartConfiguration object.
   * @param adaptorManager - a MartConfiguration Object
   */
  public MartCompleter(CompositeDSViewAdaptor adaptorManager) throws ConfigurationException {
    this.adaptorManager = adaptorManager;

    if (adaptorManager.getDatasetNames().length > 0) {
      DatasetView[] dsets = adaptorManager.getDatasetViews();
      for (int i = 0, n = dsets.length; i < n; i++) {
        DatasetView dataset = dsets[i];
        datasetViewSet.add(dataset.getInternalName());
      }
    }

    setCommandMode();
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
      setModeForLine(Readline.getLineBuffer());
      possibleValues = currentSet.tailSet(text).iterator();
    }

    if (possibleValues.hasNext()) {
      String nextKey = (String) possibleValues.next();
      if (nextKey.startsWith(text))
        return nextKey;
    }

    return null; // we reached the last choice.
  }

  public void setModeForLine(String currentCommand) {
    if (lastLine == null || !(lastLine.equals(currentCommand))) {
      if (currentCommand.startsWith(ADDC))
        setAddMode(currentCommand);
      else if (currentCommand.startsWith(REMOVEC))
        setRemoveMode(currentCommand);
      else if (currentCommand.startsWith(LISTC))
        setListMode(currentCommand);
      else if (currentCommand.startsWith(UPDATEC))
        setUpdateMode(currentCommand);
      else if (currentCommand.startsWith(SETC) || currentCommand.startsWith(UNSETC))
        setSetUnsetMode(currentCommand);
      else if (currentCommand.startsWith(DESCRIBEC))
        setDescribeMode(currentCommand);
      else if (currentCommand.startsWith(ENVC))
        setEnvironmentMode();
      else if (currentCommand.startsWith(EXECC))
        setExecuteMode(currentCommand);
      else if (currentCommand.startsWith(HELPC))
        setHelpMode();
      else if (currentCommand.startsWith(USEC)) {
        if (currentCommand.endsWith(">"))
          setMartReqMode();
        else
          setUseDatasetMode(currentCommand);
      } else {
        int usingInd = currentCommand.lastIndexOf(MartShellLib.USINGQSTART);

        if (usingInd >= 0) {
          usingLocalDataset = true;

          String[] toks = currentCommand.split("\\s+");

          //unset all modes if user has erased back to using
          if (toks.length < 3) {
            attributeMode = false;
            whereMode = false;
          }

          if (toks.length >= 2) {
            String datasetreq = toks[1];

            if (datasetreq.indexOf(">") > 0)
              datasetreq = datasetreq.substring(0, datasetreq.indexOf(">"));

            currentDataset = getCurrentDatasetFor(datasetreq);
          }
        }

        String[] lineWords = currentCommand.split(" "); // split on single space

        // determine which mode to be in during a query
        int getInd = currentCommand.lastIndexOf(MartShellLib.GETQSTART);
        int seqInd = currentCommand.lastIndexOf(MartShellLib.QSEQUENCE);
        int whereInd = currentCommand.lastIndexOf(MartShellLib.QWHERE);
        int limitInd = currentCommand.lastIndexOf(MartShellLib.QLIMIT);

        if ((usingInd > seqInd) && (usingInd > getInd) && (usingInd > whereInd) && (usingInd > limitInd)) {
          if (currentCommand.endsWith(">"))
            setMartReqMode();
          else
            setUseDatasetMode(currentCommand);
        }

        if ((seqInd > usingInd) && (seqInd > getInd) && (seqInd > whereInd) && (seqInd > limitInd))
          setDomainSpecificMode();

        if ((getInd > usingInd) && (getInd > seqInd) && (getInd > whereInd) && (getInd > limitInd))
          attributeMode = true;

        if ((whereInd > usingInd) && (whereInd > seqInd) && (whereInd > getInd) && (whereInd > limitInd)) {
          attributeMode = false;
          whereMode = true;
        }

        if ((limitInd > usingInd) && (limitInd > getInd) && (limitInd > seqInd) && (limitInd > whereInd)) {
          attributeMode = false;
          whereMode = false;
          setEmptyMode();
        }

        // if none of the key placeholders are present, may still need to further refine the mode
        if (attributeMode) {
          if (lineWords.length > 0) {
            String lastWord = lineWords[lineWords.length - 1];

            if (lastWord.equals(MartShellLib.GETQSTART)) {
              lastAttributeName = null;
              currentApages = new ArrayList();
            } else {
              if (lastWord.endsWith(",")) {
                lastAttributeName = lastWord.substring(0, lastWord.length() - 1);
                pruneAttributePages();
              }
            }

            setAttributeNames();
          }
        }

        if (whereMode) {
          if (!(whereNamesMode || whereQualifiersMode || whereValuesMode)) {
            //first time in
            whereNamesMode = true;
            whereQualifiersMode = false;
            whereValuesMode = true;
            currentFpages = new ArrayList();
            setWhereNames();
          }

          if (lineWords.length > 0) {
            String lastWord = lineWords[lineWords.length - 1];

            if (lastWord.equals(MartShellLib.QWHERE)) {
              lastFilterName = null;
              whereNamesMode = true;
              whereQualifiersMode = false;
              whereValuesMode = true;
              setWhereNames();
            } else if (MartShellLib.ALLQUALIFIERS.contains(lastWord)) {
              if (lineWords.length > 1) {
                lastFilterName = lineWords[lineWords.length - 2];
                pruneFilterPages();
              }

              if (MartShellLib.BOOLEANQUALIFIERS.contains(lastWord)) {
                whereNamesMode = false;
                whereQualifiersMode = true;
                whereValuesMode = false;
                setEmptyMode();
              } else {
                whereNamesMode = false;
                whereQualifiersMode = false;
                whereValuesMode = true;
                setWhereValues(lastWord);
              }
            }

            if (whereNamesMode) {
              if (currentDataset != null) {
                if (currentDataset.containsFilterDescription(lastWord)) {
                  String thisField = currentDataset.getFilterDescriptionByInternalName(lastWord).getField(lastWord);

                  if (thisField != null && thisField.length() > 0) {
                    lastFilterName = lastWord;
                    pruneFilterPages();

                    whereNamesMode = false;
                    whereQualifiersMode = true;
                    whereValuesMode = false;
                    setWhereQualifiers();
                  }
                }
              } else if (!usingLocalDataset && envDataset != null) {
                if (envDataset.containsFilterDescription(lastWord)) {
                  FilterDescription thisFilter = envDataset.getFilterDescriptionByInternalName(lastWord);
                  String thisField = thisFilter.getField(lastWord);

                  if (thisField != null && thisField.length() > 0) {
                    lastFilterName = lastWord;
                    pruneFilterPages();

                    logger.info(lastWord + " appears to be a filter, going to whereQualifiersMode\n");

                    whereNamesMode = false;
                    whereQualifiersMode = true;
                    whereValuesMode = false;
                    setWhereQualifiers();
                  }
                }
              } else
                setNoDatasetViewMode();
            } else if (whereQualifiersMode) {
              if (MartShellLib.ALLQUALIFIERS.contains(lastWord)) {
                if (lineWords.length > 1) {
                  lastFilterName = lineWords[lineWords.length - 2];
                  pruneFilterPages();
                }

                if (MartShellLib.BOOLEANQUALIFIERS.contains(lastWord)) {
                  setEmptyMode();
                } else {
                  whereQualifiersMode = false;
                  whereValuesMode = true;
                  setWhereValues(lastWord);
                }
              } else if (lastWord.equalsIgnoreCase(MartShellLib.FILTERDELIMITER)) {
                whereNamesMode = true;
                whereQualifiersMode = false;
                whereValuesMode = false;
                pruneFilterPages();
                setWhereNames();
              }
            } else if (whereValuesMode) {
              if (lastWord.equalsIgnoreCase(MartShellLib.FILTERDELIMITER)) {
                whereNamesMode = true;
                whereQualifiersMode = false;
                whereValuesMode = false;
                pruneFilterPages();
                setWhereNames();
              }
            }
          }
        }
      }

      lastLine = currentCommand;
    }
  }

  /**
   * Sets the Environment DatasetView for the session.  This dataset remains in effect
   * for the duration of the MartCompleter objects existence, and can only be over ridden
   * by a subsequent call to setEnvDataset, or a local dataset in the command
   * 
   * @param dataset - datasetview to set as environmental dataset
   */
  public void setEnvDataset(DatasetView dsv) {
    envDataset = dsv;
  }

  /**
   * Sets the Environment Mart for the session. This mart remains in effect
   * for the duration of the MartCompleter Objects existence, and can only be over ridden
   * by a subsequent call to setEnvMart, or a local mart in the comand
   * @param mart - DetailedDataSource environmental Mart
   */
  public void setEnvMart(DetailedDataSource mart) {
    envMart = mart;
  }

  /**
   * Sets the MartCompleter into COMMAND mode
   *
   */
  public void setCommandMode() {
    currentSet = new TreeSet();
    currentSet.addAll(commandSet);

    // reset state to pristine
    currentDataset = null;
    usingLocalDataset = false;
    currentApages = new ArrayList();
    currentFpages = new ArrayList();
    lastFilterName = null;
    lastAttributeName = null;
    attributeMode = false;
    whereMode = false;
    whereNamesMode = false;
    whereQualifiersMode = false;
    whereValuesMode = false;
    lastLine = null;
  }

  private DatasetView getCurrentDatasetFor(String name) {
    DatasetView ret = null;
    String[] toks = name.split("\\.");

    try {
      if (toks.length == 3) {
        //sourcename.datasetname.viewname

        if (!adaptorManager.supportsAdaptor(toks[0])) {
          setErrorMode("Sourcename " + toks[0] + " from datasetview request " + name + " is not a known source\n");
          return null;
        }

        DSViewAdaptor adaptor = adaptorManager.getAdaptorByName(toks[0]);

        if (!adaptor.supportsDataset(toks[1])) {
          setErrorMode(
            "Dataset "
              + toks[1]
              + " is not supported by sourcename "
              + toks[0]
              + " in datasetview request "
              + name
              + "\n");
          return null;
        }

        ret = adaptor.getDatasetViewByDatasetInternalName(toks[2], toks[3]);
      } else if (toks.length == 2) {
        //either sourcename.datasetname or datasetname.viewname relative to envMart
        if (adaptorManager.supportsAdaptor(toks[0])) {
          //assume it is sourcename.datasetname
          if (!adaptorManager.supportsAdaptor(toks[0])) {
            setErrorMode("Sourcename " + toks[0] + " from datasetview request " + name + " is not a known source\n");
            return null;
          }

          DSViewAdaptor adaptor = adaptorManager.getAdaptorByName(toks[0]);

          if (!adaptor.supportsDataset(toks[1])) {
            setErrorMode(
              "Dataset "
                + toks[1]
                + " is not supported by sourcename "
                + toks[0]
                + " in datasetview request "
                + name
                + "\n");
            return null;
          }

          ret = adaptor.getDatasetViewByDatasetInternalName(toks[1], MartShellLib.DEFAULTDATASETVIEWNAME);
        } else {
          //assume it is datasetname.viewname relative to envMart
          if (envMart == null) {
            setErrorMode("Must set environmental Mart to manipulate DatasetViews with relative name " + name + "\n");
            return null;
          }

          DSViewAdaptor adaptor = adaptorManager.getAdaptorByName(envMart.getName());
          ret = adaptor.getDatasetViewByDatasetInternalName(toks[0], toks[1]);
        }
      } else if (toks.length == 1) {
        if (envMart == null) {
          setErrorMode("Must set environmental Mart to manipulate DatasetViews with relative name " + name + "\n");
          return null;
        }

        DSViewAdaptor adaptor = adaptorManager.getAdaptorByName(envMart.getName());

        //either datasetname relative to envMart or viewname relative to envMart.envDataset
        if (adaptorManager.supportsDataset(toks[0])) {
          //assume it is datasetname relative to envMart
          ret = adaptor.getDatasetViewByDatasetInternalName(toks[0], MartShellLib.DEFAULTDATASETVIEWNAME);
        } else {
          //assume it is viewname relative to envMart and envDataset
          if (envDataset == null) {
            setErrorMode("Must set environmental Dataset to manipulate DatasetViews with relative name " + name + "\n");
            return null;
          }

          ret = adaptor.getDatasetViewByDatasetInternalName(envDataset.getDataset(), toks[0]);
        }
      }
    } catch (Exception e) {
      setErrorMode("Caught Exception manipulating DatasetView named " + name + " " + e.getMessage() + "\n");
      return null;
    }

    //may be null if not found
    return ret;
  }

  private void setHelpMode() {
    currentSet = new TreeSet();
    currentSet.addAll(helpSet);
  }

  private void setListMode(String token) {
    String[] toks = token.split("\\s+");

    if (toks.length <= 1) {
      setListBaseMode();
    } else {
      if (toks[1].equalsIgnoreCase("datasets") || toks[1].equalsIgnoreCase("datasetviews"))
        setAllAdaptorLocationMode();
    }
  }

  private void setListBaseMode() {
    currentSet = new TreeSet();
    currentSet.addAll(listSet);
  }

  private void setAddMode(String token) {
    String[] toks = token.split("\\s+");

    if (toks.length == 1)
      setAddBaseMode();
    else {
      if (toks[1].equalsIgnoreCase("Datasets")) {
        if (toks.length == 3 && toks[2].equalsIgnoreCase("from"))
          setEmptyMode();
        else
          setFromMode();
      } else {
        if (addBaseSet.contains(toks[1]))
          setEmptyMode();
      }
    }
  }

  private void setAddBaseMode() {
    currentSet = new TreeSet();
    currentSet.addAll(addBaseSet);
  }

  private void setEnvironmentMode() {
    currentSet = new TreeSet();
    currentSet.addAll(environmentSet);
  }

  private void setRemoveMode(String token) {
    String[] toks = token.split("\\s+");

    if (toks.length == 1)
      setRemoveBaseMode();
    else if (toks.length == 2) {
      String request = toks[1];

      if (request.equalsIgnoreCase("Mart"))
        setMartReqMode();
      else if (request.equalsIgnoreCase("Datasets"))
        setFromMode();
      else if (request.equalsIgnoreCase("Dataset"))
        setDatasetReqMode();
      else if (request.equalsIgnoreCase("DatasetView"))
        setDatasetViewReqMode();
      else if (request.equalsIgnoreCase("Procedure"))
        setProcedureNameMode();
    } else {
      if (toks.length == 3 && toks[2].equalsIgnoreCase("from"))
        setMartReqMode();
    }
  }

  private void setRemoveBaseMode() {
    currentSet = new TreeSet();
    currentSet.addAll(removeBaseSet);
  }

  private void setUpdateMode(String token) {
    String[] toks = token.split("\\s+");

    if (toks.length == 1)
      setUpdateBaseMode();
    else if (toks.length == 2) {
      String request = toks[1];

      if (request.equalsIgnoreCase("Datasets"))
        setFromMode();
      else {
        if (request.equalsIgnoreCase("Dataset"))
          setDatasetReqMode();
      }
    } else {
      if (toks.length == 3 && toks[2].equalsIgnoreCase("from"))
        setMartReqMode();
    }
  }

  private void setUpdateBaseMode() {
    currentSet = new TreeSet();
    currentSet.addAll(updateBaseSet);
  }

  private void setSetUnsetMode(String token) {
    String[] toks = token.split("\\s+");

    if (toks.length <= 1)
      setSetBaseMode();
    else {
      try {
        if (toks[0].equals(SETC)) {
          if (toks[1].equalsIgnoreCase("mart")) {
            if (toks.length == 3 && adaptorManager.supportsAdaptor(toks[2]))
              setEmptyMode();
            else
              setMartReqMode();
          } else if (toks[1].equalsIgnoreCase("dataset")) {
            if (toks.length == 3 && adaptorManager.supportsDataset( toks[2] ))
              setEmptyMode();
            else
              setDatasetReqMode();
          } else if (toks.length < 3 && setBaseSet.contains(toks[1]))
            setEmptyMode();
        } else {
          if (toks.length == 2 && setBaseSet.contains(toks[1]))
            setEmptyMode();
        }
      } catch (ConfigurationException e) {
        setErrorMode("Caught ConfigurationException updating Completion System\n");
      }
    }
  }

  private void setSetBaseMode() {
    currentSet = new TreeSet();
    currentSet.addAll(setBaseSet);
  }

  private void setExecuteMode(String token) {
    String[] toks = token.split("\\s+");

    if (toks.length == 1)
      setExecuteBaseMode();
    else if (toks.length == 2) {
      if (toks[1].equalsIgnoreCase("Procedure"))
        setProcedureNameMode();
      else {
        if (executeBaseSet.contains(toks[1]))
          setEmptyMode();
      }
    }
  }

  private void setExecuteBaseMode() {
    currentSet = new TreeSet();
    currentSet.addAll(executeBaseSet);
  }

  private void setDescribeMode(String token) {
    String[] toks = token.split("\\s+");
    if (toks.length == 1)
      setBaseDescribeMode();
    else if (toks.length >= 2) {
      String request = toks[1];

      try {
        if (request.equalsIgnoreCase("dataset")) {
          if (toks.length == 3 && adaptorManager.supportsDataset(toks[2]))
            setEmptyMode();
          else
            setDatasetReqMode();
        } else if (request.equalsIgnoreCase("Mart")) {
          if (toks.length == 3 && adaptorManager.supportsAdaptor(toks[2]))
            setEmptyMode();
          else
            setMartReqMode();
        } else if (request.equalsIgnoreCase("filter"))
          setDescribeFilterMode();
        else if (request.equalsIgnoreCase("attribute"))
          setDescribeAttributeMode();
        else {
          if (request.equalsIgnoreCase("procedure")) {
            setDescribeProcedureMode();
          }
        }
      } catch (ConfigurationException e) {
        setErrorMode("Caught ConfigurationException updating Completion System\n");
      }
    }
  }

  private void setBaseDescribeMode() {
    currentSet = new TreeSet();
    currentSet.addAll(describeBaseSet);
  }

  private void setDatasetReqMode() {
    currentSet = new TreeSet();
    if (envMart == null)
      setNoEnvMode();
    else
      setDatasetReqMode(envMart.getName());
  }

  private void setDatasetReqMode(String martName) {
    try {
      if (adaptorManager.supportsAdaptor(martName))
        currentSet.addAll(Arrays.asList(adaptorManager.getAdaptorByName(martName).getDatasetNames()));
    } catch (ConfigurationException e) {
      currentSet = new TreeSet();
      if (logger.isLoggable(Level.INFO))
        logger.info("Couldng set describe dataset mode, caught Configuration Exception: " + e.getMessage() + "\n");
    }
  }

  private void setDatasetViewReqMode() {
    currentSet = new TreeSet();
    if (envMart == null)
      setNoEnvMode();
    else {
      if (envDataset == null)
        setNoEnvMode();
      else
        setDatasetViewReqMode(envMart.getName(), envDataset.getDataset());
    }
  }

  private void setDatasetViewReqMode(String martName, String datasetName) {
    try {
      if (adaptorManager.supportsAdaptor(martName)
        && adaptorManager.getAdaptorByName(martName).supportsDataset(datasetName))
        currentSet.addAll(
          Arrays.asList(adaptorManager.getAdaptorByName(martName).getDatasetViewInternalNamesByDataset(datasetName)));
    } catch (ConfigurationException e) {
      currentSet = new TreeSet();
      if (logger.isLoggable(Level.INFO))
        logger.info("Couldng set dataset view req mode, caught Configuration Exception: " + e.getMessage() + "\n");
    }
  }

  private void setDescribeFilterMode() {
    if (envDataset == null)
      setNoEnvMode();
    else {
      currentSet = new TreeSet();
      currentSet.addAll(envDataset.getFilterCompleterNames());
    }
  }

  private void setDescribeAttributeMode() {
    if (envDataset == null)
      setNoEnvMode();
    else {
      currentSet = new TreeSet();
      currentSet.addAll(envDataset.getAttributeCompleterNames());
    }
  }

  private void setDescribeProcedureMode() {
    currentSet = new TreeSet();
    currentSet.addAll(procSet);
  }

  private void setAttributeNames() {
    if (currentDataset != null) {
      currentSet = new TreeSet();

      if (currentApages.size() == 0)
        currentApages = Arrays.asList(currentDataset.getAttributePages());

      for (int i = 0, n = currentApages.size(); i < n; i++) {
        AttributePage apage = (AttributePage) currentApages.get(i);

        List completers = apage.getCompleterNames();
        for (int j = 0, m = completers.size(); j < m; j++) {
          String completer = (String) completers.get(j);
          if (!currentSet.contains(completer))
            currentSet.add(completer);
        }
      }
    } else if (!(usingLocalDataset) && envDataset != null) {
      currentSet = new TreeSet();

      if (currentApages.size() == 0)
        currentApages = Arrays.asList(envDataset.getAttributePages());
      for (int i = 0, n = currentApages.size(); i < n; i++) {
        AttributePage apage = (AttributePage) currentApages.get(i);

        List completers = apage.getCompleterNames();
        for (int j = 0, m = completers.size(); j < m; j++) {
          String completer = (String) completers.get(j);
          if (!currentSet.contains(completer))
            currentSet.add(completer);
        }
      }
    } else
      setNoDatasetViewMode();
  }

  private void setFromMode() {
    currentSet = new TreeSet();
    currentSet.add("from");
  }

  private void setEmptyMode() {
    currentSet = new TreeSet();
  }

  private void setNoDatasetViewMode() {
    currentSet = new TreeSet();
    currentSet.addAll(NODATASETWARNING);
  }

  private void setNoEnvMode() {
    currentSet = new TreeSet();
    currentSet.addAll(NOENVWARNING);
  }

  private void setErrorMode(String error) {
    currentSet = new TreeSet();
    currentSet.addAll(ERRORMODE);
    currentSet.add(error);
  }

  private void pruneAttributePages() {
    List newPages = new ArrayList();
    if (currentDataset != null)
      newPages = currentDataset.getPagesForAttribute(lastAttributeName);
    else if (!usingLocalDataset && envDataset != null)
      newPages = envDataset.getPagesForAttribute(lastAttributeName);
    else
      newPages = new ArrayList();

    if (newPages.size() < currentApages.size())
      currentApages = new ArrayList(newPages);
  }

  private void setDomainSpecificMode() {
    attributeMode = false;
    currentSet = new TreeSet();
    currentSet.addAll(domainSpecificSet);
  }

  private void setAllAdaptorLocationMode() {
    currentSet = new TreeSet();
    currentSet.add("all");
    try {
      currentSet.addAll(Arrays.asList(adaptorManager.getAdaptorNames()));
    } catch (ConfigurationException e) {
      if (logger.isLoggable(Level.INFO))
        logger.info("Caught ConfigurationException getting adaptor names: " + e.getMessage() + "\n");
    }
  }

  private void setMartReqMode() {
    currentSet = new TreeSet();
    currentSet.addAll(martSet);
  }

  private void setUseDatasetMode(String token) {
    String[] toks = token.split("\\s+");

    try {
      if (toks.length <= 1) {
        if (envMart == null)
          setNoEnvMode();
        else
          setDatasetReqMode();
      } else {
        if (toks[1].indexOf(".") > -1) {
          String[] dsetreq = toks[1].split("\\.");

          if (dsetreq.length == 1) {
            setDatasetReqMode(dsetreq[0]);
          } else if (dsetreq.length == 2) {
            setDatasetViewReqMode(dsetreq[0], dsetreq[1]);
          }
        } else if (adaptorManager.supportsDataset(toks[1]))
          setEmptyMode();
      }
    } catch (ConfigurationException e) {
      setErrorMode("Caught ConfigurationException updating the Completion System\n");
    }
  }

  private void setProcedureNameMode() {
    currentSet = new TreeSet();
    currentSet.addAll(procSet);
  }

  private void setWhereNames() {
    if (currentDataset != null) {
      currentSet = new TreeSet();

      if (currentFpages.size() == 0)
        currentFpages = Arrays.asList(currentDataset.getFilterPages());

      for (int i = 0, n = currentFpages.size(); i < n; i++) {
        FilterPage fpage = (FilterPage) currentFpages.get(i);

        List completers = fpage.getCompleterNames();
        for (int j = 0, m = completers.size(); j < m; j++) {
          String completer = (String) completers.get(j);
          if (!currentSet.contains(completer))
            currentSet.add(completer);
        }
      }
    } else if (!(usingLocalDataset) && envDataset != null) {
      currentSet = new TreeSet();

      if (currentFpages.size() == 0)
        currentFpages = Arrays.asList(envDataset.getFilterPages());

      for (int i = 0, n = currentFpages.size(); i < n; i++) {
        FilterPage fpage = (FilterPage) currentFpages.get(i);

        List completers = fpage.getCompleterNames();
        for (int j = 0, m = completers.size(); j < m; j++) {
          String completer = (String) completers.get(j);
          if (!currentSet.contains(completer))
            currentSet.add(completer);
        }
      }
    } else
      setNoDatasetViewMode();
  }

  private void setWhereQualifiers() {
    currentSet = new TreeSet();

    if (currentDataset != null) {
      if (currentDataset.containsFilterDescription(lastFilterName))
        currentSet.addAll(currentDataset.getFilterCompleterQualifiersByInternalName(lastFilterName));
    } else if (!usingLocalDataset && envDataset != null) {
      if (logger.isLoggable(Level.INFO))
        logger.info("getting qualifiers for filter " + lastFilterName + "\n");

      if (envDataset.containsFilterDescription(lastFilterName)) {
        if (logger.isLoggable(Level.INFO))
          logger.info("Its a filter, getting from dataset\n");

        currentSet.addAll(envDataset.getFilterCompleterQualifiersByInternalName(lastFilterName));
      }
    } else
      setNoDatasetViewMode();
  }

  private void setWhereValues(String lastWord) {
    currentSet = new TreeSet();

    if (currentDataset != null) {
      if (currentDataset.containsFilterDescription(lastFilterName)) {
        currentSet.addAll(currentDataset.getFilterCompleterValuesByInternalName(lastFilterName));

        if (lastWord.equalsIgnoreCase("in"))
          currentSet.addAll(procSet);
      }
    } else if (!usingLocalDataset && envDataset != null) {
      if (envDataset.containsFilterDescription(lastFilterName)) {
        currentSet.addAll(envDataset.getFilterCompleterValuesByInternalName(lastFilterName));

        if (lastWord.equalsIgnoreCase("in"))
          currentSet.addAll(procSet);
      }
    } else
      setNoDatasetViewMode();
  }

  private void pruneFilterPages() {
    List newPages = new ArrayList();

    if (currentDataset != null)
      newPages = currentDataset.getPagesForFilter(lastFilterName);
    else if (!usingLocalDataset && envDataset != null)
      newPages = envDataset.getPagesForFilter(lastFilterName);
    else
      newPages = new ArrayList();

    if (newPages.size() < currentFpages.size())
      currentFpages = new ArrayList(newPages);
  }

  /**
   * Set the String names (path, url, Mart name) used to refer
   * to locations from whence DSViewAdaptor objects were loaded.
   * @param names -- names of path, url, or Mart names from whence DSViewAdaptor Objects were loaded 
   */
  public void setAdaptorLocations(Collection names) {
    adaptorLocationSet = new TreeSet();
    adaptorLocationSet.addAll(names);
  }

  public void setDatasetViewInternalNames(Collection names) {
    datasetViewSet = new TreeSet();
    datasetViewSet.addAll(names);
  }

  /**
   * Set the Names used to refer to Mart Objects in the Shell.
   * @param names -- names used to refer to Mart Objects in the Shell.
   */
  public void setMartNames(Collection names) {
    martSet = new TreeSet();
    martSet.addAll(names);
  }

  /**
   * Set the names of stored procedures
   * @param names -- names of stored procedures
   */
  public void setProcedureNames(Collection names) {
    procSet = new TreeSet();
    procSet.addAll(names);
  }

  /**
   * Set the Base Shell Commands available
   * @param names -- base commands available to the Shell.
   */
  public void setBaseCommands(Collection names) {
    commandSet = new TreeSet();
    commandSet.addAll(names);
  }

  /**
   * Set domain Specific Commands
   * @param names -- domain specific commands
   */
  public void setDomainSpecificCommands(Collection names) {
    domainSpecificSet = new TreeSet();
    domainSpecificSet.addAll(names);
  }

  /**
   * set Help commands
   * @param names -- help commands
   */
  public void setHelpCommands(Collection names) {
    helpSet = new TreeSet();
    helpSet.addAll(names);
  }

  /**
   * Set the add command sub tokens
   * @param addRequests -- add command sub tokens
   */
  public void setAddCommands(Collection requests) {
    addBaseSet = new TreeSet();
    addBaseSet.addAll(requests);
  }

  /**
   * Set the Base Remove sub tokens
   * @param removeRequests -- base remove sub tokens
   */
  public void setRemoveBaseCommands(Collection requests) {
    removeBaseSet = new TreeSet();
    removeBaseSet.addAll(requests);
  }

  /**
   * Set the List command sub tokens
   * @param listRequests -- list command sub tokens
   */
  public void setListCommands(Collection requests) {
    listSet = new TreeSet();
    listSet.addAll(requests);
  }

  /**
   * Set the Update command base sub tokens
   * @param updateRequests -- update command base sub tokens
   */
  public void setUpdateBaseCommands(Collection requests) {
    updateBaseSet = new TreeSet();
    updateBaseSet.addAll(requests);
  }

  /**
   * Set the Set command base sub tokens
   * @param requests -- Set command base sub tokens
   */
  public void setSetBaseCommands(Collection requests) {
    setBaseSet = new TreeSet();
    setBaseSet.addAll(requests);
  }

  /**
   * Set the Describe command base sub tokens
   * @param describeRequests -- describe command base sub tokens
   */
  public void setDescribeBaseCommands(Collection requests) {
    describeBaseSet = new TreeSet();
    describeBaseSet.addAll(requests);
  }

  /**
   * Set the Environment command base sub tokens
   * @param envRequests -- environment command base sub tokens
   */
  public void setEnvironmentBaseCommands(Collection requests) {
    environmentSet = new TreeSet();
    environmentSet.addAll(requests);
  }

  /**
   * Sets the Execute command base sub tokens
   * @param executeRequests -- execute command base sub tokens
   */
  public void setExecuteBaseCommands(Collection requests) {
    executeBaseSet = new TreeSet();
    executeBaseSet.addAll(requests);
  }
}