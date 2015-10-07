#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Solve the classic diet model.  This file implements
# a function that formulates and solves the model,
# but it contains no model data.  The data is
# passed in by the calling program.  Run example 'diet2.py',
# 'diet3.py', or 'diet4.py' to invoke this function.

from gurobipy import *


def solve(categories, minNutrition, maxNutrition, foods, cost,
          nutritionValues):
    # Model
    m = Model("diet")

    # Create decision variables for the nutrition information,
    # which we limit via bounds
    nutrition = {}
    for c in categories:
        nutrition[c] = m.addVar(lb=minNutrition[c], ub=maxNutrition[c], name=c)

    # Create decision variables for the foods to buy
    buy = {}
    for f in foods:
        buy[f] = m.addVar(obj=cost[f], name=f)

    # The objective is to minimize the costs
    m.modelSense = GRB.MINIMIZE

    # Update model to integrate new variables
    m.update()

    # Nutrition constraints
    for c in categories:
        m.addConstr(
          quicksum(nutritionValues[f,c] * buy[f] for f in foods) ==
                    nutrition[c], c)

    def printSolution():
        if m.status == GRB.status.OPTIMAL:
            print('\nCost: %g' % m.objVal)
            print('\nBuy:')
            for f in foods:
                if buy[f].x > 0.0001:
                    print('%s %g' % (f, buy[f].x))
            print('\nNutrition:')
            for c in categories:
                print('%s %g' % (c, nutrition[c].x))
        else:
            print('No solution')

    # Solve
    m.optimize()
    printSolution()

    print('\nAdding constraint: at most 6 servings of dairy')
    m.addConstr(buy['milk'] + buy['ice cream'] <= 6, "limit_dairy")

    # Solve
    m.optimize()
    printSolution()
