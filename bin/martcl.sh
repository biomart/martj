#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartExplorer application.

# Usage:
#
# prompt> bin/martexplorer.sh

TMP_ROOT=`dirname $0`/..

TMP_CLASSPATH=${TMP_ROOT}/build/classes
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mart-explorer.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mysql-connector-java-2.0.14-bin.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/log4j-1.2.6.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/java-getopt-1.0.9.jar

java -classpath ${TMP_CLASSPATH} org.ensembl.mart.explorer.MartExplorerTool $@