#!/usr/bin/env jython

from org.ensembl.mart.explorer import *
from org.ensembl.mart.explorer.gui import *
import sys

# add the parent directory of this directory to python's path so that the
# user does't have to set it
sys.path.append( sys.path[0]+"/..")

MartExplorerApplication.defaultLoggingConfiguration()

print "*************"
print "* MartShell *"
print "*************"


