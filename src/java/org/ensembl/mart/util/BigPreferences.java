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
package org.ensembl.mart.util;

import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/** 
* @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
* @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
*/
public class BigPreferences extends Preferences {

  private Logger logger = Logger.getLogger(BigPreferences.class.getName());
  private Preferences subPrefs = null;
  private final String HAS_MORE_NODES = "hasMoreNodes";
  private final String NEXT_NODE = "nextNode";
  
//max length of a byte stored into a preferences putByteArray is 3/4 MAX_VALUE_LENGTH
  private final int MAX_BYTE_LENGTH = (3 * MAX_VALUE_LENGTH ) / 4;
  
  
  /**
   * Returns a BigPreferences wrapped around Preferences.userNodeForPackage(c);
   * @return Preferences object which is a BigPreferences wrapped around Preferences.userNodeForPackage(c)
   */
  public static Preferences userNodeForPackage(Class c) {
    return new BigPreferences(Preferences.userNodeForPackage(c));
  }

  /**
   * Returns a BigPreferences wrapped around Preferences.systemNodeForPackage(c);
   * @return Preferences object which is a BigPreferences wrapped around Preferences.systemNodeForPackage(c)
   */
  public static Preferences systemNodeForPackage(Class c) {
    return new BigPreferences(Preferences.systemNodeForPackage(c));
  }

  private BigPreferences(Preferences prefs) {
    super();
    subPrefs = prefs;
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#put(java.lang.String, java.lang.String)
   */
  public void put(String key, String value) {
    subPrefs.put(key, value);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#get(java.lang.String, java.lang.String)
   */
  public String get(String key, String def) {
    return subPrefs.get(key, def);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#remove(java.lang.String)
   */
  public void remove(String key) {
    subPrefs.remove(key);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#clear()
   */
  public void clear() throws BackingStoreException {
    subPrefs.clear();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#putInt(java.lang.String, int)
   */
  public void putInt(String key, int value) {
    subPrefs.putInt(key, value);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#getInt(java.lang.String, int)
   */
  public int getInt(String key, int def) {
    return subPrefs.getInt(key, def);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#putLong(java.lang.String, long)
   */
  public void putLong(String key, long value) {
    subPrefs.putLong(key, value);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#getLong(java.lang.String, long)
   */
  public long getLong(String key, long def) {
    return subPrefs.getLong(key, def);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#putBoolean(java.lang.String, boolean)
   */
  public void putBoolean(String key, boolean value) {
    subPrefs.putBoolean(key, value);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#getBoolean(java.lang.String, boolean)
   */
  public boolean getBoolean(String key, boolean def) {
    return subPrefs.getBoolean(key, def);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#putFloat(java.lang.String, float)
   */
  public void putFloat(String key, float value) {
    subPrefs.putFloat(key, value);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#getFloat(java.lang.String, float)
   */
  public float getFloat(String key, float def) {
    return subPrefs.getFloat(key, def);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#putDouble(java.lang.String, double)
   */
  public void putDouble(String key, double value) {
    subPrefs.putDouble(key, value);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#getDouble(java.lang.String, double)
   */
  public double getDouble(String key, double def) {
    return subPrefs.getDouble(key, def);
  }

  /**
   * Allows users to store a byte array that might be bigger than
   * the underlying preferences MAX_VALUE_LENGTH
   */
  public void putByteArray(String key, byte[] value) {
    byte[] curBytes = new byte[MAX_BYTE_LENGTH];
    int byteIter = 0;
    int listIter = 0;
    Preferences curNode = subPrefs;
    curNode.remove(HAS_MORE_NODES);
    curNode.remove(NEXT_NODE);
    try {
     if (curNode.nodeExists(String.valueOf(listIter)))
       curNode.node(String.valueOf(listIter)).removeNode();
    } catch (BackingStoreException e) {
      if (logger.isLoggable(Level.INFO))
        logger.info("Could not clear first node of BigPreferences byte array\n" + e.getMessage() + "\n");
    }

    //there is a maximum length of bytes that one can store with one key in a preferences object
    //must store multiple parts
    //impliment a linked list underneath
    for (int i = 0, n = value.length; i < n; i++) {

      if (i > 0 && i % MAX_BYTE_LENGTH == 0) {

        //store a link in the list
        if (listIter > 0) {
          curNode.putBoolean(HAS_MORE_NODES, true);
          curNode.putInt(NEXT_NODE, listIter);
          curNode = curNode.node(String.valueOf(listIter));
        }

        curNode.putByteArray(key, curBytes);
        curBytes = new byte[MAX_BYTE_LENGTH];
        listIter++;
        byteIter = 0;
      }

      curBytes[byteIter] = value[i];
      byteIter++;
    }
    
    if (byteIter > 0) {
      if (listIter > 0) {
        curNode.putBoolean(HAS_MORE_NODES, true);
        curNode.putInt(NEXT_NODE, listIter);
        curNode = curNode.node(String.valueOf(listIter));
      } else
        curNode.putBoolean(HAS_MORE_NODES, false);
      
      byte[] lastBytes = new byte[byteIter];
      for (int i = 0; i < byteIter; i++) {
        lastBytes[i] = curBytes[i];
      }
      curNode.putBoolean(HAS_MORE_NODES, false);
      curNode.putByteArray(key, lastBytes);
    }
  }

  /**
   * Will return a single byte[], even if the underlying byte[] is bigger
   * than the underlying preferences MAX_VALUE_LENGTH
   */
  public byte[] getByteArray(String key, byte[] def) {
    List bytes = new ArrayList();
    int totalLength = 0;
    
    //loop over linkedlist
    Preferences curNode = subPrefs;
    byte[] curBytes = curNode.getByteArray(key, null);
    if (curBytes == null) {
      if (logger.isLoggable(Level.INFO))
        logger.info("Did not get expected bytes from curNode, total bytes returned could be truncated\n"); 
    } else {
      bytes.add(curBytes);
      totalLength += curBytes.length;
    }
    
    //This could silently truncate the returned bytes if the backingstore
    //goes away, causing the getBoolean default value to be returned rather
    //than the actual true value.  Unfortunately, getBoolean(key, null) does not work.
    while (curNode.getBoolean(HAS_MORE_NODES, false)) {
      int nextNode = curNode.getInt(NEXT_NODE, 0);
      if (nextNode < 1) {
        if (logger.isLoggable(Level.INFO))
          logger.info("Did not get an expected nextNode value from curNode, total bytes returned could be trucated\n");
      } else {
        curNode = curNode.node(String.valueOf(nextNode));
        curBytes = curNode.getByteArray(key, null);
      
        if (curBytes == null) {
          if (logger.isLoggable(Level.INFO))
            logger.info("Did not get expected bytes from curNode, total bytes returned could be truncated\n"); 
        } else {
          bytes.add(curBytes);
          totalLength += curBytes.length;
        }
      }  
    }
 
    byte[] retBytes = new byte[totalLength];
    int nextPos = 0;
    for (int i = 0, n = bytes.size(); i < n; i++) {
      byte[] thisChunk = (byte[]) bytes.get(i);
      System.arraycopy(thisChunk, 0, retBytes, nextPos, thisChunk.length);
      nextPos += thisChunk.length;
    }
    
    bytes = null;
    return retBytes;
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#keys()
   */
  public String[] keys() throws BackingStoreException {
    return subPrefs.keys();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#childrenNames()
   */
  public String[] childrenNames() throws BackingStoreException {
    return subPrefs.childrenNames();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#parent()
   */
  public Preferences parent() {
    return subPrefs.parent();
  }

  /**
   * The Preferences object that is returned is actually a BigPreferences object
   */
  public Preferences node(String pathName) {
    return new BigPreferences(subPrefs.node(pathName));
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#nodeExists(java.lang.String)
   */
  public boolean nodeExists(String pathName) throws BackingStoreException {
    return subPrefs.nodeExists(pathName);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#removeNode()
   */
  public void removeNode() throws BackingStoreException {
    subPrefs.removeNode();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#name()
   */
  public String name() {
    return subPrefs.name();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#absolutePath()
   */
  public String absolutePath() {
    return subPrefs.absolutePath();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#isUserNode()
   */
  public boolean isUserNode() {
    return subPrefs.isUserNode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return subPrefs.toString();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#flush()
   */
  public void flush() throws BackingStoreException {
    subPrefs.flush();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#sync()
   */
  public void sync() throws BackingStoreException {
    subPrefs.sync();
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#addPreferenceChangeListener(java.util.prefs.PreferenceChangeListener)
   */
  public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
    subPrefs.addPreferenceChangeListener(pcl);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#removePreferenceChangeListener(java.util.prefs.PreferenceChangeListener)
   */
  public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
    subPrefs.removePreferenceChangeListener(pcl);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#addNodeChangeListener(java.util.prefs.NodeChangeListener)
   */
  public void addNodeChangeListener(NodeChangeListener ncl) {
    subPrefs.addNodeChangeListener(ncl);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#removeNodeChangeListener(java.util.prefs.NodeChangeListener)
   */
  public void removeNodeChangeListener(NodeChangeListener ncl) {
    subPrefs.removeNodeChangeListener(ncl);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#exportNode(java.io.OutputStream)
   */
  public void exportNode(OutputStream os) throws IOException, BackingStoreException {
    subPrefs.exportNode(os);
  }

  /* (non-Javadoc)
   * @see java.util.prefs.Preferences#exportSubtree(java.io.OutputStream)
   */
  public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
    subPrefs.exportSubtree(os);
  }
}
