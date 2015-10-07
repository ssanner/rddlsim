#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# This example considers the following separable, convex problem:
#
#   minimize    f(x) - y + g(z)
#   subject to  x + 2 y + 3 z <= 4
#               x +   y       >= 1
#               x,    y,    z <= 1
#
# where f(u) = exp(-u) and g(u) = 2 u^2 - 4 u, for all real u. It
# formulates and solves a simpler LP model by approximating f and
# g with piecewise-linear functions. Then it transforms the model
# into a MIP by negating the approximation for f, which corresponds
# to a non-convex piecewise-linear function, and solves it again.

from gurobipy import *
from math import exp

def f(u):
    return exp(-u)

def g(u):
    return 2 * u * u - 4 * u

try:

    # Create a new model

    m = Model()

    # Create variables

    lb = 0.0
    ub = 1.0

    x = m.addVar(lb, ub, name='x')
    y = m.addVar(lb, ub, name='y')
    z = m.addVar(lb, ub, name='z')

    # Integrate new variables

    m.update()

    # Set objective for y

    m.setObjective(-y)

    # Add piecewise-linear objective functions for x and z

    npts = 101
    ptu = []
    ptf = []
    ptg = []

    for i in range(npts):
        ptu.append(lb + (ub - lb) * i / (npts - 1))
        ptf.append(f(ptu[i]))
        ptg.append(g(ptu[i]))

    m.setPWLObj(x, ptu, ptf)
    m.setPWLObj(z, ptu, ptg)

    # Add constraint: x + 2 y + 3 z <= 4

    m.addConstr(x + 2 * y + 3 * z <= 4, 'c0')

    # Add constraint: x + y >= 1

    m.addConstr(x + y >= 1, 'c1')

    # Optimize model as an LP

    m.optimize()

    print('IsMIP: %d' % m.IsMIP)
    for v in m.getVars():
        print('%s %g' % (v.VarName, v.X))
    print('Obj: %g' % m.ObjVal)
    print('')

    # Negate piecewise-linear objective function for x

    for i in range(npts):
        ptf[i] = -ptf[i]

    m.setPWLObj(x, ptu, ptf)

    # Optimize model as a MIP

    m.optimize()

    print('IsMIP: %d' % m.IsMIP)
    for v in m.getVars():
        print('%s %g' % (v.VarName, v.X))
    print('Obj: %g' % m.ObjVal)

except GurobiError:

    print('Encountered an error')
