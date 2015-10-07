#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

# Read diet model data from an Excel spreadsheet (diet.xls).
# Pass the imported data into the diet model (dietmodel.py).
#
# Note that this example reads an external data file (..\data\diet.xls).
# As a result, it must be run from the Gurobi examples/python directory.
#
# This example requires Python package 'xlrd', which isn't included
# in most Python distributions.  You can obtain it from
# http://pypi.python.org/pypi/xlrd.

import os
import xlrd

book = xlrd.open_workbook(os.path.join("..", "data", "diet.xls"))

sh = book.sheet_by_name("Categories")
categories = []
minNutrition = {}
maxNutrition = {}
i = 1
while True:
    try:
        c = sh.cell_value(i, 0)
        categories.append(c)
        minNutrition[c] = sh.cell_value(i,1)
        maxNutrition[c] = sh.cell_value(i,2)
        i = i + 1
    except IndexError:
        break

sh = book.sheet_by_name("Foods")
foods = []
cost = {}
i = 1
while True:
    try:
        f = sh.cell_value(i, 0)
        foods.append(f)
        cost[f] = sh.cell_value(i,1)
        i = i + 1
    except IndexError:
        break

sh = book.sheet_by_name("Nutrition")
nutritionValues = {}
for i in range(len(foods)):
    for j in range(len(categories)):
        nutritionValues[foods[i],categories[j]] = sh.cell_value(i+1,j+1)

import dietmodel
dietmodel.solve(categories, minNutrition, maxNutrition,
                foods, cost, nutritionValues)
