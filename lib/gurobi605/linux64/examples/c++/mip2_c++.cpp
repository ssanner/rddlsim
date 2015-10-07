/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, solves it and
   prints the objective values from all feasible solutions
   generated while solving the MIP. Then it creates the fixed
   model and solves that model. */

#include "gurobi_c++.h"
#include <cmath>
using namespace std;

int
main(int   argc,
     char *argv[])
{
  if (argc < 2) {
    cout << "Usage: mip2_c++ filename" << endl;
    return 1;
  }

  GRBEnv *env = 0;
  GRBVar *vars = 0, *fvars = 0;
  try {
    env = new GRBEnv();
    GRBModel model = GRBModel(*env, argv[1]);

    if (model.get(GRB_IntAttr_IsMIP) == 0) {
      throw GRBException("Model is not a MIP");
    }

    model.optimize();

    int optimstatus = model.get(GRB_IntAttr_Status);

    cout << "Optimization complete" << endl;
    double objval = 0;
    if (optimstatus == GRB_OPTIMAL) {
      objval = model.get(GRB_DoubleAttr_ObjVal);
      cout << "Optimal objective: " << objval << endl;
    } else if (optimstatus == GRB_INF_OR_UNBD) {
      cout << "Model is infeasible or unbounded" << endl;
      return 0;
    } else if (optimstatus == GRB_INFEASIBLE) {
      cout << "Model is infeasible" << endl;
      return 0;
    } else if (optimstatus == GRB_UNBOUNDED) {
      cout << "Model is unbounded" << endl;
      return 0;
    } else {
      cout << "Optimization was stopped with status = "
           << optimstatus << endl;
      return 0;
    }

    /* Iterate over the solutions and compute the objectives */

    int numvars = model.get(GRB_IntAttr_NumVars);
    vars = model.getVars();
    model.getEnv().set(GRB_IntParam_OutputFlag, 0);

    cout << endl;
    for ( int k = 0; k < model.get(GRB_IntAttr_SolCount); ++k ) {
      model.getEnv().set(GRB_IntParam_SolutionNumber, k);
      double objn = 0.0;

      for (int j = 0; j < numvars; j++) {
        GRBVar v = vars[j];
        objn += v.get(GRB_DoubleAttr_Obj) * v.get(GRB_DoubleAttr_Xn);
      }

      cout << "Solution " << k << " has objective: " << objn << endl;
    }
    cout << endl;
    model.getEnv().set(GRB_IntParam_OutputFlag, 1);

    /* Create a fixed model, turn off presolve and solve */

    GRBModel fixed = model.fixedModel();

    fixed.getEnv().set(GRB_IntParam_Presolve, 0);

    fixed.optimize();

    int foptimstatus = fixed.get(GRB_IntAttr_Status);

    if (foptimstatus != GRB_OPTIMAL) {
      cerr << "Error: fixed model isn't optimal" << endl;
      return 0;
    }

    double fobjval = fixed.get(GRB_DoubleAttr_ObjVal);

    if (fabs(fobjval - objval) > 1.0e-6 * (1.0 + fabs(objval))) {
      cerr << "Error: objective values are different" << endl;
      return 0;
    }

    /* Print values of nonzero variables */
    fvars = fixed.getVars();
    for (int j = 0; j < numvars; j++) {
      GRBVar v = fvars[j];
      if (v.get(GRB_DoubleAttr_X) != 0.0) {
        cout << v.get(GRB_StringAttr_VarName) << " "
             << v.get(GRB_DoubleAttr_X) << endl;
      }
    }

  } catch(GRBException e) {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  } catch (...) {
    cout << "Error during optimization" << endl;
  }

  delete[] fvars;
  delete[] vars;
  delete env;
  return 0;
}
