#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# This example reads a MIP model from a file, adds artificial
# variables to each constraint, and then minimizes the sum of the
# artificial variables.  A solution with objective zero corresponds
# to a feasible solution to the input model.
#
# We can also use FeasRelax feature to do it. In this example, we
# use minrelax=1, i.e. optimizing the returned model finds a solution
# that minimizes the original objective, but only from among those
# solutions that minimize the sum of the artificial variables.

import sys
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: feasopt.py filename')
    quit()

feasmodel = gurobi.read(sys.argv[1])

#create a copy to use FeasRelax feature later

feasmodel1 = feasmodel.copy()

# clear objective

feasmodel.setObjective(0.0)

# add slack variables

for c in feasmodel.getConstrs():
    sense = c.sense
    if sense != '>':
        feasmodel.addVar(obj=1.0, name="ArtN_" + c.constrName,
                         column=Column([-1], [c]))
    if sense != '<':
        feasmodel.addVar(obj=1.0, name="ArtP_" + c.constrName,
                         column=Column([1], [c]))

feasmodel.update()

# optimize modified model

feasmodel.write('feasopt.lp')

feasmodel.optimize()

# use FeasRelax feature

feasmodel1.feasRelaxS(0, True, False, True);

feasmodel1.write("feasopt1.lp");

feasmodel1.optimize();
