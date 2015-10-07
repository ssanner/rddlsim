#
# Copyright 2014, Gurobi Optimization, Inc.
#
# Interactive shell customization example
#
# Define a set of customizations for the Gurobi shell.
# Type 'from custom import *' to import them into your shell.
#

from gurobipy import *


# custom read command --- change directory as appropriate

def myread(name):
    return read('/home/jones/models/' + name)


# simple termination callback

def mycallback(model, where):
    if where == GRB.callback.MIP:
        time = model.cbGet(GRB.callback.RUNTIME)
        best = model.cbGet(GRB.callback.MIP_OBJBST)
        if time > 10 and best < GRB.INFINITY:
            model.terminate()


# custom optimize() function that uses callback

def myopt(model):
    model.optimize(mycallback)
