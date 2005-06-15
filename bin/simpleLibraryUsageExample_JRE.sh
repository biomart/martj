#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartExplorer GUI application.

# Usage:
#
# prompt> bin/martgui.sh
TMP_ROOT=`dirname $0`/..
 
TMP_CLASSPATH=${TMP_ROOT}
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/build/classes
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mysql-connector-java-3.0.16-ga-bin.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/log4j-1.2.6.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jython.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ensj-util.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jdom.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/martj.jar 
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/p6spy.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ojdbc14.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ecp1_0beta.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${CLASSPATH}

TMP_JYTHON_LIB=${TMP_ROOT}/lib

PLATFORM=`uname -ms`
case "$PLATFORM" in
[Ll]inux*)
  JAVA="${TMP_ROOT}/jre/linux/bin/java"
  ;;
*alpha*)
  JAVA="${TMP_ROOT}/jre/alpha/bin/java"
  ;;
*)
  echo "warning, this platform is not known to be supported, using linux jre\n"
  JAVA="${TMP_ROOT}/jre/linux/bin/java"
  ;;
esac

$JAVA -cp $TMP_CLASSPATH org.ensembl.mart.example.SimpleLibraryUsageExample
