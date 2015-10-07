/* Copyright 2014, Gurobi Optimization, Inc. */

/* A simple sensitivity analysis example which reads a MIP model
   from a file and solves it. Then each binary variable is set
   to 1-X, where X is its value in the optimal solution, and
   the impact on the objective function value is reported.
*/

#include "gurobi_c++.h"
using namespace std;

int
main(int   argc,
     char *argv[])
{
  if (argc < 2) {
    cout << "Usage: sensitivity_c++ filename" << endl;
    return 1;
  }

  GRBVar* vars = NULL;
  double* origX = NULL;

  try {

    // Create environment

    GRBEnv env = GRBEnv();

    // Read and solve model

    GRBModel model = GRBModel(env, argv[1]);

    if (model.get(GRB_IntAttr_IsMIP) == 0) {
      cout << "Model is not a MIP" << endl;
      return 1;
    }

    model.optimize();

    if (model.get(GRB_IntAttr_Status) != GRB_OPTIMAL) {
      cout << "Optimization ended with status "
           << model.get(GRB_IntAttr_Status) << endl;
      return 1;
    }

    // Store the optimal solution

    double origObjVal = model.get(GRB_DoubleAttr_ObjVal);
    vars = model.getVars();
    int numVars = model.get(GRB_IntAttr_NumVars);
    origX = model.get(GRB_DoubleAttr_X, vars, numVars);

    // Disable solver output for subsequent solves

    model.getEnv().set(GRB_IntParam_OutputFlag, 0);

    // Iterate through unfixed, binary variables in model

    for (int i = 0; i < numVars; i++) {
      GRBVar v = vars[i];
      char vType = v.get(GRB_CharAttr_VType);

      if (v.get(GRB_DoubleAttr_LB) == 0 && v.get(GRB_DoubleAttr_UB) == 1
          && (vType == GRB_BINARY || vType == GRB_INTEGER)) {

        // Set variable to 1-X, where X is its value in optimal solution

        if (origX[i] < 0.5) {
          v.set(GRB_DoubleAttr_LB, 1.0);
          v.set(GRB_DoubleAttr_Start, 1.0);
        } else {
          v.set(GRB_DoubleAttr_UB, 0.0);
          v.set(GRB_DoubleAttr_Start, 0.0);
        }

        // Update MIP start for the other variables

        for (int j = 0; j < numVars; j++) {
          if (j != i) {
            vars[j].set(GRB_DoubleAttr_Start, origX[j]);
          }
        }

        // Solve for new value and capture sensitivity information

        model.optimize();

        if (model.get(GRB_IntAttr_Status) == GRB_OPTIMAL) {
          cout << "Objective sensitivity for variable "
               << v.get(GRB_StringAttr_VarName) << " is "
               << (model.get(GRB_DoubleAttr_ObjVal) - origObjVal) << endl;
        } else {
          cout << "Objective sensitivity for variable "
               << v.get(GRB_StringAttr_VarName) << " is infinite" << endl;
        }

        // Restore the original variable bounds

        v.set(GRB_DoubleAttr_LB, 0.0);
        v.set(GRB_DoubleAttr_UB, 1.0);
      }
    }

  } catch (GRBException e) {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  } catch (...) {
    cout << "Error during optimization" << endl;
  }

  delete[] vars;
  delete[] origX;

  return 0;
}
