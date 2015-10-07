#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# This example reads an LP model from a file and solves it.
# If the model is infeasible or unbounded, the example turns off
# presolve and solves the model again. If the model is infeasible,
# the example computes an Irreducible Inconsistent Subsystem (IIS),
# and writes it to a file

import sys
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: lp.py filename')
    quit()

# Read and solve model

model = read(sys.argv[1])
model.optimize()

if model.status == GRB.status.INF_OR_UNBD:
    # Turn presolve off to determine whether model is infeasible
    # or unbounded
    model.setParam(GRB.param.presolve, 0)
    model.optimize()

if model.status == GRB.status.OPTIMAL:
    print('Optimal objective: %g' % model.objVal)
    model.write('model.sol')
    exit(0)
elif model.status != GRB.status.INFEASIBLE:
    print('Optimization was stopped with status %d' % model.status)
    exit(0)


# Model is infeasible - compute an Irreducible Inconsistent Subsystem (IIS)

print('')
print('Model is infeasible')
model.computeIIS()
model.write("model.ilp")
print("IIS written to file 'model.ilp'")
