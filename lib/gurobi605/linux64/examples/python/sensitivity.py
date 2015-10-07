#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# A simple sensitivity analysis example which reads a MIP model
# from a file and solves it. Then each binary variable is set
# to 1-X, where X is its value in the optimal solution, and
# the impact on the objective function value is reported.

import sys
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: sensitivity.py filename')
    quit()

# Read and solve model

model = read(sys.argv[1])

if model.IsMIP == 0:
    print('Model is not a MIP')
    exit(0)

model.optimize()

if model.status != GRB.status.OPTIMAL:
    print('Optimization ended with status %d' % model.status)
    exit(0)

# Store the optimal solution

origObjVal = model.ObjVal
for v in model.getVars():
    v._origX = v.X

# Disable solver output for subsequent solves

model.params.OutputFlag = 0

# Iterate through unfixed, binary variables in model

for v in model.getVars():
    if (v.LB == 0 and v.UB == 1 \
        and (v.VType == GRB.BINARY or v.VType == GRB.INTEGER)):

        # Set variable to 1-X, where X is its value in optimal solution

        if v._origX < 0.5:
            v.LB = v.Start = 1
        else:
            v.UB = v.Start = 0

        # Update MIP start for the other variables

        for vv in model.getVars():
            if not vv.sameAs(v):
                vv.Start = vv._origX

        # Solve for new value and capture sensitivity information

        model.optimize()

        if model.status == GRB.OPTIMAL:
            print('Objective sensitivity for variable %s is %g' % \
                  (v.VarName, model.ObjVal - origObjVal))
        else:
            print('Objective sensitivity for variable %s is infinite' % \
                  v.VarName)

        # Restore the original variable bounds

        v.LB = 0
        v.UB = 1
