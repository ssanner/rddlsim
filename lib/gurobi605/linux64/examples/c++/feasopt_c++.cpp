/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, adds artificial
   variables to each constraint, and then minimizes the sum of the
   artificial variables.  A solution with objective zero corresponds
   to a feasible solution to the input model.
   We can also use FeasRelax feature to do it. In this example, we
   use minrelax=1, i.e. optimizing the returned model finds a solution
   that minimizes the original objective, but only from among those
   solutions that minimize the sum of the artificial variables. */

#include "gurobi_c++.h"
using namespace std;

int
main(int argc,
     char *argv[])
{
  if (argc < 2)
  {
    cout << "Usage: feasopt_c++ filename" << endl;
    return 1;
  }

  GRBEnv* env = 0;
  GRBConstr* c = 0;
  try
  {
    env = new GRBEnv();
    GRBModel feasmodel = GRBModel(*env, argv[1]);

    // Create a copy to use FeasRelax feature later */
    GRBModel feasmodel1 = GRBModel(feasmodel);

    // clear objective
    feasmodel.setObjective(GRBLinExpr(0.0));

    // add slack variables
    c = feasmodel.getConstrs();
    for (int i = 0; i < feasmodel.get(GRB_IntAttr_NumConstrs); ++i)
    {
      char sense = c[i].get(GRB_CharAttr_Sense);
      if (sense != '>')
      {
        double coef = -1.0;
        feasmodel.addVar(0.0, GRB_INFINITY, 1.0, GRB_CONTINUOUS, 1,
                         &c[i], &coef, "ArtN_" +
                         c[i].get(GRB_StringAttr_ConstrName));
      }
      if (sense != '<')
      {
        double coef = 1.0;
        feasmodel.addVar(0.0, GRB_INFINITY, 1.0, GRB_CONTINUOUS, 1,
                         &c[i], &coef, "ArtP_" +
                         c[i].get(GRB_StringAttr_ConstrName));
      }
    }
    feasmodel.update();

    // optimize modified model
    feasmodel.write("feasopt.lp");
    feasmodel.optimize();

    // use FeasRelax feature */
    feasmodel1.feasRelax(GRB_FEASRELAX_LINEAR, true, false, true);
    feasmodel1.write("feasopt1.lp");
    feasmodel1.optimize();
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

  delete[] c;
  delete env;
  return 0;
}
