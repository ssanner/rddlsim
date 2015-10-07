#!/usr/bin/python -i

import sys
import os

try:
  from gurobipy import *
except Exception as e:
  print('')
  print('%s' % e.message)
  print('')
  if e.message.find('No Gurobi license found') != -1:
    print('Running grbgetkey...')
    os.system('grbgetkey')
    print('Restart the Gurobi Interactive Shell to use a newly retrieved license key')
  exit(1)

version = str(gurobi.version()[0]) + '.' + \
          str(gurobi.version()[1]) + '.' + \
          str(gurobi.version()[2])
platform = gurobi.platform()

print('\nGurobi Interactive Shell (' + platform + '), Version ' + version)
print('Copyright (c) 2015, Gurobi Optimization, Inc.')
print('Type "help()" for help\n')

sys.ps1 = "gurobi> "
sys.ps2 = "....... "
