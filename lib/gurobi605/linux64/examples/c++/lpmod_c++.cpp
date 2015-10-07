/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads an LP model from a file and solves it.
   If the model can be solved, then it finds the smallest positive variable,
   sets its upper bound to zero, and resolves the model two ways:
   first with an advanced start, then without an advanced start
   (i.e. 'from scratch'). */

#include "gurobi_c++.h"
using namespace std;

int
main(int argc,
     char *argv[])
{
  if (argc < 2)
  {
    cout << "Usage: lpmod_c++ filename" << endl;
    return 1;
  }

  GRBEnv* env = 0;
  GRBVar* v = 0;
  try
  {
    // Read model and determine whether it is an LP
    env = new GRBEnv();
    GRBModel model = GRBModel(*env, argv[1]);
    if (model.get(GRB_IntAttr_IsMIP) != 0)
    {
      cout << "The model is not a linear program" << endl;
      return 1;
    }

    model.optimize();

    int status = model.get(GRB_IntAttr_Status);

    if ((status == GRB_INF_OR_UNBD) || (status == GRB_INFEASIBLE) ||
        (status == GRB_UNBOUNDED))
    {
      cout << "The model cannot be solved because it is "
      << "infeasible or unbounded" << endl;
      return 1;
    }

    if (status != GRB_OPTIMAL)
    {
      cout << "Optimization was stopped with status " << status << endl;
      return 0;
    }

    // Find the smallest variable value
    double minVal = GRB_INFINITY;
    int minVar = 0;
    v = model.getVars();
    for (int j = 0; j < model.get(GRB_IntAttr_NumVars); ++j)
    {
      double sol = v[j].get(GRB_DoubleAttr_X);
      if ((sol > 0.0001) && (sol < minVal) &&
          (v[j].get(GRB_DoubleAttr_LB) == 0.0))
      {
        minVal = sol;
        minVar = j;
      }
    }

    cout << "\n*** Setting " << v[minVar].get(GRB_StringAttr_VarName)
    << " from " << minVal << " to zero ***" << endl << endl;
    v[minVar].set(GRB_DoubleAttr_UB, 0.0);

    // Solve from this starting point
    model.optimize();

    // Save iteration & time info
    double warmCount = model.get(GRB_DoubleAttr_IterCount);
    double warmTime = model.get(GRB_DoubleAttr_Runtime);

    // Reset the model and resolve
    cout << "\n*** Resetting and solving "
    << "without an advanced start ***\n" << endl;
    model.reset();
    model.optimize();

    // Save iteration & time info
    double coldCount = model.get(GRB_DoubleAttr_IterCount);
    double coldTime = model.get(GRB_DoubleAttr_Runtime);

    cout << "\n*** Warm start: " << warmCount << " iterations, " <<
    warmTime << " seconds" << endl;
    cout << "*** Cold start: " << coldCount << " iterations, " <<
    coldTime << " seconds" << endl;

  }
  catch (GRBException e)
  {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  }
  catch (...)
  {
    cout << "Error during optimization" << endl;
  }

  delete[] v;
  delete env;
  return 0;
}
