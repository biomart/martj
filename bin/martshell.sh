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
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/junit.jar

CMD="java -classpath ${TMP_CLASSPATH} -Dpython.cachedir=${CACHE_DIR} $@ org.python.util.jython -i ${TMP_ROOT}/martshell/initshell.py"

#echo $CMD
$CMD