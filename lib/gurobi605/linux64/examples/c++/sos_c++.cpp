/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example creates a very simple Special Ordered Set (SOS) model.
   The model consists of 3 continuous variables, no linear constraints,
   and a pair of SOS constraints of type 1. */

#include "gurobi_c++.h"
using namespace std;

int
main(int   argc,
     char *argv[])
{
  GRBEnv *env = 0;
  GRBVar *x = 0;
  try {
    env = new GRBEnv();

    GRBModel model = GRBModel(*env);

    // Create variables

    double ub[]    = {1, 1, 2};
    double obj[]   = {-2, -1, -1};
    string names[] = {"x0", "x1", "x2"};

    x = model.addVars(NULL, ub, obj, NULL, names, 3);

    // Integrate new variables

    model.update();

    // Add first SOS1: x0=0 or x1=0

    GRBVar sosv1[]  = {x[0], x[1]};
    double soswt1[] = {1, 2};

    model.addSOS(sosv1, soswt1, 2, GRB_SOS_TYPE1);

    // Add second SOS1: x0=0 or x2=0 */

    GRBVar sosv2[]  = {x[0], x[2]};
    double soswt2[] = {1, 2};

    model.addSOS(sosv2, soswt2, 2, GRB_SOS_TYPE1);

    // Optimize model

    model.optimize();

    for (int i = 0; i < 3; i++)
      cout << x[i].get(GRB_StringAttr_VarName) << " "
           << x[i].get(GRB_DoubleAttr_X) << endl;

    cout << "Obj: " << model.get(GRB_DoubleAttr_ObjVal) << endl;

  } catch(GRBException e) {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  } catch(...) {
    cout << "Exception during optimization" << endl;
  }

  delete[] x;
  delete env;
  return 0;
}
