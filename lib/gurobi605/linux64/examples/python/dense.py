#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# This example formulates and solves the following simple QP model:
#
#    minimize    x + y + x^2 + x*y + y^2 + y*z + z^2
#    subject to  x + 2 y + 3 z >= 4
#                x +   y       >= 1
#
# The example illustrates the use of dense matrices to store A and Q
# (and dense vectors for the other relevant data).  We don't recommend
# that you use dense matrices, but this example may be helpful if you
# already have your data in this format.

import sys
from gurobipy import *

def dense_optimize(rows, cols, c, Q, A, sense, rhs, lb, ub, vtype,
                   solution):

  model = Model()

  # Add variables to model
  for j in range(cols):
    model.addVar(lb=lb[j], ub=ub[j], vtype=vtype[j])
  model.update()
  vars = model.getVars()

  # Populate A matrix
  for i in range(rows):
    expr = LinExpr()
    for j in range(cols):
      if A[i][j] != 0:
        expr += A[i][j]*vars[j]
    model.addConstr(expr, sense[i], rhs[i])

  # Populate objective
  obj = QuadExpr()
  for i in range(cols):
    for j in range(cols):
      if Q[i][j] != 0:
        obj += Q[i][j]*vars[i]*vars[j]
  for j in range(cols):
    if c[j] != 0:
      obj += c[j]*vars[j]
  model.setObjective(obj)

  # Write model to a file
  model.update()
  model.write('dense.lp')

  # Solve
  model.optimize()

  if model.status == GRB.status.OPTIMAL:
    x = model.getAttr('x', vars)
    for i in range(cols):
      solution[i] = x[i]
    return True
  else:
    return False


# Put model data into dense matrices

c = [1, 1, 0]
Q = [[1, 1, 0], [0, 1, 1], [0, 0, 1]]
A = [[1, 2, 3], [1, 1, 0]]
sense = [GRB.GREATER_EQUAL, GRB.GREATER_EQUAL]
rhs = [4, 1]
lb = [0, 0, 0]
ub = [GRB.INFINITY, GRB.INFINITY, GRB.INFINITY]
vtype = [GRB.CONTINUOUS, GRB.CONTINUOUS, GRB.CONTINUOUS]
sol = [0]*3

# Optimize

success = dense_optimize(2, 3, c, Q, A, sense, rhs, lb, ub, vtype, sol)

if success:
  print('x: %g, y: %g, z: %g' % (sol[0], sol[1], sol[2]))
