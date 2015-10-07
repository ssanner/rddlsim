#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# This example reads an LP model from a file and solves it.
# If the model can be solved, then it finds the smallest positive variable,
# sets its upper bound to zero, and resolves the model two ways:
# first with an advanced start, then without an advanced start
# (i.e. 'from scratch').

import sys
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: lpmod.py filename')
    quit()

# Read model and determine whether it is an LP

model = read(sys.argv[1])
if model.isMIP == 1:
    print('The model is not a linear program')
    exit(1)

model.optimize()

status = model.status

if status == GRB.status.INF_OR_UNBD or status == GRB.status.INFEASIBLE \
   or status == GRB.status.UNBOUNDED:
    print('The model cannot be solved because it is infeasible or unbounded')
    exit(1)

if status != GRB.status.OPTIMAL:
    print('Optimization was stopped with status %d' % status)
    exit(0)

# Find the smallest variable value
minVal = GRB.INFINITY
for v in model.getVars():
    if v.x > 0.0001 and v.x < minVal and v.lb == 0.0:
        minVal = v.x
        minVar = v

print('\n*** Setting %s from %g to zero ***\n' % (minVar.varName, minVal))
minVar.ub = 0.0

# Solve from this starting point
model.optimize()

# Save iteration & time info
warmCount = model.IterCount
warmTime = model.Runtime

# Reset the model and resolve
print('\n*** Resetting and solving without an advanced start ***\n')
model.reset()
model.optimize()

coldCount = model.IterCount
coldTime = model.Runtime

print('')
print('*** Warm start: %g iterations, %g seconds' % (warmCount, warmTime))
print('*** Cold start: %g iterations, %g seconds' % (coldCount, coldTime))
