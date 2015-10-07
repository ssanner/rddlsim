#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Solve the classic diet model, showing how to add constraints
# to an existing model.

from gurobipy import *

# Nutrition guidelines, based on
# USDA Dietary Guidelines for Americans, 2005
# http://www.health.gov/DietaryGuidelines/dga2005/

categories, minNutrition, maxNutrition = multidict({
  'calories': [1800, 2200],
  'protein':  [91, GRB.INFINITY],
  'fat':      [0, 65],
  'sodium':   [0, 1779] })

foods, cost = multidict({
  'hamburger': 2.49,
  'chicken':   2.89,
  'hot dog':   1.50,
  'fries':     1.89,
  'macaroni':  2.09,
  'pizza':     1.99,
  'salad':     2.49,
  'milk':      0.89,
  'ice cream': 1.59 })

# Nutrition values for the foods
nutritionValues = {
  ('hamburger', 'calories'): 410,
  ('hamburger', 'protein'):  24,
  ('hamburger', 'fat'):      26,
  ('hamburger', 'sodium'):   730,
  ('chicken',   'calories'): 420,
  ('chicken',   'protein'):  32,
  ('chicken',   'fat'):      10,
  ('chicken',   'sodium'):   1190,
  ('hot dog',   'calories'): 560,
  ('hot dog',   'protein'):  20,
  ('hot dog',   'fat'):      32,
  ('hot dog',   'sodium'):   1800,
  ('fries',     'calories'): 380,
  ('fries',     'protein'):  4,
  ('fries',     'fat'):      19,
  ('fries',     'sodium'):   270,
  ('macaroni',  'calories'): 320,
  ('macaroni',  'protein'):  12,
  ('macaroni',  'fat'):      10,
  ('macaroni',  'sodium'):   930,
  ('pizza',     'calories'): 320,
  ('pizza',     'protein'):  15,
  ('pizza',     'fat'):      12,
  ('pizza',     'sodium'):   820,
  ('salad',     'calories'): 320,
  ('salad',     'protein'):  31,
  ('salad',     'fat'):      12,
  ('salad',     'sodium'):   1230,
  ('milk',      'calories'): 100,
  ('milk',      'protein'):  8,
  ('milk',      'fat'):      2.5,
  ('milk',      'sodium'):   125,
  ('ice cream', 'calories'): 330,
  ('ice cream', 'protein'):  8,
  ('ice cream', 'fat'):      10,
  ('ice cream', 'sodium'):   180 }

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
      quicksum(nutritionValues[f,c] * buy[f] for f in foods) == nutrition[c],
               c)


def printSolution():
    if m.status == GRB.status.OPTIMAL:
        print('\nCost: %g' % m.objVal)
        print('\nBuy:')
        buyx = m.getAttr('x', buy)
        nutritionx = m.getAttr('x', nutrition)
        for f in foods:
            if buy[f].x > 0.0001:
                print('%s %g' % (f, buyx[f]))
        print('\nNutrition:')
        for c in categories:
            print('%s %g' % (c, nutritionx[c]))
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
