#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Use a SQLite database with the diet model (dietmodel.py).  The database
# (diet.db) can be recreated using the included SQL script (diet.sql).
#
# Note that this example reads an external data file (..\data\diet.db).
# As a result, it must be run from the Gurobi examples/python directory.

import os
import sqlite3
from gurobipy import *

con = sqlite3.connect(os.path.join('..', 'data', 'diet.db'))
cur = con.cursor()

cur.execute('select category,minnutrition,maxnutrition from categories')
result = cur.fetchall()
categories, minNutrition, maxNutrition = multidict(
    (cat,[minv,maxv]) for cat,minv,maxv in result)

cur.execute('select food,cost from foods')
result = cur.fetchall()
foods, cost = multidict(result)

cur.execute('select food,category,value from nutrition')
result = cur.fetchall()
nutritionValues = dict(((f,c),v) for f,c,v in result)

con.close()

import dietmodel
dietmodel.solve(categories, minNutrition, maxNutrition,
                foods, cost, nutritionValues)
