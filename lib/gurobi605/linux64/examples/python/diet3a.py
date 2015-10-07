#!c:/Python27/python

# Copyright 2014, Gurobi Optimization, Inc.

# Use a Microsoft Access database and ODBC for the diet model
# (dietmodel.py).
#
# Note that this example reads an external data file (..\data\diet.accdb).
# As a result, it must be run from the Gurobi examples/python directory.
#
# Note also that you can't mix 32-bit and 64-bit programs in ODBC.
# If you are using the 32-bit version of Microsoft Office, this
# example will only work with the win32 Gurobi installation.

import pyodbc
from gurobipy import *

driver = "Driver={Microsoft Access Driver (*.mdb, *.accdb)};"
datafile = "DBQ=..\\data\\diet.accdb;"
con = pyodbc.connect(driver+datafile)
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
