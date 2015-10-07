/* Copyright 2014, Gurobi Optimization, Inc. */

/*
   This example reads a model from a file, sets up a callback that
   monitors optimization progress and implements a custom
   termination strategy, and outputs progress information to the
   screen and to a log file.

   The termination strategy implemented in this callback stops the
   optimization of a MIP model once at least one of the following two
   conditions have been satisfied:
     1) The optimality gap is less than 10%
     2) At least 10000 nodes have been explored, and an integer feasible
        solution has been found.
   Note that termination is normally handled through Gurobi parameters
   (MIPGap, NodeLimit, etc.).  You should only use a callback for
   termination if the available parameters don't capture your desired
   termination criterion.
*/

#include "gurobi_c++.h"
#include <fstream>
#include <cmath>
using namespace std;

class mycallback: public GRBCallback
{
  public:
    double lastiter;
    double lastnode;
    int numvars;
    GRBVar* vars;
    ofstream* logfile;
    mycallback(int xnumvars, GRBVar* xvars, ofstream* xlogfile) {
      lastiter = lastnode = -GRB_INFINITY;
      numvars = xnumvars;
      vars = xvars;
      logfile = xlogfile;
    }
  protected:
    void callback () {
      try {
        if (where == GRB_CB_POLLING) {
          // Ignore polling callback
        } else if (where == GRB_CB_PRESOLVE) {
          // Presolve callback
          int cdels = getIntInfo(GRB_CB_PRE_COLDEL);
          int rdels = getIntInfo(GRB_CB_PRE_ROWDEL);
          if (cdels || rdels) {
            cout << cdels << " columns and " << rdels
                 << " rows are removed" << endl;
          }
        } else if (where == GRB_CB_SIMPLEX) {
          // Simplex callback
          double itcnt = getDoubleInfo(GRB_CB_SPX_ITRCNT);
          if (itcnt - lastiter >= 100) {
            lastiter = itcnt;
            double obj = getDoubleInfo(GRB_CB_SPX_OBJVAL);
            int ispert = getIntInfo(GRB_CB_SPX_ISPERT);
            double pinf = getDoubleInfo(GRB_CB_SPX_PRIMINF);
            double dinf = getDoubleInfo(GRB_CB_SPX_DUALINF);
            char ch;
            if (ispert == 0)      ch = ' ';
            else if (ispert == 1) ch = 'S';
            else                  ch = 'P';
            cout << itcnt << " " << obj << ch << " "
                 << pinf << " " << dinf << endl;
          }
        } else if (where == GRB_CB_MIP) {
          // General MIP callback
          double nodecnt = getDoubleInfo(GRB_CB_MIP_NODCNT);
          double objbst = getDoubleInfo(GRB_CB_MIP_OBJBST);
          double objbnd = getDoubleInfo(GRB_CB_MIP_OBJBND);
          int solcnt = getIntInfo(GRB_CB_MIP_SOLCNT);
          if (nodecnt - lastnode >= 100) {
            lastnode = nodecnt;
            int actnodes = (int) getDoubleInfo(GRB_CB_MIP_NODLFT);
            int itcnt = (int) getDoubleInfo(GRB_CB_MIP_ITRCNT);
            int cutcnt = getIntInfo(GRB_CB_MIP_CUTCNT);
            cout << nodecnt << " " << actnodes << " " << itcnt
                 << " " << objbst << " " << objbnd << " "
                 << solcnt << " " << cutcnt << endl;
          }
          if (fabs(objbst - objbnd) < 0.1 * (1.0 + fabs(objbst))) {
            cout << "Stop early - 10% gap achieved" << endl;
            abort();
          }
          if (nodecnt >= 10000 && solcnt) {
            cout << "Stop early - 10000 nodes explored" << endl;
            abort();
          }
        } else if (where == GRB_CB_MIPSOL) {
          // MIP solution callback
          int nodecnt = (int) getDoubleInfo(GRB_CB_MIPSOL_NODCNT);
          double obj = getDoubleInfo(GRB_CB_MIPSOL_OBJ);
          int solcnt = getIntInfo(GRB_CB_MIPSOL_SOLCNT);
          double* x = getSolution(vars, numvars);
          cout << "**** New solution at node " << nodecnt
               << ", obj " << obj << ", sol " << solcnt
               << ", x[0] = " << x[0] << " ****" << endl;
          delete[] x;
        } else if (where == GRB_CB_MIPNODE) {
          // MIP node callback
          cout << "**** New node ****" << endl;
          if (getIntInfo(GRB_CB_MIPNODE_STATUS) == GRB_OPTIMAL) {
            double* x = getNodeRel(vars, numvars);
            setSolution(vars, x, numvars);
            delete[] x;
          }
        } else if (where == GRB_CB_BARRIER) {
          // Barrier callback
          int itcnt = getIntInfo(GRB_CB_BARRIER_ITRCNT);
          double primobj = getDoubleInfo(GRB_CB_BARRIER_PRIMOBJ);
          double dualobj = getDoubleInfo(GRB_CB_BARRIER_DUALOBJ);
          double priminf = getDoubleInfo(GRB_CB_BARRIER_PRIMINF);
          double dualinf = getDoubleInfo(GRB_CB_BARRIER_DUALINF);
          double cmpl = getDoubleInfo(GRB_CB_BARRIER_COMPL);
          cout << itcnt << " " << primobj << " " << dualobj << " "
               << priminf << " " << dualinf << " " << cmpl << endl;
        } else if (where == GRB_CB_MESSAGE) {
          // Message callback
          string msg = getStringInfo(GRB_CB_MSG_STRING);
          *logfile << msg;
        }
      } catch (GRBException e) {
        cout << "Error number: " << e.getErrorCode() << endl;
        cout << e.getMessage() << endl;
      } catch (...) {
        cout << "Error during callback" << endl;
      }
    }
};

int
main(int   argc,
     char *argv[])
{
  if (argc < 2) {
    cout << "Usage: callback_c++ filename" << endl;
    return 1;
  }

  // Open log file
  ofstream logfile("cb.log");
  if (!logfile.is_open()) {
    cout << "Cannot open cb.log for callback message" << endl;
    return 1;
  }

  GRBEnv *env = 0;
  GRBVar *vars = 0;

  try {
    // Create environment
    env = new GRBEnv();

    // Turn off display and heuristics
    env->set(GRB_IntParam_OutputFlag, 0);
    env->set(GRB_DoubleParam_Heuristics, 0.0);

    // Read model from file
    GRBModel model = GRBModel(*env, argv[1]);

    // Create a callback object and associate it with the model
    int numvars = model.get(GRB_IntAttr_NumVars);
    vars = model.getVars();
    mycallback cb = mycallback(numvars, vars, &logfile);

    model.setCallback(&cb);

    // Solve model and capture solution information
    model.optimize();

    cout << endl << "Optimization complete" << endl;
    if (model.get(GRB_IntAttr_SolCount) == 0) {
      cout << "No solution found, optimization status = "
           << model.get(GRB_IntAttr_Status) << endl;
    } else {
      cout << "Solution found, objective = "
           << model.get(GRB_DoubleAttr_ObjVal) << endl;
      for (int j = 0; j < numvars; j++) {
        GRBVar v = vars[j];
        double x = v.get(GRB_DoubleAttr_X);
        if (x != 0.0) {
          cout << v.get(GRB_StringAttr_VarName) << " " << x << endl;
        }
      }
    }

  } catch (GRBException e) {
    cout << "Error number: " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  } catch (...) {
    cout << "Error during optimization" << endl;
  }

  // Close log file
  logfile.close();

  delete[] vars;
  delete env;

  return 0;
}
