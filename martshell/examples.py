#!/usr/bin/env jython

from martshell.initshell import *

def printTablesExample():
    q = Query()
    q.host = "kaka.sanger.ac.uk"
    q.user = "anonymous"
    e = Engine()
    e.databases(q)


## if __name__=="__main__":
printTablesExample()
