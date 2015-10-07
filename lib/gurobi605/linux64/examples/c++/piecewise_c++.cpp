/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example considers the following separable, convex problem:

     minimize    f(x) - y + g(z)
     subject to  x + 2 y + 3 z <= 4
                 x +   y       >= 1
                 x,    y,    z <= 1

   where f(u) = exp(-u) and g(u) = 2 u^2 - 4 u, for all real u. It
   formulates and solves a simpler LP model by approximating f and
   g with piecewise-linear functions. Then it transforms the model
   into a MIP by negating the approximation for f, which corresponds
   to a non-convex piecewise-linear function, and solves it again.
*/

#include "gurobi_c++.h"
#include <cmath>
using namespace std;

double f(double u) { return exp(-u); }
double g(double u) { return 2 * u * u - 4 * u; }

int
main(int   argc,
     char *argv[])
{
  double *ptu = NULL;
  double *ptf = NULL;
  double *ptg = NULL;

  try {

    // Create environment

    GRBEnv env = GRBEnv();

    // Create a new model

    GRBModel model = GRBModel(env);

    // Create variables

    double lb = 0.0, ub = 1.0;

    GRBVar x = model.addVar(lb, ub, 0.0, GRB_CONTINUOUS, "x");
    GRBVar y = model.addVar(lb, ub, 0.0, GRB_CONTINUOUS, "y");
    GRBVar z = model.addVar(lb, ub, 0.0, GRB_CONTINUOUS, "z");

    // Integrate new variables

    model.update();

    // Set objective for y

    model.setObjective(-y);

    // Add piecewise-linear objective functions for x and z

    int npts = 101;
    ptu = new double[npts];
    ptf = new double[npts];
    ptg = new double[npts];

    for (int i = 0; i < npts; i++) {
      ptu[i] = lb + (ub - lb) * i / (npts - 1);
      ptf[i] = f(ptu[i]);
      ptg[i] = g(ptu[i]);
    }

    model.setPWLObj(x, npts, ptu, ptf);
    model.setPWLObj(z, npts, ptu, ptg);

    // Add constraint: x + 2 y + 3 z <= 4

    model.addConstr(x + 2 * y + 3 * z <= 4, "c0");

    // Add constraint: x + y >= 1

    model.addConstr(x + y >= 1, "c1");

    // Optimize model as an LP

    model.optimize();

    cout << "IsMIP: " << model.get(GRB_IntAttr_IsMIP) << endl;

    cout << x.get(GRB_StringAttr_VarName) << " "
         << x.get(GRB_DoubleAttr_X) << endl;
    cout << y.get(GRB_StringAttr_VarName) << " "
         << y.get(GRB_DoubleAttr_X) << endl;
    cout << z.get(GRB_StringAttr_VarName) << " "
         << z.get(GRB_DoubleAttr_X) << endl;

    cout << "Obj: " << model.get(GRB_DoubleAttr_ObjVal) << endl;

    cout << endl;

    // Negate piecewise-linear objective function for x

    for (int i = 0; i < npts; i++) {
      ptf[i] = -ptf[i];
    }

    model.setPWLObj(x, npts, ptu, ptf);

    // Optimize model as a MIP

    model.optimize();

    cout << "IsMIP: " << model.get(GRB_IntAttr_IsMIP) << endl;

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

  delete[] ptu;
  delete[] ptf;
  delete[] ptg;

  return 0;
}
