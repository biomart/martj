#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartExplorer GUI application.

# Usage:
#
# prompt> bin/martgui.sh

TMP_ROOT=`dirname $0`/..
java -jar ${TMP_ROOT}/lib/mart-explorer.jar $@