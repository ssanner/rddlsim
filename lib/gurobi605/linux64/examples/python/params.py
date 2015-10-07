#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Use parameters that are associated with a model.
#
# A MIP is solved for 5 seconds with different sets of parameters.
# The one with the smallest MIP gap is selected, and the optimization
# is resumed until the optimal solution is found.

import sys
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: params.py filename')
    quit()


# Read model and verify that it is a MIP
m = read(sys.argv[1])
if m.isMIP == 0:
    print('The model is not an integer program')
    exit(1)

# Set a 5 second time limit
m.params.timeLimit = 5

# Now solve the model with different values of MIPFocus
bestModel = m.copy()
bestModel.optimize()
for i in range(1, 4):
    m.reset()
    m.params.MIPFocus = i
    m.optimize()
    if bestModel.MIPGap > m.MIPGap:
        bestModel, m = m, bestModel # swap models

# Finally, delete the extra model, reset the time limit and
# continue to solve the best model to optimality
del m
bestModel.params.timeLimit = "default"
bestModel.optimize()
print('Solved with MIPFocus: %d' % bestModel.params.MIPFocus)
