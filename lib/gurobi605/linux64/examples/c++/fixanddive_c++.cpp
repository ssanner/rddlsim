/* Copyright 2014, Gurobi Optimization, Inc. */

/* Implement a simple MIP heuristic.  Relax the model,
   sort variables based on fractionality, and fix the 25% of
   the fractional variables that are closest to integer variables.
   Repeat until either the relaxation is integer feasible or
   linearly infeasible. */

#include "gurobi_c++.h"
#include <algorithm>
#include <cmath>
#include <deque>
using namespace std;

bool vcomp(GRBVar*, GRBVar*) throw(GRBException);

int
main(int argc,
     char *argv[])
{
  if (argc < 2)
  {
    cout << "Usage: fixanddive_c++ filename" << endl;
    return 1;
  }

  GRBEnv* env = 0;
  GRBVar* x = 0;
  try
  {
    // Read model
    env = new GRBEnv();
    GRBModel model = GRBModel(*env, argv[1]);

    // Collect integer variables and relax them
    // Note that we use GRBVar* to copy variables
    deque<GRBVar*> intvars;
    x = model.getVars();
    for (int j = 0; j < model.get(GRB_IntAttr_NumVars); ++j)
    {
      if (x[j].get(GRB_CharAttr_VType) != GRB_CONTINUOUS)
      {
        intvars.push_back(&x[j]);
        x[j].set(GRB_CharAttr_VType, GRB_CONTINUOUS);
      }
    }

    model.getEnv().set(GRB_IntParam_OutputFlag, 0);
    model.optimize();

    // Perform multiple iterations. In each iteration, identify the first
    // quartile of integer variables that are closest to an integer value
    // in the relaxation, fix them to the nearest integer, and repeat.

    for (int iter = 0; iter < 1000; ++iter)
    {

      // create a list of fractional variables, sorted in order of
      // increasing distance from the relaxation solution to the nearest
      // integer value

      deque<GRBVar*> fractional;
      for (size_t j = 0; j < intvars.size(); ++j)
      {
        double sol = fabs(intvars[j]->get(GRB_DoubleAttr_X));
        if (fabs(sol - floor(sol + 0.5)) > 1e-5)
        {
          fractional.push_back(intvars[j]);
        }
      }

      cout << "Iteration " << iter << ", obj " <<
      model.get(GRB_DoubleAttr_ObjVal) << ", fractional " <<
      fractional.size() << endl;

      if (fractional.size() == 0)
      {
        cout << "Found feasible solution - objective " <<
        model.get(GRB_DoubleAttr_ObjVal) << endl;
        break;
      }

      // Fix the first quartile to the nearest integer value
      sort(fractional.begin(), fractional.end(), vcomp);
      int nfix = fractional.size() / 4;
      nfix = (nfix > 1) ? nfix : 1;
      for (int i = 0; i < nfix; ++i)
      {
        GRBVar* v = fractional[i];
        double fixval = floor(v->get(GRB_DoubleAttr_X) + 0.5);
        v->set(GRB_DoubleAttr_LB, fixval);
        v->set(GRB_DoubleAttr_UB, fixval);
        cout << "  Fix " << v->get(GRB_StringAttr_VarName) << " to " <<
        fixval << " ( rel " << v->get(GRB_DoubleAttr_X) << " )" <<
        endl;
      }

      model.optimize();

      // Check optimization result

      if (model.get(GRB_IntAttr_Status) != GRB_OPTIMAL)
      {
        cout << "Relaxation is infeasible" << endl;
        break;
      }
    }

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

  delete[] x;
  delete env;
  return 0;
}


bool vcomp(GRBVar* v1,
           GRBVar* v2) throw(GRBException)
{
  double sol1 = fabs(v1->get(GRB_DoubleAttr_X));
  double sol2 = fabs(v2->get(GRB_DoubleAttr_X));
  double frac1 = fabs(sol1 - floor(sol1 + 0.5));
  double frac2 = fabs(sol2 - floor(sol2 + 0.5));
  return (frac1 < frac2);
}
