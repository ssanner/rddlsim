#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Sudoku example.

# The Sudoku board is a 9x9 grid, which is further divided into a 3x3 grid
# of 3x3 grids.  Each cell in the grid must take a value from 0 to 9.
# No two grid cells in the same row, column, or 3x3 subgrid may take the
# same value.
#
# In the MIP formulation, binary variables x[i,j,v] indicate whether
# cell <i,j> takes value 'v'.  The constraints are as follows:
#   1. Each cell must take exactly one value (sum_v x[i,j,v] = 1)
#   2. Each value is used exactly once per row (sum_i x[i,j,v] = 1)
#   3. Each value is used exactly once per column (sum_j x[i,j,v] = 1)
#   4. Each value is used exactly once per 3x3 subgrid (sum_grid x[i,j,v] = 1)
#
# Input datasets for this example can be found in examples/data/sudoku*.

import sys
import math
from gurobipy import *

if len(sys.argv) < 2:
    print('Usage: sudoku.py filename')
    quit()

f = open(sys.argv[1])

grid = f.read().split()

n = len(grid[0])
s = int(math.sqrt(n))


# 3-D array of variables will be indexed by (i,j,v) tuples

vars = {}


# Create our 3-D array of model variables

model = Model('sudoku')

for i in range(n):
    for j in range(n):
        for v in range(n):
            vars[i,j,v] = model.addVar(vtype=GRB.BINARY,
                                       name='G_'+ str(i)+'_'+str(j)+'_'+str(v))

# Update model to integrate new variables

model.update()


# Fix variables associated with cells whose values are pre-specified

for i in range(n):
    for j in range(n):
        if grid[i][j] != '.':
            v = int(grid[i][j]) - 1
            model.addConstr(vars[i,j,v] == 1, 'Fix_' + str(i) + '_' + str(j))

# Each cell must take one value

for i in range(n):
    for j in range(n):
        model.addConstr(quicksum([vars[i,j,v] for v in range(n)]) == 1,
                        'V_' + str(i) + '_' + str(j))

# Each value appears once per row

for i in range(n):
    for v in range(n):
        model.addConstr(quicksum([vars[i,j,v] for j in range(n)]) == 1,
                        'R_' + str(i) + '_' + str(v))

# Each value appears once per column

for j in range(n):
    for v in range(n):
        model.addConstr(quicksum([vars[i,j,v] for i in range(n)]) == 1,
                        'C_' + str(j) + '_' + str(v))

# Each value appears once per subgrid

for v in range(n):
    for i0 in range(s):
        for j0 in range(s):
            subgrid = [vars[i,j,v] for i in range(i0*s, (i0+1)*s)
                                     for j in range(j0*s, (j0+1)*s)]
            model.addConstr(quicksum(subgrid) == 1,
                            'Sub_' + str(i0) + '_' + str(j0) + '_' + str(v))

model.optimize()

model.write('sudoku.lp')

print('')
print('Solution:')
print('')

# Retrieve optimization result

solution = model.getAttr('X', vars)

for i in range(n):
    sol = ''
    for j in range(n):
        for v in range(n):
            if solution[i,j,v] > 0.5:
                sol += str(v+1)
    print(sol)
