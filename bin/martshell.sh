#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the interactive Mart Shell program.

# Usage:
#
# prompt> bin/martshell.sh

# unless python.cachedir is set in ~/.jython a cachedir will be
# automatically created. The cache speeds up the shell. It can be
# safely deleted but will be automatically recreated next time you run
# this program.

CACHE_DIR=${HOME}/.martshell_cachedir

TMP_ROOT=`dirname $0`/..

TMP_CLASSPATH=${TMP_ROOT}/build/classes 
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mart-explorer.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mysql-connector-java-3.0.7-stable-bin.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/log4j-1.2.6.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jython.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/java-getopt-1.0.9.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jdom.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/libreadline-java.jar

LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${TMP_ROOT}/lib" java -Xmx200m -classpath ${TMP_CLASSPATH} org.ensembl.mart.shell.MartShell $@