/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QCP model:

     maximize    x
     subject to  x + y + z = 1
                 x^2 + y^2 <= z^2 (second-order cone)
                 x^2 <= yz        (rotated second-order cone)
*/

#include "gurobi_c++.h"
using namespace std;

int
main(int   argc,
     char *argv[])
{
  try {
    GRBEnv env = GRBEnv();

    GRBModel model = GRBModel(env);

    // Create variables

    GRBVar x = model.addVar(0.0, GRB_INFINITY, 0.0, GRB_CONTINUOUS, "x");
    GRBVar y = model.addVar(0.0, GRB_INFINITY, 0.0, GRB_CONTINUOUS, "y");
    GRBVar z = model.addVar(0.0, GRB_INFINITY, 0.0, GRB_CONTINUOUS, "z");

    // Integrate new variables

    model.update();

    // Set objective

    GRBLinExpr obj = x;
    model.setObjective(obj, GRB_MAXIMIZE);

    // Add linear constraint: x + y + z = 1

    model.addConstr(x + y + z == 1, "c0");

    // Add second-order cone: x^2 + y^2 <= z^2

    model.addQConstr(x*x + y*y <= z*z, "qc0");

    // Add rotated cone: x^2 <= yz

    model.addQConstr(x*x <= y*z, "qc1");

    // Optimize model

    model.optimize();

    cout << x.get(GRB_StringAttr_VarName) << " "
         << x.get(GRB_DoubleAttr_X) << endl;
    cout << y.get(GRB_StringAttr_VarName) << " "
         << y.get(GRB_DoubleAttr_X) << endl;
    cout << z.get(GRB_StringAttr_VarName) << " "
         << z.get(GRB_DoubleAttr_X) << endl;

    cout << "Obj: " << model.get(GRB_DoubleAttr_ObjVal) << endl;

  } catch(GRBException e) {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  } catch(...) {
    cout << "Exception during optimization" << endl;
  }

  return 0;
}
