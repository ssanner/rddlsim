#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Facility location: a company currently ships its product from 5 plants
# to 4 warehouses. It is considering closing some plants to reduce
# costs. What plant(s) should the company close, in order to minimize
# transportation and fixed costs?
#
# Note that this example uses lists instead of dictionaries.  Since
# it does not work with sparse data, lists are a reasonable option.
#
# Based on an example from Frontline Systems:
#   http://www.solver.com/disfacility.htm
# Used with permission.

from gurobipy import *

# Warehouse demand in thousands of units
demand = [15, 18, 14, 20]

# Plant capacity in thousands of units
capacity = [20, 22, 17, 19, 18]

# Fixed costs for each plant
fixedCosts = [12000, 15000, 17000, 13000, 16000]

# Transportation costs per thousand units
transCosts = [[4000, 2000, 3000, 2500, 4500],
              [2500, 2600, 3400, 3000, 4000],
              [1200, 1800, 2600, 4100, 3000],
              [2200, 2600, 3100, 3700, 3200]]

# Range of plants and warehouses
plants = range(len(capacity))
warehouses = range(len(demand))

# Model
m = Model("facility")

# Plant open decision variables: open[p] == 1 if plant p is open.
open = []
for p in plants:
    open.append(m.addVar(vtype=GRB.BINARY, name="Open%d" % p))

# Transportation decision variables: how much to transport from
# a plant p to a warehouse w
transport = []
for w in warehouses:
    transport.append([])
    for p in plants:
        transport[w].append(m.addVar(obj=transCosts[w][p],
                                     name="Trans%d.%d" % (p, w)))

# The objective is to minimize the total fixed and variable costs
m.modelSense = GRB.MINIMIZE

# Update model to integrate new variables
m.update()

# Set optimization objective - minimize sum of fixed costs
m.setObjective(quicksum([fixedCosts[p]*open[p] for p in plants]))

# Production constraints
# Note that the right-hand limit sets the production to zero if the plant
# is closed
for p in plants:
    m.addConstr(
        quicksum(transport[w][p] for w in warehouses) <= capacity[p] * open[p],
        "Capacity%d" % p)

# Demand constraints
for w in warehouses:
    m.addConstr(quicksum(transport[w][p] for p in plants) == demand[w],
                "Demand%d" % w)

# Guess at the starting point: close the plant with the highest fixed costs;
# open all others

# First, open all plants
for p in plants:
    open[p].start = 1.0

# Now close the plant with the highest fixed cost
print('Initial guess:')
maxFixed = max(fixedCosts)
for p in plants:
    if fixedCosts[p] == maxFixed:
        open[p].start = 0.0
        print('Closing plant %s' % p)
        break
print('')

# Use barrier to solve root relaxation
m.params.method = 2

# Solve
m.optimize()

# Print solution
print('\nTOTAL COSTS: %g' % m.objVal)
print('SOLUTION:')
for p in plants:
    if open[p].x == 1.0:
        print('Plant %s open' % p)
        for w in warehouses:
            if transport[w][p].x > 0:
                print('  Transport %g units to warehouse %s' % \
                      (transport[w][p].x, w))
    else:
        print('Plant %s closed!' % p)
