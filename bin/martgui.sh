#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartExplorer GUI application.

# Usage:
#
# prompt> bin/martgui.sh
TMP_ROOT=`dirname $0`/..

if [ -s ${TMP_ROOT}/src/jython/martexplorer/MartExplorerGUIApplication.py ] 
then
  CACHE_DIR=${HOME}/.martshell_cachedir
 
  TMP_ROOT=`dirname $0`/..
 
  TMP_CLASSPATH=${TMP_ROOT}/build/classes
  TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mart-explorer.jar
  TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mysql-connector-java-3.0.7-stable-bin.jar
  TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/log4j-1.2.6.jar
  TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jython.jar
  TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/junit.jar
 
  TMP_JYTHON_LIB=${TMP_ROOT}/lib
 
  CMD="java -classpath ${TMP_CLASSPATH} -Dpython.cachedir=${CACHE_DIR} -Dpython.path=${TMP_JYTHON_LIB} org.python.util.jython ${TMP_ROOT}/src/jython/martexplorer/MartExplorerGUIApplication.py"
 
  #echo $CMD
  $CMD
else 
  java -jar ${TMP_ROOT}/lib/mart-explorer.jar $@
fi
