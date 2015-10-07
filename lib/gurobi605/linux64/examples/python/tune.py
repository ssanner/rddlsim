#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

#  This example reads a model from a file and tunes it.
#  It then writes the best parameter settings to a file
#  and solves the model using these parameters.

import sys
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: tune.py filename')
    quit()

# Read the model
model = read(sys.argv[1])

# Set the TuneResults parameter to 1
model.params.tuneResults = 1

# Tune the model
model.tune()

if model.tuneResultCount > 0:

    # Load the best tuned parameters into the model
    model.getTuneResult(0)

    # Write tuned parameters to a file
    model.write('tune.prm')

    # Solve the model using the tuned parameters
    model.optimize()
