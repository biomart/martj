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

TMP_CLASSPATH=${TMP_ROOT}
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/build/classes 
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/martj.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mysql-connector-java-3.0.7-stable-bin.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/java-getopt-1.0.9.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jdom.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/libreadline-java.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ensj-util.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ecp1_0beta.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jdbc2_0-stdext.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/p6spy.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ojdbc14.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${CLASSPATH}

TMP_LD_LIBPATH=${LD_LIBRARY_PATH}

PLATFORM=`uname -ms`
case "$PLATFORM" in
[Ll]inux*)
  TMP_LD_LIBPATH="${TMP_LD_LIBPATH}:${TMP_ROOT}/lib/linux"
  LD_LIBRARY_PATH=$TMP_LD_LIBPATH java -ea -classpath ${TMP_CLASSPATH} org.ensembl.mart.shell.MartShell $@
  ;;
*alpha*)
  TMP_LD_LIBPATH="${TMP_LD_LIBPATH}:${TMP_ROOT}/lib/alpha"
  LD_LIBRARY_PATH=$TMP_LD_LIBPATH java -ea -classpath ${TMP_CLASSPATH} org.ensembl.mart.shell.MartShell $@
  ;;
# arp
*Darwin*Power*Mac*)
  TMP_LD_LIBPATH="${DYLD_LIBRARY_PATH}:${TMP_ROOT}/lib/macosx/"
  DYLD_LIBRARY_PATH=$TMP_LD_LIBPATH java -ea -classpath ${TMP_CLASSPATH} org.ensembl.mart.shell.MartShell $@
  ;;
# arp
*)
  echo "warning, this platform is not known to be supported, using linux libraries\n"
  TMP_LD_LIBPATH="${TMP_LD_LIBPATH}:${TMP_ROOT}/lib/linux"
  LD_LIBRARY_PATH=$TMP_LD_LIBPATH java -ea -classpath ${TMP_CLASSPATH} org.ensembl.mart.shell.MartShell $@
  ;;
esac

