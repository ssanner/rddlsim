#!/usr/bin/python

# Copyright 2014, Gurobi Optimization, Inc.

#   This example reads a model from a file, sets up a callback that
#   monitors optimization progress and implements a custom
#   termination strategy, and outputs progress information to the
#   screen and to a log file.
#
#   The termination strategy implemented in this callback stops the
#   optimization of a MIP model once at least one of the following two
#   conditions have been satisfied:
#     1) The optimality gap is less than 10%
#     2) At least 10000 nodes have been explored, and an integer feasible
#        solution has been found.
#   Note that termination is normally handled through Gurobi parameters
#   (MIPGap, NodeLimit, etc.).  You should only use a callback for
#   termination if the available parameters don't capture your desired
#   termination criterion.

import sys
from gurobipy import *

# Define my callback function

def mycallback(model, where):
    if where == GRB.callback.POLLING:
        # Ignore polling callback
        pass
    elif where == GRB.callback.PRESOLVE:
        # Presolve callback
        cdels = model.cbGet(GRB.callback.PRE_COLDEL)
        rdels = model.cbGet(GRB.callback.PRE_ROWDEL)
        if cdels or rdels:
            print('%d columns and %d rows are removed' % (cdels, rdels))
    elif where == GRB.callback.SIMPLEX:
        # Simplex callback
        itcnt = model.cbGet(GRB.callback.SPX_ITRCNT)
        if itcnt - model._lastiter >= 100:
            model._lastiter = itcnt
            obj = model.cbGet(GRB.callback.SPX_OBJVAL)
            ispert = model.cbGet(GRB.callback.SPX_ISPERT)
            pinf = model.cbGet(GRB.callback.SPX_PRIMINF)
            dinf = model.cbGet(GRB.callback.SPX_DUALINF)
            if ispert == 0:
                ch = ' '
            elif ispert == 1:
                ch = 'S'
            else:
                ch = 'P'
            print('%d %g%s %g %g' % (int(itcnt), obj, ch, pinf, dinf))
    elif where == GRB.callback.MIP:
        # General MIP callback
        nodecnt = model.cbGet(GRB.callback.MIP_NODCNT)
        objbst = model.cbGet(GRB.callback.MIP_OBJBST)
        objbnd = model.cbGet(GRB.callback.MIP_OBJBND)
        solcnt = model.cbGet(GRB.callback.MIP_SOLCNT)
        if nodecnt - model._lastnode >= 100:
            model._lastnode = nodecnt
            actnodes = model.cbGet(GRB.callback.MIP_NODLFT)
            itcnt = model.cbGet(GRB.callback.MIP_ITRCNT)
            cutcnt = model.cbGet(GRB.callback.MIP_CUTCNT)
            print('%d %d %d %g %g %d %d' % (nodecnt, actnodes, \
                  itcnt, objbst, objbnd, solcnt, cutcnt))
        if abs(objbst - objbnd) < 0.1 * (1.0 + abs(objbst)):
            print('Stop early - 10% gap achieved')
            model.terminate()
        if nodecnt >= 10000 and solcnt:
            print('Stop early - 10000 nodes explored')
            model.terminate()
    elif where == GRB.callback.MIPSOL:
        # MIP solution callback
        nodecnt = model.cbGet(GRB.callback.MIPSOL_NODCNT)
        obj = model.cbGet(GRB.callback.MIPSOL_OBJ)
        solcnt = model.cbGet(GRB.callback.MIPSOL_SOLCNT)
        x = model.cbGetSolution(model.getVars())
        print('**** New solution at node %d, obj %g, sol %d, ' \
              'x[0] = %g ****' % (nodecnt, obj, solcnt, x[0]))
    elif where == GRB.callback.MIPNODE:
        # MIP node callback
        print('**** New node ****')
        if model.cbGet(GRB.callback.MIPNODE_STATUS) == GRB.status.OPTIMAL:
            x = model.cbGetNodeRel(model.getVars())
            model.cbSetSolution(model.getVars(), x)
    elif where == GRB.callback.BARRIER:
        # Barrier callback
        itcnt = model.cbGet(GRB.callback.BARRIER_ITRCNT)
        primobj = model.cbGet(GRB.callback.BARRIER_PRIMOBJ)
        dualobj = model.cbGet(GRB.callback.BARRIER_DUALOBJ)
        priminf = model.cbGet(GRB.callback.BARRIER_PRIMINF)
        dualinf = model.cbGet(GRB.callback.BARRIER_DUALINF)
        cmpl = model.cbGet(GRB.callback.BARRIER_COMPL)
        print('%d %g %g %g %g %g' % (itcnt, primobj, dualobj, \
              priminf, dualinf, cmpl))
    elif where == GRB.callback.MESSAGE:
        # Message callback
        msg = model.cbGet(GRB.callback.MSG_STRING)
        model._logfile.write(msg)


if len(sys.argv) < 2:
    print('Usage: callback.py filename')
    quit()

# Turn off display and heuristics

setParam('OutputFlag', 0)
setParam('Heuristics', 0)

# Read model from file

model = read(sys.argv[1])

# Open log file

logfile = open('cb.log', 'w')

# Pass data into my callback function

model._lastiter = -GRB.INFINITY
model._lastnode = -GRB.INFINITY
model._logfile = logfile

# Solve model and capture solution information

model.optimize(mycallback)

print('')
print('Optimization complete')
if model.SolCount == 0:
    print('No solution found, optimization status = %d' % model.Status)
else:
    print('Solution found, objective = %g' % model.ObjVal)
    for v in model.getVars():
        if v.X != 0.0:
            print('%s %g' % (v.VarName, v.X))

# Close log file

logfile.close()
