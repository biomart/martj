#!/usr/bin/env jython

# Note: if your module is in a different directory to examples.py then
# you need to set python.path (either in ~/.jython or $PYTHONPATH) and
# use this import statement:

# from martshell.initshell import *

from initshell import *

def printTablesExample():
    q = Query()
    q.host = "kaka.sanger.ac.uk"
    q.user = "anonymous"
    e = Engine()
    print e.databases(q)


## if __name__=="__main__":
printTablesExample()
